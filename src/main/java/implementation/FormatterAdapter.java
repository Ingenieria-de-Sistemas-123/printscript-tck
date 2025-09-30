package implementation;

import interpreter.PrintScriptFormatter;

import java.io.*;
import java.util.List;
import java.util.Objects;

import org.printscript.formatter.CodeFormatter;
import org.printscript.formatter.config.FormatterConfig;
import org.printscript.parser.node.ASTNode;

final class FormatterAdapter implements PrintScriptFormatter {
    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(writer, "writer");

        String source;
        try {
            source = ScriptSupport.readAll(src); // ahora devuelve String
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read source", ex);
        }

        List<ASTNode> ast;
        try {
            ast = ScriptSupport.parseAst(source, version); // usa la sobrecarga String
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to parse script: " + AdapterUtils.messageOrDefault(ex), ex);
        }

        FormatterConfig formatterConfig = FormatterConfigLoader.load(config);
        CodeFormatter formatter = new CodeFormatter();
        String formatted = formatter.format(ast, formatterConfig);
        try {
            writer.write(formatted);
            writer.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write formatted output", ex);
        }
    }
}
