package microbat.tracerecov;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
	private static Map<String, VariableGraphNode> graph = new HashMap<>();

	private static String lastVisitedID = null;

	/* public methods */

	public static void reset() {
		graph = new HashMap<>();
		lastVisitedID = null;
	}

	public static void addRelevantStep(VarValue varValue, TraceNode step) {
		if (step.getInvokingMethod().startsWith("%") && !(step.getStepOverPrevious() != null
				&& !step.getStepOverPrevious().getInvocationChildren().isEmpty())) {
			// doesn't invoke methods but reads varValue
			getVar(varValue).addRelevantStep(step);
			return;
		}

		if (!step.getInvocationChildren().isEmpty() || (step.getStepOverPrevious() != null
				&& !step.getStepOverPrevious().getInvocationChildren().isEmpty())) {
			return;
		}
		getVar(varValue).addRelevantStep(step);
	}

	/**
	 * Map variable on trace to one of the candidate variables.
	 * 
	 * 1. Identify candidate variable through bytecode analysis.
	 * 2. Find corresponding variable on trace. If the variable is 
	 *    not in the graph, add it to the graph.
	 * 3. Link variable on trace to candidate variable.
	 */
	public static void mapVariable(VarValue varValue, VarValue parentVar, TraceNode step) {
		List<String> methods = getValidInvokingMethods(step);

		for (String invokingMethod : methods) {
			/* 1. Identify candidate variable */
			String returnedField = VariableMapper.getReturnedField(invokingMethod);
			String fieldName = returnedField.split("#")[0];
			String fieldType = returnedField.split("#")[1];

			if (fieldName.equals("null") && fieldType.equals("null")) {
				/* No candidate variable, record other variables as relevant variables */
				List<VariableGraphNode> relevantVariables = new ArrayList<>();

				Function<? super VarValue, ? extends VariableGraphNode> converter = v -> {
					String type = v.getType();
					if (TraceRecovUtils.isPrimitiveType(type) || TraceRecovUtils.isString(type)
							|| TraceRecovUtils.isArray(type)) {
						return null;
					}
					return getVar(v);
				};
				relevantVariables.addAll(step.getWrittenVariables().stream().map(converter).toList());
				relevantVariables.addAll(step.getReadVariables().stream().map(converter).toList());
				
				VariableGraphNode currentVariable = getVar(varValue);
				currentVariable.addRelevantVariables(step, relevantVariables);
				relevantVariables.stream().forEach(v -> {
					if (v != null) {
						v.addRelevantVariable(step, currentVariable);
					}
				});
			} else {
				/* 2. Find corresponding variable on trace */
				VarValue returnedVariable = step.getWrittenVariables().stream()
						.filter(v -> v.getType().equals(fieldType)).findFirst().orElse(null);
				if (returnedVariable == null) {
					return;
				}
				VariableGraphNode variableOnTrace = getVar(returnedVariable);

				/* 3. Link variables */
				VariableGraphNode parentVariable = getVar(parentVar);
				parentVariable.addChild(fieldName, variableOnTrace);
				variableOnTrace.setParent(parentVariable);
			}
		}
	}

	/**
	 * If the graph hasn't been visited, return the deepest layer. Otherwise, return
	 * the parent of the last visited node.
	 */
	public static String getNextNodeIDToVisit() {
		if (lastVisitedID == null) {
			for (String key : graph.keySet()) {
				if (!graph.get(key).hasChildren()) {
					lastVisitedID = key;
					return key;
				}
			}
		} else {
			VariableGraphNode parent = graph.get(lastVisitedID).getParent();
			if (parent == null) {
				return null;
			}
			String parentID = parent.getAliasID();
			lastVisitedID = parentID;
			return parentID;
		}

		return null;
	}

	public static void addCurrentToParentVariables() {
		if (lastVisitedID == null) {
			return;
		}
		graph.get(lastVisitedID).addSelfToParentSteps();
	}

	public static List<TraceNode> getRelevantSteps(String ID) {
		if (!containsVar(ID)) {
			return new ArrayList<>();
		}
		return graph.get(ID).getRelevantSteps();
	}

	public static boolean hasChildren(String ID) {
		if (!containsVar(ID)) {
			return false;
		}
		return graph.get(ID).hasChildren();
	}

	public static List<String> getCandidateVariables(String ID) {
		if (!containsVar(ID)) {
			return new ArrayList<>();
		}
		return graph.get(ID).getVarValue().getAllDescedentChildren().stream().map(v -> v.getVarName()).toList();
	}

	public static boolean containsVar(VarValue varValue) {
		return containsVar(getID(varValue));
	}

	public static void addVar(VarValue varValue) {
		graph.put(getID(varValue), new VariableGraphNode(varValue));
	}

	/* private methods */

	/**
	 * Return all invoking methods other than the expanded ones.
	 */
	private static List<String> getValidInvokingMethods(TraceNode step) {
		String invokingMethod = step.getInvokingMethod();
		String[] methods = invokingMethod.split("%");
		List<String> validMethods = new ArrayList<String>();

		// get expanded method
		TraceNode invokeParent = null;
		if (!step.getInvocationChildren().isEmpty()) {
			invokeParent = step;
		} else {
			TraceNode previous = step.getStepOverPrevious();
			if (previous != null && !previous.getInvocationChildren().isEmpty()) {
				invokeParent = previous;
			}
		}
		String expandedMethodCall = null;
		if (invokeParent != null) {
			expandedMethodCall = invokeParent.getInvocationChildren().get(0).getMethodSign();
		}

		for (String method : methods) {
			if (method.equals(expandedMethodCall)) {
				continue;
			}
			validMethods.add(method);
		}

		return validMethods;
	}

	private static boolean containsVar(String ID) {
		return graph.containsKey(ID);
	}

	private static VariableGraphNode getVar(VarValue varValue) {
		String ID = getID(varValue);
		if (!containsVar(varValue)) {
			addVar(varValue);
		}
		return graph.get(ID);
	}

	private static String getID(VarValue varValue) {
		return Variable.truncateSimpleID(varValue.getAliasVarID());
	}
}

/**
 * This class represents a node in the variable graph.
 * 
 * @author hongshuwang
 */
class VariableGraphNode {
	private String aliasID;
	private VarValue varValue;

	private VariableGraphNode parent;
	private Map<VariableGraphNode, String> children;

	private List<TraceNode> relevantSteps;
	private Map<VariableGraphNode, Set<TraceNode>> relevantVariables;

	public VariableGraphNode(VarValue varValue) {
		this.aliasID = varValue.getAliasVarID();
		this.varValue = varValue;
		this.parent = null;
		this.children = new HashMap<>();
		this.relevantSteps = new ArrayList<>();
		this.relevantVariables = new HashMap<>();
	}

	/* setters */

	public void addRelevantStep(TraceNode step) {
		this.relevantSteps.add(step);
	}

	public void setParent(VariableGraphNode parent) {
		this.parent = parent;
	}

	public void addChild(String name, VariableGraphNode child) {
		this.children.put(child, name);
	}

	public void addRelevantVariable(TraceNode step, VariableGraphNode relevantVar) {
		if (relevantVar == null || relevantVar.getAliasID().equals(this.getAliasID())) {
			return;
		}

		if (!this.relevantVariables.containsKey(relevantVar)) {
			this.relevantVariables.put(relevantVar, new HashSet<>());
		}
		this.relevantVariables.get(relevantVar).add(step);
	}

	public void addRelevantVariables(TraceNode step, List<VariableGraphNode> relevantVars) {
		for (VariableGraphNode v : relevantVars) {
			if (!this.relevantVariables.containsKey(v)) {
				this.relevantVariables.put(v, new HashSet<>());
			}
			this.relevantVariables.get(v).add(step);
		}
	}

	public void addSelfToParentSteps() {
		if (this.parent == null) {
			return;
		}

		List<TraceNode> steps = this.parent.getRelevantSteps();
		for (TraceNode step : steps) {
			VarValue parentVar = step.getReadVariables().stream()
					.filter(v -> v.getAliasVarID().equals(parent.getAliasID())).findFirst().orElse(null);

			if (parentVar != null) {
				VarValue child = this.varValue.clone();

				child.getVariable().setName(this.parent.children.get(this));
				child.setStringValue(null);
				child.setVarID(parentVar.getVarID().concat("-" + child.getVarName()));

				VarValue foundChild = parentVar.getChildren().stream()
						.filter(v -> v.getVarID().equals(child.getVarID())).findAny().orElse(null);
				if (foundChild == null) {
					parentVar.addChild(child);
				}
			}
		}
	}

	/* getters */

	public String getAliasID() {
		return this.aliasID;
	}

	public VarValue getVarValue() {
		return this.varValue;
	}

	public VariableGraphNode getParent() {
		return this.parent;
	}

	public boolean hasChildren() {
		return !this.children.isEmpty();
	}

	public List<TraceNode> getRelevantSteps() {
		return this.relevantSteps;
	}

}
