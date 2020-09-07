package org.aksw.sparql_integrate.cli.main;

import java.io.OutputStream;
import java.util.List;

import org.aksw.jena_sparql_api.rx.ResultSetRxImpl;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * A sink for writing out bindings as a result set using {@link ResultSetMgr}
 *
 * Internally uses a ResultSet that is backed by a Flowable&lt;Binding&gt;.
 * Calling send(binding) publishes to that flowable and thus makes a new binding available in the result set.
 *
 * As the ResultSet blocks until items become available the ResultSetWriter runs in a another thread
 *
 * Uses a {@link PublishSubject}
 *
 * @author raven
 *
 */
public class SinkStreamingBinding
    extends SinkStreamingBase<Binding>
{
    protected OutputStream out;
    protected List<Var> resultVars;
    protected Lang lang;

    protected PublishSubject<Binding> subject = null;
    protected Thread thread;

    public SinkStreamingBinding(OutputStream out, List<Var> resultVars, Lang lang) {
        super();
        this.out = out;
        this.resultVars = resultVars;
        this.lang = lang;
    }

    @Override
    public void flush() {
        IO.flush(out);
    }

    @Override
    public void close() {
        if (subject != null && !subject.hasComplete()) {
            subject.onError(new RuntimeException("Closing incomplete result set (finish() was not called)"));
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void startActual() {
        subject = PublishSubject.create();

        thread = new Thread(() -> {
            try(QueryExecution qe = new ResultSetRxImpl(resultVars, subject.toFlowable(BackpressureStrategy.BUFFER)).asQueryExecution()) {
                ResultSet resultSet = qe.execSelect();
                ResultSetMgr.write(out, resultSet, lang);
            }
        });
        thread.start();
    }

    @Override
    protected void sendActual(Binding item) {
        subject.onNext(item);
    }

    @Override
    public void finishActual() {
        subject.onComplete();
    }
}
