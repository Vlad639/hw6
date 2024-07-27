package com.github.javarar.rejected.task;

import java.util.concurrent.*;

public class DelayedOnRejectedThreadExecutor extends ThreadPoolExecutor {

    private final ScheduledExecutorService scheduledExecutor;
    private final long rejectRetryDelay;
    private final TimeUnit rejectRetryTimeUnit;

    public DelayedOnRejectedThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, long rejectRetryDelay, TimeUnit rejectRetryTimeUnit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.scheduledExecutor = Executors.newScheduledThreadPool(corePoolSize);
        this.rejectRetryDelay = rejectRetryDelay;
        this.rejectRetryTimeUnit = rejectRetryTimeUnit;
        setRejectedExecutionHandler(this::rejectTaskHandler);
    }

    public DelayedOnRejectedThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, long rejectRetryDelay, TimeUnit rejectRetryTimeUnit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.scheduledExecutor = Executors.newScheduledThreadPool(corePoolSize);
        this.rejectRetryDelay = rejectRetryDelay;
        this.rejectRetryTimeUnit = rejectRetryTimeUnit;
        setRejectedExecutionHandler(this::rejectTaskHandler);
    }

    public DelayedOnRejectedThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler, long rejectRetryDelay, TimeUnit rejectRetryTimeUnit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        this.scheduledExecutor = Executors.newScheduledThreadPool(corePoolSize);
        this.rejectRetryDelay = rejectRetryDelay;
        this.rejectRetryTimeUnit = rejectRetryTimeUnit;
        setRejectedExecutionHandler(this::rejectTaskHandler);
    }

    public DelayedOnRejectedThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler, long rejectRetryDelay, TimeUnit rejectRetryTimeUnit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.scheduledExecutor = Executors.newScheduledThreadPool(corePoolSize);
        this.rejectRetryDelay = rejectRetryDelay;
        this.rejectRetryTimeUnit = rejectRetryTimeUnit;
        setRejectedExecutionHandler(this::rejectTaskHandler);
    }

    private void rejectTaskHandler(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
        System.out.printf("reject task %s, added to retry queue", runnable);
        scheduledExecutor.schedule(() -> {
            System.out.printf("retry task %s", runnable);
            threadPoolExecutor.execute(runnable);
        }, rejectRetryDelay, rejectRetryTimeUnit);
    }
}
