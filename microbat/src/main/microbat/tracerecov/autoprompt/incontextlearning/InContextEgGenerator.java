package microbat.tracerecov.autoprompt.incontextlearning;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.value.VirtualValue;
import microbat.tracerecov.autoprompt.incontextlearning.InContextExecutor.SourceCodeWritter;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.tracerecov.executionsimulator.LLMResponseType;
import microbat.util.StringFormatUtils;
import sav.strategies.dto.AppJavaClassPath;

/**
 * This class is used to instrument the given code and generate example for in-context learning.
 * 
 * @author HongshuW
 */
public class InContextEgGenerator implements InContextLearning {

	private static final Logger log = LoggerFactory.getLogger(InContextEgGenerator.class);

    private ExecutionSimulator executionSimulator;
    private AppJavaClassPath appJavaClassPath;

    public ExecutionSimulator getExecutionSimulator() {
        return executionSimulator;
    }

    public void setExecutionSimulator(ExecutionSimulator executionSimulator) {
        this.executionSimulator = executionSimulator;
    }

    public InContextEgGenerator() {}

	public InContextLearningCode getGeneratedExampleCode(
			String imports,
			String targetMethod,
			int targetLineNumber,
			String targetVariable,
			InContextLearningType type,
			ContextVariablesToString contextToString) {
		if (executionSimulator == null) {
			throw new IllegalStateException("Execution simulator is not set");
		}

		log.info("executeInContextLearning: imports: {}\ntarget: {}\ntargetLine: {}", imports, targetMethod,
				targetLineNumber);
		String input = processInputString(imports, targetMethod, targetLineNumber);
		log.info("Input code: {}", input);
		String gptSystem = getBackgroundContent();
		String gptUser = getQuestionContent(input, targetVariable);
		log.info("GPT system: {}", gptSystem);
		log.info("GPT user: {}", gptUser);

		String response = null;
		try {
			response = executionSimulator.sendRequest(gptSystem, gptUser, LLMResponseType.TEXT);
		} catch (Exception e) {
			log.error("Failed to execute in context learning", e);
			return null;
		}

		log.info("Response: {}", response);

		InContextLearningCode generatedCode = processCodeGeneratedByLLM(response);
		return generatedCode;
	}

	public InContextLearningVariables getGeneratedExampleVars(AppJavaClassPath appJavaClassPath,
			InContextLearningCode generatedCode, InContextLearningType type) {
		log.info("Generated code: {}", generatedCode.getCode());
		log.info("Marker line: {}", generatedCode.getMarkerLine());
		InContextExecutor executor = new InContextExecutor(generatedCode, appJavaClassPath);

		Trace trace = null;
		try {
			trace = executor.run();
		} catch (Exception e) {
			log.error("Failed to execute generated code", e);
			return null;
		}
		
		if (trace == null) {
			return null;
		}

		InContextLearningVariables variables = postProcessTrace(trace, type, generatedCode);
		return variables;
	}

    public static final String TARGET_METHOD_SIGN = "SampleTest#test()V";

    private static class TraceRange {
        int start;
        int end;
    }

    private int getLastDescendant(TraceNode node) {
        if (node.getInvocationChildren().size() != 0) {
            return getLastDescendant(node.getInvocationChildren().get(node.getInvocationChildren().size() - 1));
        } else if (node.getLoopChildren().size() != 0) {
            return getLastDescendant(node.getLoopChildren().get(node.getLoopChildren().size() - 1));
        } else if (node.getAbstractChildren().size() != 0) {
            return getLastDescendant(node.getAbstractChildren().get(node.getAbstractChildren().size() - 1));
        } else {
            return node.getOrder();
        }
    }

    public static class InContextLearningVariables implements ContextVariables {
        private List<VarValue> allWrittenVariables;
        private List<VarValue> allReadVariables;
        private List<VarValue> outerWrittenVariables;
        private List<VarValue> outerReadVariables;
        private InContextLearningCode code;

        public InContextLearningVariables() {
            allWrittenVariables = new ArrayList<>();
            allReadVariables = new ArrayList<>();
            outerWrittenVariables = new ArrayList<>();
            outerReadVariables = new ArrayList<>();
        }

        public InContextLearningVariables(List<VarValue> allWrittenVariables, List<VarValue> allReadVariables,
                List<VarValue> outerWrittenVariables, List<VarValue> outerReadVariables, InContextLearningCode code) {
            this.allWrittenVariables = allWrittenVariables;
            this.allReadVariables = allReadVariables;
            this.outerWrittenVariables = outerWrittenVariables;
            this.outerReadVariables = outerReadVariables;
            this.code = code;
        }

        public List<VarValue> getAllWrittenVariables() {
            return allWrittenVariables;
        }

        public void setAllWrittenVariables(List<VarValue> allWrittenVariables) {
            this.allWrittenVariables = allWrittenVariables;
        }

        public List<VarValue> getAllReadVariables() {
            return allReadVariables;
        }

        public void setAllReadVariables(List<VarValue> allReadVariables) {
            this.allReadVariables = allReadVariables;
        }

        public List<VarValue> getOuterWrittenVariables() {
            return outerWrittenVariables;
        }

        public void setOuterWrittenVariables(List<VarValue> outerWrittenVariables) {
            this.outerWrittenVariables = outerWrittenVariables;
        }

        public List<VarValue> getOuterReadVariables() {
            return outerReadVariables;
        }

        public void setOuterReadVariables(List<VarValue> outerReadVariables) {
            this.outerReadVariables = outerReadVariables;
        }

        public String getCode() {
            return code.getCode();
        }

        public void setCode(InContextLearningCode code) {
            this.code = code;
        }

        @Override
        public String getCodeToView() {
            return code.getCodeToView();
        }

        @Override
        public int getMarkerLine() {
            return code.getMarkerLine();
        }
    }

    public InContextLearningVariables postProcessTrace(
            Trace trace,
            InContextLearningType type,
            InContextLearningCode code) {
        int firstLoc = Integer.MAX_VALUE;
        int lastLoc = Integer.MIN_VALUE;

        List<Integer> targetLineTraces = new ArrayList<>();
        
        for (int i = 1; i <= trace.size(); i++) {
            TraceNode node = trace.getTraceNode(i);
            log.info("Trace method: {}, line: {}", node.getMethodSign(), node.getLineNumber());
            if (node.getMethodSign().equals(TARGET_METHOD_SIGN)) {
                firstLoc = Math.min(firstLoc, i);
                lastLoc = Math.max(lastLoc, i);

                if (node.getLineNumber() == code.getMarkerLine()) {
                    targetLineTraces.add(i);
                }
            }
        }

        if (lastLoc < 0) {
            log.error("Trace of the target method was not recorded");
            throw new IllegalStateException("Trace of the target method was not recorded");
        }

        if (targetLineTraces.size() == 0) {
            log.error("No trace node found for the target line");
            throw new IllegalStateException("No trace node found for the target line");
        }

        List<TraceRange> ranges = new ArrayList<>();
        int lastIdx = -1;
        int neighborIdx = -1;

        for (int i : targetLineTraces) {
            TraceNode node = trace.getTraceNode(i);
            if (neighborIdx == -1) {
                lastIdx = i;
                neighborIdx = getLastDescendant(node);
            } else {
                if (i == neighborIdx + 1) {
                    neighborIdx = getLastDescendant(node);
                } else {
                    TraceRange range = new TraceRange();
                    range.start = lastIdx;
                    range.end = neighborIdx;
                    ranges.add(range);

                    lastIdx = i;
                    neighborIdx = getLastDescendant(node);
                }
            }
        }

        if (lastIdx != -1) {
            TraceRange range = new TraceRange();
            range.start = lastIdx;
            range.end = neighborIdx;
            ranges.add(range);
        }

        if (ranges.size() == 0) {
            log.error("INTERNAL ERROR: No trace range found");
            throw new IllegalStateException("INTERNAL ERROR: No trace range found");
        }

        // FIXME: only take the first trace range
        TraceRange focusRange = ranges.get(0);

        InContextLearningVariables variables = new InContextLearningVariables();
        variables.setCode(code);

        for (int i = focusRange.start; i <= focusRange.end; i++) {
            TraceNode node = trace.getTraceNode(i);
            boolean isOuter = node.getMethodSign().equals(TARGET_METHOD_SIGN);

            for (VarValue var : node.getWrittenVariables()) {
                if (var instanceof VirtualValue) {
                    continue;
                }
                variables.getAllWrittenVariables().add(var);
                if (isOuter) {
                    variables.getOuterWrittenVariables().add(var);
                }
            }

            for (VarValue var : node.getReadVariables()) {
                if (var instanceof VirtualValue) {
                    continue;
                }
                variables.getAllReadVariables().add(var);
                if (isOuter) {
                    variables.getOuterReadVariables().add(var);
                }
            }
        }

        return variables;
    }

    public static String formatVarValue(VarValue var) {
        Map<String, String> vars = Map.of("var_name", var.getVarName(), "var_value", var.getStringValue());
        final String format = "Variable: ${var_name} with value: ${var_value}";
        return StringFormatUtils.formatString(format, vars);
    }

    public static String formatVarValueList(List<VarValue> vars) {
        StringBuilder sb = new StringBuilder();
        for (VarValue var : vars) {
            sb.append("  ");
            sb.append(formatVarValue(var));
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String variablesToExplainString(ContextVariables variables) {
        Map<String, String> varMap = new HashMap<>();
        varMap.put("code", variables.getCodeToView());
        varMap.put("variables_read", formatVarValueList(variables.getAllReadVariables()));
        varMap.put("variables_written", formatVarValueList(variables.getAllWrittenVariables()));
        varMap.put("variables_read_outer", formatVarValueList(variables.getOuterReadVariables()));
        varMap.put("variables_written_outer", formatVarValueList(variables.getOuterWrittenVariables()));
        return StringFormatUtils.formatString(StringFormatUtils.getPromptInContextLearningExplain(), varMap);
    }

    public static ContextVariablesToString defaultToString() {
        return (context, type) -> variablesToExplainString(context);
    }

    public String insertLineMarker(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }

        if (i == line.length()) {
            throw new IllegalArgumentException("Empty line");
        }

        return line.substring(0, i) + ">>> " + line.substring(i);
    }

    public String processInputString(String imports, String targetMethod, int targetLineNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append(imports);
        sb.append("\n");
        String[] lines = targetMethod.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i == targetLineNumber - 1) {
                sb.append(insertLineMarker(lines[i]));
            } else {
                sb.append(lines[i]);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public String getBackgroundContent() {
        return StringFormatUtils.getPromptInContextLearningSystem();
    }

    public String getQuestionContent(String code, String varName) {
        String format = StringFormatUtils.getPromptInContextLearningUser();
        HashMap<String, String> map = new HashMap<>();
        map.put("original_code", code);
        map.put("target_var", varName);
        return StringFormatUtils.formatString(format, map);
    }

    public static class InContextLearningCode implements SourceCodeWritter {
        private String code;
        private String codeToView;
        private int markerLine;

        @Override
        public void writeSourceCode(FileWriter writer) throws IOException {
            writer.write(code);
        }

        public InContextLearningCode() {
        }

        public InContextLearningCode(String code, String codeToView, int markerLine) {
            this.code = code;
            this.codeToView = codeToView;
            this.markerLine = markerLine;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getCodeToView() {
            return codeToView;
        }

        public void setCodeToView(String codeToView) {
            this.codeToView = codeToView;
        }

        public int getMarkerLine() {
            return markerLine;
        }

        public void setMarkerLine(int markerLine) {
            this.markerLine = markerLine;
        }
    }

    public static final String BEGIN_MARK = "```java";
    public static final String END_MARK = "```";

    public InContextLearningCode processCodeGeneratedByLLM(String response) {
        int beginMark = response.indexOf(BEGIN_MARK);
        if (beginMark == -1) {
            throw new IllegalArgumentException("No code block \"```java\" found in the response");
        }
        int endMark = response.indexOf(END_MARK, beginMark + BEGIN_MARK.length());
        if (endMark == -1) {
            throw new IllegalArgumentException("No end mark \"```\" found in the response");
        }

        String code = response.substring(beginMark + BEGIN_MARK.length(), endMark);
        int lastBracket = code.lastIndexOf("}");
        if (lastBracket == -1) {
            throw new IllegalArgumentException("No closing bracket found in the code block");
        }

        String codeBefore = code.substring(0, lastBracket);
        String codeAfter = code.substring(lastBracket);

        String ressembleCode = codeBefore + StringFormatUtils.getPromptInContextLearningInsertEnd() + codeAfter;

        String[] lines = ressembleCode.split("\n");

        StringBuilder sb = new StringBuilder();
        int markerLine = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int markLoc = line.indexOf(">>>");
            if (markLoc != -1) {
                if (markerLine != -1) {
                    throw new IllegalArgumentException("Multiple marker lines found in the code block \">>>\"");
                } else {
                    markerLine = i + 1;
                }

                sb.append(line.substring(0, markLoc));
                sb.append(" ");
                sb.append(line.substring(markLoc + 3));
                sb.append("\n");
            } else {
                sb.append(line);
                sb.append("\n");
            }
        }
        if (markerLine == -1) {
            throw new IllegalArgumentException("No marker line found in the code block \">>>\"");
        }

        String outputCode = sb.toString();

        return new InContextLearningCode(outputCode, code, markerLine);
    }

	/**
	 * This method is for testing purpose and not implemented here.
	 */
	@Override
	public String executeInContextLearning(String imports, String targetMethod, int targetLineNumber,
			String targetVariable, InContextLearningType type, ContextVariablesToString contextToString) {
		return null;
	}

}
