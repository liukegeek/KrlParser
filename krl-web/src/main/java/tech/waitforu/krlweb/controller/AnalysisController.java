package tech.waitforu.krlweb.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tech.waitforu.krlweb.config.ConfigStorageService;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.krl.RobotInfo;
import tech.waitforu.service.CarCallAnalysisService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 提供配置读取与 KRL 解析分析的 HTTP 接口。
 */
@RestController
@RequestMapping("/api")
public class AnalysisController {
    private final CarCallAnalysisService carCallAnalysisService;
    private final ConfigStorageService configStorageService;

    public AnalysisController(CarCallAnalysisService carCallAnalysisService,
                              ConfigStorageService configStorageService) {
        this.carCallAnalysisService = carCallAnalysisService;
        this.configStorageService = configStorageService;
    }

    @GetMapping("/config")
    public ConfigResponse getConfig() {
        try {
            return new ConfigResponse(configStorageService.getConfigPathText(), configStorageService.getConfigContent());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取配置文件失败", exception);
        }
    }

    /**
     * 支持 files 多文件或 file 单文件上传；configText 存在时覆盖磁盘配置。
     */
    @PostMapping(value = "/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<RobotInfo> analyze(@RequestPart(value = "files", required = false) List<MultipartFile> files,
                                   @RequestPart(value = "file", required = false) MultipartFile singleFile,
                                   @RequestPart(value = "configText", required = false) String configText) {
        List<MultipartFile> backupFiles = new ArrayList<>();
        if (files != null) {
            files.stream().filter(file -> file != null && !file.isEmpty()).forEach(backupFiles::add);
        }
        if (singleFile != null && !singleFile.isEmpty()) {
            backupFiles.add(singleFile);
        }
        if (backupFiles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传备份压缩包");
        }

        Config config;
        try {
            config = configStorageService.resolveConfig(configText);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配置内容无效，请检查YAML格式", exception);
        }

        List<Path> tempFiles = new ArrayList<>();
        try {
            for (MultipartFile backupFile : backupFiles) {
                Path tempFile = Files.createTempFile("krl-backup-", ".zip");
                backupFile.transferTo(tempFile);
                tempFiles.add(tempFile);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "上传文件失败", exception);
        }

        try {
            List<String> zipPathList = tempFiles.stream().map(path -> path.toAbsolutePath().toString()).toList();
            return carCallAnalysisService.carInvocateAnalyzeBatch(zipPathList, config);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "解析失败", exception);
        } finally {
            for (Path tempFile : tempFiles) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // 忽略临时文件清理失败
                }
            }
        }
    }

    public record ConfigResponse(String configPath, String content) {
    }
}
