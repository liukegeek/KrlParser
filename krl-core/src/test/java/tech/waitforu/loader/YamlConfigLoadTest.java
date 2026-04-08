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

    @Test
    void parseConfigShouldThrowWhenDefaultActionIsMissing() {
        String yaml = """
                robotInfo:
                  filePath: "/am.ini"
                fileLoadSection:
                  prefix:
                    - "/KRC/R1"
                  suffix: []
                carInvokerParseSection:
                  defaultAction: allow
                  prefix:
                    - "!BAS"
                  suffix: []
                """;

        assertThrows(KrlConfigException.class, () -> YamlConfigLoad.parseConfig(yaml));
    }

    @Test
    void parseConfigShouldThrowWhenLegacySkipTokenIsUsed() {
        String yaml = """
                robotInfo:
                  filePath: "/am.ini"
                fileLoadSection:
                  defaultAction: ignore
                  prefix:
                    - "@SKIP@"
                  suffix: []
                carInvokerParseSection:
                  defaultAction: allow
                  prefix:
                    - "!BAS"
                  suffix: []
                """;

        assertThrows(KrlConfigException.class, () -> YamlConfigLoad.parseConfig(yaml));
    }

    @Test
    void parseConfigShouldThrowWhenBlankFallbackRuleIsUsed() {
        String yaml = """
                robotInfo:
                  filePath: "/am.ini"
                fileLoadSection:
                  defaultAction: ignore
                  prefix:
                    - "!"
                  suffix: []
                carInvokerParseSection:
                  defaultAction: allow
                  prefix:
                    - "!BAS"
                  suffix: []
                """;

        assertThrows(KrlConfigException.class, () -> YamlConfigLoad.parseConfig(yaml));
    }
}
