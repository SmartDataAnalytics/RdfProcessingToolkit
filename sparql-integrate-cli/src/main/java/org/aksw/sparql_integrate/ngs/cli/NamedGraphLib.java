package org.aksw.sparql_integrate.ngs.cli;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;

import io.reactivex.Flowable;

public class NamedGraphLib {
	public Flowable<Dataset> filter(Flowable<Dataset> stream, Query filter) {
		return null;
	}
}
