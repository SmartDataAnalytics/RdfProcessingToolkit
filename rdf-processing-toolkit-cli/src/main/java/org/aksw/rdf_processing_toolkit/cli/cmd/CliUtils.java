package org.aksw.rdf_processing_toolkit.cli.cmd;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.sparql.ext.fs.OpExecutorServiceOrFile;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.geosparql.configuration.GeoSPARQLConfig;
import org.apache.jena.query.ARQ;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.engine.main.QC;
import org.apache.jena.sparql.engine.main.StageBuilder;
import org.apache.jena.sys.JenaSystem;

public class CliUtils {

    public static void configureGlobalSettings() {
        JenaSystem.init();

        RDFLanguages.register(ResultSetLang.SPARQLResultSetText);

        // Disable creation of a derby.log file ; triggered by the GeoSPARQL module
        System.setProperty("derby.stream.error.field", "org.aksw.sparql_integrate.cli.DerbyUtil.DEV_NULL");

        // Init geosparql module
        GeoSPARQLConfig.setupNoIndex();

        // Retain blank node labels
        // Note, that it is not sufficient to enable only input or output bnode labels
        ARQ.enableBlankNodeResultLabels();

        // Jena (at least up to 3.11.0) handles pseudo iris for blank nodes on the parser level
        // {@link org.apache.jena.sparql.lang.ParserBase}
        // This means, that blank nodes in SERVICE clauses would not be passed on as such
        ARQ.setFalse(ARQ.constantBNodeLabels);

        JenaExtensionHttp.register(() -> HttpClientBuilder.create().build());

        // Extended SERVICE <> keyword implementation
        // FIXME We need to conditionally activate the handler...
        registerFileServiceHandler();
    }

    // TODO In some cases we do not want join reordering / in other cases inline execution
    // completely fails
    // We need a flag + heuristic - e.g. disable reordering when SERVICE is involved
    public static void registerFileServiceHandler() {
        QC.setFactory(ARQ.getContext(), execCxt -> {
            execCxt.getContext().set(ARQ.stageGenerator, StageBuilder.executeInline);
            return new OpExecutorServiceOrFile(execCxt);
        });
    }


    public static PrefixMapping configPrefixMapping(CmdSparqlIntegrateMain cmd) {
        PrefixMapping result = new PrefixMappingImpl();
        result.setNsPrefixes(DefaultPrefixes.prefixes);
        JenaExtensionUtil.addPrefixes(result);

        JenaExtensionHttp.addPrefixes(result);

        return result;
    }

}
