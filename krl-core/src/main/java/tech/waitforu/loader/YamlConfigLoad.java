package tech.waitforu.loader;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.waitforu.exception.KrlConfigException;
import tech.waitforu.pojo.config.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 加载 YAML 配置：优先读取外部路径，不存在时回退到 classpath 的 config.yml。
 */
public class YamlConfigLoad {
    // 共享 YAML mapper，避免重复创建并保持解析行为一致
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    // 外部配置文件路径
    private final File configFile;

    /**
     * 构造函数，初始化 YAML 配置加载器
     *
     * @param configPath 配置文件的路径
     */
    public YamlConfigLoad(String configPath) {
        this.configFile = new File(configPath);
    }

    /**
     * 加载配置文件
     *
     * @return 解析后的 Config 对象
     */
    public Config loadConfig() {
        try {
            if (!configFile.exists()) {
                // 外部配置不存在时回退到 resources/config.yml
                try (InputStream configStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (configStream == null) {
                        throw new KrlConfigException("未找到默认配置文件 config.yml");
                    }
                    return YAML_MAPPER.readValue(configStream, Config.class);
                }
            }
            return YAML_MAPPER.readValue(configFile, Config.class);
        } catch (IOException exception) {
            throw new KrlConfigException("配置文件解析失败，请检查YAML格式", exception);
        }
    }

    /**
     * 将 YAML 文本解析为配置对象。
     *
     * @param yamlContent YAML 字符串内容
     * @return 解析后的配置对象
     */
    public static Config parseConfig(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            throw new KrlConfigException("配置内容为空");
        }
        try {
            return YAML_MAPPER.readValue(yamlContent, Config.class);
        } catch (IOException exception) {
            throw new KrlConfigException("配置内容无效，请检查YAML格式", exception);
        }
    }
}
