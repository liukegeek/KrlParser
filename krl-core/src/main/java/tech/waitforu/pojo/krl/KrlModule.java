package tech.waitforu.pojo.krl;

/**
 * KRL 模块实体。
 * <p>
 * 一个模块由同名的 src/dat 文件组成（dat 可缺失）。
 */
public class KrlModule {
    /** 模块名（通常为文件名，不含后缀）。 */
    private String moduleName;
    /** 模块 src 文件。 */
    private KrlFile moduleSrcFile;
    /** 模块 dat 文件。 */
    private KrlFile moduleDatFile;

    /**
     * 使用模块名构造模块对象。
     *
     * @param name 模块名
     */
    public KrlModule(String name) {
        this.moduleName = name;
    }

    /**
     * 获取模块名。
     *
     * @return 模块名
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * 获取 src 文件对象。
     *
     * @return src 文件
     */
    public KrlFile getModuleSrcFile() {
        return moduleSrcFile;
    }

    /**
     * 获取 dat 文件对象。
     *
     * @return dat 文件
     */
     public KrlFile getModuleDatFile() {
        return moduleDatFile;
    }

    /**
     * 设置模块名。
     *
     * @param moduleName 模块名
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * 设置 src 文件对象。
     *
     * @param moduleSrcFile src 文件
     */
    public void setModuleSrcFile(KrlFile moduleSrcFile) {
        this.moduleSrcFile = moduleSrcFile;
    }

    /**
     * 设置 dat 文件对象。
     *
     * @param moduleDatFile dat 文件
     */
    public void setModuleDatFile(KrlFile moduleDatFile) {
        this.moduleDatFile = moduleDatFile;
    }

    /**
     * 获取 src 文件路径。
     *
     * @return src 文件路径
     */
    public String getModuleSrcFilePath() {
        return moduleSrcFile.getPath();
    }

    /**
     * 获取 src 文件内容。
     *
     * @return src 文本内容
     */
    public String getSrcContent(){
        return moduleSrcFile.getContent();
    }

    /**
     * 获取 dat 文件内容。
     *
     * @return dat 文本内容
     */
    public String getDatContent(){
        return moduleDatFile.getContent();
    }
}
