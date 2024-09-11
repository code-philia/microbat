package microbat.tracerecov.autoprompt;

import java.util.HashMap;

import microbat.tracerecov.autoprompt.dataset.DatasetReader;

public class DefinitionInferencePromptTemplateFiller extends PromptTemplateFiller {

	private static String definitionInferencePromptExample = 
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

	@Override
	public String getDefaultPromptExample() {
		return definitionInferencePromptExample;
	}

	// TODO: update later (not used for now)
	@Override
	public String getPromptQuestion(HashMap<String, String> datapoint) {
		return null;
	}

	// TODO: update later (not used for now)
	@Override
	public String getPrompt(HashMap<String, String> datapoint, String example) {
		return null;
	}

	// TODO: update later (not used for now)
	@Override
	public String getDefaultPrompt(HashMap<String, String> datapoint) {
		return null;
	}

	@Override
	public String getExample(HashMap<String, String> datapoint, String groundTruth) {
		/* datapoint features */
		String targetField = datapoint.get(DatasetReader.TARGET_FIELD);
		String rootVarName = datapoint.get(DatasetReader.VAR_NAME);
		String classStructure = datapoint.get(DatasetReader.TARGET_VAR);
		String sourceCode = datapoint.get(DatasetReader.SOURCE_CODE);
		String invokedMethods = datapoint.get(DatasetReader.INVOKED_METHODS).strip().replace("\\n", "\n")
				.replace("\\r", "\r").replace("***", "\n");
		String[] varsInStep = datapoint.get(DatasetReader.VARS_IN_STEP).split(",");

		StringBuilder stringBuilder = new StringBuilder("\n\n<Example>\r\n");

		stringBuilder.append("Given the code as:\n```" + sourceCode + "```");

		if (!invokedMethods.isEmpty()) {
			stringBuilder.append("\n\nGiven the source code of function calls in the code:\n" + invokedMethods);
		}

		stringBuilder.append("\nVariables involved:\n");
		for (String var : varsInStep) {
			int startIndex = var.indexOf("{");
			int endIndex = var.lastIndexOf("}");
			var = var.substring(startIndex + 1, endIndex);

			String nameAndType = var.split(":")[0]; // assume one key only
			String name = nameAndType.split("\\|")[0];
			String type = nameAndType.split("\\|")[1];
			String value = var.split(":")[1];
			stringBuilder.append("`" + name + "` is of type: `" + type + "`, of runtime value \"" + value + "\",\n");
		}

		stringBuilder.append("\nwe know that later `" + rootVarName + "` has the following structure and value:\n");
		stringBuilder.append(classStructure);
		stringBuilder.append("\nBut we don't know which step during the execution modified the value.\n");

		stringBuilder.append("`" + rootVarName + "` has a field called `" + targetField + "`");

		stringBuilder.append("\n\nIn this example, the result is: " + groundTruth + "\n");
		stringBuilder.append("In the actual question, you need to analyse and get an answer, which might be T or F.\n");

		return stringBuilder.toString();
	}

	/*
	 * Adjustment Prompt TODO: update later (not used for now)
	 */

	@Override
	public String getAdjustmentPrompt(HashMap<String, String> datapoint, String example) {
		return null;
	}

	@Override
	public String getDefaultAdjustmentPrompt(HashMap<String, String> datapoint) {
		return null;
	}

	@Override
	public String getAdjustmentPromptWithLoss(String example, HashMap<String, String> datapoint, String output,
			String textualLoss) {
		return null;
	}

}
