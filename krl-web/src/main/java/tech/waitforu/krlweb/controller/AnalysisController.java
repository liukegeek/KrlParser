package tech.waitforu.krlweb.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.waitforu.krlweb.config.ConfigStorageService;
import tech.waitforu.krlweb.service.AnalysisExecutionService;
import tech.waitforu.pojo.krl.RobotInfo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 分析控制器。
 * <p>
 * 统一对外提供：
 * 1. 配置读取接口；
 * 2. 备份解析接口（返回 JSON）；
 * 3. 解析并导出 Excel 接口（返回二进制流）。
 */
@RestController
@RequestMapping("/api")
public class AnalysisController {
    /** 配置文件存储与解析服务。 */
    private final ConfigStorageService configStorageService;
    /** 同步分析执行服务。 */
    private final AnalysisExecutionService analysisExecutionService;

    /**
     * 构造控制器并注入依赖服务。
     *
     * @param configStorageService      配置管理服务
     * @param analysisExecutionService  同步分析执行服务
     */
    public AnalysisController(ConfigStorageService configStorageService,
                              AnalysisExecutionService analysisExecutionService) {
        this.configStorageService = configStorageService;
        this.analysisExecutionService = analysisExecutionService;
    }

    /**
     * 获取当前生效配置文件信息。
     *
     * @return 包含配置文件磁盘路径和配置内容的响应对象
     */
    @GetMapping("/config")
    public ConfigResponse getConfig() {
        return new ConfigResponse(configStorageService.getConfigPathText(), configStorageService.getConfigContent());
    }

    /**
     * 分析备份并返回调用关系 JSON。
     * <p>
     * 支持两种上传方式：
     * 1. {@code files} 多文件；
     * 2. {@code file} 单文件（兼容旧前端）。
     * <p>
     * 若传入 {@code configText}，优先使用前端传入配置；否则使用磁盘配置。
     *
     * @param files 多文件上传字段
     * @param singleFile 单文件上传字段
     * @param configText 前端配置文本（可选）
     * @return 机器人调用关系列表
     */
    @PostMapping(value = "/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<RobotInfo> analyze(@RequestPart(value = "files", required = false) List<MultipartFile> files,
                                   @RequestPart(value = "file", required = false) MultipartFile singleFile,
                                   @RequestPart(value = "configText", required = false) String configText) {
        return analysisExecutionService.analyze(mergeFiles(files, singleFile), configText);
    }

    /**
     * 分析备份并直接导出 Excel 文件。
     *
     * @param files 多文件上传字段
     * @param singleFile 单文件上传字段
     * @param configText 前端配置文本（可选）
     * @return Excel 字节流响应（附件下载）
     */
    @PostMapping(value = "/analysis/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> analyzeAndExportExcel(
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart(value = "file", required = false) MultipartFile singleFile,
            @RequestPart(value = "configText", required = false) String configText
    ) {
        byte[] excelBytes = analysisExecutionService.exportExcel(mergeFiles(files, singleFile), configText);

        String filename = "调用关系表.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(excelBytes);
    }

    /**
     * @param files 多文件上传字段
     * @param singleFile 单文件上传字段
     * @return 合并后的文件列表
     */
    private List<MultipartFile> mergeFiles(List<MultipartFile> files, MultipartFile singleFile) {
        List<MultipartFile> mergedFiles = new ArrayList<>();
        if (files != null) {
            mergedFiles.addAll(files);
        }
        if (singleFile != null) {
            mergedFiles.add(singleFile);
        }
        return mergedFiles;
    }

    /**
     * 配置查询响应体。
     *
     * @param configPath 配置文件绝对路径
     * @param content 配置文件文本内容
     */
    public record ConfigResponse(String configPath, String content) {
    }
}
