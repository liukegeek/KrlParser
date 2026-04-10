package tech.waitforu.krlweb.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.waitforu.krlweb.config.KrlRuntimeProperties;
import tech.waitforu.krlweb.config.RuntimeMode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RuntimeController 测试。
 */
class RuntimeControllerTest {

    /**
     * 接口应返回运行模式与应用版本，供前端展示。
     */
    @Test
    void shouldExposeRuntimeStatusWithAppVersion() throws Exception {
        KrlRuntimeProperties runtimeProperties = new KrlRuntimeProperties();
        runtimeProperties.setMode(RuntimeMode.SERVER);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RuntimeController(runtimeProperties, "1.2.3"))
                .build();

        mockMvc.perform(get("/api/runtime/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runtimeMode").value("server"))
                .andExpect(jsonPath("$.analysisMode").value("async"))
                .andExpect(jsonPath("$.appVersion").value("1.2.3"));
    }

    /**
     * 缺少构建版本时不应返回 null。
     */
    @Test
    void shouldFallbackToEmptyVersionWhenBuildInfoMissing() throws Exception {
        KrlRuntimeProperties runtimeProperties = new KrlRuntimeProperties();

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RuntimeController(runtimeProperties, (String) null))
                .build();

        mockMvc.perform(get("/api/runtime/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runtimeMode").value("desktop"))
                .andExpect(jsonPath("$.analysisMode").value("sync"))
                .andExpect(jsonPath("$.appVersion").value(""));
    }
}
