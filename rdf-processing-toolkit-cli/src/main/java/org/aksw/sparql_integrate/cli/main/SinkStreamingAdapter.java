package org.aksw.sparql_integrate.cli.main;

public class SinkStreamingAdapter<T>
    extends SinkStreamingBase<T> {

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    protected void sendActual(T item) {
    }
}
