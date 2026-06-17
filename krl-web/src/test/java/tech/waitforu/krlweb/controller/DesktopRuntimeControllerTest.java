package tech.waitforu.krlweb.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.waitforu.krlweb.config.DesktopModeGuard;
import tech.waitforu.krlweb.config.KrlRuntimeProperties;
import tech.waitforu.krlweb.config.RuntimeMode;
import tech.waitforu.krlweb.service.ApplicationShutdownService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DesktopRuntimeControllerTest {
    private KrlRuntimeProperties runtimeProperties;
    private TrackingShutdownService shutdownService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        runtimeProperties = new KrlRuntimeProperties();
        shutdownService = new TrackingShutdownService();
        DesktopRuntimeController controller = new DesktopRuntimeController(
                new DesktopModeGuard(runtimeProperties),
                shutdownService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldScheduleShutdownInDesktopMode() throws Exception {
        mockMvc.perform(post("/api/runtime/shutdown"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.message").value("程序正在退出"));

        assertTrue(shutdownService.scheduleCalled);
    }

    @Test
    void shouldRejectShutdownInServerMode() throws Exception {
        runtimeProperties.setMode(RuntimeMode.SERVER);

        mockMvc.perform(post("/api/runtime/shutdown"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("程序退出仅在桌面模式下可用"));

        assertFalse(shutdownService.scheduleCalled);
    }

    private static final class TrackingShutdownService extends ApplicationShutdownService {
        private boolean scheduleCalled;

        private TrackingShutdownService() {
            super(new GenericApplicationContext());
        }

        @Override
        public boolean scheduleShutdown() {
            scheduleCalled = true;
            return true;
        }
    }
}
