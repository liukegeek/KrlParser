package tech.waitforu.krlweb.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.waitforu.krlweb.service.AnalysisTaskService;
import tech.waitforu.krlweb.service.AnalysisTaskSnapshot;
import tech.waitforu.pojo.krl.RobotInfo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 异步分析任务控制器。
 * <p>
 * 提供完整的任务化接口：
 * 1. 提交任务；
 * 2. 查询任务状态；
 * 3. 获取分析结果；
 * 4. 下载 Excel 文件。
 */
@RestController
@RequestMapping("/api/analysis/tasks")
public class AnalysisTaskController {
    /** 异步任务服务。 */
    private final AnalysisTaskService analysisTaskService;

    /**
     * 构造任务控制器。
     *
     * @param analysisTaskService 异步任务服务
     */
    public AnalysisTaskController(AnalysisTaskService analysisTaskService) {
        this.analysisTaskService = analysisTaskService;
    }

    /**
     * 提交新的分析任务。
     * <p>
     * 兼容前端两种上传字段：
     * 1. `files` 多文件；
     * 2. `file` 单文件。
     *
     * @param files      多文件字段
     * @param singleFile 单文件字段
     * @param configText 临时配置文本
     * @return 任务快照
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisTaskSnapshot createTask(@RequestPart(value = "files", required = false) List<MultipartFile> files,
                                           @RequestPart(value = "file", required = false) MultipartFile singleFile,
                                           @RequestPart(value = "configText", required = false) String configText) {
        return analysisTaskService.submitTask(mergeFiles(files, singleFile), configText);
    }

    /**
     * 查询任务状态。
     *
     * @param taskId 任务 ID
     * @return 任务快照
     */
    @GetMapping("/{taskId}")
    public AnalysisTaskSnapshot getTask(@PathVariable("taskId") String taskId) {
        return analysisTaskService.getTaskSnapshot(taskId);
    }

    /**
     * 获取已完成任务的 JSON 结果。
     *
     * @param taskId 任务 ID
     * @return 分析结果列表
     */
    @GetMapping("/{taskId}/result")
    public List<RobotInfo> getTaskResult(@PathVariable("taskId") String taskId) {
        return analysisTaskService.getTaskResult(taskId);
    }

    /**
     * 下载已完成任务的 Excel 文件。
     *
     * @param taskId 任务 ID
     * @return Excel 二进制附件
     */
    @GetMapping("/{taskId}/excel")
    public ResponseEntity<byte[]> downloadExcel(@PathVariable("taskId") String taskId) {
        byte[] excelBytes = analysisTaskService.getTaskExcel(taskId);
        String filename = URLEncoder.encode("调用关系表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(excelBytes);
    }

    /**
     * 合并上传文件，兼容单文件与多文件前端。
     *
     * @param files      多文件字段
     * @param singleFile 单文件字段
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
}
