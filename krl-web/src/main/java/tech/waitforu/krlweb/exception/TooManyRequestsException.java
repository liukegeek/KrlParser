package tech.waitforu.krlweb.exception;

import org.springframework.http.HttpStatus;

/**
 * 429 请求过多异常。
 */
public class TooManyRequestsException extends ApiException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读消息
     */
    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读消息
     * @param cause   原始异常
     */
    public TooManyRequestsException(String message, Throwable cause) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, cause);
    }
}
