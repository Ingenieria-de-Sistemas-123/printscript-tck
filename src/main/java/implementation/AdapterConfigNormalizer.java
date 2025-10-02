// src/main/java/implementation/AdapterConfigNormalizer.java
package implementation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.printscript.formatter.config.FormatterConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class AdapterConfigNormalizer {

    private AdapterConfigNormalizer() {}

    public static FormatterConfig loadNormalized(InputStream config) {
        if (config == null) {
            return new FormatterConfig(
                    false, // spaceBeforeColon
                    true,  // spaceAfterColon
                    true,  // spaceAroundEquals
                    true,  // spaceAroundOperators
                    0,     // lineJumpBeforePrintln
                    true,  // lineJumpAfterSemicolon
                    4      // indentSize
            );
        }

        InputStreamReader reader = new InputStreamReader(config, StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        // Base con tus campos nativos
        FormatterConfig base = new Gson().fromJson(json, FormatterConfig.class);

        // === Flags "enforce" del TCK → tus booleans nativos ===
        boolean enforceNoEq =
                json.has("enforce-no-spacing-around-equals")
                        && !json.get("enforce-no-spacing-around-equals").isJsonNull()
                        && json.get("enforce-no-spacing-around-equals").getAsBoolean();

        boolean enforceAfterColon =
                json.has("enforce-spacing-after-colon-in-declaration")
                        && !json.get("enforce-spacing-after-colon-in-declaration").isJsonNull()
                        && json.get("enforce-spacing-after-colon-in-declaration").getAsBoolean();

        boolean enforceBeforeColon =
                json.has("enforce-spacing-before-colon-in-declaration")
                        && !json.get("enforce-spacing-before-colon-in-declaration").isJsonNull()
                        && json.get("enforce-spacing-before-colon-in-declaration").getAsBoolean();

        // Aplicamos overrides SOLO si el flag "enforce" viene en true
        boolean spaceAroundEquals = enforceNoEq ? false : base.getSpaceAroundEquals();
        boolean spaceAfterColon   = enforceAfterColon ? true : base.getSpaceAfterColon();
        boolean spaceBeforeColon  = enforceBeforeColon ? true : base.getSpaceBeforeColon();

        return new FormatterConfig(
                spaceBeforeColon,
                spaceAfterColon,
                spaceAroundEquals,
                base.getSpaceAroundOperators(),
                base.getLineJumpBeforePrintln(),
                base.getLineJumpAfterSemicolon(),
                base.getIndentSize()
        );
    }
}
