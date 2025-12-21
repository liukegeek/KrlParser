package tech.waitforu.pojo.carcallgraph;

/**
 * ClassName: carcallgraph.pojo.tech.waitforu.CarCode
 * Package: tech.waitforu.pojo.carcallgraph
 * Description:
 * Author: LiuKe
 * Create: 2025/12/18 11:06
 * Version 1.0
 */
public class CarCode extends tech.waitforu.pojo.carcallgraph.CallNode {
    private final int majorIndexOfCar;
    private final int minorIndexOfCar;


    public CarCode(String id, tech.waitforu.pojo.carcallgraph.NodeType nodeType, int majorIndexOfCar, int minorIndexOfCar) {
        super(id, null, nodeType, null);
        this.majorIndexOfCar = majorIndexOfCar;
        this.minorIndexOfCar = minorIndexOfCar;
        int value = getIndexOfCar(majorIndexOfCar, minorIndexOfCar);
        setValue(String.valueOf(value)); // 完成车型计算，并绑定到节点值。
        setRelevantInfo("大车型:" + majorIndexOfCar + "小车型:" + minorIndexOfCar);
    }

    public int getMajorIndexOfCar() {
        return majorIndexOfCar;
    }

    public int getMinorIndexOfCar() {
        return minorIndexOfCar;
    }

    // 根据小车型和大车型，计算出完整的车型
    //例如majorIndexOfCar = 104,minorIndexOfCar =5,返回值为1054,即：(104-4)*10+5*10+4
    // majorIndexOfCar = 65,minorIndexOfCar = 3,返回值为635
    public int getIndexOfCar(int majorIndexOfCar, int minorIndexOfCar) {
        int a = majorIndexOfCar % 10;
        return (majorIndexOfCar - a) * 10 + minorIndexOfCar * 10 + a;
    }


}
