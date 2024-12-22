package microbat.instrumentation.ondemandtrace;

import java.util.Map;

import microbat.instrumentation.CommandLine;
import microbat.instrumentation.TraceAgent;
import microbat.instrumentation.instr.TraceTransformer;
import microbat.ondemandtrace.model.traceskeleton.CodeBlockKeyIssuer;
import microbat.ondemandtrace.model.tracestates.TraceState;
import microbat.ondemandtrace.model.tracestates.TraceStateQuerier;
import microbat.ondemandtrace.model.tracestates.TraceStateReader;

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
		TraceStateReader reader = new TraceStateReader(traceStatesPath);
		Map<String, TraceState> traceStates = reader.parseTraceStates();

		/* on-demand trace loading: record inital partial trace */
		if (traceStates.isEmpty()) {
			String initialMethodKey = CodeBlockKeyIssuer.getKeyForMethod(launchClass, testCase, "()V");
			TraceStateQuerier._updateStatusToRecord(initialMethodKey);
		} else {
			TraceStateQuerier.init(traceStates);
		}
	}

	@Override
	public TraceTransformer getTransformer0() {
		return new OnDemandTraceTransformer(agentParams);
	}

}
