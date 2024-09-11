package microbat.tracerecov.executionsimulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GPTExecutionSimulator extends ExecutionSimulator {

	@Override
	protected String getUrl() {
		return SimulatorConstants.GPT_API_ENDPOINT;
	}

	@Override
	protected HttpURLConnection getConnection() throws IOException {
		URL url = new URL(getUrl());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + SimulatorConstants.API_KEY);
		connection.setDoOutput(true);

		return connection;
	}

	@Override
	protected String getResponseTypeString(LLMResponseType responseType) {
		switch (responseType) {
		case JSON:
			return "json_object";
		case TEXT:
			return "text";
		default:
			return "text";
		}
	}

	@Override
	protected JSONObject getSingleRequest(String combinedPrompt, LLMResponseType responseType) {
		JSONObject question = new JSONObject();
		question.put("role", "user");
		question.put("content", combinedPrompt);

		JSONArray messages = new JSONArray();
		messages.put(question);

		JSONObject responseFormat = new JSONObject();
		responseFormat.put("type", getResponseTypeString(responseType));

		JSONObject request = new JSONObject();
		request.put("model", SimulatorConstants.getSelectedModel());
		request.put("messages", messages);
		request.put("temperature", SimulatorConstants.TEMPERATURE);
		request.put("max_tokens", SimulatorConstants.MAX_TOKENS);
		request.put("top_p", SimulatorConstants.TOP_P);
		request.put("frequency_penalty", SimulatorConstants.FREQUENCY_PENALTY);
		request.put("presence_penalty", SimulatorConstants.PRESENCE_PENALTY);
		request.put("response_format", responseFormat);

		return request;
	}

	@Override
	protected String getSingleResponse(JSONObject responseObject) {
		return responseObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
				.trim();
	}
	
	// Method to send the prompt in segments
	@Override
	protected String sendInSegments(String backgroundContent, String questionContent, LLMResponseType responseType)
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

}
