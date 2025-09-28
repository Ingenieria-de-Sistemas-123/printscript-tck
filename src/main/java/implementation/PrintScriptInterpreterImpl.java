package implementation;

import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.io.EnvProvider;
import org.printscript.interpreter.io.OutputProvider;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.pattern.PreConfiguredTokens;
import org.printscript.lexer.pattern.TokenProvider;
import org.printscript.parser.DefaultParser;
import org.printscript.parser.Parser;
import org.printscript.parser.node.ASTNode;
import org.printscript.token.Token;

/**
 * Implementation of the PrintScriptInterpreter interface that adapts the
 * PrintScript library interpreter to the TCK requirements.  This class
 * lexes and parses the input program using the proper token provider
 * depending on the requested language version (1.0 or 1.1), performs a
 * light-weight preprocessing step to replace readInput and readEnv
 * expressions with literal nodes using the provided input and system
 * environment, and then executes the resulting AST with the library
 * interpreter.  All output is forwarded to the provided PrintEmitter
 * and any syntax, semantic or runtime errors are reported via the
 * ErrorHandler interface.
 */
public class PrintScriptInterpreterImpl implements PrintScriptInterpreter {

    @Override
    public void execute(InputStream src,
                        String version,
                        PrintEmitter emitter,
                        ErrorHandler handler,
                        InputProvider provider) {
        try {
            // 1) Tokens por versión
            TokenProvider tokenProvider = "1.0".equals(version)
                    ? PreConfiguredTokens.TOKENS_1_0
                    : PreConfiguredTokens.TOKENS_1_1;

            // 2) Lexer + Parser
            Lexer lexer = new Lexer(tokenProvider);
            List<Token> tokens = lexer.lex(new InputStreamReader(src, StandardCharsets.UTF_8));
            Parser parser = new DefaultParser();
            List<ASTNode> ast = parser.parse(tokens);

            // 3) Providers del core
            Interpreter interpreter = getInterpreter(emitter, handler, provider);
            interpreter.execute(ast);

        } catch (Exception ex) {
            handler.reportError(ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    @NotNull
    private static Interpreter getInterpreter(PrintEmitter emitter, ErrorHandler handler, InputProvider provider) {
        OutputProvider out = msg -> {
            try {
                emitter.print(msg);
            } catch (Exception ignore) {
            }
        };

        // el core usa EnvProvider:
        EnvProvider env =
                key -> System.getenv(key) != null ? System.getenv(key) : "";

        // Adapt InputProvider del TCK al del core:
        InputProvider in =
                prompt -> {
                    try {
                        return provider.input(prompt);
                    } catch (Exception e) {
                        handler.reportError(e.getMessage());
                        return "";
                    }
                };

        // 4) Ejecutar
        return new Interpreter(out);
    }
}
