package microbat.tracerecov.varexpansion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import microbat.model.value.ArrayValue;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.StringValue;
import microbat.model.value.VarValue;
import microbat.model.variable.FieldVar;
import microbat.model.variable.Variable;
import microbat.tracerecov.TraceRecovUtils;

/**
 * This class is a representation of the structure of a variable.
 * 
 * @author hongshuwang
 */
public class VariableSkeleton {

	private String type;
	private String name;
	private VariableSkeleton parent;
	private List<VariableSkeleton> children;

	public VariableSkeleton(String type) {
		this.type = type;
		this.name = "";
		this.parent = null;
		this.children = new ArrayList<>();
	}

	public VariableSkeleton(String type, String name) {
		this.type = type;
		this.name = name;
		this.parent = null;
		this.children = new ArrayList<>();
	}

	public void addChild(VariableSkeleton child) {
		this.children.add(child);
		child.parent = this;
	}

	public VarValue toVarValue(VarValue source) {
		VarValue varValue = this.createVarValue(source.getVarName(), source.getType(), source.isRoot());
		varValue.setAliasVarID(source.getAliasVarID());
		varValue.setVarID(source.getVarID());
		varValue.setStringValue(source.getStringValue());
		List<VarValue> children = this.children.stream()
				.map(v -> v.toVarValue(source.getAliasVarID(), source.getVarID(), varValue)).toList();
		varValue.setChildren(children);
		return varValue;
	}

	private VarValue toVarValue(String aliasID, String varID, VarValue parent) {
		VarValue varValue = this.createVarValue(this.name, this.type, false);
		String newAliasID = aliasID + "-" + name;
		String newVarID = varID + "-" + name;
		varValue.setAliasVarID(newAliasID);
		varValue.setVarID(newVarID);
		varValue.setParents(Arrays.asList(parent));
		List<VarValue> children = this.children.stream().map(v -> v.toVarValue(newAliasID, newVarID, varValue)).toList();
		varValue.setChildren(children);
		return varValue;
	}

	private VarValue createVarValue(String name, String type, boolean isRoot) {
		Variable variable = new FieldVar(false, name, type, type);
		VarValue varValue = null;
		if (TraceRecovUtils.isString(type)) {
			varValue = new StringValue(null, isRoot, variable);
		} else if (TraceRecovUtils.isPrimitiveType(type)) {
			varValue = new PrimitiveValue(null, isRoot, variable);
		} else if (TraceRecovUtils.isArray(type)) {
			varValue = new ArrayValue(false, isRoot, variable);
		} else {
			varValue = new ReferenceValue(false, isRoot, variable);
		}
		return varValue;
	}

}
