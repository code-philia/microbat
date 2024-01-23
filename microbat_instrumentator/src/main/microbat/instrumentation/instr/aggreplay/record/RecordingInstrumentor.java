package microbat.instrumentation.instr.aggreplay.record;

import java.util.concurrent.Semaphore;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.SWAP;

import microbat.instrumentation.instr.aggreplay.ObjectAccessInstrumentator;
import microbat.instrumentation.instr.aggreplay.agents.AggrePlayRecordingAgent;
import microbat.instrumentation.instr.aggreplay.agents.AggrePlaySharedVariableAgent;
import microbat.instrumentation.model.id.AggrePlayMethods;

public class RecordingInstrumentor extends ObjectAccessInstrumentator {
	
	public static final Semaphore LOCK_OBJECT = new Semaphore(1);
	public static final String ACQUIRE_LOCK_STRING = "_acquireLock";
	public static final String RELEASE_LOCK_STRING = "_releaseLock";
	public static final String LOCK_SIG_STRING = "()V";
	
	public RecordingInstrumentor() {
		super(AggrePlayRecordingAgent.class);
	}
	
	public RecordingInstrumentor(Class<?> clazz) {
		super(clazz);
	}

	public static void _acquireLock() {
		try {
			LOCK_OBJECT.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void _releaseLock() {
		LOCK_OBJECT.release();
	}
	
	

	@Override
	protected void instrumentPutField(ConstantPoolGen constPool, INVOKESTATIC onObjectWriteInvoke,
			InstructionList iList, InstructionHandle handle, PUTFIELD putField) {

		// TODO Auto-generated method stub
		InstructionList beforeInstructionList = new InstructionList();
		InstructionList afterInstructionList = new InstructionList();
		// load lock object
		final int acquireLockRef = constPool.addMethodref(getClass().getName().replace(".", "/"),
				ACQUIRE_LOCK_STRING, LOCK_SIG_STRING);
		final int releaseLockRef = constPool.addMethodref(getClass().getName().replace(".", "/"),
				RELEASE_LOCK_STRING, LOCK_SIG_STRING);

		INVOKESTATIC acquireLockInvokestatic = new INVOKESTATIC(acquireLockRef);
		INVOKESTATIC releaseLockInvokestatic = new INVOKESTATIC(releaseLockRef);
		beforeInstructionList.append(acquireLockInvokestatic);
		// objectRef, value
		beforeInstructionList.append(new SWAP());
		
		// value, objectRef
	
		// v, o, o
		beforeInstructionList.append(new DUP());
		beforeInstructionList.append(new LDC(constPool.addString(putField.getFieldName(constPool))));
		// v, o, o, c
		beforeInstructionList
			.append(createInvokeStatic(constPool, agentClass, 
					AggrePlayMethods.BEFORE_OBJECT_WRITE));
		// v, o
		beforeInstructionList.append(new SWAP());
		// o, v
		afterInstructionList.append(releaseLockInvokestatic);

		insertInsnHandler(iList, beforeInstructionList, handle);
		appendInstruction(iList, afterInstructionList, handle);
	}

	@Override
	protected void instrumentGetField(ConstantPoolGen constPool, INVOKESTATIC onObjectReadInvoke, InstructionList iList,
			InstructionHandle handle, GETFIELD getfield) {
		
		// TODO Auto-generated method stub
		InstructionList beforeInstructionList = new InstructionList();
		InstructionList afterInstructionList = new InstructionList();
		
		// load lock object
		final int acquireLockRef = constPool.addMethodref(getClass().getName().replace(".", "/"),
				ACQUIRE_LOCK_STRING, LOCK_SIG_STRING);
		final int releaseLockRef = constPool.addMethodref(getClass().getName().replace(".", "/"),
				RELEASE_LOCK_STRING, LOCK_SIG_STRING);
		// objectRef
		beforeInstructionList.append(new DUP());
		// objectRef, objectRef
		beforeInstructionList
			.append(new LDC(constPool.addString(getfield.getFieldName(constPool))));
		// obj, obj, field
		beforeInstructionList.append(createInvokeStatic(constPool, 
				agentClass, AggrePlayMethods.BEFORE_OBJECT_READ));
		
		afterInstructionList.append(createInvokeStatic(constPool, agentClass, 
				AggrePlayMethods.AFTER_OBJECT_READ));
		insertInsnHandler(iList, beforeInstructionList, handle);
		appendInstruction(iList, afterInstructionList, handle);
	}
	
	
	

	
	
}
