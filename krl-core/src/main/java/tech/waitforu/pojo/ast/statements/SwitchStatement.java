package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: statements.ast.pojo.tech.waitforu.SwitchStatement
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 15:08
 * Version 1.0
 */
public class SwitchStatement extends tech.waitforu.pojo.ast.statements.AbstractStatement implements Statement {
    Expression switchExpression; //用于匹配比较的表达式,比如"Switch PGNO",其中PGNO即为这里的switchExpression。

    //case块的列表,注意krl.g4的语法规则中，由于case块不单独存在，故而未将case块设置为statement的一种语法规则。
    // Java代码中，为了方便，将case块设定为Statement的实现类了！
    List<CaseBlock> caseBlocks;
    List<Statement> defaultStatementList;

    private SwitchStatement(SwitchBuilder builder) {
        super(builder);
        this.switchExpression = builder.switchExpression;
        this.addChild(switchExpression); //将switch表达式添加为子结点列表中

        this.caseBlocks = builder.caseBlocks;
        caseBlocks.forEach(this::addChild); //将case块添加为子结点列表中
        this.defaultStatementList = builder.defaultstatementList;
        defaultStatementList.forEach(this::addChild); //将default语句添加为子结点列表中
    }

    public static SwitchBuilder builder() {
        return new SwitchBuilder();
    }

    public static class SwitchBuilder extends StatementBuilder<SwitchBuilder> {
        private Expression switchExpression;
        private List<CaseBlock> caseBlocks = new ArrayList<>();
        private List<Statement> defaultstatementList = new ArrayList<>();

        protected SwitchBuilder self() {
            return this;
        }

        public SwitchBuilder withSwitchExpression(Expression switchExpression) {
            this.switchExpression = switchExpression;
            return self();
        }

        public SwitchBuilder withCaseBlocks(List<CaseBlock> caseBlocks) {
            this.caseBlocks = caseBlocks;
            return self();
        }

        public SwitchBuilder withDefaultStatements(List<Statement> defaultStatementList) {
            this.defaultstatementList = defaultStatementList;
            return self();
        }

        @Override
        public SwitchStatement build() {
            return new SwitchStatement(this);
        }
    }

    public Expression getSwitchExpression() {
        return switchExpression;
    }

    public List<CaseBlock> getCaseBlocks() {
        return caseBlocks;
    }

    public boolean addCaseBlock(CaseBlock caseBlock) {
        addChild(caseBlock);
        return caseBlocks.add(caseBlock);
    }

    //删除指定索引的case块
    public CaseBlock removeCaseBlock(int index) {
        removeChild(caseBlocks.get(index));
        return caseBlocks.remove(index);
    }

    public List<Statement> getDefaultStatementList() {
        return defaultStatementList;
    }
     public boolean addDefaultStatement(Statement statement) {
        addChild(statement);
        return defaultStatementList.add(statement);
    }

     //删除指定索引的default语句
    public Statement removeDefaultStatement(int index) {
        //这里的移除需要验证是否可行。
        removeChild(defaultStatementList.get(index));
        return defaultStatementList.remove(index);
    }


}
