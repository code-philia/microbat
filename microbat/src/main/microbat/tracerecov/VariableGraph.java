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
import microbat.tracerecov.candidatevar.CandidateVarRetriever;
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

	public static void reset() {
		graph = new HashMap<>();
		lastVisitedID = null;
	}

	/**
	 * 1. Add a relevant step of a variable to the graph.
	 * 2. Identify and add candidate variables based on the invoked method 
	 *    through bytecode analysis.
	 */
	public static void addRelevantStep(VarValue varValue, TraceNode step) {
		getVar(varValue).addRelevantStep(step);
		addCandidateVariables(varValue, step); // not used
	}

	/**
	 * Identify candidate variables of the variable based on the invoked method
	 * through bytecode analysis.
	 */
	private static void addCandidateVariables(VarValue varValue, TraceNode step) {
		String invokingMethod = step.getInvokingMethod();
		List<String> candidateVariables = CandidateVarRetriever.getCandidateVariables(invokingMethod);

		getVar(varValue).addCandidateVariables(candidateVariables);
	}

	/**
	 * Map variable on trace to one of the candidate variables.
	 * 
	 * 1. Identify return type of the invoked method through bytecode analysis.
	 * 2. Find corresponding variable on trace. If the variable is not in the 
	 *    graph, add it to the graph.
	 * 3. Link variable on trace to candidate variable.
	 */
	public static void mapVariable(VarValue varValue, TraceNode step) {
		String invokingMethod = step.getInvokingMethod();

		/* 1. Identify return type */
		String returnedField = VariableMapper.getReturnedField(invokingMethod);
		String fieldName = returnedField.split("#")[0];
		String fieldType = returnedField.split("#")[1];

		/* 2. Find corresponding variable on trace */
		VarValue returnedVariable = step.getWrittenVariables().stream()
				.filter(v -> v.getType().equals(fieldType)).findFirst().orElse(null);
		if (returnedVariable == null) {
			return;
		}
		VariableGraphNode variableOnTrace = getVar(returnedVariable);

		/* 3. Link variables */
		VariableGraphNode parentVariable = getVar(varValue);
		parentVariable.addChild(fieldName, variableOnTrace);
		variableOnTrace.setParent(parentVariable);
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

	private static boolean containsVar(String ID) {
		return graph.containsKey(ID);
	}

	public static void addVar(VarValue varValue) {
		graph.put(getID(varValue), new VariableGraphNode(varValue));
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
	private Set<String> candidateVariables;

	public VariableGraphNode(VarValue varValue) {
		this.aliasID = varValue.getAliasVarID();
		this.varValue = varValue;
		this.parent = null;
		this.children = new HashMap<>();
		this.relevantSteps = new ArrayList<>();
		this.candidateVariables = new HashSet<>();
	}

	/* setters */

	public void addRelevantStep(TraceNode step) {
		this.relevantSteps.add(step);
	}

	public void addCandidateVariables(List<String> candidateVariables) {
		this.candidateVariables.addAll(candidateVariables);
	}

	public void setParent(VariableGraphNode parent) {
		this.parent = parent;
	}

	public void addChild(String name, VariableGraphNode child) {
		this.children.put(child, name);
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

	public Set<String> getCandidateVariables() {
		return this.candidateVariables;
	}

}
