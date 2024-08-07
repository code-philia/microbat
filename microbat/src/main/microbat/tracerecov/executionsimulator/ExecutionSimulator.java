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
		/* set up connection */
		URL url = new URL(SimulatorConstants.API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + SimulatorConstants.API_KEY);
		connection.setDoOutput(true);

		/* construct request */
//		JSONObject background = null;
//		if (backgroundContent != null) {
//			background = new JSONObject();
//			background.put("role", "system");
//			background.put("content", backgroundContent);
//		}

		JSONObject question = new JSONObject();
		question.put("role", "user");
		question.put("content", backgroundContent + questionContent);

		JSONArray messages = new JSONArray();
//		if (backgroundContent != null) {
//			messages.put(background);
//		}
		messages.put(question);

		JSONObject request = new JSONObject();
		request.put("model", SimulatorConstants.getSelectedModel());
		request.put("messages", messages);
		request.put("temperature", SimulatorConstants.TEMPERATURE);
		request.put("max_tokens", SimulatorConstants.MAX_TOKENS);
		request.put("top_p", SimulatorConstants.TOP_P);
		request.put("frequency_penalty", SimulatorConstants.FREQUENCY_PENALTY);
		request.put("presence_penalty", SimulatorConstants.PRESENCE_PENALTY);

		/* send request */
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = request.toString().getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		/* parse response */
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
