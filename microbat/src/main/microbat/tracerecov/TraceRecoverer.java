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

		/*
		 * the first element is rootVar, and the last one is the direct parent of the
		 * targetVar.
		 */
		List<VarValue> criticalVariables = createQueue(targetVar, rootVar);

		Set<String> variablesToCheck = new HashSet<>();
		variablesToCheck.add(rootVar.getAliasVarID());

		List<Integer> relevantSteps = new ArrayList<>();

		/**
		 * 1. Alias Inferencing
		 */
		// determine scope of searching
		TraceNode scopeStart = determineScopeOfSearching(rootVar, trace, currentStep);
		if (scopeStart == null)
			return;
		int start = scopeStart.getOrder() + 1;
		int end = currentStep.getOrder();

		// infer alias relations
		inferAliasRelations(trace, start, end, rootVar, criticalVariables, variablesToCheck, relevantSteps);

		/**
		 * 2. Definition Inferencing
		 */
		// update scope of searching
		scopeStart = determineScopeOfSearching(rootVar, trace, currentStep);
		if (scopeStart == null)
			return;
		start = scopeStart.getOrder();
		int endIndex = relevantSteps.size() - 2; // skip the last step (self)

		// infer definition
		inferDefinition(trace, start, endIndex, rootVar, targetVar, criticalVariables, relevantSteps);
	}

	private TraceNode determineScopeOfSearching(VarValue parentVar, Trace trace, TraceNode currentStep) {
		// search for data dominator of parentVar (skip return steps TODO: test more
		// scenarios)
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

	private void inferAliasRelations(Trace trace, int scopeStart, int scopeEnd, VarValue rootVar,
			List<VarValue> criticalVariables, Set<String> variablesToCheck, List<Integer> relevantSteps) {
		// FORWARD ITERATION
		// iterate through steps in scope, infer address and add relevant variables to
		// the set
		for (int i = scopeStart; i <= scopeEnd; i++) {
			TraceNode step = trace.getTraceNode(i);

			Set<VarValue> variablesInStep = step.getAllVariables();

			// only check steps containing recovered fields in rootVar AND calling API
			boolean isRelevantStep = variablesInStep.stream()
					.anyMatch(v -> variablesToCheck.contains(v.getAliasVarID()));
			if (isRelevantStep) {

				relevantSteps.add(i);

				// INFER ADDERSS if there are some other variables
				if (variablesInStep.size() > 1) {
					try {
						// Return a map with key: written_field, value: variable_on_trace
						Map<VarValue, VarValue> fieldsWithAddressRecovered = this.executionSimulator
								.inferAliasRelations(step, rootVar, criticalVariables);

						for (VarValue writtenField : fieldsWithAddressRecovered.keySet()) {
							String writtenFieldID = writtenField.getVarID();
							boolean isCriticalVariable = criticalVariables.stream()
									.anyMatch(v -> v.getVarID().equals(writtenFieldID));

							if (isCriticalVariable) {
								// add critical variable to the set (to be checked later)
								VarValue variableOnTrace = fieldsWithAddressRecovered.get(writtenField);
								String aliasIdOfLinkedVar = variableOnTrace.getAliasVarID();
								variablesToCheck.add(aliasIdOfLinkedVar);

								// link address
								writtenField.setAliasVarID(aliasIdOfLinkedVar);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}

	private void addAliasIDsToDefinitionStep(TraceNode step, List<VarValue> criticalVariables, VarValue targetValue) {
		for (VarValue readVar : step.getReadVariables()) {
			String aliasID = readVar.getAliasVarID();
			VarValue criticalVar = null;
			for (VarValue var : criticalVariables) {
				if (var.equals(targetValue)) {
					break;
				}
				if (criticalVar == null) {
					if (aliasID.equals(var.getAliasVarID())) {
						criticalVar = var;
					}
				} else {
					VarValue varCopy = null;
					if (var instanceof ArrayValue) {
						varCopy = new ArrayValue(false, var.isRoot(), var.getVariable());
						varCopy.setStringValue(VarValue.VALUE_TBD);
					} else if (var instanceof ReferenceValue) {
						varCopy = new ReferenceValue(false, var.isRoot(), var.getVariable());
						varCopy.setStringValue(VarValue.VALUE_TBD);
					} else if (var instanceof StringValue) {
						varCopy = new StringValue(VarValue.VALUE_TBD, var.isRoot(), var.getVariable());
					} else if (var instanceof PrimitiveValue) {
						varCopy = new PrimitiveValue(VarValue.VALUE_TBD, var.isRoot(), var.getVariable());
					}

					if (varCopy != null) {
						readVar.addChild(varCopy);
						varCopy.addParent(readVar);
						readVar = varCopy;
					}
				}
			}
		}
	}

	private void inferDefinition(Trace trace, int scopeStart, int scopeEnd, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables, List<Integer> relevantSteps) {
		// BACKWARD ITERATION
		// iterate through steps in scope, infer definition
		for (int i = scopeEnd; i > scopeStart; i--) {
			int stepOrder = relevantSteps.get(i);
			TraceNode step = trace.getTraceNode(stepOrder);

			if (step.isCallingAPI()) {
				// INFER DEFINITION STEP
				boolean def = this.executionSimulator.inferDefinition(step, rootVar, targetVar, criticalVariables);
				if (def) {
					addAliasIDsToDefinitionStep(step, criticalVariables, targetVar);
					if (!step.getWrittenVariables().contains(targetVar)) {
						step.getWrittenVariables().add(targetVar);
					}
					break;
				}
			}
		}
	}

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

}
