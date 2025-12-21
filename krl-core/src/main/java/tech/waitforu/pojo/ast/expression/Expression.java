package tech.waitforu.pojo.ast.expression;

import tech.waitforu.pojo.ast.AstNode;

/**
 * ClassName: expression.ast.pojo.tech.waitforu.Expression
 * Package: tech.waitforu.pojo.ast.expression
 * Description:
 * Author: LiuKe
 * Create: 2025/12/15 09:51
 * Version 1.0
 */
public interface Expression extends AstNode {
    // 获取表达式类型
    tech.waitforu.pojo.ast.expression.ExpressionType getExpressionType();
}
