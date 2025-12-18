package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.AbstractAstNode;

/**
 * ClassName: LoopStatement
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 15:08
 * Version 1.0
 */
public class LoopStatement extends AbstractStatement implements Statement {
    // 构造函数
    private LoopStatement(LoopBuilder builder) {
        super(builder);
        this.statementType = builder.statementType;

    }

    public static LoopBuilder builder() {
        return new LoopBuilder();
    }

    public static class LoopBuilder extends AbstractStatement.StatementBuilder<LoopBuilder> {
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
