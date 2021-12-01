package org.aksw.sparql_integrate.cli.main;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RdfDataSourceFactoryRegistry {
    private static RdfDataSourceFactoryRegistry INSTANCE;

    public static RdfDataSourceFactoryRegistry get() {
        if (INSTANCE == null) {
            synchronized (RdfDataSourceFactoryRegistry.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RdfDataSourceFactoryRegistry();

                    addDefaults(INSTANCE);
                }
            }
        }
        return INSTANCE;
    }

    public static RdfDataSourceFactoryRegistry addDefaults(RdfDataSourceFactoryRegistry registry) {
        registry.put("mem", new RdfDataSourceFactoryMem());
        registry.put("tdb2", new RdfDataSourceFactoryTdb2());
        registry.put("remote", new RdfDataSourceFactoryRemote());
        registry.put("difs", new RdfDataSourceFactoryDifs());

        return registry;
    }


    protected Map<String, RdfDataSourceFactory> registry = new ConcurrentHashMap<>();


    public RdfDataSourceFactoryRegistry put(String name, RdfDataSourceFactory factory) {
        registry.put(name, factory);
        return this;
    }

    public RdfDataSourceFactory get(String name) {
        return registry.get(name);
    }
}
