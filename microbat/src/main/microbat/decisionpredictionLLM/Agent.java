package microbat.decisionpredictionLLM;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.jobs.Job;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.utils.SourceRoot;
import com.plexpt.chatgpt.util.Proxys;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.handler.DebugPilotHandler;
import microbat.handler.callbacks.HandlerCallbackManager;
import microbat.model.BreakPoint;
import microbat.model.trace.TraceNode;
import microbat.views.DialogUtil;

public class Agent {
//	private String chatUrl = "https://api.openai.com/v1/chat/completions";
	
	private static Map<String, String> CachedSpecifications = new HashMap<>();
	
	private String logFile = "D:\\ProgramDebugging\\history\\temp.log";
	private PromptConstructor promptConstructor = new PromptConstructor();
	private ToolManager toolManager = new ToolManager();
	
	private String chatUrl;
	private String apiKey;
	private String model;
	
	public Agent() {
		setConfig(1);
	}
	
    public static void clearCachedSpecifications() {
    	CachedSpecifications.clear();
    }
    
	public void setLogFile(String project) {
		logFile = "D:\\history\\"+project+".log";
	}
	
	public void setConfig(int choice) {
		chatUrl = "Specific url, for example: \"https://api.openai.com/v1/chat/completions\"";
		apiKey = "Your api key, for example: \"sk-1234567\"";
		model = "Selected model, for example: \"gpt-4\"";
		
	}
	
	public String infer_specification(DPUserFeedback curStep) {
		BreakPoint bp = curStep.getNode().getBreakPoint();
		String methodName = bp.getClassCanonicalName() + "." + bp.getMethodName();
		if(CachedSpecifications.containsKey(methodName)) {
			return CachedSpecifications.get(methodName);
		}
		String prompt = promptConstructor.inferPromptNew(curStep);
		String response = chat(prompt);
		logToFile("infer prompt", prompt);
		logToFile("infer response",response);
		CachedSpecifications.put(methodName, response);
		return response;
	}
	
	public String make_decision(String specification, DPUserFeedback curStep) {
//		Pattern toolPattern = Pattern.compile("(?s)<Request>(.*?)</Request>");
//		boolean canUseTool = (i<1);
		String prompt = promptConstructor.decisionPrompt(curStep, specification, false);
		String response = chat(prompt);
		logToFile("decision prompt",prompt);
		logToFile("decision response",response);
		return response;
		
//		Matcher toolMatcher = toolPattern.matcher(response);
//		if(toolMatcher.find()) {
//			System.out.println(toolMatcher.group(1));
//		}
//		else {
//			return response;
//		}
//		return "";
	}
	
    public String chat(String prompt){
        Map<String,String> headers = new HashMap<String,String>();
        headers.put("Content-Type","application/json");

        JSONObject json = new JSONObject();
        json.set("model",this.model);
        
        // input
        JSONObject msg = new JSONObject();
        msg.set("role", "user");
        msg.set("content", prompt);
        JSONArray array = new JSONArray();
        array.add(msg);
        json.set("messages", array);
        
        // configure
        json.set("temperature",1);
        json.set("max_tokens",4096);
        json.set("top_p",1);
        json.set("frequency_penalty",0.0);
        json.set("presence_penalty",0.0);
        
        int retry = 3;
        while(retry > 0) {
            try{
                Proxy proxy = Proxys.http("127.0.0.1", 7890);
                HttpResponse response = HttpRequest.post(this.chatUrl)
                        .headerMap(headers, false)
                        .bearerAuth(this.apiKey)
                        .setProxy(proxy)
                        .body(String.valueOf(json))
                        .timeout(600000)
                        .execute();
                if(response.getStatus()!=200) {
                	System.out.println(response);
                    retry -= 1;
                    continue;
                }
                JSONObject respBody = JSONUtil.parseObj(response.body());
                JSONArray choices = respBody.getJSONArray("choices");
                JSONObject result = choices.get(0,JSONObject.class,Boolean.TRUE);
                String message = result.getJSONObject("message").getStr("content");
                return message;
                
            }catch (Exception e){
                retry -= 1;
                continue;
            }
        }
        // shut down debugpilot
		DialogUtil.popErrorDialog("An error occured when getting reponse from LLM, please confirm again.", "Response Error");
		HandlerCallbackManager.getInstance().runDebugPilotTerminateCallbacks();
		Job.getJobManager().cancel(DebugPilotHandler.JOB_FAMALY_NAME);
        return "";
    }
    
    public void logToFile(String head, String content) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true))) {
        	bw.write("========== "+ head +" ==========\n");
            bw.write(content);
            bw.newLine();
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
