// java
package implementation;

import org.printscript.formatter.config.ConfigJsonReader;
import org.printscript.formatter.config.FormatterConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FormatterConfigLoader {
    private FormatterConfigLoader() {}

    // Defaults q coinciden con los golden de los tests
    private static final Map<String, String> DEFAULTS = Map.of(
            "mandatory-line-break-after-statement", "true",
            "indent-inside-if", "2"
    );

    static FormatterConfig load(InputStream config) {
        try {
            String userJson = (config == null) ? "" : ScriptSupport.readAll(config).trim();
            Map<String, String> userFlat = userJson.isBlank() ? Map.of() : parseSimpleFlatJson(userJson);

            Map<String, String> merged = new LinkedHashMap<>(DEFAULTS);
            merged.putAll(userFlat);

            String mergedJson = toJson(merged);
            return new ConfigJsonReader().read(mergedJson);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load formatter config", ex);
        }
    }

    //parser minimum para JSON plano: "key": value (boolean/int)
    private static final Pattern ENTRY = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(true|false|[0-9]+)");

    private static Map<String, String> parseSimpleFlatJson(String content) {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher m = ENTRY.matcher(content);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(2);
            map.put(key, val);
        }
        return map;
    }

    private static String toJson(Map<String, String> flat) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : flat.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\": ").append(e.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}
