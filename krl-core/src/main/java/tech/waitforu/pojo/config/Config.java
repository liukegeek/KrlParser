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
    private tech.waitforu.pojo.config.StrRuleConfig fileLoadSection;
    private tech.waitforu.pojo.config.StrRuleConfig invokerParseSection;
    private tech.waitforu.pojo.config.RobotInfoConfig robotInfoConfig;

    public tech.waitforu.pojo.config.StrRuleConfig getFileLoadSection() {
        return fileLoadSection;
    }

    public void setFileLoadSection(tech.waitforu.pojo.config.StrRuleConfig fileLoadSection) {
        this.fileLoadSection = fileLoadSection;
    }

    public tech.waitforu.pojo.config.StrRuleConfig getInvokerParseSection() {
        return invokerParseSection;
    }

    public void setInvokerParseSection(tech.waitforu.pojo.config.StrRuleConfig invokerParseSection) {
        this.invokerParseSection = invokerParseSection;
    }

    public tech.waitforu.pojo.config.RobotInfoConfig getRobotInfo() {
        return robotInfoConfig;
    }

    public void setRobotInfo(tech.waitforu.pojo.config.RobotInfoConfig robotInfoConfig) {
        this.robotInfoConfig = robotInfoConfig;
    }
}

