package tech.waitforu.krlweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.waitforu.service.CarCallAnalysisService;

/**
 * Core 模块服务装配配置。
 * <p>
 * 当前主要将 {@link CarCallAnalysisService} 注册为 Spring Bean，
 * 供 Web 控制器直接注入使用。
 */
@Configuration
public class CoreServiceConfiguration {

    /**
     * 注册调用关系分析服务 Bean。
     *
     * @return 新建的分析服务实例
     */
    @Bean
    public CarCallAnalysisService carCallAnalysisService() {
        return new CarCallAnalysisService();
    }
}
