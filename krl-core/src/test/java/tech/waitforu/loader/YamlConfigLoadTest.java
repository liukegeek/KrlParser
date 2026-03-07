package tech.waitforu.loader;

import org.junit.jupiter.api.Test;
import tech.waitforu.exception.KrlConfigException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * YAML 配置加载测试。
 */
class YamlConfigLoadTest {

    /**
     * 非法 YAML 文本应抛出语义化配置异常。
     */
    @Test
    void parseConfigShouldThrowWhenYamlIsInvalid() {
        assertThrows(KrlConfigException.class, () -> YamlConfigLoad.parseConfig("robotInfo: ["));
    }
}
