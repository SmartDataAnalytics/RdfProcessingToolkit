package org.aksw.sparql_integrate.cli.main;

public class SPARQLResultExProcessorForwarding<D extends SPARQLResultExProcessor>
    extends SPARQLResultExProcessorForwardingBase<D>
{
    protected D delegate;

    public SPARQLResultExProcessorForwarding(D delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    protected D getDelegate() {
        return delegate;
    }
}
