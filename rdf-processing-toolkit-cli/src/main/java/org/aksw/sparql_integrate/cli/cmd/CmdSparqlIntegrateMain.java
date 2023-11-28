package org.aksw.sparql_integrate.cli.cmd;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;

import org.aksw.jenax.arq.picocli.CmdMixinArq;
import org.aksw.rdf_processing_toolkit.cli.cmd.CmdCommonBase;
import org.aksw.rdf_processing_toolkit.cli.cmd.VersionProviderRdfProcessingToolkit;
import org.aksw.sparql_integrate.cli.main.SparqlIntegrateCmdImpls;
import com.google.common.base.StandardSystemProperty;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "integrate",
    versionProvider = VersionProviderRdfProcessingToolkit.class,
    description = "Run sequences of SPARQL queries and stream triples, quads and bindings")
public class CmdSparqlIntegrateMain
    extends CmdCommonBase
    implements Callable<Integer>
{
//    @Option(names = { "-h", "--help" }, usageHelp = true)
//    public boolean help = false;
//
//    @Option(names = { "-v", "--version" }, versionHelp = true)
//    public boolean version = false;

    @Option(names = { "--db-engine", "-e" }, description="SPARQL Engine. Supported: 'mem', 'tdb2', 'difs'")
    public String engine = "mem";

    @Option(names = { "--db-fs", "--fs" }, description="FileSystem URL against which to interpret --db-location (e.g. for webdav, leave empty for local fs).")
    public String dbFs = null;

    // --db-path is deprecated!
    @Option(names = { "--db-loc", "--loc" }, description="Access location to the database; interpreted w.r.t. engine. May be an URL, directory or file.")
    public String dbPath = null;

    @Option(names = { "--db-loader" }, description="Wrap a datasource's default loading strategy with a different one. Supported values: sansa")
    public String dbLoader = null;

    // TODO Should require --server
    @Option(names = { "--read-only" },  description="Disable SPARQL update on the server")
    public boolean readOnlyMode = false;


    /* Caching Options */

    @Option(names = { "--dataset-id" }, description="An ID for the initial dataset in the configured engine (before applying any updates). Used for cache lookups (if enabled).")
    public String datasetId = null;

    @Option(names = { "--cache-engine" }, description="Cache engine. Supported: 'none', 'mem', 'disk'")
    public String cacheEngine = null;

    @Option(names = { "--cache-loc" }, description="Cache location; if provided then engine defaults to 'disk'")
    public String cachePath = null;

    @Option(names = { "--cache-rewrite-groupby" }, description="Cache GROUP BY operations individually. Ignored if no cache engine is specified.") //, defaultValue = "false", fallbackValue = "true")
    public boolean cacheRewriteGroupBy = false;



    @Option(names = { "--tmpdir" }, description="Temporary directory")
    public String tempPath = StandardSystemProperty.JAVA_IO_TMPDIR.value();

    @Option(names = { "--db-keep" }, description="Keep generated database files")
    public boolean dbKeep = false;

    @Option(names = { "--db-set" }, description="Set an engine option")
    public Map<String, String> dbOptions = new LinkedHashMap<>();

    @Option(names = { "--db-max-result-size" }, description="Remote result size limit, ignored for local engines; defaults to ${DEFAULT-VALUE}", defaultValue = "10000")
    public Long dbMaxResultSize = null;

    @Option(names = { "--split" }, description="Create corresponding output files for each file argument with SPARQL queries")
    public String splitFolder = null;

    @Mixin
    public CmdMixinArq arqConfig;

    @Option(names= {"--bnp", "--bnode-profile"}, description="Blank node profile, empty string ('') to disable; 'auto' to autodetect, defaults to ${DEFAULT-VALUE}", defaultValue = "")
    public String bnodeProfile = null;


    @Option(names= {"--delay"}, description="Delay query execution. Simulates 'hanging' connections.", defaultValue = "", converter = ConverterDuration.class)
    public Duration delay = Duration.ZERO;

//    @Option(names = { "--explain" }, description="Enable detailed ARQ log output")
//    public boolean explain = false;

//    @Option(names = { "--set" }, description="Set ARQ options (key=value)", mapFallbackValue="true")
//    public Map<String, String> arqOptions = new HashMap<>();
//
//    @Option(names = { "--rdf10" }, description = "RDF 1.0 mode; e.g. xsd:string on literals matter", defaultValue = "false")
//    public boolean useRdf10 = false;
//
//    @Option(names = { "--geoindex" },  description = "Build Geoindex")
//    public boolean geoindex;


//    @Option(names = { "-X" }, description = "Debug output such as full stacktraces")
//    public boolean debugMode = false;

    /**
     * sparql-pattern file
     *
     */
    @Option(names = { "-a", "--algebra" }, description = "Show Algebra")
    public boolean showAlgebra = false;
    // public long numRecords = 10;

    @Option(names = { "-u" }, description = "Union default graph mode; best effort that virtually exposes all named graphs as the default graph")
    public boolean unionDefaultGraph = false;

    // TODO Make port configurable
    @Option(names = { "--server" }, description = "Start a SPARQL server")
    public boolean server = false;

    @Option(names = { "--unsafe" }, description = "Enable features that could pose security risks (e.g. reading file:// URLs) in server mode. default: ${DEFAULT-VALUE}", defaultValue = "false")
    public boolean unsafe = false;

    @Option(names = { "--port" }, description = "Server port, default: ${DEFAULT-VALUE}", defaultValue = "8642")
    public int serverPort;

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    public OutputSpec outputSpec;

    @Option(names = { "--iriasgiven" }, arity="0", description = "Use an alternative IRI() implementation that is non-validating but fast")
    public boolean useIriAsGiven = false;

    public static class OutputSpec {
        /**
         * sparql-pattern file
         *
         */
        @Option(names = { "-o", "--out-file" }, description = "output file")
        public String outFile;

        @Option(names = { "--io", },  description = "overwrites argument file on success with output; use with care")
        public String inOutFile = null;
    }

    @Option(names = { "-d", "--used-prefixes" }, description = "Number of records (bindings/quads) by which to defer RDF output in order to analyze used prefixes; default: ${DEFAULT-VALUE}", defaultValue = "100")
    public long usedPrefixDefer;


    /**
     * If not given, the output mode (quads/bindings/json) is chosen from the remaining arguments and
     * the outFormat becomes the default format of that mode
     *
     * If given, the output mode is determined by the argument
     *
     */
    @Option(names = { "--out-format", "--of" }, description = "Output format")
    public String outFormat = null;

    // Subsume jq stuff under -w jq ?

    /**
     * jq mode transforms result sets into a lossy json representation by expanding its mentioned resources up to a given depth
     * this is convenient to process in bash pipes
     *
     */
    @Option(names = { "--jq" }, parameterConsumer = ConsumeDepthValue.class, arity="0..1", fallbackValue = "3", description = "Enable jq mode")
    public Integer jqDepth = null;

    /**
     *
     *
     */
    @Option(names = { "--flat" }, description = "Suppress JSON arrays for single valued properties")
    public boolean jqFlatMode = false;

    @Option(names = { "--macro" }, description = "RDF file or URL with macro definitions")
    public List<String> macroSources = new ArrayList<>();

    @Option(names= {"--macro-profile"}, description="Macro profile. 'auto' to auto-detect.") //, defaults to: '${DEFAULT-VALUE}'", defaultValue = "")
    public Set<String> macroProfiles = new LinkedHashSet<>();


    @Option(names = { "--graphql-autoconf" }, description = "Query SPARQL endpoint for VoID and SHACL metadata on first request to map an unqualified field",
            negatable = true, defaultValue = "true", fallbackValue = "true")
    public boolean graphQlAutoConfigure;

    /**
     * --jq may be followed by an integer - picocli seems to greedily parse any argument even if it is not an integer
     *
     * @author raven
     *
     */
    static class ConsumeDepthValue implements IParameterConsumer {
        @Override
        public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
            if (!args.isEmpty()) {
                String top = args.peek();
                Integer val;
                try {
                    val = Integer.parseInt(top);
                    args.pop();
                } catch(NumberFormatException e) {
                    val = 3;
                }

                argSpec.setValue(val);
            }
        }
    }

    @Parameters(arity = "0..*", description = "File names with RDF/SPARQL content and/or SPARQL statements")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        return SparqlIntegrateCmdImpls.sparqlIntegrate(this);
    }
}
