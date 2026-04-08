package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * FOR 语句节点。
 */
public class ForStatement extends AbstractStatement implements Statement {
    /** 循环变量名。 */
    private final String counterName;
    /** 起始表达式。 */
    private final Expression fromExpression;
    /** 结束表达式。 */
    private final Expression toExpression;
    /** 步长表达式。 */
    private final Expression stepExpression;
    /** 循环体语句列表。 */
    private final List<Statement> bodyStatementList;

    /**
     * 通过 Builder 构建 FOR 语句。
     *
     * @param builder Builder
     */
    private ForStatement(ForBuilder builder) {
        super(builder);
        this.bodyStatementList = new ArrayList<>();

        this.counterName = builder.counterName;
        this.fromExpression = builder.fromExpression;
        this.toExpression = builder.toExpression;
        this.stepExpression = builder.stepExpression;

        addChild(fromExpression);
        addChild(toExpression);
        addChild(stepExpression);

        if (builder.bodyStatementList != null) {
            builder.bodyStatementList.forEach(this::addBodyStatement);
        }
    }

    /**
     * 获取 FOR 语句 Builder。
     *
     * @return Builder
     */
    public static ForBuilder builder() {
        return new ForBuilder();
    }

    /**
     * 获取循环变量名。
     *
     * @return 循环变量名
     */
    public String getCounterName() {
        return counterName;
    }

    /**
     * 获取起始表达式。
     *
     * @return 起始表达式
     */
    public Expression getFromExpression() {
        return fromExpression;
    }

    /**
     * 获取结束表达式。
     *
     * @return 结束表达式
     */
    public Expression getToExpression() {
        return toExpression;
    }

    /**
     * 获取步长表达式。
     *
     * @return 步长表达式；未显式声明时返回 null
     */
    public Expression getStepExpression() {
        return stepExpression;
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
     * FOR 语句 Builder。
     */
    public static class ForBuilder extends StatementBuilder<ForBuilder> {
        /** 循环变量名。 */
        private String counterName;
        /** 起始表达式。 */
        private Expression fromExpression;
        /** 结束表达式。 */
        private Expression toExpression;
        /** 步长表达式。 */
        private Expression stepExpression;
        /** 循环体语句列表。 */
        private List<Statement> bodyStatementList = new ArrayList<>();

        @Override
        protected ForBuilder self() {
            return this;
        }

        /**
         * 设置循环变量名。
         *
         * @param counterName 循环变量名
         * @return 当前 builder
         */
        public ForBuilder withCounterName(String counterName) {
            this.counterName = counterName;
            return self();
        }

        /**
         * 设置起始表达式。
         *
         * @param fromExpression 起始表达式
         * @return 当前 builder
         */
        public ForBuilder withFromExpression(Expression fromExpression) {
            this.fromExpression = fromExpression;
            return self();
        }

        /**
         * 设置结束表达式。
         *
         * @param toExpression 结束表达式
         * @return 当前 builder
         */
        public ForBuilder withToExpression(Expression toExpression) {
            this.toExpression = toExpression;
            return self();
        }

        /**
         * 设置步长表达式。
         *
         * @param stepExpression 步长表达式
         * @return 当前 builder
         */
        public ForBuilder withStepExpression(Expression stepExpression) {
            this.stepExpression = stepExpression;
            return self();
        }

        /**
         * 设置循环体语句列表。
         *
         * @param bodyStatementList 循环体语句列表
         * @return 当前 builder
         */
        public ForBuilder withBodyStatementList(List<Statement> bodyStatementList) {
            this.bodyStatementList = bodyStatementList;
            return self();
        }

        @Override
        public ForStatement build() {
            return new ForStatement(this);
        }
    }
}
