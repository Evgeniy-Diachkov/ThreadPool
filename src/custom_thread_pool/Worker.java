package custom_thread_pool;

import java.util.concurrent.*;

public class Worker implements Runnable {
    private final BlockingQueue<Runnable> taskQueue;
    private final CustomThreadPoolExecutor pool;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private volatile boolean running = true;

    public Worker(BlockingQueue<Runnable> taskQueue,
                  CustomThreadPoolExecutor pool,
                  long keepAliveTime,
                  TimeUnit timeUnit) {
        this.taskQueue = taskQueue;
        this.pool = pool;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
    }

    public void stopWorker() {
        running = false;
    }

    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        try {
            while (running) {
                if (pool.isShutdown()) break;

                Runnable task = taskQueue.poll(keepAliveTime, timeUnit);
                if (task != null) {
                    System.out.println("[Worker] " + currentThread.getName() + " executes " + task);
                    task.run();
                } else {
                    if (pool.getActiveThreadCount() > pool.getCorePoolSize()) {
                        System.out.println("[Worker] " + currentThread.getName() + " idle timeout, stopping.");
                        break;
                    }
                }
            }
        } catch (InterruptedException ignored) {
        } finally {
            pool.onWorkerExit(this);
            System.out.println("[Worker] " + currentThread.getName() + " terminated.");
        }
    }
}