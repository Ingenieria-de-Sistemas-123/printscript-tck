package implementation;

import interpreter.PrintScriptFormatter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;

import org.printscript.lexer.Lexer;
import org.printscript.lexer.pattern.PreConfiguredTokens;
import org.printscript.lexer.pattern.TokenProvider;

import org.printscript.token.Token;

import org.printscript.parser.DefaultParser;
import org.printscript.parser.node.ASTNode;

import org.printscript.formatter.CodeFormatter;
import org.printscript.formatter.config.FormatterConfig;

import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

@SuppressWarnings("unchecked")
public final class PrintScriptFormatterImpl implements PrintScriptFormatter {

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        try {
            Reader sourceReader = new InputStreamReader(src, StandardCharsets.UTF_8);

            TokenProvider provider = selectProvider(version);
            Lexer lexer = new Lexer(provider);

            Sequence<Token> seq = lexer.lex(sourceReader);
            List<Token> tokens = SequencesKt.toList(seq);

            DefaultParser parser = new DefaultParser();
            Object parsed = parser.parse(tokens);
            List<ASTNode> program = toProgramList(parsed);

            FormatterConfig cfg = readConfigOrDefaults(config);

            String formatted = new CodeFormatter().format(program, cfg);
            formatted = formatted.replaceAll("(\\R)[ \\t]*(\\R)+", "$1");

            writer.write(formatted);
            writer.flush();

        } catch (Exception e) {
            throw new RuntimeException("Formatter adapter failed: " + e.getMessage(), e);
        }
    }

    private static FormatterConfig readConfigOrDefaults(InputStream config) {
        if (config == null) {
            return new FormatterConfig(
                    false,
                    false,
                    true,
                    true,
                    0,
                    false,
                    4
            );
        }
        return new Gson().fromJson(
                new InputStreamReader(config, StandardCharsets.UTF_8),
                FormatterConfig.class
        );
    }

    private static List<ASTNode> toProgramList(Object parsed) throws Exception {
        if (parsed instanceof List) {
            return (List<ASTNode>) parsed;
        }
        try {
            Method m = parsed.getClass().getMethod("getChildren");
            Object children = m.invoke(parsed);
            if (children instanceof List) {
                return (List<ASTNode>) children;
            }
        } catch (NoSuchMethodException ignore) {}
        return java.util.List.of((ASTNode) parsed);
    }

    private static TokenProvider selectProvider(String version) throws Exception {
        boolean is11 = version != null && version.startsWith("1.1");

        try {
            Method m = PreConfiguredTokens.class.getMethod("forVersion", String.class);
            Object tp = m.invoke(null, version);
            if (tp instanceof TokenProvider) return (TokenProvider) tp;
        } catch (NoSuchMethodException ignore) {}

        try {
            Method m = PreConfiguredTokens.class.getMethod(is11 ? "v11" : "v10");
            Object tp = m.invoke(null);
            if (tp instanceof TokenProvider) return (TokenProvider) tp;
        } catch (NoSuchMethodException ignore) {}

        String fieldName = is11 ? "TOKENS_1_1" : "TOKENS_1_0";
        try {
            Field f = PreConfiguredTokens.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object tp = f.get(null);
            if (tp instanceof TokenProvider) return (TokenProvider) tp;
        } catch (NoSuchFieldException e) {
            Field f = PreConfiguredTokens.class.getDeclaredField(is11 ? "TOKENS11" : "TOKENS10");
            f.setAccessible(true);
            Object tp = f.get(null);
            if (tp instanceof TokenProvider) return (TokenProvider) tp;
        }

        throw new IllegalStateException("No se pudo obtener TokenProvider para versión " + version);
    }
}
