package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractSelectableChannel extends SelectableChannel {
    private final SelectorProvider provider;
    private List<SelectionKey> keyList = new ArrayList();
    private final Object blockingLock = new Object();
    boolean isBlocking = true;

    protected abstract void implCloseSelectableChannel() throws IOException;

    protected abstract void implConfigureBlocking(boolean z) throws IOException;

    protected AbstractSelectableChannel(SelectorProvider selectorProvider) {
        this.provider = selectorProvider;
    }

    @Override
    public final SelectorProvider provider() {
        return this.provider;
    }

    @Override
    public final synchronized boolean isRegistered() {
        return !this.keyList.isEmpty();
    }

    @Override
    public final synchronized SelectionKey keyFor(Selector selector) {
        SelectionKey key;
        Iterator<SelectionKey> it = this.keyList.iterator();
        while (true) {
            if (!it.hasNext()) {
                key = null;
                break;
            }
            key = it.next();
            if (key != null && key.selector() == selector) {
                break;
            }
        }
        return key;
    }

    @Override
    public final SelectionKey register(Selector selector, int interestSet, Object attachment) throws ClosedChannelException {
        SelectionKey key;
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        if (((validOps() ^ (-1)) & interestSet) != 0) {
            throw new IllegalArgumentException("no valid ops in interest set: " + interestSet);
        }
        synchronized (this.blockingLock) {
            if (this.isBlocking) {
                throw new IllegalBlockingModeException();
            }
            if (!selector.isOpen()) {
                if (interestSet == 0) {
                    throw new IllegalSelectorException();
                }
                throw new NullPointerException("selector not open");
            }
            key = keyFor(selector);
            if (key == null) {
                key = ((AbstractSelector) selector).register(this, interestSet, attachment);
                this.keyList.add(key);
            } else {
                if (!key.isValid()) {
                    throw new CancelledKeyException();
                }
                key.interestOps(interestSet);
                key.attach(attachment);
            }
        }
        return key;
    }

    @Override
    protected final synchronized void implCloseChannel() throws IOException {
        implCloseSelectableChannel();
        for (SelectionKey key : this.keyList) {
            if (key != null) {
                key.cancel();
            }
        }
    }

    @Override
    public final boolean isBlocking() {
        boolean z;
        synchronized (this.blockingLock) {
            z = this.isBlocking;
        }
        return z;
    }

    @Override
    public final Object blockingLock() {
        return this.blockingLock;
    }

    @Override
    public final SelectableChannel configureBlocking(boolean blockingMode) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        synchronized (this.blockingLock) {
            if (this.isBlocking != blockingMode) {
                if (blockingMode && containsValidKeys()) {
                    throw new IllegalBlockingModeException();
                }
                implConfigureBlocking(blockingMode);
                this.isBlocking = blockingMode;
            }
        }
        return this;
    }

    synchronized void deregister(SelectionKey k) {
        if (this.keyList != null) {
            this.keyList.remove(k);
        }
    }

    private synchronized boolean containsValidKeys() {
        boolean z;
        Iterator<SelectionKey> it = this.keyList.iterator();
        while (true) {
            if (!it.hasNext()) {
                z = false;
                break;
            }
            SelectionKey key = it.next();
            if (key != null && key.isValid()) {
                z = true;
                break;
            }
        }
        return z;
    }
}
