package tech.waitforu.krlweb.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tech.waitforu.pojo.krl.RobotInfo;
import tech.waitforu.service.CarCallAnalysisService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api")
public class AnalysisController {
    private final CarCallAnalysisService carCallAnalysisService;

    public AnalysisController(CarCallAnalysisService carCallAnalysisService) {
        this.carCallAnalysisService = carCallAnalysisService;
    }

    @PostMapping(value = "/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RobotInfo analyze(@RequestPart("file") MultipartFile file,
                             @RequestPart("config") MultipartFile configFile) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传备份压缩包");
        }
        if (configFile == null || configFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传配置文件");
        }

        Path tempFile;
        Path tempConfigFile;
        try {
            tempFile = Files.createTempFile("krl-backup-", ".zip");
            file.transferTo(tempFile);
            tempConfigFile = Files.createTempFile("krl-config-", ".yml");
            configFile.transferTo(tempConfigFile);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "上传文件失败", exception);
        }

        try {
            //调用服务分析机器人调用图
            return carCallAnalysisService.analyze(tempFile.toString(), tempConfigFile.toString());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "解析失败", exception);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
                Files.deleteIfExists(tempConfigFile);
            } catch (IOException ignored) {
                // 忽略临时文件清理失败
            }
        }
    }
}
