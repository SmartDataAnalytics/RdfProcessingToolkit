package org.aksw.sparql_integrate.cli.main;

public interface RdfDataSourceSpecBasic {
    String getLocationContext();
    RdfDataSourceSpecBasic setLocationContext(String locationContext);

    String getLocation();
    RdfDataSourceSpecBasic setLocation(String location);

    String getTempDir();
    RdfDataSourceSpecBasic setTempDir(String location);

    /** If the db did not yet exist yet and had to be created, delete it after use? true = yes*/
    Boolean isAutoDeleteIfCreated();
    RdfDataSourceSpecBasic setAutoDeleteIfCreated(Boolean onOrOff);
}