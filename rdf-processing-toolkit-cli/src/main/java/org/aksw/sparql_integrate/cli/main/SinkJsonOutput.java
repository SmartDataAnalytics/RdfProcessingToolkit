package org.aksw.sparql_integrate.cli.main;

import java.io.OutputStream;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.Sink;

public class SinkJsonOutput
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