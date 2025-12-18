package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.expression.Expression;
import tech.waitforu.pojo.ast.expression.ExpressionType;

import java.util.List;

/**
 * ClassName: Statement
 * Package: tech.waitforu.pojo.ast
 * Description: 语句接口，定义了语句的基本属性和方法
 * Author: LiuKe
 * Create: 2025/12/12 15:08
 * Version 1.0
 */
public interface Statement extends AstNode{
    List<Statement> statementList = List.of(); //子语句列表

    // 获取语句类型
    StatementType getStatementType();

    // 获取所有子语句所使用的类型
    List<StatementType> getChildStatementTypes();

    // 获取所有子语句
    List<Statement> getChildStatement();

    // 获取指定类型的子语句
    List<Statement> getChildStatement(StatementType statementType);

    // 根据类型和索引获取子语句
    Statement getChildStatement(StatementType statementType, int index);

    // 获取指定类型的第一条子语句
    Statement getChildStatementFirst(StatementType statementType);

    // 添加子语句
    boolean addChildStatement(Statement statement);

    // 删除子语句
    Statement removeChildStatement(int index);
}
