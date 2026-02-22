package tech.waitforu.pojo.carcallgraph;

/**
 * 调用关系图节点类型枚举。
 */
public enum NodeType {
    CEll, //cell程序
    P_PROGRAM, //P程序
    CAR_CODE,//车型代码结点
    CAR_PROGRAM,//车型程序
    ROUTE_PROCESS, //轨迹程序
    VIRTUAL //虚拟节点，代表逻辑上的关系，但实际却并不存在。
}
