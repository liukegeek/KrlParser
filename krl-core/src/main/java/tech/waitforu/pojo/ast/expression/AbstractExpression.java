package tech.waitforu.pojo.ast.expression;

import tech.waitforu.pojo.ast.AbstractAstNode;

/**
 * 表达式抽象基类。
 * <p>
 * 在通用 AST 节点能力上扩展表达式类型字段。
 */
public abstract class AbstractExpression extends AbstractAstNode implements Expression {
    /** 表达式类型。 */
    ExpressionType expressionType;

    /**
     * 获取表达式类型。
     *
     * @return 表达式类型
     */
    @Override
    public ExpressionType getExpressionType() {
        return expressionType;
    }

    /**
     * 设置表达式类型。
     *
     * @param expressionType 表达式类型
     */
    public void setExpressionType(ExpressionType expressionType) {
        this.expressionType = expressionType;
    }

    /**
     * 通过 Builder 构建表达式基类字段。
     *
     * @param builder 表达式构建器
     */
    protected AbstractExpression(ExpressionBuilder<?> builder){
        super(builder);
        this.expressionType = builder.expressionType;
    }

    /**
     * 表达式 Builder 基类。
     *
     * @param <T> Builder 自类型
     */
    public abstract static class ExpressionBuilder<T extends ExpressionBuilder<T>> extends AbstractAstNode.AstNodeBuilder<T> {
        /** 表达式类型。 */
        protected ExpressionType expressionType;

        /**
         * 设置表达式类型。
         *
         * @param expressionType 表达式类型
         * @return 当前 builder
         */
        public T withExpressionType(ExpressionType expressionType){
            this.expressionType = expressionType;
            return self();
        }
    }

}
