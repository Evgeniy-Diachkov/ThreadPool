// Main.java
package custom_thread_pool;

import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(
                2, // corePoolSize
                4, // maxPoolSize
                5, // keepAliveTime
                TimeUnit.SECONDS,
                5, // queueSize
                1, // minSpareThreads
                new NamedThreadFactory("MyPool"),
                new SimpleRejectionPolicy()
        );

        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("[Task] Started task " + taskId);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[Task] Finished task " + taskId);
            });
        }

        Thread.sleep(20000);

        System.out.println("[Main] Initiating shutdown");
        executor.shutdown();
        Thread.sleep(10000);
        System.out.println("[Main] Program completed");
    }
}
