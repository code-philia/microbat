package microbat.tracerecov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.ArrayValue;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.StringValue;
import microbat.model.value.VarValue;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;

public class TraceRecoverer {

	private ExecutionSimulator executionSimulator;

	public TraceRecoverer() {
		this.executionSimulator = new ExecutionSimulator();
	}

	/**
	 * 
	 * note that: aliasID == heap address
	 * (aliasID *) => heap address is known
	 * {aliasID ?} => heap address is unknown
	 * 
	 * input: {@code currentStep}, {@code rootVar} (aliasID *) -> ... (aliasID ?) -> {@code targetVar} (aliasID ?)
	 * <p>
	 * output:  
	 * {@code rootVar} (aliasID *) -> ... (aliasID ?) -> {@code targetVar} (aliasID *)
	 * 
	 * in other words, we want to recover the heap address of the {@code targetVar} if the memory address is recorded in the trace;
	 * Otherwise, we create a new heap address (i.e., aliasID) for {@code targetVar}
	 * <p>
	 * Build a variable graph. Identify relevant steps and alias relationships in
	 * this process.
	 */
	public void recoverDataDependency(TraceNode currentStep, VarValue targetVar, VarValue rootVar) {

		Trace trace = currentStep.getTrace();
		List<VarValue> criticalVariables = createQueue(targetVar, rootVar);
		Set<String> variablesToCheck = getVariablesToCheck(criticalVariables);
		List<Integer> relevantSteps = new ArrayList<>();

		// determine scope of searching
		TraceNode scopeStart = determineScopeOfSearching(rootVar, trace, currentStep);
		if (scopeStart == null)
			return;
		int start = scopeStart.getOrder() + 1;
		int end = currentStep.getOrder();

		// alias and definition inferencing
		inferAliasRelations(trace, start, end, rootVar, criticalVariables, variablesToCheck, relevantSteps);
		inferDefinition(trace, rootVar, targetVar, criticalVariables, relevantSteps);
	}

	/**
	 * the first element is rootVar, and the last one is the direct parent of the
	 * targetVar.
	 */
	private List<VarValue> createQueue(VarValue targetVar, VarValue rootVar) {
		List<VarValue> list = new ArrayList<VarValue>();

		VarValue temp = targetVar;
		list.add(temp);

		// TODO consider more complicated graph scenarios
		while (temp != rootVar) {
			temp = temp.getParents().get(0);
			if (!list.contains(temp)) {
				list.add(temp);
			}
		}

		List<VarValue> list0 = new ArrayList<VarValue>();
		// modified by hongshu
		// Add target var to list: the address of target var should also be inferred
		for (int i = list.size() - 1; i >= 0; i--) {
			list0.add(list.get(i));
		}

		return list0;
	}

	/**
	 * add all the existing alias IDs
	 */
	private Set<String> getVariablesToCheck(List<VarValue> criticalVariables) {
		Set<String> variablesToCheck = new HashSet<>();

		for (VarValue criticalVar : criticalVariables) {
			String aliasID = criticalVar.getAliasVarID();
			if (aliasID != null && !aliasID.equals("0") && !aliasID.equals("")) {
				variablesToCheck.add(aliasID);
			}
		}

		return variablesToCheck;
	}

	/**
	 * search for data dominator of parentVar (skip return steps TODO: test more
	 * scenarios)
	 */
	private TraceNode determineScopeOfSearching(VarValue parentVar, Trace trace, TraceNode currentStep) {
		VarValue lastWrittenVariable = null;
		TraceNode scopeStart = currentStep;
		while (lastWrittenVariable == null) {

			scopeStart = trace.findProducer(parentVar, scopeStart);
			if (scopeStart == null) {
				break;
			}

			lastWrittenVariable = scopeStart.getWrittenVariables().stream()
					.filter(v -> v.getVarName() != null && !v.getVarName().contains("#")).findFirst().orElse(null);
		}
		return scopeStart;
	}

	/**
	 * Alias Inference: FORWARD ITERATION
	 * 
	 * iterate through steps in scope, infer address, add relevant variables to the
	 * set and the corresponding steps
	 */
	private void inferAliasRelations(Trace trace, int scopeStart, int scopeEnd, VarValue rootVar,
			List<VarValue> criticalVariables, Set<String> variablesToCheck, List<Integer> relevantSteps) {

		for (int i = scopeStart; i <= scopeEnd; i++) {
			TraceNode step = trace.getTraceNode(i);
			if (isRelevantStep(step, variablesToCheck)) {
				relevantSteps.add(i);

				if (isRequiringAliasInference(step, variablesToCheck)) {
					// INFER ADDERSS
					try {
						Map<VarValue, VarValue> fieldToVarOnTraceMap = this.executionSimulator.inferAliasRelations(step,
								rootVar, criticalVariables);

						for (VarValue writtenField : fieldToVarOnTraceMap.keySet()) {
							if (isCriticalVariable(criticalVariables, writtenField)) {
								VarValue variableOnTrace = fieldToVarOnTraceMap.get(writtenField);
								String aliasIdOfCriticalVar = variableOnTrace.getAliasVarID();

								updateAliasIDOfField(writtenField, variableOnTrace, criticalVariables);
								variablesToCheck.add(aliasIdOfCriticalVar);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				addVarSkeletonToVariablesOnTrace(step, criticalVariables);
			}
		}
	}

	/**
	 * Definition Inference: BACKWARD ITERATION
	 * 
	 * iterate through steps in scope, infer definition
	 */
	private void inferDefinition(Trace trace, VarValue rootVar, VarValue targetVar, List<VarValue> criticalVariables,
			List<Integer> relevantSteps) {

		int startIndex = relevantSteps.size() - 2; // skip self (last step)
		for (int i = startIndex; i >= 0; i--) {
			int stepOrder = relevantSteps.get(i);
			TraceNode step = trace.getTraceNode(stepOrder);
			if (step.isCallingAPI()) {
				// INFER DEFINITION STEP
				boolean def = this.executionSimulator.inferDefinition(step, rootVar, targetVar, criticalVariables);

				if (def && !step.getWrittenVariables().contains(targetVar)) {
					step.getWrittenVariables().add(targetVar);
					break;
				}
			}
		}
	}

	/**
	 * only check steps containing variablesToCheck AND are calling API
	 */
	private boolean isRelevantStep(TraceNode step, Set<String> variablesToCheck) {
		Set<VarValue> variablesInStep = step.getAllVariables();
		Set<String> aliasIDsInStep = new HashSet<>(variablesInStep.stream().map(v -> v.getAliasVarID()).toList());

		return aliasIDsInStep.stream().anyMatch(id -> variablesToCheck.contains(id));
	}

	/**
	 * only infer address when there are new variables at the current step.
	 * 
	 * TODO: consider edge cases
	 */
	private boolean isRequiringAliasInference(TraceNode step, Set<String> variablesToCheck) {
		Set<VarValue> variablesInStep = step.getAllVariables();
		Set<String> aliasIDsInStep = new HashSet<>(variablesInStep.stream().map(v -> v.getAliasVarID()).toList());

		int numOfNewVars = 0;
		for (String id : aliasIDsInStep) {
			if (!variablesToCheck.contains(id)) {
				numOfNewVars++;
			}
		}
		return numOfNewVars > 0;
	}

	private boolean isCriticalVariable(List<VarValue> criticalVariables, VarValue variable) {
		String varID = variable.getVarID();
		return criticalVariables.stream().anyMatch(v -> v.getVarID().equals(varID));
	}

	private void updateAliasIDOfField(VarValue writtenField, VarValue variableOnTrace,
			List<VarValue> criticalVariables) {
		VarValue targetField = null;
		VarValue targetVarOnTrace = variableOnTrace;
		for (VarValue var : criticalVariables) {
			if (targetField == null) {
				if (var.equals(writtenField)) {
					targetField = var;
					targetField.setAliasVarID(targetVarOnTrace.getAliasVarID());
				}
			} else {
				targetField = var;
				String currentTargetName = targetField.getVarName();

				if (targetVarOnTrace != null) {
					targetVarOnTrace = targetVarOnTrace.getChildren().stream()
							.filter(v -> v.getVarName().equals(currentTargetName)).findFirst().orElse(null);
				}

				if (targetVarOnTrace != null) {
					targetField.setAliasVarID(targetVarOnTrace.getAliasVarID());
				} else {
					// reset children field IDs
					targetField.setAliasVarID("");
				}

			}
		}
	}

	private void addVarSkeletonToVariablesOnTrace(TraceNode step, List<VarValue> criticalVariables) {
		for (VarValue readVar : step.getReadVariables()) {
			String aliasID = readVar.getAliasVarID();
			VarValue criticalVar = null;
			for (VarValue var : criticalVariables) {
				if (criticalVar == null) {
					if (aliasID.equals(var.getAliasVarID())) {
						criticalVar = var;
					}
				} else {
					VarValue varCopy = null;
					if (var instanceof ArrayValue) {
						varCopy = new ArrayValue(false, var.isRoot(), var.getVariable());
					} else if (var instanceof ReferenceValue) {
						varCopy = new ReferenceValue(false, var.isRoot(), var.getVariable());
					} else if (var instanceof StringValue) {
						varCopy = new StringValue(VarValue.VALUE_TBD, var.isRoot(), var.getVariable());
					} else if (var instanceof PrimitiveValue) {
						varCopy = new PrimitiveValue(VarValue.VALUE_TBD, var.isRoot(), var.getVariable());
					}

					if (varCopy != null) {
						varCopy.setStringValue(VarValue.VALUE_TBD);
						readVar.addChild(varCopy);
						varCopy.addParent(readVar);
						readVar = varCopy;
					}
				}
			}
		}
	}
}
