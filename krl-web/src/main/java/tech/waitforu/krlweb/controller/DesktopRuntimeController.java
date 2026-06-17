package tech.waitforu.krlweb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.waitforu.krlweb.config.DesktopModeGuard;
import tech.waitforu.krlweb.service.ApplicationShutdownService;

/** 桌面模式运行时操作接口。 */
@RestController
@RequestMapping("/api/runtime")
public class DesktopRuntimeController {
    private final DesktopModeGuard desktopModeGuard;
    private final ApplicationShutdownService shutdownService;

    public DesktopRuntimeController(DesktopModeGuard desktopModeGuard,
                                    ApplicationShutdownService shutdownService) {
        this.desktopModeGuard = desktopModeGuard;
        this.shutdownService = shutdownService;
    }

    @PostMapping("/shutdown")
    public ResponseEntity<ShutdownResponse> shutdown() {
        desktopModeGuard.requireDesktop("程序退出");
        boolean scheduled = shutdownService.scheduleShutdown();
        String status = scheduled ? "accepted" : "already_scheduled";
        return ResponseEntity.accepted().body(new ShutdownResponse(status, "程序正在退出"));
    }

    public record ShutdownResponse(String status, String message) {
    }
}
