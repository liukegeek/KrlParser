package tech.waitforu.pojo.carcallgraph;

/**
 * 调用关系图节点标记接口。
 * <p>
 * 作为统一父类型，便于同一 children 列表中同时容纳：
 * - 通用调用节点 {@link CallNode}
 * - 扩展节点（如 {@link CarCode}）
 */
public interface CarReferenceNode {
}
