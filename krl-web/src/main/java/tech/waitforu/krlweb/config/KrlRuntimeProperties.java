package tech.waitforu.krlweb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * KRL Web 运行模式配置。
 * <p>
 * 该配置用于区分当前应用是运行在桌面个人电脑，
 * 还是运行在云服务器环境中。不同模式下，启动行为、
 * 是否自动打开浏览器以及前端分析链路都会不同。
 */
@ConfigurationProperties(prefix = "krl.runtime")
public class KrlRuntimeProperties {
    /**
     * 当前运行模式。
     * <p>
     * 默认保留桌面模式，兼容原有本机使用方式。
     */
    private RuntimeMode mode = RuntimeMode.DESKTOP;

    /**
     * 获取当前运行模式。
     *
     * @return 当前运行模式
     */
    public RuntimeMode getMode() {
        return mode;
    }

    /**
     * 设置当前运行模式。
     *
     * @param mode 运行模式
     */
    public void setMode(RuntimeMode mode) {
        this.mode = mode == null ? RuntimeMode.DESKTOP : mode;
    }

    /**
     * 判断当前是否为服务器部署模式。
     *
     * @return true 表示服务器模式
     */
    public boolean isServerMode() {
        return mode == RuntimeMode.SERVER;
    }
}
