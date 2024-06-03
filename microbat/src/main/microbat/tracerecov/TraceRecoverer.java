package microbat.tracerecov;

import java.util.ArrayList;
import java.util.List;

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
		
		
		/**
		 * the first element is rootVar, and the last one is the direct parent of the targetVar. 
		 */
		List<VarValue> criticalVariables = createQueue(targetVar, rootVar);
		
		for(VarValue criticalVar: criticalVariables) {
			
			if(criticalVar.getAliasVarID() == null || 
					criticalVar.getAliasVarID().equals("") || 
					criticalVar.getAliasVarID().equals("0")) {
				inferAddress(trace, rootVar, criticalVar);
			}
			
			List<TraceNode> definitingSteps = parseDefiningStep(criticalVar, targetVar, trace, currentStep);
			
			for(TraceNode step: definitingSteps) {
				if(!step.getWrittenVariables().contains(criticalVar)) {
					step.getWrittenVariables().add(criticalVar);
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
		

		/**
		 * we might start with the root variable
		 */
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

	private void inferAddress(Trace trace, VarValue rootVar, VarValue criticalVar) {
		// TODO Auto-generated method stub
		System.currentTimeMillis();
	}

	private List<TraceNode> parseDefiningStep(VarValue parentVar, VarValue targetVar, Trace trace, TraceNode currentStep) {
		
		List<TraceNode> definingSteps = new ArrayList<TraceNode>();
		
		VarValue lastWrittenVariable = null;
		TraceNode scopeStart = currentStep;
		while (lastWrittenVariable == null) {
			scopeStart = trace.findDataDependency(scopeStart, parentVar);
			if(scopeStart == null) {
				break;
			}
			
			lastWrittenVariable = scopeStart.getWrittenVariables().stream()
					.filter(v -> v.getVarName() != null && !v.getVarName().contains("#")).findFirst().orElse(null);
		}
		
		if(scopeStart == null) return definingSteps;

		int start = scopeStart.getOrder();
		int end = currentStep.getOrder();

		/* build variable graph */
		for (int i = start; i <= end; i++) {
			TraceNode step = trace.getTraceNode(i);

			List<VarValue> variables = new ArrayList<>();
			variables.addAll(step.getReadVariables());
			variables.addAll(step.getWrittenVariables());

			/**
			 * if {@code step} has a written/read variable equal to {@code parentVar}
			 */
//			VarValue var = variables.stream().filter(v -> variables.contains(v).findAny().orElse(null);
			if (variables.contains(parentVar)) {
				
				if(step.isCallingAPI()) {
					
					boolean def = this.executionSimulator.inferDefinition(step, parentVar, targetVar);
					if(def) {
						definingSteps.add(step);
					}
					
//					this.executionSimulator.inferenceAliasRelations(step, parentVar);
				}
				
				
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
			} 
		}

		return definingSteps;
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
