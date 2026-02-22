package tech.waitforu.pojo.krl;

import tech.waitforu.pojo.carcallgraph.CallNode;

import java.util.List;

/**
 * 机器人分析结果模型。
 * <p>
 * 包含机器人元信息以及调用关系图根节点。
 */
public class RobotInfo {
    private String robotName; // 机器人名称
    private String archiveName; // 备份名称
    private String archiveDate; // 备份日期
    private String version; // 版本号
    private List<String> techPackList;  //安装的软件包
    private CallNode callGraphRoot;

    /**
     * 构造机器人信息对象。
     *
     * @param robotName 机器人名称
     * @param archiveName 备份名称
     * @param archiveDate 备份日期
     * @param version 控制系统版本
     * @param techPackList 已安装软件包列表
     * @param callGraphRoot 调用关系图根节点
     */
    public RobotInfo(String robotName, String archiveName, String archiveDate, String version, List<String> techPackList, CallNode callGraphRoot) {
        this.robotName = robotName;
        this.archiveName = archiveName;
        this.archiveDate = archiveDate;
        this.techPackList = techPackList;
        this.version = version;
        this.callGraphRoot = callGraphRoot;
    }

    /**
     * 获取机器人名称。
     *
     * @return 机器人名称
     */
    public String getRobotName() {
        return robotName;
    }

    /**
     * 获取备份名称。
     *
     * @return 备份名称
     */
    public String getArchiveName() {
        return archiveName;
    }

    /**
     * 获取备份日期。
     *
     * @return 备份日期
     */
    public String getArchiveDate() {
        return archiveDate;
    }

    /**
     * 获取软件包列表。
     *
     * @return TechPack 列表
     */
    public List<String> getTechPackList() {
        return techPackList;
    }

    /**
     * 获取系统版本。
     *
     * @return 版本字符串
     */
    public String getVersion() {
        return version;
    }

    /**
     * 获取调用关系图根节点。
     *
     * @return 调用图根节点
     */
    public CallNode getCallGraphRoot() {
        return callGraphRoot;
    }

    /**
     * 设置机器人名称。
     *
     * @param robotName 机器人名称
     */
    public void setRobotName(String robotName) {
        this.robotName = robotName;
    }

    /**
     * 设置备份名称。
     *
     * @param archiveName 备份名称
     */
    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    /**
     * 设置备份日期。
     *
     * @param archiveDate 备份日期
     */
    public void setArchiveDate(String archiveDate) {
        this.archiveDate = archiveDate;
    }

    /**
     * 设置系统版本。
     *
     * @param version 版本字符串
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 设置软件包列表。
     *
     * @param techPackList 软件包列表
     */
    public void setTechPackList(List<String> techPackList) {
        this.techPackList = techPackList;
    }

    /**
     * 设置调用关系图根节点。
     *
     * @param callGraphRoot 调用图根节点
     */
    public void setCallGraphRoot(CallNode callGraphRoot) {
        this.callGraphRoot = callGraphRoot;
    }
}
