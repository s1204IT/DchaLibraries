package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.nio.ch.Interruptible;

public abstract class AbstractSelector extends Selector {
    private final SelectorProvider provider;
    private AtomicBoolean selectorOpen = new AtomicBoolean(true);
    private final Set<SelectionKey> cancelledKeys = new HashSet();
    private Interruptible interruptor = null;

    protected abstract void implCloseSelector() throws IOException;

    protected abstract SelectionKey register(AbstractSelectableChannel abstractSelectableChannel, int i, Object obj);

    protected AbstractSelector(SelectorProvider selectorProvider) {
        this.provider = selectorProvider;
    }

    void cancel(SelectionKey selectionKey) {
        synchronized (this.cancelledKeys) {
            this.cancelledKeys.add(selectionKey);
        }
    }

    @Override
    public final void close() throws IOException {
        if (!this.selectorOpen.getAndSet(false)) {
            return;
        }
        implCloseSelector();
    }

    @Override
    public final boolean isOpen() {
        return this.selectorOpen.get();
    }

    @Override
    public final SelectorProvider provider() {
        return this.provider;
    }

    protected final Set<SelectionKey> cancelledKeys() {
        return this.cancelledKeys;
    }

    protected final void deregister(AbstractSelectionKey abstractSelectionKey) {
        ((AbstractSelectableChannel) abstractSelectionKey.channel()).removeKey(abstractSelectionKey);
    }

    protected final void begin() {
        if (this.interruptor == null) {
            this.interruptor = new Interruptible() {
                @Override
                public void interrupt(Thread thread) {
                    AbstractSelector.this.wakeup();
                }
            };
        }
        AbstractInterruptibleChannel.blockedOn(this.interruptor);
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread.isInterrupted()) {
            this.interruptor.interrupt(threadCurrentThread);
        }
    }

    protected final void end() {
        AbstractInterruptibleChannel.blockedOn(null);
    }
}
