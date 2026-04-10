package tech.waitforu.krlweb.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Web 基础设施配置。
 * <p>
 * 负责注册配置属性对象与异步任务线程池。
 * 线程池单独抽出，便于后续根据服务器规格调整并发度。
 */
@Configuration
@EnableConfigurationProperties({
        KrlRuntimeProperties.class,
        KrlStorageProperties.class,
        KrlAnalysisProperties.class
})
public class KrlWebInfrastructureConfiguration {

    /**
     * 注册分析任务线程池。
     * <p>
     * 默认并发度控制在较保守的范围内，以降低资源占用。
     * 如需提高吞吐，可通过配置项显式调整。
     *
     * @param analysisProperties 任务并发配置
     * @return Spring 可管理的线程池执行器
     */
    @Bean(name = "analysisTaskExecutor")
    public Executor analysisTaskExecutor(KrlAnalysisProperties analysisProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = Math.max(1, analysisProperties.getMaxConcurrentTasks());
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(Math.max(1, analysisProperties.getMaxActiveTasks()));
        executor.setThreadNamePrefix("analysis-task-");
        executor.initialize();
        return executor;
    }
}
