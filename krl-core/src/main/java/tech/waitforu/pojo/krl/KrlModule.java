package tech.waitforu.pojo.krl;

/**
 * ClassName: krl.pojo.tech.waitforu.KrlModule
 * Package: tech.waitforu.pojo.krl
 * Description:
 * Author: LiuKe
 * Create: 2025/12/11 15:57
 * Version 1.0
 */
public class KrlModule {
    private String moduleName;
    private KrlFile moduleSrcFile;
    private KrlFile moduleDatFile;

    public KrlModule(String name) {
        this.moduleName = name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public KrlFile getModuleSrcFile() {
        return moduleSrcFile;
    }
     public KrlFile getModuleDatFile() {
        return moduleDatFile;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setModuleSrcFile(KrlFile moduleSrcFile) {
        this.moduleSrcFile = moduleSrcFile;
    }

    public void setModuleDatFile(KrlFile moduleDatFile) {
        this.moduleDatFile = moduleDatFile;
    }

    public String getModuleSrcFilePath() {
        return moduleSrcFile.getPath();
    }

    public String getSrcContent(){
        return moduleSrcFile.getContent();
    }

    public String getDatContent(){
        return moduleDatFile.getContent();
    }
}
