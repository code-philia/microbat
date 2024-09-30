package microbat.tracerecov.executionsimulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
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

	/* concrete methods */
	public String sendRequest(String backgroundContent, String questionContent, LLMResponseType responseType)
			throws IOException {
		String combinedPrompt = backgroundContent + questionContent;

		// Check if prompt exceeds max token
		if (isExceedingMaxTokens(combinedPrompt)) {
			return sendInSegments(backgroundContent, questionContent, responseType);
		} else {
			return sendSingleRequest(combinedPrompt, responseType);
		}
	}

	// Method to send the complete prompt in a single request
	private String sendSingleRequest(String combinedPrompt, LLMResponseType responseType) throws IOException {
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
			throws IOException {
		List<String> promptSegments = splitPrompt(backgroundContent + questionContent);

		StringBuilder combinedResponse = new StringBuilder();

		for (int i = 0; i < promptSegments.size(); i++) {
			String segment = promptSegments.get(i);

			URL url = new URL(getUrl());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + SimulatorConstants.API_KEY);
			connection.setDoOutput(true);

			JSONObject question = new JSONObject();
			question.put("role", "user");

			if (i < promptSegments.size() - 1) {
				question.put("content",
						segment + "\n(Note: Please wait for the complete input before responding with JSON only.)");
			} else {
				question.put("content",
						segment + "\n(Note: Now generate the JSON only response without any explanation.)");
			}

			JSONArray messages = new JSONArray();
			messages.put(question);

			JSONObject responseFormat = new JSONObject();
			responseFormat.put("type", responseType);

			JSONObject request = new JSONObject();
			request.put("model", SimulatorConstants.getSelectedModel());
			request.put("messages", messages);
			request.put("temperature", SimulatorConstants.TEMPERATURE);
			request.put("max_tokens", SimulatorConstants.MAX_TOKENS);
			request.put("top_p", SimulatorConstants.TOP_P);
			request.put("frequency_penalty", SimulatorConstants.FREQUENCY_PENALTY);
			request.put("presence_penalty", SimulatorConstants.PRESENCE_PENALTY);
			request.put("response_format", responseFormat);

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = request.toString().getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), "utf-8"))) {
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
		}

		return combinedResponse.toString();
	}

	// method to determine if the prompt exceeds max token limit
	private boolean isExceedingMaxTokens(String prompt) {
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

	public String expandVariable(VarValue selectedVar, TraceNode step, Pair<String, String> preValueResponse)
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

		String background = VariableExpansionUtils.getBackgroundContent(selectedVar, parentSkeleton, step);
		String content = VariableExpansionUtils.getQuestionContent(selectedVar, variableSkeletons, step,
				preValueResponse);

		this.logger.printInfoBeforeQuery("Variable Expansion", selectedVar, step, background + content);

		for (int i = 0; i < 2; i++) {
			try {
				long timeStart = System.currentTimeMillis();
				String response = sendRequest(background, content, LLMResponseType.JSON);
				long timeEnd = System.currentTimeMillis();
				LLMTimer.varExpansionTime += timeEnd - timeStart;

//				response = TraceRecovUtils.processOutputStringForLLM(response);
				this.logger.printResponse(i, response);
				VariableExpansionUtils.processResponse(selectedVar, response);
				return response;
				// break;
			} catch (org.json.JSONException | java.lang.StringIndexOutOfBoundsException e) {
				this.logger.printError(e.getMessage());
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
			} catch (org.json.JSONException | java.lang.StringIndexOutOfBoundsException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return new HashMap<>();
	}

	public boolean inferDefinition(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables) {

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
			complication = estimateComplication(step, ancestorVarOnTrace, targetVar);
		}

		System.out.println(targetVar.getVarName());
		System.out.println(step.getInvokingMethod());
		System.out.println(complication);

		if (complication == WriteStatus.GUARANTEE_WRITE) {
			return true;
		} else if (complication == WriteStatus.GUARANTEE_NO_WRITE) {
			return false;
		} else {
			return inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables);
		}
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
	private WriteStatus estimateComplication(TraceNode step, VarValue parentVar, VarValue targetVar) {

		String[] invokingMethods = step.getInvokingMethod().split("%");
		String parentVarType = parentVar.getType();

		for (String invokedMethod : invokingMethods) {

			String invokingType = invokedMethod.split("#")[0];

			if (!TraceRecovUtils.isAssignable(invokingType, parentVarType, step.getTrace().getAppJavaClassPath())) {
				continue; // GUARANTEE_NO_WRITE
			}

			try {
				CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(invokedMethod);
				if (cfg == null) {
					return WriteStatus.NO_GUARANTEE;
				}
				CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
				WriteStatus methodWriteStatus = candidateVarVerifier.getVarWriteStatus(targetVar.getVarName(),
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
			List<VarValue> criticalVariables) {

		String background = DefinitionInferenceUtils.getBackgroundContent();
		String content = DefinitionInferenceUtils.getQuestionContent(step, rootVar, targetVar, criticalVariables);

		this.logger.printInfoBeforeQuery("Definition Inference", targetVar, step, background + content);

		for (int i = 0; i < 2; i++) {
			try {
				long timeStart = System.currentTimeMillis();
				String response = sendRequest(background, content, LLMResponseType.TEXT);
				long timeEnd = System.currentTimeMillis();
				LLMTimer.defInferTime += timeEnd - timeStart;

				this.logger.printResponse(i, response);
				return DefinitionInferenceUtils.isModified(response);
			} catch (org.json.JSONException | IOException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return false;
	}
}
