package com.google.common.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface ListeningScheduledExecutorService extends ListeningExecutorService, ScheduledExecutorService {
    @Override
    ListenableScheduledFuture<?> schedule(Runnable runnable, long j, TimeUnit timeUnit);

    @Override
    <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long j, TimeUnit timeUnit);

    @Override
    ListenableScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long j, long j2, TimeUnit timeUnit);

    @Override
    ListenableScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long j, long j2, TimeUnit timeUnit);
}
