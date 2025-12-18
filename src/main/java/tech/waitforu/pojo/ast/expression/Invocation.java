package tech.waitforu.pojo.ast.expression;

import tech.waitforu.pojo.ast.programunit.ProgramUnit;
import tech.waitforu.pojo.ast.programunit.ProgramUnitType;

import java.util.List;

/**
 * ClassName: Invocation
 * Package: tech.waitforu.pojo.ast.expression
 * Description: 存放调用表达式的信息
 * Author: LiuKe
 * Create: 2025/12/15 11:33
 * Version 1.0
 */
public class Invocation extends AbstractExpression implements Expression {
    private  String targetName; //调用的目标名称
    private  ProgramUnitType targetType; //调用的目标类型
    private  List<String> argumentList; //调用的参数列表
    private ProgramUnit callTarget; //调用目标对象。

    private Invocation(InvocationBuilder b) {
        super(b);
        this.targetName = b.targetName;
        this.targetType = b.targetType;
        this.argumentList = b.argumentList;
        this.callTarget = b.callTarget;
    }

    public String getTargetName() {
        return targetName;
    }
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public ProgramUnitType getTargetType() {
        return targetType;
    }
    public void setTargetType(ProgramUnitType targetType) {
        this.targetType = targetType;
    }

    public List<String> getArgumentList() {
        return argumentList;
    }
    public void setArgumentList(List<String> argumentList) {
        this.argumentList = argumentList;
    }
    public boolean addArgument(String argument){
        return argumentList.add(argument);
    }

    public ProgramUnit getCallTarget() {
        return callTarget;
    }

     public void setCallTarget(ProgramUnit callTarget) {
        this.callTarget = callTarget;
    }

    public static InvocationBuilder builder(){
        return new InvocationBuilder();
    }

    public static class InvocationBuilder extends ExpressionBuilder<InvocationBuilder> {
        private String targetName;
        private ProgramUnitType targetType;
        private List<String> argumentList;
        private ProgramUnit callTarget;

        public InvocationBuilder withTargetName(String targetName){
            this.targetName = targetName;
            return self();
        }

        public InvocationBuilder withTargetType(ProgramUnitType targetType){
            this.targetType = targetType;
            return self();
        }

        public InvocationBuilder withArgumentList(List<String> argumentList){
            this.argumentList = argumentList;
            return self();
        }
        public InvocationBuilder withCallTarget(ProgramUnit callTarget){
            this.callTarget = callTarget;
            return self();
        }

        @Override
        protected InvocationBuilder self() {
            return this;
        }

        @Override
        public Invocation build() {
            return new Invocation(this);
        }
    }
}
