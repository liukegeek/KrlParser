package tech.waitforu.krlweb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tech.waitforu.krlweb.config.ConfigStorageService;
import tech.waitforu.krlweb.exception.BadRequestException;
import tech.waitforu.krlweb.exception.InternalServerException;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.krl.RobotInfo;
import tech.waitforu.service.CallGraphExcelExportService;
import tech.waitforu.service.CarCallAnalysisService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 同步分析执行服务。
 * <p>
 * 统一承接桌面模式下的同步分析与 Excel 导出流程，
 * 让控制器只负责参数接收和响应封装。
 */
@Service
public class AnalysisExecutionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisExecutionService.class);

    /** 核心解析服务。 */
    private final CarCallAnalysisService carCallAnalysisService;
    /** Excel 导出服务。 */
    private final CallGraphExcelExportService callGraphExcelExportService;
    /** 配置解析服务。 */
    private final ConfigStorageService configStorageService;

    /**
     * 构造同步分析执行服务。
     *
     * @param carCallAnalysisService      调用关系分析服务
     * @param callGraphExcelExportService Excel 导出服务
     * @param configStorageService        配置服务
     */
    public AnalysisExecutionService(CarCallAnalysisService carCallAnalysisService,
                                    CallGraphExcelExportService callGraphExcelExportService,
                                    ConfigStorageService configStorageService) {
        this.carCallAnalysisService = carCallAnalysisService;
        this.callGraphExcelExportService = callGraphExcelExportService;
        this.configStorageService = configStorageService;
    }

    /**
     * 同步分析上传的备份文件。
     *
     * @param files      上传文件列表
     * @param configText 临时配置文本
     * @return 机器人分析结果列表
     */
    public List<RobotInfo> analyze(List<MultipartFile> files, String configText) {
        List<MultipartFile> uploadFiles = normalizeFiles(files);
        Config config = configStorageService.resolveConfig(configText);

        Path tempDirectory = createTempDirectory();
        List<Path> storedFiles = storeUploadFiles(uploadFiles, tempDirectory);

        try {
            LOGGER.info("开始同步分析: fileCount={}, tempDir={}", storedFiles.size(), tempDirectory);
            List<RobotInfo> result = carCallAnalysisService.carInvocateAnalyzeBatch(
                    storedFiles.stream().map(path -> path.toAbsolutePath().toString()).toList(),
                    config
            );
            LOGGER.info("同步分析完成: fileCount={}, robotCount={}", storedFiles.size(), result.size());
            return result;
        } finally {
            deleteDirectoryQuietly(tempDirectory);
        }
    }

    /**
     * 基于同步分析结果导出 Excel。
     *
     * @param files      上传文件列表
     * @param configText 临时配置文本
     * @return Excel 文件字节数组
     */
    public byte[] exportExcel(List<MultipartFile> files, String configText) {
        List<RobotInfo> robotInfoList = analyze(files, configText);
        if (robotInfoList.isEmpty()) {
            throw new BadRequestException("未生成可导出的调用关系");
        }
        LOGGER.info("开始导出同步分析 Excel: robotCount={}", robotInfoList.size());
        return callGraphExcelExportService.export(robotInfoList);
    }

    /**
     * 归一化上传文件列表。
     *
     * @param files 原始文件集合
     * @return 有效文件集合
     */
    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null) {
            throw new BadRequestException("请上传备份压缩包");
        }

        List<MultipartFile> uploadFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (uploadFiles.isEmpty()) {
            throw new BadRequestException("请上传备份压缩包");
        }
        uploadFiles.forEach(this::validateZipFile);
        return uploadFiles;
    }

    /**
     * 校验上传文件必须为 zip。
     *
     * @param file 上传文件
     */
    private void validateZipFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase().endsWith(".zip")) {
            throw new BadRequestException("仅支持上传 .zip 备份文件");
        }
    }

    /**
     * 创建同步分析临时目录。
     *
     * @return 临时目录路径
     */
    private Path createTempDirectory() {
        try {
            return Files.createTempDirectory("krl-sync-");
        } catch (IOException exception) {
            throw new InternalServerException("创建临时目录失败", exception);
        }
    }

    /**
     * 将上传文件保存到临时目录。
     *
     * @param files       上传文件列表
     * @param tempDirectory 临时目录
     * @return 已保存的文件路径列表
     */
    private List<Path> storeUploadFiles(List<MultipartFile> files, Path tempDirectory) {
        List<Path> storedFiles = new ArrayList<>(files.size());
        try {
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                String safeFilename = sanitizeFilename(file.getOriginalFilename(), index);
                Path destination = tempDirectory.resolve(safeFilename);
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                storedFiles.add(destination);
            }
            return storedFiles;
        } catch (IOException exception) {
            throw new InternalServerException("上传文件失败", exception);
        }
    }

    /**
     * 生成安全文件名。
     *
     * @param originalFilename 原始文件名
     * @param index            文件索引
     * @return 安全文件名
     */
    private String sanitizeFilename(String originalFilename, int index) {
        String candidate = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "backup-" + (index + 1) + ".zip";
        String sanitized = candidate.replace('\\', '_').replace('/', '_');
        return sanitized.isBlank() ? "backup-" + (index + 1) + ".zip" : sanitized;
    }

    /**
     * 安静删除临时目录，同时记录失败日志。
     *
     * @param directory 目标目录
     */
    private void deleteDirectoryQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> pathStream = Files.walk(directory)) {
            pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    LOGGER.warn("删除同步分析临时文件失败: {}", path, exception);
                }
            });
        } catch (IOException exception) {
            LOGGER.warn("清理同步分析临时目录失败: {}", directory, exception);
        }
    }
}
