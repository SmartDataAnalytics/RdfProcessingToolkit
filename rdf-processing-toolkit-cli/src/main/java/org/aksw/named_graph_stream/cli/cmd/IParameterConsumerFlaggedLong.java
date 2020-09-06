package org.aksw.named_graph_stream.cli.cmd;

import java.util.AbstractMap.SimpleEntry;
import java.util.Stack;

import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;

public abstract class IParameterConsumerFlaggedLong implements IParameterConsumer {

    protected abstract String getFlag();

    @Override
    public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
        String flag = getFlag();

        String top = args.pop().trim();
        boolean hasFlag = top.startsWith(flag);

        if (hasFlag) {
            top = top.substring(1);
        }

        Long val;
        try {
            val = Long.parseLong(top);
        } catch(NumberFormatException e) {
            throw new RuntimeException(e);
        }

        argSpec.setValue(new SimpleEntry<>(hasFlag, val));
    }
}
