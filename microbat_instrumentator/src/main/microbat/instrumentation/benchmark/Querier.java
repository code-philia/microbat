package microbat.instrumentation.benchmark;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Querier {
    private static String url = "https://api.openai.com/v1/chat/completions";
    private static String apiKey;
    private static String model = "gpt-3.5-turbo";
    private static String base_directory = "/Applications/Eclipse.app/Contents/Eclipse/dropins/junit_lib/";
    private static String propertiesFileName = base_directory + "properties.txt";
    private static Map<String, String> dictionary;
    private static String dicFilename = base_directory + "dictionary.txt";
    private static String proFilename = base_directory + "prompt.txt";
    private static String prompt;


    public Querier() {
        dictionary = new HashMap<>();
        loadDictionary();
        prompt = addEscape(getPrompt());
        apiKey = getApiKey();
    }

    public static void addWord(String key, String value) {
        dictionary.put(key, value);
        saveDictionary(); 
        System.out.println("'" + key + "' added to dictionary.");
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

    public static void saveDictionary() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(dicFilename))) {
            for (Map.Entry<String, String> entry : dictionary.entrySet()) {
                writer.println(entry.getKey() + "|" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Error saving dictionary to file: " + e.getMessage());
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
        return input.replaceAll("(\")", "\\\\\"").replaceAll("(\n)", "\\\\n");
    }

    public static String getResult(String input) throws Exception {

        Pattern pattern = Pattern.compile("<([^<>]*)>[^<>]*<([^<>]*)>[^<>]*<([^<>]*)>$");
        Matcher matcher = pattern.matcher(input);
        
        if (matcher.find()) {
            String firstMatch = matcher.group(1);
            String secondMatch = matcher.group(2);
            String thirdMatch = matcher.group(3);
            return "<"+ firstMatch + "><" + secondMatch + "><" +thirdMatch + ">";
        } else {
            throw new Exception("No correct result found.");
        }
    }

    public static String chatGPT(String query) {

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            // The request body
            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"system\", \"content\": \"" + prompt + query + "\"}]}";
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
}