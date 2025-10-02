package interpreter;

import org.jetbrains.annotations.NotNull;

public class OutputAdapter implements org.printscript.interpreter.io.OutputProvider {
    private final PrintEmitter emitter;

    public OutputAdapter(PrintEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void println(@NotNull String s) {
        emitter.print(s);
    }
}