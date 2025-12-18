package tech.waitforu.pojo.ast.statements;

/**
 * ClassName: StatementType
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 16:03
 * Version 1.0
 */
public enum StatementType{
    IF_ELSE, // if-else语句
    FOR, // for循环语句
    WHILE,  // while循环语句
    LOOP, //LOOP循环语句
    REPEAT, // repeat循环语句
    GOTO, // goto语句
    SWITCH, // switch语句
    WAIT_FOR, // wait-for语句
    WAIT_SEC, // wait-sec语句
    EXPRESSION, // 表达式语句
    GOTO_LABEL, // goto标签语句
    MOVE, // move运动语句，包括PTP、LIN、CIRC、PTP_REL等语句
    INTERRUPT, // interrupt语句
    ASYNC, // async语句
    HALT, // halt语句
    TRIGGER, // trigger语句
    ANALOG_INPUT_STATEMENT, // 模拟输入语句
    ANALOG_OUTPUT_STATEMENT, // 模拟输出语句
    DEFAULT, // 默认语句
    CASE_BLOCK //case语句块。这里的case语句块指的是switch语句中的case语句块。实际上在krl语法规则中，语句不包含case块，这里为了代码简单融入了进来。
}
