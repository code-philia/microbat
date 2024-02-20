package microbat.instrumentation.dataflowrecovery;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.TraceAgent;
import microbat.instrumentation.filter.CodeRangeUserFilter;
import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.filter.OverLongMethodFilter;
import microbat.instrumentation.runtime.ExecutionTracer;
import microbat.instrumentation.runtime.IExecutionTracer;
import sav.strategies.dto.AppJavaClassPath;

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
	public void startup0(long vmStartupTime, long agentPreStartup) {
		/* init filter */
		AppJavaClassPath appPath = agentParams.initAppClassPath();
		GlobalFilterChecker.setup(appPath, agentParams.getIncludesExpression(), agentParams.getExcludesExpression());
		ExecutionTracer.appJavaClassPath = appPath;
		ExecutionTracer.variableLayer = agentParams.getVariableLayer();
		ExecutionTracer.setStepLimit(agentParams.getStepLimit());
		if (!agentParams.isRequireMethodSplit()) {
			agentParams.getUserFilters().register(new OverLongMethodFilter(agentParams.getOverlongMethods()));
		}

		if (!agentParams.getCodeRanges().isEmpty()) {
			agentParams.getUserFilters().register(new CodeRangeUserFilter(agentParams.getCodeRanges()));
		}

		ExecutionTracer.setExpectedSteps(agentParams.getExpectedSteps());
		ExecutionTracer.avoidProxyToString = agentParams.isAvoidProxyToString();
		
		ExecutionTracer.isRecordingLibCalls = true;
	}

	@Override
	public void shutdown() throws Exception {
		ExecutionTracer.shutdown();
		/* collect library call data and store */
		AgentLogger.debug("Collecting library call data ...");
		
		List<IExecutionTracer> tracers = ExecutionTracer.getAllThreadStore();

		int size = tracers.size();
		Map<String, Set<String>> libraryCalls = new HashMap<>();
		for (int i = 0; i < size; i++) {
			ExecutionTracer tracer = (ExecutionTracer) tracers.get(i);
			mergeMaps(libraryCalls, tracer.getLibraryCalls());
		}

		if (agentParams.getDumpFile() != null) {
			DependencyRecoveryInfo result = new DependencyRecoveryInfo(libraryCalls);
			result.saveToFile(agentParams.getDumpFile(), false);
		}
	}

	@Override
	public DependencyRecoveryTransformer getTransformer0() {
		return new DependencyRecoveryTransformer(super.agentParams);
	}

	@Override
	public void retransformBootstrapClasses(Instrumentation instrumentation, Class<?>[] retransformableClasses)
			throws Exception {
		// do nothing
	}
	
	private void mergeMaps(Map<String, Set<String>> des, Map<String, Set<String>> src) {
		for (String key : src.keySet()) {
			if (!des.containsKey(key)) {
				des.put(key, new HashSet<String>());
			}
			des.get(key).addAll(src.get(key));
		}
	}

}
