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
import org.springframework.web.server.ResponseStatusException;
import tech.waitforu.krlweb.config.ConfigStorageService;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.krl.RobotInfo;
import tech.waitforu.service.CallGraphExcelExportService;
import tech.waitforu.service.CarCallAnalysisService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    /** KRL 调用关系分析服务。 */
    private final CarCallAnalysisService carCallAnalysisService;
    /** 配置文件存储与解析服务。 */
    private final ConfigStorageService configStorageService;
    /** 调用关系 Excel 导出服务。 */
    private final CallGraphExcelExportService callGraphExcelExportService;

    /**
     * 构造控制器并注入依赖服务。
     *
     * @param carCallAnalysisService    调用关系分析服务
     * @param configStorageService      配置管理服务
     * @param callGraphExcelExportService Excel 导出服务
     */
    public AnalysisController(CarCallAnalysisService carCallAnalysisService,
                              ConfigStorageService configStorageService,
                              CallGraphExcelExportService callGraphExcelExportService) {
        this.carCallAnalysisService = carCallAnalysisService;
        this.configStorageService = configStorageService;
        this.callGraphExcelExportService = callGraphExcelExportService;
    }

    /**
     * 获取当前生效配置文件信息。
     *
     * @return 包含配置文件磁盘路径和配置内容的响应对象
     */
    @GetMapping("/config")
    public ConfigResponse getConfig() {
        try {
            return new ConfigResponse(configStorageService.getConfigPathText(), configStorageService.getConfigContent());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取配置文件失败", exception);
        }
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
        return executeAnalysis(files, singleFile, configText);
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
        List<RobotInfo> robotInfoList = executeAnalysis(files, singleFile, configText);
        if (robotInfoList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未生成可导出的调用关系");
        }

        byte[] excelBytes;
        try {
            excelBytes = callGraphExcelExportService.export(robotInfoList);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "生成Excel失败", exception);
        }

        String filename = "调用关系表.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(excelBytes);
    }

    /**
     * 执行统一分析流程（JSON/Excel 两个接口共用）。
     * <p>
     * 流程说明：
     * 1. 合并并校验上传文件；
     * 2. 解析配置（优先前端配置文本）；
     * 3. 将 MultipartFile 落地为临时 zip；
     * 4. 调用批量分析服务；
     * 5. finally 中清理临时文件。
     *
     * @param files 多文件上传字段
     * @param singleFile 单文件上传字段
     * @param configText 配置文本（可选）
     * @return 解析后的机器人信息列表
     */
    private List<RobotInfo> executeAnalysis(
            List<MultipartFile> files,
            MultipartFile singleFile,
            String configText
    ) {
        List<MultipartFile> backupFiles = new ArrayList<>();
        // 先收集多文件字段，过滤掉空项与空文件。
        if (files != null) {
            files.stream().filter(file -> file != null && !file.isEmpty()).forEach(backupFiles::add);
        }
        // 再兼容旧字段的单文件上传。
        if (singleFile != null && !singleFile.isEmpty()) {
            backupFiles.add(singleFile);
        }
        if (backupFiles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传备份压缩包");
        }

        Config config;
        try {
            // 优先解析请求内配置文本；为空时回退磁盘配置。
            config = configStorageService.resolveConfig(configText);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配置内容无效，请检查YAML格式", exception);
        }

        List<Path> tempFiles = new ArrayList<>();
        try {
            // 将上传文件写入临时目录，供 core 层按本地 zip 路径处理。
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
            // 无论解析成功或失败，都尝试清理临时文件。
            for (Path tempFile : tempFiles) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // 忽略临时文件清理失败
                }
            }
        }
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
