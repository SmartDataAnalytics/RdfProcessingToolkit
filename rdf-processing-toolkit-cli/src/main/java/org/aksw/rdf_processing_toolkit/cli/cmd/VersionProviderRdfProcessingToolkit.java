package org.aksw.rdf_processing_toolkit.cli.cmd;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

public class VersionProviderRdfProcessingToolkit
    extends VersionProviderFromClasspathProperties
{
    @Override String getResourceName() { return "rdf-processing-toolkit.properties"; }
    @Override Collection<String> getStrings(Properties p) { return Arrays.asList(
            p.get("rdf-processing-toolkit.version") + " built at " + p.get("rdf-processing-toolkit.build.timestamp")
    ); }

}
