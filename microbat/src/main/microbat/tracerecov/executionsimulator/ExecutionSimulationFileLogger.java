package microbat.tracerecov.executionsimulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONObject;
import java.util.List;

import microbat.Activator;
import microbat.codeanalysis.runtime.Condition;
import microbat.model.trace.Trace;
import microbat.preference.TraceRecovPreference;


public class ExecutionSimulationFileLogger extends ExecutionSimulationLogger {

    private String filePath;
    private String aliasFilePath;

    public ExecutionSimulationFileLogger() {
        String fileName = "var_expansion.txt";
        String aliasFileName = "aliases.txt";
        this.filePath = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.PROMPT_GT_PATH)
                + File.separator + fileName;
        this.aliasFilePath = Activator.getDefault().getPreferenceStore().getString(TraceRecovPreference.PROMPT_GT_PATH)
                + File.separator + aliasFileName;
    }

    private PrintWriter createWriter(String path) throws IOException {
        FileWriter fileWriter = new FileWriter(path, true);
        PrintWriter writer = new PrintWriter(fileWriter);
        return writer;
    }

    public String collectGT(Condition condition, Trace trace) {
        return collectGT(condition, trace, null);
    }

    public String collectGT(Condition condition, Trace trace, String aliasID) {
        JSONObject groundTruthJSON = condition.getMatchedGroundTruth(trace);
        if (groundTruthJSON == null) {
            return null;
        }
        
        String sourceCode = condition.getSourceCode(trace);

        // write to results file
        PrintWriter writer;
        try {
            writer = createWriter(this.filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        writer.println(getCsvContent(condition, groundTruthJSON, aliasID,sourceCode));
        writer.close();
        
        // collect variables and aliases
//        List<String> variablesAndAliases = condition.getGroundTruthVariablesAndAliases(trace);
//        try {
//            writer = createWriter(this.aliasFilePath);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//
//        for (String variableAndAlias : variablesAndAliases) {
//            writer.println(variableAndAlias);
//        }
//        writer.close();
        
        return groundTruthJSON.toString();
    }

    private String getCsvContent(Condition condition, JSONObject groundTruthJSON, String aliasID,String sourceCode) {
        String variableName = condition.getVariableName();
        String variableType = condition.getVariableType();
        String variableValue = condition.getVariableValue();
        String variableClassStructure = condition.getClassStructure();
        String groundTruth = groundTruthJSON.toString();

        StringBuilder stringBuilder = new StringBuilder();
        String delimiter = "###";
        stringBuilder.append(variableName);
        stringBuilder.append(delimiter);
        stringBuilder.append(variableType);
        stringBuilder.append(delimiter);
        stringBuilder.append(variableValue);
        stringBuilder.append(delimiter);
        stringBuilder.append(variableClassStructure);
        stringBuilder.append(delimiter);
        stringBuilder.append(sourceCode);
        stringBuilder.append(delimiter);
//        if (aliasID != null) {
//            stringBuilder.append(aliasID);
//            stringBuilder.append(delimiter);
//        }
        stringBuilder.append(groundTruth);
        return stringBuilder.toString();
    }
}
