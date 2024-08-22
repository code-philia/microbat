package microbat.tracerecov;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import microbat.tracerecov.executionsimulator.SimulatorConstants;

public class ExampleConstructorTest {
	private static final String BACKGROUND = """
			Given the name, type and runtime value of a variable, your task is to generate a piece of java code which
			creates a variable with the specified name and type, and let it contain the same runtime value.\n
			""";
	
	private static final String RULES = """
			Strictly follow the following rules:
			1.The launch class generated should be named 'Example' containing a 'main' method;
			2.Only return the generated java code enclosed with ```, including the 'import' statement.\n
			""";

	private static final String EXAMPLE = """
			For example, given a variable named 'list' of type 'java.util.ArrayList' with runtime value '[1,2,3]',
			you may return:
			```
			import java.util.ArrayList;
			public class Example {
			    public static void main(String[] args) {
			        ArrayList<Integer> list = new ArrayList<>();
			        list.add(1);
			        list.add(2);
			        list.add(3);
			        System.out.println(list);
			    }
			}
			```\n
			""";
	
	public static String sendRequest(String requestContent) throws IOException {
		/* set up connection */
		URL url = new URL(SimulatorConstants.API_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + SimulatorConstants.API_KEY);
		connection.setDoOutput(true);
		
		/* construct request */
		JSONObject question = new JSONObject();
		question.put("role", "user");
		question.put("content", requestContent);

		JSONArray messages = new JSONArray();
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
	
	
	/*
	 * generate java program using LLM
	 */
	public static String generateJavaCode(String varName, String varType, String varValue) {
		// interact with LLM 
		String content = "Variable name: "+ varName+", variable type: "+varType+", variable value: "+varValue;
		
		String response = null;
		try {
			response = sendRequest(BACKGROUND + EXAMPLE + RULES + content);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// extract java code
        Pattern pattern = Pattern.compile("```\n(.*?)\n```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
        	System.out.println(content);
        	System.out.println("[code generated]");
        	System.out.println(matcher.group(1));
        	return matcher.group(1);
        } else {
            System.out.println("Response Error!");
            return null;
        }
	}
}
