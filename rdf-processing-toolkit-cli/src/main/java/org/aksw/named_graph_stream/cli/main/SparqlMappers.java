package org.aksw.named_graph_stream.cli.main;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import org.aksw.jena_sparql_api.core.connection.RDFConnectionFactoryEx;
import org.aksw.jena_sparql_api.json.RdfJsonUtils;
import org.aksw.jena_sparql_api.rx.ResultSetRx;
import org.aksw.jena_sparql_api.rx.ResultSetRxImpl;
import org.aksw.jena_sparql_api.rx.SparqlRx;
import org.aksw.jena_sparql_api.rx.op.OperatorLocalOrder;
import org.aksw.jena_sparql_api.stmt.SPARQLResultEx;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.utils.ResultSetUtils;
import org.aksw.sparql_integrate.cli.main.OutputMode;
import org.aksw.sparql_integrate.cli.main.SPARQLResultExVisitor;
import org.aksw.sparql_integrate.cli.main.SPARQLResultExVisitorCollector;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;

import com.google.gson.JsonElement;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Create functions that run SPARQL statements on a given connection and yield flowables of various result items
 *
 * @author raven
 *
 */
public class SparqlMappers {
    /**
     * Return a function that runs the given sequence of sparql statements on a connection.
     * Returns a flow of all bindings yeld by select statements.
     * Non-select queries are executed nonetheless and their results are passed to the sparqlResultExVisitor
     *
     * @param stmts
     * @param sparqlResultExVisitor
     * @return
     */
    public static Function<RDFConnection, ResultSetRx> createProcessorResultSetRx(List<SparqlStmt> stmts, SPARQLResultExVisitor<?> sparqlResultExVisitor) {
        List<Var> vars = SparqlStmtUtils.getUnionProjectVars(stmts);
        Function<RDFConnection, Flowable<Binding>> mapper = createMapperBinding(stmts, sparqlResultExVisitor);
        return conn -> new ResultSetRxImpl(vars, mapper.apply(conn));
    }


    public static <I, O> FlowableTransformer<I, O> createParallelMapperOrdered(boolean parallel,
            Function<? super I, O> mapper) {
        return parallel
                ? createParallelMapperOrdered(mapper)
                : upstream -> upstream.map(mapper::apply);
    }

    /**
     * Factory method for yielding a FlowableTransformer that applies a given flatMap function in parallel
     * but apply local ordering so that items are emitted in order
     *
     * @param <I>
     * @param <O>
     * @param flatMapper
     * @return
     */
    public static <I, O> FlowableTransformer<I, O> createParallelMapperOrdered(
            Function<? super I, O> mapper) {
        return in -> in
//            .map(x -> Maps.immutableEntry(x, 0l))
            .zipWith(() -> LongStream.iterate(0, i -> i + 1).iterator(), Maps::immutableEntry)
            .parallel() //Runtime.getRuntime().availableProcessors(), 8) // Prefetch only few items
            .runOn(Schedulers.io())
            //.observeOn(Schedulers.computation())
            .map(e -> {
                I before = e.getKey();
                O after = mapper.apply(before);
                Entry<O, Long> r = new SimpleEntry<>(after, e.getValue());
                return r;
            })
            .sequential()
            .lift(OperatorLocalOrder.create(0l, i -> i + 1, (a, b) -> a - b, Entry::getValue))
            .map(Entry::getKey);
    }


    /**
     * Apply a mapper based on RDFConnection on Datasets
     *
     * @param mapper
     * @return
     */
    public static <I extends Dataset, O> FlowableTransformer<I, O> datasetToConnection(Function<RDFConnection, Flowable<O>> mapper) {
        return upstream -> {
            return upstream.flatMap(ds -> {
                return Flowable.using(
                        () -> RDFConnectionFactory.connect(ds),
                        mapper::apply,
                        RDFConnection::close);
            });
        };
    }


    public static <I extends Dataset, O> Function<I, O> datasetAsConnection(
            Function<? super RDFConnection, O> mapper) {

        return dataset -> {
            O r;
            try(RDFConnection conn = RDFConnectionFactory.connect(dataset)) {
                r = mapper.apply(conn);
            }
            return r;
        };
    }

    public static Function<RDFConnection, RDFConnection> applyContextHandler(Consumer<Context> contextHandler) {
        // Wrap the core processor with modifiers for the context
        return conn -> (contextHandler == null
            ? conn
            : RDFConnectionFactoryEx.wrapWithContext(conn, contextHandler));
    }


    /**
     * Helper function that executes a statement on a connection,
     * passes the result to a visitor (if applicable)
     * and yields an empty stream
     *
     * @param <T>
     * @param conn
     * @param stmt
     * @param sparqlResultVisitor
     * @return
     * @throws Exception
     */
    public static <T> Flowable<T> fallbackToVisitor(RDFConnection conn, SparqlStmt stmt, SPARQLResultExVisitor<?> sparqlResultVisitor) throws Exception {
        try (SPARQLResultEx sr = SparqlStmtUtils.execAny(conn, stmt)) {
            if (sparqlResultVisitor != null) {
                sparqlResultVisitor.forwardEx(sr);
            }
        }

        return Flowable.empty();
    }


    /**
     * Mapper that flatMaps all select queries to a resulting flow of bindings.
     * Non-select queries are executed and their results forwarded to a sparqlResultVisitor
     *
     * @param stmts
     * @param sparqlResultVisitor Receiver for non-select query results. May be null.
     * @return
     */
    public static Function<RDFConnection, Flowable<Binding>> createMapperBinding(
            Collection<? extends SparqlStmt> stmts,
            SPARQLResultExVisitor<?> sparqlResultVisitor
            ) {
        return createMapperSelect(stmts, sparqlResultVisitor, (conn, query) -> SparqlRx.execSelectRaw(conn, query));
    }

    public static Function<RDFConnection, Flowable<QuerySolution>> createMapperQuerySolution(
            Collection<? extends SparqlStmt> stmts,
            SPARQLResultExVisitor<?> sparqlResultVisitor
            ) {
        return createMapperSelect(stmts, sparqlResultVisitor, (conn, query) -> SparqlRx.execSelect(conn, query));
    }


    public static <B> Function<RDFConnection, Flowable<B>> createMapperSelect(
            Collection<? extends SparqlStmt> stmts,
            SPARQLResultExVisitor<?> sparqlResultVisitor,
            BiFunction<? super SparqlQueryConnection, ? super Query, Flowable<B>> execSelect
            ) {
        return conn ->
            Flowable
                .fromIterable(stmts)
                .flatMap(stmt -> {
                    Flowable<B> r = null;

                    if (stmt.isQuery()) {
                        Query query = stmt.getQuery();
                        if (query.isSelectType()) {
                            r = execSelect.apply(conn, query);
                        }
                    }

                    r = r != null ? r : fallbackToVisitor(conn, stmt, sparqlResultVisitor);

                    return r;
                });
    }

    public static Function<RDFConnection, Flowable<Quad>> createMapperQuad(
            Collection<? extends SparqlStmt> stmts,
            SPARQLResultExVisitor<?> sparqlResultVisitor) {
        return conn ->
            Flowable
                .fromIterable(stmts)
                .flatMap(stmt -> {
                    Flowable<Quad> r = null;

                    if (stmt.isQuery()) {
                        Query query = stmt.getQuery();
                        if (query.isConstructType()) {
                            if(query.isConstructQuad()) {
                                r = SparqlRx.execConstructQuads(conn, query);
                            } else {
                                r = SparqlRx.execConstructTriples(conn, query)
                                        .map(t -> new Quad(Quad.defaultGraphNodeGenerated, t));
                            }
                        }
                    }

                    r = r != null ? r : fallbackToVisitor(conn, stmt, sparqlResultVisitor);

                    return r;
                });
    }

    public static Function<RDFConnection, Flowable<JsonObject>> createMapperJson(
            Collection<? extends SparqlStmt> stmts,
            SPARQLResultExVisitor<?> sparqlResultVisitor) {
        return conn ->
            Flowable
                .fromIterable(stmts)
                .flatMap(stmt -> {
                    Flowable<JsonObject> r = null;

                    if (stmt.isQuery()) {
                        Query query = stmt.getQuery();
                        if (query.isJsonType()) {
                            r = SparqlRx.execJsonItems(conn, query);
                        }
                    }

                    r = r != null ? r : fallbackToVisitor(conn, stmt, sparqlResultVisitor);

                    return r;
                });
    }


    public static Function<RDFConnection, SPARQLResultEx> createMapperFromDataset(
            OutputMode outputMode,
            Collection<? extends SparqlStmt> stmts,
            SPARQLResultExVisitor<?> sparqlResultVisitor
            ) {

        List<Var> unionProjectVars = SparqlStmtUtils.getUnionProjectVars(stmts);
        Supplier<SPARQLResultExVisitorCollector> collectorSupp = () -> new SPARQLResultExVisitorCollector(unionProjectVars);

        Function<RDFConnection, SPARQLResultEx> result;

        switch (outputMode) {
        case TRIPLE:
        case QUAD:
            result = createMapperQuad(stmts, sparqlResultVisitor)
                .andThen(quads -> quads
                        .reduceWith(collectorSupp::get, (supp, quad) -> { supp.onQuads(Collections.singleton(quad).iterator()); return supp; })
                        .map(collector -> collector.getResult(outputMode))
                        .blockingGet());
            break;
        case BINDING:
            result = createMapperBinding(stmts, sparqlResultVisitor)
                .andThen(bindings -> bindings
                        .reduceWith(collectorSupp::get, (supp, binding) -> { supp.onResultSet(
                                ResultSetUtils.create2(unionProjectVars,
                                        Collections.singleton(binding).iterator())); return supp; })
                        .map(collector -> collector.getResult(outputMode))
                        .blockingGet());
            break;
        case JSON:
            result = createMapperJson(stmts, sparqlResultVisitor)
            .andThen(jsons -> jsons
                    .reduceWith(collectorSupp::get, (supp, json) -> { supp.onJsonItems(Collections.singleton(json).iterator()); return supp; })
                    .map(collector -> collector.getResult(outputMode))
                    .blockingGet());
            break;
        default:
            throw new IllegalArgumentException("Unknown output mode: " + outputMode);
        }

        return result;
    }


    public static Function<RDFConnection, Flowable<JsonElement>> createMapperJq(
            Collection<? extends SparqlStmt> stmts,
            SPARQLResultExVisitor<?> sparqlResultVisitor,
            int depth,
            boolean flat) {

        return createMapperQuerySolution(stmts, sparqlResultVisitor)
            .andThen(flowable -> flowable.map(qs -> {
                JsonElement json = RdfJsonUtils.toJson(qs, depth, flat);
                return json;
            }));
    }
}
