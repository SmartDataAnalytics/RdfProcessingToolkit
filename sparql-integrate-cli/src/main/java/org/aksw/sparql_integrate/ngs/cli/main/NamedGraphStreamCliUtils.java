package org.aksw.sparql_integrate.ngs.cli.main;

import java.util.Collection;
import java.util.List;

import org.aksw.jena_sparql_api.rx.RDFDataMgrEx;
import org.aksw.jena_sparql_api.rx.RDFDataMgrRx;
import org.aksw.jena_sparql_api.transform.result_set.QueryExecutionTransformResult;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.shared.PrefixMapping;

import io.reactivex.rxjava3.core.Flowable;

public class NamedGraphStreamCliUtils {

    /**
     * Open wrapper for the convention where STDIN can be referred to by the following means:
     * - no argument given
     * - single argument matching '-'
     *
     * @param args
     * @param probeLangs
     * @return
     */
    public static TypedInputStream open(List<String> args, Collection<Lang> probeLangs) {
        String src = args.isEmpty()
                ? "-"
                :args.get(0);

        TypedInputStream result = RDFDataMgrEx.open(src, probeLangs);
        return result;
    }

    /**
         * Default procedure to obtain a stream of named graphs from a
         * list of non-option arguments
         *
         * If the list is empty or the first argument is '-' data will be read from stdin
         * @param args
         */
        public static Flowable<Dataset> createNamedGraphStreamFromArgs(
                List<String> inOutArgs,
                String fmtHint,
                PrefixMapping pm) {
            //Consumer<RDFConnection> consumer = createProcessor(cm, pm);

    //		String src;
    //		if(inOutArgs.isEmpty()) {
    //			src = null;
    //		} else {
    //			String first = inOutArgs.get(0);
    //			src = first.equals("-") ? null : first;
    //		}
    //
    //		boolean useStdIn = src == null;
    //
    //		TypedInputStream tmp;
    //		if(useStdIn) {
    //			// Use the close shield to prevent closing stdin on .close()
    //			tmp = new TypedInputStream(
    //					new CloseShieldInputStream(System.in),
    //					WebContent.contentTypeTriG);
    //		} else {
    //			tmp = Objects.requireNonNull(SparqlStmtUtils.openInputStream(src), "Could not create input stream from " + src);
    //		}

    //		Collection<Lang> quadLangs = Arrays.asList(Lang.TRIX, Lang.TRIG, Lang.NQUADS);
            TypedInputStream tmp = open(inOutArgs, MainCliNamedGraphStream.quadLangs);
    //		logger.info("Probing for content type - this process may cause some exceptions to be shown");
    //		TypedInputStream tmp = probeLang(in, quadLangs);
            MainCliNamedGraphStream.logger.info("Detected format: " + tmp.getContentType());

            Flowable<Dataset> result = RDFDataMgrRx.createFlowableDatasets(() ->
                //MainCliSparqlIntegrate.prependWithPrefixes(tmp, pm))
                tmp)
                // TODO Decoding of distinguished names should go into the util method
                .map(ds -> QueryExecutionTransformResult.applyNodeTransform(RDFDataMgrRx::decodeDistinguished, ds));

            return result;
        }

}
