package implementation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.printscript.formatter.config.ConfigJsonReader;
import org.printscript.formatter.config.FormatterConfig;

final class FormatterConfigLoader {
    private FormatterConfigLoader() {
    }

    static FormatterConfig load(InputStream config) {
        if (config == null) {
            return new FormatterConfig();
        }
        try {
            String content = ScriptSupport.readAll(config);
            if (content.isBlank()) {
                return new FormatterConfig();
            }
            Path tempFile = Files.createTempFile("printscript-formatter", ".json");
            try {
                Files.writeString(tempFile, content, StandardCharsets.UTF_8);
                ConfigJsonReader reader = new ConfigJsonReader();
                return reader.readFromFile(tempFile.toString());
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load formatter config", ex);
        }
    }
}
