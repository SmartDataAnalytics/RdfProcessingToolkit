package org.aksw.named_graph_stream.cli.main;

import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;

public class DatasetFlowOps {

    /**
     * Apply a mapper based on RDFConnection on Datasets
     *
     * @param mapper
     * @return
     */
    public static <I extends Dataset, O> FlowableTransformer<I, O> datasetToConnection(Function<RDFConnection, Flowable<O>> mapper) {
        return upstream -> {
            return upstream.flatMap(ds -> {
                return Flowable.using(
                        () -> RDFConnectionFactory.connect(ds),
                        mapper::apply,
                        RDFConnection::close);
            });
        };
    }



}
