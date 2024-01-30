package microbat.instrumentation.instr.aggreplay.agents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.TraceAgent;
import microbat.instrumentation.instr.SystemClassTransformer;
import microbat.instrumentation.instr.TraceInstrumenter;
import microbat.instrumentation.instr.TraceTransformer;
import microbat.instrumentation.instr.aggreplay.AggrePlayTraceInstrumenter;
import microbat.instrumentation.instr.aggreplay.shared.BasicTransformer;
import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.RecordingOutput;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.model.generator.SharedMemoryGenerator;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.SharedMemoryLocation;
import microbat.instrumentation.runtime.ExecutionTracer;
import microbat.instrumentation.runtime.IExecutionTracer;
import microbat.model.trace.Trace;
import microbat.sql.Recorder;

public class AggrePlayReplayAgent extends TraceAgent {
	
	private AgentParams agentParams;
	private IExecutionTracer executionTracer;
	
	/**
	 * Current replay values
	 */
	private SharedMemoryGenerator sharedMemGenerator;
	private ClassFileTransformer transformer;
	private static AggrePlayReplayAgent attachedAgent;
	
	/**
	 * Recorded output
	 */
	private ReadWriteAccessList rwal;
	public AggrePlayReplayAgent(CommandLine cmd) {
		super(cmd);
		agentParams = AgentParams.initFrom(cmd);
	
		this.transformer = new BasicTransformer(new AggrePlayTraceInstrumenter(agentParams));
	}
	

	
	public static AggrePlayReplayAgent getAttached(CommandLine cmd) {
		attachedAgent = new AggrePlayReplayAgent(cmd);
		return attachedAgent;
	}
	
	public static void _onObjectRead(Object object, String field) {
		
//		if (!attachedAgent.sharedMemGenerator.isSharedObject(object, field)) {
//			return;
//		}
//		SharedMemoryLocation sharedMemLocation = attachedAgent.sharedMemGenerator.ofField(object, field);
//		while (!sharedMemLocation.isSameAsLastWrite()) {
//			Thread.yield();
//		}
		// execute the read
	}
	
	public static void _afterObjectWrite() {
		
	}

	public static void _afterObjectRead() {
		
	}
	
	@Override
	public void startup0(long vmStartupTime, long agentPreStartup) {
		SystemClassTransformer.attachThreadId(getInstrumentation());
		File concDumpFile = new File(agentParams.getConcDumpFile());
		try {
			FileReader concReader = new FileReader(concDumpFile);
			RecordingOutput input = new RecordingOutput();
			SharedDataParser parser = new SharedDataParser();
			List<ParseData> result = parser.parse(concReader);
			input.parse(result.get(0));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		super.startup0(vmStartupTime, agentPreStartup);
		ExecutionTracer._start();
	}
	

	@Override
	public void shutdown() throws Exception {
		// Agent._exitProgram(getProgramMsg());
		System.out.println("Shutting down");
		try {

			shutdownInner();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Shutdown");
	}

	private void shutdownInner() {
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
			trace.setThreadName(tracer.getThreadName());
			trace.setMain(ExecutionTracer.getMainThreadStore().equals(tracer));
			
			// TODO(Gab): Botch needed because tracer can be initialised 
			// in thread id instrumenter
			if (trace.getAppJavaClassPath() == null) {
				trace.setAppJavaClassPath(ExecutionTracer.appJavaClassPath);
			}
			constructTrace(trace);
			traceList.add(trace);
		}
		
//		timer.newPoint("Saving trace");
		Recorder.create(agentParams).store(traceList);
//		AgentLogger.debug(timer.getResultString());
	}
	

	@Override
	public void finishTest(String junitClass, String junitMethod) {
		// TODO Auto-generated method stub
		
	}
	
	


	@Override
	public ClassFileTransformer getTransformer() {
		// TODO Auto-generated method stub
		return this.transformer;
	}

	@Override
	public void retransformBootstrapClasses(Instrumentation instrumentation, Class<?>[] retransformableClasses)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exitTest(String testResultMsg, String junitClass, String junitMethod, long threadId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isInstrumentationActive0() {
		// TODO Auto-generated method stub
		return false;
	}

}
