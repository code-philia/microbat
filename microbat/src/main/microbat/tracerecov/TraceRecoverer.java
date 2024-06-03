package microbat.tracerecov;

import java.util.ArrayList;
import java.util.List;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

public class TraceRecoverer {
	/**
	 * @author hongshuwang
	 */
	public void recoverDataDependency(Trace trace, TraceNode currentStep, VarValue targetVar, VarValue rootVar) {
		List<VarValue> parents = parseParentVariables(trace, currentStep, targetVar, rootVar);

		/**
		 * we might start with the root variable
		 */
		for (VarValue parent : parents) {
			List<TraceNode> candidateSteps = parseCandidateReadingSteps(trace, parent);
			for (TraceNode step : candidateSteps) {
				if (isWritingTarget(step)) {
					step.addWrittenVariable(targetVar);
				}
			}
		}

//		/* 3. Execution Simulation */
//		/*
//		 * Identify additional linking steps and simulate execution by calling LLM
//		 * model.
//		 */
//		try {
//			
//			executionSimulator.recoverLinkageSteps();
//			executionSimulator.sendRequests();
//		} catch (IOException ioException) {
//			ioException.printStackTrace();
//		}

	}

	private List<TraceNode> parseCandidateReadingSteps(Trace trace, VarValue parent) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<VarValue> parseParentVariables(Trace trace, TraceNode currentStep, VarValue targetVar,
			VarValue rootVar) {

		/* initialize graph */
		VariableGraph.reset();
		VariableGraph.addVar(rootVar);

		/* determine the scope of recovery */
		// search for last written step other than a return step
		VarValue lastWrittenVariable = null;
		TraceNode scopeStart = currentStep;
		while (lastWrittenVariable == null) {
			scopeStart = trace.findDataDependency(scopeStart, rootVar);
			lastWrittenVariable = scopeStart.getWrittenVariables().stream()
					.filter(v -> v.getVarName() != null && !v.getVarName().contains("#")).findFirst().orElse(null);
		}

		int start = scopeStart.getOrder();
		int end = currentStep.getOrder();

		/* build variable graph */
		for (int i = start; i <= end; i++) {
			TraceNode step = trace.getTraceNode(i);

			List<VarValue> variables = new ArrayList<>();
			variables.addAll(step.getReadVariables());
			variables.addAll(step.getWrittenVariables());

			VarValue varInGraph = variables.stream().filter(v -> VariableGraph.containsVar(v)).findAny().orElse(null);
			if (varInGraph != null) {
				/* identify relevant steps */
				VariableGraph.addRelevantStep(step);

				/* variable mapping */
				VariableGraph.mapVariable(step);
			} else {
				continue;
			}
		}

		return null;
	}

	private boolean isWritingTarget(TraceNode step) {
		// TODO Auto-generated method stub
		return false;
	}

	private TraceNode searchTrueDefinition(VarValue targetStep, List<TraceNode> candidateDefinitions) {
		// TODO Auto-generated method stub
		return null;
	}
}
