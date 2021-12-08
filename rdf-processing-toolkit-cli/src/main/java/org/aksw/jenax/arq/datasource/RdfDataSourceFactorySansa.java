package org.aksw.jenax.arq.datasource;

import java.util.Map;

import org.aksw.jenax.arq.connection.core.RDFLinkAdapterEx;
import org.aksw.jenax.connection.datasource.RdfDataSource;
import org.aksw.jenax.connection.datasource.RdfDataSourceDelegateBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdflink.LinkDatasetGraph;
import org.apache.jena.rdflink.RDFConnectionAdapter;
import org.apache.jena.rdflink.RDFLink;
import org.apache.jena.rdflink.RDFLinkModular;

import net.sansa_stack.spark.io.rdf.loader.LinkDatasetGraphSansa;

public class RdfDataSourceFactorySansa
//    implements RdfDataSourceWrapper
{
    public static Configuration createDefaultHadoopConfiguration() {
        Configuration conf = new Configuration(false);
        conf.set("fs.defaultFS", "file:///");
        return conf;
    }

    // @Override
    public RdfDataSource create(RdfDataSource dataSource, Map<String, Object> config) {
        // RdfDataSourceSpecBasic spec = RdfDataSourceSpecBasicFromMap.wrap(config);

        RdfDataSource result = new RdfDataSourceDelegateBase(dataSource) {
            @Override
            public org.apache.jena.rdfconnection.RDFConnection getConnection() {
                RDFConnection conn = dataSource.getConnection();
                RDFLink link = RDFLinkAdapterEx.adapt(conn);

                LinkDatasetGraph linkDg = LinkDatasetGraphSansa.create(createDefaultHadoopConfiguration(), () -> RDFLinkAdapterEx.adapt(dataSource.getConnection()));

                RDFConnection r = RDFConnectionAdapter.adapt(
                        new RDFLinkModular(link, link, linkDg));
                return r;
            }
        };

        return result;
    }

}
