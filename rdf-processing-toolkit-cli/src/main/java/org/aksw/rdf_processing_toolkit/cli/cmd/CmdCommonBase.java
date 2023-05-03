package org.aksw.rdf_processing_toolkit.cli.cmd;

import picocli.CommandLine.Option;

public class CmdCommonBase
	implements HasDebugMode
{
    @Option(names = { "-X" }, description = "Debug output such as full stacktraces")
    public boolean debugMode = false;

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Option(names = { "-v", "--version" }, versionHelp = true)
    public boolean version = false;

	@Override
	public boolean isDebugMode() {
		return debugMode;
	}
}
