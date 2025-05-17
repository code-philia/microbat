package microbat.tracerecov.executionsimulator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import microbat.Activator;
import microbat.preference.RecovSlicingPreference;

public class GPTExecutionSimulator extends ExecutionSimulator {

	private static String isOpenAIEndpointStr = Activator.getDefault().getPreferenceStore()
			.getString(RecovSlicingPreference.IS_OPENAI_ENDPOINT);

	@Override
	protected String getUrl() {
		if (SimulatorConstants.GPT_API_ENDPOINT_OVERRIDE != null) {
			return SimulatorConstants.GPT_API_ENDPOINT_OVERRIDE;
		}
		if (isOpenAIEndpointStr != null && isOpenAIEndpointStr.equals("true")) {
			return SimulatorConstants.GPT_API_ENDPOINT;
		} else {
			return SimulatorConstants.GPT_TB_API_ENDPOINT;
		}
	}

	@Override
	protected HttpURLConnection getConnection() throws IOException {
		URL url = new URL(getUrl());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + getAPIKey());
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
	protected JsonObject getSingleRequest(String combinedPrompt, LLMResponseType responseType) {
		JsonObject question = new JsonObject();
		question.addProperty("role", "user");
		question.addProperty("content", combinedPrompt);

		JsonArray messages = new JsonArray();
		messages.add(question);

		JsonObject responseFormat = new JsonObject();
		responseFormat.addProperty("type", getResponseTypeString(responseType));

		JsonObject request = new JsonObject();
		request.addProperty("model", SimulatorConstants.getSelectedModel());
		request.add("messages", messages);
		request.addProperty("temperature", SimulatorConstants.TEMPERATURE);
		request.addProperty("max_tokens", SimulatorConstants.MAX_TOKENS);
		request.addProperty("top_p", SimulatorConstants.TOP_P);
		request.addProperty("frequency_penalty", SimulatorConstants.FREQUENCY_PENALTY);
		request.addProperty("presence_penalty", SimulatorConstants.PRESENCE_PENALTY);
		request.add("response_format", responseFormat);

		return request;
	}

	@Override
	protected String getSingleResponse(JsonObject responseObject) {
		return responseObject
				.get("choices")
				.getAsJsonArray()
				.get(0)
				.getAsJsonObject()
				.get("message")
				.getAsJsonObject()
				.get("content")
				.getAsString()
				.trim();
	}

	@Override
	protected String getAPIKey() {
		if (isOpenAIEndpointStr != null && isOpenAIEndpointStr.equals("true")) {
			return SimulatorConstants.API_KEY;
		} else {
			return SimulatorConstants.TB_API_KEY;
		}
	}

}
