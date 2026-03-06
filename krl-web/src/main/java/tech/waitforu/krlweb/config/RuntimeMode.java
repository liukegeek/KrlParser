package tech.waitforu.krlweb.config;

/**
 * 运行模式枚举。
 * <p>
 * `DESKTOP` 用于当前桌面应用模式：
 * 启动后允许自动打开浏览器，并默认绑定本机地址。
 * <p>
 * `SERVER` 用于云端部署模式：
 * 禁止任何本机桌面行为，只保留标准 Web 服务语义。
 */
public enum RuntimeMode {
    /** 本地桌面模式。 */
    DESKTOP,
    /** 服务器部署模式。 */
    SERVER;

    /**
     * 将任意字符串安全转换为运行模式。
     * <p>
     * 未识别值统一回退为桌面模式，避免因拼写问题直接导致启动失败。
     *
     * @param value 原始字符串
     * @return 解析后的运行模式
     */
    public static RuntimeMode from(String value) {
        if (value == null || value.isBlank()) {
            return DESKTOP;
        }
        try {
            return RuntimeMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return DESKTOP;
        }
    }
}
