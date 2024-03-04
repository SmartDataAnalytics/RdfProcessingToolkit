package org.aksw.sparql_integrate.cli.main;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.aksw.jenax.arq.picocli.CmdMixinArq;

import com.google.common.base.StandardSystemProperty;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

public class SparqlIntegrateBuilder {

    // "SPARQL Engine. Supported: 'mem', 'tdb2', 'difs'
    protected String engine = "mem";

    // FileSystem URL against which to interpret --db-location (e.g. for webdav, leave empty for local fs).
    protected String dbFs = null;

    // --db-path is deprecated!
    // "Access location to the database; interpreted w.r.t. engine. May be an URL, directory or file.
    protected String dbPath = null;

    // Wrap a datasource's default loading strategy with a different one. Supported values: insert (materializes LOAD as INSERT DATA), sansa (parallel loader)
    protected String dbLoader = null;

    // TODO Should require --server
    // "Disable SPARQL update on the server")
    protected boolean readOnlyMode = false;

    // Set property that can be accessed using the SPARQL function sys:getenv(key).
    protected Map<String, String> env;

    /* Caching Options */

    // An ID for the initial dataset in the configured engine (before applying any updates). Used for cache lookups (if enabled).
    protected String datasetId = null;

    // description="Cache engine. Supported: 'none', 'mem', 'disk'
    protected String cacheEngine = null;

    // Cache location; if provided then engine defaults to 'disk'
    protected String cachePath = null;

    // Cache GROUP BY operations individually. Ignored if no cache engine is specified.
    protected boolean cacheRewriteGroupBy = false;

    // Temporary directory
    protected String tempPath = StandardSystemProperty.JAVA_IO_TMPDIR.value();

    // Keep generated database files
    protected boolean dbKeep = false;

    // Set an engine option
    protected Map<String, String> dbOptions = new LinkedHashMap<>();;

    // Remote result size limit, ignored for local engines; defaults to ${DEFAULT-VALUE}", default@Option(names = { "--db-max-result-size" }
    protected Long dbMaxResultSize = null;

    // "Create corresponding output files for each file argument with SPARQL queries")
    protected String splitFolder = null;

    // @Mixin
    protected CmdMixinArq arqConfig = new CmdMixinArq();

    @Option(names= {"--bnp", "--bnode-profile"}, description="Blank node profile, empty string ('') to disable; 'auto' to autodetect, defaults to ${DEFAULT-VALUE}", defaultValue = "")
    protected String bnodeProfile = null;

    // "Delay query execution. Simulates 'hanging' connections.", defaultValue = "", converter = ConverterDuration.class)
    protected Duration delay = Duration.ZERO;

    /**
     * sparql-pattern file
     *
     */
    @Option(names = { "-a", "--algebra" }, description = "Show Algebra")
    protected boolean showAlgebra = false;
    // public long numRecords = 10;

    @Option(names = { "-u" }, description = "Union default graph mode; best effort that virtually exposes all named graphs as the default graph")
    protected boolean unionDefaultGraph = false;

    // TODO Make port configurable
    @Option(names = { "--server" }, description = "Start a SPARQL server")
    protected boolean server = false;

    @Option(names = { "--unsafe" }, description = "Enable features that could pose security risks (e.g. reading file:// URLs) in server mode. default: ${DEFAULT-VALUE}", defaultValue = "false")
    protected boolean unsafe = false;

    @Option(names = { "--port" }, description = "Server port, default: ${DEFAULT-VALUE}", defaultValue = "8642")
    protected int serverPort;

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    protected OutputSpec outputSpec;

    @Option(names = { "--iriasgiven" }, arity="0", description = "Use an alternative IRI() implementation that is non-validating but fast")
    protected boolean useIriAsGiven = false;

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
    protected long usedPrefixDefer;


    /**
     * If not given, the output mode (quads/bindings/json) is chosen from the remaining arguments and
     * the outFormat becomes the default format of that mode
     *
     * If given, the output mode is determined by the argument
     *
     */
    @Option(names = { "--out-format", "--of" }, description = "Output format")
    protected String outFormat = null;

    @Option(names = { "--out-mkdirs" }, description = "Create directories to the output file as needed.")
    protected boolean outMkDirs = false;

    // Subsume jq stuff under -w jq ?

    /**
     * jq mode transforms result sets into a lossy json representation by expanding its mentioned resources up to a given depth
     * this is convenient to process in bash pipes
     *
     */
    // @Option(names = { "--jq" }, parameterConsumer = ConsumeDepthValue.class, arity="0..1", fallbackValue = "3", description = "Enable jq mode")
    protected Integer jqDepth = null;

    /**
     *
     *
     */
    @Option(names = { "--flat" }, description = "Suppress JSON arrays for single valued properties")
    protected boolean jqFlatMode = false;

    @Option(names = { "--macro" }, description = "RDF file or URL with macro definitions")
    protected List<String> macroSources = new ArrayList<>();

    @Option(names= {"--macro-profile"}, description="Macro profile. 'auto' to auto-detect.") //, defaults to: '${DEFAULT-VALUE}'", defaultValue = "")
    protected Set<String> macroProfiles = new LinkedHashSet<>();


    @Option(names = { "--graphql-autoconf" }, description = "Query SPARQL endpoint for VoID and SHACL metadata on first request to map an unqualified field",
            negatable = true, defaultValue = "true", fallbackValue = "true")
    protected boolean graphQlAutoConfigure;

    @Option(names = { "--polyfill-lateral" }, description = "Polyfill LATERAL by evaluating it on the client (may transmit large volumes of data).")
    protected boolean polyfillLateral;



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

    // @Parameters(arity = "0..*", description = "File names with RDF/SPARQL content and/or SPARQL statements")
    public List<String> nonOptionArgs = new ArrayList<>();

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getDbFs() {
        return dbFs;
    }

    public void setDbFs(String dbFs) {
        this.dbFs = dbFs;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getDbLoader() {
        return dbLoader;
    }

    public void setDbLoader(String dbLoader) {
        this.dbLoader = dbLoader;
    }

    public boolean isReadOnlyMode() {
        return readOnlyMode;
    }

    public void setReadOnlyMode(boolean readOnlyMode) {
        this.readOnlyMode = readOnlyMode;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getCacheEngine() {
        return cacheEngine;
    }

    public void setCacheEngine(String cacheEngine) {
        this.cacheEngine = cacheEngine;
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    public boolean isCacheRewriteGroupBy() {
        return cacheRewriteGroupBy;
    }

    public void setCacheRewriteGroupBy(boolean cacheRewriteGroupBy) {
        this.cacheRewriteGroupBy = cacheRewriteGroupBy;
    }

    public String getTempPath() {
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    public boolean isDbKeep() {
        return dbKeep;
    }

    public void setDbKeep(boolean dbKeep) {
        this.dbKeep = dbKeep;
    }

    public Map<String, String> getDbOptions() {
        return dbOptions;
    }

    public void setDbOptions(Map<String, String> dbOptions) {
        this.dbOptions = dbOptions;
    }

    public Long getDbMaxResultSize() {
        return dbMaxResultSize;
    }

    public void setDbMaxResultSize(Long dbMaxResultSize) {
        this.dbMaxResultSize = dbMaxResultSize;
    }

    public String getSplitFolder() {
        return splitFolder;
    }

    public void setSplitFolder(String splitFolder) {
        this.splitFolder = splitFolder;
    }

    public CmdMixinArq getArqConfig() {
        return arqConfig;
    }

    public void setArqConfig(CmdMixinArq arqConfig) {
        this.arqConfig = arqConfig;
    }

    public String getBnodeProfile() {
        return bnodeProfile;
    }

    public void setBnodeProfile(String bnodeProfile) {
        this.bnodeProfile = bnodeProfile;
    }

    public Duration getDelay() {
        return delay;
    }

    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    public boolean isShowAlgebra() {
        return showAlgebra;
    }

    public void setShowAlgebra(boolean showAlgebra) {
        this.showAlgebra = showAlgebra;
    }

    public boolean isUnionDefaultGraph() {
        return unionDefaultGraph;
    }

    public void setUnionDefaultGraph(boolean unionDefaultGraph) {
        this.unionDefaultGraph = unionDefaultGraph;
    }

    public boolean isServer() {
        return server;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    public boolean isUnsafe() {
        return unsafe;
    }

    public void setUnsafe(boolean unsafe) {
        this.unsafe = unsafe;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public OutputSpec getOutputSpec() {
        return outputSpec;
    }

    public void setOutputSpec(OutputSpec outputSpec) {
        this.outputSpec = outputSpec;
    }

    public boolean isUseIriAsGiven() {
        return useIriAsGiven;
    }

    public void setUseIriAsGiven(boolean useIriAsGiven) {
        this.useIriAsGiven = useIriAsGiven;
    }

    public long getUsedPrefixDefer() {
        return usedPrefixDefer;
    }

    public void setUsedPrefixDefer(long usedPrefixDefer) {
        this.usedPrefixDefer = usedPrefixDefer;
    }

    public String getOutFormat() {
        return outFormat;
    }

    public void setOutFormat(String outFormat) {
        this.outFormat = outFormat;
    }

    public boolean isOutMkDirs() {
        return outMkDirs;
    }

    public void setOutMkDirs(boolean outMkDirs) {
        this.outMkDirs = outMkDirs;
    }

    public Integer getJqDepth() {
        return jqDepth;
    }

    public void setJqDepth(Integer jqDepth) {
        this.jqDepth = jqDepth;
    }

    public boolean isJqFlatMode() {
        return jqFlatMode;
    }

    public void setJqFlatMode(boolean jqFlatMode) {
        this.jqFlatMode = jqFlatMode;
    }

    public List<String> getMacroSources() {
        return macroSources;
    }

    public void setMacroSources(List<String> macroSources) {
        this.macroSources = macroSources;
    }

    public Set<String> getMacroProfiles() {
        return macroProfiles;
    }

    public void setMacroProfiles(Set<String> macroProfiles) {
        this.macroProfiles = macroProfiles;
    }

    public boolean isGraphQlAutoConfigure() {
        return graphQlAutoConfigure;
    }

    public void setGraphQlAutoConfigure(boolean graphQlAutoConfigure) {
        this.graphQlAutoConfigure = graphQlAutoConfigure;
    }

    public boolean isPolyfillLateral() {
        return polyfillLateral;
    }

    public void setPolyfillLateral(boolean polyfillLateral) {
        this.polyfillLateral = polyfillLateral;
    }

    public List<String> getNonOptionArgs() {
        return nonOptionArgs;
    }

    public void setNonOptionArgs(List<String> nonOptionArgs) {
        this.nonOptionArgs = nonOptionArgs;
    }



}
