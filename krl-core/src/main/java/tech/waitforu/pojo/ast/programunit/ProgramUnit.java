package tech.waitforu.pojo.ast.programunit;

import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.declaration.Declaration;
import tech.waitforu.pojo.ast.statements.Statement;
import tech.waitforu.pojo.ast.statements.StatementType;

import java.util.List;

/**
 * 程序单元接口。
 * <p>
 * 统一抽象 data/function/procedure 三类节点的公共属性与行为。
 */
public interface ProgramUnit extends AstNode {
    /**
     * 获取程序单元名称。
     *
     * @return 程序单元名称
     */
    String getName();

    /**
     * 设置程序单元名称。
     *
     * @param name 程序单元名称
     */
    void setName(String name);

    /**
     * 获取程序单元类型。
     *
     * @return 程序单元类型
     */
    tech.waitforu.pojo.ast.programunit.ProgramUnitType getType();

    /**
     * 设置程序单元类型。
     *
     * @param type 程序单元类型
     */
    void setType(tech.waitforu.pojo.ast.programunit.ProgramUnitType type);

    /**
     * 获取声明列表。
     *
     * @return 声明列表
     */
    List<Declaration> getDeclarationList();

    /**
     * 根据变量名查找声明。
     *
     * @param variableName 变量名
     * @return 声明对象；不存在返回 null
     */
    Declaration getDeclaration(String variableName);

    /**
     * 添加声明。
     *
     * @param declaration 声明对象
     * @return true 表示添加成功
     */
    boolean addDeclaration(Declaration declaration);

    /**
     * 删除指定索引声明。
     *
     * @param index 索引
     * @return 删除的声明
     */
    Declaration removeDeclaration(int index);

    /**
     * 是否带 GLOBAL 修饰符。
     *
     * @return true 表示有 GLOBAL 修饰符
     */
    boolean getIsGlobal();

    /**
     * 设置 GLOBAL 修饰符标记。
     *
     * @param isGlobal GLOBAL 标记
     */
    void setIsGlobal(boolean isGlobal);

    /**
     * 获取全部语句列表。
     *
     * @return 语句列表
     */
    List<Statement> getStatementList();

    /**
     * 获取指定类型语句列表。
     *
     * @param statementType 语句类型
     * @return 匹配语句列表
     */
    List<Statement> getStatementList(StatementType statementType);

    /**
     * 根据索引获取语句。
     *
     * @param index 语句索引
     * @return 语句对象
     */
    Statement getStatement(int index);

    /**
     * 根据类型和索引获取语句。
     *
     * @param statementType 语句类型
     * @param index 类型内索引
     * @return 匹配语句；不存在返回 null
     */
    Statement getStatement(StatementType statementType, int index);

    /**
     * 获取指定类型第一条语句。
     *
     * @param statementType 语句类型
     * @return 第一条匹配语句；不存在返回 null
     */
    Statement getStatementFirst(StatementType statementType);

    /**
     * 获取第一条语句。
     *
     * @return 第一条语句；不存在返回 null
     */
    Statement getStatementFirst();

    /**
     * 判断是否包含指定类型语句。
     *
     * @param statementType 语句类型
     * @return true 表示存在
     */
    boolean hasStatement(StatementType statementType);

    /**
     * 添加语句。
     *
     * @param statement 语句对象
     * @return true 表示添加成功
     */
    boolean addStatement(Statement statement);

    /**
     * 删除指定索引语句。
     *
     * @param index 语句索引
     * @return 删除语句
     */
    Statement removeStatement(int index);
}
