package tech.waitforu.krlweb.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tech.waitforu.exception.KrlConfigException;
import tech.waitforu.loader.YamlConfigLoad;
import tech.waitforu.krlweb.exception.InternalServerException;
import tech.waitforu.pojo.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Web 层配置存储服务。
 * <p>
 * 负责：
 * 1. 统一确定配置文件路径；
 * 2. 启动时自动初始化默认配置；
 * 3. 对外提供配置读取与配置解析能力。
 */
@Service
public class ConfigStorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigStorageService.class);
    // 标准化后的配置文件绝对路径
    private final Path configPath;

    /**
     * 构造配置存储服务并规范化配置路径。
     *
     * @param configPathValue 配置路径（支持从配置项注入）
     */
    public ConfigStorageService(@Value("${krl.config.path:${user.home}/.KrlParser/Config.yml}") String configPathValue) {
        this.configPath = Path.of(configPathValue).toAbsolutePath().normalize();
    }

    /**
     * Bean 初始化后执行，确保磁盘上存在可用配置文件。
     *
     */
    @PostConstruct
    public void initialize() {
        try {
            ensureConfigFileExists();
        } catch (IOException exception) {
            throw new IllegalStateException("初始化配置文件失败", exception);
        }
    }

    /**
     * 获取配置文件绝对路径文本。
     *
     * @return 绝对路径字符串
     */
    public String getConfigPathText() {
        return configPath.toString();
    }

    /**
     * 读取配置文件文本内容。
     *
     * @return UTF-8 编码的配置文本
     */
    public String getConfigContent() {
        try {
            ensureConfigFileExists();
            return Files.readString(configPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new InternalServerException("读取配置文件失败", exception);
        }
    }

    /**
     * 优先使用请求内配置文本，否则读取磁盘配置。
     *
     * @param inlineConfigText 前端传入配置文本，可为空
     * @return 解析后的配置对象
     */
    public Config resolveConfig(String inlineConfigText) {
        if (StringUtils.hasText(inlineConfigText)) {
            // 如果请求内配置文本非空，直接解析返回。
            return YamlConfigLoad.parseConfig(inlineConfigText);
        }
        // 如果请求内配置文本为空，从磁盘配置文件加载，解析并返回。
        try {
            return new YamlConfigLoad(configPath.toString()).loadConfig();
        } catch (KrlConfigException exception) {
            throw new InternalServerException("磁盘配置文件无效，请检查 Config.yml", exception);
        }
    }

    /**
     * 确保配置文件目录存在，若不存在则创建，同时从resources/config.yml中复制默认配置。
     *
     * @throws IOException 如果创建目录或复制默认配置文件时发生 I/O 错误
     */
    private void ensureConfigFileExists() throws IOException {
        Path parent = configPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.exists(configPath)) {
            // 如果配置文件已存在，直接返回。
            return;
        }
        // 不存在时从 classpath 复制默认配置文件。
        ClassPathResource defaultConfig = new ClassPathResource("config.yml");
        if (!defaultConfig.exists()) {
            throw new IOException("默认配置文件 config.yml 不存在");
        }
        try (InputStream inputStream = defaultConfig.getInputStream()) {
            Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("已创建默认配置文件: {}", configPath);
        }
    }
}
