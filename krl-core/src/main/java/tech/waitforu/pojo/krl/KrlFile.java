package tech.waitforu.pojo.krl;

import java.util.Objects;

/**
 * ClassName: krl.pojo.tech.waitforu.KrlFile
 * Package: tech.waitforu.pojo.krl
 * Description: krl中文件对应的类，包含文件的路径、名称、内容、目录、创建时间、修改时间、大小、类型、后缀等信息，如果不指定，则为空内容:""
 * Author: LiuKe
 * Create: 2025/12/12 09:12
 * Version 1.0
 */
public class KrlFile {

    private String path = "";
    private String name = "";
    private String content = "";
    private String directory = "";

    private String createTime = "";
    private String modifyTime = "";
    private Long size = 0L;

    private tech.waitforu.pojo.krl.KrlFileType type = tech.waitforu.pojo.krl.KrlFileType.NO_DEFINITION;
    private String suffix = "";



    public KrlFile(String path, String createTime, String modifyTime, Long size) {
        this(path, createTime, modifyTime, size, "");
    }

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

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }


    // 根据起始索引和结束索引获取子字符串。比如content="apple"  getContent(1, 3)表示获取从第1个字符到第3个字符的子字符串:"ppl"。
    public String getContent(int startIndex, int stopIndex) {
        if (startIndex < 0 || stopIndex >= content.length() || startIndex > stopIndex) {
            return "";
        }

        return content.substring(startIndex, stopIndex+1);
    }

    public String getDirectory() {
        return directory;
    }

    public String getCreateTime() {
        return createTime;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    public Long getSize() {
        return this.size;
    }

    public tech.waitforu.pojo.krl.KrlFileType getType() {
        return type;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setContent(String content) {
        this.content = Objects.requireNonNullElse(content, "");
    }

    private String getNameFromPath() {
        if (this.path == null || this.path.isEmpty()) {
            return "";
        }
        //截取掉文件夹的路径。
        String nameWithSuffix = this.path.substring(this.path.lastIndexOf("/") + 1);
        //截取掉后缀名，剩下的就是文件名。
        return nameWithSuffix.substring(0, nameWithSuffix.lastIndexOf("."));
    }

    private String getDirectoryFromPath() {
        if (this.path == null || this.path.isEmpty()) {
            return "";
        }
        return this.path.substring(0, this.path.lastIndexOf("/"));
    }
    private String getSuffixFromPath() {
        if (this.path == null || this.path.isEmpty()) {
            return "";
        }
        return this.path.substring(this.path.lastIndexOf(".") + 1);
    }

    private tech.waitforu.pojo.krl.KrlFileType getTypeFromSuffix() {
        if (suffix == null || suffix.isEmpty()) {
            return tech.waitforu.pojo.krl.KrlFileType.NO_DEFINITION;
        }
        return switch (this.suffix) {
            case "src" -> tech.waitforu.pojo.krl.KrlFileType.SRC;
            case "dat" -> tech.waitforu.pojo.krl.KrlFileType.DAT;
            case "ini" -> tech.waitforu.pojo.krl.KrlFileType.INI;
            case "submit" -> tech.waitforu.pojo.krl.KrlFileType.SUBMIT;
            default -> tech.waitforu.pojo.krl.KrlFileType.NO_DEFINITION;
        };
    }


}
