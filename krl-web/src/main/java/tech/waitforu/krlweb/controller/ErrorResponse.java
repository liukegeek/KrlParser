package tech.waitforu.krlweb.controller;

/**
 * 统一错误响应模型。
 *
 * @param status HTTP 状态码
 * @param message 错误描述信息
 */
public record ErrorResponse(int status, String message) {
}
