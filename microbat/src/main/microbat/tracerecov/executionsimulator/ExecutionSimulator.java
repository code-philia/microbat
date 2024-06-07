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

import microbat.Activator;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.MicrobatPreference;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.candidatevar.CandidateVarVerificationException;
import microbat.tracerecov.candidatevar.CandidateVarVerifier;
import microbat.tracerecov.candidatevar.CandidateVarVerifier.WriteStatus;
import microbat.tracerecov.varexpansion.VarSkeletonBuilder;
import microbat.tracerecov.varexpansion.VariableSkeleton;

/**
 * This class is used to simulate execution through LLM and retrieve
 * approximated values for candidate variables.
 * 
 * @author hongshuwang
 */
public class ExecutionSimulator {

	private static String apiKey = Activator.getDefault().getPreferenceStore().getString(MicrobatPreference.API_KEY);

	public ExecutionSimulator() {
	}

	public void expandVariable(VarValue selectedVar, TraceNode step)
			throws IOException {
		
		if(selectedVar.isExpanded()) {
			return;
		}
		
		System.out.println("***Variable Expansion***");
		System.out.println();
		
		List<VariableSkeleton> variableSkeletons = new ArrayList<>();

		/*
		 * Expand the selected variable.
		 */
		VariableSkeleton parentSkeleton = VarSkeletonBuilder.getVariableStructure(selectedVar.getType(), step.getTrace().getAppJavaClassPath());
			variableSkeletons.add(parentSkeleton);
			
		// assume var layer == 1, then only elementArray will be recorded in ArrayList
		if (!selectedVar.getChildren().isEmpty()) {
			VarValue child = selectedVar.getChildren().get(0);
			String childType = child.getType();
			if (childType.contains("[]")) {
				childType = childType.substring(0, childType.length() - 2); // remove [] at the end
			}
			VariableSkeleton childSkeleton = VarSkeletonBuilder.getVariableStructure(childType, step.getTrace().getAppJavaClassPath());
			variableSkeletons.add(childSkeleton);
		}

		String background = VariableExpansionUtils.getBackgroundContent();
		String content = VariableExpansionUtils.getQuestionContent(selectedVar, variableSkeletons, step);
		System.out.println(background);
		System.out.println(content);

		for (int i = 0; i < 2; i++) {
			try {
				String response = sendRequest(background, content);
				System.out.println(i + "th try with GPT to generate response as " + response);
				VariableExpansionUtils.processResponse(selectedVar, response);
				break;
			} catch (org.json.JSONException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Return a map with key: written_field, value: variable_on_trace
	 */
	public Map<VarValue, VarValue> inferenceAliasRelations(TraceNode step, VarValue rootVar, List<VarValue> criticalVariables) throws IOException {
		System.out.println();
		System.out.println("***Alias Inferencing***");
		System.out.println();

		String background = AliasInferenceUtils.getBackgroundContent();
		String content = AliasInferenceUtils.getQuestionContent(step, rootVar, criticalVariables);
		System.out.println(background);
		System.out.println(content);

		for (int i = 0; i < 2; i++) {
			try {
				String response = sendRequest(background, content);
				System.out.println(i + "th try with GPT to generate response as " + response);
				return AliasInferenceUtils.processResponse(response, rootVar, step);
			} catch (org.json.JSONException e) {
				e.printStackTrace();
			}
		}
		
		return new HashMap<>();
	}

	// TODO: Not Used
//	public void recoverLinkageSteps() throws IOException {
//		List<TraceNode> steps = VariableGraph.getPotentialLinkageSteps();
//		String background = LinkageEstimationUtils.getBackgroundContent();
//		String content = LinkageEstimationUtils.getQuestionContent(steps);
//		System.out.println(background);
//		System.out.println(content);
//
//		String response = sendRequest(background, content);
//		System.out.println(response);
//
//		LinkageEstimationUtils.processResponse(response, steps);
//	}

//	public void sendRequests() throws IOException {
//		String variableID = VariableGraph.getNextNodeIDToVisit();
//		while (variableID != null) {
//			List<TraceNode> relevantSteps = VariableGraph.getRelevantSteps(variableID);
//			if (!relevantSteps.isEmpty()) {
//				if (VariableGraph.hasChildren(variableID)) {
//					String background = SimulationUtilsWithCandidateVar.getBackgroundContent();
//					String content = SimulationUtilsWithCandidateVar.getQuestionContent(variableID, relevantSteps);
//					System.out.println(background);
//					System.out.println(content);
//
//					String response = this.sendRequest(background, content);
//					System.out.println(response);
//
//					SimulationUtilsWithCandidateVar.processResponse(response, variableID, relevantSteps);
//				} else {
//					String background = SimulationUtils.getBackgroundContent();
//					String content = SimulationUtils.getQuestionContent(variableID, relevantSteps);
//					System.out.println(background);
//					System.out.println(content);
//
//					String response = this.sendRequest(background, content);
//					System.out.println(response);
//					SimulationUtils.processResponse(response, variableID, relevantSteps);
//				}
//			}
//
//			VariableGraph.addCurrentToParentVariables();
//			variableID = VariableGraph.getNextNodeIDToVisit();
//		}
//	}

	private String sendRequest(String backgroundContent, String questionContent) throws IOException {
		/* set up connection */
		URL url = new URL(SimulatorConstants.API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
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
		request.put("model", SimulatorConstants.GPT4O);
		request.put("messages", messages);
		request.put("temperature", SimulatorConstants.TEMPERATURE);

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

	public boolean inferDefinition(TraceNode step, VarValue rootVar, VarValue targetVar, List<VarValue> criticalVariables) {
		
		WriteStatus complication = WriteStatus.NO_GUARANTEE;
		String targetFieldName = targetVar.getVarName();
		if (targetFieldName.contains("[") || targetFieldName.contains("]")) {
			// always query LLM for elements in array
			complication = WriteStatus.NO_GUARANTEE;
		} else {
			VarValue ancestorVarOnTrace = null;
			for (VarValue readVarInStep : step.getReadVariables()) {
				String aliasID = readVarInStep.getAliasVarID();
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
		}
		
		System.out.println(targetVar.getVarName());
		System.out.println(step.getInvokingMethod());
		System.out.println(complication);
		
		if(complication == WriteStatus.GUARANTEE_WRITE) {
			return true;
		}
		else if(complication == WriteStatus.GUARANTEE_NO_WRITE) {
			return false;
		}
		else {
			boolean def = false;
			try {
				def = inferDefinitionByLLM(step, rootVar, targetVar, criticalVariables);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return def;
		}
	}

	/**
	 * we only care about deterministic flow
	 * 
	 * must-analysis
	 * 
	 * guarantee write: 1. 2.
	 * 
	 * guarantee no-write 1. 2
	 * 
	 */
	private WriteStatus estimateComplication(TraceNode step, VarValue parentVar, VarValue targetVar) {
		
		String[] invokingMethods = step.getInvokingMethod().split("%");
		
		for (String methodSig : invokingMethods) {
			if (!methodSig.contains("#")) {
				continue;
			}
			CandidateVarVerifier candidateVarVerifier;
			try {
				candidateVarVerifier = new CandidateVarVerifier(parentVar.getType());
				// TODO: change to field name with path
				WriteStatus writeStatus = candidateVarVerifier.verifyCandidateVariable(methodSig, targetVar.getVarName());
				if (writeStatus != WriteStatus.NO_GUARANTEE) {
					return writeStatus;
				}
			} catch (CandidateVarVerificationException e) {
				e.printStackTrace();
			}
		}
		
		return WriteStatus.NO_GUARANTEE;
	}

	private boolean inferDefinitionByLLM(TraceNode step, VarValue rootVar, VarValue targetVar, List<VarValue> criticalVariables) throws IOException {
		System.out.println();
		System.out.println("***Definition Inference***");
		System.out.println();

		String background = DefinitionInferenceUtils.getBackgroundContent();
		String content = DefinitionInferenceUtils.getQuestionContent(step, rootVar, targetVar, criticalVariables);
		System.out.println(background);
		System.out.println(content);

		for (int i = 0; i < 2; i++) {
			try {
				String response = sendRequest(background, content);
				System.out.println(i + "th try with GPT to generate response as " + response);
				return DefinitionInferenceUtils.isModified(response);
			} catch (org.json.JSONException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
}
