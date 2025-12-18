package tech.waitforu.pojo.ast.expression;

import tech.waitforu.pojo.ast.AbstractAstNode;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.statements.Statement;

/**
 * ClassName: Expression
 * Package: tech.waitforu.pojo.ast.expression
 * Description:
 * Author: LiuKe
 * Create: 2025/12/15 09:51
 * Version 1.0
 */
public interface Expression extends AstNode {
    // 获取表达式类型
    ExpressionType getExpressionType();
}
