package org.aksw.sparql_integrate.cli.main;

public abstract class SinkStreamingBase<T>
    extends LifeCycleBase
    implements SinkStreaming<T>
{
    public final void send(T item) {
        expectStarted();
        sendActual(item);
    }

    protected void startActual() {};
    protected void finishActual() {};

    protected abstract void sendActual(T item);
}