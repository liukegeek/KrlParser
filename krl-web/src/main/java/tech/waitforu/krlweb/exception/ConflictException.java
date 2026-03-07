package tech.waitforu.krlweb.exception;

import org.springframework.http.HttpStatus;

/**
 * 409 资源状态冲突异常。
 */
public class ConflictException extends ApiException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读消息
     */
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读消息
     * @param cause   原始异常
     */
    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, message, cause);
    }
}
