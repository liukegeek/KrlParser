package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * REPEAT/UNTIL 语句节点。
 */
public class RepeatStatement extends AbstractStatement implements Statement {
    /** UNTIL 条件表达式。 */
    private final Expression untilExpression;

    /** 循环体语句列表。 */
    private final List<Statement> bodyStatementList;

    /**
     * 通过 Builder 构建 REPEAT/UNTIL 语句。
     *
     * @param builder Builder
     */
    private RepeatStatement(RepeatBuilder builder) {
        super(builder);
        this.untilExpression = builder.untilExpression;
        this.bodyStatementList = new ArrayList<>();

        addChild(untilExpression);

        if (builder.bodyStatementList != null) {
            builder.bodyStatementList.forEach(this::addBodyStatement);
        }
    }

    /**
     * 获取 REPEAT/UNTIL 语句 Builder。
     *
     * @return Builder
     */
    public static RepeatBuilder builder() {
        return new RepeatBuilder();
    }

    /**
     * 获取 UNTIL 条件表达式。
     *
     * @return UNTIL 条件表达式
     */
    public Expression getUntilExpression() {
        return untilExpression;
    }

    /**
     * 获取循环体语句列表。
     *
     * @return 循环体语句列表
     */
    public List<Statement> getBodyStatementList() {
        return List.copyOf(bodyStatementList);
    }

    public boolean addBodyStatement(Statement statement) {
        if (!addChildStatement(statement)) {
            return false;
        }
        return bodyStatementList.add(statement);
    }

    /**
     * REPEAT/UNTIL 语句 Builder。
     */
    public static class RepeatBuilder extends StatementBuilder<RepeatBuilder> {
        /** UNTIL 条件表达式。 */
        private Expression untilExpression;
        /** 循环体语句列表。 */
        private java.util.List<Statement> bodyStatementList = new ArrayList<>();

        @Override
        protected RepeatBuilder self() {
            return this;
        }

        /**
         * 设置 UNTIL 条件表达式。
         *
         * @param untilExpression UNTIL 条件表达式
         * @return 当前 builder
         */
        public RepeatBuilder withUntilExpression(Expression untilExpression) {
            this.untilExpression = untilExpression;
            return self();
        }

        /**
         * 设置循环体语句列表。
         *
         * @param bodyStatementList 循环体语句列表
         * @return 当前 builder
         */
        public RepeatBuilder withBodyStatementList(java.util.List<Statement> bodyStatementList) {
            this.bodyStatementList = bodyStatementList;
            return self();
        }

        @Override
        public RepeatStatement build() {
            return new RepeatStatement(this);
        }
    }
}
