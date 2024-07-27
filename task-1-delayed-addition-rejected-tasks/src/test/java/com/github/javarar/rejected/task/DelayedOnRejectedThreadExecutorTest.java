package com.github.javarar.rejected.task;

import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DelayedOnRejectedThreadExecutorTest {

    @Test
    void testName() throws InterruptedException {
        DelayedOnRejectedThreadExecutor executor = new DelayedOnRejectedThreadExecutor(
                2,
                3,
                300,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(5),
                1000,
                TimeUnit.MILLISECONDS
        );

        AtomicLong finishTasks = new AtomicLong();

        int taskCount = 10;
        for (int i = 0; i < taskCount; i++) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    Thread.sleep(500);
                    System.out.printf("Run task %d%n", finalI);
                    finishTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            });
        }

        Thread.sleep(3500);
        assert finishTasks.get() == taskCount;
    }
}
