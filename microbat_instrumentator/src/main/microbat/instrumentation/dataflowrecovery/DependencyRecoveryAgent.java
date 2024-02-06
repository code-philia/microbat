package microbat.instrumentation.dataflowrecovery;

import java.lang.instrument.Instrumentation;

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

	@Override
	public void shutdown() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DependencyRecoveryTransformer getTransformer0() {
		return new DependencyRecoveryTransformer(super.agentParams);
	}

	@Override
	public void retransformBootstrapClasses(Instrumentation instrumentation, Class<?>[] retransformableClasses)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

}
