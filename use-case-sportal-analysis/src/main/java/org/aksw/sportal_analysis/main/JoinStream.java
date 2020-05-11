package org.aksw.sportal_analysis.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.ext.com.google.common.collect.Multimap;
import org.apache.jena.ext.com.google.common.collect.MultimapBuilder;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingMap;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.aggregate.Accumulator;

import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;

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

class AccSubQuery
{
    protected Query query;
    protected Graph graph;


}


class AccFilter
{
    protected List<Expr> filters;


}

class AccGroupBy
//    implements Emitter<Entry<Binding, Binding>>
{
    protected VarExprList groupVarExpr;
    protected List<ExprAggregator> aggregators;
    protected ExecutionContext execCxt;


    protected boolean noInput = true;


    protected Multimap<Binding, Pair<Var, Accumulator>> accumulators = MultimapBuilder.hashKeys().arrayListValues().build();

    /**
     * The delegate to emit to
     */
    protected Emitter<Binding> emitter;

    private static Pair<Var, Accumulator> placeholder = Pair.create((Var)null, (Accumulator)null) ;

    public AccGroupBy(
        VarExprList groupVarExpr,
        List<ExprAggregator> aggregators,
        ExecutionContext execCxt)
    {
        super();
        this.groupVarExpr = groupVarExpr;
        this.aggregators = aggregators;
        this.execCxt = execCxt;
    }


    // @Override
    public void accumulate(Binding b) {

        this.noInput = false;

        boolean hasAggregators = ( aggregators != null && ! aggregators.isEmpty() );
        boolean hasGroupBy = ! groupVarExpr.isEmpty();
        // boolean noInput = ! iter.hasNext();

        // Case: No input.
        // 1/ GROUP BY - no rows.
        // 2/ No GROUP BY, e.g. COUNT=0, the results is one row always and not handled here.

        // Case: there is input.
        // Phase 1 : Create keys and aggreators per key, and pump bindings through the aggregators.
        // Multimap<Binding, Pair<Var, Accumulator>> accumulators = MultimapBuilder.hashKeys().arrayListValues().build();

        Binding key = genKey(groupVarExpr, b, execCxt);

        if ( !hasAggregators ) {
            // Put in a dummy to remember the input.
            accumulators.put(key, placeholder);
            //continue;
        }

        // Create if does not exist.
        if ( !accumulators.containsKey(key) ) {
            for ( ExprAggregator agg : aggregators ) {
                Accumulator x = agg.getAggregator().createAccumulator();
                Var v = agg.getVar();
                accumulators.put(key, Pair.create(v, x));
            }
        }

        // Do the per-accumulator calculation.
        for ( Pair<Var, Accumulator> pair : accumulators.get(key) )
            pair.getRight().accumulate(b, execCxt);

        // Phase 2 : There was input and so there are some groups.
        // For each bucket, get binding, add aggregator values to the binding.
        // We used AccNull so there are always accumulators.

    }

    public void onComplete() {
        boolean hasAggregators = ( aggregators != null && ! aggregators.isEmpty() );
        boolean hasGroupBy = ! groupVarExpr.isEmpty();

        if ( noInput ) {
            if ( hasGroupBy )
                // GROUP
                 //return Iter.nullIterator() ;
            if ( ! hasAggregators ) {
                // No GROUP BY, no aggregators. One result row of no colums.
                //return Iter.singleton(BindingFactory.binding());
                emitter.onNext(BindingFactory.binding());
            }
            // No GROUP BY, has aggregators. Insert default values.
            BindingMap binding = BindingFactory.create();
            for ( ExprAggregator agg : aggregators ) {
                Node value = agg.getAggregator().getValueEmpty();
                if ( value == null )
                    continue;
                Var v = agg.getVar();
                binding.add(v, value);
            }
            //return Iter.singleton(binding);
            emitter.onNext(binding);
        }

        List<Binding> results = new ArrayList<>();
        for ( Binding k : accumulators.keySet() ) {
            Collection<Pair<Var, Accumulator>> accs = accumulators.get(k);
            BindingMap b = BindingFactory.create(k);

            for ( Pair<Var, Accumulator> pair : accs ) {
                NodeValue value = pair.getRight().getValue();
                if ( value == null )
                    continue;
                Var v = pair.getLeft();
                b.add(v, value.asNode());
            }
            results.add(b);
        }
        //return results.iterator();
    }

    static private Binding genKey(VarExprList vars, Binding binding, ExecutionContext execCxt) {
        return copyProject(vars, binding, execCxt);
    }

    static private Binding copyProject(VarExprList vars, Binding binding, ExecutionContext execCxt) {
        // No group vars (implicit or explicit) => working on whole result set.
        // Still need a BindingMap to assign to later.
        BindingMap x = BindingFactory.create();
        for ( Var var : vars.getVars() ) {
            Node node = vars.get(var, binding, execCxt);
            // Null returned for unbound and error.
            if ( node != null ) {
                x.add(var, node);
            }
        }
        return x;
    }
}




public class JoinStream {
    public static Flowable<Binding> create(Query query, Supplier<? extends SparqlQueryConnection> queryConnection) {
        return null;
    }


    public static AccGroupBy createSink(Query query, ExecutionContext execCxt) {
        VarExprList groupVarExpr = query.getGroupBy();
        List<ExprAggregator> aggreators = query.getAggregators();

        return new AccGroupBy(groupVarExpr, aggreators, execCxt);

    }

    public static void main(String[] args) {
        Sink<Binding> ac1Sink = null;


        Dataset ds = null;
        SparqlQueryConnection conn = RDFConnectionFactory.connect(ds);
        Query query = QueryFactory.create("SELECT * { ?s ?p ?o }");

        Flowable<Binding> root = JoinStream.create(query, () -> conn);


        // Flowable.
        // root.doOnNext(onNext);



    }
}
