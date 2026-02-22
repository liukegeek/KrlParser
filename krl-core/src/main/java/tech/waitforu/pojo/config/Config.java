package tech.waitforu.pojo.config;

/**
 * 顶层配置模型，对应 YAML 根节点。
 */
public class Config {
    /** 文件加载过滤配置。 */
    private StrRuleConfig fileLoadSection;
    /** 调用解析过滤配置。 */
    private StrRuleConfig carInvokerParseSection;
    /** 机器人信息文件路径配置。 */
    private RobotInfoConfig robotInfoConfig;

    // 通过Jackson文件对config.yml进行解析时，依靠get、set方法进行属性的赋值和获取。

    /**
     * 获取文件加载过滤配置。
     *
     * @return 文件加载规则
     */
    public StrRuleConfig getFileLoadSection() {
        return fileLoadSection;
    }

    /**
     * 设置文件加载过滤配置。
     *
     * @param fileLoadSection 文件加载规则
     */
    public void setFileLoadSection(StrRuleConfig fileLoadSection) {
        this.fileLoadSection = fileLoadSection;
    }

    /**
     * 获取调用解析过滤配置。
     *
     * @return 调用解析规则
     */
    public StrRuleConfig getCarInvokerParseSection() {
        return carInvokerParseSection;
    }

    /**
     * 设置调用解析过滤配置。
     *
     * @param carInvokerParseSection 调用解析规则
     */
    public void setCarInvokerParseSection(StrRuleConfig carInvokerParseSection) {
        this.carInvokerParseSection = carInvokerParseSection;
    }

    /**
     * 获取机器人信息文件配置。
     *
     * @return 机器人信息配置
     */
    public RobotInfoConfig getRobotInfoConfig() {
        return robotInfoConfig;
    }

    /**
     * 设置机器人信息文件配置。
     *
     * @param robotInfoConfig 机器人信息配置
     */
    public void setRobotInfo(RobotInfoConfig robotInfoConfig) {
        this.robotInfoConfig = robotInfoConfig;
    }
}
