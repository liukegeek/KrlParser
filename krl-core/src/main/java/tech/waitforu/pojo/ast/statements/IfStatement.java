package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * IF/ELSE 语句节点。
 */
public class IfStatement extends AbstractStatement implements Statement {
    /** IF 条件表达式。 */
    private final Expression conditionExpression;
    /** THEN 分支语句列表。 */
    private final List<Statement> thenStatementList;
    /** ELSE 分支语句列表。 */
    private final List<Statement> elseStatementList;

    /**
     * 通过 Builder 构建 IF/ELSE 语句。
     *
     * @param builder Builder
     */
    private IfStatement(IfBuilder builder) {
        super(builder);
        this.childStatementList = new ArrayList<>();
        this.conditionExpression = builder.conditionExpression;
        this.thenStatementList = new ArrayList<>();
        this.elseStatementList = new ArrayList<>();

        addChild(conditionExpression);

        if (builder.thenStatementList != null) {
            builder.thenStatementList.forEach(this::addThenStatement);
        }
        if (builder.elseStatementList != null) {
            builder.elseStatementList.forEach(this::addElseStatement);
        }
    }

    /**
     * 获取 IF/ELSE 语句 Builder。
     *
     * @return Builder
     */
    public static IfBuilder builder() {
        return new IfBuilder();
    }

    /**
     * 获取条件表达式。
     *
     * @return 条件表达式
     */
    public Expression getConditionExpression() {
        return conditionExpression;
    }

    /**
     * 获取 THEN 分支语句列表。
     *
     * @return THEN 分支语句列表拷贝
     */
    public List<Statement> getThenStatementList() {
        return new ArrayList<>(thenStatementList);
    }

    /**
     * 获取 ELSE 分支语句列表。
     *
     * @return ELSE 分支语句列表拷贝
     */
    public List<Statement> getElseStatementList() {
        return new ArrayList<>(elseStatementList);
    }

    /**
     * 添加 THEN 分支语句并建立 AST 子节点关系。
     *
     * @param statement THEN 分支语句
     * @return true 表示添加成功
     */
    public boolean addThenStatement(Statement statement) {
        if (!addChildStatement(statement)) {
            return false;
        }
        return thenStatementList.add(statement);
    }

    /**
     * 添加 ELSE 分支语句并建立 AST 子节点关系。
     *
     * @param statement ELSE 分支语句
     * @return true 表示添加成功
     */
    public boolean addElseStatement(Statement statement) {
        if (!addChildStatement(statement)) {
            return false;
        }
        return elseStatementList.add(statement);
    }

    /**
     * IF/ELSE 语句 Builder。
     */
    public static class IfBuilder extends StatementBuilder<IfBuilder> {
        /** 条件表达式。 */
        private Expression conditionExpression;
        /** THEN 分支语句列表。 */
        private List<Statement> thenStatementList = new ArrayList<>();
        /** ELSE 分支语句列表。 */
        private List<Statement> elseStatementList = new ArrayList<>();

        @Override
        protected IfBuilder self() {
            return this;
        }

        /**
         * 设置条件表达式。
         *
         * @param conditionExpression 条件表达式
         * @return 当前 builder
         */
        public IfBuilder withConditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return self();
        }

        /**
         * 设置 THEN 分支语句列表。
         *
         * @param thenStatementList THEN 分支语句列表
         * @return 当前 builder
         */
        public IfBuilder withThenStatementList(List<Statement> thenStatementList) {
            this.thenStatementList = thenStatementList;
            return self();
        }

        /**
         * 设置 ELSE 分支语句列表。
         *
         * @param elseStatementList ELSE 分支语句列表
         * @return 当前 builder
         */
        public IfBuilder withElseStatementList(List<Statement> elseStatementList) {
            this.elseStatementList = elseStatementList;
            return self();
        }

        @Override
        public IfStatement build() {
            return new IfStatement(this);
        }
    }
}
