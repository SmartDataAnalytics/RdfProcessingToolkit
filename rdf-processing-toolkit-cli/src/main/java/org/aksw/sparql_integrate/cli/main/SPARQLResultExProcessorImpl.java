package org.aksw.sparql_integrate.cli.main;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.aksw.jena_sparql_api.rx.DatasetFactoryEx;
import org.aksw.jena_sparql_api.stmt.SPARQLResultEx;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

import com.github.jsonldjava.shaded.com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class SPARQLResultExProcessorImpl
    extends SinkStreamingBase<SPARQLResultEx>
    implements SPARQLResultExProcessor
{
    protected Gson gson = new Gson();

    /**
     * The sink for quad may output immediately or write to a dataset first
     * Prefixes are preconfigured on the sink so no need for StreamRDF
     */
    protected SinkStreaming<Quad> quadSink;
    protected SinkStreaming<JsonElement> jsonSink;
    protected SinkStreaming<Binding> bindingSink;
    protected Closeable closeAction;

    public SPARQLResultExProcessorImpl(
            SinkStreaming<Quad> quadSink,
            SinkStreaming<JsonElement> jsonSink,
            SinkStreaming<Binding> bindingSink,
            Closeable closeAction) {
        super();
        this.quadSink = quadSink;
        this.jsonSink = jsonSink;
        this.bindingSink = bindingSink;
        this.closeAction = closeAction;
    }


    public Sink<Quad> getQuadSink() {
        return quadSink;
    }

    public SinkStreaming<Binding> getBindingSink() {
        return bindingSink;
    }

    public Sink<JsonElement> getJsonSink() {
        return jsonSink;
    }


    @Override
    public Void onBooleanResult(Boolean value) {
        throw new UnsupportedOperationException("Boolean results not supported");
    }


    @Override
    public Void onResultSet(ResultSet rs) {
        while (rs.hasNext()) {
            Binding binding = rs.nextBinding();
            bindingSink.send(binding);
        }

        return null;
    }


    @Override
    public Void onJsonItems(Iterator<JsonObject> it) {
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


    @Override
    protected void startActual() {
        quadSink.start();
        bindingSink.start();
        jsonSink.start();
    }

    @Override
    protected void finishActual() {
        jsonSink.finish();
        bindingSink.finish();
        quadSink.finish();
    }

    @Override
    protected void sendActual(SPARQLResultEx item) {
        forward(item);
    }

    @Override
    public void flush() {
        quadSink.flush();
        bindingSink.flush();
        jsonSink.flush();
    }


    @Override
    public void close() {
        quadSink.close();
        bindingSink.close();
        jsonSink.close();
        if (closeAction != null) {
            try {
                closeAction.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Configure a SPARQLResultExProcessor to delegate
     * JSON, triples/quads and bindings to the appropriate target.
     * 
     * TODO Wrap as a builder
     * @param outputMode
     * @param out
     * @param err
     * @param pm
     * @param outRdfFormat
     * @param deferCount Number of items to analyze for used prefixes before writing them out
     * @param outLang
     * @param resultSetVars
     * @param closeAction
     * @return
     */
    public static SPARQLResultExProcessorImpl configureForOutputMode(
            OutputMode outputMode,
            OutputStream out,
            OutputStream err,
            PrefixMapping pm,
            RDFFormat outRdfFormat,
            long deferCount,
            Lang outLang,
            List<Var> resultSetVars,
            Closeable closeAction
            ) {

        SPARQLResultExProcessorImpl result;

        Supplier<Dataset> datasetSupp = () -> DatasetFactoryEx.createInsertOrderPreservingDataset();
        switch (outputMode) {
        case TRIPLE:
        case QUAD:
            Objects.requireNonNull(outRdfFormat);

            result = new SPARQLResultExProcessorImpl(
                    SinkStreamingQuads.createSinkQuads(outRdfFormat, out, pm, deferCount, datasetSupp),
                    new SinkStreamingJsonArray(err, false),
                    new SinkStreamingAdapter<>(),
                    closeAction) { //new SinkStreamingBinding(err, resultSetVars, ResultSetLang.SPARQLResultSetText)) {
                @Override
                public Void onResultSet(ResultSet rs) {
                    ResultSetMgr.write(err, rs, ResultSetLang.SPARQLResultSetText);
                    return null;
                }
            };

            break;
        case JSON:
            result = new SPARQLResultExProcessorImpl(
                    SinkStreamingQuads.createSinkQuads(RDFFormat.TRIG_BLOCKS, err, pm, 0, datasetSupp),
                    new SinkStreamingJsonArray(out),
                    //new SinkStreamingBinding(err, resultSetVars, ResultSetLang.SPARQLResultSetText));
                    new SinkStreamingAdapter<>(),
                    closeAction) {
                @Override
                public Void onResultSet(ResultSet rs) {
                    ResultSetMgr.write(err, rs, ResultSetLang.SPARQLResultSetText);
                    return null;
                }
            };
            break;
        case BINDING:
            Objects.requireNonNull(outLang);

            result = new SPARQLResultExProcessorImpl(
                    SinkStreamingQuads.createSinkQuads(RDFFormat.TRIG_BLOCKS, err, pm, 0, datasetSupp),
                    new SinkStreamingJsonArray(err, false),
                    new SinkStreamingBinding(out, resultSetVars, outLang),
                    closeAction);
            break;
        default:
            throw new IllegalArgumentException("Unknown output mode: " + outputMode);
        };


        return result;
    }
}