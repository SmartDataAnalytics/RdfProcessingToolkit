package org.aksw.sparql_integrate.ngs.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class CmdNgsSubjects {
    @Parameter(description="Non option args")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Parameter(names={"-o", "--out-format"})
    public String outFormat = "trig/pretty";


//	@Parameter(names={"-h", "--help"}, help = true)
//	public boolean help = false;
}
