package tech.waitforu.krlweb.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.waitforu.exception.KrlConfigException;
import tech.waitforu.exception.KrlExportException;
import tech.waitforu.exception.KrlInputException;
import tech.waitforu.exception.KrlParseException;
import tech.waitforu.krlweb.exception.ApiException;

/**
 * 全局异常处理器。
 * <p>
 * 统一将服务端异常转换为结构化 JSON 错误响应，
 * 避免前端只拿到默认 HTML 错误页。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 Web 层语义化异常。
     *
     * @param exception API 异常
     * @param request   当前请求
     * @return 标准错误响应
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        HttpStatus status = exception.getStatus();
        logMappedException(status, request, exception.getMessage(), exception);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), exception.getMessage()));
    }

    /**
     * 处理配置、输入和解析阶段的业务异常。
     *
     * @param exception 业务异常
     * @param request   当前请求
     * @return 400 错误响应
     */
    @ExceptionHandler({KrlConfigException.class, KrlInputException.class, KrlParseException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestException(RuntimeException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logMappedException(status, request, exception.getMessage(), exception);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), exception.getMessage()));
    }

    /**
     * 处理导出异常。
     *
     * @param exception 导出异常
     * @param request   当前请求
     * @return 500 错误响应
     */
    @ExceptionHandler(KrlExportException.class)
    public ResponseEntity<ErrorResponse> handleExportException(KrlExportException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        logMappedException(status, request, exception.getMessage(), exception);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), exception.getMessage()));
    }

    /**
     * 兜底处理未捕获异常。
     *
     * @param exception 任意未捕获异常
     * @param request   当前请求
     * @return 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "服务器内部错误，请查看日志";
        logMappedException(status, request, message, exception);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message));
    }

    /**
     * 统一记录异常日志。
     *
     * @param status  映射后的 HTTP 状态码
     * @param request 当前请求
     * @param message 对外消息
     * @param cause   原始异常
     */
    private void logMappedException(HttpStatus status, HttpServletRequest request, String message, Exception cause) {
        String requestUri = request != null ? request.getRequestURI() : "N/A";
        if (status.is4xxClientError()) {
            LOGGER.warn("请求处理失败: status={}, uri={}, message={}", status.value(), requestUri, message);
        } else {
            LOGGER.error("请求处理失败: status={}, uri={}, message={}", status.value(), requestUri, message, cause);
        }
    }
}
