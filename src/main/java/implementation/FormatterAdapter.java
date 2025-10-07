// java
package implementation;

import interpreter.PrintScriptFormatter;
import org.printscript.formatter.CodeFormatter;
import org.printscript.formatter.config.ConfigJsonReader;
import org.printscript.formatter.config.FormatterConfig;
import org.printscript.parser.node.ASTNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

/**
 * Adapter para el TCK: parsea el código según la versión (soporta 1.0 y 1.1),
 * carga configuración JSON y delega en CodeFormatter.
 */
public final class FormatterAdapter implements PrintScriptFormatter {

    private final CodeFormatter formatter = new CodeFormatter();

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(writer, "writer");

        try {
            // Leer fuente completa
            String source = ScriptSupport.readAll(src);

            // Parsear AST (lanza IllegalArgumentException si versión inválida)
            List<ASTNode> ast = ScriptSupport.parseAst(source, version);

            // Cargar config (defaults si null o vacía)
            FormatterConfig cfg = FormatterConfigLoader.load(config);

            // Formatear con el source original para layout tracking
            String pretty = formatter.format(ast, cfg, source);

            writer.write(pretty);
            writer.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException("Error leyendo fuente", ex);
        } catch (RuntimeException ex) {
            if (ScriptSupport.isSyntaxException(ex)) {
                throw new IllegalStateException("Error de sintaxis al formatear: " + ex.getMessage(), ex);
            }
            throw ex;
        }
    }
}
