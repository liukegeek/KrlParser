package tech.waitforu.pojo.ast.expression;

/**
 * ClassName: expression.ast.pojo.tech.waitforu.VariableExpression
 * Package: tech.waitforu.pojo.ast.expression
 * Description:
 * Author: LiuKe
 * Create: 2025/12/21 13:22
 * Version 1.0
 */
public class VariableExpression extends tech.waitforu.pojo.ast.expression.AbstractExpression implements tech.waitforu.pojo.ast.expression.Expression {
    private String variableName; //变量名

    private VariableExpression(VariableBuilder builder){
        super(builder);
        this.variableName = builder.variableName;
    }

    public static VariableBuilder builder(){
        return new VariableBuilder();
    }

    public String getVariableName() {
        return variableName;
    }
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public static class VariableBuilder extends ExpressionBuilder<VariableBuilder> {
        private String variableName; //变量名

        @Override
        protected VariableBuilder self() {
            return this;
        }

        public VariableBuilder withVariableName(String variableName) {
            this.variableName = variableName;
            return self();
        }

        @Override
        public VariableExpression build() {
            return new VariableExpression(this);
        }
    }
}
