package org.aksw.named_graph_stream.cli.main;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.aksw.commons.rx.op.FlowableOperatorSequentialGroupBy;
import org.aksw.commons.util.string.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import io.reactivex.rxjava3.core.FlowableTransformer;

public class NamedGraphStreamOps {
    public static final String BASE_IRI_BNODE = "urn:bnode:";
    public static final String BASE_IRI_LITERAL = "urn:literal:";

    public static String craftIriForNode(Node node) {
        String result = node.isURI()
                ? node.getURI()
                : node.isBlank()
                    ? BASE_IRI_BNODE + node.getBlankNodeLabel()
                    : BASE_IRI_LITERAL + StringUtils.urlEncode(node.getLiteralLexicalForm());
        return result;
    }


    /**
     * Typical use case is to group a sequence of consecutive triples by subject
     *
     * @param field
     * @return
     */
    public static FlowableTransformer<Triple, Dataset> groupConsecutiveTriplesByComponent(
            Function<? super Triple, ? extends Node> grouper,
            Supplier<? extends Dataset> datasetSupplier) {

        return upstream ->
            upstream
                .lift(FlowableOperatorSequentialGroupBy.<Triple, Node, List<Triple>>create(
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

}
