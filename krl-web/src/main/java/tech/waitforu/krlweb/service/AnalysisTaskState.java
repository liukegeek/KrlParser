package tech.waitforu.krlweb.service;

/**
 * 分析任务状态枚举。
 * <p>
 * 用于描述一个异步分析任务在整个生命周期中的阶段。
 */
public enum AnalysisTaskState {
    /** 已提交，尚未开始执行。 */
    PENDING,
    /** 正在执行解析或导出逻辑。 */
    RUNNING,
    /** 已成功完成。 */
    SUCCEEDED,
    /** 已执行失败。 */
    FAILED
}
