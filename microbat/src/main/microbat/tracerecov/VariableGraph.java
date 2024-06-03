package microbat.tracerecov;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.tracerecov.varmapping.VariableMapper;

/**
 * This class acts as a graph of variables that is built while traversing along
 * the execution trace.
 * 
 * @author hongshuwang
 */
public class VariableGraph {
	
//	/**
//	 * heap address --> var value
//	 */
//	private static Set<VarValue> graph = new HashSet<>();
//	private static List<TraceNode> potentialLinkageSteps = new ArrayList<>();
//	private static String lastVisitedID = null;
//
//	/* public methods */
//
//	public static void reset() {
//		graph = new HashSet<>();
//		potentialLinkageSteps = new ArrayList<>();
//		lastVisitedID = null;
//	}
//
//	public static void addRelevantStep(TraceNode step) {
//		// doesn't invoke methods but reads varValue
//		// OR step without expandable method calls 
//		if ((step.isCallingAPI() && !(step.getStepOverPrevious() != null
//				&& !step.getStepOverPrevious().getInvocationChildren().isEmpty())) || isStepToConsider(step)) {
//
//			System.out.println();
//			System.out.println("Relevant Step: " + step.getOrder());
//			
//			List<VarValue> variables = new ArrayList<>();
//			variables.addAll(step.getReadVariables());
//			variables.addAll(step.getWrittenVariables());
//
//			for (VarValue var : variables) {
//				if (!TraceRecovUtils.isPrimitiveType(var.getType()) && !var.getVarName().equals("this")) {
//					getVar(var).addRelevantStep(step);
//					System.out.println("Relevant Variable: " + var.getType() + " " + var.getVarName());
//				}
//			}
//		}
//	}
//
//	/**
//	 * Map variable on trace to one of the candidate variables.
//	 * 
//	 * 1. Identify candidate variable through bytecode analysis.
//	 * 2. Find corresponding variable on trace. If the variable is 
//	 *    not in the graph, add it to the graph.
//	 * 3. Link variable on trace to candidate variable.
//	 */
//	public static void mapVariable(TraceNode step) {
//		if (!isStepToConsider(step)) {
//			return;
//		}
//
////		List<VarValue> variables = step.getReadVariables();
////
////		List<String> methods = getValidInvokingMethods(step);
////
////		VarValue returnedVariable = null;
////
////		for (String invokingMethod : methods) {
////			String type = invokingMethod.split("#")[0];
////			VarValue parentVar = variables.stream().filter(v -> v.getType().equals(type)).findFirst().orElse(null);
////			if (parentVar == null) {
////				continue;
////			}
////
////			/* 1. Identify candidate variable */
////			String returnedField = VariableMapper.getReturnedField(invokingMethod);
////			if (returnedField != null) {
////				String fieldName = returnedField.split("#")[0];
////				String fieldType = returnedField.split("#")[1];
////
////				if (TraceRecovUtils.isCompositeType(fieldType)) {
////					/* 2. Find corresponding variable on trace */
////					returnedVariable = step.getWrittenVariables().stream().filter(v -> v.getType().equals(fieldType))
////							.findFirst().orElse(null);
////					if (returnedVariable != null) {
////						VariableGraphNode variableOnTrace = getVar(returnedVariable);
////
////						/* 3. Link variables */
////						VariableGraphNode parentVariable = getVar(parentVar);
////						parentVariable.addChild(fieldName, variableOnTrace);
////						variableOnTrace.setParent(parentVariable);
////
////						for (VarValue var : variables) {
////							getVar(var).removeRelevantStep(step);
////						}
////						linkageSteps.add(step);
////					}
////				}
////			}
////		}
////
////		/* record current step as potential linkage step */
////		if (!methods.isEmpty() && returnedVariable == null && !linkageSteps.contains(step)) {
////			potentialLinkageSteps.add(step);
////		}
//
//		potentialLinkageSteps.add(step);
//	}
//
//	public static List<TraceNode> getPotentialLinkageSteps() {
//		return potentialLinkageSteps;
//	}
//
//	/**
//	 * If the graph hasn't been visited, return the deepest layer. Otherwise, return
//	 * the parent of the last visited node.
//	 */
//	public static String getNextNodeIDToVisit() {
//		if (lastVisitedID == null) {
//			for (String key : graph.keySet()) {
//				if (!graph.get(key).hasChildren()) {
//					lastVisitedID = key;
//					return key;
//				}
//			}
//		} else {
//			VariableGraphNode parent = graph.get(lastVisitedID).getParent();
//			if (parent == null) {
//				return null;
//			}
//			String parentID = parent.getAliasID();
//			lastVisitedID = parentID;
//			return parentID;
//		}
//
//		return null;
//	}
//
//	public static void addCurrentToParentVariables() {
//		if (lastVisitedID == null) {
//			return;
//		}
//		graph.get(lastVisitedID).addSelfToParentSteps();
//	}
//
//	public static void linkVariables(TraceNode step, VarValue var1, VarValue var2, String field1, String field2) {
//		if (!containsVar(var1) || !containsVar(var2)) {
//			return;
//		}
//
//		VariableGraphNode varNode1 = getVar(var1);
//		VariableGraphNode varNode2 = getVar(var2);
//		String[] fields1 = field1.split("\\.");
//		String[] fields2 = field2.split("\\.");
//
//		if (fields1[0] == fields2[0] || fields1[0].equals(fields2[0])) {
//			return;
//		}
//		
//		int i = 1;
//		while (i <= Math.min(fields1.length, fields2.length)) {
//			int i1 = fields1.length - i;
//			int i2 = fields2.length - i;
//			String f1 = fields1[i1];
//			String f2 = fields2[i2];
//			if (f1 == f2 || f1.equals(f2)) {
//				i++;
//				continue;
//			} else if (i1 == 0) {
//				// var2 is the parent
//				VariableGraphNode field = varNode2.getFieldWithName(fields2[i2]);
//				if (field == null) {
//					StringBuilder nameBuilder = new StringBuilder();
//					for (int j = 1; j <= i2; j++) {
//						nameBuilder.append(fields2[j]);
//						if (j < i2) {
//							nameBuilder.append(".");
//						}
//					}
//					varNode2.addChild(nameBuilder.toString(), varNode1);
//					varNode1.setParent(varNode2);
//
//					varNode2.removeRelevantStep(step);
//					varNode1.removeRelevantStep(step);
//				}
//			} else if (i2 == 0) {
//				// var1 is the parent
//				VariableGraphNode field = varNode1.getFieldWithName(fields1[i1]);
//				if (field == null) {
//					StringBuilder nameBuilder = new StringBuilder();
//					for (int j = 1; j <= i1; j++) {
//						nameBuilder.append(fields1[j]);
//						if (j < i1) {
//							nameBuilder.append(".");
//						}
//					}
//					varNode1.addChild(nameBuilder.toString(), varNode2);
//					varNode2.setParent(varNode1);
//
//					varNode1.removeRelevantStep(step);
//					varNode2.removeRelevantStep(step);
//				}
//			}
//			break;
//		}
//	}
//
//	public static List<TraceNode> getRelevantSteps(String ID) {
//		if (!containsVar(ID)) {
//			return new ArrayList<>();
//		}
//		return graph.get(ID).getRelevantSteps();
//	}
//
//	public static boolean hasChildren(String ID) {
//		if (!containsVar(ID)) {
//			return false;
//		}
//		return graph.get(ID).hasChildren();
//	}
//
//	public static List<String> getCandidateVariables(String ID) {
//		if (!containsVar(ID)) {
//			return new ArrayList<>();
//		}
//		return graph.get(ID).getCandidateVariables();
//	}
//
//	public static boolean containsVar(VarValue varValue) {
//		return containsVar(getID(varValue));
//	}
//
//	public static void addVar(VarValue varValue) {
//		graph.put(getID(varValue), new VariableGraphNode(varValue));
//	}
//	
//	public static boolean isStepToConsider(TraceNode step) {
//		return step.getInvocationChildren().isEmpty()
//				&& (step.getStepOverPrevious() == null || step.getStepOverPrevious().getInvocationChildren().isEmpty());
//	}
//
//	/* private methods */
//
//	/**
//	 * Return all invoking methods other than the expanded ones.
//	 */
//	private static List<String> getValidInvokingMethods(TraceNode step) {
//		String invokingMethod = step.getInvokingMethod();
//		String[] methods = invokingMethod.split("%");
//		List<String> validMethods = new ArrayList<String>();
//
//		// get expanded method
//		TraceNode invokeParent = null;
//		if (!step.getInvocationChildren().isEmpty()) {
//			invokeParent = step;
//		} else {
//			TraceNode previous = step.getStepOverPrevious();
//			if (previous != null && !previous.getInvocationChildren().isEmpty()) {
//				invokeParent = previous;
//			}
//		}
//		String expandedMethodCall = null;
//		if (invokeParent != null) {
//			expandedMethodCall = invokeParent.getInvocationChildren().get(0).getMethodSign();
//		}
//
//		for (String method : methods) {
//			if (method.equals(expandedMethodCall)) {
//				continue;
//			}
//			validMethods.add(method);
//		}
//
//		return validMethods;
//	}
//
//	public static boolean containsVar(String ID) {
//		return graph.containsKey(ID);
//	}
//
//	private static VariableGraphNode getVar(VarValue varValue) {
//		String ID = getID(varValue);
//		if (!containsVar(varValue)) {
//			addVar(varValue);
//		}
//		return graph.get(ID);
//	}
//
//	private static String getID(VarValue varValue) {
//		return Variable.truncateSimpleID(varValue.getAliasVarID());
//	}
}

/**
 * This class represents a node in the variable graph.
 * 
 * @author hongshuwang
 */
//class VariableGraphNode {
//	private String aliasID;
//	private VarValue varValue;
//
////	private VariableGraphNode parent;
////	private Map<VariableGraphNode, String> children;
//
////	private List<TraceNode> relevantSteps;
//
//	public VariableGraphNode(VarValue varValue) {
//		this.aliasID = varValue.getAliasVarID();
//		this.varValue = varValue;
////		this.parent = null;
////		this.children = new HashMap<>();
////		this.relevantSteps = new ArrayList<>();
//	}
//
//	/* setters */
//
////	public void addRelevantStep(TraceNode step) {
////		this.relevantSteps.add(step);
////	}
////
////	public void removeRelevantStep(TraceNode step) {
////		this.relevantSteps.remove(step);
////	}
////
////	public void setParent(VariableGraphNode parent) {
////		this.parent = parent;
////	}
////
////	public void addChild(String name, VariableGraphNode child) {
////		this.children.put(child, name);
////	}
//
////	public void addSelfToParentSteps() {
////		if (this.parent == null) {
////			return;
////		}
////
////		List<TraceNode> steps = this.parent.getRelevantSteps();
////		for (TraceNode step : steps) {
////			VarValue parentVar = step.getReadVariables().stream()
////					.filter(v -> v.getAliasVarID().equals(parent.getAliasID())).findFirst().orElse(null);
////
////			if (parentVar != null) {
////				VarValue child = this.varValue.clone();
////
////				child.getVariable().setName(this.parent.children.get(this));
////				child.setStringValue(null);
////				child.setVarID(parentVar.getVarID().concat("-" + child.getVarName()));
////
////				VarValue foundChild = parentVar.getChildren().stream()
////						.filter(v -> v.getVarID().equals(child.getVarID())).findAny().orElse(null);
////				if (foundChild == null) {
////					parentVar.addChild(child);
////				}
////			}
////		}
////	}
//
//	/* getters */
//
//	public String getAliasID() {
//		return this.aliasID;
//	}
//
//	public VarValue getVarValue() {
//		return this.varValue;
//	}
//
////	public VariableGraphNode getParent() {
////		return this.parent;
////	}
////
////	public boolean hasChildren() {
////		return !this.children.isEmpty();
////	}
////	
////	public List<String> getCandidateVariables() {
////		ArrayList<String> candidateVariables = new ArrayList<>();
////		for (VariableGraphNode c : this.children.keySet()) {
////			candidateVariables.add(this.children.get(c));
////		}
////		return candidateVariables;
////	}
////
////	public List<TraceNode> getRelevantSteps() {
////		return this.relevantSteps;
////	}
////
////	public VariableGraphNode getFieldWithName(String name) {
////		if (name.contains("[") && name.contains("]")) {
////			String arrayName = name.split("\\[")[0];
////			for (VariableGraphNode key : children.keySet()) {
////				if (children.get(key).equals(arrayName)) {
////					return key;
////				}
////			}
////		} else {
////			for (VariableGraphNode key : children.keySet()) {
////				if (children.get(key).equals(name)) {
////					return key;
////				}
////			}
////		}
////		return null;
////	}
//
//}
