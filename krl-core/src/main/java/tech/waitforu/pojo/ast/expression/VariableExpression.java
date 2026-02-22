package tech.waitforu.pojo.ast.expression;

/**
 * 变量表达式节点。
 */
public class VariableExpression extends tech.waitforu.pojo.ast.expression.AbstractExpression implements tech.waitforu.pojo.ast.expression.Expression {
    /** 变量名。 */
    private String variableName;

    /**
     * 通过 Builder 构建变量表达式。
     *
     * @param builder 构建器
     */
    private VariableExpression(VariableBuilder builder){
        super(builder);
        this.variableName = builder.variableName;
    }

    /**
     * 获取变量表达式 Builder。
     *
     * @return Builder
     */
    public static VariableBuilder builder(){
        return new VariableBuilder();
    }

    /**
     * 获取变量名。
     *
     * @return 变量名
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * 设置变量名。
     *
     * @param variableName 变量名
     */
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    /**
     * 变量表达式 Builder。
     */
    public static class VariableBuilder extends ExpressionBuilder<VariableBuilder> {
        /** 变量名。 */
        private String variableName;

        /**
         * 返回当前 builder。
         *
         * @return 当前 builder
         */
        @Override
        protected VariableBuilder self() {
            return this;
        }

        /**
         * 设置变量名。
         *
         * @param variableName 变量名
         * @return 当前 builder
         */
        public VariableBuilder withVariableName(String variableName) {
            this.variableName = variableName;
            return self();
        }

        /**
         * 构建变量表达式对象。
         *
         * @return VariableExpression
         */
        @Override
        public VariableExpression build() {
            return new VariableExpression(this);
        }
    }
}
