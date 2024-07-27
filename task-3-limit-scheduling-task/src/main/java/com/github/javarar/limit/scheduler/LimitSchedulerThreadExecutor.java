package com.github.javarar.limit.scheduler;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class LimitSchedulerThreadExecutor extends ScheduledThreadPoolExecutor {
    public LimitSchedulerThreadExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    public LimitSchedulerThreadExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public LimitSchedulerThreadExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public LimitSchedulerThreadExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    public void scheduleAtFixedRateWIthLimit(Runnable command, long initialDelay, long period, TimeUnit unit, long limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Attempts must be greater than 0!");
        }

        AtomicLong currentAttempts = new AtomicLong(limit);
        AtomicReference<ScheduledFuture<?>> scheduledFutureReference = new AtomicReference<>();

        ScheduledFuture<?> scheduledFuture = super.scheduleAtFixedRate(() -> {
            if (currentAttempts.get() > 0) {
                command.run();
                currentAttempts.decrementAndGet();
            } else {
                ScheduledFuture<?> canceled = scheduledFutureReference.get();
                if (canceled != null && !canceled.isCancelled()) {
                    canceled.cancel(true);
                }
            }
        }, initialDelay, period, unit);

        scheduledFutureReference.set(scheduledFuture);
    }


}
