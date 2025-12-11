package tech.waitforu.antlr4.krl.impl;

import org.antlr.v4.runtime.tree.ParseTree;
import tech.waitforu.antlr4.krl.krlBaseVisitor;
import tech.waitforu.antlr4.krl.krlParser;
import tech.waitforu.pojo.krl.CallStatement;
import tech.waitforu.pojo.krl.KrlStatement;
import tech.waitforu.pojo.krl.LoopStatement;
import tech.waitforu.pojo.krl.SwitchCase;
import tech.waitforu.pojo.krl.SwitchStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Collects high level statements that are interesting for call graph generation.
 */
public class KrlStatementCollector {

    public List<KrlStatement> collect(krlParser.StatementListContext context) {
        List<KrlStatement> statements = new ArrayList<>();
        for (krlParser.StatementContext statementContext : context.statement()) {
            KrlStatement statement = collectStatement(statementContext);
            if (statement != null) {
                statements.add(statement);
            }
        }
        return statements;
    }

    private KrlStatement collectStatement(krlParser.StatementContext context) {
        if (context instanceof krlParser.LoopStatementContext loopContext) {
            List<KrlStatement> body = collect(loopContext.statementList());
            return new LoopStatement(body);
        }
        if (context instanceof krlParser.SwitchStatementContext switchContext) {
            return buildSwitchStatement(switchContext);
        }
        if (context instanceof krlParser.ExpressionStatementContext expressionStatementContext) {
            Optional<CallStatement> callStatement = extractCall(expressionStatementContext.expression());
            return callStatement.orElse(null);
        }
        return null;
    }

    private SwitchStatement buildSwitchStatement(krlParser.SwitchStatementContext context) {
        krlParser.SwitchBlockStatementGroupsContext block = context.switchBlockStatementGroups();
        List<SwitchCase> switchCases = new ArrayList<>();
        List<krlParser.CaseLabelContext> labels = block.caseLabel();
        List<krlParser.StatementListContext> bodies = block.statementList();

        for (int i = 0; i < labels.size(); i++) {
            List<String> labelTexts = labels.get(i).expression().stream()
                    .map(ParseTree::getText)
                    .collect(Collectors.toList());
            List<KrlStatement> statements = collect(bodies.get(i));
            switchCases.add(new SwitchCase(labelTexts, statements));
        }

        List<KrlStatement> defaultStatements = Collections.emptyList();
        if (bodies.size() > labels.size()) {
            defaultStatements = collect(bodies.get(bodies.size() - 1));
        }

        return new SwitchStatement(context.expression().getText(), switchCases, defaultStatements);
    }

    private Optional<CallStatement> extractCall(krlParser.ExpressionContext expressionContext) {
        CallExpressionVisitor visitor = new CallExpressionVisitor();
        CallExpressionResult result = visitor.visit(expressionContext);
        if (result == null || result.name == null) {
            return Optional.empty();
        }
        return Optional.of(new CallStatement(result.name, result.arguments));
    }

    private static class CallExpressionResult {
        private final String name;
        private final List<String> arguments;

        CallExpressionResult(String name, List<String> arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }

    private static class CallExpressionVisitor extends krlBaseVisitor<CallExpressionResult> {
        @Override
        public CallExpressionResult visitInvokeCallable(krlParser.InvokeCallableContext ctx) {
            List<String> arguments = ctx.expression().stream()
                    .map(ParseTree::getText)
                    .collect(Collectors.toList());
            return new CallExpressionResult(ctx.callableName.getText(), arguments);
        }

        @Override
        protected CallExpressionResult aggregateResult(CallExpressionResult aggregate, CallExpressionResult nextResult) {
            return aggregate == null ? nextResult : aggregate;
        }
    }
}
