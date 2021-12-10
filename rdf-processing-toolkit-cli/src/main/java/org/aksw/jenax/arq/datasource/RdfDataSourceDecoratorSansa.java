package org.aksw.jenax.arq.datasource;

import java.util.Map;

import org.aksw.jenax.arq.connection.core.RDFLinkAdapterEx;
import org.aksw.jenax.arq.connection.link.RDFLinkDelegateWithWorkerThread;
import org.aksw.jenax.arq.connection.link.RDFLinkUtils;
import org.aksw.jenax.connection.datasource.RdfDataSource;
import org.aksw.jenax.connection.datasource.RdfDataSourceDelegateBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdflink.LinkDatasetGraph;
import org.apache.jena.rdflink.RDFConnectionAdapter;
import org.apache.jena.rdflink.RDFLink;
import org.apache.jena.rdflink.RDFLinkModular;

import net.sansa_stack.spark.io.rdf.loader.LinkDatasetGraphSansa;

public class RdfDataSourceDecoratorSansa
{
    public static Configuration createDefaultHadoopConfiguration() {
        Configuration conf = new Configuration(false);
        conf.set("fs.defaultFS", "file:///");
        return conf;
    }

    // @Override
    public RdfDataSource decorate(RdfDataSource dataSource, Map<String, Object> config) {
        // RdfDataSourceSpecBasic spec = RdfDataSourceSpecBasicFromMap.wrap(config);

        RdfDataSource result = new RdfDataSourceDelegateBase(dataSource) {
            @Override
            public org.apache.jena.rdfconnection.RDFConnection getConnection() {
                RDFConnection rawConn = dataSource.getConnection();
                // RDFLink queryLink = RDFLinkAdapterEx.adapt(rawConn);


                // RDFConnection conn = RDFConnectionAdapter.adapt(RDFLinkDelegateWithWorkerThread.wrap(RDFLinkAdapterEx.adapt(connx)));

                // If true then the graphstore LOAD action may acquire multiple update connections for the INSERT requests
                // Multiple concurrent update transaction are prone to deadlocks

                RDFLink rawUpdateLink = RDFLinkAdapterEx.adapt(rawConn);

                boolean enforceSameThreadOnLink = false;
                RDFLink updateLink = enforceSameThreadOnLink
                        ? RDFLinkDelegateWithWorkerThread.wrap(rawUpdateLink)
                        : rawUpdateLink;


                boolean allowMultipleConnections = true;

                LinkDatasetGraph linkDg;
                if (allowMultipleConnections) {
                    linkDg = LinkDatasetGraphSansa.create(createDefaultHadoopConfiguration(), () -> RDFLinkAdapterEx.adapt(dataSource.getConnection()));
                } else {
                    linkDg = LinkDatasetGraphSansa.create(createDefaultHadoopConfiguration(), () -> new RDFLinkAdapterEx(RDFConnectionAdapter.adapt(updateLink)) {
                        @Override
                        public void close() {
                            // noop as we reuse the primary connection - the primary one has to be closed
                        }
                    });
                }

                RDFConnection r = RDFConnectionAdapter.adapt(
                        RDFLinkUtils.wrapWithLoadViaLinkDatasetGraph(new RDFLinkModular(updateLink, updateLink, linkDg)));
                return r;
            }
        };

        return result;
    }

}
