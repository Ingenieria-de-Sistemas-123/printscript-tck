package implementation;

import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;
import org.printscript.interpreter.Interpreter;
import org.printscript.interpreter.adapter.AstToIr;
import org.printscript.interpreter.adapter.AstToIrMapper;
import org.printscript.interpreter.adapter.AstToIrV11;
import org.printscript.interpreter.io.IOContext;
import org.printscript.interpreter.io.OutputProvider;
import org.printscript.interpreter.io.SystemEnvProvider;
import org.printscript.interpreter.runtime.RuntimeError;
import org.printscript.lexer.Lexer;
import org.printscript.lexer.exception.LexicalException;
import org.printscript.parser.DefaultParser;
import org.printscript.parser.ParseException;
import org.printscript.parser.node.ASTNode;
import org.printscript.token.Token;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PrintScriptInterpreterImpl implements PrintScriptInterpreter {

    @Override
    public void execute(
            InputStream src,
            String version,
            PrintEmitter emitter,
            ErrorHandler handler,
            InputProvider provider
    ) {
        // leer el código fuente
        String source;
        try {
            source = new String(src.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            handler.reportError("Error reading source: " + e.getMessage());
            return;
        }

        try {
            // análisis léxico y sintáctico
            Lexer lexer = new Lexer();
            List<Token> tokens = lexer.lex(source);
            DefaultParser parser = new DefaultParser();
            List<ASTNode> ast = parser.parse(tokens);

            // adaptadores de entrada y salida
            OutputProvider outputProvider = emitter::print;

            org.printscript.interpreter.io.InputProvider psInput =
                    new org.printscript.interpreter.io.InputProvider() {
                        @Override
                        public String readLine(String prompt) {
                            // imprimir el prompt ANTES de leer, como StdinInputProvider:contentReference[oaicite:1]{index=1}
                            emitter.print(prompt);
                            String line = provider.input(prompt);
                            if (line == null) {
                                // propagar el mismo error que el intérprete oficial
                                throw new RuntimeError(
                                        "No se recibió input para el prompt '" + prompt + "'"
                                );
                            }
                            return line;
                        }
                    };

            IOContext io = new IOContext(psInput, new SystemEnvProvider());

            // seleccionar mapper según versión
            AstToIrMapper mapper;
            String v = version.trim();
            if ("1.0".equals(v)) {
                mapper = new AstToIr();
            } else if ("1.1".equals(v)) {
                mapper = new AstToIrV11(); // lanzará un error si 1.1 no está implementado:contentReference[oaicite:1]{index=1}
            } else {
                handler.reportError("Unsupported PrintScript version: " + version);
                return;
            }


            // ejecutar y capturar cualquier error en tiempo de ejecución
            Interpreter interpreter = new Interpreter(outputProvider);
            try {
                interpreter.execute(ast, io, mapper);
            } catch (Throwable ex) {
                // reportar errores de tiempo de ejecución (incluidos OutOfMemoryError)
                handler.reportError(ex.getMessage());
            }

        } catch (LexicalException | ParseException e) {
            handler.reportError(e.getMessage());
        }
    }
}
