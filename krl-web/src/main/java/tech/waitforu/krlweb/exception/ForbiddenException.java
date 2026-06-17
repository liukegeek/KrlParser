package tech.waitforu.krlweb.exception;

import org.springframework.http.HttpStatus;

/** 403 禁止访问异常。 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
