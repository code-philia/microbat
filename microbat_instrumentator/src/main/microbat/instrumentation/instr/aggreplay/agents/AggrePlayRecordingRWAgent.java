package microbat.instrumentation.instr.aggreplay.agents;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import javassist.compiler.ast.Pair;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.instr.aggreplay.ObjectAccessInstrumentator;
import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.instr.aggreplay.record.RecordingInstrumentor;
import microbat.instrumentation.instr.aggreplay.shared.BasicTransformer;
import microbat.instrumentation.instr.aggreplay.shared.RecordingOutput;
import microbat.instrumentation.model.generator.ObjectIdGenerator;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.model.id.Event;
import microbat.instrumentation.model.id.MemoryLocation;
import microbat.instrumentation.model.id.ObjectId;
import microbat.instrumentation.model.id.ReadWriteAccessList;
import microbat.instrumentation.model.id.SharedMemoryLocation;

/**
 * Agent used for recording data.
 * Only records read and write.
 * @author Gabau
 *
 */
public class AggrePlayRecordingRWAgent extends RNRRecordingAgent {
	
	private Semaphore smp = new Semaphore(1);
	private Map<ObjectId, List<Event>> lockAcquisitionListMap = new HashMap<>();


	
	protected AggrePlayRecordingRWAgent(CommandLine cml) {
		super(cml);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onLockAcquire(Object object) {
		ObjectId oId = objectIdGenerator.getId(object);
		if (!lockAcquisitionListMap.containsKey(oId)) {
			lockAcquisitionListMap.put(oId, new LinkedList<Event>());
		}
		lockAcquisitionListMap.get(oId).add(new Event(null));
	}
	@Override
	protected RecordingOutput getRecordingOutput() {
		RecordingOutput result = new RecordingOutput(new ReadWriteAccessList(), ThreadIdGenerator.threadGenerator.getThreadIds(), 
				sharedGenerator.getAllLocations(),
				lockAcquisitionListMap);
		return result;
	}

	@Override
	protected void parseSharedOutput(SharedVariableOutput svo) {
		
	}

	@Override
	public void startTest(String junitClass, String junitMethod) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finishTest(String junitClass, String junitMethod) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ClassFileTransformer getTransformer0() {
		return new BasicTransformer(new RecordingInstrumentor(RNRRecordingAgent.class));
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
	
	private void acquireLock() {
		try {
			smp.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void releaseLock() {
		smp.release();
	}
	
	@Override
	protected void onRead(SharedMemoryLocation sml) {
		Event readEvent = new Event(sml);
		lastEvent.set(readEvent);
		acquireLock();
		sml.appendExList(sml.getLastWrite(), readEvent);
	}
	@Override
	protected void onWrite(SharedMemoryLocation sml) {
		Event writeEvent = new Event(sml);
		lastEvent.set(writeEvent);
		acquireLock();
		sml.setLastWrite(writeEvent);
	}

	@Override
	protected void afterWrite() {
		releaseLock();
	}

	@Override
	protected void afterRead() {
		releaseLock();
	}

}
