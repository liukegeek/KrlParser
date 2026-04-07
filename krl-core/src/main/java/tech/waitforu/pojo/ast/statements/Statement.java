package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.AstNode;

import java.util.List;

/**
 * 语句节点接口。
 * <p>
 * 定义语句类型与子语句管理能力。
 */
public interface Statement extends AstNode{
    List<Statement> statementList = List.of(); //子语句列表

    /**
     * 获取语句类型。
     *
     * @return 语句类型
     */
    StatementType getStatementType();

    /**
     * 获取子语句出现的类型集合。
     *
     * @return 子语句类型列表
     */
    List<StatementType> getChildStatementTypes();

    /**
     * 获取全部子语句。
     *
     * @return 子语句列表
     */
    List<Statement> getChildStatement();

    /**
     * 获取指定类型子语句。
     *
     * @param statementType 语句类型
     * @return 子语句列表
     */
    List<Statement> getChildStatement(StatementType statementType);

    /**
     * 根据类型和索引获取子语句。
     *
     * @param statementType 语句类型
     * @param index 类型内索引
     * @return 匹配子语句，不存在返回 null
     */
    Statement getChildStatement(StatementType statementType, int index);

    /**
     * 获取指定类型第一条子语句。
     *
     * @param statementType 语句类型
     * @return 第一条匹配子语句，不存在返回 null
     */
    Statement getChildStatementFirst(StatementType statementType);

}
