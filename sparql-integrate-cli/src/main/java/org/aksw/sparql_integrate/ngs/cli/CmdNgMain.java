package org.aksw.sparql_integrate.ngs.cli;

import com.beust.jcommander.Parameter;

public class CmdNgMain {
	@Parameter(names={"-h", "--help"}, help=true)
	public boolean help = false;
}