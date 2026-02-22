package tech.waitforu.pojo.ast.programunit;

import tech.waitforu.pojo.ast.AbstractAstNode;
import tech.waitforu.pojo.ast.declaration.Declaration;
import tech.waitforu.pojo.ast.statements.Statement;
import tech.waitforu.pojo.ast.statements.StatementType;
import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 程序单元抽象基类。
 * <p>
 * 封装程序单元共有字段：名称、类型、GLOBAL 标记、声明列表、语句列表。
 */
public abstract class AbstractProgramUnit extends AbstractAstNode implements ProgramUnit {
    /** 程序单元名称。 */
    String name;
    /** 程序单元类型。 */
    ProgramUnitType type;
    /** 是否带 GLOBAL 修饰符。 */
    boolean isGlobal = false;
    /** 声明列表。 */
    List<Declaration> declarationList = new ArrayList<>();
    /** 语句列表。 */
    List<Statement> statementsList = new ArrayList<>();

    /**
     * 通过显式参数构造程序单元。
     *
     * @param startIndex 起始索引
     * @param stopIndex 结束索引
     * @param krlFile 所属文件
     */
    public AbstractProgramUnit(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }

    /**
     * 获取程序单元名称。
     *
     * @return 名称
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 设置程序单元名称。
     *
     * @param name 名称
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取程序单元类型。
     *
     * @return 类型
     */
    @Override
    public ProgramUnitType getType() {
        return type;
    }

    /**
     * 设置程序单元类型。
     *
     * @param type 类型
     */
    @Override
    public void setType(ProgramUnitType type) {
        this.type = type;
    }

    /**
     * 获取声明列表。
     *
     * @return 声明列表
     */
    @Override
    public List<Declaration> getDeclarationList() {
        return declarationList;
    }

    /**
     * 根据变量名查找声明。
     *
     * @param variableName 变量名
     * @return 匹配声明；不存在返回 null
     */
    @Override
    public Declaration getDeclaration(String variableName) {
        for (Declaration declaration : declarationList) {
            if (declaration.getVariableName().equals(variableName)) {
                return declaration;
            }
        }
        return null;
    }

    /**
     * 添加声明并建立 AST 子节点关系。
     *
     * @param declaration 声明对象
     * @return true 表示添加成功
     */
    @Override
    public boolean addDeclaration(Declaration declaration) {
        declarationList.add(declaration);
        addChild(declaration);
        return true;
    }

    /**
     * 删除指定索引声明并断开 AST 子节点关系。
     *
     * @param index 声明索引
     * @return 删除的声明
     */
    @Override
    public Declaration removeDeclaration(int index) {
        Declaration declaration = declarationList.remove(index);
        removeChild(declaration);
        return declaration;
    }

    /**
     * 是否带 GLOBAL 修饰符。
     *
     * @return true 表示带 GLOBAL
     */
    @Override
    public boolean getIsGlobal() {
        return isGlobal;
    }

    /**
     * 设置 GLOBAL 修饰符标记。
     *
     * @param isGlobal GLOBAL 标记
     */
    @Override
    public void setIsGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    /**
     * 获取语句列表。
     *
     * @return 语句列表
     */
    @Override
    public List<Statement> getStatementList() {
        return statementsList;
    }

    /**
     * 获取指定类型语句列表。
     *
     * @param statementType 语句类型
     * @return 匹配语句列表
     */
    @Override
    public List<Statement> getStatementList(StatementType statementType) {
        List<Statement> statementList = new ArrayList<>();
        for (Statement statement : statementsList) {
            if (statement != null && statement.getStatementType() == statementType) {
                statementList.add(statement);
            }
        }
        return statementList;
    }

    /**
     * 根据索引获取语句。
     *
     * @param index 语句索引
     * @return 语句对象
     */
    @Override
    public Statement getStatement(int index) {
        return statementsList.get(index);
    }

    /**
     * 根据类型和索引获取语句。
     *
     * @param statementType 语句类型
     * @param index 类型内索引
     * @return 匹配语句；不存在返回 null
     */
    @Override
    public Statement getStatement(StatementType statementType, int index) {
        int count = 0;
        for (Statement statement : statementsList) {
            if (statement.getStatementType() == statementType) {
                if (count == index) {
                    return statement;
                }
                count++;
            }
        }
        return null;
    }

    /**
     * 获取指定类型第一条语句。
     *
     * @param statementType 语句类型
     * @return 第一条匹配语句；不存在返回 null
     */
    @Override
    public Statement getStatementFirst(StatementType statementType) {
        for (Statement statement : statementsList) {
            if (statement == null) {
                continue;
            }
            if (statement.getStatementType() == statementType) {
                return statement;
            }
        }
        return null;
    }

    /**
     * 获取第一条语句。
     *
     * @return 第一条语句；不存在返回 null
     */
    @Override
    public Statement getStatementFirst() {
        if (statementsList.isEmpty()) {
            return null;
        }
        return statementsList.getFirst();
    }

    /**
     * 判断是否包含指定类型语句。
     *
     * @param statementType 语句类型
     * @return true 表示存在
     */
    @Override
    public boolean hasStatement(StatementType statementType) {
        return !getStatementList(statementType).isEmpty();
    }

    /**
     * 添加语句并建立 AST 子节点关系。
     *
     * @param statement 语句对象
     * @return true 表示添加成功
     */
    @Override
    public boolean addStatement(Statement statement) {
        if (statementsList.add(statement)) {
            return addChild(statement);
        }
        return false;
    }

    /**
     * 删除指定索引语句并断开 AST 子节点关系。
     *
     * @param index 语句索引
     * @return 删除语句
     */
    @Override
    public Statement removeStatement(int index) {
        Statement statement = statementsList.remove(index);
        removeChild(statement);
        return statement;
    }

    /**
     * 程序单元 Builder 抽象基类。
     *
     * @param <T> Builder 自类型
     */
    public abstract static class ProgramUnitBuilder<T extends ProgramUnitBuilder<T>> extends AbstractAstNode.AstNodeBuilder<T> {
        /** 程序单元名称。 */
        protected String name;
        /** 程序单元类型。 */
        protected ProgramUnitType type;
        /** GLOBAL 标记。 */
        protected boolean isGlobal = false;
        /** 声明列表。 */
        protected List<Declaration> declarationList = new ArrayList<>();
        /** 语句列表。 */
        protected List<Statement> statementsList = new ArrayList<>();

        /**
         * 返回当前 builder（自引用模式）。
         *
         * @return 当前 builder
         */
        @Override
        protected abstract T self();

        /**
         * 设置程序单元名称。
         *
         * @param name 名称
         * @return 当前 builder
         */
        public T withName(String name) {
            this.name = name;
            return self();
        }

        /**
         * 设置程序单元类型。
         *
         * @param type 类型
         * @return 当前 builder
         */
        public T withType(ProgramUnitType type) {
            this.type = type;
            return self();
        }

        /**
         * 设置 GLOBAL 标记。
         *
         * @param isGlobal GLOBAL 标记
         * @return 当前 builder
         */
        public T withIsGlobal(boolean isGlobal) {
            this.isGlobal = isGlobal;
            return self();
        }

        /**
         * 设置声明列表。
         *
         * @param declarationList 声明列表
         * @return 当前 builder
         */
        public T withDeclarationList(List<Declaration> declarationList) {
            this.declarationList = declarationList;
            return self();
        }

        /**
         * 设置语句列表。
         *
         * @param statementsList 语句列表
         * @return 当前 builder
         */
        public T withStatementsList(List<Statement> statementsList) {
            this.statementsList = statementsList;
            return self();
        }
    }
}
