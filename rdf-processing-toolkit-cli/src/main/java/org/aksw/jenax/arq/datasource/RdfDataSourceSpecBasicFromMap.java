package org.aksw.jenax.arq.datasource;

import java.util.LinkedHashMap;
import java.util.Map;

import org.aksw.commons.collections.MapUtils;

public class RdfDataSourceSpecBasicFromMap
    implements RdfDataSourceSpecBasic
{
    protected Map<String, Object> map;

    public RdfDataSourceSpecBasicFromMap(Map<String, Object> map) {
        super();
        this.map = map;
    }

    public static RdfDataSourceSpecBasicFromMap wrap(Map<String, Object> map) {
        return new RdfDataSourceSpecBasicFromMap(map);
    }

    public static RdfDataSourceSpecBasicFromMap create() {
        return wrap(new LinkedHashMap<>());
    }

    public Map<String, Object> getMap() {
        return map;
    }

    @Override
    public String getLocationContext() {
        return (String)map.get(RdfDataSourceSpecTerms.LOCATION_CONTEXT_KEY);
    }

    @Override
    public RdfDataSourceSpecBasic setLocationContext(String locationContext) {
        MapUtils.putWithRemoveOnNull(map, RdfDataSourceSpecTerms.LOCATION_CONTEXT_KEY, locationContext);
        return this;
    }

    @Override
    public String getLocation() {
        return (String)map.get(RdfDataSourceSpecTerms.LOCATION_KEY);
    }

    @Override
    public RdfDataSourceSpecBasic setLocation(String location) {
        MapUtils.putWithRemoveOnNull(map, RdfDataSourceSpecTerms.LOCATION_KEY, location);
        return this;
    }

    @Override
    public Boolean isAutoDeleteIfCreated() {
        return (Boolean)map.get(RdfDataSourceSpecTerms.AUTO_DELETE_IF_CREATED_KEY);
    }

    @Override
    public RdfDataSourceSpecBasic setAutoDeleteIfCreated(Boolean onOrOff) {
        MapUtils.putWithRemoveOnNull(map, RdfDataSourceSpecTerms.AUTO_DELETE_IF_CREATED_KEY, onOrOff);
        return this;
    }

    @Override
    public String getTempDir() {
        return (String)map.get(RdfDataSourceSpecTerms.TEMP_DIR_KEY);
    }

    @Override
    public RdfDataSourceSpecBasic setTempDir(String tempDir) {
        MapUtils.putWithRemoveOnNull(map, RdfDataSourceSpecTerms.TEMP_DIR_KEY, tempDir);
        return this;
    }
}