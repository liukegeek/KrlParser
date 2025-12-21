package tech.waitforu.pojo.ast.statements;

/**
 * ClassName: statements.ast.pojo.tech.waitforu.LoopStatement
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 15:08
 * Version 1.0
 */
public class LoopStatement extends tech.waitforu.pojo.ast.statements.AbstractStatement implements Statement {
    // 构造函数
    private LoopStatement(LoopBuilder builder) {
        super(builder);
        this.statementType = builder.statementType;

    }

    public static LoopBuilder builder() {
        return new LoopBuilder();
    }

    public static class LoopBuilder extends StatementBuilder<LoopBuilder> {
        @Override
        protected LoopBuilder self() {
            return this;
        }
        // 构建方法
        @Override
        public LoopStatement build() {
            return new LoopStatement(this);
        }
    }
}
