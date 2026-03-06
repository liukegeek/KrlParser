package tech.waitforu.krlweb.service;

import java.time.Instant;
import java.util.List;

/**
 * 分析任务对外快照。
 * <p>
 * 控制器通过该对象向前端返回任务状态，
 * 避免直接暴露内部可变任务实体。
 *
 * @param taskId       任务 ID
 * @param status       当前状态
 * @param message      当前阶段说明
 * @param createdAt    创建时间
 * @param startedAt    开始执行时间
 * @param finishedAt   结束时间
 * @param archiveNames 本次任务对应的上传文件名列表
 * @param resultReady  是否可读取 JSON 结果
 * @param excelReady   是否可下载 Excel 结果
 */
public record AnalysisTaskSnapshot(String taskId,
                                   AnalysisTaskState status,
                                   String message,
                                   Instant createdAt,
                                   Instant startedAt,
                                   Instant finishedAt,
                                   List<String> archiveNames,
                                   boolean resultReady,
                                   boolean excelReady) {
}
