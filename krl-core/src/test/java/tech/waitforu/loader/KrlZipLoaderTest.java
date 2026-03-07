package tech.waitforu.loader;

import org.junit.jupiter.api.Test;
import tech.waitforu.exception.KrlInputException;
import tech.waitforu.pojo.config.StrRuleConfig;
import tech.waitforu.rule.IgnoreRuleByStr;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 备份压缩包加载测试。
 */
class KrlZipLoaderTest {

    /**
     * 不存在的压缩包路径应抛出语义化输入异常。
     */
    @Test
    void constructorShouldThrowWhenZipFileDoesNotExist() {
        StrRuleConfig ignoreSection = new StrRuleConfig();
        ignoreSection.setPrefix(List.of("!"));
        ignoreSection.setSuffix(List.of("@SKIP@"));

        assertThrows(KrlInputException.class,
                () -> new KrlZipLoader("/path/not/exist.zip", new IgnoreRuleByStr(ignoreSection)));
    }
}
