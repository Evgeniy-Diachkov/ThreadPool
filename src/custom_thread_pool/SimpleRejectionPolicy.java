package custom_thread_pool;

public class SimpleRejectionPolicy implements RejectionPolicy {
    @Override
    public void reject(Runnable task) {
        System.out.println("[Rejected] Task " + task + " was rejected due to overload!");
    }
}