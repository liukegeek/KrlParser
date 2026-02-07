package tech.waitforu.service;

import tech.waitforu.loader.KrlZipLoader;
import tech.waitforu.loader.YamlConfigLoad;
import tech.waitforu.parser.CarCallReferenceAnalyze;
import tech.waitforu.parser.IniParser;
import tech.waitforu.parser.ModuleRepository;
import tech.waitforu.pojo.carcallgraph.CallNode;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.config.RobotInfoConfig;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.RobotInfo;
import tech.waitforu.rule.IgnoreRuleByStr;

import java.io.IOException;
import java.util.List;

/**
 * ClassName: service.tech.waitforu.CarCallAnalysisService
 * Package: tech.waitforu.service
 * Description: 解析KRL备份文件并构建车型调用关系。
 * Author: LiuKe
 * Create: 2025/12/21 19:22
 * Version 1.0
 */
public class CarCallAnalysisService {
    public RobotInfo carInvocateAnalyze(String zipFilePath, String configFilePath) throws IOException {
        YamlConfigLoad yamlConfigLoad = new YamlConfigLoad(configFilePath);
        Config config = yamlConfigLoad.loadConfig();

        IgnoreRuleByStr fileLoadRule = new IgnoreRuleByStr(config.getFileLoadSection());
        IgnoreRuleByStr carInvokerParseRule = new IgnoreRuleByStr(config.getCarInvokerParseSection());
        RobotInfoConfig robotInfoConfig = config.getRobotInfo();

        KrlZipLoader krlZipLoader = new KrlZipLoader(zipFilePath, fileLoadRule);
        List<KrlFile> krlFileList = krlZipLoader.getKrlFileList();

        ModuleRepository moduleRepository = new ModuleRepository();
        moduleRepository.assembleFromFileList(krlFileList);

        CarCallReferenceAnalyze carCallReferenceAnalyze = new CarCallReferenceAnalyze(moduleRepository, carInvokerParseRule);
        CallNode callGraphRoot = carCallReferenceAnalyze.analyze();


        //获取机器人信息文件
        if (robotInfoConfig == null || robotInfoConfig.getFilePath() == null) {
            throw new IllegalArgumentException("机器人信息文件路径不能为空");
        }
        KrlFile robotInfoFile = krlZipLoader.getFile(robotInfoConfig.getFilePath());
        if (robotInfoFile == null) {
            throw new IllegalArgumentException("机器人信息文件不存在");
        }
        IniParser iniParser = new IniParser(robotInfoFile.getContent());
        return new RobotInfo(
                iniParser.get("Roboter", "RobName"),
                iniParser.get("Archive", "Name"),
                iniParser.get("Archive", "Date"),
                iniParser.get("Version", "Version"),
                List.of(iniParser.get("TechPacks", "TechPacks").split("\\|")),
                callGraphRoot
        );
    }

    public List<RobotInfo> carInvocateAnalyzeBatch(List<String> zipFilePathList, String configFilePath) throws IOException {
        return zipFilePathList.stream().map(zipFilePath -> {
            try {
                return carInvocateAnalyze(zipFilePath, configFilePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
