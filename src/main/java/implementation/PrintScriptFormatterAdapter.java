// java
package implementation;

import interpreter.PrintScriptFormatter;
import kotlin.sequences.SequencesKt;
import org.printscript.formatter.CodeFormatter;
import org.printscript.formatter.config.ConfigJsonReader;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.pattern.PreConfiguredTokens;
import org.printscript.lexer.pattern.TokenProvider;
import org.printscript.parser.DefaultParser;
import org.printscript.parser.Parser;
import org.printscript.parser.node.ASTNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class PrintScriptFormatterAdapter implements PrintScriptFormatter {
    private final CodeFormatter formatter;
    private final ParserFactory parserFactory;
    private final ConfigJsonReader configReader;
    private final TokenProviderSelector tokenProviderSelector;

    public PrintScriptFormatterAdapter() {
        this(new CodeFormatter(), DefaultParser::new, new ConfigJsonReader(), PrintScriptFormatterAdapter::defaultTokenProvider);
    }

    public PrintScriptFormatterAdapter(
            CodeFormatter formatter,
            ParserFactory parserFactory,
            ConfigJsonReader configReader,
            TokenProviderSelector tokenProviderSelector
    ) {
        this.formatter = Objects.requireNonNull(formatter);
        this.parserFactory = Objects.requireNonNull(parserFactory);
        this.configReader = Objects.requireNonNull(configReader);
        this.tokenProviderSelector = Objects.requireNonNull(tokenProviderSelector);
    }

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        Objects.requireNonNull(src, "src InputStream no puede ser null");
        Objects.requireNonNull(version, "version no puede ser null");
        Objects.requireNonNull(config, "config InputStream no puede ser null");
        Objects.requireNonNull(writer, "writer no puede ser null");
        try {
            // Leer todo el source para poder pasarlo al formatter como originalSource
            String source = new String(src.readAllBytes(), StandardCharsets.UTF_8);

            TokenProvider tokenProvider = tokenProviderSelector.select(version);
            Parser parser = parserFactory.create();
            Lexer lexer = new Lexer(tokenProvider);

            try (BufferedReader reader = new BufferedReader(new StringReader(source))) {
                var tokens = lexer.lex(reader);
                var astSequence = parser.parse(tokens);
                List<ASTNode> ast = SequencesKt.toList(astSequence);

                String configText = new String(config.readAllBytes(), StandardCharsets.UTF_8);
                var formatterConfig = this.configReader.read(configText);

                String formatted = formatter.format(ast, formatterConfig, source);
                writer.write(formatted);
                writer.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al formatear el código: " + e.getMessage(), e);
        }
    }

    private static TokenProvider defaultTokenProvider(String version) {
        switch (version.trim()) {
            case "1.0":
                return PreConfiguredTokens.TOKENS_1_0;
            case "1.1":
                return PreConfiguredTokens.TOKENS_1_1;
            default:
                throw new IllegalArgumentException("Unsupported PrintScript version: " + version);
        }
    }

    public interface ParserFactory {
        Parser create();
    }

    public interface TokenProviderSelector {
        TokenProvider select(String version);
    }
}
