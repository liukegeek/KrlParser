package tech.waitforu.exception;

/**
 * KRL 语法或调用关系解析异常。
 */
public class KrlParseException extends KrlAnalysisException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读的异常消息
     */
    public KrlParseException(String message) {
        super(message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读的异常消息
     * @param cause   原始异常
     */
    public KrlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
