package tech.waitforu.krlweb.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
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
    /** 当前应用版本。 */
    private final String appVersion;

    /**
     * 构造运行模式控制器。
     *
     * @param runtimeProperties     运行模式配置
     * @param buildPropertiesProvider 构建信息提供器
     */
    @Autowired
    public RuntimeController(KrlRuntimeProperties runtimeProperties,
                             ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this(runtimeProperties, resolveAppVersion(buildPropertiesProvider.getIfAvailable()));
    }

    RuntimeController(KrlRuntimeProperties runtimeProperties, String appVersion) {
        this.runtimeProperties = runtimeProperties;
        this.appVersion = appVersion == null ? "" : appVersion.trim();
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
        return new RuntimeStatusResponse(runtimeMode, analysisMode, appVersion);
    }

    /**
     * 解析应用版本号。
     *
     * @param buildProperties 构建信息
     * @return 构建版本，缺失时返回空字符串
     */
    private static String resolveAppVersion(BuildProperties buildProperties) {
        if (buildProperties == null) {
            return "";
        }
        String version = buildProperties.getVersion();
        return version == null ? "" : version.trim();
    }

    /**
     * 运行模式状态响应。
     *
     * @param runtimeMode  当前运行模式，`desktop` 或 `server`
     * @param analysisMode 当前分析模式，`sync` 或 `async`
     * @param appVersion   当前应用版本
     */
    public record RuntimeStatusResponse(String runtimeMode, String analysisMode, String appVersion) {
    }
}
