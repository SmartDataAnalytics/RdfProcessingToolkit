package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class CmdNgsSort {
	/**
	 * sparql-pattern file
	 * 
	 */
	@Parameter(description="Non option args")
	public List<String> nonOptionArgs = new ArrayList<>();

	@Parameter(names={"-k", "--key"})
	public String key = null;

	@Parameter(names={"-R", "--random-sort"})
	public boolean randomSort = false;
	
	@Parameter(names={"-u", "--unique"})
	public boolean unique = false;

	@Parameter(names={"-S", "--buffer-size"})
	public String bufferSize = null;

	@Parameter(names={"-T", "--temporary-directory"})
	public String temporaryDirectory = null;

	// TODO Integrate oshi to get physical core count by default
	@Parameter(names={"--parallel"})
	public int parallel = -1;

	// TODO Clarify merge semantics
	// At present it is for conflating consecutive named graphs with the same name into a single graph
	@Parameter(names={"-m", "--merge"})
	public boolean merge = false;

}
