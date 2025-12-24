package tech.waitforu.krlweb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class KrlWebApplication {

    static {
        configureLoggingDirectory();
    }

    private static final Logger LOGGER = LogManager.getLogger(KrlWebApplication.class);

    public static void main(String[] args) {
        int port = System.getProperty("server.port") != null ? Integer.parseInt(System.getProperty("server.port")) : 0;

        // 如果没有从配置文件中获取到"server.port"设定的端口值，则设定port=0。当端口配置为0时，表示使用随机端口。在后续的BrowserAutoLauncher类中会对使用到的随机端口进行获取。
        if (port == 0 || !isPortInUse(port)) {
            SpringApplication.run(KrlWebApplication.class, args);
            LOGGER.info("Krl车型调用分析程序已启动，日志目录: {}", System.getProperty("log.dir"));
        } else {
            String url = "http://localhost:" + port;
            String os = System.getProperty("os.name").toLowerCase();
            Runtime runtime = Runtime.getRuntime();
            LOGGER.info("端口 {} 已被占用，无法启动应用后端服务，即将直接打开浏览器访问，直接打开浏览器访问: {}", port, url);
            try {
                if (os.contains("win")) {
                    // Windows
                    runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    // macOS
                    runtime.exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    // Linux/Unix
                    runtime.exec(new String[]{"xdg-open", url});
                }
                System.exit(0); // 退出当前实例
            } catch (IOException e) {
                LOGGER.error("无法打开浏览器: {}", e.getMessage());
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

    private static boolean isPortInUse(int port) {
        try (Socket socket = new Socket()) {
            // 尝试连接到指定端口
            socket.connect(new InetSocketAddress("localhost", port), 1000);
            return true; // 端口已被占用
        } catch (IOException e) {
            return false;// 端口未被占用
        }
    }
}
