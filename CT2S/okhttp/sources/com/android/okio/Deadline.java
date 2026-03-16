package com.android.okio;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;

public class Deadline {
    public static final Deadline NONE = new Deadline() {
        @Override
        public Deadline start(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean reached() {
            return false;
        }
    };
    private long deadlineNanos;

    public Deadline start(long timeout, TimeUnit unit) {
        this.deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
        return this;
    }

    public boolean reached() {
        return System.nanoTime() - this.deadlineNanos >= 0;
    }

    public final void throwIfReached() throws IOException {
        if (reached()) {
            throw new IOException("Deadline reached");
        }
        if (Thread.interrupted()) {
            throw new InterruptedIOException();
        }
    }
}
