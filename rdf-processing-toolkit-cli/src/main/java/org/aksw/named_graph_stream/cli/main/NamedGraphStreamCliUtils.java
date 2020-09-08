package org.aksw.named_graph_stream.cli.main;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.transform.result_set.QueryExecutionTransformResult;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.Dataset;
import org.apache.jena.shared.PrefixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.rxjava3.core.Flowable;

public class NamedGraphStreamCliUtils {

    private static final Logger logger = LoggerFactory.getLogger(NamedGraphStreamCliUtils.class);

    /**
     * Open wrapper for the convention where STDIN can be referred to by the following means:
     * - no argument given
     * - single argument matching '-'
     *
     * @param args
     * @param probeLangs
     * @return
     */
//    public static TypedInputStream open(String args, Collection<Lang> probeLangs) {
//        String src = args.isEmpty()
//                ? "-"
//                : args.get(0);
//
//        TypedInputStream result = RDFDataMgrEx.open(src, probeLangs);
//        return result;
//    }

    /**
         * Default procedure to obtain a stream of named graphs from a
         * list of non-option arguments
         *
         * If the list is empty or the first argument is '-' data will be read from stdin
         * @param args
         */
        public static Flowable<Dataset> createNamedGraphStreamFromArgs(
                List<String> rawArgs,
                String fmtHint,
                PrefixMapping pm) {

            List<String> args = NgsCmdImpls.preprocessArgs(rawArgs);
            Map<String, Callable<TypedInputStream>> map = NgsCmdImpls.validate(args, MainCliNamedGraphStream.quadLangs, true);


            Flowable<Dataset> result = Flowable.fromIterable(map.entrySet())
                    .flatMap(arg -> {
//                        TypedInputStream tmp = RDFDataMgrEx.open(arg, MainCliNamedGraphStream.quadLangs);

                        String argName = arg.getKey();
                        logger.info("Loading stream for arg " + argName);
                        Callable<TypedInputStream> inSupp = arg.getValue();
                        Flowable<Dataset> r = RDFDataMgrRx.createFlowableDatasets(inSupp)
                        // TODO Decoding of distinguished names should go into the util method
                            .map(ds -> QueryExecutionTransformResult.applyNodeTransform(RDFDataMgrRx::decodeDistinguished, ds));
                        return r;
                    });

            return result;
        }

}