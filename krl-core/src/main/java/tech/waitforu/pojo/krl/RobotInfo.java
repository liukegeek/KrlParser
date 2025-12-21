package tech.waitforu.pojo.krl;

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
    private final String robotName; // 机器人名称
    private final String archiveName; // 备份名称
    private final String archiveDate; // 备份日期
    private final String version; // 版本号
    private final List<String> techPackList;  //安装的软件包

    public RobotInfo(String robotName, String archiveName, String archiveDate,String version, List<String> techPackList) {
        this.robotName = robotName;
        this.archiveName = archiveName;
        this.archiveDate = archiveDate;
        this.techPackList = techPackList;
        this.version = version;
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
}
