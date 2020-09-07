package org.aksw.sparql_integrate.cli.main;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.aksw.jena_sparql_api.rx.DatasetFactoryEx;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

import com.github.jsonldjava.shaded.com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class SPARQLResultExProcessor
    extends LifeCycleBase
    implements SPARQLResultExVisitor<Void>
{
    protected Gson gson = new Gson();

    /**
     * The sink for quad may output immediately or write to a dataset first
     * Prefixes are preconfigured on the sink so no need for StreamRDF
     */
    protected SinkStreaming<Quad> quadSink;
    // protected Sink<Binding> bindingSink;
    protected SinkStreaming<JsonElement> jsonSink;
    protected SinkStreaming<Binding> bindingSink;

    public SPARQLResultExProcessor(
            SinkStreaming<Quad> quadSink,
            SinkStreaming<JsonElement> jsonSink,
            // HACK because a Sink<Binding> is much more difficult to get right (it needs to know the result set vars in advance)
            SinkStreaming<Binding> bindingSink) {
        super();
        this.quadSink = quadSink;
        this.jsonSink = jsonSink;
        this.bindingSink = bindingSink;
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
    public Void onResultSet(ResultSet rs) {
        while (rs.hasNext()) {
            Binding binding = rs.nextBinding();
            bindingSink.send(binding);
        }

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
                    new SinkStreamingJsonArray(err, false),
                    new SinkStreamingBinding(err, resultSetVars, ResultSetLang.SPARQLResultSetText));
            break;
        case JSON:
            result = new SPARQLResultExProcessor(
                    SinkStreamingQuads.createSinkQuads(RDFFormat.TRIG_PRETTY, err, pm, datasetSupp),
                    new SinkStreamingJsonArray(out),
                    new SinkStreamingBinding(err, resultSetVars, ResultSetLang.SPARQLResultSetText));
            break;
        case BINDING:
            Objects.requireNonNull(outLang);

            result = new SPARQLResultExProcessor(
                    SinkStreamingQuads.createSinkQuads(RDFFormat.TRIG_PRETTY, err, pm, datasetSupp),
                    new SinkStreamingJsonArray(err, false),
                    new SinkStreamingBinding(out, resultSetVars, outLang));
            break;
        default:
            throw new IllegalArgumentException("Unknown output mode: " + outputMode);
        };


        return result;
    }
}