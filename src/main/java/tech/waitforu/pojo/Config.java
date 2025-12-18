package tech.waitforu.pojo;


/**
 * ClassName: Config
 * Package: tech.waitforu.pojo
 * Description: yaml中的配置信息对应的数据实体。
 * Author: LiuKe
 * Create: 2025/12/10 14:51
 * Version 1.0
 */
public class Config {
    public StrRuleConfig fileLoadSection;
    public StrRuleConfig invokerParseSection;
    public RobotInfo robotInfo;

    public StrRuleConfig getFileLoadSection() {
        return fileLoadSection;
    }

    public void setFileLoadSection(StrRuleConfig fileLoadSection) {
        this.fileLoadSection = fileLoadSection;
    }

    public StrRuleConfig getInvokerParseSection() {
        return invokerParseSection;
    }

    public void setInvokerParseSection(StrRuleConfig invokerParseSection) {
        this.invokerParseSection = invokerParseSection;
    }

    public RobotInfo getRobotInfoFilePath() {
        return robotInfo;
    }

    public void setRobotInfoFilePath(RobotInfo robotInfo) {
        this.robotInfo = robotInfo;
    }
}

