package microbat.instrumentation.instr.aggreplay;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.SWAP;

import javassist.bytecode.Opcode;
import microbat.instrumentation.instr.AbstractInstrumenter;
import microbat.instrumentation.model.id.AggrePlayMethods;

public abstract class ObjectAccessInstrumentator extends AbstractInstrumenter {

	protected Class<?> agentClass;
	/**
	 * Fired after a new object is created
	 */
	public static final String ON_NEW_OBJECT = "_onNewObject";
	public static final String ON_NEW_OBJECT_SIG = "(Ljava/lang/Object;)V";
	
	// for getfield and putfield instructions
	/**
	 * Before object write
	 */
	public static final String ON_OBJECT_WRITE = "_onObjectWrite";
	public static final String ON_OBJECT_WRITE_SIG = "(Ljava/lang/Object;Ljava/lang/String;)V";
	/**
	 * Before object read
	 */
	public static final String ON_OBJECT_READ = "_onObjectRead";
	public static final String ON_OBJECT_READ_SIG = "(Ljava/lang/Object;Ljava/lang/String;)V";
	
	public ObjectAccessInstrumentator(Class<?> agentClass) {
		this.agentClass = agentClass;
	}
	
	@Override
	protected boolean instrumentMethod(ClassGen classGen, ConstantPoolGen constPool, MethodGen methodGen, Method method,
			boolean isAppClass, boolean isMainMethod, boolean isEntry) {

		// TODO Auto-generated method stub
		return false;
	}

	private int addFieldName(ConstantPoolGen constPool, FieldInstruction getfield) {
		String fieldName = getfield.getName(constPool);
		return constPool.addString(fieldName);
	}
	

	@Override
	protected byte[] instrument(String classFName, String className, JavaClass jc) {
		ClassGen classGen = new ClassGen(jc);
		ConstantPoolGen constPool = classGen.getConstantPool();
		String agentClassString = agentClass.getName().replace(".", "/");
		final int onNewObjectMethod = 
				constPool.addMethodref(agentClassString, ON_NEW_OBJECT, ON_NEW_OBJECT_SIG);
		final int onObjectWrite =
				constPool.addMethodref(agentClassString, ON_OBJECT_WRITE, ON_OBJECT_WRITE_SIG);
		final int onObjectRead = 
				constPool.addMethodref(agentClassString, ON_OBJECT_READ, ON_OBJECT_READ_SIG);
		
		final INVOKESTATIC onNewObjectInvoke = new INVOKESTATIC(onNewObjectMethod);
		final INVOKESTATIC onObjectWriteInvoke = new INVOKESTATIC(onObjectWrite);
		final INVOKESTATIC onObjectReadInvoke = new INVOKESTATIC(onObjectRead);
		final DUP dup = new DUP();
		
		for (Method method : classGen.getMethods()) {
			MethodGen mGen = new MethodGen(method, className, constPool);
			InstructionList iList =  mGen.getInstructionList();
			for (InstructionHandle handle: iList) {
				InstructionList newList = new InstructionList();
				if (handle.getInstruction().getOpcode() == Opcode.NEW) {
					newList.append(dup);
					newList.append(onNewObjectInvoke);
					if(handle.getNext() == null) {
						iList.append(newList);
					} else {
						insertInsnHandler(iList, newList, handle.getNext());
					}
					newList.dispose();
					continue;
				}
				if (handle.getInstruction().getOpcode() == Opcode.GETFIELD) {
					GETFIELD getfield = (GETFIELD) handle.getInstruction();
					instrumentGetField(constPool, onObjectReadInvoke, iList, handle, getfield);
					continue;
				}
				if (handle.getInstruction().getOpcode() == Opcode.PUTFIELD) {
					PUTFIELD putField = (PUTFIELD) handle.getInstruction();
					instrumentPutField(constPool, onObjectWriteInvoke, iList, handle, putField);
					continue;
				}
				
			}
			
			mGen.setMaxLocals();
			mGen.setMaxStack();
			classGen.replaceMethod(method, mGen.getMethod());
		}
	
		return classGen.getJavaClass().getBytes();
	}

	protected void instrumentPutField(ConstantPoolGen constPool, final INVOKESTATIC onObjectWriteInvoke, InstructionList iList,
			InstructionHandle handle, PUTFIELD putField) {
		InstructionList newList = new InstructionList();
		int fieldRef = addFieldName(constPool, putField);
		newList.append(new SWAP());
		newList.append(new DUP()); // object, object
		
		newList.append(new LDC(fieldRef)); // object, object, fieldName
		newList.append(onObjectWriteInvoke); // object
		newList.append(new SWAP());
		insertInsnHandler(iList, newList, handle);
		newList.dispose();
	}

	protected void instrumentGetField(
			ConstantPoolGen constPool, final INVOKESTATIC onObjectReadInvoke,
			InstructionList iList, InstructionHandle handle, GETFIELD getfield) {
		InstructionList newList = new InstructionList();
		int fieldRef = addFieldName(constPool, getfield);
		newList.append(new DUP()); // object, object
		newList.append(new LDC(fieldRef)); // object, object, fieldName
		newList.append(onObjectReadInvoke); // object
		insertInsnHandler(iList, newList, handle);
		newList.dispose();
	}
	
	protected INVOKESTATIC createInvokeStatic(ConstantPoolGen cpg, Class<?> clazz, AggrePlayMethods method) {
		return createInvokeStatic(cpg, clazz, method.methodName, method.methodSig);
	}
	
	protected INVOKESTATIC createInvokeStatic(ConstantPoolGen cpg, Class<?> clazz, String methodName, String signature) {
		return new INVOKESTATIC(cpg.addMethodref(clazz.getName().replace(".", "/"), methodName, signature));
	}

}
