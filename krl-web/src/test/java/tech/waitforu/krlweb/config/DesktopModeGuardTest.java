package tech.waitforu.krlweb.config;

import org.junit.jupiter.api.Test;
import tech.waitforu.krlweb.exception.ForbiddenException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DesktopModeGuardTest {

    @Test
    void shouldAllowOperationInDesktopMode() {
        DesktopModeGuard guard = new DesktopModeGuard(new KrlRuntimeProperties());

        assertDoesNotThrow(() -> guard.requireDesktop("配置初始化"));
    }

    @Test
    void shouldRejectOperationInServerMode() {
        KrlRuntimeProperties properties = new KrlRuntimeProperties();
        properties.setMode(RuntimeMode.SERVER);
        DesktopModeGuard guard = new DesktopModeGuard(properties);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> guard.requireDesktop("配置初始化")
        );

        assertEquals("配置初始化仅在桌面模式下可用", exception.getMessage());
    }
}
