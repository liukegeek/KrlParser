package tech.waitforu.pojo.ast.expression;

import tech.waitforu.pojo.ast.AbstractAstNode;

/**
 * ClassName: expression.ast.pojo.tech.waitforu.AbstractExpression
 * Package: tech.waitforu.pojo.ast.expression
 * Description:
 * Author: LiuKe
 * Create: 2025/12/17 10:50
 * Version 1.0
 */
public abstract class AbstractExpression extends AbstractAstNode implements Expression {
    ExpressionType expressionType;

    @Override
    public ExpressionType getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(ExpressionType expressionType) {
        this.expressionType = expressionType;
    }

    protected AbstractExpression(ExpressionBuilder<?> builder){
        super(builder);
        this.expressionType = builder.expressionType;
    }

    public abstract static class ExpressionBuilder<T extends ExpressionBuilder<T>> extends AbstractAstNode.AstNodeBuilder<T> {
        protected ExpressionType expressionType;
        public T withExpressionType(ExpressionType expressionType){
            this.expressionType = expressionType;
            return self();
        }
    }

}
