package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * Count the number of graphs by default, or other aspects based on the parameters
 * 
 * @author raven
 *
 */
public class CmdNgsWc {
	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();	

	@Parameter(description="Count number of triples/quads", names={"-l", "--lines"})
	public boolean numQuads = false;

	@Parameter(description="Do not validate; count lines instead of parsing nquads", names={"--nv", "--no-validate"})
	public boolean noValidate = false;
}
