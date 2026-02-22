package tech.waitforu.krlweb.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全局异常处理器。
 * <p>
 * 统一将服务端异常转换为结构化 JSON 错误响应，
 * 避免前端只拿到默认 HTML 错误页。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理显式抛出的 HTTP 状态异常。
     *
     * @param exception 业务层抛出的状态异常
     * @return 标准错误响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        int statusCode = status != null ? status.value() : HttpStatus.INTERNAL_SERVER_ERROR.value();
        return ResponseEntity.status(statusCode)
                .body(new ErrorResponse(statusCode, exception.getReason()));
    }

    /**
     * 兜底处理未捕获异常。
     *
     * @param exception 任意未捕获异常
     * @return 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage()));
    }
}
