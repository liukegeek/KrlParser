package tech.waitforu.pojo.config;

import tech.waitforu.exception.KrlConfigException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 配置校验器。
 * <p>
 * 负责在分析开始前校验规则配置是否合法，避免运行期因规则冲突或旧语义哨兵值导致结果不可预测。
 */
public final class ConfigValidator {

    private static final String ACTION_ALLOW = "ALLOW";
    private static final String ACTION_IGNORE = "IGNORE";

    private ConfigValidator() {
    }

    /**
     * 校验顶层配置。
     *
     * @param config 顶层配置对象
     */
    public static void validate(Config config) {
        if (config == null) {
            throw new KrlConfigException("配置不能为空");
        }
        validateRuleSection("fileLoadSection", config.getFileLoadSection());
        validateRuleSection("carInvokerParseSection", config.getCarInvokerParseSection());
    }

    /**
     * 校验单个字符串规则 section。
     *
     * @param sectionName 配置 section 名称
     * @param ruleConfig 规则配置对象
     */
    public static void validateRuleSection(String sectionName, StrRuleConfig ruleConfig) {
        Objects.requireNonNull(sectionName, "sectionName 不能为空");
        if (ruleConfig == null) {
            throw new KrlConfigException("配置缺少规则段: " + sectionName);
        }

        String defaultAction = normalizeAction(ruleConfig.getDefaultAction());
        if (!ACTION_ALLOW.equals(defaultAction) && !ACTION_IGNORE.equals(defaultAction)) {
            throw new KrlConfigException(sectionName + ".defaultAction 必须为 allow 或 ignore");
        }

        validateRuleList(sectionName, "prefix", ruleConfig.getPrefix());
        validateRuleList(sectionName, "suffix", ruleConfig.getSuffix());
    }

    /**
     * 校验字符串规则列表。
     *
     * @param sectionName 配置 section 名称
     * @param listName    规则列表名称
     * @param rules       规则列表
     */
    private static void validateRuleList(String sectionName, String listName, List<String> rules) {
        if (rules == null) {
            return;
        }
        for (int i = 0; i < rules.size(); i++) {
            String rawRule = rules.get(i);
            if (rawRule == null) {
                throw new KrlConfigException(sectionName + "." + listName + "[" + i + "] 不能为空");
            }
            String normalized = rawRule.trim();
            if (normalized.isEmpty()) {
                throw new KrlConfigException(sectionName + "." + listName + "[" + i
                        + "] 不能为空白；请使用 defaultAction 指定默认行为");
            }
            if ("!".equals(normalized)) {
                throw new KrlConfigException(sectionName + "." + listName + "[" + i
                        + "] 不能为单独的 !；请使用 defaultAction 指定默认行为");
            }
            if ("@SKIP@".equalsIgnoreCase(normalized)) {
                throw new KrlConfigException(sectionName + "." + listName + "[" + i
                        + "] 不能使用旧语义 @SKIP@；请直接删除该项");
            }
        }
    }

    /**
     * 标准化 defaultAction 文本。
     *
     * @param defaultAction 原始动作文本
     * @return 标准化后的动作
     */
    public static String normalizeAction(String defaultAction) {
        if (defaultAction == null) {
            return "";
        }
        return defaultAction.trim().toUpperCase(Locale.ROOT);
    }
}
