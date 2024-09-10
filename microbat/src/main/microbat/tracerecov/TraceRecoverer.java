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
import microbat.model.variable.FieldVar;
import microbat.model.variable.Variable;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.tracerecov.executionsimulator.ExecutionSimulatorFactory;

public class TraceRecoverer {

	private ExecutionSimulator executionSimulator;

	public TraceRecoverer() {
		this.executionSimulator = ExecutionSimulatorFactory.getExecutionSimulator();
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

		// determine scope of searching
		TraceNode scopeStart = determineScopeOfSearching(criticalVariables, trace, currentStep);
		if (scopeStart == null)
			return;
		int start = scopeStart.getOrder() + 1;
		int end = currentStep.getOrder() - 1;

		// alias inference
		inferAliasRelations(trace, start, end, rootVar, criticalVariables, variablesToCheck);

		// update scope of searching
		scopeStart = determineScopeOfSearching(criticalVariables, trace, currentStep);
		if (scopeStart == null)
			return;
		start = scopeStart.getOrder() + 1;
		end = currentStep.getOrder() - 1;

		// definition inference
		inferDefinition(trace, start, end, rootVar, targetVar, criticalVariables, variablesToCheck);
//		inferDefinition(trace, start, end, rootVar, targetVar, criticalVariables);
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
			if (isValidAliasID(aliasID)) {
				variablesToCheck.add(aliasID);
			}
		}

		return variablesToCheck;
	}

	private boolean isValidAliasID(String aliasID) {
		return aliasID != null && !aliasID.equals("0") && !aliasID.equals("");
	}

	/**
	 * search for data dominator of parentVar (skip return steps TODO: test more
	 * scenarios)
	 */
	private TraceNode determineScopeOfSearching(VarValue variable, Trace trace, TraceNode currentStep) {
		VarValue lastWrittenVariable = null;
		TraceNode scopeStart = currentStep;
		while (lastWrittenVariable == null) {

			scopeStart = trace.findProducer(variable, scopeStart);
			if (scopeStart == null) {
				break;
			}

			lastWrittenVariable = scopeStart.getWrittenVariables().stream()
					.filter(v -> v.getVarName() != null && !v.getVarName().contains("#")).findFirst().orElse(null);
		}
		return scopeStart;
	}

	private TraceNode determineScopeOfSearching(List<VarValue> criticalVariables, Trace trace, TraceNode currentStep) {
		int start = currentStep.getOrder();
		for (VarValue variable : criticalVariables) {
			if (variable == null || variable.getAliasVarID() == null || variable.getAliasVarID().equals("")
					|| variable.getAliasVarID().equals("0")) {
				continue;
			}
			TraceNode producer = determineScopeOfSearching(variable, trace, currentStep);
			if (producer != null && producer.getOrder() < start) {
				start = producer.getOrder();
			}
		}
		return trace.getTraceNode(start);
	}

	/**
	 * Alias Inference: FORWARD ITERATION
	 * 
	 * iterate through steps in scope, infer address, add relevant variables to the
	 * set and the corresponding steps
	 */
	private void inferAliasRelations(Trace trace, int scopeStart, int scopeEnd, VarValue rootVar,
			List<VarValue> criticalVariables, Set<String> variablesToCheck) {

		for (int i = scopeStart; i <= scopeEnd; i++) {
			TraceNode step = trace.getTraceNode(i);
			if (isRelevantStep(step, variablesToCheck) && isRequiringAliasInference(step, variablesToCheck)
					&& isStepToCheck(step)) {
				// INFER ADDERSS
				try {
					Map<VarValue, VarValue> fieldToVarOnTraceMap = this.executionSimulator.inferAliasRelations(step,
							rootVar, criticalVariables);

					for (VarValue writtenField : fieldToVarOnTraceMap.keySet()) {
						if (isCriticalVariable(criticalVariables, writtenField)) {
							VarValue variableOnTrace = fieldToVarOnTraceMap.get(writtenField);
							String aliasIdOfCriticalVar = variableOnTrace.getAliasVarID();

							if (isValidAliasID(aliasIdOfCriticalVar)) {
								updateAliasIDOfField(writtenField, variableOnTrace, criticalVariables);
								variablesToCheck.add(aliasIdOfCriticalVar);
							}
						} else {
							/*
							 * key and value: variable on trace. Field in variable is not recorded.
							 */
							if (isValidAliasID(writtenField.getAliasVarID())) {
								variablesToCheck.add(writtenField.getAliasVarID());
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
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
	private void inferDefinition(Trace trace, int start, int end, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables, Set<String> variablesToCheck) {

		for (int i = end; i >= start; i--) {
			TraceNode step = trace.getTraceNode(i);
			if (isRelevantStep(step, variablesToCheck) && isStepToCheck(step)) {
				// INFER DEFINITION STEP
				boolean def = this.executionSimulator.inferDefinition(step, rootVar, targetVar, criticalVariables);

				if (def && !step.getWrittenVariables().contains(targetVar)) {
					step.getWrittenVariables().add(targetVar);
					break;
				}
			}
		}
	}
	
	private void inferDefinition(Trace trace, int start, int end, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables) {

		for (int i = end; i >= start; i--) {
			TraceNode step = trace.getTraceNode(i);
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

	private boolean isStepToCheck(TraceNode step) {
		return step.isCallingAPI() && step.getInvokingMethod() != null && !step.getInvokingMethod().equals("%");
	}

	/**
	 * only infer address when there are new variables at the current step.
	 * 
	 * TODO: consider edge cases
	 */
	private boolean isRequiringAliasInference(TraceNode step, Set<String> variablesToCheck) {
		Set<VarValue> variablesInStep = step.getAllVariables();

		int numOfNewVars = 0;
		for (VarValue varInStep : variablesInStep) {
			if (!variablesToCheck.contains(varInStep.getAliasVarID())
					&& !varInStep.getVarName().contains("ConditionResult")) {
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
					if (targetVarOnTrace.getAliasVarID().equals(targetField.getAliasVarID())) {
						break;
					}
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
			if (aliasID == null) {
				continue;
			}
			for (VarValue var : criticalVariables) {
				if (criticalVar == null) {
					if (aliasID.equals(var.getAliasVarID())) {
						criticalVar = var;
					}
				} else {
					VarValue varValueCopy = null;
					Variable variableCopy = new FieldVar(var.isStatic(), var.getVarName(), var.getType(),
							var.getType());
					if (var instanceof ArrayValue) {
						varValueCopy = new ArrayValue(false, var.isRoot(), variableCopy);
					} else if (var instanceof ReferenceValue) {
						varValueCopy = new ReferenceValue(false, var.isRoot(), variableCopy);
					} else if (var instanceof StringValue) {
						varValueCopy = new StringValue(VarValue.VALUE_TBD, var.isRoot(), variableCopy);
					} else if (var instanceof PrimitiveValue) {
						varValueCopy = new PrimitiveValue(VarValue.VALUE_TBD, var.isRoot(), variableCopy);
					}

					if (varValueCopy != null) {
						varValueCopy.setStringValue(VarValue.VALUE_TBD);
						varValueCopy.setVarID(readVar.getVarID() + "." + varValueCopy.getVarName());
						varValueCopy.setAliasVarID(var.getAliasVarID());

						readVar.updateChild(varValueCopy);
						readVar = varValueCopy;
					}
				}
			}
		}
	}
}
