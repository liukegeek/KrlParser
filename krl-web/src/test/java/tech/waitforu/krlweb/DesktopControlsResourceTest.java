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
