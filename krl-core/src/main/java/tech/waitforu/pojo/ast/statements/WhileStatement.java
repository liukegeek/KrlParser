package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.Expression;

import java.util.ArrayList;

/**
 * WHILE 语句节点。
 */
public class WhileStatement extends AbstractStatement implements Statement {
    /** 循环条件表达式。 */
    private final Expression conditionExpression;

    /**
     * 通过 Builder 构建 WHILE 语句。
     *
     * @param builder Builder
     */
    private WhileStatement(WhileBuilder builder) {
        super(builder);
        this.childStatementList = new ArrayList<>();
        this.conditionExpression = builder.conditionExpression;

        addChild(conditionExpression);

        if (builder.bodyStatementList != null) {
            builder.bodyStatementList.forEach(this::addChildStatement);
        }
    }

    /**
     * 获取 WHILE 语句 Builder。
     *
     * @return Builder
     */
    public static WhileBuilder builder() {
        return new WhileBuilder();
    }

    /**
     * 获取循环条件表达式。
     *
     * @return 循环条件表达式
     */
    public Expression getConditionExpression() {
        return conditionExpression;
    }

    /**
     * WHILE 语句 Builder。
     */
    public static class WhileBuilder extends StatementBuilder<WhileBuilder> {
        /** 循环条件表达式。 */
        private Expression conditionExpression;
        /** 循环体语句列表。 */
        private java.util.List<Statement> bodyStatementList = new ArrayList<>();

        @Override
        protected WhileBuilder self() {
            return this;
        }

        /**
         * 设置循环条件表达式。
         *
         * @param conditionExpression 条件表达式
         * @return 当前 builder
         */
        public WhileBuilder withConditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return self();
        }

        /**
         * 设置循环体语句列表。
         *
         * @param bodyStatementList 循环体语句列表
         * @return 当前 builder
         */
        public WhileBuilder withBodyStatementList(java.util.List<Statement> bodyStatementList) {
            this.bodyStatementList = bodyStatementList;
            return self();
        }

        @Override
        public WhileStatement build() {
            return new WhileStatement(this);
        }
    }
}
