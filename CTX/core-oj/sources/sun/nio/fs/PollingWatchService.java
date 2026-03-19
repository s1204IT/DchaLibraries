package sun.nio.fs;

import com.sun.nio.file.SensitivityWatchEventModifier;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

class PollingWatchService extends AbstractWatchService {
    private final Map<Object, PollingWatchKey> map = new HashMap();
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    });

    PollingWatchService() {
    }

    @Override
    WatchKey register(final Path path, WatchEvent.Kind<?>[] kindArr, WatchEvent.Modifier... modifierArr) throws IOException {
        final HashSet hashSet = new HashSet(kindArr.length);
        for (WatchEvent.Kind<?> kind : kindArr) {
            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY || kind == StandardWatchEventKinds.ENTRY_DELETE) {
                hashSet.add(kind);
            } else if (kind != StandardWatchEventKinds.OVERFLOW) {
                if (kind == null) {
                    throw new NullPointerException("An element in event set is 'null'");
                }
                throw new UnsupportedOperationException(kind.name());
            }
        }
        if (hashSet.isEmpty()) {
            throw new IllegalArgumentException("No events to register");
        }
        final SensitivityWatchEventModifier sensitivityWatchEventModifier = SensitivityWatchEventModifier.MEDIUM;
        if (modifierArr.length > 0) {
            for (WatchEvent.Modifier modifier : modifierArr) {
                if (modifier == null) {
                    throw new NullPointerException();
                }
                if (modifier instanceof SensitivityWatchEventModifier) {
                    sensitivityWatchEventModifier = (SensitivityWatchEventModifier) modifier;
                } else {
                    throw new UnsupportedOperationException("Modifier not supported");
                }
            }
        }
        if (!isOpen()) {
            throw new ClosedWatchServiceException();
        }
        try {
            return (WatchKey) AccessController.doPrivileged(new PrivilegedExceptionAction<PollingWatchKey>() {
                @Override
                public PollingWatchKey run() throws IOException {
                    return PollingWatchService.this.doPrivilegedRegister(path, hashSet, sensitivityWatchEventModifier);
                }
            });
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause != null && (cause instanceof IOException)) {
                throw ((IOException) cause);
            }
            throw new AssertionError(e);
        }
    }

    private PollingWatchKey doPrivilegedRegister(Path path, Set<? extends WatchEvent.Kind<?>> set, SensitivityWatchEventModifier sensitivityWatchEventModifier) throws IOException {
        PollingWatchKey pollingWatchKey;
        BasicFileAttributes attributes = Files.readAttributes(path, (Class<BasicFileAttributes>) BasicFileAttributes.class, new LinkOption[0]);
        if (!attributes.isDirectory()) {
            throw new NotDirectoryException(path.toString());
        }
        Object objFileKey = attributes.fileKey();
        if (objFileKey == null) {
            throw new AssertionError((Object) "File keys must be supported");
        }
        synchronized (closeLock()) {
            if (!isOpen()) {
                throw new ClosedWatchServiceException();
            }
            synchronized (this.map) {
                pollingWatchKey = this.map.get(objFileKey);
                if (pollingWatchKey == null) {
                    pollingWatchKey = new PollingWatchKey(path, this, objFileKey);
                    this.map.put(objFileKey, pollingWatchKey);
                } else {
                    pollingWatchKey.disable();
                }
            }
            pollingWatchKey.enable(set, sensitivityWatchEventModifier.sensitivityValueInSeconds());
        }
        return pollingWatchKey;
    }

    @Override
    void implClose() throws IOException {
        synchronized (this.map) {
            Iterator<Map.Entry<Object, PollingWatchKey>> it = this.map.entrySet().iterator();
            while (it.hasNext()) {
                PollingWatchKey value = it.next().getValue();
                value.disable();
                value.invalidate();
            }
            this.map.clear();
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                PollingWatchService.this.scheduledExecutor.shutdown();
                return null;
            }
        });
    }

    private static class CacheEntry {
        private long lastModified;
        private int lastTickCount;

        CacheEntry(long j, int i) {
            this.lastModified = j;
            this.lastTickCount = i;
        }

        int lastTickCount() {
            return this.lastTickCount;
        }

        long lastModified() {
            return this.lastModified;
        }

        void update(long j, int i) {
            this.lastModified = j;
            this.lastTickCount = i;
        }
    }

    private class PollingWatchKey extends AbstractWatchKey {
        private Map<Path, CacheEntry> entries;
        private Set<? extends WatchEvent.Kind<?>> events;
        private final Object fileKey;
        private ScheduledFuture<?> poller;
        private int tickCount;
        private volatile boolean valid;

        PollingWatchKey(Path path, PollingWatchService pollingWatchService, Object obj) throws IOException {
            super(path, pollingWatchService);
            this.fileKey = obj;
            this.valid = true;
            this.tickCount = 0;
            this.entries = new HashMap();
            try {
                DirectoryStream<Path> directoryStreamNewDirectoryStream = Files.newDirectoryStream(path);
                Throwable th = null;
                try {
                    try {
                        for (Path path2 : directoryStreamNewDirectoryStream) {
                            this.entries.put(path2.getFileName(), new CacheEntry(Files.getLastModifiedTime(path2, LinkOption.NOFOLLOW_LINKS).toMillis(), this.tickCount));
                        }
                        if (directoryStreamNewDirectoryStream != null) {
                            directoryStreamNewDirectoryStream.close();
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } finally {
                }
            } catch (DirectoryIteratorException e) {
                throw e.getCause();
            }
        }

        Object fileKey() {
            return this.fileKey;
        }

        @Override
        public boolean isValid() {
            return this.valid;
        }

        void invalidate() {
            this.valid = false;
        }

        void enable(Set<? extends WatchEvent.Kind<?>> set, long j) {
            synchronized (this) {
                this.events = set;
                this.poller = PollingWatchService.this.scheduledExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        PollingWatchKey.this.poll();
                    }
                }, j, j, TimeUnit.SECONDS);
            }
        }

        void disable() {
            synchronized (this) {
                if (this.poller != null) {
                    this.poller.cancel(false);
                }
            }
        }

        @Override
        public void cancel() {
            this.valid = false;
            synchronized (PollingWatchService.this.map) {
                PollingWatchService.this.map.remove(fileKey());
            }
            disable();
        }

        synchronized void poll() {
            if (this.valid) {
                this.tickCount++;
                try {
                    try {
                        DirectoryStream<Path> directoryStreamNewDirectoryStream = Files.newDirectoryStream(watchable());
                        try {
                            for (Path path : directoryStreamNewDirectoryStream) {
                                try {
                                    long millis = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
                                    CacheEntry cacheEntry = this.entries.get(path.getFileName());
                                    if (cacheEntry == null) {
                                        this.entries.put(path.getFileName(), new CacheEntry(millis, this.tickCount));
                                        if (this.events.contains(StandardWatchEventKinds.ENTRY_CREATE)) {
                                            signalEvent(StandardWatchEventKinds.ENTRY_CREATE, path.getFileName());
                                        } else if (this.events.contains(StandardWatchEventKinds.ENTRY_MODIFY)) {
                                            signalEvent(StandardWatchEventKinds.ENTRY_MODIFY, path.getFileName());
                                        }
                                    } else {
                                        if (cacheEntry.lastModified != millis && this.events.contains(StandardWatchEventKinds.ENTRY_MODIFY)) {
                                            signalEvent(StandardWatchEventKinds.ENTRY_MODIFY, path.getFileName());
                                        }
                                        cacheEntry.update(millis, this.tickCount);
                                    }
                                } catch (IOException e) {
                                }
                            }
                            directoryStreamNewDirectoryStream.close();
                        } catch (DirectoryIteratorException e2) {
                            directoryStreamNewDirectoryStream.close();
                        } catch (Throwable th) {
                            try {
                                directoryStreamNewDirectoryStream.close();
                            } catch (IOException e3) {
                            }
                            throw th;
                        }
                    } catch (IOException e4) {
                    }
                    Iterator<Map.Entry<Path, CacheEntry>> it = this.entries.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Path, CacheEntry> next = it.next();
                        if (next.getValue().lastTickCount() != this.tickCount) {
                            Path key = next.getKey();
                            it.remove();
                            if (this.events.contains(StandardWatchEventKinds.ENTRY_DELETE)) {
                                signalEvent(StandardWatchEventKinds.ENTRY_DELETE, key);
                            }
                        }
                    }
                } catch (IOException e5) {
                    cancel();
                    signal();
                }
            }
        }
    }
}
