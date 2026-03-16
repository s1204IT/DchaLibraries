package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractSelector extends Selector {
    private SelectorProvider provider;
    private final AtomicBoolean isOpen = new AtomicBoolean(true);
    private final Set<SelectionKey> cancelledKeysSet = new HashSet();
    private final Runnable wakeupRunnable = new Runnable() {
        @Override
        public void run() {
            AbstractSelector.this.wakeup();
        }
    };

    protected abstract void implCloseSelector() throws IOException;

    protected abstract SelectionKey register(AbstractSelectableChannel abstractSelectableChannel, int i, Object obj);

    protected AbstractSelector(SelectorProvider selectorProvider) {
        this.provider = null;
        this.provider = selectorProvider;
    }

    @Override
    public final void close() throws IOException {
        if (this.isOpen.getAndSet(false)) {
            implCloseSelector();
        }
    }

    @Override
    public final boolean isOpen() {
        return this.isOpen.get();
    }

    @Override
    public final SelectorProvider provider() {
        return this.provider;
    }

    protected final Set<SelectionKey> cancelledKeys() {
        return this.cancelledKeysSet;
    }

    protected final void deregister(AbstractSelectionKey key) {
        ((AbstractSelectableChannel) key.channel()).deregister(key);
        key.isValid = false;
    }

    protected final void begin() {
        Thread.currentThread().pushInterruptAction$(this.wakeupRunnable);
    }

    protected final void end() {
        Thread.currentThread().popInterruptAction$(this.wakeupRunnable);
    }

    void cancel(SelectionKey key) {
        synchronized (this.cancelledKeysSet) {
            this.cancelledKeysSet.add(key);
        }
    }
}
