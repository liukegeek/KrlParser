package tech.waitforu.krlweb.exception;

import org.springframework.http.HttpStatus;

/**
 * 400 请求异常。
 */
public class BadRequestException extends ApiException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读消息
     */
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读消息
     * @param cause   原始异常
     */
    public BadRequestException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }
}
