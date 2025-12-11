package tech.waitforu.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: FileSectionConfig
 * Package: tech.waitforu.pojo
 * Description: yaml中的配置信息对应的数据实体，用于加载和解析文件
 * Author: LiuKe
 * Create: 2025/12/10 15:35
 * Version 1.0
 */
public class FileSectionConfig {
    private List<String> prefix = new ArrayList<>();
    private List<String> suffix = new ArrayList<>();

    public List<String> getPrefix() {
        return prefix;
    }

    public void setPrefix(List<String> prefix) {
        this.prefix = prefix;
    }

    public List<String> getSuffix() {
        return suffix;
    }

    public void setSuffix(List<String> suffix) {
        this.suffix = suffix;
    }
}
