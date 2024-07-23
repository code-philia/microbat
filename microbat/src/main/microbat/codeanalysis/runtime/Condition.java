package microbat.codeanalysis.runtime;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

import microbat.instrumentation.AgentParams;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;

public class Condition {
	private String variableName;
	private String variableType;
	private String variableValue;

	private String classStructure;

	public Condition() {

	}

	public Condition(String variableName, String variableType, String variableValue, String classStructure) {
		super();
		this.variableName = variableName;
		this.variableType = variableType;
		this.variableValue = variableValue;
		this.classStructure = classStructure;
	}
	
	public JSONObject getMatchedGroundTruth(Trace trace) {
		VarValue groundTruthVar = null;
		for (TraceNode step : trace.getExecutionList()) {
			for (VarValue readVariable : step.getReadVariables()) {
				if (matchBasicCondition(readVariable)) {
					groundTruthVar = readVariable;
					break;
				}
			}
			if (groundTruthVar != null) {
				break;
			}
		}
		return groundTruthVar == null ? null : groundTruthVar.toJSON();
	}
	
	public List<String> getGroundTruthVariablesAndAliases(Trace trace) {
        List<String> variablesAndAliases = new ArrayList<>();
        for (TraceNode step : trace.getExecutionList()) {
            for (VarValue readVariable : step.getReadVariables()) {
                if (matchBasicCondition(readVariable)) {
                    collectVariablesAndAliases(readVariable, variablesAndAliases);
                }
            }
        }
        return variablesAndAliases;
    }

    private void collectVariablesAndAliases(VarValue varValue, List<String> list) {
        list.add(varValue.getVarName() + ":" + varValue.getAliasVarID());
        for (VarValue child : varValue.getChildren()) {
            collectVariablesAndAliases(child, list);
        }
    }
    
	public String getSourceCode(Trace trace) {
		for (TraceNode step : trace.getExecutionList()) {
			for (VarValue readVariable : step.getReadVariables()) {
				if (matchBasicCondition(readVariable)) {
					int lineNo = step.getLineNumber();
					String location = step.getBreakPoint().getFullJavaFilePath();
					String sourceCode = TraceRecovUtils
							.processInputStringForLLM(TraceRecovUtils.getSourceCode(location, lineNo).trim());
					return sourceCode;
				}
			}
		}
		return null;
	}
	
	/**
	 * Copied from {@code RuntimeCondition.matchBasicCondition}
	 * If varValue or any of its ancestors match the condition, return true.
	 * 
	 * @param varValue
	 * @return
	 */
	public boolean matchBasicCondition(VarValue varValue) {
		if (varValue == null || this.variableName == null || this.variableType == null || this.variableValue == null) {
			return false;
		}

		boolean isMatched = this.variableName.equals(varValue.getVarName())
				&& this.variableType.equals(varValue.getType()) && this.variableValue.equals(varValue.getStringValue());

		if (isMatched) {
			return true;
		} else {
			// In this scenario, added fields only have one parent each
			if (varValue.getParents().isEmpty()) {
				return false;
			}
			return matchBasicCondition(varValue.getParents().get(0));
		}
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableType() {
		return variableType;
	}

	public void setVariableType(String variableType) {
		this.variableType = variableType;
	}

	public String getVariableValue() {
		return variableValue;
	}

	public void setVariableValue(String variableValue) {
		this.variableValue = variableValue;
	}

	public String getClassStructure() {
		return classStructure;
	}

	public void setClassStructure(String classStructure) {
		this.classStructure = classStructure;
	}

	@Override
	public String toString() {
		return AgentParams.OPT_CONDITION_VAR_NAME + ":" + variableName + "###" + AgentParams.OPT_CONDITION_VAR_TYPE + ":"
				+ variableType + "###" + AgentParams.OPT_CONDITION_VAR_VALUE + ":" + variableValue + "###"
				+ AgentParams.OPT_CONDITION_CLASS_STRUCTURE + ":" + classStructure + "###";
	}

}