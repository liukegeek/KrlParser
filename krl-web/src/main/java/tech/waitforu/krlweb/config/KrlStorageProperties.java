package tech.waitforu.krlweb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * KRL Web 存储配置。
 * <p>
 * 统一定义任务运行时产生的临时文件、结果文件和清理策略，
 * 避免在代码中散落硬编码路径，便于桌面和服务器模式复用。
 */
@ConfigurationProperties(prefix = "krl.storage")
public class KrlStorageProperties {
    /** 临时文件目录。 */
    private String tempDir = System.getProperty("user.home", ".") + "/.KrlParser/tmp";
    /** 任务结果目录。 */
    private String resultDir = System.getProperty("user.home", ".") + "/.KrlParser/results";
    /** 已完成任务的保留时长。 */
    private Duration taskRetention = Duration.ofHours(12);
    /** 定时清理任务的执行周期。 */
    private Duration cleanupInterval = Duration.ofMinutes(30);

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public String getResultDir() {
        return resultDir;
    }

    public void setResultDir(String resultDir) {
        this.resultDir = resultDir;
    }

    public Duration getTaskRetention() {
        return taskRetention;
    }

    public void setTaskRetention(Duration taskRetention) {
        this.taskRetention = taskRetention;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }
}
