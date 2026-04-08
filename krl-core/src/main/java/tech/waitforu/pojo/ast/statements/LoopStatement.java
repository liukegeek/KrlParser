package tech.waitforu.pojo.ast.statements;

import java.util.ArrayList;
import java.util.List;

/**
 * LOOP 语句节点。
 */
public class LoopStatement extends tech.waitforu.pojo.ast.statements.AbstractStatement implements Statement {

    /** 循环体语句列表。 */
    private final List<Statement> bodyStatementList;

    /**
     * 通过 Builder 构建 LOOP 语句。
     *
     * @param builder Builder
     */
    private LoopStatement(LoopBuilder builder) {
        super(builder);
        this.bodyStatementList = new ArrayList<>();

        if (builder.bodyStatementList != null) {
            builder.bodyStatementList.forEach(this::addBodyStatement);
        }


    }

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
     * 获取 LOOP 语句 Builder。
     *
     * @return Builder
     */
    public static LoopBuilder builder() {
        return new LoopBuilder();
    }

    /**
     * LOOP 语句 Builder。
     */
    public static class LoopBuilder extends StatementBuilder<LoopBuilder> {
        /** 循环体语句列表。 */
        private List<Statement> bodyStatementList = new ArrayList<>();

        /**
         * 返回当前 builder。
         *
         * @return 当前 builder
         */
        @Override
        protected LoopBuilder self() {
            return this;
        }

        /**
         * 设置循环体语句列表。
         *
         * @param bodyStatementList 循环体语句列表
         * @return 当前 builder
         */
        public LoopBuilder withBodyStatementList(List<Statement> bodyStatementList) {
            this.bodyStatementList = bodyStatementList;
            return self();
        }

        /**
         * 构建 LOOP 语句对象。
         *
         * @return LoopStatement
         */
        @Override
        public LoopStatement build() {
            return new LoopStatement(this);
        }
    }
}
