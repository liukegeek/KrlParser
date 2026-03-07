package tech.waitforu.exception;

/**
 * KRL 配置相关异常。
 */
public class KrlConfigException extends KrlAnalysisException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读的异常消息
     */
    public KrlConfigException(String message) {
        super(message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读的异常消息
     * @param cause   原始异常
     */
    public KrlConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
