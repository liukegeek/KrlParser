package tech.waitforu.pojo.ast.expression;

/**
 * ClassName: expression.ast.pojo.tech.waitforu.ExpressionType
 * Package: tech.waitforu.pojo.ast.expression
 * Description:
 * Author: LiuKe
 * Create: 2025/12/15 10:06
 * Version 1.0
 */
public enum ExpressionType {
    //基本元素表达式
    PRIMARY,
    // 逻辑非表达式
    NOT,
    // 正负号表达式
    NEG_AND_POS,
    // 几何表达式
    GEOMETRY,
    // 乘除表达式
    MULTIPLY_DIVIDE,
    // 加减表达式
    PLUS_MINUS,
    // 与表达式
    AND,
    // 异或表达式
    EXOR,
    // 或表达式
    OR,
    // 关系表达式
    RELATION,
    // 赋值表达式
    ASSIGNMENT,
    // 括号表达式
    BRACKET,
    // 字面量表达式
    LITERAL,
    // 变量表达式
    VARIABLE,
    // 结构体成员表达式
    STRUCT_MEMBER,
    // 调用可调用表达式
    INVOKE_CALLABLE,
    // 默认表达式
    DEFAULT

}
