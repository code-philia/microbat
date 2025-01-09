package microbat.util;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringFormatUtils {
    private static Logger log = LoggerFactory.getLogger(StringFormatUtils.class);

    private StringFormatUtils() {
    }

    public static String formatString(String template, Map<String, String> values) {
        StringFormatBuilder builder = new StringFormatBuilder(values);
        template.codePoints().forEach(builder);
        return builder.finish();
    }

    /**
     * A builder to build a formatted string.
     * 
     * This builder is used to build a formatted string with placeholders.
     * The placeholders are in the form of `{key}` where `key` is the key in the
     * `values` map. Double braces `{{` and `}}` are used to escape the braces.
     * 
     * Examples:
     * 
     * <pre>
     * String formatted = null;
     * 
     * formatted = StringFormatUtils.formatString("Hello, {name}!", Map.of("name", "world"));
     * assertEquals("Hello, world!", formatted);
     * 
     * formatted = StringFormatUtils.formatString("Hello, {{name}}!", Map.of("name", "world"));
     * assertEquals("Hello, {name}!", formatted);
     * 
     * formatted = StringFormatUtils.formatString("Hello, {{{name}!", Map.of("name", "world"));
     * assertEquals("Hello, {world!", formatted);
     * </pre>
     */
    private static class StringFormatBuilder implements IntConsumer {
        private StringBuilder builder;
        private StringBuilder keyBuilder;
        private int location;

        // status
        // 0 normal
        // 1 one brace
        // 2 one brace reading
        // 3 one right brace

        private int status;
        private Map<String, String> values;

        public StringFormatBuilder(Map<String, String> values) {
            this.values = values;

            this.builder = new StringBuilder();
            this.keyBuilder = null;
            this.status = 0;
            this.location = 0;
        }

        public String finish() {
            if (status != 0) {
                throw new IllegalArgumentException("Unmatched brace in the end of the string");
            }
            return builder.toString();
        }

        @Override
        public void accept(int value) {
            char[] chars = Character.toChars(value);
            int codePointLength = chars.length;

            if (status == 0) {
                if (value == '{') {
                    status = 1;
                } else if (value == '}') {
                    status = 3;
                } else {
                    builder.append(chars);
                }
            } else if (status == 1) {
                if (value == '{') {
                    status = 0;
                    builder.append('{');
                } else if (value == '}') {
                    throw new IllegalArgumentException("Empty key in brace at char " + location);
                } else {
                    status = 2;
                    keyBuilder = new StringBuilder();
                    keyBuilder.append(chars);
                }
            } else if (status == 2) {
                if (value == '{') {
                    throw new IllegalArgumentException("Nested '{' in key at char " + location);
                } else if (value == '}') {
                    status = 0;
                    String key = keyBuilder.toString();
                    keyBuilder = null;
                    if (!values.containsKey(key)) {
                        throw new IllegalArgumentException("Key not found: " + key + " at char " + location);
                    }
                    String replacement = values.get(key);
                    if (replacement == null) {
                        throw new IllegalArgumentException("Null value for key: " + key + " at char " + location);
                    }
                    builder.append(replacement);
                } else {
                    keyBuilder.append(chars);
                }
            } else if (status == 3) {
                if (value == '}') {
                    status = 0;
                    builder.append('}');
                } else {
                    throw new IllegalArgumentException("Unmatched right brace at char " + (location - 1));
                }
            } else {
                throw new IllegalStateException("Invalid status: " + status);
            }

            this.location += codePointLength;
        }

    }

    private static Map<String, String> promptCacheMap = new HashMap<>();

    public static synchronized String loadPrompt(String name) {
        if (promptCacheMap.containsKey(name)) {
            return promptCacheMap.get(name);
        }

        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];

        String resourceName = StringFormatUtils.formatString("/prompts/{name}.md", Map.of("name", name));

        try (InputStream is = StringFormatUtils.class.getResourceAsStream(resourceName)) {
            int read;
            while ((read = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, read));
            }
        } catch (Exception e) {
            log.error("Failed to load prompt: " + name, e);
            throw new RuntimeException("Failed to load prompt: " + name, e);
        }

        String prompt = sb.toString();
        promptCacheMap.put(name, prompt);

        return prompt;
    }

    public static final String PROMPT_NAME_IN_CONTEXT_LEARNING_SYSTEM = "in_context_learning_system";
    public static final String PROMPT_NAME_IN_CONTEXT_LEARNING_USER = "in_context_learning_user";

    public static String getPromptInContextLearningSystem() {
        return loadPrompt(PROMPT_NAME_IN_CONTEXT_LEARNING_SYSTEM);
    }

    /** This prompt should be formatted with {original_code} */
    public static String getPromptInContextLearningUser() {
        return loadPrompt(PROMPT_NAME_IN_CONTEXT_LEARNING_USER);
    }

    public static String decodeWithIgnore(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        StringBuilder sb = new StringBuilder();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        CharBuffer charBuffer = CharBuffer.allocate(bytes.length);

        decoder.decode(byteBuffer, charBuffer, true);
        charBuffer.flip();
        sb.append(charBuffer);

        decoder.flush(charBuffer);
        charBuffer.flip();
        sb.append(charBuffer);

        return sb.toString();
    }
}
