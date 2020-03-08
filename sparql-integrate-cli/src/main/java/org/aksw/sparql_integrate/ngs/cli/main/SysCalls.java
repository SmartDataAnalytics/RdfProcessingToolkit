package org.aksw.sparql_integrate.ngs.cli.main;

import java.util.List;

import org.aksw.sparql_integrate.ngs.cli.cmd.CmdNgsSort;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.ext.com.google.common.collect.Lists;

public class SysCalls {

	public static List<String> createDefaultSortSysCall(CmdNgsSort cmdSort) {
		List<String> result = Lists.newArrayList("/usr/bin/sort", "-t", "\t");
		if(cmdSort.unique) {
			result.add("-u");
		}
	
		if(cmdSort.reverse) {
			result.add("-r");
		}

		if(cmdSort.randomSort) {
			result.add("-R");
		} else {
			result.add("-h");
		}
		
		if(!Strings.isNullOrEmpty(cmdSort.temporaryDirectory)) {
			result.add("-T");
			result.add(cmdSort.temporaryDirectory);
		}
		
		if(!Strings.isNullOrEmpty(cmdSort.bufferSize)) {
			result.add("-S");
			result.add(cmdSort.bufferSize);
		}
		
		if(cmdSort.parallel > 0) {
			result.add("--parallel");
			result.add("" + cmdSort.parallel);
		}
	
		return result;
	}

}
