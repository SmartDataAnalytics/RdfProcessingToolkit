package org.aksw.sparql_integrate.cli.main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aksw.jena_sparql_api.common.DefaultPrefixes;
import org.aksw.jena_sparql_api.json.RdfJsonUtils;
import org.aksw.jena_sparql_api.rx.DatasetFactoryEx;
import org.aksw.jena_sparql_api.rx.RDFLanguagesEx;
import org.aksw.jena_sparql_api.sparql.ext.fs.JenaExtensionFs;
import org.aksw.jena_sparql_api.sparql.ext.http.JenaExtensionHttp;
import org.aksw.jena_sparql_api.sparql.ext.util.JenaExtensionUtil;
import org.aksw.jena_sparql_api.stmt.SPARQLResultEx;
import org.aksw.jena_sparql_api.stmt.SparqlStmt;
import org.aksw.jena_sparql_api.stmt.SparqlStmtUtils;
import org.aksw.jena_sparql_api.utils.GraphUtils;
import org.aksw.jena_sparql_api.utils.PrefixUtils;
import org.aksw.named_graph_stream.cli.main.MainCliNamedGraphStream;
import org.aksw.sparql_integrate.cli.SparqlScriptProcessor;
import org.aksw.sparql_integrate.cli.SparqlScriptProcessor.Provenance;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain.OutputSpec;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.ext.com.google.common.base.Stopwatch;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.geosparql.configuration.GeoSPARQLConfig;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.lang.SinkQuadsToDataset;
import org.apache.jena.riot.out.SinkQuadOutput;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.riot.resultset.ResultSetWriterRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.resultset.SPARQLResult;
import org.apache.jena.sys.JenaSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.shaded.com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Depending on the arguments the determined main output type is among this enum.
 * See also the detectOutputMode method.
 *
 * @author raven
 *
 */
enum OutputMode
{
    UNKOWN,
    QUAD,
    BINDING,
    JSON
}

interface SPARQLResultVisitor<T> {
    T onBooleanResult(Boolean value);
    T onResultSet(ResultSet it);
    T onJson(Iterator<JsonObject> it);

    default T forward(SPARQLResult sr) {
        T result;
        if (sr.isResultSet()) {
            result = onResultSet(sr.getResultSet());
        } else if (sr.isBoolean()) {
            result = onBooleanResult(sr.getBooleanResult());
        } else {
            throw new IllegalArgumentException("Unknow case " + sr);
        }

        return result;
    }
}

abstract class SPARQLResultVisitorFowarding<T>
    implements SPARQLResultVisitor<T>
{
    protected abstract SPARQLResultVisitor<T> getDelegate();

    @Override
    public T onBooleanResult(Boolean value) {
        return getDelegate().onBooleanResult(value);
    }

    @Override
    public T onResultSet(ResultSet it) {
        return getDelegate().onResultSet(it);
    }

    @Override
    public T onJson(Iterator<JsonObject> it) {
        return getDelegate().onJson(it);
    }

}

abstract class SPARQLResultExVisitorFowarding<T>
    extends SPARQLResultVisitorFowarding<T>
    implements SPARQLResultExVisitor<T>
{
    @Override
    protected abstract SPARQLResultExVisitor<T> getDelegate();

    @Override
    public T onQuads(Iterator<Quad> it) {
        return getDelegate().onQuads(it);
    }

    @Override
    public T onTriples(Iterator<Triple> it) {
        return getDelegate().onTriples(it);
    }
}

interface SPARQLResultExVisitor<T>
    extends SPARQLResultVisitor<T> {

    T onQuads(Iterator<Quad> it);
    T onTriples(Iterator<Triple> it);

    default T forwardEx(SPARQLResultEx sr) {
        T result;

        if (sr.isTriples()) {
            result = onTriples(sr.getTriples());
        } else if (sr.isQuads()) {
            result = onQuads(sr.getQuads());
        } else if (sr.isUpdateType()) {
            // nothing to do
            result = null;
        } else {
            result = forward(sr);
        }

        return result;
    }
}


interface SinkStreaming<T>
    extends Sink<T>, LifeCycle
{
    void start();
    void finish();
}

abstract class LifeCycleBase
    implements LifeCycle
{
    public enum State {
        NEW,
        STARTED,
        FINISHED
    }

    protected State state = State.NEW;

    protected void expectStarted() {
        if (State.STARTED.equals(state)) {
            throw new IllegalStateException("expected state to be STARTED; was: " + state);
        }
    }

    @Override
    public void start() {
        if (!State.NEW.equals(state)) {
            throw new IllegalStateException("start() may only be called in state NEW; was: " + state);
        }
        state = State.STARTED;

        startActual();
    }

    @Override
    public void finish() {
        if (!State.STARTED.equals(state)) {
            throw new IllegalStateException("finish() may only be called in state STARTED; was" + state);
        }
        state = State.FINISHED;

        finishActual();
    }


    protected abstract void startActual();
    protected abstract void finishActual();
}

abstract class SinkStreamingBase<T>
    extends LifeCycleBase
    implements SinkStreaming<T>
{
    public void send(T item) {
        if (!State.STARTED.equals(state)) {
            throw new IllegalStateException("send() may only be called in state STARTED; was" + state);
        }

        sendActual(item);
    }

    protected void startActual() {};
    protected void finishActual() {};

    protected abstract void sendActual(T item);
}

abstract class SinkStreamingWrapper<T>
    extends SinkStreamingBase<T> {

    protected abstract Sink<T> getDelegate();

    @Override
    public void flush() {
        getDelegate().flush();
    }

    @Override
    public void close() {
        getDelegate().close();
    }

    @Override
    protected void sendActual(T item) {
        getDelegate().send(item);
    }

    public static <T> SinkStreaming<T> wrap(Sink<T> delegate) {
        return new SinkStreamingWrapper<T>() {
            @Override
            protected Sink<T> getDelegate() {
                return delegate;
            }
        };
    }
}

class SinkJsonOutput
    implements Sink<JsonObject>
{
    protected OutputStream out;

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public void send(JsonObject item) {
        // TODO Auto-generated method stub

    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

}

class SinkStreamingToSink

{

}

interface LifeCycle {
    void start();
    void finish();
}

//interface Output {
//	void flush();
//
//}

class SinkStreamingQuads
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

/**
 * Writes a json array of indefinite length on the given output stream.
 * Writes '[' on start, ']' on close and ', ' before every item except the first
 *
 * @author raven
 *
 */
class SinkStreamingJsonArray
   extends SinkStreamingBase<JsonElement>
{
    protected OutputStream out;
    protected PrintStream pout;

    protected boolean isFirstItem = true;

    public SinkStreamingJsonArray(OutputStream out) {
        super();
        this.out = out;
        this.pout = new PrintStream(out);
    }

    @Override
    public void flush() {
        IO.flush(out);
    }

    @Override
    public void close() {
        IO.flush(out);
    }

    @Override
    protected void startActual() {
        pout.print("[");
    }

    @Override
    protected void sendActual(JsonElement item) {
        if (isFirstItem) {
            isFirstItem = false;
        } else {
            pout.println(",");
        }

        pout.print(Objects.toString(item));
    }

    @Override
    protected void finishActual() {
        pout.println("]");
    }
}


class SPARQLResultExProcessor
    extends LifeCycleBase
    implements SPARQLResultExVisitor<Void>
{
    protected Gson gson = new Gson();

    /**
     * The sink for quad may output immediately or write to a dataset first
     */
    protected SinkStreaming<Quad> quadSink;
    // protected Sink<Binding> bindingSink;
    protected SinkStreaming<JsonElement> jsonSink;
    protected Consumer<ResultSet> resultSetHandler;

    public SPARQLResultExProcessor(
            SinkStreaming<Quad> quadSink,
            SinkStreaming<JsonElement> jsonSink,
            // HACK because a Sink<Binding> is much more difficult to get right (it needs to know the result set vars in advance)
            Consumer<ResultSet> resultSetHandler) {
        super();
        this.quadSink = quadSink;
        this.jsonSink = jsonSink;
        this.resultSetHandler = resultSetHandler;
    }


    public Sink<Quad> getQuadSink() {
        return quadSink;
    }


    public Sink<JsonElement> getJsonSink() {
        return jsonSink;
    }


    @Override
    public Void onBooleanResult(Boolean value) {
        throw new UnsupportedOperationException("Boolean results not supported");
    }


    @Override
    public Void onResultSet(ResultSet it) {
        resultSetHandler.accept(it);
        return null;
    }


    @Override
    public Void onJson(Iterator<JsonObject> it) {
        while (it.hasNext()) {
            JsonObject json = it.next();
            String jsonStr = json.toString();
            JsonElement el = gson.fromJson(jsonStr, JsonElement.class);

            jsonSink.send(el);
        }

        return null;
    }


    @Override
    public Void onQuads(Iterator<Quad> it) {
        while (it.hasNext()) {
            Quad quad = it.next();
            quadSink.send(quad);
        }

        return null;
    }


    @Override
    public Void onTriples(Iterator<Triple> it) {
        return onQuads(Iterators.transform(it, t -> new Quad(Quad.defaultGraphIRI, t)));
    }


    public static SPARQLResultExProcessor configureForOutputMode(
            OutputMode outputMode,
            OutputStream out,
            OutputStream err,
            PrefixMapping pm,
            RDFFormat outRdfFormat,
            Lang outLang,
            List<Var> resultSetVars
            ) {

        SPARQLResultExProcessor result;

        Supplier<Dataset> datasetSupp = () -> DatasetFactoryEx.createInsertOrderPreservingDataset();
        switch (outputMode) {
        case QUAD:
            Objects.requireNonNull(outRdfFormat);

            result = new SPARQLResultExProcessor(
                    SinkStreamingQuads.createSinkQuads(outRdfFormat, out, pm, datasetSupp),
                    new SinkStreamingJsonArray(err),
                    rs -> ResultSetMgr.write(err, rs, ResultSetLang.SPARQLResultSetText));
            break;
        case JSON:
            result = new SPARQLResultExProcessor(
                    SinkStreamingQuads.createSinkQuads(RDFFormat.TRIG_PRETTY, err, pm, datasetSupp),
                    new SinkStreamingJsonArray(out),
                    rs -> ResultSetMgr.write(err, rs, ResultSetLang.SPARQLResultSetText));
            break;
        case BINDING:
            Objects.requireNonNull(outLang);

            result = new SPARQLResultExProcessor(
                    SinkStreamingQuads.createSinkQuads(RDFFormat.TRIG_PRETTY, err, pm, datasetSupp),
                    new SinkStreamingJsonArray(err),
                    rs -> ResultSetMgr.write(out, rs, outLang));
            break;
        default:
            throw new IllegalArgumentException("Unknown output mode: " + outputMode);
        };


        return result;
    }


    @Override
    protected void startActual() {
        quadSink.start();
        jsonSink.start();
//        bindingSink.start();
    }

    @Override
    protected void finishActual() {
        jsonSink.finish();
//    	binndingSink.finish();
        quadSink.finish();
    }
}


public class SparqlIntegrateCmdImpls {
    private static final Logger logger = LoggerFactory.getLogger(SparqlIntegrateCmdImpls.class);

    public static PrefixMapping configPrefixMapping(CmdSparqlIntegrateMain cmd) {
        PrefixMapping result = new PrefixMappingImpl();
        result.setNsPrefixes(DefaultPrefixes.prefixes);
        JenaExtensionUtil.addPrefixes(result);

        JenaExtensionHttp.addPrefixes(result);

        return result;
    }

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
        JenaExtensionFs.registerFileServiceHandler();
    }


    public static RDFConnection configConnection(CmdSparqlIntegrateMain cmd) {
        RDFConnection result = RDFConnectionFactory.connect(DatasetFactory.create());
        return result;
//    	Dataset ds = DatasetFactory.create;
//    	RDFConnection conn =
//
//        try(RDFConnection actualConn = RDFConnectionFactoryEx.wrapWithContext(
//                RDFConnectionFactoryEx.wrapWithQueryParser(RDFConnectionFactory.connect(dataset),
//                str -> {
//                    SparqlStmt stmt = sparqlParser.apply(str);
//                    SparqlStmt r = SparqlStmtUtils.applyNodeTransform(stmt, x -> NodeUtils.substWithLookup(x, System::getenv));
//                    return r;
//                }))) {

    }



    /**
     * If the last query is a json query then the mode is json.
     * If there is a construct query the mode is quads.
     * If there is no construct query but a select one, the mode is bindings.
     *
     */
    public static OutputMode detectOutputMode(List<SparqlStmt> stmts) {
        OutputMode result = null;
        if (stmts.isEmpty()) {
            result = OutputMode.QUAD;
        } else {
            SparqlStmt last = stmts.get(stmts.size() - 1);
            if (last.isQuery()) {
                Query q = last.getQuery();
                if (q.isJsonType()) {
                    result = OutputMode.JSON;
                }
            }


            int quadCount = 0;
            int bindingCount = 0;
            if (result == null) {
                for (SparqlStmt stmt : stmts) {
                    if (stmt.isQuery()) {
                        Query q = stmt.getQuery();

                        if (q.isConstructType()) {
                            ++quadCount;
                        } else if (q.isSelectType()) {
                            ++bindingCount;
                        }
                    }
                }

                if (quadCount != 0) {
                    result = OutputMode.QUAD;
                } else if (bindingCount != 0) {
                    result = OutputMode.BINDING;
                }
            }


            if(result == null) {
                result = OutputMode.QUAD;
            }
        }

        return result;
    }


    public static OutputMode determineOutputMode(Lang lang) {
        OutputMode result;
        if (RDFLanguages.isTriples(lang) || RDFLanguages.isQuads(lang)) {
            result = OutputMode.QUAD;
        } else if (ResultSetWriterRegistry.isRegistered(lang)) {
            result = OutputMode.BINDING;
        } else {
            //result = OutputMode.UNKOWN;
            result = null;
        }

        return result;
    }



    public static int sparqlIntegrate(CmdSparqlIntegrateMain cmd) throws Exception {
        configureGlobalSettings();

        Stopwatch sw = Stopwatch.createStarted();

        PrefixMapping prefixMapping = configPrefixMapping(cmd);

        SparqlScriptProcessor processor = SparqlScriptProcessor.create(prefixMapping);

        List<String> args = cmd.nonOptionArgs;

        // If an in/out file is given prepend it to the arguments
        Path outFile = null;
        String outFilename = null;
        Path tmpFile;
        if (cmd.outputSpec != null) {
            OutputSpec outputSpec = cmd.outputSpec;
            if (outputSpec.inOutFile != null) {
                outFilename = outputSpec.inOutFile;
                args.listIterator().add(outFilename);
            } else if (outputSpec.outFile != null) {
                outFilename = outputSpec.outFile;
            }
        }

        OutputStream operationalOut;
        if(!Strings.isNullOrEmpty(outFilename)) {
            outFile = Paths.get(outFilename).toAbsolutePath();
            if(Files.exists(outFile) && !Files.isWritable(outFile)) {
                throw new RuntimeException("Cannot write to specified output file: " + outFile.toAbsolutePath());
            }

            Path parent = outFile.getParent();
            String tmpName = "." + outFile.getFileName().toString() + ".tmp";
            tmpFile = parent.resolve(tmpName);

            operationalOut = Files.newOutputStream(tmpFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // Register a shutdown hook to delete the temporary file
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    operationalOut.close();
                    Files.deleteIfExists(tmpFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));

        } else {
            operationalOut = MainCliNamedGraphStream.out;
            outFile = null;
            tmpFile = null;
        }

        processor.process(args);

        List<Entry<SparqlStmt, Provenance>> workloads = processor.getSparqlStmts();

        List<SparqlStmt> stmts = workloads.stream().map(Entry::getKey).collect(Collectors.toList());

        List<Query> queries = stmts.stream().filter(SparqlStmt::isQuery).map(SparqlStmt::getQuery).collect(Collectors.toList());
        List<Query> selectQueries = queries.stream().filter(Query::isSelectType).collect(Collectors.toList());

        // Create the union of variables used in select queries
        Set<Var> selectVars = new LinkedHashSet<>();
        for(Query query : selectQueries) {
            List<Var> varContrib = query.getProjectVars();
            selectVars.addAll(varContrib);
        }


        boolean jqMode = cmd.jqDepth != null;
        int jqDepth = jqMode ? cmd.jqDepth : 3;
        boolean jqFlatMode = cmd.jqFlatMode;


        OutputMode outputMode;

        String outFormat = cmd.outFormat;

        RDFFormat outRdfFormat = null;
        Lang outLang = null;

        if (outFormat != null) {
            if ("json".equalsIgnoreCase(outFormat)) {
                outputMode = OutputMode.JSON;
            } else {

                try {
                    outRdfFormat = RDFLanguagesEx.findRdfFormat(outFormat);
                    outLang = outRdfFormat.getLang();
                } catch (Exception e) {
                    outLang = RDFLanguagesEx.findLang(outFormat);
                }

                outputMode = determineOutputMode(outLang);
            }
        } else {
            outputMode = jqMode ? OutputMode.JSON : detectOutputMode(stmts);

            switch (outputMode) {
            case BINDING:
                outLang = ResultSetLang.SPARQLResultSetJSON;
                break;
            case QUAD:
                outRdfFormat = RDFFormat.TRIG_BLOCKS;
                outLang = outRdfFormat.getLang();
                break;
            case JSON:
                // Nothing to do
                break;
            default:
                throw new IllegalStateException("Unkwon outputMode");
            }

        }





        // RDFLanguagesEx.findRdfFormat(cmd.outFormat, probeFormats)

        SPARQLResultExProcessor handler = SPARQLResultExProcessor.configureForOutputMode(
                outputMode,
                MainCliNamedGraphStream.out,
                System.err,
                prefixMapping,
                outRdfFormat,
                outLang,
                new ArrayList<>(selectVars));

        handler.start();

        SPARQLResultExVisitor<Void> effectiveHandler;
        if (jqMode) {
            effectiveHandler = new SPARQLResultExVisitorFowarding<Void>() {
                @Override
                protected SPARQLResultExVisitor<Void> getDelegate() {
                    return handler;
                }

                @Override
                public Void onResultSet(ResultSet it) {
                    while (it.hasNext()) {
                        QuerySolution qs = it.next();
                        JsonElement json = RdfJsonUtils.toJson(qs, jqDepth, jqFlatMode);
                        handler.getJsonSink().send(json);
                    }
                    return null;
                }
            };
        } else {
            effectiveHandler = handler;
        }


        try(RDFConnection conn = configConnection(cmd)) {
            for (Entry<SparqlStmt, Provenance> workload : workloads) {
                SparqlStmt stmt = workload.getKey();
                Provenance prov = workload.getValue();
                logger.info("Processing " + prov);
                try(SPARQLResultEx sr = SparqlStmtUtils.execAny(conn, stmt)) {
                    effectiveHandler.forwardEx(sr);
                }
            }
        }

        handler.finish();

        operationalOut.flush();

        if(outFile != null) {
            operationalOut.close();
            Files.move(tmpFile, outFile, StandardCopyOption.REPLACE_EXISTING);
        }

        logger.info("SPARQL overall execution finished after " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms");

//        if (cmd.startServer) {
//            SparqlService sparqlService = FluentSparqlService.from(actualConn).create();
//
//            Function<String, SparqlStmt> sparqlStmtParser = SparqlStmtParserImpl.create(Syntax.syntaxSPARQL_11,
//                    globalPrefixes, false);// .getQueryParser();
//
//            int port = 7532;
//            Server server = FactoryBeanSparqlServer.newInstance()
//                    .setSparqlServiceFactory((serviceUri, datasetDescription, httpClient) -> sparqlService)
//                    .setSparqlStmtParser(sparqlStmtParser).setPort(port).create();
//
//            server.start();
//
//            URI browseUri = new URI("http://localhost:" + port + "/sparql");
//            if (Desktop.isDesktopSupported()) {
//                Desktop.getDesktop().browse(browseUri);
//            } else {
//                System.err.println("SPARQL service with in-memory result dataset running at " + browseUri);
//            }
//
//            server.join();
//        }


//        String outFilename = null;
//        String ioFilename = null;
//
//        Path outFile;
//        Path tmpFile;
//        OutputStream operationalOut;
//        if(!Strings.isNullOrEmpty(outFilename)) {
//            outFile = Paths.get(outFilename).toAbsolutePath();
//            if(Files.exists(outFile) && !Files.isWritable(outFile)) {
//                throw new RuntimeException("Cannot write to specified output file: " + outFile.toAbsolutePath());
//            }
//
//            Path parent = outFile.getParent();
//            String tmpName = "." + outFile.getFileName().toString() + ".tmp";
//            tmpFile = parent.resolve(tmpName);
//
//            operationalOut = Files.newOutputStream(tmpFile,
//                    StandardOpenOption.CREATE,
//                    StandardOpenOption.WRITE,
//                    StandardOpenOption.TRUNCATE_EXISTING);
//
//            // Register a shutdown hook to delete the temporary file
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                try {
//                    operationalOut.close();
//                    Files.deleteIfExists(tmpFile);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }));
//
//        } else {
//            operationalOut = new CloseShieldOutputStream(new FileOutputStream(FileDescriptor.out)); //System.out;
//            outFile = null;
//            tmpFile = null;
//        }
//


        return 0;
    }
}
