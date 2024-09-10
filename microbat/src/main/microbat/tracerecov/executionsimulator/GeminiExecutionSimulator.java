package microbat.tracerecov.executionsimulator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiExecutionSimulator extends ExecutionSimulator {

	@Override
	protected String getUrl() {
		StringBuilder urlBuilder = new StringBuilder(SimulatorConstants.GEMINI_API_ENDPOINT);
		urlBuilder.append(SimulatorConstants.getSelectedModel());
		urlBuilder.append(":generateContent?key=" + SimulatorConstants.API_KEY);
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
	protected JSONObject getSingleRequest(String combinedPrompt, LLMResponseType responseType) {
		/* content */
		JSONObject part = new JSONObject();
		part.put("text", combinedPrompt);

		JSONArray parts = new JSONArray();
		parts.put(part);

		JSONObject content = new JSONObject();
		content.put("parts", parts);

		JSONArray contents = new JSONArray();
		contents.put(content);

		/* generationConfig */
		JSONObject generationConfig = new JSONObject();
//		generationConfig.put("responseMimeType", getResponseTypeString(responseType));
		generationConfig.put("maxOutputTokens", SimulatorConstants.MAX_TOKENS);
		generationConfig.put("temperature", SimulatorConstants.TEMPERATURE);
		generationConfig.put("topP", SimulatorConstants.GEMINI_TOP_P);
		generationConfig.put("topK", SimulatorConstants.GEMINI_TOP_K);

		/* request */
		JSONObject request = new JSONObject();
		request.put("contents", contents);
		request.put("generationConfig", generationConfig);

		return request;
	}

	@Override
	protected String getSingleResponse(JSONObject responseObject) {
		return responseObject.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
				.getJSONObject(0).getString("text").trim();
	}

}
