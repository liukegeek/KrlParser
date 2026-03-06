package tech.waitforu.krlweb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.waitforu.krlweb.config.KrlRuntimeProperties;

/**
 * 运行模式状态控制器。
 * <p>
 * 该接口专门供前端启动时读取当前应用运行形态，
 * 让页面明确知道应该使用：
 * 1. 桌面同步分析链路；
 * 2. 服务器异步任务链路。
 * <p>
 * 由于系统已经移除登录认证，因此这里只暴露纯运行状态，不再混入登录态信息。
 */
@RestController
@RequestMapping("/api/runtime")
public class RuntimeController {
    /** 当前运行模式配置。 */
    private final KrlRuntimeProperties runtimeProperties;

    /**
     * 构造运行模式控制器。
     *
     * @param runtimeProperties 运行模式配置
     */
    public RuntimeController(KrlRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    /**
     * 查询当前前端应使用的运行模式与分析模式。
     *
     * @return 运行状态响应
     */
    @GetMapping("/status")
    public RuntimeStatusResponse status() {
        String runtimeMode = runtimeProperties.getMode().name().toLowerCase();
        String analysisMode = runtimeProperties.isServerMode() ? "async" : "sync";
        return new RuntimeStatusResponse(runtimeMode, analysisMode);
    }

    /**
     * 运行模式状态响应。
     *
     * @param runtimeMode  当前运行模式，`desktop` 或 `server`
     * @param analysisMode 当前分析模式，`sync` 或 `async`
     */
    public record RuntimeStatusResponse(String runtimeMode, String analysisMode) {
    }
}
