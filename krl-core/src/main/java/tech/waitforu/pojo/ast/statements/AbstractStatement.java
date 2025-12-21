package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.AbstractAstNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * ClassName: statements.ast.pojo.tech.waitforu.AbstractStatement
 * Package: tech.waitforu.pojo.ast.statements
 * Description:
 * Author: LiuKe
 * Create: 2025/12/15 15:01
 * Version 1.0
 */
public abstract class AbstractStatement extends AbstractAstNode implements Statement {
    //子语句列表
    protected List<Statement> childStatementList;
    //语句类型
    protected StatementType statementType;

    protected AbstractStatement(StatementBuilder<?> builder) {
        super(builder);
        this.statementType = builder.statementType;
        this.childStatementList = builder.childStatementList;
    }

    @Override
    public StatementType getStatementType() {
        return statementType;
    }

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

    @Override
    public List<Statement> getChildStatement() {
        return new ArrayList<>(childStatementList);
    }

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

    @Override
    public Statement getChildStatement(StatementType statementType, int index) {
        List<Statement> statements = getChildStatement(statementType);
        if (index >= 0 && index < statements.size()) {
            return statements.get(index);
        }
        return null;
    }

    @Override
    public Statement getChildStatementFirst(StatementType statementType) {
        return getChildStatement(statementType, 0);
    }

    @Override
    public boolean addChildStatement(Statement statement) {
        if (statement == null) {
            return false;
        }

        childStatementList.add(statement);
        addChild(statement);
        return true;

    }

    @Override
    public Statement removeChildStatement(int index) {
        if (index >= 0 && index < childStatementList.size()) {
            Statement statement = childStatementList.remove(index);
            removeChild(statement);
            return statement;
        }
        return null;
    }

    public abstract static class StatementBuilder<T extends StatementBuilder<T>> extends AbstractAstNode.AstNodeBuilder<T> {
        protected StatementType statementType;
        protected List<Statement> childStatementList;

        @Override
        protected abstract T self();

        public T withStatementType(StatementType statementType) {
            this.statementType = statementType;
            return self();
        }

        public T withChildStatementList(List<Statement> childStatementList) {
            this.childStatementList = childStatementList;
            return self();
        }
        //待后续扩展
    }

}
