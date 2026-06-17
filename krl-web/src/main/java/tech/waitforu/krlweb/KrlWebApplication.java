package tech.waitforu.krlweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import tech.waitforu.krlweb.config.RuntimeMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * KRL Web 应用启动入口。
 * <p>
 * 当前启动器同时兼容两种运行形态：
 * 1. 桌面模式：保留自动打开浏览器等本机体验；
 * 2. 服务器模式：严格按标准 Web 服务启动，不执行任何桌面行为。
 */
@SpringBootApplication
@EnableScheduling
public class KrlWebApplication {
    private static final String DEFAULT_RUNTIME_MODE = "desktop";
    private static final String DEFAULT_SERVER_PORT = "2026";
    private static final Logger LOGGER = LoggerFactory.getLogger(KrlWebApplication.class);
    private static volatile String bootstrapWarningMessage;

    /** 类加载时先创建兜底日志目录，防止日志系统初始化过早。 */
    static {
        configureFallbackLoggingDirectory();
    }

    /**
     * 程序主入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        BootstrapSettings bootstrapSettings = resolveBootstrapSettings(args);
        System.setProperty("log.dir", bootstrapSettings.logDir());

        String desktopUrl = "http://localhost:" + bootstrapSettings.port();
        try {
            SpringApplication.run(KrlWebApplication.class, args);
            LOGGER.info("KRL Parser 已启动，运行模式: {}, 日志目录: {}",
                    bootstrapSettings.runtimeMode(), bootstrapSettings.logDir());
            if (bootstrapWarningMessage != null) {
                LOGGER.warn(bootstrapWarningMessage);
            }
            if (bootstrapSettings.runtimeMode() == RuntimeMode.DESKTOP) {
                openBrowser(desktopUrl);
            }
        } catch (Exception exception) {
            if (bootstrapSettings.runtimeMode() == RuntimeMode.DESKTOP && isPortInUseException(exception)) {
                LOGGER.info("检测到端口 {} 已被占用，尝试打开已运行的桌面实例: {}",
                        bootstrapSettings.port(), desktopUrl);
                openBrowser(desktopUrl);
                System.exit(0);
                return;
            }
            LOGGER.error("程序启动失败", exception);
            throw exception;
        }
    }

    /**
     * 解析启动期必须用到的基础配置。
     * <p>
     * 这里不能依赖 Spring Environment，因为在 `SpringApplication.run` 之前，
     * Spring 容器尚未完成初始化。为此需要手动按“命令行参数 -> JVM 参数 -> 环境变量 -> 默认值”顺序解析。
     * 不能直接读取 application.yml 作为默认值，因为此时其中的 Spring 占位符尚未解析。
     *
     * @param args 启动参数
     * @return 启动期基础配置
     */
    private static BootstrapSettings resolveBootstrapSettings(String[] args) {
        String runtimeModeValue = resolveBootstrapProperty(args, "krl.runtime.mode", "KRL_RUNTIME_MODE",
                DEFAULT_RUNTIME_MODE);
        String portValue = resolveBootstrapProperty(args, "server.port", "SERVER_PORT",
                DEFAULT_SERVER_PORT);
        String logDir = resolveBootstrapProperty(args, "krl.log.dir", "KRL_LOG_DIR",
                getDefaultLogDirectory().toString());

        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException exception) {
            bootstrapWarningMessage = "启动端口配置无效，已回退到默认端口 " + DEFAULT_SERVER_PORT;
            port = Integer.parseInt(DEFAULT_SERVER_PORT);
        }
        return new BootstrapSettings(RuntimeMode.from(runtimeModeValue), port,
                Path.of(logDir).toAbsolutePath().normalize().toString());
    }

    /**
     * 按固定优先级解析启动属性。
     *
     * @param args         启动参数
     * @param propertyName Spring 风格属性名
     * @param envName      对应环境变量名
     * @param defaultValue 默认值
     * @return 解析结果
     */
    private static String resolveBootstrapProperty(String[] args,
                                                   String propertyName,
                                                   String envName,
                                                   String defaultValue) {
        String argumentPrefix = "--" + propertyName + "=";
        String argumentValue = Arrays.stream(args)
                .filter(arg -> arg.startsWith(argumentPrefix))
                .map(arg -> arg.substring(argumentPrefix.length()))
                .findFirst()
                .orElse(null);
        if (argumentValue != null && !argumentValue.isBlank()) {
            return argumentValue;
        }
        String systemProperty = System.getProperty(propertyName);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    /**
     * 设置兜底日志目录。
     * <p>
     * 若用户没有通过环境变量或 JVM 参数传入日志目录，
     * 则默认使用 `~/.KrlParser/logs`。
     */
    private static void configureFallbackLoggingDirectory() {
        if (System.getProperty("log.dir") != null && !System.getProperty("log.dir").isBlank()) {
            return;
        }
        String envLogDir = System.getenv("KRL_LOG_DIR");
        Path logDir = envLogDir != null && !envLogDir.isBlank()
                ? Path.of(envLogDir)
                : getDefaultLogDirectory();
        try {
            Files.createDirectories(logDir);
        } catch (IOException exception) {
            bootstrapWarningMessage = "创建日志目录失败，将继续尝试使用该目录: " + logDir.toAbsolutePath().normalize();
        }
        System.setProperty("log.dir", logDir.toAbsolutePath().normalize().toString());
    }

    private static Path getDefaultLogDirectory() {
        return Path.of(System.getProperty("user.home", "."), ".KrlParser", "logs");
    }

    /**
     * 判断异常链中是否包含端口占用异常。
     *
     * @param throwable 待检测异常
     * @return true 表示端口被占用
     */
    private static boolean isPortInUseException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.net.BindException
                    || current.getClass().getName().contains("PortInUseException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 打开系统默认浏览器。
     *
     * @param url 目标地址
     */
    private static void openBrowser(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        Runtime runtime = Runtime.getRuntime();
        try {
            if (os.contains("win")) {
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                runtime.exec("open " + url);
            } else {
                runtime.exec(new String[]{"xdg-open", url});
            }
        } catch (IOException exception) {
            LOGGER.warn("自动打开浏览器失败: {}", url, exception);
        }
    }

    /**
     * 启动期所需配置载体。
     *
     * @param runtimeMode 运行模式
     * @param port        服务端口
     * @param logDir      日志目录
     */
    private record BootstrapSettings(RuntimeMode runtimeMode, int port, String logDir) {
    }
}
