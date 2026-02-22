package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.AbstractAstNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 语句抽象基类。
 * <p>
 * 提供语句类型、子语句列表与常见 CRUD 操作。
 */
public abstract class AbstractStatement extends AbstractAstNode implements Statement {
    /** 子语句列表。 */
    protected List<Statement> childStatementList;
    /** 语句类型。 */
    protected StatementType statementType;

    /**
     * 通过 Builder 构建语句基类字段。
     *
     * @param builder 语句构建器
     */
    protected AbstractStatement(StatementBuilder<?> builder) {
        super(builder);
        this.statementType = builder.statementType;
        this.childStatementList = builder.childStatementList;
    }

    /**
     * 获取语句类型。
     *
     * @return 语句类型
     */
    @Override
    public StatementType getStatementType() {
        return statementType;
    }

    /**
     * 获取子语句类型集合。
     *
     * @return 子语句类型列表（去重）
     */
    @Override
    public List<StatementType> getChildStatementTypes() {
        HashSet<StatementType> typeSets = new HashSet<>();
        for (Statement statement : childStatementList) {
            if (statement == null) {
                continue;
            }
            typeSets.add(statement.getStatementType());
        }
        return new ArrayList<>(typeSets);
    }

    /**
     * 获取全部子语句。
     *
     * @return 子语句列表拷贝
     */
    @Override
    public List<Statement> getChildStatement() {
        return new ArrayList<>(childStatementList);
    }

    /**
     * 获取指定类型的子语句列表。
     *
     * @param statementType 语句类型
     * @return 匹配子语句列表
     */
    @Override
    public List<Statement> getChildStatement(StatementType statementType) {
        List<Statement> statements = new ArrayList<>();
        for (Statement statement : childStatementList) {
            if (statement == null) {
                continue;
            }
            if (statement.getStatementType() == statementType) {
                statements.add(statement);
            }
        }
        return statements;
    }

    /**
     * 获取指定类型和索引的子语句。
     *
     * @param statementType 语句类型
     * @param index 索引
     * @return 子语句；越界返回 null
     */
    @Override
    public Statement getChildStatement(StatementType statementType, int index) {
        List<Statement> statements = getChildStatement(statementType);
        if (index >= 0 && index < statements.size()) {
            return statements.get(index);
        }
        return null;
    }

    /**
     * 获取指定类型第一条子语句。
     *
     * @param statementType 语句类型
     * @return 第一条匹配子语句；不存在返回 null
     */
    @Override
    public Statement getChildStatementFirst(StatementType statementType) {
        return getChildStatement(statementType, 0);
    }

    /**
     * 添加子语句并建立 AST 子节点关系。
     *
     * @param statement 子语句
     * @return true 表示添加成功
     */
    @Override
    public boolean addChildStatement(Statement statement) {
        if (statement == null) {
            return false;
        }

        childStatementList.add(statement);
        addChild(statement);
        return true;

    }

    /**
     * 删除指定索引子语句并断开 AST 子节点关系。
     *
     * @param index 子语句索引
     * @return 删除的语句；越界返回 null
     */
    @Override
    public Statement removeChildStatement(int index) {
        if (index >= 0 && index < childStatementList.size()) {
            Statement statement = childStatementList.remove(index);
            removeChild(statement);
            return statement;
        }
        return null;
    }

    /**
     * 语句 Builder 抽象基类。
     *
     * @param <T> Builder 自类型
     */
    public abstract static class StatementBuilder<T extends StatementBuilder<T>> extends AbstractAstNode.AstNodeBuilder<T> {
        /** 语句类型。 */
        protected StatementType statementType;
        /** 子语句列表。 */
        protected List<Statement> childStatementList;

        /**
         * 返回当前 builder（自引用模式）。
         *
         * @return 当前 builder
         */
        @Override
        protected abstract T self();

        /**
         * 设置语句类型。
         *
         * @param statementType 语句类型
         * @return 当前 builder
         */
        public T withStatementType(StatementType statementType) {
            this.statementType = statementType;
            return self();
        }

        /**
         * 设置子语句列表。
         *
         * @param childStatementList 子语句列表
         * @return 当前 builder
         */
        public T withChildStatementList(List<Statement> childStatementList) {
            this.childStatementList = childStatementList;
            return self();
        }
        //待后续扩展
    }

}
