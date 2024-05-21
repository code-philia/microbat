package microbat.tracerecov.executionsimulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;

/**
 * This class is used to simulate execution through LLM and retrieve
 * approximated values for candidate variables.
 * 
 * @author hongshuwang
 */
public class ExecutionSimulator {

	private static String apiKey = "";
	
	private Map<String, List<TraceNode>> relevantSteps;
	private TraceNode currentStep;
	private VarValue var;

	public ExecutionSimulator(Map<String, List<TraceNode>> relevantSteps, TraceNode currentStep, VarValue var) {
		this.relevantSteps = relevantSteps;
		this.currentStep = currentStep;
		this.var = var;
	}
	
	public List<String> sendRequests() throws IOException {
		List<String> responses = new ArrayList<>();
		for (String key : relevantSteps.keySet()) {
			String response = sendRequest(key, relevantSteps.get(key));
			responses.add(response);
		}
		return responses;
	}

	private String sendRequest(String aliasID, List<TraceNode> steps) throws IOException {
		/* set up connection */
		URL url = new URL(SimulationUtils.API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setDoOutput(true);

		/* construct request */
		JSONObject background = new JSONObject();
		background.put("role", "system");
		background.put("content", SimulationUtils.getBackgroundContent());

		JSONObject question = new JSONObject();
		question.put("role", "user");
		question.put("content", SimulationUtils.getQuestionContent(currentStep, var, steps, aliasID));

		JSONObject request = new JSONObject();
		request.put("model", SimulationUtils.GPT3);
		request.put("messages", new org.json.JSONArray().put(background).put(question));
		request.put("temperature", SimulationUtils.TEMPERATURE);

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
