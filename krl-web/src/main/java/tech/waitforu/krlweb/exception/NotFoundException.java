package tech.waitforu.krlweb.exception;

import org.springframework.http.HttpStatus;

/**
 * 404 资源不存在异常。
 */
public class NotFoundException extends ApiException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读消息
     */
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读消息
     * @param cause   原始异常
     */
    public NotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, message, cause);
    }
}
