package microbat.tracerecov.executionsimulator;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.autoprompt.DefinitionInferenceExampleSearcher;
import microbat.tracerecov.autoprompt.ExampleSearcher;
import microbat.tracerecov.autoprompt.dataset.DatasetReader;
import microbat.tracerecov.coderetriever.SourceCodeRetriever;

public class DefinitionInferenceUtils {

	/* Request content */

	private static final String DEFINITION_INFERENCE_BACKGROUND = "<Background>\n"
			+ "You are a Java expert, you need to analyze whether a variable is written.";

	private static final String DEFINITION_INFERENCE_EXAMPLE = 
			"\n\n<Example>\n"
			+ "Given the code as:\n"
			+ "```hashMap.put(ch, tmp + 1);```\n"
			+ "\n"
			+ "Given the source code of function calls in the code:\n"
			+ "static final int hash(Object key) {\n"
			+ "    int n;\n"
			+ "    if (key == null) {\n"
			+ "        n = 0;\n"
			+ "    } else {\n"
			+ "        int h = key.hashCode();\n"
			+ "        n = h ^ h >>> 16;\n"
			+ "    }\n"
			+ "    return n;\n"
			+ "}\n"
			+ "@IntrinsicCandidate\n"
			+ "public static Integer valueOf(int i) {\n"
			+ "    if (i >= -128 && i <= IntegerCache.high) {\n"
			+ "        return IntegerCache.cache[i + 128];\n"
			+ "    }\n"
			+ "    return new Integer(i);\n"
			+ "}\n"
			+ "java.util.HashMap#newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;\n"
			+ "void afterNodeInsertion(boolean evict) {\n"
			+ "}\n"
			+ "@IntrinsicCandidate\n"
			+ "public static Character valueOf(char c) {\n"
			+ "    if (c <= '\\u007f') {\n"
			+ "        return CharacterCache.cache[c];\n"
			+ "    }\n"
			+ "    return new Character(c);\n"
			+ "}\n"
			+ "java.util.HashMap$Node#<init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)V\n"
			+ "public V put(K var1, V var2);\n"
			+ "java.util.HashMap#resize()[Ljava/util/HashMap$Node;\n"
			+ "final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {\n"
			+ "    int i;\n"
			+ "    Node<K, V> p;\n"
			+ "    int n;\n"
			+ "    Node<K, V>[] tab = this.table;\n"
			+ "    if (this.table == null || (n = tab.length) == 0) {\n"
			+ "        tab = this.resize();\n"
			+ "        n = tab.length;\n"
			+ "    }\n"
			+ "    if ((p = tab[i = n - 1 & hash]) == null) {\n"
			+ "        tab[i] = this.newNode(hash, key, value, null);\n"
			+ "    } else {\n"
			+ "        Node<K, V> e;\n"
			+ "        Object k;\n"
			+ "        if (p.hash == hash && ((k = p.key) == key || key != null && key.equals(k))) {\n"
			+ "            e = p;\n"
			+ "        } else if (p instanceof TreeNode) {\n"
			+ "            e = ((TreeNode) p).putTreeVal(this, tab, hash, key, value);\n"
			+ "        } else {\n"
			+ "            int binCount = 0;\n"
			+ "            while (true) {\n"
			+ "                if ((e = p.next) == null) {\n"
			+ "                    p.next = this.newNode(hash, key, value, null);\n"
			+ "                    if (binCount < 7)\n"
			+ "                        break;\n"
			+ "                    this.treeifyBin(tab, hash);\n"
			+ "                    break;\n"
			+ "                }\n"
			+ "                if (e.hash == hash && ((k = e.key) == key || key != null && key.equals(k)))\n"
			+ "                    break;\n"
			+ "                p = e;\n"
			+ "                ++binCount;\n"
			+ "            }\n"
			+ "        }\n"
			+ "        if (e != null) {\n"
			+ "            Object oldValue = e.value;\n"
			+ "            if (!onlyIfAbsent || oldValue == null) {\n"
			+ "                e.value = value;\n"
			+ "            }\n"
			+ "            this.afterNodeAccess(e);\n"
			+ "            return oldValue;\n"
			+ "        }\n"
			+ "    }\n"
			+ "    ++this.modCount;\n"
			+ "    if (++this.size > this.threshold) {\n"
			+ "        this.resize();\n"
			+ "    }\n"
			+ "    this.afterNodeInsertion(evict);\n"
			+ "    return null;\n"
			+ "}\n"
			+ "\n"
			+ "Variables involved:\n"
			+ "`hashMap` is of type: `java.util.Map`, of runtime value \"{}\",\n"
			+ "`ch` is of type: `char`, of runtime value \"97\",\n"
			+ "`tmp` is of type: `int`, of runtime value \"0\",\n"
			+ "`table` is of type: `java.util.HashMap$Node[]`, of runtime value \"null\",\n"
			+ "`threshold` is of type: `int`, of runtime value \"12\",\n"
			+ "`modCount` is of type: `int`, of runtime value \"0\",\n"
			+ "\n"
			+ "we know that later `table` has the following structure and value:\n"
			+ "{\"table|java.util.HashMap$Node[]\":[\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\",\"null\"]}\n"
			+ "But we don't know which step during the execution modified the value.\n"
			+ "`table` has a field called `table.table[1]`\n"
			+ "\n"
			+ "In this example, the result is: T\n"
			+ "In the actual question, you need to analyse and get an answer, which might be T or F.\n";
	
	/* Methods */

	public static String getBackgroundContent() {
		return DEFINITION_INFERENCE_BACKGROUND + DEFINITION_INFERENCE_EXAMPLE;
	}

	public static String getBackgroundContent(VarValue rootVar) {
		return DEFINITION_INFERENCE_BACKGROUND + getExample(rootVar);
	}

	private static HashMap<String, String> getDatapointFromStep(VarValue rootVar) {

		// TARGET_VAR
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		HashMap<String, String> datapoint = new HashMap<>();
		datapoint.put(DatasetReader.TARGET_FIELD, "");
		datapoint.put(DatasetReader.VAR_NAME, "");
		datapoint.put(DatasetReader.TARGET_VAR, jsonString);
		datapoint.put(DatasetReader.SOURCE_CODE, "");
		datapoint.put(DatasetReader.INVOKED_METHODS, "");
		datapoint.put(DatasetReader.VARS_IN_STEP, "");
		datapoint.put(DatasetReader.GROUND_TRUTH, ""); // not available yet

		return datapoint;
	}

	private static String getExample(VarValue rootVar) {
		HashMap<String, String> datapoint = getDatapointFromStep(rootVar);

		ExampleSearcher exampleSearcher = new DefinitionInferenceExampleSearcher();
		String closestExample = exampleSearcher.searchForExample(datapoint);

		// TODO: add default example
		if (closestExample == null || closestExample.equals("")) {
			return "";
		}
		return closestExample;
	}

	public static String getQuestionContent(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables) {
		/* source code */
		int lineNo = step.getLineNumber();
		String location = step.getBreakPoint().getFullJavaFilePath();
		String sourceCode = TraceRecovUtils
				.processInputStringForLLM(TraceRecovUtils.getSourceCodeOfALine(location, lineNo).trim());

		/* variable properties */
		String rootVarName = rootVar.getVarName();
		String targetVarName = targetVar.getVarName();

		/* type structure */
		String jsonString = TraceRecovUtils.processInputStringForLLM(rootVar.toJSON().toString());

		/* all variables */
		Set<VarValue> variablesInStep = step.getAllVariables();

		/* invoked methods to be checked */
		Set<String> invokedMethods = TraceRecovUtils.getInvokedMethodsToBeChecked(step.getInvokingMethod());

		StringBuilder question = new StringBuilder("<Question>\n" + "Given the code as:\n```");
		question.append(sourceCode);
		question.append("```");

		// invoked methods
		SourceCodeRetriever sourceCodeRetriever = new SourceCodeRetriever();
		if (!invokedMethods.isEmpty()) {
			question.append("\n\nGiven the source code of function calls in the code:\n");
			for (String methodSig : invokedMethods) {
				String methodSourceCode = methodSig;
				if (SourceCodeDatabase.sourceCodeMap.containsKey(methodSig)) {
					methodSourceCode = SourceCodeDatabase.sourceCodeMap.get(methodSig);
				} else {
					methodSourceCode = sourceCodeRetriever.getMethodCode(methodSig,
							step.getTrace().getAppJavaClassPath());
					SourceCodeDatabase.sourceCodeMap.put(methodSig, methodSourceCode);
				}
				question.append(methodSourceCode);
				question.append("\n");
			}
		}

		// variables information (name, type, value)
		question.append("\nVariables involved:");
		for (VarValue var : step.getReadVariables()) {
			question.append("`");
			question.append(var.getVarName());
			question.append("` is of type `");
			question.append(var.getType());
			question.append("`, of runtime value \"");
			question.append(var.getStringValue());
			question.append("\",");
		}

		question.append("\n\nwe know that later `" + rootVarName + "` has the following structure and value:\n");
		question.append(jsonString);
		question.append("\n\nBut we don't know which step during the execution modified the value.\n");

		boolean isFirstVar = true;
		for (VarValue var : variablesInStep) {
			VarValue criticalVariable = null;
			if (var.getAliasVarID() != null) {
				criticalVariable = criticalVariables.stream().filter(v -> var.getAliasVarID().equals(v.getAliasVarID()))
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
				question.append(isFirstVar ? "where\n`" : "`");
				question.append(var.getVarName());
				question.append("` has the same memory address as `");
				question.append(cascadeFieldName);
				question.append("`,\n");
				isFirstVar = false;
			}
		}

		question.append("`" + rootVarName + "` has a field called `");

		String cascadeFieldName = "";
		int stopIndex = criticalVariables.size() - 1;
		for (int i = 0; i < stopIndex; i++) {
			VarValue criticalVar = criticalVariables.get(i);
			cascadeFieldName += criticalVar.getVarName() + ".";
		}
		cascadeFieldName += targetVarName;

		question.append(cascadeFieldName);
		question.append("`, does the code directly or indirectly write this field?"
				+ "\nIn your response, return T for true and F for false. Do not include explanation.");

		return question.toString();
	}

	public static boolean isModified(String response) {
		response = response.trim();
		return response.equals("T") ? true : false;
	}

}
