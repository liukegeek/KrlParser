package tech.waitforu.pojo;


/**
 * ClassName: Config
 * Package: tech.waitforu.pojo
 * Description:
 * Author: LiuKe
 * Create: 2025/12/10 14:51
 * Version 1.0
 */
public class Config {
    public FileSectionConfig fileLoadSection;
    public FileSectionConfig fileParseSection;

    public FileSectionConfig getFileLoadSection() {
        return fileLoadSection;
    }

    public void setFileLoadSection(FileSectionConfig fileLoadSection) {
        this.fileLoadSection = fileLoadSection;
    }

    public FileSectionConfig getFileParseSection() {
        return fileParseSection;
    }

    public void setFileParseSection(FileSectionConfig fileParseSection) {
        this.fileParseSection = fileParseSection;
    }
}

