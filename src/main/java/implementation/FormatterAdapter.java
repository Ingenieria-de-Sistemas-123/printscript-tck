package implementation;

import interpreter.PrintScriptFormatter;
import org.printscript.formatter.CodeFormatter;
import org.printscript.formatter.config.FormatterConfig;
import org.printscript.parser.node.ASTNode;

import java.io.*;
import java.util.List;
import java.util.Objects;

final class FormatterAdapter implements PrintScriptFormatter {
    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(writer, "writer");

        final String source;
        try {
            source = ScriptSupport.readAll(src);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read source", ex);
        }

        FormatterConfig formatterConfig = FormatterConfigLoader.load(config);

        final String formatted;
        try {
            List<ASTNode> parsedSource = ScriptSupport.parseAst(source, version);
            CodeFormatter codeFormatter = new CodeFormatter();
            formatted = codeFormatter.format(parsedSource, formatterConfig);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to format script: " + AdapterUtils.messageOrDefault(ex), ex);
        }

        try {
            writer.write(formatted);
            writer.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write formatted output", ex);
        }
    }
}
