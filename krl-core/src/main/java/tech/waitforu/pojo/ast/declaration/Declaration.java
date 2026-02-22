package tech.waitforu.pojo.ast.declaration;

import tech.waitforu.pojo.ast.AstNode;

import java.util.List;

/**
 * 声明节点接口。
 */
public interface Declaration extends AstNode {
    /**
     * 获取声明变量名。
     *
     * @return 变量名
     */
    String getVariableName();

    /**
     * 获取声明修饰符列表。
     *
     * @return 修饰符列表
     */
    List<String> getModifierList();

}
