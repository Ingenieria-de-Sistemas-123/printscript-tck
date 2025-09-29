package implementation;

import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.io.EnvProvider;
import org.printscript.interpreter.io.IOContext;
import org.printscript.interpreter.io.OutputProvider;
import org.printscript.parser.node.ASTNode;

final class InterpreterAdapter implements PrintScriptInterpreter {
    @Override
    public void execute(InputStream src, String version, PrintEmitter emitter, ErrorHandler handler, InputProvider provider) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(version, "version");

        ErrorHandler safeHandler = AdapterUtils.safeHandler(handler);
        PrintEmitter safeEmitter = AdapterUtils.safeEmitter(emitter);

        String source;
        try {
            source = ScriptSupport.readAll(src);
        } catch (IOException ex) {
            safeHandler.reportError("Failed to read source: " + AdapterUtils.messageOrDefault(ex));
            return;
        }

        List<ASTNode> ast;
        try {
            ast = ScriptSupport.parseAst(source, version);
        } catch (RuntimeException ex) {
            String message = AdapterUtils.messageOrDefault(ex);
            if (!ScriptSupport.isSyntaxException(ex)) {
                message = "Interpreter error: " + message;
            }
            safeHandler.reportError(message);
            return;
        }

        Interpreter interpreter = getInterpreter(provider, safeEmitter);
        org.printscript.interpreter.ErrorHandler interpreterHandler = safeHandler::reportError;
        try {
            interpreter.execute(ast, interpreterHandler);
        } catch (RuntimeException ex) {
            safeHandler.reportError("Interpreter error: " + AdapterUtils.messageOrDefault(ex));
        }
    }

    @NotNull
    private static Interpreter getInterpreter(InputProvider provider, PrintEmitter safeEmitter) {
        OutputProvider outputProvider = safeEmitter::print;
        org.printscript.interpreter.io.InputProvider inputProvider = name -> {
            if (provider == null) {
                return "";
            }
            String value = provider.input(name);
            return value != null ? value : "";
        };
        EnvProvider envProvider = System::getenv;
        IOContext context = new IOContext(inputProvider, envProvider);
        return new Interpreter(outputProvider, context);
    }
}