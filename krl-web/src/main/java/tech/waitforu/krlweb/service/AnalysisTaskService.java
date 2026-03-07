package tech.waitforu.krlweb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tech.waitforu.exception.KrlAnalysisException;
import tech.waitforu.krlweb.exception.BadRequestException;
import tech.waitforu.krlweb.exception.ConflictException;
import tech.waitforu.krlweb.exception.InternalServerException;
import tech.waitforu.krlweb.exception.NotFoundException;
import tech.waitforu.krlweb.exception.TooManyRequestsException;
import tech.waitforu.krlweb.config.ConfigStorageService;
import tech.waitforu.krlweb.config.KrlAnalysisProperties;
import tech.waitforu.krlweb.config.KrlStorageProperties;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.krl.RobotInfo;
import tech.waitforu.service.CallGraphExcelExportService;
import tech.waitforu.service.CarCallAnalysisService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 异步分析任务服务。
 * <p>
 * 负责完整承接“上传 zip -> 后台解析 -> 生成 JSON 与 Excel -> 提供查询与下载”的流程。
 * 该服务是服务器模式的核心：
 * 1. 避免长时间同步 HTTP 阻塞；
 * 2. 为前端提供任务轮询能力；
 * 3. 将中间文件和结果文件落盘，便于运维和定时清理。
 */
@Service
public class AnalysisTaskService {
    /** 统一 JSON 读写器。 */
    private final ObjectMapper objectMapper;
    /** 核心解析服务。 */
    private final CarCallAnalysisService carCallAnalysisService;
    /** Excel 导出服务。 */
    private final CallGraphExcelExportService callGraphExcelExportService;
    /** 配置文件解析服务。 */
    private final ConfigStorageService configStorageService;
    /** 存储路径配置。 */
    private final KrlStorageProperties storageProperties;
    /** 并发与任务数限制配置。 */
    private final KrlAnalysisProperties analysisProperties;
    /** 异步执行器。 */
    private final Executor analysisTaskExecutor;
    /** 内存中的任务索引。 */
    private final Map<String, AnalysisTaskRecord> taskStore = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisTaskService.class);

    /**
     * 构造任务服务。
     *
     * @param objectMapper               JSON 序列化器
     * @param carCallAnalysisService     调用关系分析服务
     * @param callGraphExcelExportService Excel 导出服务
     * @param configStorageService       配置解析服务
     * @param storageProperties          存储配置
     * @param analysisProperties         并发限制配置
     * @param analysisTaskExecutor       任务执行线程池
     */
    public AnalysisTaskService(ObjectMapper objectMapper,
                               CarCallAnalysisService carCallAnalysisService,
                               CallGraphExcelExportService callGraphExcelExportService,
                               ConfigStorageService configStorageService,
                               KrlStorageProperties storageProperties,
                               KrlAnalysisProperties analysisProperties,
                               @Qualifier("analysisTaskExecutor") Executor analysisTaskExecutor) {
        this.objectMapper = objectMapper;
        this.carCallAnalysisService = carCallAnalysisService;
        this.callGraphExcelExportService = callGraphExcelExportService;
        this.configStorageService = configStorageService;
        this.storageProperties = storageProperties;
        this.analysisProperties = analysisProperties;
        this.analysisTaskExecutor = analysisTaskExecutor;
    }

    /**
     * 初始化任务相关目录。
     *
     */
    @PostConstruct
    public void initializeDirectories() {
        try {
            Files.createDirectories(getTempRoot());
            Files.createDirectories(getResultRoot());
        } catch (IOException exception) {
            throw new IllegalStateException("初始化任务存储目录失败", exception);
        }
    }

    /**
     * 提交新的异步分析任务。
     * <p>
     * 提交流程分为两段：
     * 1. 在请求线程中完成输入校验、配置解析、上传文件落盘；
     * 2. 再交由后台线程做耗时解析与 Excel 生成。
     * <p>
     * 这样可以避免请求结束后 `MultipartFile` 失效，也可以让任务从提交开始就具备可查询状态。
     *
     * @param files      上传的 zip 文件集合
     * @param configText 前端临时配置文本
     * @return 新建任务的状态快照
     */
    public AnalysisTaskSnapshot submitTask(List<MultipartFile> files, String configText) {
        List<MultipartFile> uploadFiles = normalizeFiles(files);
        if (uploadFiles.isEmpty()) {
            throw new BadRequestException("请上传备份压缩包");
        }
        validateQueueCapacity();

        Config config = configStorageService.resolveConfig(configText);

        String taskId = UUID.randomUUID().toString().replace("-", "");
        Path tempTaskDir = getTempRoot().resolve(taskId);
        Path inputDir = tempTaskDir.resolve("input");
        Path resultTaskDir = getResultRoot().resolve(taskId);
        Path resultJsonPath = resultTaskDir.resolve("result.json");
        Path excelPath = resultTaskDir.resolve("call-graph.xlsx");

        List<StoredUploadFile> storedFiles = storeUploadFiles(uploadFiles, inputDir);
        AnalysisTaskRecord taskRecord = new AnalysisTaskRecord(taskId, storedFiles, tempTaskDir, resultTaskDir,
                resultJsonPath, excelPath);
        taskStore.put(taskId, taskRecord);
        LOGGER.info("已提交分析任务: taskId={}, fileCount={}, tempDir={}, resultDir={}",
                taskId, storedFiles.size(), tempTaskDir, resultTaskDir);

        analysisTaskExecutor.execute(() -> runTask(taskRecord, config));
        return taskRecord.toSnapshot();
    }

    /**
     * 查询任务快照。
     *
     * @param taskId 任务 ID
     * @return 当前任务快照
     */
    public AnalysisTaskSnapshot getTaskSnapshot(String taskId) {
        return getTask(taskId).toSnapshot();
    }

    /**
     * 读取指定任务的 JSON 结果。
     * <p>
     * 仅当任务成功完成后允许读取，避免前端在运行中拿到不完整数据。
     *
     * @param taskId 任务 ID
     * @return 机器人分析结果列表
     */
    public List<RobotInfo> getTaskResult(String taskId) {
        AnalysisTaskRecord taskRecord = getTask(taskId);
        ensureTaskSucceeded(taskRecord);
        if (taskRecord.result != null) {
            return taskRecord.result;
        }
        try {
            taskRecord.result = objectMapper.readValue(taskRecord.resultJsonPath.toFile(), new TypeReference<>() {
            });
            return taskRecord.result;
        } catch (IOException exception) {
            throw new InternalServerException("读取任务结果失败", exception);
        }
    }

    /**
     * 读取指定任务的 Excel 结果。
     *
     * @param taskId 任务 ID
     * @return Excel 文件二进制内容
     */
    public byte[] getTaskExcel(String taskId) {
        AnalysisTaskRecord taskRecord = getTask(taskId);
        ensureTaskSucceeded(taskRecord);
        if (!Files.exists(taskRecord.excelPath)) {
            throw new NotFoundException("当前任务尚未生成Excel文件");
        }
        try {
            return Files.readAllBytes(taskRecord.excelPath);
        } catch (IOException exception) {
            throw new InternalServerException("读取Excel文件失败", exception);
        }
    }

    /**
     * 定时清理已结束且超过保留时间的任务目录。
     * <p>
     * 清理策略只作用于成功或失败的终态任务，
     * 不会误删正在排队或执行中的任务。
     */
    @Scheduled(fixedDelayString = "${krl.storage.cleanup-interval:PT30M}")
    public void cleanupExpiredTasks() {
        Instant threshold = Instant.now().minus(storageProperties.getTaskRetention());
        taskStore.values().removeIf(taskRecord -> {
            if (!taskRecord.isTerminal()) {
                return false;
            }
            Instant finishedAt = taskRecord.finishedAt;
            if (finishedAt == null || finishedAt.isAfter(threshold)) {
                return false;
            }
            deleteDirectoryQuietly(taskRecord.tempTaskDir);
            deleteDirectoryQuietly(taskRecord.resultTaskDir);
            LOGGER.info("已清理过期任务: taskId={}", taskRecord.taskId);
            return true;
        });
    }

    /**
     * 真正执行后台分析任务。
     * <p>
     * 这里包含整个耗时核心链路：
     * 1. 调用 core 层解析多个备份；
     * 2. 序列化分析结果到磁盘；
     * 3. 生成 Excel 文件到结果目录；
     * 4. 更新最终任务状态。
     *
     * @param taskRecord 任务实体
     * @param config     已解析配置对象
     */
    private void runTask(AnalysisTaskRecord taskRecord, Config config) {
        taskRecord.status = AnalysisTaskState.RUNNING;
        taskRecord.startedAt = Instant.now();
        taskRecord.message = "正在解析备份文件";
        LOGGER.info("开始执行分析任务: taskId={}, fileCount={}",
                taskRecord.taskId, taskRecord.uploadFiles.size());
        try {
            Files.createDirectories(taskRecord.resultTaskDir);
            List<String> zipFilePathList = taskRecord.uploadFiles.stream()
                    .map(storedUploadFile -> storedUploadFile.path().toAbsolutePath().toString())
                    .toList();

            List<RobotInfo> robotInfoList = carCallAnalysisService.carInvocateAnalyzeBatch(zipFilePathList, config);
            taskRecord.result = robotInfoList;
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(taskRecord.resultJsonPath.toFile(), robotInfoList);

            taskRecord.message = "正在生成 Excel 文件";
            byte[] excelBytes = callGraphExcelExportService.export(robotInfoList);
            Files.write(taskRecord.excelPath, excelBytes);

            taskRecord.status = AnalysisTaskState.SUCCEEDED;
            taskRecord.message = "任务执行完成";
            taskRecord.finishedAt = Instant.now();
            LOGGER.info("分析任务执行成功: taskId={}, resultPath={}, excelPath={}",
                    taskRecord.taskId, taskRecord.resultJsonPath, taskRecord.excelPath);
        } catch (KrlAnalysisException | IllegalArgumentException exception) {
            taskRecord.status = AnalysisTaskState.FAILED;
            taskRecord.message = sanitizeTaskFailureMessage(exception.getMessage());
            taskRecord.finishedAt = Instant.now();
            LOGGER.warn("分析任务执行失败: taskId={}, message={}", taskRecord.taskId, taskRecord.message, exception);
        } catch (Exception exception) {
            taskRecord.status = AnalysisTaskState.FAILED;
            taskRecord.message = "任务执行失败，请查看服务端日志";
            taskRecord.finishedAt = Instant.now();
            LOGGER.error("分析任务执行失败: taskId={}", taskRecord.taskId, exception);
        }
    }

    /**
     * 归一化上传文件集合。
     *
     * @param files 原始上传文件列表
     * @return 非空且合法的文件列表
     */
    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .peek(this::validateZipFile)
                .toList();
    }

    /**
     * 校验当前活动任务数，避免小机器被瞬间打满。
     */
    private void validateQueueCapacity() {
        long activeTaskCount = taskStore.values().stream()
                .filter(taskRecord -> taskRecord.status == AnalysisTaskState.PENDING
                        || taskRecord.status == AnalysisTaskState.RUNNING)
                .count();
        if (activeTaskCount >= analysisProperties.getMaxActiveTasks()) {
            throw new TooManyRequestsException("当前排队任务过多，请稍后重试");
        }
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
     * 将上传文件保存到任务输入目录。
     * <p>
     * 由于任务是异步执行的，必须先把 MultipartFile 持久化到本地文件，
     * 后台线程再从这些路径继续处理。
     *
     * @param files    上传文件列表
     * @param inputDir 任务输入目录
     * @return 已保存文件的元数据列表
     */
    private List<StoredUploadFile> storeUploadFiles(List<MultipartFile> files, Path inputDir) {
        try {
            Files.createDirectories(inputDir);
            List<StoredUploadFile> storedFiles = new ArrayList<>();
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                String safeFilename = sanitizeFilename(file.getOriginalFilename(), index);
                Path destination = inputDir.resolve(safeFilename);
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                storedFiles.add(new StoredUploadFile(safeFilename, destination));
            }
            return storedFiles;
        } catch (IOException exception) {
            throw new InternalServerException("上传文件保存失败", exception);
        }
    }

    /**
     * 从任务仓库中读取任务。
     *
     * @param taskId 任务 ID
     * @return 任务实体
     */
    private AnalysisTaskRecord getTask(String taskId) {
        AnalysisTaskRecord taskRecord = taskStore.get(taskId);
        if (taskRecord == null) {
            throw new NotFoundException("任务不存在或已过期");
        }
        return taskRecord;
    }

    /**
     * 确保任务已经成功完成。
     *
     * @param taskRecord 任务实体
     */
    private void ensureTaskSucceeded(AnalysisTaskRecord taskRecord) {
        if (taskRecord.status == AnalysisTaskState.PENDING || taskRecord.status == AnalysisTaskState.RUNNING) {
            throw new ConflictException("任务尚未完成，请稍后重试");
        }
        if (taskRecord.status == AnalysisTaskState.FAILED) {
            throw new ConflictException(taskRecord.message != null ? taskRecord.message : "任务执行失败");
        }
    }

    /**
     * 生成安全文件名，避免原始文件名包含路径分隔符或为空。
     *
     * @param originalFilename 原始文件名
     * @param index            当前索引
     * @return 安全可写的文件名
     */
    private String sanitizeFilename(String originalFilename, int index) {
        String candidate = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "backup-" + (index + 1) + ".zip";
        String sanitized = candidate.replace('\\', '_').replace('/', '_');
        return sanitized.isBlank() ? "backup-" + (index + 1) + ".zip" : sanitized;
    }

    /**
     * 获取临时目录根路径。
     *
     * @return 临时目录根路径
     */
    private Path getTempRoot() {
        return Path.of(storageProperties.getTempDir()).toAbsolutePath().normalize();
    }

    /**
     * 获取结果目录根路径。
     *
     * @return 结果目录根路径
     */
    private Path getResultRoot() {
        return Path.of(storageProperties.getResultDir()).toAbsolutePath().normalize();
    }

    /**
     * 安静删除目录树。
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
                    LOGGER.warn("删除任务文件失败: {}", path, exception);
                }
            });
            LOGGER.debug("已清理任务目录: {}", directory);
        } catch (IOException exception) {
            LOGGER.warn("清理任务目录失败: {}", directory, exception);
        }
    }

    /**
     * 将底层异常消息收敛为用户可读文本，避免把实现细节直接暴露给前端。
     *
     * @param rawMessage 原始异常消息
     * @return 任务可读失败消息
     */
    private String sanitizeTaskFailureMessage(String rawMessage) {
        if (!StringUtils.hasText(rawMessage)) {
            return "任务执行失败，请检查输入文件和配置";
        }
        return rawMessage;
    }

    /**
     * 已保存上传文件的元数据。
     *
     * @param originalName 原始文件名
     * @param path         存储路径
     */
    private record StoredUploadFile(String originalName, Path path) {
    }

    /**
     * 任务内部实体。
     * <p>
     * 该对象只在服务内部使用，允许保存可变运行状态。
     */
    private static final class AnalysisTaskRecord {
        /** 任务 ID。 */
        private final String taskId;
        /** 上传文件列表。 */
        private final List<StoredUploadFile> uploadFiles;
        /** 临时任务目录。 */
        private final Path tempTaskDir;
        /** 结果任务目录。 */
        private final Path resultTaskDir;
        /** 结果 JSON 文件路径。 */
        private final Path resultJsonPath;
        /** Excel 文件路径。 */
        private final Path excelPath;
        /** 任务创建时间。 */
        private final Instant createdAt;
        /** 当前任务状态。 */
        private volatile AnalysisTaskState status;
        /** 当前阶段消息。 */
        private volatile String message;
        /** 开始执行时间。 */
        private volatile Instant startedAt;
        /** 完成时间。 */
        private volatile Instant finishedAt;
        /** 任务结果缓存。 */
        private volatile List<RobotInfo> result;

        /**
         * 构造任务实体。
         *
         * @param taskId         任务 ID
         * @param uploadFiles    上传文件列表
         * @param tempTaskDir    临时目录
         * @param resultTaskDir  结果目录
         * @param resultJsonPath JSON 路径
         * @param excelPath      Excel 路径
         */
        private AnalysisTaskRecord(String taskId,
                                   List<StoredUploadFile> uploadFiles,
                                   Path tempTaskDir,
                                   Path resultTaskDir,
                                   Path resultJsonPath,
                                   Path excelPath) {
            this.taskId = taskId;
            this.uploadFiles = List.copyOf(uploadFiles);
            this.tempTaskDir = tempTaskDir;
            this.resultTaskDir = resultTaskDir;
            this.resultJsonPath = resultJsonPath;
            this.excelPath = excelPath;
            this.createdAt = Instant.now();
            this.status = AnalysisTaskState.PENDING;
            this.message = "任务已创建，等待执行";
        }

        /**
         * 判断任务是否已经进入终态。
         *
         * @return true 表示成功或失败
         */
        private boolean isTerminal() {
            return status == AnalysisTaskState.SUCCEEDED || status == AnalysisTaskState.FAILED;
        }

        /**
         * 转换为对外可返回的只读快照。
         *
         * @return 任务快照
         */
        private AnalysisTaskSnapshot toSnapshot() {
            return new AnalysisTaskSnapshot(
                    taskId,
                    status,
                    message,
                    createdAt,
                    startedAt,
                    finishedAt,
                    uploadFiles.stream().map(StoredUploadFile::originalName).collect(Collectors.toList()),
                    status == AnalysisTaskState.SUCCEEDED && Files.exists(resultJsonPath),
                    status == AnalysisTaskState.SUCCEEDED && Files.exists(excelPath)
            );
        }
    }
}
