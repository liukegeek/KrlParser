package tech.waitforu.krlweb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/** 负责在响应返回后关闭 Spring 容器并结束 Java 进程。 */
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
