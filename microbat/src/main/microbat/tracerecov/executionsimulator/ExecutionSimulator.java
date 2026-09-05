package microbat.tracerecov.executionsimulator;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;
import microbat.codeanalysis.bytecode.CFG;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.CannotBuildCFGException;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.executionsimulator.VariableExpansionUtils.VariableExpansionExample;
import microbat.tracerecov.staticverifiers.CandidateVarVerifier;
import microbat.tracerecov.staticverifiers.WriteStatus;
import microbat.tracerecov.varskeleton.VarSkeletonBuilder;
import microbat.tracerecov.varskeleton.VariableSkeleton;
import sav.common.core.Pair;

/**
 * This class is used to simulate execution through LLM and retrieve
 * approximated values for candidate variables.
 * 
 * @author hongshuwang
 */
@Slf4j
public abstract class ExecutionSimulator {

	protected ExecutionSimulationLogger logger;

	public ExecutionSimulator() {
		this.logger = new ExecutionSimulationLogger();
	}

	/* abstract methods to be implemented */
	protected abstract String getUrl();

	protected abstract HttpURLConnection getConnection() throws IOException;

	protected abstract String getResponseTypeString(LLMResponseType responseType);

	protected abstract JsonObject getSingleRequest(String combinedPrompt, LLMResponseType responseType);

	protected abstract String getSingleResponse(JsonObject responseObject);

	protected abstract String getAPIKey();

	public final static String[] NEED_ABSTRACT_WORD = new String[] { "linkedlist", "queue", "set", "deque", "bitset",
			"map", "hashtable", "stream", };

	/* concrete methods */
	public String sendRequest(String backgroundContent, String questionContent, LLMResponseType responseType)
			throws IOException, RuntimeException {
		String combinedPrompt = backgroundContent + questionContent;
		long start = System.currentTimeMillis();
		String requestId = UUID.randomUUID().toString();
		InstanceJsonlLogger logger = InstanceJsonlLogger.current();
		if (logger != null) {
			logger.event("llm_request_start", InstanceJsonlLogger.details("requestId", requestId,
					"responseType", responseType, "prompt", combinedPrompt));
		}

		try {
			String response;
			if (isExceedingMaxTokens(combinedPrompt)) {
				response = sendInSegments(backgroundContent, questionContent, responseType);
			} else {
				response = sendSingleRequest(combinedPrompt, responseType);
			}
			if (logger != null) {
				logger.event("llm_request_end", InstanceJsonlLogger.details("requestId", requestId,
						"durationMs", System.currentTimeMillis() - start, "response", response));
			}
			return response;
		} catch (IOException | RuntimeException e) {
			if (logger != null) {
				logger.event("llm_request_error", InstanceJsonlLogger.details("requestId", requestId,
						"durationMs", System.currentTimeMillis() - start, "error", e.toString()));
			}
			throw e;
		}
	}

	public static String dumpFilePath;
	public static BufferedOutputStream dumpOutputStream;

	public static void initDumpOutputStream() {
		if (dumpFilePath == null) {
			dumpOutputStream = null;
		} else {
			try {
				dumpOutputStream = new BufferedOutputStream(new FileOutputStream(dumpFilePath, true));
			} catch (IOException e) {
				log.error("Failed to initialize dump output stream: " + e.getMessage());
			}
		}
	}

	public static void dumpTaskName(String taskName) {
		JsonObject object = new JsonObject();
		object.addProperty("taskName", taskName);
		dumpToFile(object);
	}

	public static synchronized void dumpToFile(JsonObject object) {
		if (dumpOutputStream == null) {
			return;
		}
		try {
			byte[] bytes = object.toString().getBytes("utf-8");
			dumpOutputStream.write(bytes);
			dumpOutputStream.write('\n');
			dumpOutputStream.flush();
		} catch (IOException e) {
			log.error("Failed to write to dump file: " + e.getMessage());
		}
	}

	// Method to send the complete prompt in a single request
	private String sendSingleRequest(String combinedPrompt, LLMResponseType responseType)
			throws IOException {
		HttpURLConnection connection = getConnection();
		JsonObject request = getSingleRequest(combinedPrompt, responseType);

		dumpToFile(request);
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = request.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		Exception readException = null;
		InputStream is = null;
		try {
			if (responseCode >= 400) {
				is = connection.getErrorStream();
			} else {
				is = connection.getInputStream();
			}

			byte[] buffer = new byte[65536];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead);
			}
		} catch (Exception e) {
			readException = e;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					log.error("Failed to close input stream: " + e.getMessage());
				}
			}
		}

		byte[] bytes = baos.toByteArray();
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(bytes));
		String response = charBuffer.toString();

		if (readException != null) {
			log.error("Failed to read response {}", response, readException);
			throw new IOException("Failed to read response.", readException);
		}

		if (responseCode != HttpURLConnection.HTTP_OK) {
			RuntimeException e = new RuntimeException("Failed : HTTP error code : " + responseCode);
			log.error("Failed to send LLM request. HTTP code: {}. Content: {}", responseCode, response, e);
			throw e;
		}

		try {
			JsonElement jsonElement = com.google.gson.JsonParser.parseString(response);
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			dumpToFile(jsonObject);
			return getSingleResponse(jsonObject);
		} catch (Exception e) {
			log.error("Failed to parse response: {}", response, e);
			throw new IOException("Failed to parse response.", e);
		}
	}

	// Method to send the prompt in segments
	private String sendInSegments(String backgroundContent, String questionContent, LLMResponseType responseType)
			throws IOException, RuntimeException {
		List<String> promptSegments = splitPrompt(backgroundContent + questionContent);

		StringBuilder combinedResponse = new StringBuilder();

		String segment = promptSegments.get(0);

		HttpURLConnection connection = getConnection();
		JsonObject request = getSingleRequest(segment, responseType);

		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = request.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
				StringBuilder response = new StringBuilder();
				String responseLine;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}

				JSONObject responseObject = new JSONObject(response.toString());
				String segmentResponse = responseObject.getJSONArray("choices").getJSONObject(0)
						.getJSONObject("message").getString("content").trim();

				combinedResponse.append(segmentResponse);
			}
		} else {
			throw new RuntimeException("Failed : HTTP error code : " + responseCode);
		}

		return combinedResponse.toString();
	}

	// method to determine if the prompt exceeds max token limit
	private boolean isExceedingMaxTokens(String prompt) {
		if (prompt.length() > SimulatorConstants.MAX_TOKENS) {
			return true;
		}
		int tokenCount = customTokenizeAndCount(prompt);
		return tokenCount > SimulatorConstants.MAX_TOKENS;
	}

	private int customTokenizeAndCount(String prompt) {
		// We split strings using regular expressions to simulate simple word
		// segmentation
		// \w+ matches words, \p{Punct} matches punctuation, \s+ matches Spaces, and \d+
		// matches numbers
		String[] tokens = prompt.split("\\s+|(?=\\p{Punct})|(?<=\\p{Punct})|(?=\\d+)|(?<=\\d+)");
		return tokens.length;
	}

	// Helper method to split prompt into smaller segments
	private List<String> splitPrompt(String prompt) {
		int maxLength = SimulatorConstants.MAX_TOKENS;
		List<String> segments = new ArrayList<>();

		int length = prompt.length();
		for (int i = 0; i < length; i += maxLength) {
			segments.add(prompt.substring(i, Math.min(length, i + maxLength)));
		}

		return segments;
	}

	public String expandVariable(VarValue selectedVar, TraceNode step, Pair<String, String> preValueResponse,
			VarValue exampleVar) throws IOException {
		return expandVariable(selectedVar, step, preValueResponse, exampleVar, null);
	}

	public String expandVariable(VarValue selectedVar, TraceNode step, Pair<String, String> preValueResponse,
			VarValue exampleVar, String focalVarName)
				throws IOException {
		long moduleStart = System.currentTimeMillis();
		InstanceJsonlLogger instanceLogger = InstanceJsonlLogger.current();
		if (instanceLogger != null) {
			instanceLogger.event("module_start", InstanceJsonlLogger.details("module", "variable_expansion",
					"step", step.getOrder(), "line", step.getLineNumber(), "variable", selectedVar.getVarName()));
		}
		try {

		if (selectedVar.isExpanded()) {
			return null;
		}

		List<VariableSkeleton> variableSkeletons = new ArrayList<>();

		/*
		 * Expand the selected variable.
		 */
		VariableSkeleton parentSkeleton = VarSkeletonBuilder.getVariableStructure(selectedVar.getType(),
				step.getTrace().getAppJavaClassPath());
		if (parentSkeleton == null) {
			return null;
		}

		variableSkeletons.add(parentSkeleton);

		// assume var layer == 1, then only elementArray will be recorded in ArrayList
		if (!selectedVar.getChildren().isEmpty()) {
			VarValue child = selectedVar.getChildren().get(0);
			String childType = child.getType();
			if (childType.contains("[]")) {
				childType = childType.substring(0, childType.length() - 2); // remove [] at the end
			}
			VariableSkeleton childSkeleton = VarSkeletonBuilder.getVariableStructure(childType,
					step.getTrace().getAppJavaClassPath());
			variableSkeletons.add(childSkeleton);
		}

		// String background = exampleVar == null
		// ? VariableExpansionUtils.getBackgroundContent(selectedVar, parentSkeleton,
		// step)
		// : VariableExpansionUtils.getBackgroundContentGivenExample(exampleVar,
		// parentSkeleton, step);
		VariableExpansionExample example = exampleVar == null
				? VariableExpansionUtils.getExample(selectedVar, parentSkeleton, step)
				: VariableExpansionUtils.formatGivenExample(exampleVar, parentSkeleton, step);
		String content = VariableExpansionUtils.getQuestionContent(
				example,
				selectedVar,
				variableSkeletons,
				step,
				preValueResponse,
				focalVarName);

		this.logger.printInfoBeforeQuery("Variable Expansion", selectedVar, step, content);

		for (int i = 0; i < 5; i++) {
			try {
				// variable expansion
				long timeStart = System.currentTimeMillis();
				String response = sendRequest("", content, LLMResponseType.TEXT);
				long timeEnd = System.currentTimeMillis();
				LLMTimer.varExpansionTime += timeEnd - timeStart;

				this.logger.printResponse(i, response);
				String jsonResponse = GptTaskInfo.findPatternIn("json", response);
				VariableExpansionUtils.processResponse(selectedVar, jsonResponse);

				selectedVar.setExpanded(true);

				// data structure abstraction
				if (shouldAbstract(selectedVar.getType())) {
					String fullExpandedValue = TraceRecovUtils
							.processInputStringForLLM(selectedVar.toJSON().toString());
					selectedVar.setFullExpandedValue(fullExpandedValue);
					return abstractDataStructure(selectedVar, step, exampleVar);
				}
				return response;
			} catch (RuntimeException | IOException e) {
				this.logger.printError(e.getMessage());
				selectedVar.setExpanded(false);
			}
		}

		return null;
		} finally {
			if (instanceLogger != null) {
				instanceLogger.event("module_end", InstanceJsonlLogger.details("module", "variable_expansion",
						"durationMs", System.currentTimeMillis() - moduleStart, "success", selectedVar.isExpanded()));
			}
		}
	}

	private static boolean shouldAbstract(String typeString) {
		typeString = typeString.toLowerCase();
		for (String word : NEED_ABSTRACT_WORD) {
			if (typeString.contains(word)) {
				return true;
			}
		}
		return false;
	}

	public String abstractDataStructure(VarValue selectedVar, TraceNode step, VarValue exampleVar)
			throws IOException {
		long moduleStart = System.currentTimeMillis();
		InstanceJsonlLogger instanceLogger = InstanceJsonlLogger.current();
		if (instanceLogger != null) {
			instanceLogger.event("module_start", InstanceJsonlLogger.details("module", "data_structure_expansion",
					"step", step.getOrder(), "line", step.getLineNumber(), "variable", selectedVar.getVarName()));
		}
		try {

		if (selectedVar.isExpansionAbstracted()) {
			return null;
		}

		String background = "";
		String content = DataStructureAbstractionUtils.generatePrompt(exampleVar, selectedVar);

		this.logger.printInfoBeforeQuery("Data Structure Abstraction", selectedVar, step, background + content);

		for (int i = 0; i < 5; i++) {
			try {
				long timeStart = System.currentTimeMillis();
				String response = sendRequest(background, content, LLMResponseType.TEXT);
				long timeEnd = System.currentTimeMillis();
				LLMTimer.varExpansionTime += timeEnd - timeStart;

				this.logger.printResponse(i, response);
				DataStructureAbstractionUtils.processResponse(selectedVar, response);

				selectedVar.setExpansionAbstracted(true);
				return response;
			} catch (RuntimeException | IOException e) {
				this.logger.printError(e.getMessage());
				selectedVar.setExpansionAbstracted(false);
			}
		}

		return null;
		} finally {
			if (instanceLogger != null) {
				instanceLogger.event("module_end", InstanceJsonlLogger.details("module", "data_structure_expansion",
						"durationMs", System.currentTimeMillis() - moduleStart,
						"success", selectedVar.isExpansionAbstracted()));
			}
		}
	}

	/**
	 * Return a map with key: written_field, value: variable_on_trace
	 */
	public Map<VarValue, VarValue> inferAliasRelations(TraceNode step, VarValue rootVar,
			List<VarValue> criticalVariables) throws IOException {
		return inferAliasRelationsByLLM(step, rootVar, criticalVariables);
	}

	public Map<VarValue, VarValue> inferAliasRelationsByLLM(TraceNode step, VarValue rootVar,
			List<VarValue> criticalVariables) throws IOException {
		long moduleStart = System.currentTimeMillis();
		InstanceJsonlLogger instanceLogger = InstanceJsonlLogger.current();
		if (instanceLogger != null) {
			instanceLogger.event("module_start", InstanceJsonlLogger.details("module", "alias_inference",
					"step", step.getOrder(), "line", step.getLineNumber(), "rootVariable", rootVar.getVarName(),
					"criticalVariableCount", criticalVariables.size()));
		}
		try {

		String background = AliasInferenceUtils.getBackgroundContent();
		String content = AliasInferenceUtils.getQuestionContent(step, rootVar, criticalVariables);

		this.logger.printInfoBeforeQuery("Alias Inferencing", criticalVariables.get(criticalVariables.size() - 1), step,
				background + content);

		for (int i = 0; i < 2; i++) {
			try {
				long timeStart = System.currentTimeMillis();
				String response = sendRequest(background, content, LLMResponseType.JSON);
				long timeEnd = System.currentTimeMillis();
				LLMTimer.aliasInferTime += timeEnd - timeStart;

				this.logger.printResponse(i, response);
				return AliasInferenceUtils.processResponse(response, rootVar, step);
			} catch (RuntimeException | IOException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return new HashMap<>();
		} finally {
			if (instanceLogger != null) {
				instanceLogger.event("module_end", InstanceJsonlLogger.details("module", "alias_inference",
						"durationMs", System.currentTimeMillis() - moduleStart));
			}
		}
	}

	public boolean inferDefinition(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables, TraceNode srcStep, String focalVarName) {

		if (shouldAbstract(rootVar.getType())) {
			return inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables, srcStep, focalVarName);
		}
		if (1 + 1 == 2) {
			return inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables, srcStep, focalVarName);
		}

		WriteStatus complication = WriteStatus.NO_GUARANTEE;
		VarValue ancestorVarOnTrace = null;
		for (VarValue readVarInStep : step.getReadVariables()) {
			String aliasID = readVarInStep.getAliasVarID();
			if (aliasID == null) {
				continue;
			}
			VarValue criticalAncestor = criticalVariables.stream()
					.filter(criticalVar -> aliasID.equals(criticalVar.getAliasVarID())).findFirst().orElse(null);

			if (criticalAncestor != null) {
				ancestorVarOnTrace = readVarInStep;
				break;
			}
		}
		if (ancestorVarOnTrace == null) {
			complication = WriteStatus.GUARANTEE_NO_WRITE;
		} else if (TraceRecovUtils.shouldBeChecked(ancestorVarOnTrace.getType())) {
			complication = estimateComplication(step, ancestorVarOnTrace, criticalVariables);
		}

		System.out.println(targetVar.getVarName());
		System.out.println(step.getInvokingMethod());
		System.out.println(complication);

		if (complication == WriteStatus.GUARANTEE_WRITE) {
			return true;
		} else if (complication == WriteStatus.GUARANTEE_NO_WRITE) {
			return false;
		} else {
			return inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables, srcStep, focalVarName);
		}
	}

	private VarValue findMatchingVar(TraceNode step, VarValue rootVar) {
		for (VarValue var : step.getAllVariables()) {
			String aliasVarID = var.getAliasVarID();
			String varID = var.getVarID();

			// TODO: check whether fields of rootVar match with vars in step
			// This check is omitted since alias inference is disabled
			if (rootVar.getAliasVarID() != null && rootVar.getAliasVarID().equals(aliasVarID)) {
				return var;
			}
			if (rootVar.getVarID() != null && rootVar.getVarID().equals(varID)) {
				return var;
			}
		}
		return null;
	}

	/**
	 * deterministic flow: guarantee_write, guarantee_no_write
	 * must-analysis by LLM: no_guarantee
	 * 
	 * Algorithm:
	 * 1. if any method writes the targetVar, stop and return GUARANTEE_WRITE
	 * 2. if the write status of any method cannot be determined, the overall status
	 * is NO_GUARANTEE
	 * 3. if all methods are guaranteed not to write to targetVar, the overall
	 * status is GUARANTEE_NO_WRITE
	 */
	private WriteStatus estimateComplication(TraceNode step, VarValue parentVar, List<VarValue> criticalVariables) {

		String[] invokingMethods = step.getInvokingMethod().split("%");
		String parentVarType = parentVar.getType();

		for (String invokedMethod : invokingMethods) {

			String invokingType = invokedMethod.split("#")[0];

			if (!TraceRecovUtils.isAssignable(invokingType, parentVarType, step.getTrace().getAppJavaClassPath())) {
				continue; // GUARANTEE_NO_WRITE
			} else {
				invokedMethod = parentVarType + "#" + invokedMethod.split("#")[1]; // replace general type by runtime
																					// type
			}

			try {
				CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(invokedMethod);
				if (cfg == null) {
					return WriteStatus.NO_GUARANTEE;
				}

				CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
				StringBuilder cascadingNameBuilder = new StringBuilder();
				for (int i = 1; i < criticalVariables.size(); i++) {
					VarValue var = criticalVariables.get(i);
					cascadingNameBuilder.append(var.getVarName());
					if (i < criticalVariables.size() - 1) {
						cascadingNameBuilder.append(".");
					}
				}

				WriteStatus methodWriteStatus = candidateVarVerifier.getVarWriteStatus(cascadingNameBuilder.toString(),
						invokedMethod);
				if (methodWriteStatus == WriteStatus.GUARANTEE_WRITE || methodWriteStatus == WriteStatus.NO_GUARANTEE) {
					return methodWriteStatus;
				}
			} catch (CannotBuildCFGException e) {
				e.printStackTrace();
			}
		}

		return WriteStatus.GUARANTEE_NO_WRITE;
	}

	public static String targetFileName = null;
	public static int targetLineNumber = -1;
	public static Writer defInfWriter = null;

	private boolean inferDefinitionByLLM(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables, TraceNode srcStep, String focalVarName) {
		long moduleStart = System.currentTimeMillis();
		InstanceJsonlLogger instanceLogger = InstanceJsonlLogger.current();
		if (instanceLogger != null) {
			instanceLogger.event("module_start", InstanceJsonlLogger.details("module", "definition_inference",
					"step", step.getOrder(), "line", step.getLineNumber(), "targetVariable", targetVar.getVarName()));
		}
		try {

		VarValue matchedVar = findMatchingVar(step, rootVar);

		if (matchedVar == null) {
			log.error("Cannot find matching variable for rootVar: {}", rootVar.getVarName());
			return false;
		}

		if (!matchedVar.isExpanded()) {
			try {
				expandVariable(matchedVar, step, null, rootVar, focalVarName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		String targetClass = step.getClassCanonicalName();
		int targetLine = step.getLineNumber();
		Writer w = defInfWriter;

		String background = "";
		String content = DefinitionInferenceUtils.generatePrompt(step, rootVar, targetVar, criticalVariables,
				srcStep);

		// System.out.println("Definition Inference------------------------------------------------");
		// System.out.println(background);
		// System.out.println(content);
		// this.logger.printInfoBeforeQuery("Definition Inference", targetVar, step, background + content);

		for (int i = 0; i < 2; i++) {
			try {
				long timeStart = System.currentTimeMillis();
				String response = sendRequest(background, content, LLMResponseType.TEXT);
				long timeEnd = System.currentTimeMillis();
				LLMTimer.defInferTime += timeEnd - timeStart;

				// this.logger.printResponse(i, response);
				boolean res = DefinitionInferenceUtils.isModified(response);
				if(w != null) {
					JSONObject obj = new JSONObject();
					obj.put("tgt_file", targetFileName);
					obj.put("tgt_line", targetLineNumber);
					obj.put("gt_class", targetClass);
					obj.put("gt_line", targetLine);
					obj.put("prompt", content);
					obj.put("response", response);
					obj.put("result", res);

					synchronized (w) {
						w.write(obj.toString() + "\n");
						w.flush();
					}
				}
				return res;
			} catch (IOException | RuntimeException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return false;
		} finally {
			if (instanceLogger != null) {
				instanceLogger.event("module_end", InstanceJsonlLogger.details("module", "definition_inference",
						"durationMs", System.currentTimeMillis() - moduleStart));
			}
		}
	}

	public static final Pattern VARIABLE_ROOT_NAME = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)[\\.\\[]?.*$");

	public String getCriticalVar(TraceNode slicingCriterion, String criticalVarName) {
		long moduleStart = System.currentTimeMillis();
		InstanceJsonlLogger instanceLogger = InstanceJsonlLogger.current();
		if (instanceLogger != null) {
			instanceLogger.event("module_start", InstanceJsonlLogger.details("module", "critical_variable_identification",
					"step", slicingCriterion.getOrder(), "line", slicingCriterion.getLineNumber(),
					"criticalVariable", criticalVarName));
		}
		try {
		Set<String> variableNames = new HashSet<>();
		for (VarValue var : slicingCriterion.getAllVariables()) {
			String name = var.getVarName();
			variableNames.add(name);
		}
		Matcher matcher = VARIABLE_ROOT_NAME.matcher(criticalVarName);
		if (matcher.find()) {
			String rootName = matcher.group(1);
			if (variableNames.contains(rootName)) {
				return rootName;
			}
		}

		String prompt = CriticalVarUtils.getPromptForVarIdentification(slicingCriterion, criticalVarName);

		System.out.println(prompt);

		for (int i = 0; i < 3; i++) {
			try {
				String response = sendRequest("", prompt, LLMResponseType.TEXT);
				this.logger.printResponse(i, response);
				return GptTaskInfo.findLabelIn("variable", response);
			} catch (IOException | RuntimeException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return "";
		} finally {
			if (instanceLogger != null) {
				instanceLogger.event("module_end", InstanceJsonlLogger.details("module", "critical_variable_identification",
						"durationMs", System.currentTimeMillis() - moduleStart));
			}
		}
	}

	public String getCriticalField(VarValue rootVar, TraceNode slicingCriterion, String criticalVarName) {
		long moduleStart = System.currentTimeMillis();
		InstanceJsonlLogger instanceLogger = InstanceJsonlLogger.current();
		if (instanceLogger != null) {
			instanceLogger.event("module_start", InstanceJsonlLogger.details("module", "critical_field_identification",
					"step", slicingCriterion.getOrder(), "line", slicingCriterion.getLineNumber(),
					"rootVariable", rootVar.getVarName()));
		}
		try {
		String prompt = CriticalVarUtils.getPromptForFieldIdentification(rootVar, slicingCriterion, criticalVarName);

		Set<String> fieldNames = new HashSet<>();
		for (VarValue field : rootVar.getAllDescedentChildren()) {
			fieldNames.add(field.getVarName());
		}

		if (fieldNames.isEmpty()) {
			return "";
		}

		System.out.println(prompt);

		for (int i = 0; i < 3; i++) {
			try {
				String response = sendRequest("", prompt, LLMResponseType.TEXT);
				response = GptTaskInfo.findLabelIn("field", response);
				if (!fieldNames.contains(response)) {
					RuntimeException e = new RuntimeException("The field name is not in the list of field names.");
					log.error("The field name is not in the list of field names. Expected: {}, Found: {}", fieldNames,
							response, e);
					throw e;
				}
				this.logger.printResponse(i, response);
				return response.strip();
			} catch (Exception e) {
				this.logger.printError(e.getMessage());
			}
		}

		return "";
		} finally {
			if (instanceLogger != null) {
				instanceLogger.event("module_end", InstanceJsonlLogger.details("module", "critical_field_identification",
						"durationMs", System.currentTimeMillis() - moduleStart));
			}
		}
	}
}
