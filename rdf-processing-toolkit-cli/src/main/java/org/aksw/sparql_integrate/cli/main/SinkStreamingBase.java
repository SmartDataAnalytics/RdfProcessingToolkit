package org.aksw.sparql_integrate.cli.main;

public abstract class SinkStreamingBase<T>
    extends LifeCycleBase
    implements SinkStreaming<T>
{
    public final void send(T item) {
        if (!State.STARTED.equals(state)) {
            throw new IllegalStateException("send() may only be called in state STARTED; was" + state);
        }

        sendActual(item);
    }

    protected void startActual() {};
    protected void finishActual() {};

    protected abstract void sendActual(T item);
}