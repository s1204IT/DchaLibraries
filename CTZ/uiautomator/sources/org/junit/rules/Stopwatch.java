package org.junit.rules;

import java.util.concurrent.TimeUnit;
import org.junit.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public abstract class Stopwatch implements TestRule {
    private final Clock clock;
    private volatile long endNanos;
    private volatile long startNanos;

    public Stopwatch() {
        this(new Clock());
    }

    Stopwatch(Clock clock) {
        this.clock = clock;
    }

    public long runtime(TimeUnit timeUnit) {
        return timeUnit.convert(getNanos(), TimeUnit.NANOSECONDS);
    }

    protected void succeeded(long j, Description description) {
    }

    protected void failed(long j, Throwable th, Description description) {
    }

    protected void skipped(long j, AssumptionViolatedException assumptionViolatedException, Description description) {
    }

    protected void finished(long j, Description description) {
    }

    private long getNanos() {
        if (this.startNanos == 0) {
            throw new IllegalStateException("Test has not started");
        }
        long jNanoTime = this.endNanos;
        if (jNanoTime == 0) {
            jNanoTime = this.clock.nanoTime();
        }
        return jNanoTime - this.startNanos;
    }

    private void starting() {
        this.startNanos = this.clock.nanoTime();
        this.endNanos = 0L;
    }

    private void stopping() {
        this.endNanos = this.clock.nanoTime();
    }

    @Override
    public final Statement apply(Statement statement, Description description) {
        return new InternalWatcher().apply(statement, description);
    }

    private class InternalWatcher extends TestWatcher {
        private InternalWatcher() {
        }

        @Override
        protected void starting(Description description) {
            Stopwatch.this.starting();
        }

        @Override
        protected void finished(Description description) {
            Stopwatch.this.finished(Stopwatch.this.getNanos(), description);
        }

        @Override
        protected void succeeded(Description description) {
            Stopwatch.this.stopping();
            Stopwatch.this.succeeded(Stopwatch.this.getNanos(), description);
        }

        @Override
        protected void failed(Throwable th, Description description) {
            Stopwatch.this.stopping();
            Stopwatch.this.failed(Stopwatch.this.getNanos(), th, description);
        }

        @Override
        protected void skipped(AssumptionViolatedException assumptionViolatedException, Description description) {
            Stopwatch.this.stopping();
            Stopwatch.this.skipped(Stopwatch.this.getNanos(), assumptionViolatedException, description);
        }
    }

    static class Clock {
        Clock() {
        }

        public long nanoTime() {
            return System.nanoTime();
        }
    }
}
