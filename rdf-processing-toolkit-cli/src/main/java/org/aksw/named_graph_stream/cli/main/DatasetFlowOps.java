package org.aksw.named_graph_stream.cli.main;

public class DatasetFlowOps {

    /**
     * Apply a mapper based on RDFConnection on Datasets
     *
     * @param mapper
     * @return
     */
//    public static <I extends Dataset, O> FlowableTransformer<I, O> datasetToConnection(Function<RDFConnection, Flowable<O>> mapper) {
//        return upstream -> {
//            return upstream.flatMap(ds -> {
//                return Flowable.using(
//                        () -> RDFConnectionFactory.connect(ds),
//                        mapper::apply,
//                        RDFConnection::close);
//            });
//        };
//    }



}
