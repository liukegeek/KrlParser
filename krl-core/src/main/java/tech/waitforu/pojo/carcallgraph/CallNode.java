package tech.waitforu.pojo.carcallgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: carcallgraph.pojo.tech.waitforu.CallNode
 * Package: tech.waitforu.pojo.carcallgraph
 * Description:
 * Author: LiuKe
 * Create: 2025/12/17 16:26
 * Version 1.0
 */
public class CallNode implements tech.waitforu.pojo.carcallgraph.CarReferenceNode {
    private String id; //结点唯一标识符（用于引用）
    private String value; // 节点值（cell、P11、SA3H622、SA3H_WELD等）
    private tech.waitforu.pojo.carcallgraph.NodeType nodeType; // 节点类型（机器人、cell程序、P程序、车型程序、轨迹程序等）
    private String relevantInfo; // 相关信息，比如这个结点所对应的程序内容。
    private final List<tech.waitforu.pojo.carcallgraph.CarReferenceNode> children; // 子节点（调用的其他函数、过程、程序等）
    private final Map<String, Object> propertyMap; // 可选：额外信息（路径、行号、文件名、注释…）

    // 构造函数
    public CallNode(String id, String value, tech.waitforu.pojo.carcallgraph.NodeType nodeType, String relevantInfo) {
        this.id = id;
        this.value = value;
        this.nodeType = nodeType;
        this.relevantInfo = relevantInfo;
        children = new ArrayList<>();
        propertyMap = new HashMap<>();
    }

    public void addChild(tech.waitforu.pojo.carcallgraph.CarReferenceNode child) {
        children.add(child);
    }

    public void addProperty(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("键不能为空");
        }
        if (propertyMap.containsKey(key)) {
            throw new IllegalArgumentException("键已存在");
        }
        propertyMap.put(key, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public tech.waitforu.pojo.carcallgraph.NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(tech.waitforu.pojo.carcallgraph.NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getRelevantInfo() {
        return relevantInfo;
    }

    public void setRelevantInfo(String relevantInfo) {
        this.relevantInfo = relevantInfo;
    }

    public List<tech.waitforu.pojo.carcallgraph.CarReferenceNode> getChildren() {
        return children;
    }

    public Map<String, Object> getPropertyMap() {
        return propertyMap;
    }

    public Object getProperty(String key) {
        if (key == null) {
            throw new IllegalArgumentException("键不能为空");
        }
        return propertyMap.get(key);
    }
}
