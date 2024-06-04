package microbat.tracerecov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
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
	public void recoverDataDependency(Trace trace, TraceNode currentStep, VarValue targetVar, VarValue rootVar) {
		
//		/**
//		 * the first element is rootVar, and the last one is the direct parent of the targetVar. 
//		 */
//		List<VarValue> criticalVariables = createQueue(targetVar, rootVar);
//		
//		for(VarValue criticalVar: criticalVariables) {
//			
//			if(criticalVar.getAliasVarID() == null || 
//					criticalVar.getAliasVarID().equals("") || 
//					criticalVar.getAliasVarID().equals("0")) {
////				 inferAddress(rootVar, trace, currentStep);
//			}
//			
//			List<TraceNode> definitingSteps = parseDefiningStep(criticalVar, targetVar, trace, currentStep);
//			
//			for(TraceNode step: definitingSteps) {
//				if(!step.getWrittenVariables().contains(criticalVar)) {
//					step.getWrittenVariables().add(criticalVar);
//				}
//			}
//		}
		
		
		Set<String> variablesToCheck = new HashSet<>();
		variablesToCheck.add(rootVar.getAliasVarID());
		
		// determine scope of searching
		TraceNode scopeStart = determineScopeOfSearching(rootVar, trace, currentStep);
		if(scopeStart == null) return;
		int start = scopeStart.getOrder();
		int end = currentStep.getOrder();
		
		// iterate through steps in scope, infer address and add relevant variables to the set
		for (int i = start; i <= end; i++) {
			TraceNode step = trace.getTraceNode(i);

			List<VarValue> variables = new ArrayList<>();
			variables.addAll(step.getReadVariables());
			variables.addAll(step.getWrittenVariables());
			
			// only check steps containing recovered fields in rootVar AND calling API
			boolean isRelevantStep = variables.stream().anyMatch(v -> variablesToCheck.contains(v.getAliasVarID()));
			if (isRelevantStep && step.isCallingAPI()) {
				// if there are some other variables, INFER ADDERSS
				if (variables.size() > 1) {
					try {
						// add relevant fields to the set (to be checked later)
						// TODO: change rootVar to be variable at this step
						Set<String> fieldsWithAddressRecovered = inferAddress(rootVar, step);
						variablesToCheck.addAll(fieldsWithAddressRecovered);
						
						// TODO: expand field with recovered address via LLM
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				// INFER DEFINITION STEP
				if (step.isCallingAPI()) {
					// TODO: change rootVar to be variable at this step
					// TODO: include relationship between variables
					boolean def = parseDefiningStep(rootVar, targetVar, step);
					if (def && !step.getWrittenVariables().contains(rootVar)) {
							step.getWrittenVariables().add(rootVar);
					}
				}
				
			}
		}
		
		
		
//		System.out.println("***Relevant Steps***");
//
//		/* initialize graph */
//		
//		
//		VariableGraph.reset();
//		VariableGraph.addVar(rootVar);
//
//		/** determine the scope of recovery */
//		// search for last written step other than a return step
//		VarValue lastWrittenVariable = null;
//		TraceNode scopeStart = currentStep;
//		while (lastWrittenVariable == null) {
//			scopeStart = trace.findDataDependency(scopeStart, rootVar);
//			lastWrittenVariable = scopeStart.getWrittenVariables().stream()
//					.filter(v -> v.getVarName() != null && !v.getVarName().contains("#")).findFirst().orElse(null);
//		}
//
//		int start = scopeStart.getOrder();
//		int end = currentStep.getOrder();
//
//		/* build variable graph */
//		for (int i = start; i <= end; i++) {
//			TraceNode step = trace.getTraceNode(i);
//
//			List<VarValue> variables = new ArrayList<>();
//			variables.addAll(step.getReadVariables());
//			variables.addAll(step.getWrittenVariables());
//
//			VarValue varInGraph = variables.stream().filter(v -> VariableGraph.containsVar(v)).findAny().orElse(null);
//			if (varInGraph != null) {
//				/* relevant steps identification */
//				VariableGraph.addRelevantStep(step);
//
//				/* alias inferencing */
//				List<String> validAddresses = variables.stream().map(v -> v.getAliasVarID()).toList();
//				Set<String> validAddressSet = new HashSet<>(validAddresses);
//				if (VariableGraph.isStepToConsider(step) && validAddressSet.size() > 1) {
//					try {
//						this.executionSimulator.inferenceAliasRelations(step, rootVar);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			} else {
//				continue;
//			}
//		}
//
//		return null;
		

//		/**
//		 * we might start with the root variable
//		 */
//		for (VarValue parent : parents) {
//			List<TraceNode> candidateSteps = parseCandidateReadingSteps(trace, parent);
//			for (TraceNode step : candidateSteps) {
//				if (isWritingTarget(step)) {
//					step.addWrittenVariable(targetVar);
//				}
//			}
//		}

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
	
	private TraceNode determineScopeOfSearching(VarValue parentVar, Trace trace, TraceNode currentStep) {
		// search for data dominator of parentVar (skip return steps TODO: test more scenarios)
		VarValue lastWrittenVariable = null;
		TraceNode scopeStart = currentStep;
		while (lastWrittenVariable == null) {
			scopeStart = trace.findDataDependency(scopeStart, parentVar);
			if(scopeStart == null) {
				break;
			}
		
			lastWrittenVariable = scopeStart.getWrittenVariables().stream().filter(v -> v.getVarName() != null && !v.getVarName().contains("#")).findFirst().orElse(null);
		}
		return scopeStart;
	}

	private Set<String> inferAddress(VarValue variable, TraceNode step) throws IOException {
		Set<VarValue> variables = this.executionSimulator.inferenceAliasRelations(step, variable);
		Set<String> ids = new HashSet<>();
		for (VarValue v : variables) {
			ids.add(v.getAliasVarID());
		}
		return ids;
	}

	private boolean parseDefiningStep(VarValue parentVar, VarValue targetVar, TraceNode step) {
		return this.executionSimulator.inferDefinition(step, parentVar, targetVar);
	}

	private List<VarValue> createQueue(VarValue targetVar, VarValue rootVar) {
		List<VarValue>  list = new ArrayList<VarValue>();
		
		VarValue temp = targetVar;
		list.add(temp);
		
		//TODO consider more complicated graph scenarios
		while(temp != rootVar) {
			
			temp = temp.getParents().get(0);
			if(!list.contains(temp)) {
				list.add(temp);
			}
			
		}
		
		List<VarValue> list0 = new ArrayList<VarValue>();
		for(int i=list.size()-1; i>=1; i--) {
			list0.add(list.get(i));
		}
		
		return list0;
	}


	
}
