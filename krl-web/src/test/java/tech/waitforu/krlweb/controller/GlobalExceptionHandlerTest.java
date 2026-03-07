package tech.waitforu.krlweb.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.waitforu.exception.KrlConfigException;
import tech.waitforu.krlweb.exception.BadRequestException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理器测试。
 */
class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    /**
     * 创建仅用于异常映射验证的轻量控制器。
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Web 层异常应保持原始状态码和消息。
     */
    @Test
    void shouldMapApiExceptionToConfiguredStatus() throws Exception {
        mockMvc.perform(get("/test/api-exception").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("参数错误"));
    }

    /**
     * 非法参数异常应映射为 400。
     */
    @Test
    void shouldMapIllegalArgumentExceptionToBadRequest() throws Exception {
        mockMvc.perform(get("/test/illegal-argument").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("非法参数"));
    }

    /**
     * Core 配置异常应映射为 400。
     */
    @Test
    void shouldMapKrlConfigExceptionToBadRequest() throws Exception {
        mockMvc.perform(get("/test/config-exception").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("配置无效"));
    }

    /**
     * 未知异常不应泄露原始堆栈消息。
     */
    @Test
    void shouldMapUnknownExceptionToGenericInternalServerError() throws Exception {
        mockMvc.perform(get("/test/runtime-exception").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误，请查看日志"));
    }

    /**
     * 仅用于抛出测试异常的控制器。
     */
    @RestController
    @RequestMapping("/test")
    private static final class ExceptionThrowingController {

        /**
         * 抛出 Web 层请求异常。
         *
         * @return never
         */
        @GetMapping("/api-exception")
        String apiException() {
            throw new BadRequestException("参数错误");
        }

        /**
         * 抛出普通非法参数异常。
         *
         * @return never
         */
        @GetMapping("/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("非法参数");
        }

        /**
         * 抛出 core 配置异常。
         *
         * @return never
         */
        @GetMapping("/config-exception")
        String configException() {
            throw new KrlConfigException("配置无效");
        }

        /**
         * 抛出未知运行时异常。
         *
         * @return never
         */
        @GetMapping("/runtime-exception")
        String runtimeException() {
            throw new RuntimeException("不应暴露");
        }
    }
}
