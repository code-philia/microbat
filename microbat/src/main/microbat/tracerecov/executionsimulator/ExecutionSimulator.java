package microbat.tracerecov.executionsimulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.tracerecov.VariableGraph;
import microbat.tracerecov.varexpansion.VariableSkeleton;

/**
 * This class is used to simulate execution through LLM and retrieve
 * approximated values for candidate variables.
 * 
 * @author hongshuwang
 */
public class ExecutionSimulator {

	private static String apiKey = "";

	public ExecutionSimulator() {
	}
	
	public void expandVariable(VarValue selectedVar, List<VariableSkeleton> variableSkeletons, TraceNode step) throws IOException {
		String background = VariableExpansionUtils.getBackgroundContent();
		String content = VariableExpansionUtils.getQuestionContent(selectedVar, variableSkeletons, step);
		System.out.println(background);
		System.out.println(content);

		String response = sendRequest(background, content);
		System.out.println(response);
		
		VariableExpansionUtils.processResponse(selectedVar, response);
	}

	public void recoverLinkageSteps() throws IOException {
		List<TraceNode> steps = VariableGraph.getPotentialLinkageSteps();
		String background = LinkageEstimationUtils.getBackgroundContent();
		String content = LinkageEstimationUtils.getQuestionContent(steps);
		System.out.println(background);
		System.out.println(content);

		String response = sendRequest(background, content);
		System.out.println(response);

		LinkageEstimationUtils.processResponse(response, steps);
	}

	public void sendRequests() throws IOException {
		String variableID = VariableGraph.getNextNodeIDToVisit();
		while (variableID != null) {
			List<TraceNode> relevantSteps = VariableGraph.getRelevantSteps(variableID);
			if (!relevantSteps.isEmpty()) {
				if (VariableGraph.hasChildren(variableID)) {
					String background = SimulationUtilsWithCandidateVar.getBackgroundContent();
					String content = SimulationUtilsWithCandidateVar.getQuestionContent(variableID, relevantSteps);
					System.out.println(background);
					System.out.println(content);

					String response = this.sendRequest(background, content);
					System.out.println(response);

					SimulationUtilsWithCandidateVar.processResponse(response, variableID, relevantSteps);
				} else {
					String background = SimulationUtils.getBackgroundContent();
					String content = SimulationUtils.getQuestionContent(variableID, relevantSteps);
					System.out.println(background);
					System.out.println(content);

					String response = this.sendRequest(background, content);
					System.out.println(response);
					SimulationUtils.processResponse(response, variableID, relevantSteps);
				}
			}

			VariableGraph.addCurrentToParentVariables();
			variableID = VariableGraph.getNextNodeIDToVisit();
		}
	}

	private String sendRequest(String backgroundContent, String questionContent) throws IOException {
		/* set up connection */
		URL url = new URL(SimulatorConstants.API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setDoOutput(true);

		/* construct request */
		JSONObject background = new JSONObject();
		background.put("role", "system");
		background.put("content", backgroundContent);

		JSONObject question = new JSONObject();
		question.put("role", "user");
		question.put("content", questionContent);

		JSONObject request = new JSONObject();
		request.put("model", SimulatorConstants.GPT4O);
		request.put("messages", new org.json.JSONArray().put(background).put(question));
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
}
