package sun.nio.fs;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

abstract class AbstractWatchKey implements WatchKey {
    static final boolean $assertionsDisabled = false;
    static final int MAX_EVENT_LIST_SIZE = 512;
    static final Event<Object> OVERFLOW_EVENT = new Event<>(StandardWatchEventKinds.OVERFLOW, null);
    private final Path dir;
    private final AbstractWatchService watcher;
    private State state = State.READY;
    private List<WatchEvent<?>> events = new ArrayList();
    private Map<Object, WatchEvent<?>> lastModifyEvents = new HashMap();

    private enum State {
        READY,
        SIGNALLED
    }

    protected AbstractWatchKey(Path path, AbstractWatchService abstractWatchService) {
        this.watcher = abstractWatchService;
        this.dir = path;
    }

    final AbstractWatchService watcher() {
        return this.watcher;
    }

    @Override
    public Path watchable() {
        return this.dir;
    }

    final void signal() {
        synchronized (this) {
            if (this.state == State.READY) {
                this.state = State.SIGNALLED;
                this.watcher.enqueueKey(this);
            }
        }
    }

    final void signalEvent(WatchEvent.Kind<?> kind, Object obj) {
        boolean z = kind == StandardWatchEventKinds.ENTRY_MODIFY;
        synchronized (this) {
            int size = this.events.size();
            if (size > 0) {
                WatchEvent<?> watchEvent = this.events.get(size - 1);
                if (watchEvent.kind() != StandardWatchEventKinds.OVERFLOW && (kind != watchEvent.kind() || !Objects.equals(obj, watchEvent.context()))) {
                    if (!this.lastModifyEvents.isEmpty()) {
                        if (z) {
                            WatchEvent<?> watchEvent2 = this.lastModifyEvents.get(obj);
                            if (watchEvent2 != null) {
                                ((Event) watchEvent2).increment();
                                return;
                            }
                        } else {
                            this.lastModifyEvents.remove(obj);
                        }
                    }
                    if (size >= 512) {
                        kind = StandardWatchEventKinds.OVERFLOW;
                        obj = null;
                        z = false;
                    }
                }
                ((Event) watchEvent).increment();
                return;
            }
            Event event = new Event(kind, obj);
            if (z) {
                this.lastModifyEvents.put(obj, event);
            } else if (kind == StandardWatchEventKinds.OVERFLOW) {
                this.events.clear();
                this.lastModifyEvents.clear();
            }
            this.events.add(event);
            signal();
        }
    }

    @Override
    public final List<WatchEvent<?>> pollEvents() {
        List<WatchEvent<?>> list;
        synchronized (this) {
            list = this.events;
            this.events = new ArrayList();
            this.lastModifyEvents.clear();
        }
        return list;
    }

    @Override
    public final boolean reset() {
        boolean zIsValid;
        synchronized (this) {
            if (this.state == State.SIGNALLED && isValid()) {
                if (this.events.isEmpty()) {
                    this.state = State.READY;
                } else {
                    this.watcher.enqueueKey(this);
                }
            }
            zIsValid = isValid();
        }
        return zIsValid;
    }

    private static class Event<T> implements WatchEvent<T> {
        private final T context;
        private int count = 1;
        private final WatchEvent.Kind<T> kind;

        Event(WatchEvent.Kind<T> kind, T t) {
            this.kind = kind;
            this.context = t;
        }

        @Override
        public WatchEvent.Kind<T> kind() {
            return this.kind;
        }

        @Override
        public T context() {
            return this.context;
        }

        @Override
        public int count() {
            return this.count;
        }

        void increment() {
            this.count++;
        }
    }
}
