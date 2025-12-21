package tech.waitforu.pojo.config;

/**
 * ClassName: config.pojo.tech.waitforu.RobotInfoConfig
 * Package: tech.waitforu.pojo
 * Description: 配置文件中机器人信息文件路径配置信息 对应的数据模型，用于加载和解析文件。
 * Author: LiuKe
 * Create: 2025/12/18 10:28
 * Version 1.0
 */
public class RobotInfoConfig {
    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
