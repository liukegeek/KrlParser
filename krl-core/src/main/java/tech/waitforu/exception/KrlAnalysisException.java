package tech.waitforu.exception;

/**
 * KRL 解析业务异常基类。
 * <p>
 * Core 层所有需要传递到 Web 层统一处理的异常都应继承该类型，
 * 以便在边界层做一致的 HTTP 映射和日志记录。
 */
public class KrlAnalysisException extends RuntimeException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读的异常消息
     */
    public KrlAnalysisException(String message) {
        super(message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读的异常消息
     * @param cause   原始异常
     */
    public KrlAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
