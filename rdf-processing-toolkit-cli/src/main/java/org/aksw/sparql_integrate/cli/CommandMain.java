package org.aksw.sparql_integrate.cli;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Show SPARQL Stream information")
public class CommandMain {
	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();

	@Parameter(names="-f", description="Preferred RDF format")
	public String preferredRdfFormat;
	
	@Parameter(names={"-h", "--help"}, help = true)
	public boolean help = false;

	@Parameter(names="-u")
	public boolean isUnionDefaultGraphMode = false;
}
