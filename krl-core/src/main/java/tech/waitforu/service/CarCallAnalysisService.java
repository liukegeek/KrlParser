package tech.waitforu.service;

import org.ini4j.Ini;
import tech.waitforu.loader.KrlZipLoader;
import tech.waitforu.parser.CarCallReferenceAnalyze;
import tech.waitforu.parser.ModuleRepository;
import tech.waitforu.pojo.carcallgraph.CallNode;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.config.RobotInfoConfig;
import tech.waitforu.pojo.config.StrRuleConfig;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.RobotInfo;
import tech.waitforu.rule.IgnoreRuleByStr;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ClassName: service.tech.waitforu.CarCallAnalysisService
 * Package: tech.waitforu.service
 * Description: 解析KRL备份文件并构建车型调用关系。
 * Author: LiuKe
 * Create: 2025/12/21 19:22
 * Version 1.0
 */
public class CarCallAnalysisService {
    private static final String DEFAULT_CONFIG_RESOURCE = "/config.yml";
    private final ObjectMapper yamlMapper;

    public CarCallAnalysisService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public CarCallAnalysisResult analyze(String zipFilePath) {
        Config config = loadConfig();
        IgnoreRuleByStr fileLoadRule = new IgnoreRuleByStr(config.getFileLoadSection());
        IgnoreRuleByStr invokerParseRule = new IgnoreRuleByStr(config.getInvokerParseSection());

        KrlZipLoader krlZipLoader = new KrlZipLoader(zipFilePath, fileLoadRule);
        List<KrlFile> krlFileList = krlZipLoader.getKrlFileList();

        ModuleRepository moduleRepository = new ModuleRepository();
        moduleRepository.assembleFromFileList(krlFileList);

        CarCallReferenceAnalyze carCallReferenceAnalyze = new CarCallReferenceAnalyze(moduleRepository, invokerParseRule);
        CallNode callGraph = carCallReferenceAnalyze.analyze();
        RobotInfo robotInfo = parseRobotInfo(krlZipLoader, config.getRobotInfo());

        return new CarCallAnalysisResult(
                robotInfo,
                callGraph,
                moduleRepository.getModuleNameList(),
                moduleRepository.getCallableNameList()
        );
    }

    private Config loadConfig() {
        Config config = new Config();
        try (var inputStream = CarCallAnalysisService.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (inputStream != null) {
                config = yamlMapper.readValue(inputStream, Config.class);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("读取默认配置失败: " + DEFAULT_CONFIG_RESOURCE, exception);
        }

        if (config.getFileLoadSection() == null) {
            config.setFileLoadSection(new StrRuleConfig());
        }
        if (config.getInvokerParseSection() == null) {
            config.setInvokerParseSection(new StrRuleConfig());
        }
        if (config.getRobotInfo() == null) {
            RobotInfoConfig robotInfoConfig = new RobotInfoConfig();
            robotInfoConfig.setFilePath("/am.ini");
            config.setRobotInfo(robotInfoConfig);
        }

        return config;
    }

    private RobotInfo parseRobotInfo(KrlZipLoader krlZipLoader, RobotInfoConfig robotInfoConfig) {
        if (robotInfoConfig == null || robotInfoConfig.getFilePath() == null) {
            return null;
        }

        KrlFile robotInfoFile = krlZipLoader.getFile(robotInfoConfig.getFilePath());
        if (robotInfoFile == null) {
            return null;
        }

        try {
            Ini ini = new Ini(new StringReader(robotInfoFile.getContent()));
            String robotName = fallbackValue(
                    readIniValue(ini, "Robot", "RobName", "RobotName", "Name"),
                    readIniValue(ini, "KRC", "RobName", "RobotName")
            );
            String archiveName = fallbackValue(
                    readIniValue(ini, "Archive", "Name", "ID"),
                    readIniValue(ini, "General", "ArchiveName")
            );
            String archiveDate = fallbackValue(
                    readIniValue(ini, "Archive", "Date", "CreateDate", "CreationDate"),
                    readIniValue(ini, "General", "ArchiveDate")
            );
            String version = fallbackValue(
                    readIniValue(ini, "KRC", "Version", "KSS", "KSS_Version"),
                    readIniValue(ini, "Archive", "KSS")
            );

            List<String> techPackList = readTechPackList(ini);

            return new RobotInfo(
                    normalizeValue(robotName),
                    normalizeValue(archiveName),
                    normalizeValue(archiveDate),
                    normalizeValue(version),
                    techPackList
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("解析机器人信息失败: " + robotInfoConfig.getFilePath(), exception);
        }
    }

    private List<String> readTechPackList(Ini ini) {
        List<String> techPackList = new ArrayList<>();
        Optional.ofNullable(ini.get("TechPacks"))
                .ifPresent(section -> section.keySet().forEach(techPackList::add));
        Optional.ofNullable(ini.get("TechPack"))
                .ifPresent(section -> section.keySet().forEach(techPackList::add));
        Optional.ofNullable(ini.get("TechPacksInstalled"))
                .ifPresent(section -> section.keySet().forEach(techPackList::add));
        return techPackList;
    }

    private String readIniValue(Ini ini, String section, String... keys) {
        if (ini.get(section) == null) {
            return null;
        }
        for (String key : keys) {
            String value = ini.get(section, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String fallbackValue(String primary, String fallback) {
        return primary != null ? primary : fallback;
    }

    private String normalizeValue(String value) {
        return value == null || value.isBlank() ? "未知" : value.trim();
    }
}
