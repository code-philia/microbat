package microbat.instrumentation.benchmark;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Querier {
    private static String url = "https://api.openai.com/v1/chat/completions";
    private static String apiKey;
	private static String model3 = "gpt-3.5-turbo";
    private static String model4 = "gpt-4-turbo-preview";
    private static String base_directory = "/Applications/Eclipse.app/Contents/Eclipse/dropins/junit_lib/";
    private static String propertiesFileName = base_directory + "properties.txt";
    /* v1: (request : response) */
    private static Map<String, String> dictionary;
    /* v2: getter methods */
    private static Map<String, Integer> getterMethods;
    private static int limit = 2;
    private static String dicFilename = base_directory + "dictionary_v2.txt";
    private static String proFilename = base_directory + "prompt_v2.txt";
    private static String prompt;


    public Querier() {
        dictionary = new HashMap<>();
        getterMethods = new HashMap<>();
//        loadDictionary();
        loadGetterMethods();
        prompt = addEscape(getPrompt());
        apiKey = getApiKey();
    }
    
    public static void setUp() {
    	dictionary = new HashMap<>();
        getterMethods = new HashMap<>();
//        loadDictionary();
        loadGetterMethods();
        prompt = addEscape(getPrompt());
        apiKey = getApiKey();
    }

    public static void addWord(String key, String value) {
        dictionary.put(key, value);
        saveDictionary(); 
        System.out.println("'" + key + "' added to dictionary.");
    }
    
    public static void addGetterMethodOccurrence(String methodInfo) {
    	if (!getterMethods.containsKey(methodInfo)) {
    		getterMethods.put(methodInfo, 1);
            saveGetterMethod();
    	} else {
    		getterMethods.put(methodInfo, getterMethods.get(methodInfo) + 1);
    		saveGetterMethod();
    	}
    }
    
    public static void removeGetterMethodOccurrence(String methodInfo) {
    	if (getterMethods.containsKey(methodInfo)) {
    		getterMethods.put(methodInfo, getterMethods.get(methodInfo) - 1);
    		saveGetterMethod();
    	}
    }

    public static String searchWord(String key) {
        String value = dictionary.get(key);
        if (value != null){
            System.out.println( key + ": " + value);
            return value;
        }
        else{
            System.out.println( key + "' not found in dictionary.");
            return null;
        }
    }

    public static void loadDictionary() {
        try (BufferedReader reader = new BufferedReader(new FileReader(dicFilename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                dictionary.put(parts[0], parts[1]);
            }
        } catch (IOException e) {
            System.err.println("Error loading dictionary from file: " + e.getMessage());
        }
    }
    
    public static void loadGetterMethods() {
        try (BufferedReader reader = new BufferedReader(new FileReader(dicFilename))) {
            String line;
            while ((line = reader.readLine()) != null) {
            	String[] parts = line.split("\\|", 2);
                getterMethods.put(parts[0], Integer.valueOf(parts[1]));
            }
        } catch (IOException e) {
            System.err.println("Error loading dictionary from file: " + e.getMessage());
        }
    }

    public static void saveDictionary() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(dicFilename))) {
            for (Map.Entry<String, String> entry : dictionary.entrySet()) {
                writer.println(entry.getKey() + "|" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Error saving dictionary to file: " + e.getMessage());
        }
    }
    
    public static void saveGetterMethod() {
    	try (PrintWriter writer = new PrintWriter(new FileWriter(dicFilename))) {
    		for (Map.Entry<String, Integer> entry : getterMethods.entrySet()) {
                writer.println(entry.getKey() + "|" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Error appending line to file: " + e.getMessage());
        }
    }

//    public static String getApiKey() {
//        Properties properties = new Properties();
//        try {
//            properties.load(new FileInputStream(CONFIG_FILE));
//            return properties.getProperty("API_KEY");
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
    
    public static String getApiKey() {
        try (BufferedReader reader = new BufferedReader(new FileReader(propertiesFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                return line.split("=")[1];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getPrompt() {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(proFilename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String addEscape(String input) {
    	char[] characters = input.toCharArray();
    	for (int i = 0; i < characters.length; i++) {
    		char c = characters[i];
    		if (c < 0 || c > 127) {
    			characters[i] = 65; // replace with A
    		}
    	}
		String request = new String(characters);
		return request.replaceAll("(\")", "\\\\\"").replaceAll("(\n)", "\\\\n");
    }

//    public static String getResult(String input) throws Exception {
//
//        Pattern pattern = Pattern.compile("<([^<>]*)>[^<>]*<([^<>]*)>[^<>]*<([^<>]*)>$");
//        Matcher matcher = pattern.matcher(input);
//        
//        if (matcher.find()) {
//            String firstMatch = matcher.group(1);
//            String secondMatch = matcher.group(2);
//            String thirdMatch = matcher.group(3);
//            return "<"+ firstMatch + "><" + secondMatch + "><" +thirdMatch + ">";
//        } else {
//            throw new Exception("No correct result found.");
//        }
//    }
    
    public static String getResult(String input) {
    	int firstBracketIndex = input.indexOf('<');
    	if (firstBracketIndex > 0) {
    		return input.substring(firstBracketIndex);
    	}
        return input;
    }
    
    public static String getResultV2(String input) {
    	int firstBracketIndex = input.indexOf('{');
    	if (firstBracketIndex > 0) {
    		return input.substring(firstBracketIndex);
    	}
        return input;
    }

    public static String chatGPT(String query) {

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            // The request body
            String body = "{\"model\": \"" + model3 + "\", \"messages\": [{\"role\": \"system\", \"content\": \"" + prompt + query + "\"}]}";
            System.out.println(body);
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            // Response from ChatGPT
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuffer response = new StringBuffer();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            // calls the method to extract the message.
            return extractMessageFromJSONResponse(response.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String extractMessageFromJSONResponse(String response) {
        int start = response.indexOf("content")+ 11;

        int end = response.indexOf("\"", start);

        return response.substring(start, end);

    }

    public static String getDependency(String query) {
        String value = searchWord(query);
        if (value != null) return value;
        else{
            try {
                value = chatGPT(addEscape(query));
                value = getResult(value);
                addWord(query, value);
                return value;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        return value;
    }
    
    public static String getDependencyV2(String methodInfo, String query) {
    	if (getterMethods == null) {
    		setUp();
    	}
    	
    	String value = "";
        if (getterMethods.containsKey(methodInfo) && getterMethods.get(methodInfo) >= limit) {
        	return value;
        } else{
            try {
                value = chatGPT(addEscape(query));
                value = getResult(value);
                return value;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        return value;
    }  
}