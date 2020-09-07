package org.aksw.sparql_integrate.cli.main;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.aksw.jena_sparql_api.utils.GraphUtils;
import org.aksw.jena_sparql_api.utils.PrefixUtils;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.lang.SinkQuadsToDataset;
import org.apache.jena.riot.out.SinkQuadOutput;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.core.Quad;

public class SinkStreamingQuads
{

    /**
     * Create a sink that for line based format
     * streams directly to the output stream or collects quads in memory and emits them
     * all at once in the given format when flushing the sink.
     *
     * @param r
     * @param format
     * @param out
     * @param dataset The dataset implementation to use for non-streaming data.
     *                Allows for use of insert-order preserving dataset implementations.
     * @return
     */
    public static SinkStreaming<Quad> createSinkQuads(RDFFormat format, OutputStream out, PrefixMapping pm, Supplier<Dataset> datasetSupp) {
        boolean useStreaming = format == null ||
                Arrays.asList(Lang.NTRIPLES, Lang.NQUADS).contains(format.getLang());

        SinkStreaming<Quad> result;
        if(useStreaming) {
            result = SinkStreamingWrapper.wrap(new SinkQuadOutput(out, null, null));
        } else {
            Dataset dataset = datasetSupp.get();
            SinkQuadsToDataset core = new SinkQuadsToDataset(false, dataset.asDatasetGraph());

            return new SinkStreamingBase<Quad>() {
                @Override
                public void close() {
                    core.close();
                }

                @Override
                public void sendActual(Quad item) {
                    core.send(item);
                }

                @Override
                public void flush() {
                    core.flush();
                }

                @Override
                public void finishActual() {
                    // TODO Prefixed graph names may break
                    // (where to define their namespace anyway? - e.g. in the default or the named graph?)
                    PrefixMapping usedPrefixes = new PrefixMappingImpl();

                    Stream.concat(
                            Stream.of(dataset.getDefaultModel()),
                            Streams.stream(dataset.listNames()).map(dataset::getNamedModel))
                    .forEach(m -> {
                        // PrefixMapping usedPrefixes = new PrefixMappingImpl();
                        try(Stream<Node> nodeStream = GraphUtils.streamNodes(m.getGraph())) {
                            PrefixUtils.usedPrefixes(pm, nodeStream, usedPrefixes);
                        }
                        m.clearNsPrefixMap();
                        // m.setNsPrefixes(usedPrefixes);
                    });

                    dataset.getDefaultModel().setNsPrefixes(usedPrefixes);
                    RDFDataMgr.write(out, dataset, format);
                }
            };
        }

        return result;
    }

}