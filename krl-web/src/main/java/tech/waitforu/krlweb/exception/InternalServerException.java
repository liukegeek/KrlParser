package tech.waitforu.krlweb.exception;

import org.springframework.http.HttpStatus;

/**
 * 500 服务器内部异常。
 */
public class InternalServerException extends ApiException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读消息
     */
    public InternalServerException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读消息
     * @param cause   原始异常
     */
    public InternalServerException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
