package org.aksw.sparql_integrate.ngs.cli;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class CmdNgsWc {
	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();	

	@Parameter(description="Count number of graphs (not lines)", names={"-l", "--lines"})
	public boolean numGraphs = false;
}
