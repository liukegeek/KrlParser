package tech.waitforu.rule;

import tech.waitforu.pojo.config.StrRuleConfig;

import java.util.List;
import java.util.Objects;

/**
 * 字符串忽略规则执行器。
 * <p>
 * 规则约定：
 * - 以 {@code !} 开头：命中后返回“忽略”；
 * - 不以 {@code !} 开头：命中后返回“不忽略”；
 * - {@code @SKIP@}：跳过该规则项。
 */
public class IgnoreRuleByStr {

    // 前缀列表，以!开头的路径代表忽略，其他路径代表选择
    private final List<String> prefix;
    // 后缀列表，以!开头的路径代表忽略，其他路径代表选择
    private final List<String> suffix;

    /**
     * 使用配置构造规则执行器。
     *
     * @param strRuleConfig 规则配置
     */
    public IgnoreRuleByStr(StrRuleConfig strRuleConfig) {
        prefix = strRuleConfig.getPrefix().stream().map(String::toUpperCase).toList();
        suffix = strRuleConfig.getSuffix().stream().map(String::toUpperCase).toList();
    }


    /**
     * 判断字符串是否应被忽略。
     * <p>
     * 执行顺序：先前缀后后缀，按配置列表顺序短路返回。
     *
     * @param str 待判断字符串
     * @return true=忽略；false=保留
     */
    public boolean isIgnore(String str) {

        //转换为大写，便于比较
        str = str.toUpperCase();

        for (String pref : prefix) {
            if (Objects.equals(pref, "@SKIP@".toUpperCase())){
                continue;
            }


            // 如果在白名单规则范围中，则不忽略
            if (!pref.startsWith("!") && str.startsWith(pref)) {
                return false;
            }

            // 如果在黑名单规则范围中，则忽略
            if (pref.startsWith("!") && str.startsWith(pref.substring(1))) {
                return true;
            }

            //如果不在当前白名单、黑名单，则继续与下一规则匹配。
        }

        // 前缀匹配结束，与后缀进行匹配。

        for (String suffix : suffix) {
            if (Objects.equals(suffix, "@SKIP@".toUpperCase())){
                continue;
            }

            if (!suffix.startsWith("!") && str.endsWith(suffix)) {
                return false;
            }

            if (suffix.startsWith("!") && str.endsWith(suffix.substring(1))) {
                return true;
            }
        }

        // 没有特别说明，则文件默认忽略。
        return true;
    }
}
