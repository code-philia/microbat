package microbat.instrumentation.instr.aggreplay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;

import microbat.instrumentation.instr.AbstractInstrumenter;
import microbat.instrumentation.instr.aggreplay.agents.SharedVariableAgent;
import microbat.instrumentation.model.generator.ThreadIdGenerator;
import microbat.instrumentation.runtime.ExecutionTracer;

/**
 * Instrumenter solely for generating thread id
 * @author Gabau
 *
 */
public class ThreadIdInstrumenter extends AbstractInstrumenter {
	
	// the class containing _onThreadStart (Ljava/lang/Thread;)V, IMPT!! should not be interface
	private Class<?> instrumentedClass = getClass();
	
	public ThreadIdInstrumenter() {}
	
	public ThreadIdInstrumenter(Class<?> instrumentedClass) {
		this.instrumentedClass = instrumentedClass;
	}
	
	public static void _onThreadStart(Thread thread) {
		if (ExecutionTracer.isRecordingOrStarted()) {
			ThreadIdGenerator.threadGenerator.createId(thread);
		}
	}
	
	
	@Override
	protected boolean shouldInstrument(String className) {
		return className.equals(Thread.class.getName());
	}

	@Override
	protected byte[] instrument(String classFName, String className, JavaClass jc) {
		System.out.println("Instrumeted thread " + classFName);
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
		String threadStartClass = instrumentedClass.getName().replace(".", "/");
		ALOAD aload = new ALOAD(0);
		INVOKESTATIC invokestatic = new INVOKESTATIC(constantPoolGen.addMethodref(
				threadStartClass, "_onThreadStart", "(Ljava/lang/Thread;)V"));
		iList.insert(invokestatic);
		iList.insert(aload);
		iList.setPositions();
		mGen.setMaxLocals();
		mGen.setMaxStack();
		cg.replaceMethod(startMethod, mGen.getMethod());
	}
	

	@Override
	protected boolean instrumentMethod(ClassGen classGen, ConstantPoolGen constPool, MethodGen methodGen, Method method,
			boolean isAppClass, boolean isMainMethod, boolean isEntry) {
		return false;
	}

}
