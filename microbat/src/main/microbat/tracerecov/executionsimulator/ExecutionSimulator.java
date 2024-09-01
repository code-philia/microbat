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
import microbat.tracerecov.candidatevar.CandidateVarVerifier;
import microbat.tracerecov.candidatevar.CandidateVarVerifier.WriteStatus;
import microbat.tracerecov.varskeleton.VarSkeletonBuilder;
import microbat.tracerecov.varskeleton.VariableSkeleton;
import sav.common.core.Pair;

/**
 * This class is used to simulate execution through LLM and retrieve
 * approximated values for candidate variables.
 * 
 * @author hongshuwang
 */
public class ExecutionSimulator {

	protected ExecutionSimulationLogger logger;

	public ExecutionSimulator() {
		this.logger = new ExecutionSimulationLogger();
	}
	
	public String sendRequest(String backgroundContent, String questionContent) throws IOException {
	    String combinedPrompt = backgroundContent + questionContent;

	    // Check if prompt exceeds max token
	    if (isExceedingMaxTokens(combinedPrompt)) {
	        return sendInSegments(backgroundContent, questionContent);
	    } else {
	        return sendSingleRequest(combinedPrompt);
	    }
	}

	// Method to send the complete prompt in a single request
	private String sendSingleRequest(String combinedPrompt) throws IOException {
	    URL url = new URL(SimulatorConstants.API_URL);
	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

	    connection.setRequestMethod("POST");
	    connection.setRequestProperty("Content-Type", "application/json");
	    connection.setRequestProperty("Authorization", "Bearer " + SimulatorConstants.API_KEY);
	    connection.setDoOutput(true);

	    JSONObject question = new JSONObject();
	    question.put("role", "user");
	    question.put("content", combinedPrompt);

	    JSONArray messages = new JSONArray();
	    messages.put(question);

	    JSONObject request = new JSONObject();
	    request.put("model", SimulatorConstants.getSelectedModel());
	    request.put("messages", messages);
	    request.put("temperature", SimulatorConstants.TEMPERATURE);
	    request.put("max_tokens", SimulatorConstants.MAX_TOKENS);
	    request.put("top_p", SimulatorConstants.TOP_P);
	    request.put("frequency_penalty", SimulatorConstants.FREQUENCY_PENALTY);
	    request.put("presence_penalty", SimulatorConstants.PRESENCE_PENALTY);

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
	            return responseObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
	                    .getString("content").trim();
	        }
	    } else {
	        throw new RuntimeException("Failed : HTTP error code : " + responseCode);
	    }
	}

	// Method to send the prompt in segments
	private String sendInSegments(String backgroundContent, String questionContent) throws IOException {
	    List<String> promptSegments = splitPrompt(backgroundContent + questionContent);

	    StringBuilder combinedResponse = new StringBuilder();

	    for (int i = 0; i < promptSegments.size(); i++) {
	        String segment = promptSegments.get(i);

	        URL url = new URL(SimulatorConstants.API_URL);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Content-Type", "application/json");
	        connection.setRequestProperty("Authorization", "Bearer " + SimulatorConstants.API_KEY);
	        connection.setDoOutput(true);

	        JSONObject question = new JSONObject();
	        question.put("role", "user");

	        if (i < promptSegments.size() - 1) {
	            question.put("content", segment + "\n(Note: Please wait for the complete input before responding with JSON only.)");
	        } else {
	            question.put("content", segment + "\n(Note: Now generate the JSON only response without any explanation.)");
	        }


	        JSONArray messages = new JSONArray();
	        messages.put(question);

	        JSONObject request = new JSONObject();
	        request.put("model", SimulatorConstants.getSelectedModel());
	        request.put("messages", messages);
	        request.put("temperature", SimulatorConstants.TEMPERATURE);
	        request.put("max_tokens", SimulatorConstants.MAX_TOKENS);
	        request.put("top_p", SimulatorConstants.TOP_P);
	        request.put("frequency_penalty", SimulatorConstants.FREQUENCY_PENALTY);
	        request.put("presence_penalty", SimulatorConstants.PRESENCE_PENALTY);

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
	                String segmentResponse = responseObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
	                        .getString("content").trim();

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
	    // We split strings using regular expressions to simulate simple word segmentation
	    // \w+ matches words, \p{Punct} matches punctuation, \s+ matches Spaces, and \d+ matches numbers
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
				String response = sendRequest(background, content);
				response = TraceRecovUtils.processInputStringForLLM(response);
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

		String background = AliasInferenceUtils.getBackgroundContent();
		String content = AliasInferenceUtils.getQuestionContent(step, rootVar, criticalVariables);

		this.logger.printInfoBeforeQuery("Alias Inferencing", criticalVariables.get(criticalVariables.size() - 1), step,
				background + content);

		for (int i = 0; i < 2; i++) {
			try {
				String response = sendRequest(background, content);
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

//		WriteStatus complication = WriteStatus.NO_GUARANTEE;
//		VarValue ancestorVarOnTrace = null;
//		for (VarValue readVarInStep : step.getReadVariables()) {
//			String aliasID = readVarInStep.getAliasVarID();
//			if (aliasID == null) {
//				continue;
//			}
//			VarValue criticalAncestor = criticalVariables.stream()
//					.filter(criticalVar -> aliasID.equals(criticalVar.getAliasVarID())).findFirst().orElse(null);
//
//			if (criticalAncestor != null) {
//				ancestorVarOnTrace = readVarInStep;
//				break;
//			}
//		}
//		if (ancestorVarOnTrace == null) {
//			complication = WriteStatus.GUARANTEE_NO_WRITE;
//		} else if (TraceRecovUtils.shouldBeChecked(ancestorVarOnTrace.getType())) {
//			complication = estimateComplication(step, ancestorVarOnTrace, targetVar);
//		}
//
//		System.out.println(targetVar.getVarName());
//		System.out.println(step.getInvokingMethod());
//		System.out.println(complication);
//
//		if (complication == WriteStatus.GUARANTEE_WRITE) {
//			return true;
//		} else if (complication == WriteStatus.GUARANTEE_NO_WRITE) {
//			return false;
//		} else {
//			return inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables);
//		}
		return inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables);
	}

	/**
	 * deterministic flow: guarantee_write, guarantee_no_write
	 * must-analysis by LLM: no_guarantee
	 */
	private WriteStatus estimateComplication(TraceNode step, VarValue parentVar, VarValue targetVar) {

		String[] invokingMethods = step.getInvokingMethod().split("%");

		for (String invokedMethod : invokingMethods) {

			try {
				CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(invokedMethod);
				if (cfg == null) {
					return WriteStatus.NO_GUARANTEE;
				}
				CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
				return candidateVarVerifier.getVarWriteStatus(targetVar.getVarName());
			} catch (CannotBuildCFGException e) {
				e.printStackTrace();
			}
		}

		return WriteStatus.NO_GUARANTEE;
	}

	private boolean inferDefinitionByLLM(TraceNode step, VarValue rootVar, VarValue targetVar,
			List<VarValue> criticalVariables) {

		String background = DefinitionInferenceUtils.getBackgroundContent();
		String content = DefinitionInferenceUtils.getQuestionContent(step, rootVar, targetVar, criticalVariables);

		this.logger.printInfoBeforeQuery("Definition Inference", targetVar, step, background + content);

		for (int i = 0; i < 2; i++) {
			try {
				String response = sendRequest(background, content);
				this.logger.printResponse(i, response);
				return DefinitionInferenceUtils.isModified(response);
			} catch (org.json.JSONException | IOException e) {
				this.logger.printError(e.getMessage());
			}
		}

		return false;
	}
}
