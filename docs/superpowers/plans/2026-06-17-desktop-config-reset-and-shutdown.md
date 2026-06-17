# Desktop Config Reset and Shutdown Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add desktop-only controls that restore the persisted configuration from the bundled default and gracefully terminate the Java application.

**Architecture:** A shared desktop-mode guard protects both mutation endpoints. `ConfigStorageService` performs failure-safe replacement of `Config.yml`; a dedicated shutdown service schedules Spring context closure and JVM termination after the HTTP response is accepted. Static HTML/JavaScript exposes both controls only after runtime mode detection.

**Tech Stack:** Java 21, Spring Boot 3.4.2, Spring MVC, JUnit 5, Mockito, MockMvc, vanilla HTML/CSS/JavaScript, Maven.

---

## File map

- Create `krl-web/src/main/java/tech/waitforu/krlweb/exception/ForbiddenException.java`: standard HTTP 403 API exception.
- Create `krl-web/src/main/java/tech/waitforu/krlweb/config/DesktopModeGuard.java`: one backend policy boundary for desktop-only operations.
- Create `krl-web/src/test/java/tech/waitforu/krlweb/config/DesktopModeGuardTest.java`: mode policy regression tests.
- Modify `krl-web/src/main/java/tech/waitforu/krlweb/config/ConfigStorageService.java`: reset the persisted file from the bundled resource using a temporary file and replace move.
- Create `krl-web/src/test/java/tech/waitforu/krlweb/config/ConfigStorageServiceTest.java`: reset success and failure-safety tests.
- Create `krl-web/src/main/java/tech/waitforu/krlweb/controller/ConfigController.java`: desktop-only `POST /api/config/reset` endpoint.
- Create `krl-web/src/test/java/tech/waitforu/krlweb/controller/ConfigControllerTest.java`: endpoint response, access control, and no-side-effect tests.
- Create `krl-web/src/main/java/tech/waitforu/krlweb/service/ApplicationShutdownService.java`: idempotent delayed context close and process exit.
- Create `krl-web/src/test/java/tech/waitforu/krlweb/service/ApplicationShutdownServiceTest.java`: ordering and idempotency tests without terminating the test JVM.
- Create `krl-web/src/main/java/tech/waitforu/krlweb/controller/DesktopRuntimeController.java`: desktop-only `POST /api/runtime/shutdown` endpoint.
- Create `krl-web/src/test/java/tech/waitforu/krlweb/controller/DesktopRuntimeControllerTest.java`: endpoint status and server-mode rejection tests.
- Create `krl-web/src/test/java/tech/waitforu/krlweb/DesktopControlsResourceTest.java`: static-resource regression coverage for the new controls and mode gating.
- Modify `krl-web/src/main/resources/static/index.html`: add reset and shutdown buttons.
- Modify `krl-web/src/main/resources/static/js/app.js`: bind reset/shutdown flows and desktop-only visibility.
- Modify `krl-web/src/main/resources/static/css/styles.css`: add a danger button treatment.
- Modify `README.md`: document the desktop-only operations.

### Task 1: Enforce desktop-only backend operations

**Files:**
- Create: `krl-web/src/main/java/tech/waitforu/krlweb/exception/ForbiddenException.java`
- Create: `krl-web/src/main/java/tech/waitforu/krlweb/config/DesktopModeGuard.java`
- Create: `krl-web/src/test/java/tech/waitforu/krlweb/config/DesktopModeGuardTest.java`
- Modify: `krl-web/src/test/java/tech/waitforu/krlweb/controller/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write failing mode guard and exception mapping tests**

Create `DesktopModeGuardTest`:

```java
package tech.waitforu.krlweb.config;

import org.junit.jupiter.api.Test;
import tech.waitforu.krlweb.exception.ForbiddenException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DesktopModeGuardTest {

    @Test
    void shouldAllowOperationInDesktopMode() {
        KrlRuntimeProperties properties = new KrlRuntimeProperties();
        DesktopModeGuard guard = new DesktopModeGuard(properties);

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
```

Extend `GlobalExceptionHandlerTest.ExceptionThrowingController` with `GET /test/forbidden` that throws `new ForbiddenException("禁止操作")`, and add an assertion for status `403`, JSON status `403`, and message `禁止操作`.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```bash
mvn -pl krl-web -am -Dtest=DesktopModeGuardTest,GlobalExceptionHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because `DesktopModeGuard` and `ForbiddenException` do not exist.

- [ ] **Step 3: Implement the 403 exception and desktop guard**

Create `ForbiddenException`:

```java
package tech.waitforu.krlweb.exception;

import org.springframework.http.HttpStatus;

/** 403 禁止访问异常。 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(HttpStatus.FORBIDDEN, message, cause);
    }
}
```

Create `DesktopModeGuard`:

```java
package tech.waitforu.krlweb.config;

import org.springframework.stereotype.Component;
import tech.waitforu.krlweb.exception.ForbiddenException;

/** 集中保护只允许桌面模式执行的操作。 */
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
```

- [ ] **Step 4: Run focused tests and verify GREEN**

Run the command from Step 2.

Expected: both test classes pass with zero failures.

- [ ] **Step 5: Commit the policy boundary**

```bash
git add krl-web/src/main/java/tech/waitforu/krlweb/exception/ForbiddenException.java \
  krl-web/src/main/java/tech/waitforu/krlweb/config/DesktopModeGuard.java \
  krl-web/src/test/java/tech/waitforu/krlweb/config/DesktopModeGuardTest.java \
  krl-web/src/test/java/tech/waitforu/krlweb/controller/GlobalExceptionHandlerTest.java
git commit -m "feat: 限制桌面专属操作"
```

### Task 2: Reset the persisted configuration safely

**Files:**
- Modify: `krl-web/src/main/java/tech/waitforu/krlweb/config/ConfigStorageService.java`
- Create: `krl-web/src/test/java/tech/waitforu/krlweb/config/ConfigStorageServiceTest.java`

- [ ] **Step 1: Write failing reset service tests**

Create tests in the same package so they can use a package-private test constructor accepting `Path` and `Resource`:

```java
package tech.waitforu.krlweb.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import tech.waitforu.krlweb.exception.InternalServerException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigStorageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void shouldReplaceExistingConfigWithBundledDefault() throws IOException {
        Path configPath = tempDirectory.resolve("Config.yml");
        Files.writeString(configPath, "old: value\n", StandardCharsets.UTF_8);
        String defaultContent = "default: value\n";
        ConfigStorageService service = new ConfigStorageService(
                configPath,
                new ByteArrayResource(defaultContent.getBytes(StandardCharsets.UTF_8))
        );

        String result = service.resetToDefault();

        assertEquals(defaultContent, result);
        assertEquals(defaultContent, Files.readString(configPath, StandardCharsets.UTF_8));
    }

    @Test
    void shouldPreserveExistingConfigWhenDefaultResourceCannotBeRead() throws IOException {
        Path configPath = tempDirectory.resolve("Config.yml");
        String existingContent = "keep: this\n";
        Files.writeString(configPath, existingContent, StandardCharsets.UTF_8);
        ConfigStorageService service = new ConfigStorageService(configPath, new AbstractResource() {
            @Override
            public String getDescription() {
                return "failing default config";
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("read failed");
            }
        });

        assertThrows(InternalServerException.class, service::resetToDefault);
        assertEquals(existingContent, Files.readString(configPath, StandardCharsets.UTF_8));
    }

    @Test
    void shouldPreserveExistingConfigWhenReplacementFails() throws IOException {
        Path configPath = tempDirectory.resolve("Config.yml");
        String existingContent = "keep: this\n";
        Files.writeString(configPath, existingContent, StandardCharsets.UTF_8);
        ConfigStorageService service = new ConfigStorageService(
                configPath,
                new ByteArrayResource("default: value\n".getBytes(StandardCharsets.UTF_8)),
                (source, target) -> {
                    throw new IOException("replace failed");
                }
        );

        assertThrows(InternalServerException.class, service::resetToDefault);
        assertEquals(existingContent, Files.readString(configPath, StandardCharsets.UTF_8));
        try (var files = Files.list(tempDirectory)) {
            assertEquals(1L, files.count(), "失败后必须删除临时文件");
        }
    }
}
```

- [ ] **Step 2: Run the service test and verify RED**

Run:

```bash
mvn -pl krl-web -am -Dtest=ConfigStorageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because the test constructor and `resetToDefault()` do not exist.

- [ ] **Step 3: Implement resource injection and atomic replacement**

Modify `ConfigStorageService` to hold the resource and replacement strategy:

```java
private final Path configPath;
private final Resource defaultConfigResource;
private final ConfigFileReplacer configFileReplacer;
```

Keep the Spring constructor and delegate to testable constructors:

```java
public ConfigStorageService(@Value("${krl.config.path:${user.home}/.KrlParser/Config.yml}") String configPathValue) {
    this(Path.of(configPathValue).toAbsolutePath().normalize(), new ClassPathResource("config.yml"));
}

ConfigStorageService(Path configPath, Resource defaultConfigResource) {
    this(configPath, defaultConfigResource, ConfigStorageService::replaceConfigFile);
}

ConfigStorageService(Path configPath,
                     Resource defaultConfigResource,
                     ConfigFileReplacer configFileReplacer) {
    this.configPath = configPath.toAbsolutePath().normalize();
    this.defaultConfigResource = defaultConfigResource;
    this.configFileReplacer = configFileReplacer;
}
```

Add these methods and imports for `Resource`, `AtomicMoveNotSupportedException`, `StandardOpenOption`, and `StandardCopyOption`:

```java
public String resetToDefault() {
    Path temporaryFile = null;
    try {
        createConfigDirectory();
        byte[] defaultContent = readDefaultConfig();
        temporaryFile = Files.createTempFile(
                configPath.getParent(),
                configPath.getFileName().toString(),
                ".tmp"
        );
        Files.write(temporaryFile, defaultContent,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        configFileReplacer.replace(temporaryFile, configPath);
        LOGGER.info("已将配置文件恢复为内置默认配置: {}", configPath);
        return new String(defaultContent, StandardCharsets.UTF_8);
    } catch (IOException exception) {
        throw new InternalServerException("初始化配置文件失败", exception);
    } finally {
        if (temporaryFile != null) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException exception) {
                LOGGER.warn("清理配置临时文件失败: {}", temporaryFile, exception);
            }
        }
    }
}

private void createConfigDirectory() throws IOException {
    Path parent = configPath.getParent();
    if (parent != null) {
        Files.createDirectories(parent);
    }
}

private byte[] readDefaultConfig() throws IOException {
    if (!defaultConfigResource.exists()) {
        throw new IOException("默认配置文件 config.yml 不存在");
    }
    try (InputStream inputStream = defaultConfigResource.getInputStream()) {
        return inputStream.readAllBytes();
    }
}

private static void replaceConfigFile(Path temporaryFile, Path configPath) throws IOException {
    try {
        Files.move(temporaryFile, configPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporaryFile, configPath, StandardCopyOption.REPLACE_EXISTING);
    }
}

@FunctionalInterface
interface ConfigFileReplacer {
    void replace(Path source, Path target) throws IOException;
}
```

Refactor `ensureConfigFileExists()` to preserve its existing create-only behavior while sharing the injected resource:

```java
private void ensureConfigFileExists() throws IOException {
    createConfigDirectory();
    if (Files.exists(configPath)) {
        return;
    }
    if (!defaultConfigResource.exists()) {
        throw new IOException("默认配置文件 config.yml 不存在");
    }
    try (InputStream inputStream = defaultConfigResource.getInputStream()) {
        Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("已创建默认配置文件: {}", configPath);
    }
}
```

- [ ] **Step 4: Run the reset tests and verify GREEN**

Run the command from Step 2.

Expected: all three tests pass; both failure cases leave `keep: this` unchanged and replacement failure leaves no temporary file.

- [ ] **Step 5: Run existing configuration regressions**

```bash
mvn -pl krl-web -am -Dtest=ConfigStorageServiceTest,YamlConfigLoadTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all selected tests pass.

- [ ] **Step 6: Commit safe reset storage**

```bash
git add krl-web/src/main/java/tech/waitforu/krlweb/config/ConfigStorageService.java \
  krl-web/src/test/java/tech/waitforu/krlweb/config/ConfigStorageServiceTest.java
git commit -m "feat: 支持安全初始化配置文件"
```

### Task 3: Expose the desktop-only configuration reset endpoint

**Files:**
- Create: `krl-web/src/main/java/tech/waitforu/krlweb/controller/ConfigController.java`
- Create: `krl-web/src/test/java/tech/waitforu/krlweb/controller/ConfigControllerTest.java`

- [ ] **Step 1: Write failing MockMvc endpoint tests**

Create `ConfigControllerTest` with two tests. Use `Mockito.mock(ConfigStorageService.class)`, real `DesktopModeGuard`, `MockMvcBuilders.standaloneSetup`, and `GlobalExceptionHandler`.

```java
@Test
void shouldResetConfigInDesktopMode() throws Exception {
    when(configStorageService.resetToDefault()).thenReturn("default: value\n");
    when(configStorageService.getConfigPathText()).thenReturn("/tmp/Config.yml");

    mockMvc.perform(post("/api/config/reset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configPath").value("/tmp/Config.yml"))
            .andExpect(jsonPath("$.content").value("default: value\n"));

    verify(configStorageService).resetToDefault();
}

@Test
void shouldRejectResetInServerModeWithoutChangingConfig() throws Exception {
    runtimeProperties.setMode(RuntimeMode.SERVER);

    mockMvc.perform(post("/api/config/reset"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("配置初始化仅在桌面模式下可用"));

    verify(configStorageService, never()).resetToDefault();
}
```

The setup constructs:

```java
runtimeProperties = new KrlRuntimeProperties();
configStorageService = mock(ConfigStorageService.class);
ConfigController controller = new ConfigController(
        configStorageService,
        new DesktopModeGuard(runtimeProperties)
);
mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
```

- [ ] **Step 2: Run endpoint tests and verify RED**

```bash
mvn -pl krl-web -am -Dtest=ConfigControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because `ConfigController` does not exist.

- [ ] **Step 3: Implement `ConfigController`**

```java
package tech.waitforu.krlweb.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.waitforu.krlweb.config.ConfigStorageService;
import tech.waitforu.krlweb.config.DesktopModeGuard;

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
```

- [ ] **Step 4: Run endpoint and existing config API tests**

```bash
mvn -pl krl-web -am -Dtest=ConfigControllerTest,KrlWebApplicationTests -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: tests pass and the Spring context reports no ambiguous mappings.

- [ ] **Step 5: Commit the reset endpoint**

```bash
git add krl-web/src/main/java/tech/waitforu/krlweb/controller/ConfigController.java \
  krl-web/src/test/java/tech/waitforu/krlweb/controller/ConfigControllerTest.java
git commit -m "feat: 暴露桌面配置初始化接口"
```

### Task 4: Schedule graceful application shutdown

**Files:**
- Create: `krl-web/src/main/java/tech/waitforu/krlweb/service/ApplicationShutdownService.java`
- Create: `krl-web/src/test/java/tech/waitforu/krlweb/service/ApplicationShutdownServiceTest.java`
- Create: `krl-web/src/main/java/tech/waitforu/krlweb/controller/DesktopRuntimeController.java`
- Create: `krl-web/src/test/java/tech/waitforu/krlweb/controller/DesktopRuntimeControllerTest.java`

- [ ] **Step 1: Write failing shutdown service tests**

Use a mocked `ConfigurableApplicationContext`, a `CountDownLatch`, and a package-private constructor accepting the exit action and delay:

```java
@Test
void shouldCloseContextBeforeExitingProcess() throws InterruptedException {
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    CountDownLatch exited = new CountDownLatch(1);
    List<String> events = new CopyOnWriteArrayList<>();
    doAnswer(invocation -> {
        events.add("close");
        return null;
    }).when(context).close();
    ApplicationShutdownService service = new ApplicationShutdownService(context, () -> {
        events.add("exit");
        exited.countDown();
    }, 0L);

    assertTrue(service.scheduleShutdown());
    assertTrue(exited.await(2, TimeUnit.SECONDS));
    assertEquals(List.of("close", "exit"), events);
}

@Test
void shouldScheduleShutdownOnlyOnce() throws InterruptedException {
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    CountDownLatch exited = new CountDownLatch(1);
    AtomicInteger exitCount = new AtomicInteger();
    ApplicationShutdownService service = new ApplicationShutdownService(context, () -> {
        exitCount.incrementAndGet();
        exited.countDown();
    }, 10L);

    assertTrue(service.scheduleShutdown());
    assertFalse(service.scheduleShutdown());
    assertTrue(exited.await(2, TimeUnit.SECONDS));
    verify(context, times(1)).close();
    assertEquals(1, exitCount.get());
}
```

- [ ] **Step 2: Run service tests and verify RED**

```bash
mvn -pl krl-web -am -Dtest=ApplicationShutdownServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because `ApplicationShutdownService` does not exist.

- [ ] **Step 3: Implement the idempotent shutdown service**

```java
package tech.waitforu.krlweb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ApplicationShutdownService {
    private static final long RESPONSE_FLUSH_DELAY_MILLIS = 300L;

    private final ConfigurableApplicationContext applicationContext;
    private final Runnable processExit;
    private final long delayMillis;
    private final AtomicBoolean shutdownScheduled = new AtomicBoolean(false);

    @Autowired
    public ApplicationShutdownService(ConfigurableApplicationContext applicationContext) {
        this(applicationContext, () -> System.exit(0), RESPONSE_FLUSH_DELAY_MILLIS);
    }

    ApplicationShutdownService(ConfigurableApplicationContext applicationContext,
                               Runnable processExit,
                               long delayMillis) {
        this.applicationContext = applicationContext;
        this.processExit = processExit;
        this.delayMillis = Math.max(0L, delayMillis);
    }

    public boolean scheduleShutdown() {
        if (!shutdownScheduled.compareAndSet(false, true)) {
            return false;
        }
        Thread shutdownThread = new Thread(this::shutdown, "krl-application-shutdown");
        shutdownThread.setDaemon(false);
        shutdownThread.start();
        return true;
    }

    private void shutdown() {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        try {
            applicationContext.close();
        } finally {
            processExit.run();
        }
    }
}
```

- [ ] **Step 4: Run shutdown service tests and verify GREEN**

Run the command from Step 2.

Expected: both tests pass; the event list is `[close, exit]`; close and exit each occur once.

- [ ] **Step 5: Write failing shutdown controller tests**

Build `DesktopRuntimeControllerTest` like `ConfigControllerTest`, mocking `ApplicationShutdownService` and using a real guard.

Desktop assertion:

```java
when(shutdownService.scheduleShutdown()).thenReturn(true);
mockMvc.perform(post("/api/runtime/shutdown"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("accepted"))
        .andExpect(jsonPath("$.message").value("程序正在退出"));
verify(shutdownService).scheduleShutdown();
```

Server assertion:

```java
runtimeProperties.setMode(RuntimeMode.SERVER);
mockMvc.perform(post("/api/runtime/shutdown"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("程序退出仅在桌面模式下可用"));
verify(shutdownService, never()).scheduleShutdown();
```

Add a third test where `scheduleShutdown()` returns `false`; expect HTTP 202 with status `already_scheduled` and message `程序正在退出`.

- [ ] **Step 6: Run controller tests and verify RED**

```bash
mvn -pl krl-web -am -Dtest=DesktopRuntimeControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because `DesktopRuntimeController` does not exist.

- [ ] **Step 7: Implement the runtime shutdown controller**

```java
package tech.waitforu.krlweb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.waitforu.krlweb.config.DesktopModeGuard;
import tech.waitforu.krlweb.service.ApplicationShutdownService;

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
```

- [ ] **Step 8: Run all shutdown and runtime controller tests**

```bash
mvn -pl krl-web -am -Dtest=ApplicationShutdownServiceTest,DesktopRuntimeControllerTest,RuntimeControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all selected tests pass.

- [ ] **Step 9: Commit graceful shutdown support**

```bash
git add krl-web/src/main/java/tech/waitforu/krlweb/service/ApplicationShutdownService.java \
  krl-web/src/test/java/tech/waitforu/krlweb/service/ApplicationShutdownServiceTest.java \
  krl-web/src/main/java/tech/waitforu/krlweb/controller/DesktopRuntimeController.java \
  krl-web/src/test/java/tech/waitforu/krlweb/controller/DesktopRuntimeControllerTest.java
git commit -m "feat: 支持桌面程序优雅退出"
```

### Task 5: Add desktop-only controls to the Web interface

**Files:**
- Create: `krl-web/src/test/java/tech/waitforu/krlweb/DesktopControlsResourceTest.java`
- Modify: `krl-web/src/main/resources/static/index.html`
- Modify: `krl-web/src/main/resources/static/js/app.js`
- Modify: `krl-web/src/main/resources/static/css/styles.css`

- [ ] **Step 1: Write a failing static resource regression test**

Create a test that reads resources as UTF-8 and checks the required integration points:

```java
package tech.waitforu.krlweb;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopControlsResourceTest {

    @Test
    void shouldContainDesktopResetAndShutdownControls() throws IOException {
        String html = readResource("static/index.html");
        String javascript = readResource("static/js/app.js");
        String css = readResource("static/css/styles.css");

        assertAll(
                () -> assertTrue(html.contains("id=\"configResetBtn\"")),
                () -> assertTrue(html.contains("id=\"shutdownButton\"")),
                () -> assertTrue(html.contains("desktop-only-action hidden")),
                () -> assertTrue(javascript.contains("/api/config/reset")),
                () -> assertTrue(javascript.contains("/api/runtime/shutdown")),
                () -> assertTrue(javascript.contains("runtimeState.runtimeMode === 'desktop'")),
                () -> assertTrue(javascript.contains("uploadState.configContent = payload.content || ''")),
                () -> assertTrue(css.contains(".toolbar-button.danger"))
        );
    }

    private String readResource(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 2: Run the resource test and verify RED**

```bash
mvn -pl krl-web -am -Dtest=DesktopControlsResourceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: the assertion fails because neither new button exists.

- [ ] **Step 3: Add both controls to `index.html`**

Add after `downloadExcelBtn`:

```html
<button id="shutdownButton" type="button" class="toolbar-button danger toolbar-item desktop-only-action hidden">
    <i data-lucide="power"></i>
    <span>退出程序</span>
</button>
```

Add before `configReloadBtn`:

```html
<button id="configResetBtn" type="button" class="toolbar-button warning small-button desktop-only-action hidden">
    <i data-lucide="rotate-ccw"></i>
    <span>初始化配置</span>
</button>
```

- [ ] **Step 4: Add danger button styling**

Add beside the existing warning style:

```css
.toolbar-button.danger {
    background: #dc2626;
    color: #ffffff;
    box-shadow: 0 12px 26px rgba(220, 38, 38, 0.2);
}

.toolbar-button.danger:hover:not(:disabled) {
    background: #b91c1c;
}
```

- [ ] **Step 5: Register DOM references and mode visibility**

Add `shutdownButton` and `configResetBtn` to the `dom` object. Extend `refreshModeSpecificUi()`:

```javascript
const showDesktopActions = runtimeState.runtimeMode === 'desktop';
[dom.configResetBtn, dom.shutdownButton].forEach((button) => {
    button?.classList.toggle('hidden', !showDesktopActions);
});
```

Because both buttons start with `hidden` in HTML, server mode never flashes a privileged control before `/api/runtime/status` completes.

- [ ] **Step 6: Implement configuration payload synchronization and reset flow**

Extract the shared state update:

```javascript
function applyConfigPayload(payload) {
    uploadState.configPath = payload.configPath || '';
    uploadState.configContent = payload.content || '';
    uploadState.configLoaded = true;
    dom.configPathText.textContent = uploadState.configPath || '--';
    dom.configTextarea.value = uploadState.configContent;
    updateActionButtons();
}
```

Make `loadConfigFromServer()` call `applyConfigPayload(await response.json())`. Add:

```javascript
async function resetConfigToDefault() {
    const confirmed = window.confirm('此操作将覆盖磁盘中的 Config.yml，是否继续？');
    if (!confirmed) {
        return;
    }
    dom.configResetBtn.disabled = true;
    setLoading(true, '正在初始化配置...');
    try {
        const response = await apiFetch('/api/config/reset', {method: 'POST'});
        if (!response.ok) {
            const message = await extractErrorMessage(response);
            throw new Error(`初始化配置失败: ${message}`);
        }
        const payload = await response.json();
        applyConfigPayload(payload);
        uploadState.lastSuccessfulTaskId = null;
        uploadState.lastSuccessfulSignature = null;
        setTaskBadge('配置已初始化，等待分析', 'info');
        alert('配置文件已恢复为默认内容。');
    } catch (error) {
        console.error(error);
        alert(error.message || '初始化配置失败');
    } finally {
        dom.configResetBtn.disabled = false;
        setLoading(false, '');
    }
}
```

- [ ] **Step 7: Implement the shutdown interaction**

```javascript
async function shutdownApplication() {
    const confirmed = window.confirm('退出后正在执行的分析将被终止，是否继续？');
    if (!confirmed) {
        return;
    }
    dom.shutdownButton.disabled = true;
    setHeaderMenuOpen(false);
    setLoading(true, '正在退出程序...');
    try {
        const response = await apiFetch('/api/runtime/shutdown', {method: 'POST'});
        if (!response.ok) {
            const message = await extractErrorMessage(response);
            throw new Error(`退出程序失败: ${message}`);
        }
        setLoading(true, '程序已退出，可以关闭此页面。');
    } catch (error) {
        console.error(error);
        dom.shutdownButton.disabled = false;
        setLoading(false, '');
        alert(error.message || '退出程序失败');
    }
}
```

Bind the events in `bindPageEvents()`:

```javascript
dom.configResetBtn?.addEventListener('click', resetConfigToDefault);
dom.shutdownButton?.addEventListener('click', shutdownApplication);
```

- [ ] **Step 8: Run the resource test and verify GREEN**

Run the command from Step 2.

Expected: the resource test passes.

- [ ] **Step 9: Run the Web module test suite**

```bash
mvn -pl krl-web -am test
```

Expected: all `krl-core` and `krl-web` tests pass.

- [ ] **Step 10: Commit the desktop UI controls**

```bash
git add krl-web/src/test/java/tech/waitforu/krlweb/DesktopControlsResourceTest.java \
  krl-web/src/main/resources/static/index.html \
  krl-web/src/main/resources/static/js/app.js \
  krl-web/src/main/resources/static/css/styles.css
git commit -m "feat: 添加配置初始化与退出按钮"
```

### Task 6: Document and verify the complete feature

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update user-facing documentation**

Under “自动初始化”, add:

```markdown
- 桌面模式可在 Config 编辑器中点击“初始化配置”，将磁盘配置恢复为应用内置默认内容；服务器模式不提供该操作
```

Under the desktop workflow section, document that “退出程序” closes the background Java process and that neither desktop-only endpoint is exposed for use in server mode.

- [ ] **Step 2: Run formatting and diff checks**

```bash
git diff --check
rg -n "configResetBtn|shutdownButton|/api/config/reset|/api/runtime/shutdown" krl-web/src/main krl-web/src/test README.md
```

Expected: `git diff --check` exits zero and every identifier appears in its expected backend, frontend, and test location.

- [ ] **Step 3: Run the full test suite from a clean Maven invocation**

```bash
mvn clean test
```

Expected: reactor build success with zero test failures.

- [ ] **Step 4: Build the executable application**

```bash
mvn clean package
```

Expected: reactor build success and `krl-web/target/krl-web-1.0-SNAPSHOT.jar` exists.

- [ ] **Step 5: Verify the bundled default config in the packaged JAR**

```bash
jar tf krl-web/target/krl-web-1.0-SNAPSHOT.jar | rg 'BOOT-INF/lib/krl-core-.*\.jar'
unzip -p krl-web/target/krl-web-1.0-SNAPSHOT.jar BOOT-INF/lib/krl-core-1.0-SNAPSHOT.jar > /tmp/krl-core-verification.jar
jar tf /tmp/krl-core-verification.jar | rg '^config.yml$'
```

Expected: the Web JAR contains the core JAR and the nested core JAR contains `config.yml`.

- [ ] **Step 6: Perform browser verification in desktop mode**

Start the packaged JAR with an isolated configuration path:

```bash
java -jar krl-web/target/krl-web-1.0-SNAPSHOT.jar \
  --server.port=2027 \
  --krl.runtime.mode=desktop \
  --krl.config.path=/tmp/krl-parser-desktop-verification/Config.yml
```

Open `http://127.0.0.1:2027` and verify:

1. “初始化配置” appears inside the Config dialog.
2. After changing `/tmp/krl-parser-desktop-verification/Config.yml`, confirming initialization restores the bundled content in both the file and text area.
3. Cancelling either confirmation produces no request and no state change.
4. “退出程序” appears in the operation menu.
5. Confirming exit leaves the completion overlay visible and the Java process terminates without manual kill.

- [ ] **Step 7: Verify server-mode defense in depth**

Start with `--krl.runtime.mode=server --server.port=2028`, then verify both controls are hidden and run:

```bash
curl -i -X POST http://127.0.0.1:2028/api/config/reset
curl -i -X POST http://127.0.0.1:2028/api/runtime/shutdown
```

Expected: both responses are HTTP 403 and the server process remains running.

- [ ] **Step 8: Commit documentation**

```bash
git add README.md
git commit -m "docs: 说明桌面配置初始化与退出功能"
```

- [ ] **Step 9: Inspect final scope**

```bash
git status --short
git log --oneline --decorate -8
git diff origin/main...HEAD --stat
```

Expected: no uncommitted changes remain; commits are limited to the approved design, plan, backend policy, reset, shutdown, UI, tests, and documentation.
