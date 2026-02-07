package tech.waitforu.loader;

import tech.waitforu.pojo.config.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * ClassName: loader.tech.waitforu.YamlConfigLoad
 * Package: tech.waitforu
 * Description: 用于加载 YAML 格式的配置文件
 * Author: LiuKe
 * Create: 2025/12/10 15:39
 * Version 1.0
 */
public class YamlConfigLoad {
    // yamlMapper用于通过readValue方法从一个`.yml`解析中对应的JAVA对象
    private final ObjectMapper yamlMapper;
    // configFile 用于存储配置文件的路径
    private final File configFile;

    /**
     * 构造函数，初始化 YAML 配置加载器
     * @param configPath 配置文件的路径
     */
    public YamlConfigLoad(String configPath) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.configFile = new File(configPath);
    }

    /**
     * 加载配置文件
     * @return 解析后的 Config 对象
     * @throws IOException 如果读取配置文件时发生 I/O 错误
     */
    public Config loadConfig() throws IOException {
        if (!configFile.exists()) {
            // 找不到外部配置，使用resources中的默认配置"config.yml",
            InputStream configStream = getClass().getClassLoader().getResourceAsStream("config.yml");
            return yamlMapper.readValue(configStream,Config.class);
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
