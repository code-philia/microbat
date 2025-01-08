package microbat.util;

import java.util.Map;
import java.util.function.IntConsumer;

public class StringFormatUtils {
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
}
