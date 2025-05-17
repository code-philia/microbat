package microbat.tracerecov.executionsimulator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class GeminiExecutionSimulator extends ExecutionSimulator {

	@Override
	protected String getUrl() {
		StringBuilder urlBuilder = new StringBuilder(SimulatorConstants.GEMINI_API_ENDPOINT);
		urlBuilder.append(SimulatorConstants.getSelectedModel());
		urlBuilder.append(":generateContent?key=" + getAPIKey());
		return urlBuilder.toString();
	}

	@Override
	protected HttpURLConnection getConnection() throws IOException {
		URL url = new URL(getUrl());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);

		return connection;
	}

	@Override
	protected String getResponseTypeString(LLMResponseType responseType) {
		switch (responseType) {
			case JSON:
				return "application/json";
			case TEXT:
				return "text/plain";
			default:
				return "text/plain";
		}
	}

	@Override
	protected JsonObject getSingleRequest(String combinedPrompt, LLMResponseType responseType) {
		/* content */
		JsonObject part = new JsonObject();
		part.addProperty("text", combinedPrompt);

		JsonArray parts = new JsonArray();
		parts.add(part);

		JsonObject content = new JsonObject();
		content.add("parts", parts);

		JsonArray contents = new JsonArray();
		contents.add(content);

		/* generationConfig */
		JsonObject generationConfig = new JsonObject();
		// generationConfig.put("responseMimeType",
		// getResponseTypeString(responseType));
		generationConfig.addProperty("maxOutputTokens", SimulatorConstants.MAX_TOKENS);
		generationConfig.addProperty("temperature", SimulatorConstants.TEMPERATURE);
		// generationConfig.put("topP", SimulatorConstants.GEMINI_TOP_P);
		// generationConfig.put("topK", SimulatorConstants.GEMINI_TOP_K);

		/* request */
		JsonObject request = new JsonObject();
		request.add("contents", contents);
		request.add("generationConfig", generationConfig);

		return request;
	}

	@Override
	protected String getSingleResponse(JsonObject responseObject) {
		return responseObject
				.get("candidates")
				.getAsJsonArray()
				.get(0)
				.getAsJsonObject()
				.get("content")
				.getAsJsonObject()
				.get("parts")
				.getAsJsonArray()
				.get(0)
				.getAsJsonObject()
				.get("text")
				.getAsString()
				.trim();
	}

	@Override
	protected String getAPIKey() {
		return SimulatorConstants.API_KEY;
	}

}
