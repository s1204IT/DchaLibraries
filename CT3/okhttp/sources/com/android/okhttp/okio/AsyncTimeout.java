package com.android.okhttp.okio;

import java.io.IOException;
import java.io.InterruptedIOException;

public class AsyncTimeout extends Timeout {
    private static AsyncTimeout head;
    private boolean inQueue;
    private AsyncTimeout next;
    private long timeoutAt;

    public final void enter() {
        if (this.inQueue) {
            throw new IllegalStateException("Unbalanced enter/exit");
        }
        long timeoutNanos = timeoutNanos();
        boolean hasDeadline = hasDeadline();
        if (timeoutNanos == 0 && !hasDeadline) {
            return;
        }
        this.inQueue = true;
        scheduleTimeout(this, timeoutNanos, hasDeadline);
    }

    private static synchronized void scheduleTimeout(AsyncTimeout node, long timeoutNanos, boolean hasDeadline) {
        if (head == null) {
            head = new AsyncTimeout();
            new Watchdog().start();
        }
        long now = System.nanoTime();
        if (timeoutNanos != 0 && hasDeadline) {
            node.timeoutAt = Math.min(timeoutNanos, node.deadlineNanoTime() - now) + now;
        } else if (timeoutNanos != 0) {
            node.timeoutAt = now + timeoutNanos;
        } else if (hasDeadline) {
            node.timeoutAt = node.deadlineNanoTime();
        } else {
            throw new AssertionError();
        }
        long remainingNanos = node.remainingNanos(now);
        AsyncTimeout prev = head;
        while (prev.next != null && remainingNanos >= prev.next.remainingNanos(now)) {
            prev = prev.next;
        }
        node.next = prev.next;
        prev.next = node;
        if (prev == head) {
            AsyncTimeout.class.notify();
        }
    }

    public final boolean exit() {
        if (!this.inQueue) {
            return false;
        }
        this.inQueue = false;
        return cancelScheduledTimeout(this);
    }

    private static synchronized boolean cancelScheduledTimeout(AsyncTimeout node) {
        for (AsyncTimeout prev = head; prev != null; prev = prev.next) {
            if (prev.next == node) {
                prev.next = node.next;
                node.next = null;
                return false;
            }
        }
        return true;
    }

    private long remainingNanos(long now) {
        return this.timeoutAt - now;
    }

    protected void timedOut() {
    }

    public final Sink sink(final Sink sink) {
        return new Sink() {
            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                boolean throwOnTimeout = false;
                AsyncTimeout.this.enter();
                try {
                    try {
                        sink.write(source, byteCount);
                        throwOnTimeout = true;
                    } catch (IOException e) {
                        throw AsyncTimeout.this.exit(e);
                    }
                } finally {
                    AsyncTimeout.this.exit(throwOnTimeout);
                }
            }

            @Override
            public void flush() throws IOException {
                boolean throwOnTimeout = false;
                AsyncTimeout.this.enter();
                try {
                    try {
                        sink.flush();
                        throwOnTimeout = true;
                    } catch (IOException e) {
                        throw AsyncTimeout.this.exit(e);
                    }
                } finally {
                    AsyncTimeout.this.exit(throwOnTimeout);
                }
            }

            @Override
            public void close() throws IOException {
                boolean throwOnTimeout = false;
                AsyncTimeout.this.enter();
                try {
                    try {
                        sink.close();
                        throwOnTimeout = true;
                    } catch (IOException e) {
                        throw AsyncTimeout.this.exit(e);
                    }
                } finally {
                    AsyncTimeout.this.exit(throwOnTimeout);
                }
            }

            @Override
            public Timeout timeout() {
                return AsyncTimeout.this;
            }

            public String toString() {
                return "AsyncTimeout.sink(" + sink + ")";
            }
        };
    }

    public final Source source(final Source source) {
        return new Source() {
            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                boolean throwOnTimeout = false;
                AsyncTimeout.this.enter();
                try {
                    try {
                        long result = source.read(sink, byteCount);
                        throwOnTimeout = true;
                        return result;
                    } catch (IOException e) {
                        throw AsyncTimeout.this.exit(e);
                    }
                } finally {
                    AsyncTimeout.this.exit(throwOnTimeout);
                }
            }

            @Override
            public void close() throws IOException {
                boolean throwOnTimeout = false;
                try {
                    try {
                        source.close();
                        throwOnTimeout = true;
                    } catch (IOException e) {
                        throw AsyncTimeout.this.exit(e);
                    }
                } finally {
                    AsyncTimeout.this.exit(throwOnTimeout);
                }
            }

            @Override
            public Timeout timeout() {
                return AsyncTimeout.this;
            }

            public String toString() {
                return "AsyncTimeout.source(" + source + ")";
            }
        };
    }

    final void exit(boolean throwOnTimeout) throws IOException {
        boolean timedOut = exit();
        if (timedOut && throwOnTimeout) {
            throw newTimeoutException(null);
        }
    }

    final IOException exit(IOException cause) throws IOException {
        return !exit() ? cause : newTimeoutException(cause);
    }

    protected IOException newTimeoutException(IOException cause) {
        InterruptedIOException e = new InterruptedIOException("timeout");
        if (cause != null) {
            e.initCause(cause);
        }
        return e;
    }

    private static final class Watchdog extends Thread {
        public Watchdog() {
            super("Okio Watchdog");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    AsyncTimeout timedOut = AsyncTimeout.awaitTimeout();
                    if (timedOut != null) {
                        timedOut.timedOut();
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static synchronized AsyncTimeout awaitTimeout() throws InterruptedException {
        AsyncTimeout node = head.next;
        if (node == null) {
            AsyncTimeout.class.wait();
            return null;
        }
        long waitNanos = node.remainingNanos(System.nanoTime());
        if (waitNanos > 0) {
            long waitMillis = waitNanos / 1000000;
            AsyncTimeout.class.wait(waitMillis, (int) (waitNanos - (waitMillis * 1000000)));
            return null;
        }
        head.next = node.next;
        node.next = null;
        return node;
    }
}
