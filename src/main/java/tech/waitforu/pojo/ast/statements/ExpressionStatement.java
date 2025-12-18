package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.AbstractExpression;
import tech.waitforu.pojo.ast.expression.Expression;

/**
 * ClassName: ExpressionStatement
 * Package: tech.waitforu.pojo.ast.statements
 * Description:
 * Author: LiuKe
 * Create: 2025/12/17 11:34
 * Version 1.0
 */
public class ExpressionStatement extends AbstractStatement implements Statement {
    private Expression expression;

    private ExpressionStatement(ExpressionStatementBuilder builder){
        super(builder);
        this.expression = builder.expression;
        addChild(this.expression);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        removeChild(this.expression);
        this.expression = expression;
        addChild(this.expression);
    }

    public static ExpressionStatementBuilder builder() {
        return new ExpressionStatementBuilder();
    }

    public static class ExpressionStatementBuilder extends AbstractStatement.StatementBuilder<ExpressionStatementBuilder> {
        private Expression expression;

        @Override
        protected ExpressionStatementBuilder self() {
            return this;
        }

        public ExpressionStatementBuilder withExpression(Expression expression){
            this.expression = expression;
            return self();
        }

        @Override
        public ExpressionStatement build() {
            return new ExpressionStatement(this);
        }
    }
}
