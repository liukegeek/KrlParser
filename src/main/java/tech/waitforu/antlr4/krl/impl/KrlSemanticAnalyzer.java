package tech.waitforu.antlr4.krl.impl;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import tech.waitforu.antlr4.krl.krlLexer;
import tech.waitforu.antlr4.krl.krlParser;
import tech.waitforu.pojo.krl.KrlProgram;
import tech.waitforu.pojo.krl.KrlStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * High level semantic helper that turns a KRL source string into a simple
 * call graph friendly model.
 */
public class KrlSemanticAnalyzer {

    /**
     * Parse a KRL source string into a {@link KrlProgram} containing nested statements.
     */
    public KrlProgram analyze(String content) {
        CharStream charStream = CharStreams.fromString(content);
        krlLexer lexer = new krlLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        krlParser parser = new krlParser(tokens);

        ParseTree parseTree = parser.start();
        List<KrlStatement> statements = collectStatements(parseTree);
        String moduleName = extractModuleName(parseTree);
        return new KrlProgram(moduleName, statements);
    }

    private List<KrlStatement> collectStatements(ParseTree parseTree) {
        krlParser.StatementListContext statementListContext = findFirstStatementList(parseTree);
        if (statementListContext == null) {
            return new ArrayList<>();
        }
        return new KrlStatementCollector().collect(statementListContext);
    }

    private String extractModuleName(ParseTree parseTree) {
        List<krlParser.ModuleNameContext> moduleNameContexts = ((krlParser.StartContext) parseTree)
                .getRuleContexts(krlParser.ModuleNameContext.class);
        if (moduleNameContexts.isEmpty()) {
            return null;
        }
        return moduleNameContexts.get(0).getText();
    }

    private krlParser.StatementListContext findFirstStatementList(ParseTree tree) {
        if (tree instanceof krlParser.StatementListContext) {
            return (krlParser.StatementListContext) tree;
        }
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            krlParser.StatementListContext nested = findFirstStatementList(child);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
}
