package tech.waitforu.krlweb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@SpringBootApplication
public class KrlWebApplication {

    static {
        configureLoggingDirectory();
    }

    private static final Logger LOGGER = LogManager.getLogger(KrlWebApplication.class);

    public static void main(String[] args) {
        // 确保在 Spring 启动前设置，Logback 才能读取到
        if (System.getProperty("log.dir") == null) {
            System.setProperty("log.dir", "logs");
        }

        //System.getProperty 读取的是操作系统的环境变量或 JVM 启动参数。
        //YAML 加载时机：application.yml 是由 Spring 框架加载的。在你调用 SpringApplication.run 之前，Spring 的环境还没有初始化，它根本不知道 YAML 文件的存在，更读不到里面的 port: 2026。
        //因此在main方法中，我们需要先加载application.yml配置文件，然后从配置文件中获取"server.port"设定的端口值。
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml")); // 指定你的配置文件
        Properties properties = yaml.getObject();
        if (properties == null) {
            LOGGER.error("无法加载application.yml配置文件");
            System.exit(1);
        }
        // 如果没有从配置文件中获取到"server.port"设定的端口值，则设定port=0。当端口配置为0时，表示使用随机端口。在后续的BrowserAutoLauncher类中会对使用到的随机端口进行获取。
        int port = properties.getProperty("server.port") != null ? Integer.parseInt(properties.getProperty("server.port")) : 0;
        String url = "http://localhost:" + port;
        try {
            // 尝试启动 Spring 应用
            SpringApplication.run(KrlWebApplication.class, args);
            // 如果到这里没有报错，说明后端启动成功，自动打开浏览器访问
            openBrowser(url);
            LOGGER.info("Krl车型调用分析程序已启动，日志目录: {}", System.getProperty("log.dir"));

        } catch (Exception e) {
            // 关键：判断异常链中是否包含端口占用异常
            if (isPortInUseException(e)) {
                LOGGER.info("检测到端口 {} 已被占用，可能是程序已在运行。即将直接打开浏览器访问，直接打开浏览器访问: {}", port, url);
                openBrowser(url);
                // 正常退出当前尝试启动的进程，因为已经有一个在运行了
                System.exit(0);
            } else {
                // 如果是其他类型的错误（如代码报错），则正常打印堆栈信息
                LOGGER.error("程序启动失败，原因: ", e);
                throw e;
            }
        }
    }


    private static void configureLoggingDirectory() {
        Path logDir;
        String configured = System.getProperty("log.dir");
        if (configured != null && !configured.isBlank()) {
            return;
        }
        String userHome = System.getProperty("user.home", ".");
        File desktopDir = new File(userHome, "Desktop");
        //检查这个路径是否存在并且确实是一个目录
        if (!desktopDir.exists() || !desktopDir.isDirectory()) {

            // 标准桌面路径不存在，尝试使用用户主目录作为备用路径。
            desktopDir = new File(userHome);

            // 对于非标准系统（如某些Linux发行版桌面目录名可能不同），
            // 这可能需要更复杂的逻辑，或者干脆回退到用户主目录
        }
        logDir = Paths.get(userHome, ".KrlParser", "logs");
        try {
            Files.createDirectories(logDir);
            System.setProperty("log.dir", logDir.toString());
        } catch (IOException ex) {
            System.err.println("无法创建日志目录 " + logDir + ": " + ex.getMessage());
            System.setProperty("log.dir", logDir.toAbsolutePath().toString());
        }
    }

    /**
     * 判断是否为端口占用异常
     */
    private static boolean isPortInUseException(Throwable e) {
        while (e != null) {
            // SpringBoot 端口占用通常抛出 BindException 或特定的 PortInUseException
            if (e instanceof java.net.BindException ||
                e.getClass().getName().contains("PortInUseException")) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    /**
     * 封装打开浏览器的逻辑
     */
    private static void openBrowser(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime runtime = Runtime.getRuntime();
        try {
            if (os.contains("win")) {
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                runtime.exec("open " + url);
            } else {
                runtime.exec(new String[]{"xdg-open", url});
            }
        } catch (IOException e) {
            LOGGER.error("自动打开浏览器失败: {}", e.getMessage());
        }
    }
}
