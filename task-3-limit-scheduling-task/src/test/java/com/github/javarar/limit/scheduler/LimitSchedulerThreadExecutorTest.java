package com.github.javarar.limit.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class LimitSchedulerThreadExecutorTest {



    @Test
    void testName() throws InterruptedException {
        LimitSchedulerThreadExecutor executor = new LimitSchedulerThreadExecutor(10);

        AtomicLong currentSum = new AtomicLong();
        executor.scheduleAtFixedRateWIthLimit(currentSum::incrementAndGet, 0, 100, TimeUnit.MILLISECONDS, 10);

        Thread.sleep(1500);
        assertEquals(10, currentSum.get());
    }
}
