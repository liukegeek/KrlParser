package tech.waitforu.pojo.ast.statements;

/**
 * LOOP 语句节点。
 */
public class LoopStatement extends tech.waitforu.pojo.ast.statements.AbstractStatement implements Statement {
    /**
     * 通过 Builder 构建 LOOP 语句。
     *
     * @param builder Builder
     */
    private LoopStatement(LoopBuilder builder) {
        super(builder);
        this.statementType = builder.statementType;

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
