package tech.waitforu.pojo.ast.programunit;

import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.declaration.Declaration;
import tech.waitforu.pojo.ast.statements.Statement;
import tech.waitforu.pojo.ast.statements.StatementType;

import java.util.List;

/**
 * ClassName: ProgramUnit
 * Package: tech.waitforu.pojo.ast.programunit
 * Description: 程序单元接口,包含数据块、函数块、程序块
 * Author: LiuKe
 * Create: 2025/12/15 09:10
 * Version 1.0
 */
public interface ProgramUnit extends AstNode {
    //获取程序单元的名称
    String getName();
    //设置程序单元的名称
    void setName(String name);


    //获取程序单元的类型
    ProgramUnitType getType();
    //设置程序单元的类型
    void setType(ProgramUnitType type);


    //获取程序单元的声明列表
    List<Declaration> getDeclarationList();
    //根据变量名称获取声明
    Declaration getDeclaration(String variableName);
    //添加声明
    boolean addDeclaration(Declaration declaration);
    //移除声明
    Declaration removeDeclaration(int index);

    //是否有global修饰符
    boolean getIsGlobal();
    //设置是否有global修饰符
    void setIsGlobal(boolean isGlobal);


    //获取程序单元的语句列表
    List<Statement> getStatementList();
    //获取指定类型的语句列表
    List<Statement> getStatementList(StatementType statementType);
    //根据索引获取语句
    Statement getStatement(int index);
    //根据类型和索引获取语句
    Statement getStatement(StatementType statementType, int index);
    //获取指定类型的第一条语句
    Statement getStatementFirst(StatementType statementType);
    //获取第一条语句
    Statement getStatementFirst();
    //是否包含指定类型的语句
    boolean hasStatement(StatementType statementType);
    //添加语句
    boolean addStatement(Statement statement);
    //移除指定索引的语句
    Statement removeStatement(int index);










}
