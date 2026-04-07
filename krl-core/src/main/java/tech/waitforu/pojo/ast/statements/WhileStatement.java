package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * WHILE 语句节点。
 */
public class WhileStatement extends AbstractStatement implements Statement {
    /** 循环条件表达式。 */
    private final Expression conditionExpression;
    /** 循环体语句列表。 */
    private final List<Statement> bodyStatementList;

    /**
     * 通过 Builder 构建 WHILE 语句。
     *
     * @param builder Builder
     */
    private WhileStatement(WhileBuilder builder) {
        super(builder);
        this.conditionExpression = builder.conditionExpression;
        this.bodyStatementList = new ArrayList<>();

        addChild(conditionExpression);

        if (builder.bodyStatementList != null) {
            builder.bodyStatementList.forEach(this::addBodyStatement);
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
     * 获取循环体语句列表。
     *
     * @return 循环体语句列表
     */
    public List<Statement> getBodyStatementList() {
        return bodyStatementList;
    }

    /**
     * 添加循环体语句, 并将其添加到子语句列表中。
     *
     * @param statement 循环体语句
     * @return 是否添加成功
     */
    public boolean addBodyStatement(Statement statement) {
        if (!addChildStatement(statement)) {
            return false;
        }
        return bodyStatementList.add(statement);
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
