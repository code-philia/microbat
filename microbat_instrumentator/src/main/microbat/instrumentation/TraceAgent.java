package microbat.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import microbat.instrumentation.filter.CodeRangeUserFilter;
import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.filter.OverLongMethodFilter;
import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.TraceTransformer;
import microbat.instrumentation.instr.aggreplay.TimeoutThread;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.runtime.ExecutionTracer;
import microbat.instrumentation.runtime.IExecutionTracer;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.sql.Recorder;
import sav.strategies.dto.AppJavaClassPath;

public class TraceAgent extends Agent {
	private AgentParams agentParams;
	protected static ThreadIdGenerator threadIdGenerator = new ThreadIdGenerator();
	private TimeoutThread timeoutThread = new TimeoutThread();
//	private StopTimer timer;

	public TraceAgent(CommandLine cmd) {
		this.agentParams = AgentParams.initFrom(cmd);
	}
	
	public static void _onThreadStart(Thread thread) {
		threadIdGenerator.createId(thread);
	}

	public void startup0(long vmStartupTime, long agentPreStartup) {
		timeoutThread.start();
		SystemClassTransformer.attachThreadId(getInstrumentation(), this.getClass());
//		timer = new StopTimer("Trace Construction");
//		timer.newPoint("Execution");
		/* init filter */
		AppJavaClassPath appPath = agentParams.initAppClassPath();
		GlobalFilterChecker.setup(appPath, agentParams.getIncludesExpression(), agentParams.getExcludesExpression());
		ExecutionTracer.appJavaClassPath = appPath;
		System.out.println("Exection class paths" + ExecutionTracer.appJavaClassPath.getClasspaths());
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
	}

	public void shutdown() throws Exception {
		ExecutionTracer.shutdown();
		/* collect trace & store */
		AgentLogger.debug("Building trace dependencies ...");
//		timer.newPoint("Building trace dependencies");
		// FIXME -mutithread LINYUN [3]
		// LLT: only trace of main thread is recorded.
		List<IExecutionTracer> tracers = ExecutionTracer.getAllThreadStore();

		int size = tracers.size();
		List<Trace> traceList = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {

			ExecutionTracer tracer = (ExecutionTracer) tracers.get(i);

			Trace trace = tracer.getTrace();
			trace.setThreadId(tracer.getThreadId());
			trace.setInnerThreadId(threadIdGenerator.getId(tracer.getThreadId()));
			trace.setThreadName(tracer.getThreadName());
			trace.setMain(ExecutionTracer.getMainThreadStore().equals(tracer));
			trace.setAppJavaClassPath(ExecutionTracer.appJavaClassPath);
			constructTrace(trace);
			traceList.add(trace);
		}
		
//		timer.newPoint("Saving trace");
		Recorder.create(agentParams).store(traceList);
//		AgentLogger.debug(timer.getResultString());
	}

	//FIXME this method can be handled in an asynchronized way
	public void constructTrace(Trace trace) {
		GlobalFilterChecker.addFilterInfo(trace);

		StepMismatchChecker.logNormalSteps(trace);
		ExecutionTracer.dispose(); // clear cache
		long t1 = System.currentTimeMillis();
		AgentLogger.debug("create VirtualDataRelation....");
		createVirtualDataRelation(trace);
		long t2 = System.currentTimeMillis();
		AgentLogger.debug("time for createVirtualDataRelation: " + (t2 - t1) / 1000);

		// TODO Xuezhi we need to comment the code to build control dependencies here.
		t1 = System.currentTimeMillis();
		AgentLogger.debug("construct ControlDomianceRelation....");
		trace.constructControlDomianceRelation();
		t2 = System.currentTimeMillis();

		// trace.constructLoopParentRelation();

	}

//	private void writeOutput(Trace trace) throws Exception {
//		AgentLogger.debug("Saving trace...");
//
//		if (agentParams.getDumpFile() != null) {
//			RunningInfo result = new RunningInfo();
//			result.setProgramMsg(Agent.getProgramMsg());
//			result.setTrace(trace);
//			result.setCollectedSteps(trace.getExecutionList().size());
//			result.setExpectedSteps(agentParams.getExpectedSteps());
//			result.saveToFile(agentParams.getDumpFile(), false);
//			AgentLogger.debug(result.toString());
//		} else if (agentParams.getTcpPort() != AgentConstants.UNSPECIFIED_INT_VALUE) {
//			TcpConnector tcpConnector = new TcpConnector(agentParams.getTcpPort());
//			TraceOutputWriter traceWriter = tcpConnector.connect();
//			traceWriter.writeString(Agent.getProgramMsg());
//			traceWriter.writeTrace(trace);
//			traceWriter.flush();
//			Thread.sleep(10000l);
//			tcpConnector.close();
//		}
//
//		AgentLogger.debug("Trace saved.");
//	}

	private void createVirtualDataRelation(Trace trace) {
		for (int i = 0; i < trace.size(); i++) {
			int order = i + 1;
			TraceNode currentNode = trace.getTraceNode(order);
			if (order < trace.size()) {
				TraceNode nextNode = trace.getTraceNode(order + 1);
				currentNode.setStepInNext(nextNode);
				nextNode.setStepInPrevious(currentNode);
			} else if (order == trace.size()) {
				if (order > 1) {
					TraceNode prevNode = trace.getTraceNode(order - 1);
					currentNode.setStepInPrevious(prevNode);
				}
			}

			TraceNode previousStepOver = currentNode.getStepOverPrevious();
			if (previousStepOver != null
					&& previousStepOver.getClassCanonicalName().equals(currentNode.getClassCanonicalName())
					&& Math.abs(previousStepOver.getLineNumber() - currentNode.getLineNumber()) <= 0) {
				for (VarValue readVar : previousStepOver.getReadVariables()) {
					if (!currentNode.containReadVariable(readVar)) {
						currentNode.addReadVariable(readVar);
					}
				}
			}

			if (currentNode.getInvocationParent() != null && !currentNode.getPassParameters().isEmpty()) {
				TraceNode invocationParent = currentNode.getInvocationParent();
				TraceNode firstChild = invocationParent.getInvocationChildren().get(0);
				if (firstChild.getOrder() == currentNode.getOrder()) {
					for (VarValue value : currentNode.getPassParameters()) {
						invocationParent.addWrittenVariable(value);							
					}
				}
			}

			if (currentNode.getInvocationParent() != null && !currentNode.getReturnedVariables().isEmpty()) {
				TraceNode invocationParent = currentNode.getInvocationParent();
				TraceNode returnStep = invocationParent.getStepOverNext();

				if (returnStep == null) {
					returnStep = currentNode.getStepInNext();
				}

				if (returnStep != null) {
					for (VarValue value : currentNode.getReturnedVariables()) {
						currentNode.addWrittenVariable(value);
						returnStep.addReadVariable(value);
					}
				}
			}

		}
	}

	public AgentParams getAgentParams() {
		return agentParams;
	}

	@Override
	public void startTest(String junitClass, String junitMethod) {
		ExecutionTracer._start();
		ExecutionTracer.appJavaClassPath.setOptionalTestClass(junitClass);
		ExecutionTracer.appJavaClassPath.setOptionalTestMethod(junitMethod);
	}

	@Override
	public void finishTest(String junitClass, String junitMethod) {
		ExecutionTracer.shutdown();
	}

	@Override
	public TraceTransformer getTransformer0() {
		return new TraceTransformer(agentParams);
	}

	@Override
	public void retransformBootstrapClasses(Instrumentation instrumentation, Class<?>[] retransformableClasses)
			throws Exception {
		instrumentation.retransformClasses(retransformableClasses);
	}

	@Override
	public void exitTest(String testResultMsg, String junitClass, String junitMethod, long threadId) {
		// do nothing, not used.
	}

	@Override
	public boolean isInstrumentationActive0() {
		return !ExecutionTracer.isShutdown();
	}
}
