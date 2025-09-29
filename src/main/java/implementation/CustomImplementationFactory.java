package implementation;

import interpreter.PrintScriptFormatter;
import interpreter.PrintScriptInterpreter;
import interpreter.PrintScriptLinter;

public class CustomImplementationFactory implements PrintScriptFactory {
    private final PrintScriptInterpreter interpreter;
    private final PrintScriptFormatter formatter;
    private final PrintScriptLinter linter;

    public CustomImplementationFactory() {
        this.interpreter = new InterpreterAdapter();
        this.formatter = new FormatterAdapter();
        this.linter = new LinterAdapter();
    }

    @Override
    public PrintScriptInterpreter interpreter() {
        return interpreter;
    }

    @Override
    public PrintScriptFormatter formatter() {
        return formatter;
    }

    @Override
    public PrintScriptLinter linter() {
        return linter;
    }
}