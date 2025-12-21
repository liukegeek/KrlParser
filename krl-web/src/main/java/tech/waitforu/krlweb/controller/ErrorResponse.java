package tech.waitforu.krlweb.controller;

/**
 * ClassName: controller.tech.waitforu.krlweb.ErrorResponse
 * Package: tech.waitforu.krlweb.controller
 * Description: 统一错误响应。
 * Author: LiuKe
 * Create: 2025/12/21 19:22
 * Version 1.0
 */
public record ErrorResponse(int status, String message) {
}
