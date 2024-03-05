package microbat.instrumentation.instr.aggreplay.record;

import java.util.concurrent.Semaphore;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.DUP2;
import org.apache.bcel.generic.DUP2_X1;
import org.apache.bcel.generic.DUP_X2;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.POP2;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.SWAP;
import org.apache.bcel.generic.Type;

import microbat.instrumentation.instr.aggreplay.ObjectAccessInstrumentator;
import microbat.instrumentation.instr.aggreplay.agents.AggrePlayRecordingAgent;
import microbat.instrumentation.instr.aggreplay.agents.AggrePlaySharedVariableAgent;
import microbat.instrumentation.model.id.AggrePlayMethods;

public class RecordingInstrumentor extends ObjectAccessInstrumentator {
	
	public static final String ACQUIRE_LOCK_STRING = "_acquireLock";
	public static final String RELEASE_LOCK_STRING = "_releaseLock";
	public static final String LOCK_SIG_STRING = "()V";
	
	public RecordingInstrumentor() {
		super(AggrePlayRecordingAgent.class);
	}
	
	public RecordingInstrumentor(Class<?> clazz) {
		super(clazz);
	}
	
	

	

	@Override
	protected void instrumentPutStatic(ConstantPoolGen cpg, InstructionList il, InstructionHandle ih) {
		/**
		 * Need to call GETSTATIC to force the class loader to run first
		 * so that there is no interleaving between static calls.
		 */
		InstructionList beforePutStatic = new InstructionList();
		PUTSTATIC ps = (PUTSTATIC) ih.getInstruction();
		GETSTATIC gs = new GETSTATIC(ps.getIndex());
		beforePutStatic.append(gs);
		if (ps.getType(cpg).equals(Type.LONG) || ps.getType(cpg).equals(Type.DOUBLE)) {
			beforePutStatic.append(new POP2());
		} else {
			beforePutStatic.append(new POP());
		}
		insertInsnHandler(il, beforePutStatic, ih);
		
		super.instrumentPutStatic(cpg, il, ih);
		InstructionList afterInstructionList = new InstructionList();
		afterInstructionList.append(AggrePlayMethods.AFTER_OBJECT_WRITE.toInvokeStatic(cpg, agentClass));
		appendInstruction(il, afterInstructionList, ih);
	}

	protected boolean isComputationType2(FieldInstruction ih, ConstantPoolGen cpg) {
		return ih.getType(cpg).equals(Type.LONG)
				|| ih.getType(cpg).equals(Type.DOUBLE);
	}
	
	@Override
	protected void instrumentGetStaticInstruction(ConstantPoolGen cpg, InstructionList il, InstructionHandle ih) {
		InstructionList beforeGetStaticInstructionList = new InstructionList();
		beforeGetStaticInstructionList.append(ih.getInstruction());
		if (isComputationType2((FieldInstruction) ih.getInstruction(), cpg)) {
			beforeGetStaticInstructionList.append(new POP2());
		} else {
			beforeGetStaticInstructionList.append(new POP());
		}
		insertInsnHandler(il, beforeGetStaticInstructionList, ih);
		super.instrumentGetStaticInstruction(cpg, il, ih);
		InstructionList afterInstructionList = new InstructionList();
		afterInstructionList.append(AggrePlayMethods.AFTER_OBJECT_READ.toInvokeStatic(cpg, agentClass));
		appendInstruction(il, afterInstructionList, ih);
	}


	
	

	@Override
	protected void instrumentArrayRead(ConstantPoolGen cpg, InstructionList il, InstructionHandle ih) {
		// TODO Auto-generated method stub
		super.instrumentArrayRead(cpg, il, ih);
		InstructionList toAppend = new InstructionList();
		toAppend.append(AggrePlayMethods.AFTER_OBJECT_READ.toInvokeStatic(cpg, agentClass));
		appendInstruction(il, toAppend, ih);
	}

	@Override
	protected void instrumentArrayWrite(ConstantPoolGen cpg, InstructionList il, InstructionHandle ih) {
		// TODO Auto-generated method stub
		super.instrumentArrayWrite(cpg, il, ih);
		InstructionList toAppend = new InstructionList();
		toAppend.append(AggrePlayMethods.AFTER_OBJECT_WRITE.toInvokeStatic(cpg, agentClass));
		appendInstruction(il, toAppend, ih);
	}

	@Override
	protected void instrumentPutField(ConstantPoolGen constPool, INVOKESTATIC onObjectWriteInvoke,
			InstructionList iList, InstructionHandle handle, PUTFIELD putField) {
		InstructionList beforeInstructionList = new InstructionList();
		InstructionList afterInstructionList = new InstructionList();

		if (isComputationType2(putField, constPool)) {
			beforeInstructionList.append(new DUP2_X1());
			beforeInstructionList.append(new POP2());
			beforeInstructionList.append(new DUP());
		} else {
			// objectRef, value
			beforeInstructionList.append(new SWAP());
			// v, o, o
			beforeInstructionList.append(new DUP());	
		}
		beforeInstructionList.append(new LDC(constPool.addString(putField.getFieldName(constPool))));
		// v, o, o, c
		beforeInstructionList
			.append(createInvokeStatic(constPool, agentClass, 
					AggrePlayMethods.BEFORE_OBJECT_WRITE));
		// v, o
		if (isComputationType2(putField, constPool)) {
			beforeInstructionList.append(new DUP_X2());
			beforeInstructionList.append(new POP());
		} else {
			beforeInstructionList.append(new SWAP());
		}
		// o, v
		afterInstructionList.append(AggrePlayMethods.AFTER_OBJECT_WRITE.toInvokeStatic(constPool, agentClass));

		insertInsnHandler(iList, beforeInstructionList, handle);
		appendInstruction(iList, afterInstructionList, handle);
	}

	@Override
	protected void instrumentGetField(ConstantPoolGen constPool, INVOKESTATIC onObjectReadInvoke, InstructionList iList,
			InstructionHandle handle, GETFIELD getfield) {
		
		// TODO Auto-generated method stub
		InstructionList beforeInstructionList = new InstructionList();
		InstructionList afterInstructionList = new InstructionList();
		
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
