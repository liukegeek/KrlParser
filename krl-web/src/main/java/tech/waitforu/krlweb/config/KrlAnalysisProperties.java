package tech.waitforu.krlweb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * KRL 解析任务调度配置。
 * <p>
 * 云端部署时，上传 zip 并解析调用关系属于典型的高 I/O 与高 CPU 混合任务。
 * 因此需要明确并发度上限和活动任务上限，避免在小内存服务器上出现资源争用。
 */
@ConfigurationProperties(prefix = "krl.analysis")
public class KrlAnalysisProperties {
    /** 同时执行中的最大任务数。 */
    private int maxConcurrentTasks = 1;
    /** 系统中允许存在的活动任务总数（排队+执行）。 */
    private int maxActiveTasks = 2;

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = Math.max(1, maxConcurrentTasks);
    }

    public int getMaxActiveTasks() {
        return maxActiveTasks;
    }

    public void setMaxActiveTasks(int maxActiveTasks) {
        this.maxActiveTasks = Math.max(1, maxActiveTasks);
    }
}
