package implementation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.printscript.linter.IdentifierStyle;
import org.printscript.linter.LintConfig;

final class LintConfigLoader {
    private static final Pattern STRING_PAIR = Pattern.compile("\"(.*?)\"\\s*:\\s*\"(.*?)\"");

    private LintConfigLoader() {
    }

    static LintConfig load(InputStream config) {
        if (config == null) {
            return new LintConfig();
        }
        try {
            String content = ScriptSupport.readAll(config);
            if (content.isBlank()) {
                return new LintConfig();
            }
            Map<String, String> values = extractStringPairs(content);
            String styleValue = values.get("identifierStyle");
            if (styleValue == null || styleValue.isBlank()) {
                return new LintConfig();
            }
            IdentifierStyle style = parseIdentifierStyle(styleValue);
            return new LintConfig(style);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load lint config", ex);
        } catch (IllegalArgumentException ex) {
            return new LintConfig();
        }
    }

    private static IdentifierStyle parseIdentifierStyle(String value) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Empty identifier style");
        }
        String enumName = normalized
                .replace("-", "_")
                .replace(" ", "_")
                .replace(".", "_")
                .toUpperCase(Locale.ROOT);
        if ("CAMELCASE".equals(enumName)) {
            enumName = "CAMEL_CASE";
        }
        if ("SNAKECASE".equals(enumName)) {
            enumName = "SNAKE_CASE";
        }
        return IdentifierStyle.valueOf(enumName);
    }

    private static Map<String, String> extractStringPairs(String json) {
        Map<String, String> values = new HashMap<>();
        Matcher matcher = STRING_PAIR.matcher(json);
        while (matcher.find()) {
            values.put(matcher.group(1), matcher.group(2));
        }
        return values;
    }
}