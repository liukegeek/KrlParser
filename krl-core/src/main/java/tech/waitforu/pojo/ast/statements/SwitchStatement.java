package tech.waitforu.pojo.ast.statements;

import tech.waitforu.pojo.ast.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * SWITCH 语句节点。
 */
public class SwitchStatement extends tech.waitforu.pojo.ast.statements.AbstractStatement implements Statement {
    /** 用于匹配比较的表达式，如 {@code SWITCH PGNO} 中的 {@code PGNO}。 */
    Expression switchExpression;

    // case 块列表。语法层 case 不单独成 statement，
    // 这里为便于处理将其包装成 Statement 子类型。
    List<CaseBlock> caseBlocks;
    /** default 分支语句列表。 */
    List<Statement> defaultStatementList;

    /**
     * 通过 Builder 构建 switch 语句。
     *
     * @param builder Builder
     */
    private SwitchStatement(SwitchBuilder builder) {
        super(builder);
        this.switchExpression = builder.switchExpression;
        this.addChild(switchExpression); //将switch表达式添加为子结点列表中

        this.caseBlocks = builder.caseBlocks;
        caseBlocks.forEach(this::addChild); //将case块添加为子结点列表中
        this.defaultStatementList = builder.defaultstatementList;
        defaultStatementList.forEach(this::addChild); //将default语句添加为子结点列表中
    }

    /**
     * 获取 switch 语句 Builder。
     *
     * @return Builder
     */
    public static SwitchBuilder builder() {
        return new SwitchBuilder();
    }

    /**
     * switch 语句 Builder。
     */
    public static class SwitchBuilder extends StatementBuilder<SwitchBuilder> {
        /** switch 匹配表达式。 */
        private Expression switchExpression;
        /** case 块列表。 */
        private List<CaseBlock> caseBlocks = new ArrayList<>();
        /** default 语句列表。 */
        private List<Statement> defaultstatementList = new ArrayList<>();

        /**
         * 返回当前 builder。
         *
         * @return 当前 builder
         */
        protected SwitchBuilder self() {
            return this;
        }

        /**
         * 设置 switch 匹配表达式。
         *
         * @param switchExpression 匹配表达式
         * @return 当前 builder
         */
        public SwitchBuilder withSwitchExpression(Expression switchExpression) {
            this.switchExpression = switchExpression;
            return self();
        }

        /**
         * 设置 case 块列表。
         *
         * @param caseBlocks case 块列表
         * @return 当前 builder
         */
        public SwitchBuilder withCaseBlocks(List<CaseBlock> caseBlocks) {
            this.caseBlocks = caseBlocks;
            return self();
        }

        /**
         * 设置 default 语句列表。
         *
         * @param defaultStatementList default 语句列表
         * @return 当前 builder
         */
        public SwitchBuilder withDefaultStatements(List<Statement> defaultStatementList) {
            this.defaultstatementList = defaultStatementList;
            return self();
        }

        /**
         * 构建 switch 语句对象。
         *
         * @return SwitchStatement
         */
        @Override
        public SwitchStatement build() {
            return new SwitchStatement(this);
        }
    }

    /**
     * 获取 switch 匹配表达式。
     *
     * @return 匹配表达式
     */
    public Expression getSwitchExpression() {
        return switchExpression;
    }

    /**
     * 获取 case 块列表。
     *
     * @return case 块列表
     */
    public List<CaseBlock> getCaseBlocks() {
        return caseBlocks;
    }

    /**
     * 添加 case 块并建立 AST 子节点关系。
     *
     * @param caseBlock case 块
     * @return true 表示添加成功
     */
    public boolean addCaseBlock(CaseBlock caseBlock) {
        addChild(caseBlock);
        return caseBlocks.add(caseBlock);
    }

    /**
     * 删除指定索引 case 块并断开 AST 子节点关系。
     *
     * @param index case 块索引
     * @return 删除的 case 块
     */
    public CaseBlock removeCaseBlock(int index) {
        removeChild(caseBlocks.get(index));
        return caseBlocks.remove(index);
    }

    /**
     * 获取 default 分支语句列表。
     *
     * @return default 语句列表
     */
    public List<Statement> getDefaultStatementList() {
        return defaultStatementList;
    }

    /**
     * 添加 default 分支语句并建立 AST 子节点关系。
     *
     * @param statement default 语句
     * @return true 表示添加成功
     */
     public boolean addDefaultStatement(Statement statement) {
        addChild(statement);
        return defaultStatementList.add(statement);
    }

     /**
      * 删除指定索引 default 语句并断开 AST 子节点关系。
      *
      * @param index default 语句索引
      * @return 删除的语句
      */
    public Statement removeDefaultStatement(int index) {
        //这里的移除需要验证是否可行。
        removeChild(defaultStatementList.get(index));
        return defaultStatementList.remove(index);
    }


}
