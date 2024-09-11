package org.aksw.rdf_processing_toolkit.cli.cmd;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jenax.arq.service.vfs.ServiceExecutorFactoryRegistratorVfs;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.ARQ;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sys.JenaSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

public class CliUtils {
    private static final Logger logger = LoggerFactory.getLogger(CliUtils.class);

    public static void benchmark(String name, Consumer<String> msgReceiver, Runnable runnable) {
        Stopwatch sw = Stopwatch.createStarted();
        runnable.run();
        msgReceiver.accept(name + " timed at " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }

    public static void configureGlobalSettings() {
        // System.setProperty("jena.geosparql.skip", String.valueOf(true));
        RDFLanguages.init();

        // Disable creation of a derby.log file ; triggered by the GeoSPARQL module
        System.setProperty("derby.stream.error.field", "org.aksw.sparql_integrate.cli.DerbyUtil.DEV_NULL");

        // benchmark("jena-init", logger::info, () -> {
        JenaSystem.init();
        // });

        // With jena ~4.5.0 registering RS_Text deregisters ntriples because
        // they have the same content type (text/plain)
        RDFLanguages.register(ResultSetLang.RS_Text);
        RDFLanguages.register(Lang.NTRIPLES);


        // Init geosparql module
        // TODO Init of geosparql takes a while which is annoying during startup
        // GeoSPARQLConfig.setupNoIndex();

        // Retain blank node labels
        // Note, that it is not sufficient to enable only input or output bnode labels
        ARQ.enableBlankNodeResultLabels();

        // Jena (at least up to 3.11.0) handles pseudo iris for blank nodes on the parser level
        // {@link org.apache.jena.sparql.lang.ParserBase}
        // This means, that blank nodes in SERVICE clauses would not be passed on as such
        // FIXME check what is broken by NOT turning this off (n.b parsing <_:....> in sparql queries fails if False)
        //ARQ.setFalse(ARQ.constantBNodeLabels);

        JenaExtensionHttp.register(() -> HttpClientBuilder.create().build());

        // Extended SERVICE <> keyword implementation
        // FIXME SparqlIntegrate already has a conditional service handler - but sbs and ngs need it to
        registerFileServiceHandler();
    }

    // TODO In some cases we do not want join reordering / in other cases inline execution
    // completely fails
    // We need a flag + heuristic - e.g. disable reordering when SERVICE is involved
    public static void registerFileServiceHandler() {
        ServiceExecutorFactoryRegistratorVfs.register(ARQ.getContext());

//        QC.setFactory(ARQ.getContext(), execCxt -> {
////            execCxt.getContext().set(ARQ.stageGenerator, StageBuilder.executeInline);
//            // return new OpExecutorServiceOrFile(execCxt);
//            ServiceExecutorFactoryRegistratorVfs.register(execCxt.getContext());
//            return new OpExecutorWithCustomServiceExecutors(execCxt);
//        });
    }

    public static PrefixMapping configPrefixMapping(CmdSparqlIntegrateMain cmd) {
        PrefixMapping result = new PrefixMappingImpl();
        result.setNsPrefixes(DefaultPrefixes.get());
        JenaExtensionUtil.addPrefixes(result);

        JenaExtensionHttp.addPrefixes(result);

        return result;
    }
}
