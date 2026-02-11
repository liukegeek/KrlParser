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
import java.util.ArrayList;
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
        return carInvocateAnalyze(zipFilePath, yamlConfigLoad.loadConfig());
    }

    public RobotInfo carInvocateAnalyze(String zipFilePath, Config config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("配置不能为空");
        }
        // 从Config中解析出相关规则信息
        IgnoreRuleByStr fileLoadRule = new IgnoreRuleByStr(config.getFileLoadSection());
        IgnoreRuleByStr carInvokerParseRule = new IgnoreRuleByStr(config.getCarInvokerParseSection());
        RobotInfoConfig robotInfoConfig = config.getRobotInfoConfig();

        // 按加载规则(fileLoadRule)加载zip备份中的各文件。
        KrlZipLoader krlZipLoader = new KrlZipLoader(zipFilePath, fileLoadRule);
        List<KrlFile> krlFileList = krlZipLoader.getKrlFileList();

        // 从KRL文件列表中构建模块仓库(moduleRepository)。
        ModuleRepository moduleRepository = new ModuleRepository();
        moduleRepository.assembleFromFileList(krlFileList);

        // 从模块仓库(moduleRepository)中分析出车型调用关系，返回调用关系图的根节点(callGraphRoot)。
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

        // 从机器人信息文件中解析出机器人信息。
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
        YamlConfigLoad yamlConfigLoad = new YamlConfigLoad(configFilePath);
        return carInvocateAnalyzeBatch(zipFilePathList, yamlConfigLoad.loadConfig());
    }

    public List<RobotInfo> carInvocateAnalyzeBatch(List<String> zipFilePathList, Config config) throws IOException {
        if (zipFilePathList == null || zipFilePathList.isEmpty()) {
            return List.of();
        }
        List<RobotInfo> result = new ArrayList<>(zipFilePathList.size());
        for (String zipFilePath : zipFilePathList) {
            result.add(carInvocateAnalyze(zipFilePath, config));
        }
        return result;
    }
}
