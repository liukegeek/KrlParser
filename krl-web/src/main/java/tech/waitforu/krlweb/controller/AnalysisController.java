package tech.waitforu.krlweb.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tech.waitforu.service.CarCallAnalysisResult;
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
    public CarCallAnalysisResult analyze(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传备份压缩包");
        }

        Path tempFile;
        try {
            tempFile = Files.createTempFile("krl-backup-", ".zip");
            file.transferTo(tempFile);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "上传文件失败", exception);
        }

        try {
            return carCallAnalysisService.analyze(tempFile.toString());
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // 忽略临时文件清理失败
            }
        }
    }
}
