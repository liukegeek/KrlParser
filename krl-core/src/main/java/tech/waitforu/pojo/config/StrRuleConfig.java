package tech.waitforu.pojo.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符串规则配置模型。
 * <p>
 * 用于描述前缀/后缀匹配规则列表。
 */
public class StrRuleConfig {
    /** 默认动作：allow / ignore。 */
    private String defaultAction;
    /** 前缀规则列表。 */
    private List<String> prefix = new ArrayList<>();
    /** 后缀规则列表。 */
    private List<String> suffix = new ArrayList<>();

    /**
     * 获取默认动作。
     *
     * @return allow / ignore
     */
    public String getDefaultAction() {
        return defaultAction;
    }

    /**
     * 设置默认动作。
     *
     * @param defaultAction allow / ignore
     */
    public void setDefaultAction(String defaultAction) {
        this.defaultAction = defaultAction;
    }

    /**
     * 获取前缀规则列表。
     *
     * @return 前缀规则列表
     */
    public List<String> getPrefix() {
        return prefix;
    }

    /**
     * 设置前缀规则列表。
     *
     * @param prefix 前缀规则列表
     */
    public void setPrefix(List<String> prefix) {
        this.prefix = prefix == null ? new ArrayList<>() : prefix;
    }

    /**
     * 获取后缀规则列表。
     *
     * @return 后缀规则列表
     */
    public List<String> getSuffix() {
        return suffix;
    }

    /**
     * 设置后缀规则列表。
     *
     * @param suffix 后缀规则列表
     */
    public void setSuffix(List<String> suffix) {
        this.suffix = suffix == null ? new ArrayList<>() : suffix;
    }
}
