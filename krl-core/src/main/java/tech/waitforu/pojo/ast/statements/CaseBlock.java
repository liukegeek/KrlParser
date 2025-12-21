package tech.waitforu.pojo.ast.statements;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: statements.ast.pojo.tech.waitforu.CaseBlock
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 15:51
 * Version 1.0
 */
public class CaseBlock extends tech.waitforu.pojo.ast.statements.AbstractStatement implements tech.waitforu.pojo.ast.statements.Statement {
    List<String> caseLabel; // 一个case后面可能有多个标签，比如: case: 1, 2, 3


    private CaseBlock(CaseBuilder builder) {
        super(builder);
        caseLabel = builder.caseLabel;
    }

    public static CaseBuilder builder() {
        return new CaseBuilder();
    }

    public static class CaseBuilder extends StatementBuilder<CaseBuilder> {
        protected List<String> caseLabel; // 一个case后面可能有多个标签，比如: case: 1, 2, 3


        @Override
        protected CaseBuilder self() {
            return this;
        }

        public CaseBuilder withCaseLabel(List<String> caseLabel) {
            this.caseLabel = caseLabel;
            return self();
        }

        // 构建方法
        @Override
        public CaseBlock build() {
            return new CaseBlock(this);
        }
    }


    public List<String> getCaseLabel() {
        return new ArrayList<>(caseLabel);
    }

    public String getCaseLabel(int index) {
        return caseLabel.get(index);
    }

    public String getCaseLabelFirst() {
        return caseLabel.getFirst();
    }

    public void setCaseLabel(List<String> caseLabel) {
        this.caseLabel = new ArrayList<>(caseLabel);
    }

    // 添加一个case标签,label是结点的本身属性，不是子结点。
    public boolean addCaseLabel(String caseLabel) {
        return this.caseLabel.add(caseLabel);
    }
}
