package tech.waitforu.pojo.ast.declaration;

import tech.waitforu.pojo.ast.AstNode;

import java.util.List;

/**
 * ClassName: Declaration
 * Package: tech.waitforu.pojo.ast.declaration
 * Description:
 * Author: LiuKe
 * Create: 2025/12/15 10:03
 * Version 1.0
 */
public interface Declaration extends AstNode {
    String getVariableName();
    List<String> getModifierList();

}
