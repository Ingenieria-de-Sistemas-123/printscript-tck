package implementation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.printscript.linter.IdentifierStyle;
import org.printscript.linter.LintConfig;

final class LintConfigLoader {
    private static final Pattern ENTRY = Pattern.compile(
            "\\\"(?<key>[^\\\"]+)\\\"\\s*:\\s*(?:(?<string>\\\"(?:\\\\.|[^\\\\\\\"])*\\\")|(?<bool>true|false))",
            Pattern.CASE_INSENSITIVE
    );

    private LintConfigLoader() {
    }

    static LintConfig load(InputStream config) {
        if (config == null) {
            return new LintConfig();
        }
        try {
            String raw = ScriptSupport.readAll(config);
            if (raw.isBlank()) {
                return new LintConfig();
            }
            ConfigValues values = readValues(raw);
            LintConfig lintConfig = values.identifierStyle != null
                    ? new LintConfig(values.identifierStyle)
                    : new LintConfig();
            applyBoolean(lintConfig, "setMandatoryVariableOrLiteralInPrintln", values.mandatoryPrintln);
            applyBoolean(lintConfig, "setMandatoryVariableOrLiteralInReadInput", values.mandatoryReadInput);
            return lintConfig;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load lint config", ex);
        }
    }

    private static ConfigValues readValues(String json) {
        ConfigValues values = new ConfigValues();
        Matcher matcher = ENTRY.matcher(json);
        while (matcher.find()) {
            String rawKey = matcher.group("key");
            String normalized = normalizeKey(rawKey);
            String stringValue = matcher.group("string");
            if (stringValue != null) {
                stringValue = unquote(stringValue);
            }
            String boolValue = matcher.group("bool");

            switch (normalized) {
                case "identifier-style":
                case "identifier-format":
                case "identifierstyle":
                    if (stringValue != null) {
                        IdentifierStyle parsed = parseIdentifierStyle(stringValue);
                        if (parsed != null) {
                            values.identifierStyle = parsed;
                        }
                    }
                    break;
                case "mandatory-variable-or-literal-in-println":
                    if (boolValue != null) {
                        values.mandatoryPrintln = Boolean.parseBoolean(boolValue);
                    }
                    break;
                case "mandatory-variable-or-literal-in-read-input":
                case "mandatory-variable-or-literal-in-readinput":
                    if (boolValue != null) {
                        values.mandatoryReadInput = Boolean.parseBoolean(boolValue);
                    }
                    break;
                default:
                    break;
            }
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String normalizeKey(String key) {
        String trimmed = key.trim();
        String withHyphen = trimmed
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .replace('_', '-')
                .replace(' ', '-')
                .replace('.', '-')
                .toLowerCase(Locale.ROOT);
        return withHyphen.replaceAll("-+", "-");
    }

    private static IdentifierStyle parseIdentifierStyle(String value) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        String enumName = normalized
                .replace('-', '_')
                .replace(' ', '_')
                .replace('.', '_')
                .toUpperCase(Locale.ROOT);
        if ("CAMELCASE".equals(enumName)) {
            enumName = "CAMEL_CASE";
        }
        if ("SNAKECASE".equals(enumName)) {
            enumName = "SNAKE_CASE";
        }
        try {
            return IdentifierStyle.valueOf(enumName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void applyBoolean(LintConfig target, String methodName, Boolean value) {
        if (value == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, boolean.class);
            method.invoke(target, value);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method method = target.getClass().getMethod(methodName, Boolean.class);
                method.invoke(target, value);
            } catch (ReflectiveOperationException ignoredAgain) {
                // Older configs without the flag simply ignore the value.
            }
        }
    }

    private static final class ConfigValues {
        private IdentifierStyle identifierStyle;
        private Boolean mandatoryPrintln;
        private Boolean mandatoryReadInput;
    }
}