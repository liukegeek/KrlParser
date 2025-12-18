package tech.waitforu.pojo.ast.programunit;

import tech.waitforu.pojo.ast.AbstractAstNode;
import tech.waitforu.pojo.ast.declaration.Declaration;
import tech.waitforu.pojo.ast.statements.AbstractStatement;
import tech.waitforu.pojo.ast.statements.Statement;
import tech.waitforu.pojo.ast.statements.StatementType;
import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: AbstractProgramUnit
 * Package: tech.waitforu.pojo.ast.programunit
 * Description:
 * Author: LiuKe
 * Create: 2025/12/15 09:25
 * Version 1.0
 */
public abstract class AbstractProgramUnit extends AbstractAstNode implements ProgramUnit {
    String name;  //程序单元的名称
    ProgramUnitType type; //程序单元的类型
    boolean isGlobal = false;  //程序单元是否有global修饰符
    List<Declaration> declarationList = new ArrayList<>(); //程序单元的声明列表
    List<Statement> statementsList = new ArrayList<>();  //程序单元的语句列表

    public AbstractProgramUnit(int startIndex, int stopIndex, KrlFile krlFile) {
        super(startIndex, stopIndex, krlFile);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ProgramUnitType getType() {
        return type;
    }

    @Override
    public void setType(ProgramUnitType type) {
        this.type = type;
    }

    @Override
    public List<Declaration> getDeclarationList() {
        return declarationList;
    }

    @Override
    public Declaration getDeclaration(String variableName) {
        for (Declaration declaration : declarationList) {
            if (declaration.getVariableName().equals(variableName)) {
                return declaration;
            }
        }
        return null;
    }

    @Override
    public boolean addDeclaration(Declaration declaration) {
        declarationList.add(declaration);
        addChild(declaration);
        return true;
    }

    @Override
    public Declaration removeDeclaration(int index) {
        Declaration declaration = declarationList.remove(index);
        removeChild(declaration);
        return declaration;
    }


    @Override
    public boolean getIsGlobal() {
        return isGlobal;
    }

    @Override
    public void setIsGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    @Override
    public List<Statement> getStatementList() {
        return statementsList;
    }

    @Override
    public List<Statement> getStatementList(StatementType statementType) {
        List<Statement> statementList = new ArrayList<>();
        for (Statement statement : statementsList) {
            if (statement.getStatementType() == statementType) {
                statementList.add(statement);
            }
        }
        return statementList;
    }

    @Override
    public Statement getStatement(int index) {
        return statementsList.get(index);
    }

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

    @Override
    public Statement getStatementFirst() {
        if (statementsList.isEmpty()) {
            return null;
        }
        return statementsList.getFirst();
    }

    @Override
    public boolean hasStatement(StatementType statementType) {
        return !getStatementList(statementType).isEmpty();
    }

    @Override
    public boolean addStatement(Statement statement) {
        if (statementsList.add(statement)) {

            return addChild(statement);
        }
        return false;
    }

    @Override
    public Statement removeStatement(int index) {
        Statement statement = statementsList.remove(index);
        removeChild(statement);
        return statement;
    }

    public abstract static class ProgramUnitBuilder<T extends ProgramUnitBuilder<T>> extends AbstractAstNode.AstNodeBuilder<T> {
        protected String name;
        protected ProgramUnitType type;
        protected boolean isGlobal = false;
        protected List<Declaration> declarationList = new ArrayList<>();
        protected List<Statement> statementsList = new ArrayList<>();

        @Override
        protected abstract T self();

        public T withName(String name) {
            this.name = name;
            return self();
        }

        public T withType(ProgramUnitType type) {
            this.type = type;
            return self();
        }

        public T withIsGlobal(boolean isGlobal) {
            this.isGlobal = isGlobal;
            return self();
        }

        public T withDeclarationList(List<Declaration> declarationList) {
            this.declarationList = declarationList;
            return self();
        }

        public T withStatementsList(List<Statement> statementsList) {
            this.statementsList = statementsList;
            return self();
        }
    }
}
