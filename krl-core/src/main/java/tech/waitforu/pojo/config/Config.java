package tech.waitforu.pojo.config;

/**
 * ClassName: config.pojo.tech.waitforu.Config
 * Package: tech.waitforu.pojo
 * Description: yaml中的配置信息对应的数据实体。
 * Author: LiuKe
 * Create: 2025/12/10 14:51
 * Version 1.0
 */
public class Config {
    private StrRuleConfig fileLoadSection;
    private StrRuleConfig carInvokerParseSection;
    private RobotInfoConfig robotInfoConfig;

    // 通过Jackson文件对config.yml进行解析时，依靠get、set方法进行属性的赋值和获取。

    public StrRuleConfig getFileLoadSection() {
        return fileLoadSection;
    }

    public void setFileLoadSection(StrRuleConfig fileLoadSection) {
        this.fileLoadSection = fileLoadSection;
    }

    public StrRuleConfig getCarInvokerParseSection() {
        return carInvokerParseSection;
    }

    public void setCarInvokerParseSection(StrRuleConfig carInvokerParseSection) {
        this.carInvokerParseSection = carInvokerParseSection;
    }

    public RobotInfoConfig getRobotInfoConfig() {
        return robotInfoConfig;
    }

    public void setRobotInfo(RobotInfoConfig robotInfoConfig) {
        this.robotInfoConfig = robotInfoConfig;
    }
}

