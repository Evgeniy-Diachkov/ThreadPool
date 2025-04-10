package custom_thread_pool;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolExecutor implements CustomExecutor {
    public int getActiveThreadCount() {
        return activeThreads.get();
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    private final List<BlockingQueue<Runnable>> taskQueues;
    private final Set<Worker> workers = ConcurrentHashMap.newKeySet();
    private final ThreadFactory threadFactory;
    private final RejectionPolicy rejectionPolicy;

    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger rrIndex = new AtomicInteger(0);
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
        this.threadFactory = threadFactory;
        this.rejectionPolicy = rejectionPolicy;
        this.taskQueues = Collections.synchronizedList(new ArrayList<>());
        ensureMinSpareThreads(); // initialize spare threads at startup
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            System.out.println("[Pool] Task rejected - pool is shutting down.");
            return;
        }

        if (taskQueues.isEmpty()) {
            rejectionPolicy.reject(command);
            return;
        }

        int index = rrIndex.getAndIncrement() % taskQueues.size();
        BlockingQueue<Runnable> queue = taskQueues.get(index);

        if (!queue.offer(command)) {
            rejectionPolicy.reject(command);
        } else {
            System.out.println("[Pool] Task accepted into queue #" + index + ": " + command.getClass().getSimpleName() + " - " + command);
            ensureMinSpareThreads();
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<>(task);
        execute(futureTask);
        return futureTask;
    }

    private void ensureMinSpareThreads() {
        int idleThreads = workers.size();
        while (idleThreads < minSpareThreads && activeThreads.get() < maxPoolSize) {
            if (activeThreads.incrementAndGet() <= maxPoolSize) {
                BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueSize);
                taskQueues.add(queue);
                Worker worker = new Worker(queue, this, keepAliveTime, timeUnit);
                workers.add(worker);
                Thread thread = threadFactory.newThread(worker);
                thread.start();
                idleThreads++;
                System.out.println("[Pool] Min spare thread check: started extra worker. Active: " + activeThreads.get());
            } else {
                activeThreads.decrementAndGet();
                break;
            }
        }
    }

    public void onWorkerExit(Worker worker) {
        workers.remove(worker);
        activeThreads.decrementAndGet();
        System.out.println("[Pool] Worker exited. Total workers left: " + workers.size());
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        System.out.println("[Pool] Shutdown initiated.");
    }

    @Override
    public void shutdownNow() {
        System.out.println("[Pool] Force shutdown initiated.");
        for (Worker worker : workers) {
            worker.stopWorker();
        }
    }

    public boolean isShutdown() {
        return isShutdown;
    }
}
