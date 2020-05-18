package org.aksw.sportal_analysis.main;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.aksw.jena_sparql_api.rx.SparqlRx;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import org.apache.jena.ext.com.google.common.collect.MultimapBuilder;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.engine.binding.BindingProject;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprLib;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.aggregate.Accumulator;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.apache.jena.vocabulary.RDF;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;

/**
 * ExecutionNode: For a given binding return a flow of subsequent bindings
 *
 * @author raven
 *
 */
interface NodeExec {
    Flowable<Binding> apply(Binding binding);
}

class NodeSpec {
    protected Query query;

//    public Flowable<Binding> createExec(ExecutionContext ctx, Binding binding) {
//    }
}

interface StreamNode {

}

// Aggregate bindings based on a query
// thereby using the passed in bindings instead of the queryPattern
//class AccQuerySelect
//	implements Acc<Binding>
//{
//	protected Query query;
//
//	@Override
//	public void accumulate(Binding binding) {
//
//	}
//
//	@Override
//	public Binding getValue() {
//		QueryExecution.create().query(query).build().
//	}
//}

interface AccPush {
    void accumulate(Binding binding);
}

class AccSubQuery {
    protected Query query;
    protected Graph graph;

}

class AccFilter {
    protected List<Expr> filters;

}


abstract class FlowBase<T>
    implements FlowableSubscriber<T>
{
    protected FlowableEmitter<T> emitter;

    public FlowBase(FlowableEmitter<T> emitter) {
        super();
        this.emitter = emitter;
    }

    @Override
    public void onError(Throwable t) {
        emitter.onError(t);
    }

    @Override
    public void onSubscribe(@NonNull Subscription s) {
        emitter.setCancellable(s::cancel);
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onComplete() {
        emitter.onComplete();
    }
}

abstract class QueryFlowBase<T>
    extends FlowBase<T>
{
    protected ExecutionContext execCxt;

    public QueryFlowBase(FlowableEmitter<T> emitter, ExecutionContext execCxt) {
        super(emitter);
        this.execCxt = execCxt;
    }
}


/**
 * Execution of projection
 *
 * Based on {@link org.apache.jena.sparql.engine.iterator.QueryIterAssign}
 * @author raven
 *
 */
class AccProject
    extends QueryFlowBase<Binding>
{
    protected Collection<Var> vars;

    public AccProject(FlowableEmitter<Binding> emitter, ExecutionContext execCxt, Collection<Var> vars) {
        super(emitter, execCxt);
        this.vars = vars;
    }

    @Override
    public void onNext(@NonNull Binding binding) {
        Binding b = new BindingProject(vars, binding);
        emitter.onNext(b);
    }

    public static FlowableTransformer<Binding, Binding> createTransformer(
            ExecutionContext execCxt,
            Collection<Var> vars) {
        return RxUtils.createTransformer(emitter -> new AccProject(emitter, execCxt, vars));
    }

}

/**
 * Execution of assignment
 *
 * Based on {@link org.apache.jena.sparql.engine.iterator.QueryIterAssign}
 * @author raven
 *
 */
class AccAssign
    extends QueryFlowBase<Binding>
{
    protected VarExprList exprs;

    public AccAssign(FlowableEmitter<Binding> emitter, ExecutionContext execCxt, VarExprList exprs) {
        super(emitter, execCxt);
        this.exprs = exprs;
    }

    @Override
    public void onNext(@NonNull Binding binding) {
        BindingMap b = BindingFactory.create(binding);
        for (Var v : exprs.getVars()) {
            Node n = exprs.get(v, b, execCxt);
            if (n != null) {
                b.add(v, n);
            }
        }
        emitter.onNext(b);
    }


    public static FlowableTransformer<Binding, Binding> createTransformer(
            ExecutionContext execCxt,
            VarExprList exprs) {
        return RxUtils.createTransformer(emitter -> new AccAssign(emitter, execCxt, exprs));
    }

}


class AccGroupBy
    extends QueryFlowBase<Binding>
{
    protected VarExprList groupVarExpr;
    protected List<ExprAggregator> aggregators;

    protected boolean noInput = true;

    protected Multimap<Binding, Pair<Var, Accumulator>> accumulators = MultimapBuilder.hashKeys().arrayListValues()
            .build();

    private static Pair<Var, Accumulator> placeholder = Pair.create((Var) null, (Accumulator) null);

    public AccGroupBy(
            FlowableEmitter<Binding> emitter,
            ExecutionContext execCxt,
            VarExprList groupVarExpr,
            List<ExprAggregator> aggregators
            ) {
        super(emitter, execCxt);
        this.groupVarExpr = groupVarExpr;
        this.aggregators = aggregators;
    }

    @Override
    public void onNext(Binding b) {

        this.noInput = false;

        boolean hasAggregators = (aggregators != null && !aggregators.isEmpty());
        boolean hasGroupBy = !groupVarExpr.isEmpty();
        // boolean noInput = ! iter.hasNext();

        // Case: No input.
        // 1/ GROUP BY - no rows.
        // 2/ No GROUP BY, e.g. COUNT=0, the results is one row always and not handled
        // here.

        // Case: there is input.
        // Phase 1 : Create keys and aggreators per key, and pump bindings through the
        // aggregators.
        // Multimap<Binding, Pair<Var, Accumulator>> accumulators =
        // MultimapBuilder.hashKeys().arrayListValues().build();

        Binding key = genKey(groupVarExpr, b, execCxt);

        if (!hasAggregators) {
            // Put in a dummy to remember the input.
            accumulators.put(key, placeholder);
            // continue;
        }

        // Create if does not exist.
        if (!accumulators.containsKey(key)) {
            for (ExprAggregator agg : aggregators) {
                Accumulator x = agg.getAggregator().createAccumulator();
                Var v = agg.getVar();
                accumulators.put(key, Pair.create(v, x));
            }
        }

        // Do the per-accumulator calculation.
        for (Pair<Var, Accumulator> pair : accumulators.get(key))
            pair.getRight().accumulate(b, execCxt);

        // Phase 2 : There was input and so there are some groups.
        // For each bucket, get binding, add aggregator values to the binding.
        // We used AccNull so there are always accumulators.

    }

    public void onComplete() {
        boolean hasAggregators = (aggregators != null && !aggregators.isEmpty());
        boolean hasGroupBy = !groupVarExpr.isEmpty();

        if (noInput) {
            if (hasGroupBy)
                // GROUP
                // return Iter.nullIterator() ;
                if (!hasAggregators) {
                    // No GROUP BY, no aggregators. One result row of no colums.
                    // return Iter.singleton(BindingFactory.binding());
                    emitter.onNext(BindingFactory.binding());
                }
            // No GROUP BY, has aggregators. Insert default values.
            BindingMap binding = BindingFactory.create();
            for (ExprAggregator agg : aggregators) {
                Node value = agg.getAggregator().getValueEmpty();
                if (value == null)
                    continue;
                Var v = agg.getVar();
                binding.add(v, value);
            }
            // return Iter.singleton(binding);
            emitter.onNext(binding);
        }

        for (Binding k : accumulators.keySet()) {
            Collection<Pair<Var, Accumulator>> accs = accumulators.get(k);
            BindingMap b = BindingFactory.create(k);

            for (Pair<Var, Accumulator> pair : accs) {
                NodeValue value = pair.getRight().getValue();
                if (value == null)
                    continue;
                Var v = pair.getLeft();
                b.add(v, value.asNode());
            }
            // results.add(b);
            emitter.onNext(b);
        }

        emitter.onComplete();

        // return results.iterator();
    }

    static private Binding genKey(VarExprList vars, Binding binding, ExecutionContext execCxt) {
        return copyProject(vars, binding, execCxt);
    }

    static private Binding copyProject(VarExprList vars, Binding binding, ExecutionContext execCxt) {
        // No group vars (implicit or explicit) => working on whole result set.
        // Still need a BindingMap to assign to later.
        BindingMap x = BindingFactory.create();
        for (Var var : vars.getVars()) {
            Node node = vars.get(var, binding, execCxt);
            // Null returned for unbound and error.
            if (node != null) {
                x.add(var, node);
            }
        }
        return x;
    }


    public static FlowableTransformer<Binding, Binding> createTransformer(
            ExecutionContext execCxt,
            VarExprList groupVarExpr,
            List<ExprAggregator> aggregators) {
        return RxUtils.createTransformer(emitter ->
            new AccGroupBy(emitter, execCxt, groupVarExpr, aggregators));
    }


}


class RxUtils {
    /**
     * Utils method to create a transformer from a function that takes a FlowableEmitter and
     * yields a FlowableSubscriber from it.
     *
     * @param <I>
     * @param <O>
     * @param fsSupp
     * @return
     */
    public static <I, O> FlowableTransformer<I, O> createTransformer(Function<? super FlowableEmitter<O>, ? extends FlowableSubscriber<I>> fsSupp) {
        return upstream -> {
            Flowable<O> result = Flowable.create(new FlowableOnSubscribe<O>() {
                @Override
                public void subscribe(FlowableEmitter<O> emitter) throws Exception {
                    FlowableSubscriber<I> subscriber = fsSupp.apply(emitter);
                    upstream.subscribe(subscriber);
                }
            }, BackpressureStrategy.BUFFER);

            return result;
        };
    }
}

public class JoinStream {
    public static Flowable<Binding> create(Query query, Supplier<? extends SparqlQueryConnection> queryConnSupp) {
        // FIXME Close the connection; tie it to the query execution
        // Queryexecution qe = new QueryExecution();
        return SparqlRx.execSelectRaw(() -> queryConnSupp.get().query(query));
    }

    public static ExecutionContext createExecutionContextDefault() {
        Context context = ARQ.getContext().copy();
        context.set(ARQConstants.sysCurrentTime, NodeFactoryExtra.nowAsDateTime());
        ExecutionContext result = new ExecutionContext(context, null, null, null);
        return result;
    }
    public static FlowableTransformer<Binding, Binding> createSink(Query query) {
        ExecutionContext execCxt = createExecutionContextDefault();
        FlowableTransformer<Binding, Binding> result = createTransform(query, execCxt);
        return result;
    }

    public static FlowableTransformer<Binding, Binding> createTransform(Query query, ExecutionContext execCxt) {
        VarExprList groupVarExpr = query.getGroupBy();
        List<ExprAggregator> aggregators = query.getAggregators();

        // Create an updated projection
        VarExprList rawProj = query.getProject();


        VarExprList exprs = new VarExprList();
        for (Var v : rawProj.getVars() )
        {
            Expr e = rawProj.getExpr(v) ;
            if ( e != null )
            {
                Expr e2 = ExprLib.replaceAggregateByVariable(e) ;
                exprs.add(v, e2) ;
            } else {
                exprs.add(v);
            }
            // Include in project
            // vars.add(v) ;
        }



        Op op = Algebra.compile(query);
        System.out.println(op);

        FlowableTransformer<Binding, Binding> result = upstream -> upstream
                .compose(AccGroupBy.createTransformer(execCxt, groupVarExpr, aggregators))
                .compose(AccAssign.createTransformer(execCxt, exprs))
                .compose(AccProject.createTransformer(execCxt, exprs.getVars()));

        return result;
    }


    public static void main(String[] args) {
        FlowableTransformer<Binding, Binding> accGroupBy = createSink(QueryFactory.create("SELECT (COUNT(*) + 1 AS ?c) { }"));



        Dataset ds = DatasetFactory.create();
        ds.asDatasetGraph().add(Quad.defaultGraphIRI, RDF.Nodes.type, RDF.Nodes.type, RDF.Nodes.type);
        SparqlQueryConnection conn = RDFConnectionFactory.connect(ds);
        Query query = QueryFactory.create("SELECT * { ?s ?p ?o }");

        Flowable<Binding> root = JoinStream.create(query, () -> conn);

        Flowable<Binding> x = root.share();
        ConnectableFlowable<Binding> publisher = x.publish();

        //publisher.subscribe(y -> System.out.println("Listener 1" + y));
        Flowable<Binding> counter = publisher.compose(accGroupBy);

        counter.subscribe(count -> System.out.println("Counter saw: " + count));

        publisher.subscribe(y -> System.out.println("Listener 2" + y));





        // Start the whole process
        Disposable d = publisher.connect();



        // x.conn

        // root.share()

        // Flowable.
        // root.doOnNext(onNext);

    }
}
