package microbat.tracerecov.executionsimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import microbat.codeanalysis.runtime.Condition;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.StepLimitException;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import sav.strategies.dto.AppJavaClassPath;

/**
 *  Given a variable with runtime value (varName, varType, varValue)
 *  1. Ask LLM to generate a program creating a same variable with same value;
 *  2. Compile the program;
 *  3. Using microbat to instrument the generated program, get the expansion form of the variable
 */

public class ExampleConstructor {
	
	public ExampleConstructor() {}

	/*
	 * send request and get response from LLM
	 */
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
		String background = ExampleConstructorUtils.getBackground();
		String example = ExampleConstructorUtils.getExample();
		String rules = ExampleConstructorUtils.getRules();
		String content = "Variable name: "+ varName+", variable type: "+varType+", variable value: "+varValue;
		
		String response = null;
		try {
			response = sendRequest(background + example + rules + content);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// extract java code
        Pattern pattern = Pattern.compile("```\n(.*?)\n```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
        	System.out.println(matcher.group(1));
        	return matcher.group(1);
        } else {
            System.out.println("Response Error!");
            return null;
        }
	}
	
	
	/*
	 * main method of example constructor
	 */
	public static String constructExample(String varName, String varType, String varValue) {
		// step 1: generate java code
//		String javaCode = generateJavaCode(varName,varType,varValue);

		// step 2: save to .java file and compile
//		String dirName = ExampleConstructorUtils.createDir();
		String dirName = "1";
		String srcFilePath = ExampleConstructorUtils.CODE_GEN_BASE_FOLDER+File.separator+
				dirName+File.separator+
				ExampleConstructorUtils.CODE_SRC_FOLDER+File.separator+
				ExampleConstructorUtils.CODE_GEN_FILENAME; // path/to/src/code.java
		
		String targetFilePath = ExampleConstructorUtils.CODE_GEN_BASE_FOLDER+File.separator+
				dirName+File.separator+
				ExampleConstructorUtils.CODE_BUILD_FOLDER; // path/to/build
		
//		ExampleConstructorUtils.saveToFile(srcFilePath,javaCode);
//		ExampleConstructorUtils.compileJavaFile(srcFilePath,targetFilePath);
		
		// step 3: generate trace through conditional instrumentation
		Condition condition = new Condition(varName,varType,varValue,"null");
		AppJavaClassPath appClassPath = ExampleConstructorUtils.createAppClassPath(srcFilePath, targetFilePath, dirName);
		InstrumentationExecutor exectuor = new InstrumentationExecutor(appClassPath,
				ExampleConstructorUtils.EXAMPLE_BASE_FOLDER+File.separator+"trace", dirName, new ArrayList<String>(), new ArrayList<String>(), condition);
		
		RunningInfo info = null;
		try {
			info = exectuor.run();
		} catch (StepLimitException e) {
			e.printStackTrace();
		}
		Trace trace = info.getMainTrace();
		System.out.println(trace.size());
		System.out.println("Successfully generated trace!");
		for (TraceNode step : trace.getExecutionList()) {
			System.out.println("At step "+step.getOrder()+", code: "+step.getLineNumber());
			for (VarValue readVariable : step.getReadVariables()) {
				System.out.println(readVariable.getVarName()+":"+readVariable.getType()+"  "+readVariable.getStringValue());
			}
		}
		
		// step 4: get expansion form		
		ExecutionSimulationFileLogger gtLogger = new ExecutionSimulationFileLogger();
		String groundTruthStr = gtLogger.collectGT(condition, trace);
		
		System.out.println(groundTruthStr);
	
		return groundTruthStr;
	}
	
	public static void main(String[] args) {
		System.out.println("done");
	}
	
}