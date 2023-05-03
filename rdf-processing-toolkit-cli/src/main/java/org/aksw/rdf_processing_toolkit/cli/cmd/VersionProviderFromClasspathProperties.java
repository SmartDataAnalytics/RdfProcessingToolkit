package org.aksw.rdf_processing_toolkit.cli.cmd;

import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

import picocli.CommandLine.IVersionProvider;

/**
 * Implementation of picocli's {@link IVersionProvider} that reads the version string
 * from an entry of a properties file on the class path with a specific key.
 * 
 * @author raven
 *
 */
public abstract class VersionProviderFromClasspathProperties implements IVersionProvider {

    public abstract String getResourceName();
    public abstract Collection<String> getStrings(Properties properties);

    @Override
    public String[] getVersion() throws Exception {
        String resourceName = getResourceName();

        Properties properties = new Properties();
        try (InputStream in = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resourceName),
                "Resource not found: " + resourceName)) {
            properties.load(in);
        }

        String[] result = getStrings(properties).toArray(new String[0]);
        return result;
    }

}
