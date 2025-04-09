// RejectionPolicy.java
package custom_thread_pool;

public interface RejectionPolicy {
    void reject(Runnable task);
}