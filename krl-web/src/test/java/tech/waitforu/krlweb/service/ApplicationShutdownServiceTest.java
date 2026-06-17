package tech.waitforu.krlweb.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationShutdownServiceTest {

    @Test
    void shouldCloseContextBeforeExitingProcess() throws InterruptedException {
        List<String> events = new CopyOnWriteArrayList<>();
        CountDownLatch exited = new CountDownLatch(1);
        GenericApplicationContext context = contextWithCloseMarker(events);
        ApplicationShutdownService service = new ApplicationShutdownService(context, () -> {
            events.add("exit");
            exited.countDown();
        }, 0L);

        assertTrue(service.scheduleShutdown());
        assertTrue(exited.await(2, TimeUnit.SECONDS));

        assertFalse(context.isActive());
        assertEquals(List.of("close", "exit"), events);
    }

    @Test
    void shouldScheduleShutdownOnlyOnce() throws InterruptedException {
        List<String> events = new CopyOnWriteArrayList<>();
        CountDownLatch exited = new CountDownLatch(1);
        GenericApplicationContext context = contextWithCloseMarker(events);
        ApplicationShutdownService service = new ApplicationShutdownService(context, () -> {
            events.add("exit");
            exited.countDown();
        }, 10L);

        assertTrue(service.scheduleShutdown());
        assertFalse(service.scheduleShutdown());
        assertTrue(exited.await(2, TimeUnit.SECONDS));
        assertEquals(List.of("close", "exit"), events);
    }

    private GenericApplicationContext contextWithCloseMarker(List<String> events) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(CloseMarker.class, () -> new CloseMarker(events));
        context.refresh();
        return context;
    }

    private record CloseMarker(List<String> events) implements AutoCloseable {
        @Override
        public void close() {
            events.add("close");
        }
    }
}
