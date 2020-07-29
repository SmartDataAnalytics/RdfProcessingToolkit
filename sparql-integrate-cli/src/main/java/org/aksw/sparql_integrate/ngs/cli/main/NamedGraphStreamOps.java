package org.aksw.sparql_integrate.ngs.cli.main;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.aksw.commons.util.strings.StringUtils;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.rx.op.OperatorOrderedGroupBy;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsMap;
import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsSort;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.SparqlQueryConnection;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.engine.http.Service;
import org.apache.jena.sparql.util.Context;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import joptsimple.internal.Strings;

public class NamedGraphStreamOps {

    public static String craftIriForNode(Node node) {
        String result = node.isURI()
                ? node.getURI()
                : node.isBlank()
                    ? "x-bnode://" + node.getBlankNodeLabel()
                    : "x-literal:// " + StringUtils.urlEncode(node.getLiteralLexicalForm());
        return result;
    }


    /**
     * Typical use case is to group a sequence of consecutive triples by subject
     *
     * @param field
     * @return
     */
    public static FlowableTransformer<Triple, Dataset> groupConsecutiveTriplesByComponent(
            Function<Triple, Node> grouper,
            Supplier<Dataset> datasetSupplier) {

        return upstream ->
            upstream
                .lift(OperatorOrderedGroupBy.<Triple, Node, List<Triple>>create(
                        grouper::apply,
                        groupKey -> new ArrayList<>(),
                        (l, t) -> l.add(t)))
                .map(e -> {
                    Node g = e.getKey();
                    List<Triple> l = e.getValue();
                    Dataset ds = datasetSupplier.get();
                    String graphName = craftIriForNode(g);
                    Model m = ds.getNamedModel(graphName);
                    Graph mg = m.getGraph();
                    for(Triple t : l) {
                        mg.add(t);
                    }
                    return ds;
                });
    }


    /**
     *
     * @param cmdSort
     * @param keyQueryParser
     * @param format         Serialization format when passing data to the system
     *                       sort command
     * @return
     */
    public static FlowableTransformer<Dataset, Dataset> createSystemSorter(CmdNgsSort cmdSort,
            SparqlQueryParser keyQueryParser) {
        String keyArg = cmdSort.key;

        Function<? super SparqlQueryConnection, Node> keyMapper = MainCliNamedGraphStream.createKeyMapper(keyArg,
                keyQueryParser, MainCliNamedGraphStream.DISTINCT_NAMED_GRAPHS);

//			keyQueryParser = keyQueryParser != null
//					? keyQueryParser
//					: SparqlQueryParserWrapperSelectShortForm.wrap(SparqlQueryParserImpl.create(DefaultPrefixes.prefixes));

        // SPARQL : SELECT ?key { ?s eg:hash ?key }
        // Short SPARQL: ?key { ?s eg:hash ?key }
        // LDPath : issue: what to use as the root?

        List<String> sortArgs = SysCalls.createDefaultSortSysCall(cmdSort);

        FlowableTransformer<Dataset, Dataset> sorter = DatasetFlowOps.sysCallSort(keyMapper, sortArgs);

        FlowableTransformer<Dataset, Dataset> result = !cmdSort.merge ? sorter
                : upstream -> upstream.compose(sorter).compose(s -> DatasetFlowOps.mergeConsecutiveDatasets(s));
        return result;
    }

    public static void map(PrefixMapping pm, CmdNgsMap cmdMap, OutputStream out) throws Exception {

        String timeoutSpec = cmdMap.serviceTimeout;
        Consumer<Context> contextHandler = cxt -> {
            if (!Strings.isNullOrEmpty(timeoutSpec)) {
                cxt.set(Service.queryTimeout, timeoutSpec);
            }
        };

        Flowable<Dataset> flow = MainCliNamedGraphStream.mapCore(contextHandler, pm, cmdMap);

//        RDFDataMgrRx.writeDatasets(flow, out, RDFFormat.TRIG_PRETTY);

//
        Flowable<Throwable> tmp = flow.buffer(1)
                .compose(RDFDataMgrRx.createDatasetBatchWriter(out, RDFFormat.TRIG_PRETTY));

        Throwable e = tmp.singleElement().blockingGet();
        if (e != null) {
            throw new RuntimeException(e);
        }

        // flow.blockingForEach(System.out::print);

        // flow.forEach(System.out::println);
        // RDFDataMgrRx.writeDatasets(flow, new NullOutputStream(), RDFFormat.TRIG);
        // RDFDataMgrRx.writeDatasets(flow, System.out, RDFFormat.TRIG_PRETTY);

    }

}
