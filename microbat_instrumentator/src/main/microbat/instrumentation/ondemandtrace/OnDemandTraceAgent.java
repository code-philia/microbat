package microbat.instrumentation.ondemandtrace;

import microbat.instrumentation.CommandLine;
import microbat.instrumentation.TraceAgent;
import microbat.instrumentation.instr.TraceTransformer;
import microbat.ondemandtrace.model.traceskeleton.CodeBlockKeyIssuer;
import microbat.ondemandtrace.model.tracestates.TraceStateQuerier;

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

		/* read existing trace states */
		String launchClass = agentParams.getLaunchClass();
		String testCase = agentParams.getTestCaseName();
		String traceStatesPath = System.getProperty("java.io.tmpdir") + launchClass + "#" + testCase + ".txt";
		TraceStateQuerier.init(traceStatesPath);

		/* on-demand trace loading: record inital partial trace */
		if (TraceStateQuerier.hasNoRecordedTraceStates()) {
			String initialMethodKey = CodeBlockKeyIssuer.getKeyForMethod(launchClass, testCase, "()V");
			TraceStateQuerier._updateStatusToRecord(initialMethodKey);
		}
	}

	@Override
	public TraceTransformer getTransformer0() {
		return new OnDemandTraceTransformer(agentParams);
	}

}
