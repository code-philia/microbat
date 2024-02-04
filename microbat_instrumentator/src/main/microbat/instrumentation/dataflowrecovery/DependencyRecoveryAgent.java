package microbat.instrumentation.dataflowrecovery;

import microbat.instrumentation.CommandLine;
import microbat.instrumentation.TraceAgent;

/**
 * Agent used to recover data dependency relevant to library calls.
 * 
 * @author hongshuwang
 */
public class DependencyRecoveryAgent extends TraceAgent {

	public DependencyRecoveryAgent(CommandLine cmd) {
		super(cmd);
	}

}
