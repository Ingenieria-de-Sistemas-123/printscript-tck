package implementation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.printscript.formatter.config.ConfigJsonReader;
import org.printscript.formatter.config.FormatterConfig;

final class FormatterConfigLoader {
    private FormatterConfigLoader() {}

    // Detecta "clave": (sólo las claves de objeto JSON, no valores) y las normaliza a camelCase
    private static final Pattern JSON_KEY_PATTERN =
            Pattern.compile("\"([A-Za-z][A-Za-z0-9_\\-]*)\"\\s*:");

    static FormatterConfig load(InputStream config) {
        if (config == null) {
            return new FormatterConfig();
        }
        try {
            String content = ScriptSupport.readAll(config);
            if (content.isBlank()) {
                return new FormatterConfig();
            }

            // Normalizar claves de configuración del TCK a camelCase esperado por la librería
            String normalized = normalizeJsonKeysToCamelCase(content);

            Path tempFile = Files.createTempFile("printscript-formatter", ".json");
            try {
                Files.writeString(tempFile, normalized, StandardCharsets.UTF_8);
                return new ConfigJsonReader().readFromFile(tempFile.toString());
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load formatter config", ex);
        }
    }

    private static String normalizeJsonKeysToCamelCase(String json) {
        Matcher m = JSON_KEY_PATTERN.matcher(json);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String rawKey = m.group(1); // puede venir en kebab-case o snake_case
            String camel = toCamelCase(rawKey);
            // Reemplaza por la clave en camelCase preservando el ":" y el espacio posterior
            m.appendReplacement(sb, "\"" + camel + "\":");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String toCamelCase(String key) {
        // Convierte kebab/snake a camelCase: e.g. mandatory-single-space-separation -> mandatorySingleSpaceSeparation
        String[] parts = key.split("[-_]");
        if (parts.length == 0) return key;
        StringBuilder out = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) out.append(p.substring(1));
        }
        return out.toString();
    }
}
