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
}
