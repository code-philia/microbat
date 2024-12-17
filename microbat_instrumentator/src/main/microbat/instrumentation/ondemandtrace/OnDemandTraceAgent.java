package microbat.instrumentation.ondemandtrace;

import microbat.instrumentation.CommandLine;
import microbat.instrumentation.TraceAgent;
import microbat.instrumentation.instr.TraceTransformer;
import microbat.instrumentation.runtime.ExecutionTracer;

/**
 * This class is the agent responsible for On-Demand Trace Loading.
 * 
 * @author HongshuW
 */
public class OnDemandTraceAgent extends TraceAgent {

	public OnDemandTraceAgent(CommandLine cmd) {
		super(cmd);
	}

	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		/* init filter */
		super.startup0(vmStartupTime, agentPreStartup);
	}

	@Override
	public TraceTransformer getTransformer0() {
		return new OnDemandTraceTransformer(agentParams);
	}

}
