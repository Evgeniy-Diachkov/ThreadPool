// Main.java
package custom_thread_pool;

import java.util.concurrent.*;

public class Main {

    private static CustomThreadPoolExecutor createExecutor() {
        return new CustomThreadPoolExecutor(
                2, // corePoolSize
                5, // maxPoolSize
                5, // keepAliveTime
                TimeUnit.SECONDS,
                3, // queueSize
                2, // minSpareThreads
                new NamedThreadFactory("MyPool"),
                new SimpleRejectionPolicy()
        );
    }
    public static void main(String[] args) throws InterruptedException {
        CustomThreadPoolExecutor executor = createExecutor();

        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            executor.execute(() -> {
                String threadName = Thread.currentThread().getName();
                System.out.println("[Task] Started task " + taskId + " on " + threadName);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println("[Task] Task " + taskId + " was interrupted.");
                    Thread.currentThread().interrupt();
                }
                System.out.println("[Task] Finished task " + taskId + " on " + threadName);
            });
        }

        Thread.sleep(10000);

        Future<String> result = executor.submit(() -> {
            String thread = Thread.currentThread().getName();
            System.out.println("[Callable] Executing in " + thread);
            Thread.sleep(1000);
            return "Result from " + thread;
        });

        try {
            System.out.println("[Main] Callable result: " + result.get());
        } catch (ExecutionException e) {
            System.out.println("[Main] Callable execution failed: " + e.getMessage());
        }
        System.out.println("[Main] Graceful shutdown requested");
        executor.shutdown();

        Thread.sleep(3000);
        System.out.println("[Main] Forcing shutdown if not yet terminated");
        executor.shutdownNow();

        System.out.println("[Main] Program completed");
    }
}
