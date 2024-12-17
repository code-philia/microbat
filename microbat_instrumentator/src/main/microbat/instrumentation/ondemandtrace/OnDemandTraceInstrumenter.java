package microbat.instrumentation.ondemandtrace;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.TraceInstrumenter;

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

			/* `boolean instrumentationSwitch = true;` */
			String booleanVarName = "instrumentationSwitch";
			LocalVariableGen boolVar = methodGen.addLocalVariable(booleanVarName, Type.BOOLEAN, null, null);
			InstructionHandle startOfVarScope = outputInstructions.append(new ICONST(1)); // 1: true
			outputInstructions.append(new ISTORE(boolVar.getIndex()));
			InstructionHandle endOfVarScope = outputInstructions.append(new ILOAD(boolVar.getIndex()));

			boolVar.setStart(startOfVarScope);
			boolVar.setEnd(endOfVarScope);

			/* `if (instrumentationSwitch) { tracing code } else { original code }` */
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

}
