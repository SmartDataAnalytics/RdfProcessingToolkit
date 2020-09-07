package org.aksw.sparql_integrate.cli.main;

import java.util.Iterator;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;

public abstract class SPARQLResultExVisitorFowarding<T>
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