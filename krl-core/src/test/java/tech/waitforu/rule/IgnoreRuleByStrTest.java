package tech.waitforu.rule;

import org.junit.jupiter.api.Test;
import tech.waitforu.exception.KrlConfigException;
import tech.waitforu.pojo.config.StrRuleConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字符串忽略规则回归测试。
 */
class IgnoreRuleByStrTest {

    @Test
    void shouldKeepWhenOnlyAllowPrefixMatches() {
        IgnoreRuleByStr rule = new IgnoreRuleByStr("carInvokerParseSection",
                ruleConfig("allow", List.of("CALL"), List.of()));

        assertFalse(rule.isIgnore("CALL_PICK_PART"));
    }

    @Test
    void shouldIgnoreWhenOnlyIgnoreSuffixMatches() {
        IgnoreRuleByStr rule = new IgnoreRuleByStr("carInvokerParseSection",
                ruleConfig("allow", List.of(), List.of("!_CAR")));

        assertTrue(rule.isIgnore("PUTPART_TO_CAR"));
    }

    @Test
    void shouldThrowWhenAllowAndIgnoreRulesConflict() {
        IgnoreRuleByStr rule = new IgnoreRuleByStr("carInvokerParseSection",
                ruleConfig("allow", List.of("CALL"), List.of("!_CAR")));

        assertThrows(KrlConfigException.class, () -> rule.isIgnore("CALL_TO_CAR"));
    }

    @Test
    void shouldUseDefaultAllowWhenNothingMatches() {
        IgnoreRuleByStr rule = new IgnoreRuleByStr("carInvokerParseSection",
                ruleConfig("allow", List.of("!BAS"), List.of()));

        assertFalse(rule.isIgnore("CUSTOM_CALL"));
    }

    @Test
    void shouldUseDefaultIgnoreWhenNothingMatches() {
        IgnoreRuleByStr rule = new IgnoreRuleByStr("fileLoadSection",
                ruleConfig("ignore", List.of("/KRC/R1"), List.of("!.DAT")));

        assertTrue(rule.isIgnore("/TEMP/README.TXT"));
    }

    private StrRuleConfig ruleConfig(String defaultAction, List<String> prefix, List<String> suffix) {
        StrRuleConfig config = new StrRuleConfig();
        config.setDefaultAction(defaultAction);
        config.setPrefix(prefix);
        config.setSuffix(suffix);
        return config;
    }
}
