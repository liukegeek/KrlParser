package tech.waitforu.krlweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.waitforu.service.CarCallAnalysisService;

@Configuration
public class CoreServiceConfiguration {

    @Bean
    public CarCallAnalysisService carCallAnalysisService() {
        return new CarCallAnalysisService();
    }
}
