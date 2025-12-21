package tech.waitforu.pojo.carcallgraph;

/**
 * ClassName: carcallgraph.pojo.tech.waitforu.NodeType
 * Package: tech.waitforu.pojo.carcallgraph
 * Description: 
 * Author: LiuKe
 * Create: 2025/12/17 16:26 
 * Version 1.0   
*/
public enum NodeType {
    ROBOT,//机器人信息
    CEll, //cell程序
    CAR_CODE,//车型代码结点
    P_PROGRAM, //P程序
    CAR_PROGRAM,//车型程序
    ROUTE_PROCESS, //轨迹程序
    VIRTUAL //虚拟节点，代表逻辑上的关系，但实际却并不存在。
}
