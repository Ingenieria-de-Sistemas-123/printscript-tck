package implementation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.printscript.lexer.Lexer;
import org.printscript.lexer.exception.LexicalException;
import org.printscript.lexer.pattern.PreConfiguredTokens;
import org.printscript.lexer.pattern.TokenProvider;
import org.printscript.parser.DefaultParser;
import org.printscript.parser.ParseException;
import org.printscript.parser.node.ASTNode;
import org.printscript.token.Token;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

final class ScriptSupport {
    private ScriptSupport() {
    }

    static Sequence<ASTNode> parseAstSequence(Reader reader, String version) {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(version, "version");
        TokenProvider provider = tokenProviderFor(version);
        Lexer lexer = new Lexer(provider);
        Sequence<Token> tokens = lexer.lex(reader);
        DefaultParser parser = new DefaultParser();
        return parser.parse(tokens); // streaming sequence
    }

    // parseAst usando Reader: materializa la Sequence en una List (solo usar si realmente se necesita en memoria)
    static List<ASTNode> parseAst(Reader reader, String version) {
        Sequence<ASTNode> seq = parseAstSequence(reader, version);
        List<ASTNode> ast = new ArrayList<>();
        for (ASTNode node : SequencesKt.asIterable(seq)) {
            ast.add(node);
        }
        return ast;
    }

    // Sobrecarga conveniente si ya se tiene el código en memoria
    static List<ASTNode> parseAst(String source, String version) {
        Objects.requireNonNull(source, "source");
        return parseAst(new StringReader(source), version);
    }

    // readAll lee completamente un InputStream pequeño (configs). No usar para scripts grandes.
    static String readAll(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "src");
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8); StringWriter writer = new StringWriter()) {
            reader.transferTo(writer);
            return writer.toString();
        }
    }

    private static TokenProvider tokenProviderFor(String version) {
        if (version == null) throw new IllegalArgumentException("Version must not be null");
        String normalized = version.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Version must not be blank");
        return switch (normalized) {
            case "1.0" -> PreConfiguredTokens.TOKENS_1_0;
            case "1.1" -> PreConfiguredTokens.TOKENS_1_1;
            default -> throw new IllegalArgumentException("Unsupported PrintScript version: " + version);
        };
    }

    static boolean isSyntaxException(Throwable throwable) {
        return !(throwable instanceof ParseException) && !(throwable instanceof LexicalException);
    }
}
