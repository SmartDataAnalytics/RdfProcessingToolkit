package org.aksw.sparql_integrate.cli.cmd;

import java.time.Duration;

import org.aksw.commons.util.time.TimeAgo;

import picocli.CommandLine.ITypeConverter;

public class ConverterDuration implements ITypeConverter<Duration> {
    @Override
    public Duration convert(String value) throws Exception {
        Duration result = TimeAgo.parse(value);
        return result;
    }
}
