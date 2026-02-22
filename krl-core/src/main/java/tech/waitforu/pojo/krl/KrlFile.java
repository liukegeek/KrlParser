package tech.waitforu.pojo.krl;

import java.util.Objects;

/**
 * KRL 文件模型。
 * <p>
 * 保存文件路径、文件名、目录、内容、时间、大小、后缀与类型等元信息。
 */
public class KrlFile {

    /** 文件完整路径。 */
    private String path = "";
    /** 文件名（不含后缀）。 */
    private String name = "";
    /** 文件文本内容。 */
    private String content = "";
    /** 文件目录路径。 */
    private String directory = "";

    /** 文件创建时间（格式化字符串）。 */
    private String createTime = "";
    /** 文件修改时间（格式化字符串）。 */
    private String modifyTime = "";
    /** 文件大小（字节）。 */
    private Long size = 0L;

    /** 文件类型枚举。 */
    private KrlFileType type = KrlFileType.NO_DEFINITION;
    /** 文件后缀（不含点）。 */
    private String suffix = "";


    /**
     * 构造文件对象（内容默认为空）。
     *
     * @param path 文件路径
     * @param createTime 创建时间
     * @param modifyTime 修改时间
     * @param size 文件大小
     */
    public KrlFile(String path, String createTime, String modifyTime, Long size) {
        this(path, createTime, modifyTime, size, "");
    }

    /**
     * 构造文件对象。
     *
     * @param path 文件路径
     * @param createTime 创建时间
     * @param modifyTime 修改时间
     * @param size 文件大小
     * @param content 文件内容
     */
    public KrlFile(String path, String createTime, String modifyTime, Long size, String content) {
        this.path = Objects.requireNonNullElse(path, "");
        this.createTime = Objects.requireNonNullElse(createTime, "");
        this.modifyTime = Objects.requireNonNullElse(modifyTime, "");
        this.size = Objects.requireNonNullElse(size, 0L);
        this.content = Objects.requireNonNullElse(content, "");

        this.name = getNameFromPath();
        this.directory = getDirectoryFromPath();
        this.suffix = getSuffixFromPath();
        this.type = getTypeFromSuffix();
    }

    /**
     * 获取文件路径。
     *
     * @return 文件路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 获取文件名（不含后缀）。
     *
     * @return 文件名
     */
    public String getName() {
        return name;
    }

    /**
     * 获取文件全部内容。
     *
     * @return 文件文本
     */
    public String getContent() {
        return content;
    }


    /**
     * 根据起止索引读取文件子串（闭区间）。
     * <p>
     * 示例：{@code content="apple"}，{@code getContent(1, 3)} 返回 {@code "ppl"}。
     *
     * @param startIndex 起始索引（含）
     * @param stopIndex 结束索引（含）
     * @return 子串；索引非法时返回空字符串
     */
    public String getContent(int startIndex, int stopIndex) {
        if (startIndex < 0 || stopIndex >= content.length() || startIndex > stopIndex) {
            return "";
        }

        return content.substring(startIndex, stopIndex+1);
    }

    /**
     * 获取目录路径。
     *
     * @return 目录路径
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间字符串
     */
    public String getCreateTime() {
        return createTime;
    }

    /**
     * 获取修改时间。
     *
     * @return 修改时间字符串
     */
    public String getModifyTime() {
        return modifyTime;
    }

    /**
     * 获取文件大小。
     *
     * @return 文件大小（字节）
     */
    public Long getSize() {
        return this.size;
    }

    /**
     * 获取文件类型。
     *
     * @return 文件类型枚举
     */
    public KrlFileType getType() {
        return type;
    }

    /**
     * 获取文件后缀。
     *
     * @return 后缀字符串
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * 设置文件内容。
     *
     * @param content 新文件内容
     */
    public void setContent(String content) {
        this.content = Objects.requireNonNullElse(content, "");
    }

    /**
     * 从路径中提取文件名（不含后缀）。
     *
     * @return 文件名
     */
    private String getNameFromPath() {
        if (this.path == null || this.path.isEmpty()) {
            return "";
        }
        //截取掉文件夹的路径。
        String nameWithSuffix = this.path.substring(this.path.lastIndexOf("/") + 1);
        //截取掉后缀名，剩下的就是文件名。
        return nameWithSuffix.substring(0, nameWithSuffix.lastIndexOf("."));
    }

    /**
     * 从路径中提取目录部分。
     *
     * @return 目录路径
     */
    private String getDirectoryFromPath() {
        if (this.path == null || this.path.isEmpty()) {
            return "";
        }
        return this.path.substring(0, this.path.lastIndexOf("/"));
    }

    /**
     * 从路径中提取后缀。
     *
     * @return 后缀字符串
     */
    private String getSuffixFromPath() {
        if (this.path == null || this.path.isEmpty()) {
            return "";
        }
        return this.path.substring(this.path.lastIndexOf(".") + 1);
    }

    /**
     * 根据后缀映射文件类型。
     *
     * @return 文件类型枚举
     */
    private KrlFileType getTypeFromSuffix() {
        if (suffix == null || suffix.isEmpty()) {
            return tech.waitforu.pojo.krl.KrlFileType.NO_DEFINITION;
        }
        return switch (this.suffix) {
            case "src" -> KrlFileType.SRC;
            case "dat" -> KrlFileType.DAT;
            case "ini" -> KrlFileType.INI;
            case "submit" -> KrlFileType.SUBMIT;
            default -> KrlFileType.NO_DEFINITION;
        };
    }


}
