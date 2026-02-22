package tech.waitforu.pojo.carcallgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用关系图通用节点。
 * <p>
 * 描述一个可视化节点与其子调用节点关系，并支持挂载扩展属性。
 */
public class CallNode implements CarReferenceNode {
    private String id; //结点唯一标识符（用于引用）
    private String value; // 节点值（cell、P11、SA3H622、SA3H_WELD等）
    private NodeType nodeType; // 节点类型（机器人、cell程序、P程序、车型程序、轨迹程序等）
    private String relevantInfo; // 相关信息，比如这个结点所对应的程序内容。
    private final List<CarReferenceNode> children; // 子节点（调用的其他函数、过程、程序等）
    private final Map<String, Object> propertyMap; // 可选：额外信息（路径、行号、文件名、注释…）

    /**
     * 创建调用节点。
     *
     * @param id 节点唯一标识
     * @param value 节点显示值
     * @param nodeType 节点类型
     * @param relevantInfo 节点关联文本
     */
    public CallNode(String id, String value, NodeType nodeType, String relevantInfo) {
        this.id = id;
        this.value = value;
        this.nodeType = nodeType;
        this.relevantInfo = relevantInfo;
        children = new ArrayList<>();
        propertyMap = new HashMap<>();
    }

    /**
     * 添加子节点。
     *
     * @param child 子节点
     */
    public void addChild(CarReferenceNode child) {
        children.add(child);
    }

    /**
     * 添加属性项（键不可重复）。
     *
     * @param key 属性键
     * @param value 属性值
     */
    public void addProperty(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("键不能为空");
        }
        if (propertyMap.containsKey(key)) {
            throw new IllegalArgumentException("键已存在");
        }
        propertyMap.put(key, value);
    }

    /**
     * 获取节点 ID。
     *
     * @return 节点 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置节点 ID。
     *
     * @param id 节点 ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取节点显示值。
     *
     * @return 显示值
     */
    public String getValue() {
        return value;
    }

    /**
     * 设置节点显示值。
     *
     * @param value 显示值
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * 获取节点类型。
     *
     * @return 节点类型
     */
    public NodeType getNodeType() {
        return nodeType;
    }

    /**
     * 设置节点类型。
     *
     * @param nodeType 节点类型
     */
    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * 获取节点关联信息。
     *
     * @return 关联信息文本
     */
    public String getRelevantInfo() {
        return relevantInfo;
    }

    /**
     * 设置节点关联信息。
     *
     * @param relevantInfo 关联信息文本
     */
    public void setRelevantInfo(String relevantInfo) {
        this.relevantInfo = relevantInfo;
    }

    /**
     * 获取子节点列表。
     *
     * @return 子节点列表
     */
    public List<CarReferenceNode> getChildren() {
        return children;
    }

    /**
     * 获取属性映射表。
     *
     * @return 属性映射
     */
    public Map<String, Object> getPropertyMap() {
        return propertyMap;
    }

    /**
     * 根据键读取属性值。
     *
     * @param key 属性键
     * @return 属性值，不存在时返回 null
     */
    public Object getProperty(String key) {
        if (key == null) {
            throw new IllegalArgumentException("键不能为空");
        }
        return propertyMap.get(key);
    }
}
