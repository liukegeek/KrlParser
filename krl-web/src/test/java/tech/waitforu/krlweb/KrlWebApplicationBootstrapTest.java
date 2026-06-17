package tech.waitforu.krlweb;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KrlWebApplicationBootstrapTest {

    @Test
    void usesConcreteDefaultLogDirectoryWithoutOverrides() throws Exception {
        String previousLogDirectory = System.getProperty("krl.log.dir");
        try {
            System.clearProperty("krl.log.dir");

            Method resolver = KrlWebApplication.class
                    .getDeclaredMethod("resolveBootstrapSettings", String[].class);
            resolver.setAccessible(true);
            Object settings = resolver.invoke(null, (Object) new String[0]);

            Method logDirectoryAccessor = settings.getClass().getDeclaredMethod("logDir");
            logDirectoryAccessor.setAccessible(true);
            String actualLogDirectory = (String) logDirectoryAccessor.invoke(settings);

            String environmentLogDirectory = System.getenv("KRL_LOG_DIR");
            Path expectedLogDirectory = environmentLogDirectory == null || environmentLogDirectory.isBlank()
                    ? Path.of(System.getProperty("user.home", "."), ".KrlParser", "logs")
                    : Path.of(environmentLogDirectory);

            assertFalse(actualLogDirectory.contains("${"),
                    "启动配置不能保留未解析的 Spring 占位符");
            assertEquals(expectedLogDirectory.toAbsolutePath().normalize().toString(), actualLogDirectory);
        } finally {
            restoreSystemProperty("krl.log.dir", previousLogDirectory);
        }
    }

    private static void restoreSystemProperty(String propertyName, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(propertyName);
        } else {
            System.setProperty(propertyName, previousValue);
        }
    }
}
