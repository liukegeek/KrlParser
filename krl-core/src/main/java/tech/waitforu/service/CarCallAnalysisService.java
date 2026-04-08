package tech.waitforu.service;

import tech.waitforu.exception.KrlInputException;
import tech.waitforu.loader.KrlZipLoader;
import tech.waitforu.loader.YamlConfigLoad;
import tech.waitforu.parser.CarCallReferenceAnalyze;
import tech.waitforu.parser.IniParser;
import tech.waitforu.parser.ModuleRepository;
import tech.waitforu.pojo.carcallgraph.CallNode;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.config.ConfigValidator;
import tech.waitforu.pojo.config.RobotInfoConfig;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.RobotInfo;
import tech.waitforu.rule.IgnoreRuleByStr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 车型调用关系分析服务。
 * <p>
 * 该服务负责串联完整离线分析流程：
 * 1. 加载并解释配置规则；
 * 2. 读取 zip 备份中的 KRL 文件；
 * 3. 构建模块仓库；
 * 4. 从 cell 程序出发构建调用关系树；
 * 5. 解析机器人基础信息并组装最终结果。
 */
public class CarCallAnalysisService {
    /**
     * 按配置文件路径分析单个备份文件。
     *
     * @param zipFilePath 备份 zip 绝对路径
     * @param configFilePath 配置文件绝对路径
     * @return 单个机器人的解析结果
     */
    public RobotInfo carInvocateAnalyze(String zipFilePath, String configFilePath) {
        YamlConfigLoad yamlConfigLoad = new YamlConfigLoad(configFilePath);
        return carInvocateAnalyze(zipFilePath, yamlConfigLoad.loadConfig());
    }

    /**
     * 按配置对象分析单个备份文件。
     *
     * @param zipFilePath 备份 zip 绝对路径
     * @param config 解析配置对象
     * @return 单个机器人的解析结果
     */
    public RobotInfo carInvocateAnalyze(String zipFilePath, Config config) {
        ConfigValidator.validate(config);
        // 1) 从配置对象中提取两类规则：文件加载规则、调用过滤规则。
        IgnoreRuleByStr fileLoadRule = new IgnoreRuleByStr("fileLoadSection", config.getFileLoadSection());
        IgnoreRuleByStr carInvokerParseRule = new IgnoreRuleByStr("carInvokerParseSection", config.getCarInvokerParseSection());
        RobotInfoConfig robotInfoConfig = config.getRobotInfoConfig();

        // 2) 按 fileLoadRule 遍历 zip，读取参与分析的文件内容与元信息。
        KrlZipLoader krlZipLoader = new KrlZipLoader(zipFilePath, fileLoadRule);
        List<KrlFile> krlFileList = krlZipLoader.getKrlFileList();

        // 3) 将 src/dat 聚合成模块，构建后续可检索的模块仓库。
        ModuleRepository moduleRepository = new ModuleRepository();
        moduleRepository.assembleFromFileList(krlFileList);

        // 4) 从 cell 程序出发解析调用链，生成调用图根节点。
        CarCallReferenceAnalyze carCallReferenceAnalyze = new CarCallReferenceAnalyze(moduleRepository, carInvokerParseRule);
        CallNode callGraphRoot = carCallReferenceAnalyze.analyze();


        // 5) 读取机器人信息文件 am.ini，用于补充机器人元数据。
        if (robotInfoConfig == null || robotInfoConfig.getFilePath() == null) {
            throw new KrlInputException("机器人信息文件路径不能为空");
        }
        KrlFile robotInfoFile = krlZipLoader.getFile(robotInfoConfig.getFilePath());
        if (robotInfoFile == null) {
            throw new KrlInputException("机器人信息文件不存在: " + robotInfoConfig.getFilePath());
        }
        IniParser iniParser;
        try {
            iniParser = new IniParser(robotInfoFile.getContent());
        } catch (IOException exception) {
            throw new KrlInputException("机器人信息文件格式无效: " + robotInfoConfig.getFilePath(), exception);
        }

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

    /**
     * 按配置文件路径批量分析多个备份文件。
     *
     * @param zipFilePathList 备份 zip 路径列表
     * @param configFilePath 配置文件路径
     * @return 机器人结果列表（顺序与输入路径一致）
     */
    public List<RobotInfo> carInvocateAnalyzeBatch(List<String> zipFilePathList, String configFilePath) {
        YamlConfigLoad yamlConfigLoad = new YamlConfigLoad(configFilePath);
        return carInvocateAnalyzeBatch(zipFilePathList, yamlConfigLoad.loadConfig());
    }

    /**
     * 按配置对象批量分析多个备份文件。
     *
     * @param zipFilePathList 备份 zip 路径列表
     * @param config 配置对象
     * @return 机器人结果列表；输入为空时返回空列表
     */
    public List<RobotInfo> carInvocateAnalyzeBatch(List<String> zipFilePathList, Config config) {
        if (zipFilePathList == null || zipFilePathList.isEmpty()) {
            return List.of();
        }
        List<RobotInfo> result = new ArrayList<>(zipFilePathList.size());
        // 顺序遍历每个备份，保证输出顺序与输入一致，便于前端映射。
        for (String zipFilePath : zipFilePathList) {
            result.add(carInvocateAnalyze(zipFilePath, config));
        }
        return result;
    }
}
