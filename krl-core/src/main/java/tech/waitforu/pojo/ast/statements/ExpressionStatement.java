package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.Expression;

/**
 * 表达式语句节点。
 */
public class ExpressionStatement extends AbstractStatement implements Statement {
    /** 语句中包含的表达式。 */
    private Expression expression;

    /**
     * 通过 Builder 构建表达式语句。
     *
     * @param builder Builder
     */
    private ExpressionStatement(ExpressionStatementBuilder builder){
        super(builder);
        this.expression = builder.expression;
        addChild(this.expression);
    }

    /**
     * 获取表达式。
     *
     * @return 表达式对象
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * 设置表达式并更新 AST 子节点关系。
     *
     * @param expression 新表达式
     */
    public void setExpression(Expression expression) {
        removeChild(this.expression);
        this.expression = expression;
        addChild(this.expression);
    }

    /**
     * 获取表达式语句 Builder。
     *
     * @return Builder
     */
    public static ExpressionStatementBuilder builder() {
        return new ExpressionStatementBuilder();
    }

    /**
     * 表达式语句 Builder。
     */
    public static class ExpressionStatementBuilder extends AbstractStatement.StatementBuilder<ExpressionStatementBuilder> {
        /** 表达式。 */
        private Expression expression;

        /**
         * 返回当前 builder。
         *
         * @return 当前 builder
         */
        @Override
        protected ExpressionStatementBuilder self() {
            return this;
        }

        /**
         * 设置表达式。
         *
         * @param expression 表达式
         * @return 当前 builder
         */
        public ExpressionStatementBuilder withExpression(Expression expression){
            this.expression = expression;
            return self();
        }

        /**
         * 构建表达式语句。
         *
         * @return ExpressionStatement
         */
        @Override
        public ExpressionStatement build() {
            return new ExpressionStatement(this);
        }
    }
}
