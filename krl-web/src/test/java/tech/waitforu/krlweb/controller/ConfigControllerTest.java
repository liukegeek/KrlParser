package tech.waitforu.krlweb.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.waitforu.krlweb.config.ConfigStorageService;
import tech.waitforu.krlweb.config.DesktopModeGuard;
import tech.waitforu.krlweb.config.KrlRuntimeProperties;
import tech.waitforu.krlweb.config.RuntimeMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfigControllerTest {
    private KrlRuntimeProperties runtimeProperties;
    private TrackingConfigStorageService configStorageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        runtimeProperties = new KrlRuntimeProperties();
        configStorageService = new TrackingConfigStorageService();
        ConfigController controller = new ConfigController(
                configStorageService,
                new DesktopModeGuard(runtimeProperties)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldResetConfigInDesktopMode() throws Exception {
        mockMvc.perform(post("/api/config/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configPath").value("/tmp/Config.yml"))
                .andExpect(jsonPath("$.content").value("default: value\n"));

        assertTrue(configStorageService.resetCalled);
    }

    @Test
    void shouldRejectResetInServerModeWithoutChangingConfig() throws Exception {
        runtimeProperties.setMode(RuntimeMode.SERVER);

        mockMvc.perform(post("/api/config/reset"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("配置初始化仅在桌面模式下可用"));

        assertFalse(configStorageService.resetCalled);
    }

    private static final class TrackingConfigStorageService extends ConfigStorageService {
        private boolean resetCalled;

        private TrackingConfigStorageService() {
            super("/tmp/krl-parser-controller-test/Config.yml");
        }

        @Override
        public String resetToDefault() {
            resetCalled = true;
            return "default: value\n";
        }

        @Override
        public String getConfigPathText() {
            return "/tmp/Config.yml";
        }
    }
}
