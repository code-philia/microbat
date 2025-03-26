package microbat.tracerecov.executionsimulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import microbat.codeanalysis.bytecode.CFG;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.CannotBuildCFGException;
import microbat.tracerecov.TraceRecovUtils;
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
public abstract class ExecutionSimulator {

	protected ExecutionSimulationLogger logger;

	public ExecutionSimulator() {
		this.logger = new ExecutionSimulationLogger();
	}

	/* abstract methods to be implemented */
	protected abstract String getUrl();

	protected abstract HttpURLConnection getConnection() throws IOException;

	protected abstract String getResponseTypeString(LLMResponseType responseType);

	protected abstract JSONObject getSingleRequest(String combinedPrompt, LLMResponseType responseType);

	protected abstract String getSingleResponse(JSONObject responseObject);

	protected abstract String getAPIKey();

	public final static String[] NEED_ABSTRACT_WORD = new String[] { "linkedlist", "queue", "set", "deque", "bitset",
			"map", "hashtable", "stream", };

	/* concrete methods */
	public String sendRequest(String backgroundContent, String questionContent, LLMResponseType responseType)
			throws IOException, RuntimeException {
		String combinedPrompt = backgroundContent + questionContent;

		// Check if prompt exceeds max token
		if (isExceedingMaxTokens(combinedPrompt)) {
			return sendInSegments(backgroundContent, questionContent, responseType);
		} else {
			return sendSingleRequest(combinedPrompt, responseType);
		}
	}

	// Method to send the complete prompt in a single request
	private String sendSingleRequest(String combinedPrompt, LLMResponseType responseType)
			throws IOException, RuntimeException {
		HttpURLConnection connection = getConnection();
		JSONObject request = getSingleRequest(combinedPrompt, responseType);

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
				return getSingleResponse(responseObject);
			}
		} else {
			throw new RuntimeException("Failed : HTTP error code : " + responseCode);
		}
	}

	// Method to send the prompt in segments
	private String sendInSegments(String backgroundContent, String questionContent, LLMResponseType responseType)
			throws IOException, RuntimeException {
		List<String> promptSegments = splitPrompt(backgroundContent + questionContent);

		StringBuilder combinedResponse = new StringBuilder();

		String segment = promptSegments.get(0);

		HttpURLConnection connection = getConnection();
		JSONObject request = getSingleRequest(segment, responseType);

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

	public String expandVariable(VarValue selectedVar, TraceNode step, Pair<String, String> preValueResponse, VarValue exampleVar)
			throws IOException {

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

		String background = exampleVar == null
				? VariableExpansionUtils.getBackgroundContent(selectedVar, parentSkeleton, step)
				: VariableExpansionUtils.getBackgroundContentGivenExample(exampleVar, parentSkeleton, step);
		String content = VariableExpansionUtils.getQuestionContent(selectedVar, variableSkeletons, step,
				preValueResponse);

		this.logger.printInfoBeforeQuery("Variable Expansion", selectedVar, step, background + content);

		for (int i = 0; i < 5; i++) {
			try {
				// variable expansion
				long timeStart = System.currentTimeMillis();
				String response = sendRequest(background, content, LLMResponseType.JSON);
				long timeEnd = System.currentTimeMillis();
				LLMTimer.varExpansionTime += timeEnd - timeStart;

				this.logger.printResponse(i, response);
				VariableExpansionUtils.processResponse(selectedVar, response);
				
				selectedVar.setExpanded(true);

				// data structure abstraction
				if (shouldAbstract(selectedVar.getType())) {
					String fullExpandedValue = TraceRecovUtils.processInputStringForLLM(selectedVar.toJSON().toString());
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

		if (selectedVar.isExpansionAbstracted()) {
			return null;
		}

		String background = DataStructureAbstractionUtils.getBackgroundContent(exampleVar);
		String content = DataStructureAbstractionUtils.getQuestionContent(selectedVar);

		this.logger.printInfoBeforeQuery("Data Structure Abstraction", selectedVar, step, background + content);

		for (int i = 0; i < 5; i++) {
			try {
				long timeStart = System.currentTimeMillis();
				String response = sendRequest(background, content, LLMResponseType.JSON);
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
	}

	public boolean inferDefinition(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables, TraceNode srcStep) {

		if (shouldAbstract(rootVar.getType())) {
			return inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables, srcStep);
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
			return inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables, srcStep);
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
	 * 2. if the write status of any method cannot be determined, the overall status is NO_GUARANTEE
	 * 3. if all methods are guaranteed not to write to targetVar, the overall status is GUARANTEE_NO_WRITE
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

	private boolean inferDefinitionByLLM(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables, TraceNode srcStep) {

		VarValue matchedVar = findMatchingVar(step, rootVar);

		if (!matchedVar.isExpanded()) {
			try {
				expandVariable(matchedVar, step, null, rootVar);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		String background = DefinitionInferenceUtils.getBackgroundContent();
		String content = DefinitionInferenceUtils.getQuestionContent(step, rootVar, targetVar, criticalVariables, srcStep);

		System.out.println("Definition Inference------------------------------------------------");
		System.out.println(background);
		System.out.println(content);

		this.logger.printInfoBeforeQuery("Definition Inference", targetVar, step, background + content);

		for (int i = 0; i < 2; i++) {
			try {
				long timeStart = System.currentTimeMillis();
				String response = sendRequest(background, content, LLMResponseType.TEXT);
				long timeEnd = System.currentTimeMillis();
				LLMTimer.defInferTime += timeEnd - timeStart;

				this.logger.printResponse(i, response);
				return DefinitionInferenceUtils.isModified(response);
			} catch (IOException | RuntimeException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return false;
	}
	
	public String getCriticalVar(TraceNode slicingCriterion, String criticalVarName) {
		String prompt = CriticalVarUtils.getPromptForVarIdentification(slicingCriterion, criticalVarName);
		
		System.out.println(prompt);

		for (int i = 0; i < 2; i++) {
			try {

				String response = sendRequest("", prompt, LLMResponseType.TEXT);
				this.logger.printResponse(i, response);
				return response.strip();
			} catch (IOException | RuntimeException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return "";
	}

	public String getCriticalField(VarValue rootVar, TraceNode slicingCriterion, String criticalVarName) {
		String prompt = CriticalVarUtils.getPromptForFieldIdentification(rootVar, slicingCriterion, criticalVarName);

		System.out.println(prompt);

		for (int i = 0; i < 2; i++) {
			try {

				String response = sendRequest("", prompt, LLMResponseType.TEXT);
				this.logger.printResponse(i, response);
				return response.strip();
			} catch (IOException | RuntimeException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return "";
	}
}
