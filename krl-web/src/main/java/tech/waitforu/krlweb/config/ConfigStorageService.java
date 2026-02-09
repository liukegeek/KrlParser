package tech.waitforu.krlweb.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tech.waitforu.loader.YamlConfigLoad;
import tech.waitforu.pojo.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 管理 Web 侧配置文件的路径、读取和默认落盘。
 */
@Service
public class ConfigStorageService {
    // 标准化后的配置文件绝对路径
    private final Path configPath;

    public ConfigStorageService(@Value("${krl.config.path:${user.home}/.KrlParser/Config.yml}") String configPathValue) {
        this.configPath = Path.of(configPathValue).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initialize() throws IOException {
        ensureConfigFileExists();
    }

    public String getConfigPathText() {
        return configPath.toString();
    }

    public String getConfigContent() throws IOException {
        ensureConfigFileExists();
        return Files.readString(configPath, StandardCharsets.UTF_8);
    }

    /**
     * 优先使用请求内配置文本，否则读取磁盘配置。
     */
    public Config resolveConfig(String inlineConfigText) throws IOException {
        if (StringUtils.hasText(inlineConfigText)) {
            return YamlConfigLoad.parseConfig(inlineConfigText);
        }
        return new YamlConfigLoad(configPath.toString()).loadConfig();
    }

    private void ensureConfigFileExists() throws IOException {
        Path parent = configPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.exists(configPath)) {
            return;
        }
        // 不存在时从 classpath 复制默认配置
        ClassPathResource defaultConfig = new ClassPathResource("config.yml");
        if (!defaultConfig.exists()) {
            throw new IOException("默认配置文件 config.yml 不存在");
        }
        try (InputStream inputStream = defaultConfig.getInputStream()) {
            Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
