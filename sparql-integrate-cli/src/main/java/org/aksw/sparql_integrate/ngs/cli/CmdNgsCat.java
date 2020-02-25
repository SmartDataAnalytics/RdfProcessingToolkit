package org.aksw.sparql_integrate.ngs.cli;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class CmdNgsCat {
	@Parameter(names={"-h", "--help"}, help = true)
	public boolean help = false;

	@Parameter(names={"-o", "--out-format"})
	public String outFormat = "trig/pretty";

	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();	

}
