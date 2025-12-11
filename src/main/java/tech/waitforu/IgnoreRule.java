package tech.waitforu;

import tech.waitforu.pojo.FileSectionConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * ClassName: IgnoreRule
 * Package: tech.waitforu
 * Description:
 * Author: LiuKe
 * Create: 2025/11/10 16:59
 * Version 1.0
 */
public class IgnoreRule {

    // 前缀列表，以!开头的路径代表忽略，其他路径代表选择
    private final List<String> prefix;
    // 后缀列表，以!开头的路径代表忽略，其他路径代表选择
    private final List<String> suffix;

    public IgnoreRule(FileSectionConfig fileSectionConfig) {
        prefix = fileSectionConfig.getPrefix();
        suffix = fileSectionConfig.getSuffix();
    }


    //true代表跳过，false代表选择
    public boolean isIgnore(Path path_p) {
        String path = path_p.toString();

        for (String pref : prefix) {
            if (Objects.equals(pref, "@SKIP@")){
                continue;
            }


            // 如果在白名单规则范围中，则不忽略
            if (!pref.startsWith("!") && path.startsWith(pref)) {
                return false;
            }

            // 如果在黑名单规则范围中，则忽略
            if (pref.startsWith("!") && path.startsWith(pref.substring(1))) {
                return true;
            }

            //如果不在当前白名单、黑名单，则继续与下一规则匹配。
        }

        // 前缀匹配结束，与后缀进行匹配。

        for (String suffix : suffix) {
            if (Objects.equals(suffix, "@SKIP@")){
                continue;
            }

            if (!suffix.startsWith("!") && path.endsWith(suffix)) {
                return false;
            }

            if (suffix.startsWith("!") && path.endsWith(suffix.substring(1))) {
                return true;
            }
        }

        // 没有特别说明，则文件默认忽略。
        return true;
    }
}
