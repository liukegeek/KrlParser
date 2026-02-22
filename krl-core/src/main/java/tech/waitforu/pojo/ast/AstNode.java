package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

import java.util.List;

/**
 * 抽象语法树节点接口。
 * <p>
 * 统一定义 AST 节点的位置信息、父子关系与遍历能力。
 */
public interface AstNode{

    /**
     * 获取节点在源文件中的原始文本内容。
     *
     * @return 原始文本
     */
    String getTextContent();

    /**
     * 获取起始字符索引（含）。
     *
     * @return 起始索引
     */
    int getStartIndex();

    /**
     * 获取结束字符索引（含）。
     *
     * @return 结束索引
     */
    int getStopIndex();

    /**
     * 设置起始字符索引。
     *
     * @param startIndex 起始索引
     */
    void setStartIndex(int startIndex);

    /**
     * 设置结束字符索引。
     *
     * @param stopIndex 结束索引
     */
    void setStopIndex(int stopIndex);

    /**
     * 获取所属 KRL 文件。
     *
     * @return 文件对象
     */
    KrlFile getKrlFile();

    /**
     * 设置所属 KRL 文件。
     *
     * @param krlFile 文件对象
     */
    void setKrlFile(KrlFile krlFile);

    /**
     * 获取父节点。
     *
     * @return 父节点
     */
    AstNode getParent();

    /**
     * 设置父节点。
     *
     * @param parent 父节点
     */
    void setParent(AstNode parent);

    /**
     * 获取子节点列表（拷贝视图）。
     *
     * @return 子节点列表
     */
    List<AstNode> getChildren();

    /**
     * 获取指定索引子节点。
     *
     * @param index 子节点索引
     * @return 子节点
     */
    AstNode getChild(int index);

    /**
     * 添加子节点。
     *
     * @param child 子节点
     * @return true 表示添加成功
     */
    boolean addChild(AstNode child);

    /**
     * 移除指定子节点。
     *
     * @param child 子节点
     * @return 被移除节点；不存在返回 null
     */
    AstNode removeChild(AstNode child);

    /**
     * 沿父链回溯到根节点。
     *
     * @return 根节点
     */
    AstNode findRootNode();


    /**
     * 递归查找指定类型节点。
     *
     * @param clazz 目标类型
     * @return 匹配节点列表
     * @param <T> 目标节点类型
     */
    <T extends AstNode> List<T> findNodesByType(Class<T> clazz);
}
