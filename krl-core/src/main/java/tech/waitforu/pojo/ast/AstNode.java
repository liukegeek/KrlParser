package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

import java.util.List;

/**
 * ClassName: ast.pojo.tech.waitforu.AstNode
 * Package: tech.waitforu.pojo.ast
 * Description: 抽象语法树节点接口
 * Author: LiuKe
 * Create: 2025/12/11 17:28
 * Version 1.0
 */
public interface AstNode{

    String getTextContent();
    int getStartIndex();
    int getStopIndex();
    void setStartIndex(int startIndex);
    void setStopIndex(int stopIndex);
    KrlFile getKrlFile();
    void setKrlFile(KrlFile krlFile);

    AstNode getParent();
    void setParent(AstNode parent);

    List<AstNode> getChildren();
    AstNode getChild(int index);
    boolean addChild(AstNode child);
    AstNode removeChild(AstNode child);
    AstNode findRootNode();


    <T extends AstNode> List<T> findNodesByType(Class<T> clazz);









}
