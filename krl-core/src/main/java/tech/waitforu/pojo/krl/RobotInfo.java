package tech.waitforu.pojo.krl;

import tech.waitforu.pojo.carcallgraph.CallNode;

import java.util.List;

/**
 * ClassName: config.pojo.tech.waitforu.RobotInfoConfig
 * Package: tech.waitforu.pojo.krl
 * Description: 机器人信息
 * Author: LiuKe
 * Create: 2025/12/12 09:23
 * Version 1.0
 */
public class RobotInfo {
    private String robotName; // 机器人名称
    private String archiveName; // 备份名称
    private String archiveDate; // 备份日期
    private String version; // 版本号
    private List<String> techPackList;  //安装的软件包
    private CallNode callGraphRoot;

    public RobotInfo(String robotName, String archiveName, String archiveDate, String version, List<String> techPackList, CallNode callGraphRoot) {
        this.robotName = robotName;
        this.archiveName = archiveName;
        this.archiveDate = archiveDate;
        this.techPackList = techPackList;
        this.version = version;
        this.callGraphRoot = callGraphRoot;
    }

    public String getRobotName() {
        return robotName;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public String getArchiveDate() {
        return archiveDate;
    }

    public List<String> getTechPackList() {
        return techPackList;
    }

    public String getVersion() {
        return version;
    }

    public CallNode getCallGraphRoot() {
        return callGraphRoot;
    }

    public void setRobotName(String robotName) {
        this.robotName = robotName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public void setArchiveDate(String archiveDate) {
        this.archiveDate = archiveDate;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setTechPackList(List<String> techPackList) {
        this.techPackList = techPackList;
    }

    public void setCallGraphRoot(CallNode callGraphRoot) {
        this.callGraphRoot = callGraphRoot;
    }
}
