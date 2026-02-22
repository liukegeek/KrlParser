package tech.waitforu.pojo.ast.expression;

import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.ast.programunit.ProgramUnitType;

import java.util.List;

/**
 * 调用表达式节点。
 * <p>
 * 描述一次 callable 调用，包括目标名称、参数列表、目标类型与解析后的目标对象。
 */
public class Invocation extends tech.waitforu.pojo.ast.expression.AbstractExpression implements Expression {
    /** 调用目标名称。 */
    private  String targetName;
    /** 目标程序单元类型。 */
    private  ProgramUnitType targetType;
    /** 调用参数列表。 */
    private  List<String> argumentList;
    /** 解析后的调用目标对象。 */
    private ProgramUnit callTarget;

    /**
     * 通过 Builder 构建调用表达式。
     *
     * @param b 构建器
     */
    private Invocation(InvocationBuilder b) {
        super(b);
        this.targetName = b.targetName;
        this.targetType = b.targetType;
        this.argumentList = b.argumentList;
        this.callTarget = b.callTarget;
    }

    /**
     * 获取调用目标名称。
     *
     * @return 目标名称
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * 设置调用目标名称。
     *
     * @param targetName 目标名称
     */
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    /**
     * 获取目标类型。
     *
     * @return 目标类型
     */
    public ProgramUnitType getTargetType() {
        return targetType;
    }

    /**
     * 设置目标类型。
     *
     * @param targetType 目标类型
     */
    public void setTargetType(ProgramUnitType targetType) {
        this.targetType = targetType;
    }

    /**
     * 获取参数列表。
     *
     * @return 参数列表
     */
    public List<String> getArgumentList() {
        return argumentList;
    }

    /**
     * 设置参数列表。
     *
     * @param argumentList 参数列表
     */
    public void setArgumentList(List<String> argumentList) {
        this.argumentList = argumentList;
    }

    /**
     * 增加一个调用参数。
     *
     * @param argument 参数文本
     * @return true 表示添加成功
     */
    public boolean addArgument(String argument){
        return argumentList.add(argument);
    }

    /**
     * 获取调用目标对象。
     *
     * @return 目标程序单元
     */
    public ProgramUnit getCallTarget() {
        return callTarget;
    }

    /**
     * 设置调用目标对象。
     *
     * @param callTarget 目标程序单元
     */
     public void setCallTarget(ProgramUnit callTarget) {
        this.callTarget = callTarget;
    }

    /**
     * 获取 Invocation Builder。
     *
     * @return Builder
     */
    public static InvocationBuilder builder(){
        return new InvocationBuilder();
    }

    /**
     * 调用表达式 Builder。
     */
    public static class InvocationBuilder extends ExpressionBuilder<InvocationBuilder> {
        /** 调用目标名称。 */
        private String targetName;
        /** 调用目标类型。 */
        private ProgramUnitType targetType;
        /** 参数列表。 */
        private List<String> argumentList;
        /** 调用目标对象。 */
        private ProgramUnit callTarget;

        /**
         * 设置目标名称。
         *
         * @param targetName 目标名称
         * @return 当前 builder
         */
        public InvocationBuilder withTargetName(String targetName){
            this.targetName = targetName;
            return self();
        }

        /**
         * 设置目标类型。
         *
         * @param targetType 目标类型
         * @return 当前 builder
         */
        public InvocationBuilder withTargetType(ProgramUnitType targetType){
            this.targetType = targetType;
            return self();
        }

        /**
         * 设置参数列表。
         *
         * @param argumentList 参数列表
         * @return 当前 builder
         */
        public InvocationBuilder withArgumentList(List<String> argumentList){
            this.argumentList = argumentList;
            return self();
        }

        /**
         * 设置目标程序单元对象。
         *
         * @param callTarget 目标程序单元
         * @return 当前 builder
         */
        public InvocationBuilder withCallTarget(ProgramUnit callTarget){
            this.callTarget = callTarget;
            return self();
        }

        /**
         * 返回当前 builder。
         *
         * @return 当前 builder
         */
        @Override
        protected InvocationBuilder self() {
            return this;
        }

        /**
         * 构建 Invocation 对象。
         *
         * @return Invocation
         */
        @Override
        public Invocation build() {
            return new Invocation(this);
        }
    }
}
