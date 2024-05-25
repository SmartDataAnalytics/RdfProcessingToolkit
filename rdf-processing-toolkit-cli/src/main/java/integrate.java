import org.aksw.rdf_processing_toolkit.cli.cmd.RptCmdUtils;
import org.aksw.sparql_integrate.cli.cmd.CmdSparqlIntegrateMain;

public class integrate {
    public static void main(String[] args) throws Exception {
    	RptCmdUtils.execCmd(CmdSparqlIntegrateMain.class, args); 
    }
}
