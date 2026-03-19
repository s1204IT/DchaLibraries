package sun.nio.fs;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

abstract class AbstractWatchService implements WatchService {
    private volatile boolean closed;
    private final LinkedBlockingDeque<WatchKey> pendingKeys = new LinkedBlockingDeque<>();
    private final WatchKey CLOSE_KEY = new AbstractWatchKey(null, 0 == true ? 1 : 0) {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void cancel() {
        }
    };
    private final Object closeLock = new Object();

    abstract void implClose() throws IOException;

    abstract WatchKey register(Path path, WatchEvent.Kind<?>[] kindArr, WatchEvent.Modifier... modifierArr) throws IOException;

    protected AbstractWatchService() {
    }

    final void enqueueKey(WatchKey watchKey) {
        this.pendingKeys.offer(watchKey);
    }

    private void checkOpen() {
        if (this.closed) {
            throw new ClosedWatchServiceException();
        }
    }

    private void checkKey(WatchKey watchKey) {
        if (watchKey == this.CLOSE_KEY) {
            enqueueKey(watchKey);
        }
        checkOpen();
    }

    @Override
    public final WatchKey poll() {
        checkOpen();
        WatchKey watchKeyPoll = this.pendingKeys.poll();
        checkKey(watchKeyPoll);
        return watchKeyPoll;
    }

    @Override
    public final WatchKey poll(long j, TimeUnit timeUnit) throws InterruptedException {
        checkOpen();
        WatchKey watchKeyPoll = this.pendingKeys.poll(j, timeUnit);
        checkKey(watchKeyPoll);
        return watchKeyPoll;
    }

    @Override
    public final WatchKey take() throws InterruptedException {
        checkOpen();
        WatchKey watchKeyTake = this.pendingKeys.take();
        checkKey(watchKeyTake);
        return watchKeyTake;
    }

    final boolean isOpen() {
        return !this.closed;
    }

    final Object closeLock() {
        return this.closeLock;
    }

    @Override
    public final void close() throws IOException {
        synchronized (this.closeLock) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            implClose();
            this.pendingKeys.clear();
            this.pendingKeys.offer(this.CLOSE_KEY);
        }
    }
}
