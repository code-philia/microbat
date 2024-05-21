package microbat.tracerecov;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

public class ExecutionSimulator {
	
	private static final String API_URL = "https://api.openai.com/v1/chat/completions";
	private static final String GPT3 = "gpt-3.5-turbo";
	private static final String GPT4 = "gpt-4-turbo";
	private static final String GPT4O = "gpt-4o";
	
	private static String apiKey = "";
	private static String promptPrefix;
	
	public ExecutionSimulator() {}
	
	public String sendQuery(String prompt) throws IOException {
		/* connect */
		URL url = new URL(API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setDoOutput(true);
		
		JSONObject message = new JSONObject();
		message.put("role", "user");
		message.put("content", prompt);
		
		JSONObject request = new JSONObject();
		request.put("model", GPT3);
		request.put("messages", new org.json.JSONArray().put(message));
		request.put("temperature", 0.2);
		
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
				return responseObject.getJSONArray("choices")
						.getJSONObject(0)
						.getJSONObject("message")
						.getString("content").trim();
			}
		} else {
			throw new RuntimeException("Failed : HTTP error code : " + responseCode);
		}
	}
}
