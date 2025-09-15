package implementation;

import interpreter.PrintScriptFormatter;
import interpreter.PrintScriptInterpreter;
import interpreter.PrintScriptLinter;

import java.io.BufferedInputStream;
import java.util.Arrays;

public class CustomImplementationFactory implements PrintScriptFactory {

    @Override
    public PrintScriptInterpreter interpreter() {
        return new PrintScriptInterpreterImpl();
    }

    @Override
    public PrintScriptFormatter formatter() {
        return new PrintScriptFormatterImpl();
    }

    @Override
    public PrintScriptLinter linter() {
        return new PrintScriptLinterImpl();
    }
}