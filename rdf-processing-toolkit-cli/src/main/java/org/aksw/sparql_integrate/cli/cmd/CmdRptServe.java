package org.aksw.sparql_integrate.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.aksw.rdf_processing_toolkit.cli.cmd.CmdRptMain;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Unmatched;

@Command(name = "serve", description = "Alias for `integrate --server`")
public class CmdRptServe implements Runnable {
    @ParentCommand CmdRptMain parent;

    @Unmatched
    public List<String> args = new ArrayList<>();

    @Override
    public void run() {
        new CommandLine(new CmdSparqlIntegrateMain())
            .execute(Stream.concat(Stream.of("--server"), args.stream()).toArray(String[]::new));
    }
}
