package microbat.decisionpredictionLLM;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

public class testGpt {
    public static void main(String[] args) throws IOException {
    	Agent agent = new Agent();

//        Path filePath = Path.of("D:\\microbat\\microbat\\src\\main\\microbat\\decisionpredictionLLM\\prompt_demo\\decide_prompt.txt");
//        String prompt = Files.readString(filePath, StandardCharsets.UTF_8);
    	String prompt = """
    	Given the following java function and test, your task is to determine if the function contains a bug. If so, add comment like "// here is a bug" after the previous buggy line. If it doesn't contain bug, return "true".
    	Don't return too much explanation.
    	
    	```java
	    public static int bitcount(int n) {
		    int count = 0;
		    while (n != 0) {
		        n = (n ^ (n - 1));
		        count++;
		    }
		    return count;
	    }
	    public void test_0() throws java.lang.Exception {
	        int result = BITCOUNT.bitcount((int)127);
	        org.junit.Assert.assertEquals( (int) 7, result);
	    }
    	```	
    	""";
    	
		// 2.predict using gpt api
    	System.out.println("USER:");
    	System.out.println(prompt);
		String response = agent.chat(prompt);
		System.out.println("GPT:");
		System.out.println(response);
    }
}
