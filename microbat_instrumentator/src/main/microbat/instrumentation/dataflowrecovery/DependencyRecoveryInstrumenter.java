package microbat.instrumentation.dataflowrecovery;

import java.util.List;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import microbat.instrumentation.AgentConstants;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.LocalVariableSupporter;
import microbat.instrumentation.instr.TraceInstrumenter;
import microbat.instrumentation.instr.instruction.info.LineInstructionInfo;
import microbat.instrumentation.runtime.IExecutionTracer;

/**
 * @author hongshuwang
 */
public class DependencyRecoveryInstrumenter extends TraceInstrumenter {

	public DependencyRecoveryInstrumenter(AgentParams params) {
		super(params);
	}
	
	/**
	 * Only instrument invoke instructions
	 */
	@Override
	protected boolean instrumentMethod(ClassGen classGen, ConstantPoolGen constPool, MethodGen methodGen, Method method,
			boolean isAppClass, boolean isMainMethod, boolean isEntry) {
		if (!userFilters.isInstrumentable(classGen.getClassName(), method, methodGen.getLineNumbers())) {
			return false;
		}
		tempVarIdx = 0;
		InstructionList insnList = methodGen.getInstructionList();
		InstructionHandle startInsn = insnList.getStart();
		if (startInsn == null) {
			// empty method
			return false;
		}
		
		/* fill up missing variables in localVariableTable */
		LocalVariableSupporter.fillUpVariableTable(methodGen, method, constPool);
		List<LineInstructionInfo> lineInsnInfos = LineInstructionInfo.buildLineInstructionInfos(classGen, constPool,
				methodGen, method, isAppClass, insnList);
		int startLine = Integer.MAX_VALUE;
		int endLine = AgentConstants.UNKNOWN_LINE;
		for (LineInstructionInfo lineInfo : lineInsnInfos) {
			int line = lineInfo.getLine();
			if (line < startLine) {
				startLine = line;
			}
			if (line > endLine) {
				endLine = line;
			}
		}
		if (startLine == Integer.MAX_VALUE) {
			startLine = AgentConstants.UNKNOWN_LINE;
		}
		LocalVariableGen classNameVar = createLocalVariable(CLASS_NAME, methodGen, constPool);
		LocalVariableGen methodSigVar = createLocalVariable(METHOD_SIGNATURE, methodGen, constPool);
		LocalVariableGen tracerVar = methodGen.addLocalVariable(TRACER_VAR_NAME, Type.getType(IExecutionTracer.class),
				insnList.getStart(), insnList.getEnd());
		
		userFilters.filter(lineInsnInfos, classGen.getClassName(), method);
		for (LineInstructionInfo lineInfo : lineInsnInfos) {
			int line = lineInfo.getLine();
			
			injectCodeTracerHitLine(insnList, constPool, tracerVar, lineInfo.getLine(), lineInfo.getLineNumberInsn(),
					classNameVar, methodSigVar, lineInfo.hasExceptionTarget(), lineInfo.getReadWriteInsnTotal(false),
					lineInfo.getReadWriteInsnTotal(true), lineInfo);
			
			/* instrument Invocation instructions */
			InstructionFactory instructionFactory = new InstructionFactory(classGen, constPool);
			for (InstructionHandle insn : lineInfo.getInvokeInstructions()) {
				injectCodeTracerInvokeMethod(methodGen, insnList, constPool, instructionFactory, tracerVar, insn, line,
						classNameVar, methodSigVar, isAppClass);
			}

			lineInfo.dispose();
		}
		injectCodeInitTracer(methodGen, constPool, startLine, endLine, isAppClass, classNameVar,
				methodSigVar, isMainMethod, tracerVar);
		return true;
	}

}
