package tech.waitforu.rule;

import tech.waitforu.pojo.config.StrRuleConfig;
import tech.waitforu.pojo.config.ConfigValidator;
import tech.waitforu.exception.KrlConfigException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 字符串忽略规则执行器。
 * <p>
 * 规则约定：
 * - 以 {@code !} 开头：命中后返回“忽略”；
 * - 不以 {@code !} 开头：命中后返回“不忽略”；
 * - 前缀与后缀规则会被同时评估，而不是按组短路。
 */
public class IgnoreRuleByStr {

    private static final String ACTION_IGNORE = "IGNORE";

    // 前缀列表，以!开头的路径代表忽略，其他路径代表选择
    private final List<String> prefix;
    // 后缀列表，以!开头的路径代表忽略，其他路径代表选择
    private final List<String> suffix;
    /** 规则所在配置段名称，用于输出错误信息。 */
    private final String sectionName;
    /** 未命中任何规则时的默认动作。 */
    private final boolean defaultIgnore;

    /**
     * 使用配置构造规则执行器。
     *
     * @param strRuleConfig 规则配置
     */
    public IgnoreRuleByStr(StrRuleConfig strRuleConfig) {
        this("<unknown>", strRuleConfig);
    }

    /**
     * 使用配置构造规则执行器，并附带规则段名称以便输出冲突信息。
     *
     * @param sectionName 规则段名称
     * @param strRuleConfig 规则配置
     */
    public IgnoreRuleByStr(String sectionName, StrRuleConfig strRuleConfig) {
        this.sectionName = Objects.requireNonNull(sectionName, "sectionName 不能为空");
        ConfigValidator.validateRuleSection(sectionName, strRuleConfig);
        prefix = normalizeRules(strRuleConfig.getPrefix());
        suffix = normalizeRules(strRuleConfig.getSuffix());
        defaultIgnore = ACTION_IGNORE.equals(ConfigValidator.normalizeAction(strRuleConfig.getDefaultAction()));
    }

    /**
     * 判断字符串是否应被忽略。
     *
     * @param str 待判断字符串
     * @return true=忽略；false=保留
     */
    public boolean isIgnore(String str) {
        Objects.requireNonNull(str, "待匹配字符串不能为空");

        String normalizedInput = str.trim().toUpperCase(Locale.ROOT);
        List<String> allowMatches = new ArrayList<>();
        List<String> ignoreMatches = new ArrayList<>();

        collectMatches(normalizedInput, prefix, MatchType.PREFIX, allowMatches, ignoreMatches);
        collectMatches(normalizedInput, suffix, MatchType.SUFFIX, allowMatches, ignoreMatches);

        if (!allowMatches.isEmpty() && !ignoreMatches.isEmpty()) {
            throw new KrlConfigException(sectionName + " 规则冲突：输入 [" + str + "] 同时命中允许规则 "
                    + allowMatches + " 与忽略规则 " + ignoreMatches);
        }
        if (!ignoreMatches.isEmpty()) {
            return true;
        }
        if (!allowMatches.isEmpty()) {
            return false;
        }
        return defaultIgnore;
    }

    /**
     * 规则列表归一化。
     *
     * @param rules 规则列表
     * @return 归一化后的规则列表
     */
    private List<String> normalizeRules(List<String> rules) {
        if (rules == null) {
            return List.of();
        }
        return rules.stream()
                .map(rule -> rule.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    /**
     * 收集匹配规则。每个规则进行匹配和字符串匹配，一个字符串可能命中多个规则，如果匹配到规则且是允许规则，则规则添加到允许匹配规则列表。同理，匹配到忽略规则 则添加到忽略匹配规则列表。
     *
     * @param input       输入字符串
     * @param rules       规则列表
     * @param matchType   匹配类型（前缀或后缀）
     * @param allowMatches 允许匹配规则列表
     * @param ignoreMatches 忽略匹配规则列表
     */
    private void collectMatches(String input,
                                List<String> rules,
                                MatchType matchType,
                                List<String> allowMatches,
                                List<String> ignoreMatches) {
        for (String rule : rules) {
            boolean ignoreRule = rule.startsWith("!");
            String pattern = ignoreRule ? rule.substring(1) : rule;
            boolean matched = matchType == MatchType.PREFIX
                    ? input.startsWith(pattern)
                    : input.endsWith(pattern);
            if (!matched) {
                //规则未命中，跳过
                continue;
            }
            // 规则命中，根据规则类型，将其添加到对应的匹配规则列表中
            String matchText = matchType.name() + ":" + rule;
            if (ignoreRule) {
                ignoreMatches.add(matchText);
            } else {
                allowMatches.add(matchText);
            }
        }
    }

    private enum MatchType {
        PREFIX,
        SUFFIX
    }
}
