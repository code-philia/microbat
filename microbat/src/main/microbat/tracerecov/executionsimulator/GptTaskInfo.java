package microbat.tracerecov.executionsimulator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringSubstitutor;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public enum GptTaskInfo {
    DATA_STRUCTURE_ABSTRACTION("data_structure_abstraction"),
    IDENTIFY_CRITICAL_VARIABLE("identify_critical_variable"),
    VARIABLE_EXPANSION("variable_expansion");

    private final String promptFolder;
    private final Map<String, String> loadedPrompts;
    private final Map<String, String> loadedJson;

    private GptTaskInfo(String promptFolder) {
        this.promptFolder = promptFolder;
        this.loadedPrompts = new ConcurrentHashMap<>();
        this.loadedJson = new ConcurrentHashMap<>();
    }

    private String loadFile(String fileName) {
        String promptPath = "prompts/" + promptFolder + "/" + fileName;
        try (InputStream is = GptTaskInfo.class.getClassLoader().getResourceAsStream(promptPath)) {
            if (is == null) {
                RuntimeException e = new RuntimeException("Failed to load prompt file.");
                log.error("Failed to load prompt file. Path: {}", promptPath, e);
                throw e;
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return content;
        } catch (Exception e) {
            log.error("Failed to load from prompts. File: {}", fileName, e);
            throw new RuntimeException("Failed to load prompt.", e);
        }
    }

    public JsonElement loadJson(String jsonFileName) {
        if (!loadedJson.containsKey(jsonFileName)) {
            loadedJson.put(jsonFileName, loadFile(jsonFileName + ".json"));
        }
        String jsonString = loadedJson.get(jsonFileName);
        JsonElement jsonElement = JsonParser.parseString(jsonString);
        return jsonElement;
    }

    public String loadPrompt(String promptFile) {
        if (!loadedPrompts.containsKey(promptFile)) {
            loadedPrompts.put(promptFile, loadFile(promptFile + ".md"));
        }
        return loadedPrompts.get(promptFile);
    }

    public String loadPromptUser() {
        return loadPrompt("user");
    }

    public static String formatPromptString(String format, Map<String, String> values) {
        StringSubstitutor sub = new StringSubstitutor(values);
        return sub.replace(format);
    }

    private static final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();
    private static final Map<String, Pattern> compiledLabels = new ConcurrentHashMap<>();

    public static String findPatternIn(String pattern, String text) {
        if (!compiledPatterns.containsKey(pattern)) {
            String regex = String.format("```%s\\s*(.*?)\\s*```", pattern);
            Pattern p = Pattern.compile(regex, Pattern.DOTALL);
            compiledPatterns.put(pattern, p);
        }

        Pattern p = compiledPatterns.get(pattern);
        Matcher m = p.matcher(text);

        List<String> block = new ArrayList<>();
        while (m.find()) {
            String blockText = m.group(1);
            block.add(blockText);
        }

        if (block.isEmpty()) {
            RuntimeException e = new RuntimeException("No code block found.");
            log.error("No code block found in text. Pattern: {}, Text: {}", pattern, text, e);
            throw e;
        }
        if (block.size() > 1) {
            RuntimeException e = new RuntimeException("Multiple code blocks found.");
            log.error("Multiple code blocks found in text. Pattern: {}, Text: {}", pattern, text, e);
            throw e;
        }

        return block.get(0);
    }

    public static String findLabelIn(String label, String text) {
        if (!compiledLabels.containsKey(label)) {
            String regex = String.format("<%s>(.*?)</%s>", label, label);
            Pattern p = Pattern.compile(regex);
            compiledLabels.put(label, p);
        }

        Pattern p = compiledLabels.get(label);
        Matcher m = p.matcher(text);

        List<String> block = new ArrayList<>();
        while (m.find()) {
            String blockText = m.group(1);
            block.add(blockText);
        }

        if (block.isEmpty()) {
            RuntimeException e = new RuntimeException("No code block found.");
            log.error("No label found in text. Label: {}, Text: {}", label, text, e);
            throw e;
        }
        if (block.size() > 1) {
            RuntimeException e = new RuntimeException("Multiple code blocks found.");
            log.error("Multiple labels found in text. Label: {}, Text: {}", label, text, e);
            throw e;
        }

        return block.get(0);
    }

}
