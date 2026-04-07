package tech.waitforu.pojo.ast.statements;

import java.util.ArrayList;
import java.util.List;

/**
 * SWITCH 语句中的 CASE 块节点。
 */
public class CaseBlock extends tech.waitforu.pojo.ast.statements.AbstractStatement implements tech.waitforu.pojo.ast.statements.Statement {
    /** case 标签列表；一个 case 可包含多个标签。 */
    List<String> caseLabel;
    List<Statement> bodyStatementList;


    /**
     * 通过 Builder 构建 case 块。
     *
     * @param builder Builder
     */
    private CaseBlock(CaseBuilder builder) {
        super(builder);
        if (builder.caseLabel == null) {
            caseLabel = new ArrayList<>();
        } else {
            caseLabel = builder.caseLabel;
        }
        bodyStatementList = new ArrayList<>();
        if (builder.bodyStatementList != null) {
            builder.bodyStatementList.forEach(this::addBodyStatement);
        }

    }

    /**
     * 获取 case 块 Builder。
     *
     * @return Builder
     */
    public static CaseBuilder builder() {
        return new CaseBuilder();
    }

    /**
     * case 块 Builder。
     */
    public static class CaseBuilder extends StatementBuilder<CaseBuilder> {
        /** case 标签列表。 */
        protected List<String> caseLabel;
        /** case 块体语句列表。 */
        protected List<Statement> bodyStatementList = new ArrayList<>();


        /**
         * 返回当前 builder。
         *
         * @return 当前 builder
         */
        @Override
        protected CaseBuilder self() {
            return this;
        }

        /**
         * 设置 case 标签列表。
         *
         * @param caseLabel case 标签列表
         * @return 当前 builder
         */
        public CaseBuilder withCaseLabel(List<String> caseLabel) {
            this.caseLabel = caseLabel;
            return self();
        }

        public CaseBuilder withBodyStatementList(List<Statement> bodyStatementList) {
            this.bodyStatementList = bodyStatementList;
            return self();
        }

        /**
         * 构建 case 块对象。
         *
         * @return CaseBlock
         */
        @Override
        public CaseBlock build() {
            return new CaseBlock(this);
        }
    }


    /**
     * 获取 case 标签列表。
     *
     * @return 标签列表拷贝
     */
    public List<String> getCaseLabel() {
        return new ArrayList<>(caseLabel);
    }

    /**
     * 获取指定索引 case 标签。
     *
     * @param index 索引
     * @return case 标签
     */
    public String getCaseLabel(int index) {
        return caseLabel.get(index);
    }

    /**
     * 获取第一个 case 标签。
     *
     * @return 第一个标签
     */
    public String getCaseLabelFirst() {
        return caseLabel.getFirst();
    }

    /**
     * 设置 case 标签列表。
     *
     * @param caseLabel 标签列表
     */
    public void setCaseLabel(List<String> caseLabel) {
        if (caseLabel == null) {
            this.caseLabel = new ArrayList<>();
        } else {
            this.caseLabel = new ArrayList<>(caseLabel);
        }
    }

    /**
     * 添加一个 case 标签。
     * <p>
     * 标签是该节点的属性，不作为 AST 子节点。
     *
     * @param caseLabel 标签文本
     * @return true 表示添加成功
     */
    public boolean addCaseLabel(String caseLabel) {
        return this.caseLabel.add(caseLabel);
    }

    /**
     * 获取 case 块体语句列表。
     *
     * @return 语句列表拷贝
     */
    public List<Statement> getBodyStatementList() {
        return List.copyOf(bodyStatementList);
    }

    /**
     * 添加 case 块体语句。
     * @param statement 语句
     * @return true 表示添加成功
     */
    public boolean addBodyStatement(Statement statement) {
        if (!addChildStatement(statement)) {
            return false;
        }
        return bodyStatementList.add(statement);
    }
}
