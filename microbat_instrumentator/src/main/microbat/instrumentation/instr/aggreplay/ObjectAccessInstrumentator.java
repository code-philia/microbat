package microbat.instrumentation.instr.aggreplay;

import java.io.ObjectOutputStream.PutField;

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

import javassist.bytecode.Opcode;
import microbat.instrumentation.instr.AbstractInstrumenter;

public abstract class ObjectAccessInstrumentator extends AbstractInstrumenter {

	private Class<?> agentClass;
	// new
	private static final String ON_NEW_OBJECT = "_onNewObject";
	private static final String ON_NEW_OBJECT_SIG = "(Ljava/lang/Object;)V";
	
	// for getfield and putfield instructions
	private static final String ON_OBJECT_WRITE = "_onObjectWrite";
	private static final String ON_OBJECT_WRITE_SIG = "(Ljava/lang/Object;Ljava/lang/String;)V";
	private static final String ON_OBJECT_READ = "_onObjectRead";
	private static final String ON_OBJECT_READ_SIG = "(Ljava/lang/Object;Ljava/lang/String;)V";
	
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
			InstructionList newList = new InstructionList();
			for (InstructionHandle handle: iList) {
				if (handle.getInstruction().getOpcode() == Opcode.NEW) {
					newList.append(handle.getInstruction());
					newList.append(dup);
					newList.append(onNewObjectInvoke);
					continue;
				}
				if (!(handle.getInstruction() instanceof FieldInstruction)) {
					newList.append(handle.getInstruction());
					continue;
				}
				if (handle.getInstruction().getOpcode() == Opcode.GETFIELD) {
					GETFIELD getfield = (GETFIELD) handle.getInstruction();
					int fieldRef = addFieldName(constPool, getfield);
					newList.append(dup); // object, object
					newList.append(new LDC(fieldRef)); // object, object, fieldName
					newList.append(onObjectReadInvoke); // object
					newList.append(handle.getInstruction());
					continue;
				}
				if (handle.getInstruction().getOpcode() == Opcode.PUTFIELD) {
					PUTFIELD putField = (PUTFIELD) handle.getInstruction();
					int fieldRef = addFieldName(constPool, putField);
					newList.append(dup); // object, object
					newList.append(new LDC(fieldRef)); // object, object, fieldName
					newList.append(onObjectWriteInvoke); // object
					newList.append(handle.getInstruction());
					continue;
				}
				
			}
			
			mGen.setInstructionList(newList);
			mGen.setMaxLocals();
			mGen.setMaxStack();
			classGen.replaceMethod(method, mGen.getMethod());
		}
		
		
		// TODO Auto-generated method stub
		return classGen.getJavaClass().getBytes();
	}

}
