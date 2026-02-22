package tech.waitforu.pojo.config;

/**
 * 机器人信息配置模型。
 * <p>
 * 用于指定备份中机器人元信息文件（通常是 ini）的路径。
 */
public class RobotInfoConfig {
    /** 机器人信息文件路径。 */
    private String filePath;

    /**
     * 获取机器人信息文件路径。
     *
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 设置机器人信息文件路径。
     *
     * @param filePath 文件路径
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
