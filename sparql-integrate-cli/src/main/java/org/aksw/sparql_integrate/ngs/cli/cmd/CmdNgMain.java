package org.aksw.sparql_integrate.ngs.cli.cmd;

import com.beust.jcommander.Parameter;

public class CmdNgMain {
	@Parameter(names={"-h", "--help"}, help=true)
	public boolean help = false;
	
	
	@Parameter(names={"-o", "--out-format"})
	public String format = "trig/pretty";

}
