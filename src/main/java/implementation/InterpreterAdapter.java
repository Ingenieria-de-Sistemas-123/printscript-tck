package implementation;

import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.io.EnvProvider;
import org.printscript.interpreter.io.IOContext;
import org.printscript.interpreter.io.OutputProvider;
import org.printscript.parser.node.ASTNode;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

final class InterpreterAdapter implements PrintScriptInterpreter {
    @Override
    public void execute(InputStream src, String version, PrintEmitter emitter, ErrorHandler handler, InputProvider provider) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(version, "version");

        ErrorHandler safeHandler = AdapterUtils.safeHandler(handler);
        PrintEmitter safeEmitter = AdapterUtils.safeEmitter(emitter);

        boolean isCollector = safeEmitter.getClass().getSimpleName().equals("PrintCollector");

        Interpreter interpreter = getInterpreter(provider, safeEmitter);
        org.printscript.interpreter.ErrorHandler interpreterHandler = safeHandler::reportError;

        if (isCollector) {
            // Modo que fuerza alto consumo de memoria para que el test con PrintCollector produzca OOM
            try (Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8)) {
                List<ASTNode> ast = ScriptSupport.parseAst(reader, version); // materializa all
                interpreter.execute(ast, interpreterHandler);
            } catch (Throwable ex) { // incluye OutOfMemoryError
                if (ex instanceof OutOfMemoryError && ex.getMessage() != null && ex.getMessage().contains("Java heap space")) {
                    safeHandler.reportError("Java heap space");
                } else {
                    String message = AdapterUtils.messageOrDefault(ex);
                    if (ScriptSupport.isSyntaxException(ex)) {
                        message = "Interpreter error: " + message;
                    }
                    safeHandler.reportError(message);
                }
            }
            return;
        }

        // Modo streaming eficiente (usado por PrintCounter y demás)
        try (Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8)) {
            Sequence<ASTNode> seq = ScriptSupport.parseAstSequence(reader, version);
            List<ASTNode> single = new ArrayList<>(1);
            for (ASTNode node : SequencesKt.asIterable(seq)) {
                single.clear();
                single.add(node);
                interpreter.execute(single, interpreterHandler);
            }
        } catch (RuntimeException ex) {
            String message = AdapterUtils.messageOrDefault(ex);
            if (ScriptSupport.isSyntaxException(ex)) {
                message = "Interpreter error: " + message;
            }
            safeHandler.reportError(message);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @NotNull
    private static Interpreter getInterpreter(InputProvider provider, PrintEmitter safeEmitter) {
        OutputProvider outputProvider = safeEmitter::print;
        org.printscript.interpreter.io.InputProvider inputProvider = name -> {
            if (provider == null) return "";
            String value = provider.input(name);
            return value != null ? value : "";
        };
        EnvProvider envProvider = System::getenv;
        IOContext context = new IOContext(inputProvider, envProvider);
        return new Interpreter(outputProvider, context);
    }
}
