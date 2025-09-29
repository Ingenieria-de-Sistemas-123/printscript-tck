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

final class ScriptSupport {
    private ScriptSupport() {
    }

    static List<ASTNode> parseAst(String source, String version) {
        Objects.requireNonNull(source, "source");
        TokenProvider provider = tokenProviderFor(version);
        Lexer lexer = new Lexer(provider);

        // Lexer.lex ahora retorna Sequence<Token>
        Sequence<Token> seq = lexer.lex(new StringReader(source));
        List<Token> tokens = new ArrayList<>();
        for (var it = seq.iterator(); it.hasNext(); ) {
            tokens.add(it.next());
        }

        DefaultParser parser = new DefaultParser();
        return parser.parse(tokens);
    }

    static String readAll(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "src");
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
             StringWriter writer = new StringWriter()) {
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
        return throwable instanceof ParseException || throwable instanceof LexicalException;
    }
}
