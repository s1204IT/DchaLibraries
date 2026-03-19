package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public abstract class AbstractSelectableChannel extends SelectableChannel {
    static final boolean $assertionsDisabled = false;
    private final SelectorProvider provider;
    private SelectionKey[] keys = null;
    private int keyCount = 0;
    private final Object keyLock = new Object();
    private final Object regLock = new Object();
    boolean blocking = true;

    protected abstract void implCloseSelectableChannel() throws IOException;

    protected abstract void implConfigureBlocking(boolean z) throws IOException;

    protected AbstractSelectableChannel(SelectorProvider selectorProvider) {
        this.provider = selectorProvider;
    }

    @Override
    public final SelectorProvider provider() {
        return this.provider;
    }

    private void addKey(SelectionKey selectionKey) {
        int i = 0;
        if (this.keys != null && this.keyCount < this.keys.length) {
            while (i < this.keys.length && this.keys[i] != null) {
                i++;
            }
        } else if (this.keys == null) {
            this.keys = new SelectionKey[3];
        } else {
            SelectionKey[] selectionKeyArr = new SelectionKey[this.keys.length * 2];
            while (i < this.keys.length) {
                selectionKeyArr[i] = this.keys[i];
                i++;
            }
            this.keys = selectionKeyArr;
            i = this.keyCount;
        }
        this.keys[i] = selectionKey;
        this.keyCount++;
    }

    private SelectionKey findKey(Selector selector) {
        synchronized (this.keyLock) {
            if (this.keys == null) {
                return null;
            }
            for (int i = 0; i < this.keys.length; i++) {
                if (this.keys[i] != null && this.keys[i].selector() == selector) {
                    return this.keys[i];
                }
            }
            return null;
        }
    }

    void removeKey(SelectionKey selectionKey) {
        synchronized (this.keyLock) {
            for (int i = 0; i < this.keys.length; i++) {
                if (this.keys[i] == selectionKey) {
                    this.keys[i] = null;
                    this.keyCount--;
                }
            }
            ((AbstractSelectionKey) selectionKey).invalidate();
        }
    }

    private boolean haveValidKeys() {
        synchronized (this.keyLock) {
            if (this.keyCount == 0) {
                return false;
            }
            for (int i = 0; i < this.keys.length; i++) {
                if (this.keys[i] != null && this.keys[i].isValid()) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public final boolean isRegistered() {
        boolean z;
        synchronized (this.keyLock) {
            z = this.keyCount != 0;
        }
        return z;
    }

    @Override
    public final SelectionKey keyFor(Selector selector) {
        return findKey(selector);
    }

    @Override
    public final SelectionKey register(Selector selector, int i, Object obj) throws ClosedChannelException {
        SelectionKey selectionKeyRegister;
        synchronized (this.regLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (((~validOps()) & i) != 0) {
                throw new IllegalArgumentException();
            }
            if (this.blocking) {
                throw new IllegalBlockingModeException();
            }
            SelectionKey selectionKeyFindKey = findKey(selector);
            if (selectionKeyFindKey != null) {
                selectionKeyFindKey.interestOps(i);
                selectionKeyFindKey.attach(obj);
            }
            if (selectionKeyFindKey == null) {
                synchronized (this.keyLock) {
                    if (!isOpen()) {
                        throw new ClosedChannelException();
                    }
                    selectionKeyRegister = ((AbstractSelector) selector).register(this, i, obj);
                    addKey(selectionKeyRegister);
                }
            } else {
                selectionKeyRegister = selectionKeyFindKey;
            }
        }
        return selectionKeyRegister;
    }

    @Override
    protected final void implCloseChannel() throws IOException {
        int length;
        implCloseSelectableChannel();
        synchronized (this.keyLock) {
            if (this.keys != null) {
                length = this.keys.length;
            } else {
                length = 0;
            }
            for (int i = 0; i < length; i++) {
                SelectionKey selectionKey = this.keys[i];
                if (selectionKey != null) {
                    selectionKey.cancel();
                }
            }
        }
    }

    @Override
    public final boolean isBlocking() {
        boolean z;
        synchronized (this.regLock) {
            z = this.blocking;
        }
        return z;
    }

    @Override
    public final Object blockingLock() {
        return this.regLock;
    }

    @Override
    public final SelectableChannel configureBlocking(boolean z) throws IOException {
        synchronized (this.regLock) {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.blocking == z) {
                return this;
            }
            if (z && haveValidKeys()) {
                throw new IllegalBlockingModeException();
            }
            implConfigureBlocking(z);
            this.blocking = z;
            return this;
        }
    }
}
