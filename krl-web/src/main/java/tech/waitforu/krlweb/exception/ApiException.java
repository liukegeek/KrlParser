package tech.waitforu.krlweb.exception;

import org.springframework.http.HttpStatus;

/**
 * Web 层 API 异常基类。
 * <p>
 * 统一承载 HTTP 状态码和用户可读消息，
 * 由全局异常处理器转换为标准 JSON 错误响应。
 */
public abstract class ApiException extends RuntimeException {
    /** 对应的 HTTP 状态码。 */
    private final HttpStatus status;

    /**
     * 使用状态码和消息构造异常。
     *
     * @param status  HTTP 状态码
     * @param message 用户可读消息
     */
    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * 使用状态码、消息和原始异常构造异常。
     *
     * @param status  HTTP 状态码
     * @param message 用户可读消息
     * @param cause   原始异常
     */
    protected ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * 获取 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public HttpStatus getStatus() {
        return status;
    }
}
