package tech.waitforu.exception;

/**
 * 导出调用关系产物时抛出的异常。
 */
public class KrlExportException extends KrlAnalysisException {

    /**
     * 使用异常消息构造异常。
     *
     * @param message 用户可读的异常消息
     */
    public KrlExportException(String message) {
        super(message);
    }

    /**
     * 使用异常消息和原始异常构造异常。
     *
     * @param message 用户可读的异常消息
     * @param cause   原始异常
     */
    public KrlExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
