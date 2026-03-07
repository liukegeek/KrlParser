package tech.waitforu.exception;

/**
 * 输入数据不合法或备份内容缺失时抛出的异常。
 */
public class KrlInputException extends KrlAnalysisException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读的异常消息
     */
    public KrlInputException(String message) {
        super(message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读的异常消息
     * @param cause   原始异常
     */
    public KrlInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
