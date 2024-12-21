package microbat.instrumentation.ondemandtrace;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUSH;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.TraceInstrumenter;
import microbat.instrumentation.ondemandtrace.traceskeleton.CodeBlockKeyIssuer;

/**
 * This class is responsible for instrumenting loaded java classes for on-demand
 * trace loading.
 * 
 * @author HongshuW
 */
public class OnDemandTraceInstrumenter extends TraceInstrumenter {

	public OnDemandTraceInstrumenter(AgentParams params) {
		super(params);
	}

	@Override
	protected boolean instrumentMethod(ClassGen classGen, ConstantPoolGen constPool, MethodGen methodGen, Method method,
			boolean isAppClass, boolean isMainMethod, boolean isEntry) {
		if (!userFilters.isInstrumentable(classGen.getClassName(), method, methodGen.getLineNumbers())) {
			return false;
		}

		// original code
		InstructionList originalInstructions = methodGen.getInstructionList().copy();

		// instrumented code for tracing
		boolean changed = super.instrumentMethod(classGen, constPool, methodGen, method, isAppClass, isMainMethod,
				isEntry);

		if (changed) {
			InstructionList outputInstructions = new InstructionList();

			InstructionList tracingInstructions = methodGen.getInstructionList();

			/* Invoke `TraceStatusStore._isToRecord("class%method")` */
			loadControlBoolToStack(methodGen, outputInstructions, constPool);
			OnDemandTraceMethods isToRecordMethod = OnDemandTraceMethods.IS_TO_RECORD;
			insertInvocationOfStaticMethod(isToRecordMethod, constPool, outputInstructions);

			/*
			 * `
			 * if (TraceStatusStore._isToRecord("class%method")) {
			 * 		// tracing code
			 * } else {
			 * 		// original code
			 * }
			 * `
			 */
			InstructionHandle originalStartHandle = originalInstructions.getStart();
			outputInstructions.append(new IFEQ(originalStartHandle));
			outputInstructions.append(tracingInstructions);
			outputInstructions.append(originalInstructions);

			// update instructions
			methodGen.setInstructionList(outputInstructions);

			tracingInstructions.dispose();
		}

		originalInstructions.dispose();
		return changed;
	}
	
	private void loadControlBoolToStack(MethodGen methodGen, InstructionList instrList, ConstantPoolGen constPool) {
		/* load "class%method" to stack */
		String codeBlockKey = CodeBlockKeyIssuer.getKeyForMethod(methodGen);
		instrList.append(new PUSH(constPool, codeBlockKey));
	}

	private void insertInvocationOfStaticMethod(OnDemandTraceMethods staticMethod, ConstantPoolGen constPool,
			InstructionList instrList) {
		int staticMethodIndex = constPool.addMethodref(staticMethod.getDeclareClass(), staticMethod.getMethodName(),
				staticMethod.getMethodSign());
		instrList.append(new INVOKESTATIC(staticMethodIndex));
	}

}
