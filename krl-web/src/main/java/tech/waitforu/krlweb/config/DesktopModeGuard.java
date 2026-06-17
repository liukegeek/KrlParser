package tech.waitforu.krlweb.config;

import org.springframework.stereotype.Component;
import tech.waitforu.krlweb.exception.ForbiddenException;

/** 保护只允许桌面模式执行的操作。 */
@Component
public class DesktopModeGuard {
    private final KrlRuntimeProperties runtimeProperties;

    public DesktopModeGuard(KrlRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    public void requireDesktop(String operationName) {
        if (runtimeProperties.isServerMode()) {
            throw new ForbiddenException(operationName + "仅在桌面模式下可用");
        }
    }
}
