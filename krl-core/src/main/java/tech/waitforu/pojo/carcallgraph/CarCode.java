package tech.waitforu.pojo.carcallgraph;

/**
 * 车型代码节点。
 * <p>
 * 基于主车型索引与次车型索引计算最终车型编码值。
 */
public class CarCode extends CallNode {
    /** 主车型索引（来自 cell/P 程序 case 标签）。 */
    private final int majorIndexOfCar;
    /** 次车型索引（来自 P 程序 case 标签）。 */
    private final int minorIndexOfCar;


    /**
     * 构建车型代码节点并计算编码值。
     *
     * @param id 节点 ID
     * @param nodeType 节点类型
     * @param majorIndexOfCar 主车型索引
     * @param minorIndexOfCar 次车型索引
     */
    public CarCode(String id, tech.waitforu.pojo.carcallgraph.NodeType nodeType, int majorIndexOfCar, int minorIndexOfCar) {
        super(id, null, nodeType, null);
        this.majorIndexOfCar = majorIndexOfCar;
        this.minorIndexOfCar = minorIndexOfCar;
        int value = getIndexOfCar(majorIndexOfCar, minorIndexOfCar);
        setValue(String.valueOf(value)); // 完成车型计算，并绑定到节点值。
        setRelevantInfo("大车型:" + majorIndexOfCar + "小车型:" + minorIndexOfCar);
    }

    /**
     * 获取主车型索引。
     *
     * @return 主车型索引
     */
    public int getMajorIndexOfCar() {
        return majorIndexOfCar;
    }

    /**
     * 获取次车型索引。
     *
     * @return 次车型索引
     */
    public int getMinorIndexOfCar() {
        return minorIndexOfCar;
    }

    /**
     * 根据主/次车型索引计算完整车型编码。
     * <p>
     * 示例：
     * 1. major=104, minor=5 -> 1054；
     * 2. major=65, minor=3 -> 635。
     *
     * @param majorIndexOfCar 主车型索引
     * @param minorIndexOfCar 次车型索引
     * @return 计算后的车型编码
     */
    public int getIndexOfCar(int majorIndexOfCar, int minorIndexOfCar) {
        int a = majorIndexOfCar % 10;
        return (majorIndexOfCar - a) * 10 + minorIndexOfCar * 10 + a;
    }


}
