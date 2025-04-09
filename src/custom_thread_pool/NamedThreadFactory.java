// NamedThreadFactory.java
package custom_thread_pool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = prefix + "-worker-" + counter.getAndIncrement();
        System.out.println("[ThreadFactory] Creating new thread: " + threadName);
        return new Thread(r, threadName);
    }
}