package custom_thread_pool;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolExecutor implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    private final BlockingQueue<Runnable> taskQueue;
    private final Set<Worker> workers = ConcurrentHashMap.newKeySet();
    private final ThreadFactory threadFactory;
    private final RejectionPolicy rejectionPolicy;

    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private volatile boolean isShutdown = false;

    public CustomThreadPoolExecutor(int corePoolSize, int maxPoolSize,
                                    long keepAliveTime, TimeUnit timeUnit,
                                    int queueSize, int minSpareThreads,
                                    ThreadFactory threadFactory,
                                    RejectionPolicy rejectionPolicy) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.taskQueue = new ArrayBlockingQueue<>(queueSize);
        this.threadFactory = threadFactory;
        this.rejectionPolicy = rejectionPolicy;
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) return;

        if (!taskQueue.offer(command)) {
            rejectionPolicy.reject(command);
        } else {
            maybeStartWorker();
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<>(task);
        execute(futureTask);
        return futureTask;
    }

    private void maybeStartWorker() {
        int current = activeThreads.get();

        if (current < corePoolSize ||
                (taskQueue.size() > 0 && current < maxPoolSize)) {

            if (activeThreads.incrementAndGet() <= maxPoolSize) {
                Worker worker = new Worker(taskQueue, this, keepAliveTime, timeUnit);
                workers.add(worker);
                Thread thread = threadFactory.newThread(worker);
                thread.start();
            } else {
                activeThreads.decrementAndGet();
            }
        }
    }

    public void onWorkerExit(Worker worker) {
        workers.remove(worker);
        activeThreads.decrementAndGet();
    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        for (Worker worker : workers) {
            worker.stopWorker();
        }
    }

    public boolean isShutdown() {
        return isShutdown;
    }
}

