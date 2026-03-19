package java.lang;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public abstract class Process {
    public abstract void destroy();

    public abstract int exitValue();

    public abstract InputStream getErrorStream();

    public abstract InputStream getInputStream();

    public abstract OutputStream getOutputStream();

    public abstract int waitFor() throws InterruptedException;

    public boolean waitFor(long j, TimeUnit timeUnit) throws InterruptedException {
        long jNanoTime = System.nanoTime();
        long nanos = timeUnit.toNanos(j);
        do {
            try {
                exitValue();
                return true;
            } catch (IllegalThreadStateException e) {
                if (nanos > 0) {
                    Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(nanos) + 1, 100L));
                }
                nanos = timeUnit.toNanos(j) - (System.nanoTime() - jNanoTime);
            }
        } while (nanos > 0);
        return false;
    }

    public Process destroyForcibly() {
        destroy();
        return this;
    }

    public boolean isAlive() {
        try {
            exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }
}
