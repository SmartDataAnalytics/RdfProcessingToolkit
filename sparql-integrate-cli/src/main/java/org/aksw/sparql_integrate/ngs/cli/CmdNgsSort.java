package org.aksw.sparql_integrate.ngs.cli;

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
}
