package tech.waitforu.pojo;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * ClassName: IgnoreRuleByStr
 * Package: tech.waitforu
 * Description: 通过输入字符串判断是否符合条件，并返回true或者false
 * Author: LiuKe
 * Create: 2025/11/10 16:59
 * Version 1.0
 */
public class IgnoreRuleByStr {

    // 前缀列表，以!开头的路径代表忽略，其他路径代表选择
    private final List<String> prefix;
    // 后缀列表，以!开头的路径代表忽略，其他路径代表选择
    private final List<String> suffix;

    public IgnoreRuleByStr(StrRuleConfig strRuleConfig) {
        prefix = strRuleConfig.getPrefix().stream().map(String::toUpperCase).toList();
        suffix = strRuleConfig.getSuffix().stream().map(String::toUpperCase).toList();
    }


    //true代表跳过，false代表选择
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
