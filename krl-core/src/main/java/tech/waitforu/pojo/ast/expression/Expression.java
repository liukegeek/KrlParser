package tech.waitforu.pojo.ast.expression;

import tech.waitforu.pojo.ast.AstNode;

/**
 * 表达式节点接口。
 */
public interface Expression extends AstNode {
    /**
     * 获取表达式类型。
     *
     * @return 表达式类型
     */
    ExpressionType getExpressionType();
}
