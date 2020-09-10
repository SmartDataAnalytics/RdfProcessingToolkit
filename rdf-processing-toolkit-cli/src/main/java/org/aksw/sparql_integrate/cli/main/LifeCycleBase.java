package org.aksw.sparql_integrate.cli.main;

/**
 * Basic implementation of a LifeCycle that checks for state violations
 *
 * @author raven
 *
 */
public abstract class LifeCycleBase
    implements LifeCycle
{
    public enum State {
        NEW,
        STARTED,
        FINISHED
    }

    protected State state = State.NEW;

    /**
     * Call this method in your own of a derived class to ensure the correct state
     *
     */
    protected void expectStarted() {
        if (!State.STARTED.equals(state)) {
            throw new IllegalStateException("expected state to be STARTED; was: " + state);
        }
    }

    @Override
    public final void start() {
        if (!State.NEW.equals(state)) {
            throw new IllegalStateException("start() may only be called once when in state NEW; was: " + state);
        }
        state = State.STARTED;

        startActual();
    }

    @Override
    public final void finish() {
        if (!State.STARTED.equals(state)) {
            throw new IllegalStateException("finish() may only be called once when in state STARTED; was" + state);
        }
        state = State.FINISHED;

        finishActual();
    }


    protected abstract void startActual();
    protected abstract void finishActual();
}