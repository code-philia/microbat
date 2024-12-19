package microbat.instrumentation.ondemandtrace;

import microbat.instrumentation.CommandLine;
import microbat.instrumentation.TraceAgent;
import microbat.instrumentation.instr.TraceTransformer;
import microbat.instrumentation.ondemandtrace.tracestatus.CodeBlockKeyIssuer;
import microbat.instrumentation.ondemandtrace.tracestatus.TraceStatusStore;

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

		/* on-demand trace loading: record inital partial trace */
		String className = agentParams.getLaunchClass().replace('.', '/');
		String initialMethodKey = CodeBlockKeyIssuer.getKeyForMethod(className, agentParams.getTestCaseName(), "()V");
		TraceStatusStore._updateStatusToRecord(initialMethodKey);
	}

	@Override
	public TraceTransformer getTransformer0() {
		return new OnDemandTraceTransformer(agentParams);
	}

}
