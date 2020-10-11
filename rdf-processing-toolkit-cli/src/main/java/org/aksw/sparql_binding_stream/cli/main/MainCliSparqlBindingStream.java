package org.aksw.sparql_binding_stream.cli.main;

import org.aksw.commons.util.exception.ExceptionUtilsAksw;
import org.aksw.rdf_processing_toolkit.cli.cmd.CliUtils;
import org.aksw.sparql_binding_stream.cli.cmd.CmdSbsMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

public class MainCliSparqlBindingStream {
    private static final Logger logger = LoggerFactory.getLogger(MainCliSparqlBindingStream.class);

    static { CliUtils.configureGlobalSettings(); }

    public static void main(String[] args) {
        int exitCode = mainCore(args);
        System.exit(exitCode);
    }

    public static int mainCore(String[] args) {
        int result = new CommandLine(new CmdSbsMain())
            .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                boolean debugMode = false;
                if (debugMode) {
                    ExceptionUtilsAksw.rethrowIfNotBrokenPipe(ex);
                } else {
                    ExceptionUtilsAksw.forwardRootCauseMessageUnless(ex, logger::error, ExceptionUtilsAksw::isBrokenPipeException);
                }
                return 0;
            })
            .execute(args);
        return result;
    }


//    public static void experiment(String[] args) {
//
//        Dataset ds = RDFDataMgr.loadDataset("/home/raven/Projects/Eclipse/rdf-processing-toolkit-parent/rdf-processing-toolkit-cli/src/test/resources/ngs-nato-phonetic-alphabet.trig");
//        try(RDFConnection conn = RDFConnectionFactory.connect(ds)) {
//            try(QueryExecution qe = conn.query("SELECT * { GRAPH ?g { ?s ?p ?o } }")) {
//                ResultSet rs = qe.execSelect();
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                Lang lang = ResultSetLang.SPARQLResultSetJSON;
//                ResultSetMgr.write(out, rs, lang);
//
//                System.out.println(new String(out.toByteArray(), StandardCharsets.UTF_8));
//
//                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//                TypedInputStream tis = RDFDataMgrEx.probeLang(in, Arrays.asList(lang));
//                System.out.println(tis.getMediaType());
//
//                ResultSet parsed = ResultSetMgr.read(tis.getInputStream(), RDFLanguages.contentTypeToLang(tis.getContentType()));
//                System.out.println(ResultSetFormatter.asText(parsed));
//
//                // RDFDataMgr.open(src)
//                //RDFDataMgrEx.open(src, probeLangs)
//            }
//        }
//
//    }
}
