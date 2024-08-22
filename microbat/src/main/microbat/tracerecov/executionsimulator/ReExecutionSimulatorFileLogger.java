package microbat.tracerecov.executionsimulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import microbat.Activator;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.preference.TraceRecovPreference;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.varskeleton.VarSkeletonBuilder;
import microbat.tracerecov.varskeleton.VariableSkeleton;

public class ReExecutionSimulatorFileLogger {
	
	private String aliasFilePath;	private String definitionFilePath;
	
	public ReExecutionSimulatorFileLogger() {
        String aliasFileName = "aliases_gt.txt";
        String definitionFileName = "definition_gt.txt";
        
        this.aliasFilePath = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.PROMPT_GT_PATH)
                + File.separator + aliasFileName;
        this.definitionFilePath = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.PROMPT_GT_PATH)
                + File.separator + definitionFileName;
	}
	
    private PrintWriter createWriter(String path) throws IOException {
        FileWriter fileWriter = new FileWriter(path, true);
        PrintWriter writer = new PrintWriter(fileWriter);
        return writer;
    }

    public void collectAliasGT(TraceNode step, VarValue rootVar, List<VarValue> criticalVariables, Map<VarValue, VarValue> map) {
        // write to results file
        PrintWriter writer;
        try {
            writer = createWriter(this.aliasFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        writer.println(getAliasCsvContent(step, rootVar, criticalVariables, map));
        writer.close();
    }

    public void collectDefinitionGT(TraceNode step, VarValue rootVar, VarValue targetVar, List<VarValue> criticalVariables, boolean result) {
        PrintWriter writer;
        try {
            writer = createWriter(this.definitionFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        writer.println(getDefinitionCsvContent(step, rootVar, targetVar, criticalVariables, result));
        writer.close();
    }
    
    private String getAliasCsvContent(TraceNode step, VarValue rootVar, List<VarValue> criticalVariables, Map<VarValue, VarValue> map) {
    	StringBuilder groundTruth = new StringBuilder();
    	String delimiter = "###";
    	
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils.processInputStringForLLM(TraceRecovUtils.getSourceCode(location, lineNo).trim());

		/* all variables */
		Set<VarValue> variablesInStep = step.getAllVariables();
		/* variable properties */
		String rootVarName = rootVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		/* invoked methods to be checked */
		Set<String> invokedMethods = TraceRecovUtils.getInvokedMethodsToBeChecked(step.getInvokingMethod());
    	
    	groundTruth.append("step order: "+step.getOrder());    	groundTruth.append(delimiter);
		
		// source code
		groundTruth.append(sourceCode);
		groundTruth.append(delimiter);

		// variables information (name, type, value)
//		for (VarValue var : variablesInStep) {
//			groundTruth.append("`");
//			groundTruth.append(var.getVarName());
//			groundTruth.append("` is of type `");
//			groundTruth.append(var.getType());
//			groundTruth.append("`, of runtime value \"");
//			groundTruth.append(var.getStringValue());
//			groundTruth.append("\",");
//		}
		Map<String,String> varInStepMap = new HashMap<>();
		for(VarValue var:variablesInStep) {
			varInStepMap.put(var.getVarName()+":"+var.getType(),var.getStringValue());
		}
		groundTruth.append((new JSONObject(varInStepMap)).toString());
		groundTruth.append(delimiter);

		// target variable structure
		groundTruth.append(rootVarName);
		groundTruth.append(":");
		groundTruth.append(jsonString);
		groundTruth.append(delimiter);

		// existing alias relations
		Map<String,String> existingAliasMap = new HashMap<>();
		for (VarValue var : variablesInStep) {
			VarValue criticalVariable = null;
			if (var.getAliasVarID() != null) {
				criticalVariable = criticalVariables.stream().filter(v -> Variable.truncateSimpleID(var.getAliasVarID()).equals(Variable.truncateSimpleID(v.getAliasVarID())))
						.findFirst().orElse(null);
			}
			if (criticalVariable == null) {
				continue;
			}

			String cascadeFieldName = "";
			int splitIndex = criticalVariable.getVarID().indexOf(".");
			if (splitIndex >= 0) {
				cascadeFieldName = rootVar.getVarName() + criticalVariable.getVarID().substring(splitIndex);
			} else {
				cascadeFieldName = rootVar.getVarName();
			}

			if (!cascadeFieldName.equals(var.getVarName())) {
//				groundTruth.append("`");
//				groundTruth.append(var.getVarName());
//				groundTruth.append("` has the same memory address as `");
//				groundTruth.append(cascadeFieldName);
//				groundTruth.append("`,");
				existingAliasMap.put(var.getVarName(), cascadeFieldName);
			}
		}
		groundTruth.append((new JSONObject(existingAliasMap)).toString());
		groundTruth.append(delimiter);
		
		// fields in other variables
		Map<String,String> fieldsOfVarMap = new HashMap<>();
		for (VarValue var : variablesInStep) {
			if (var.equals(rootVar)) {
				continue;
			}
			VariableSkeleton varSkeleton = VarSkeletonBuilder.getVariableStructure(var.getType(), null);
			if (varSkeleton == null) {
				continue;
			}
//			groundTruth.append("`"+ var.getVarName());
//			groundTruth.append("` has the following fields:");
//			groundTruth.append(varSkeleton.fieldsToString());
//			groundTruth.append(",");
			fieldsOfVarMap.put(var.getVarName(), varSkeleton.fieldsToString());
		}
		groundTruth.append((new JSONObject(fieldsOfVarMap)).toString());
		groundTruth.append(delimiter);

		// invoked methods
//		if (!invokedMethods.isEmpty()) {
//			for (String methodSig : invokedMethods) {
//				groundTruth.append(methodSig);
//				groundTruth.append(";");
//			}
//		}
//		else {
//			groundTruth.append(" ");
//		}
		groundTruth.append((new JSONArray(invokedMethods)).toString());
		groundTruth.append(delimiter);
		
		// keys (critical variables)
		List<String> criticalVarName = new ArrayList<String>();
		String cascadeName = "";
		for (VarValue criticalVar : criticalVariables) {
			criticalVarName.add(cascadeName + criticalVar.getVarName());
			cascadeName += criticalVar.getVarName() + ".";
		}
		groundTruth.append((new JSONArray(criticalVarName)).toString());
		groundTruth.append(delimiter);
		
		System.out.println("cascadeName of criticalVarName:"+criticalVarName);//PRINT
		
		// ground truth
		Map<String,String> gt = new HashMap<String,String>();
		for (Map.Entry<VarValue, VarValue> entry : map.entrySet()) {
		    VarValue key_var = entry.getKey();
		    VarValue value_var = entry.getValue();
		    for(String name:criticalVarName) {
		    	if(name.endsWith(key_var.getVarName())) {
		    		gt.put(name, getCascadeName(value_var));
		    		break;
		    	}
		    }
		}
        groundTruth.append((new JSONObject(gt)).toString());
        
        groundTruth.append(delimiter);
    	groundTruth.append(step.getBreakPoint().getFullJavaFilePath());

    	return groundTruth.toString();
    }
    
    public String getCascadeName(VarValue v) {
    	String cascadeName = v.getVarName();
    	while(!v.getParents().isEmpty()) {
    		v = v.getParents().get(0);
    		cascadeName = v.getVarName()+"."+cascadeName;
    	}
    	return cascadeName;
    }
    
    
    private String getDefinitionCsvContent(TraceNode step, VarValue rootVar, VarValue targetVar, List<VarValue> criticalVariables,boolean result) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCode(location, lineNo).trim());

		/* variable properties */
		String rootVarName = rootVar.getVarName();
		String targetVarName = targetVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		/* all variables */
		Set<VarValue> variablesInStep = step.getAllVariables();

		StringBuilder groundTruth = new StringBuilder();
    	String delimiter = "###";
    	
    	// step info
    	groundTruth.append("step order: "+step.getOrder());
    	groundTruth.append(delimiter);
    	
		groundTruth.append(sourceCode);
		groundTruth.append(delimiter);

		Map<String,String> varInStepMap = new HashMap<>();
		for(VarValue var:step.getReadVariables()) {
			varInStepMap.put(var.getVarName()+":"+var.getType(),var.getStringValue());
		}
		groundTruth.append((new JSONObject(varInStepMap)).toString());
		groundTruth.append(delimiter);

		// target variable structure
		groundTruth.append(rootVarName);
		groundTruth.append(":");
		groundTruth.append(jsonString);
		groundTruth.append(delimiter);
		
		// existing alias relations
		Map<String,String> existingAliasMap = new HashMap<>();
		for (VarValue var : variablesInStep) {
			VarValue criticalVariable = null;
			if (var.getAliasVarID() != null) {
				criticalVariable = criticalVariables.stream().filter(v -> Variable.truncateSimpleID(var.getAliasVarID()).equals(Variable.truncateSimpleID(v.getAliasVarID())))
						.findFirst().orElse(null);
			}
			if (criticalVariable == null) {
				continue;
			}

			String cascadeFieldName = "";
			int splitIndex = criticalVariable.getVarID().indexOf(".");
			if (splitIndex >= 0) {
				cascadeFieldName = rootVar.getVarName() + criticalVariable.getVarID().substring(splitIndex);
			} else {
				cascadeFieldName = rootVar.getVarName();
			}

			if (!cascadeFieldName.equals(var.getVarName())) {
				existingAliasMap.put(var.getVarName(), cascadeFieldName);
			}
		}
		groundTruth.append((new JSONObject(existingAliasMap)).toString());
		groundTruth.append(delimiter);
		
		// target variable
		String cascadeFieldName = "";
		int stopIndex = criticalVariables.size() - 1;
		for (int i = 0; i < stopIndex; i++) {
			VarValue criticalVar = criticalVariables.get(i);
			cascadeFieldName += criticalVar.getVarName() + ".";
		}
		cascadeFieldName += targetVarName;

		groundTruth.append(cascadeFieldName);
		groundTruth.append(delimiter);
		
		// ground truth
		if(result == true) {
			groundTruth.append("T");
		}
		else {
			groundTruth.append("F");
		}

		return groundTruth.toString();
    }
}