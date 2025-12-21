package tech.waitforu.service;

import tech.waitforu.pojo.carcallgraph.CallNode;
import tech.waitforu.pojo.krl.RobotInfo;

import java.util.List;

/**
 * ClassName: service.tech.waitforu.CarCallAnalysisResult
 * Package: tech.waitforu.service
 * Description: 车型调用关系分析结果。
 * Author: LiuKe
 * Create: 2025/12/21 19:22
 * Version 1.0
 */
public record CarCallAnalysisResult(
        RobotInfo robotInfo,
        CallNode callGraph,
        List<String> moduleNames,
        List<String> callableNames
) {
}
