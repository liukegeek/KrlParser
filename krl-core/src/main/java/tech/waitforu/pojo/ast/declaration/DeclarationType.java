package tech.waitforu.pojo.ast.declaration;

/**
 * ClassName: declaration.ast.pojo.tech.waitforu.DeclarationType
 * Package: tech.waitforu.pojo.ast.declaration
 * Description:
 * Author: LiuKe
 * Create: 2025/12/15 10:04
 * Version 1.0
 */
public enum DeclarationType {
    Forward, //前向声明
    Enum, //枚举声明
    Struct, //结构体声明
    Variable, //变量声明
    Expression, //表达式赋值
    IMPORT //导入声明，古老的语法(KSS4.x/5.2中存在过)。在8.x版本中已被弃用。现使用GLOBAL关键字彻底改革了数据共享机制。
}
