package microbat.tracerecov.executionsimulator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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

}
