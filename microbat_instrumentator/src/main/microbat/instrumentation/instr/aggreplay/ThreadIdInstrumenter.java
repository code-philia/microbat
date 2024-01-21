package microbat.instrumentation.instr.aggreplay;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;

import microbat.instrumentation.AggrePlaySharedVariableAgent;
import microbat.instrumentation.instr.AbstractInstrumenter;

/**
 * Instrumenter solely for generating thread id
 * @author Gabau
 *
 */
public class ThreadIdInstrumenter extends AbstractInstrumenter {

	@Override
	protected boolean shouldInstrument(String className) {
		return className.equals(Thread.class.getName());
	}

	@Override
	protected byte[] instrument(String classFName, String className, JavaClass jc) {
		// TODO Auto-generated method stub
		ClassGen classGen = new ClassGen(jc);
		Method startMethod = null;
		for (Method method: classGen.getMethods()) {
			if ("start".equals(method.getName())) {
				startMethod = method;
			}
		}
		instrumentStartMethod(classGen, startMethod);
		return classGen.getJavaClass().getBytes();
	}
	
	protected void instrumentStartMethod(ClassGen cg, Method startMethod) {
		MethodGen mGen = new MethodGen(startMethod, cg.getClassName(), cg.getConstantPool());
		InstructionList iList = mGen.getInstructionList();
		ConstantPoolGen constantPoolGen = cg.getConstantPool();
		String aggrePlayClassNameString = AggrePlaySharedVariableAgent.class.getName().replace(".", "/");
		ALOAD aload = new ALOAD(0);
		INVOKESTATIC invokestatic = new INVOKESTATIC(constantPoolGen.addMethodref(
				aggrePlayClassNameString, "_onThreadStart", "(Ljava/lang/Thread;)V"));
		iList.insert(invokestatic);
		iList.insert(aload);
		mGen.setMaxLocals();
		mGen.setMaxStack();
		cg.replaceMethod(startMethod, mGen.getMethod());
	}
	

	@Override
	protected boolean instrumentMethod(ClassGen classGen, ConstantPoolGen constPool, MethodGen methodGen, Method method,
			boolean isAppClass, boolean isMainMethod, boolean isEntry) {
		// TODO Auto-generated method stub
		return false;
	}

}
