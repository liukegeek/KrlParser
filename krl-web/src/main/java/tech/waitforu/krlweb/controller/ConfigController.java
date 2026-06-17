package tech.waitforu.krlweb.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.waitforu.krlweb.config.ConfigStorageService;
import tech.waitforu.krlweb.config.DesktopModeGuard;

/** 配置文件维护接口。 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final ConfigStorageService configStorageService;
    private final DesktopModeGuard desktopModeGuard;

    public ConfigController(ConfigStorageService configStorageService,
                            DesktopModeGuard desktopModeGuard) {
        this.configStorageService = configStorageService;
        this.desktopModeGuard = desktopModeGuard;
    }

    @PostMapping("/reset")
    public ConfigResetResponse resetConfig() {
        desktopModeGuard.requireDesktop("配置初始化");
        String content = configStorageService.resetToDefault();
        return new ConfigResetResponse(configStorageService.getConfigPathText(), content);
    }

    public record ConfigResetResponse(String configPath, String content) {
    }
}
