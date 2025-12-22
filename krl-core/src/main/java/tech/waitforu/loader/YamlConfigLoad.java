package tech.waitforu.loader;

import tech.waitforu.pojo.config.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

/**
 * ClassName: loader.tech.waitforu.YamlConfigLoad
 * Package: tech.waitforu
 * Description:
 * Author: LiuKe
 * Create: 2025/12/10 15:39
 * Version 1.0
 */
public class YamlConfigLoad {
    private final ObjectMapper yamlMapper;
    private final File configFile;

    public YamlConfigLoad(String configPath) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.configFile = new File(configPath);
    }

    public Config loadConfig(Config defaultConfig) throws IOException {
        if (!configFile.exists()) {
            // 找不到外部配置，返回默认配置
            return defaultConfig;
        }
        return yamlMapper.readValue(configFile, Config.class);
    }

    public void saveConfig(Config config) throws IOException {
        // 确保目录存在
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        yamlMapper.writeValue(configFile, config);
    }
}
