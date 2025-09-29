package implementation;

import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import org.printscript.linter.LintConfig;
import org.printscript.linter.Linter;
import org.printscript.linter.issue.Issue;
import org.printscript.linter.rules.NoDuplicateVariableRule;
import org.printscript.linter.rules.PrintlnRestrictionRule;
import org.printscript.linter.rules.Rule;
import org.printscript.linter.rules.StringNumberConcatRule;
import org.printscript.parser.node.ASTNode;

final class LinterAdapter implements PrintScriptLinter {
    private static final List<Rule> RULES = List.of(
            new PrintlnRestrictionRule(),
            new StringNumberConcatRule(),
            new NoDuplicateVariableRule()
    );

    @Override
    public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(version, "version");
        ErrorHandler safeHandler = AdapterUtils.safeHandler(handler);

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
            safeHandler.reportError("Failed to parse script: " + AdapterUtils.messageOrDefault(ex));
            return;
        }

        LintConfig lintConfig = LintConfigLoader.load(config);
        Linter linter = new Linter(RULES, lintConfig);
        List<Issue> issues = linter.analyze(ast);
        for (Issue issue : issues) {
            safeHandler.reportError(formatIssue(issue));
        }
    }

    private String formatIssue(Issue issue) {
        return String.format(
                "[%s] %s: %s (%d:%d - %d:%d)",
                issue.getSeverity(),
                issue.getRuleId(),
                issue.getMessage(),
                issue.getStartLine(),
                issue.getStartCol(),
                issue.getEndLine(),
                issue.getEndCol()
        );
    }
}
