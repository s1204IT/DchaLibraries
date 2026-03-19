package sun.nio.ch;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class SelectorImpl extends AbstractSelector {
    protected HashSet<SelectionKey> keys;
    private Set<SelectionKey> publicKeys;
    private Set<SelectionKey> publicSelectedKeys;
    protected Set<SelectionKey> selectedKeys;

    protected abstract int doSelect(long j) throws IOException;

    protected abstract void implClose() throws IOException;

    protected abstract void implDereg(SelectionKeyImpl selectionKeyImpl) throws IOException;

    protected abstract void implRegister(SelectionKeyImpl selectionKeyImpl);

    @Override
    public abstract Selector wakeup();

    protected SelectorImpl(SelectorProvider selectorProvider) {
        super(selectorProvider);
        this.keys = new HashSet<>();
        this.selectedKeys = new HashSet();
        if (Util.atBugLevel("1.4")) {
            this.publicKeys = this.keys;
            this.publicSelectedKeys = this.selectedKeys;
        } else {
            this.publicKeys = Collections.unmodifiableSet(this.keys);
            this.publicSelectedKeys = Util.ungrowableSet(this.selectedKeys);
        }
    }

    @Override
    public Set<SelectionKey> keys() {
        if (!isOpen() && !Util.atBugLevel("1.4")) {
            throw new ClosedSelectorException();
        }
        return this.publicKeys;
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        if (!isOpen() && !Util.atBugLevel("1.4")) {
            throw new ClosedSelectorException();
        }
        return this.publicSelectedKeys;
    }

    private int lockAndDoSelect(long j) throws IOException {
        int iDoSelect;
        synchronized (this) {
            if (!isOpen()) {
                throw new ClosedSelectorException();
            }
            synchronized (this.publicKeys) {
                synchronized (this.publicSelectedKeys) {
                    iDoSelect = doSelect(j);
                }
            }
        }
        return iDoSelect;
    }

    @Override
    public int select(long j) throws IOException {
        if (j < 0) {
            throw new IllegalArgumentException("Negative timeout");
        }
        if (j == 0) {
            j = -1;
        }
        return lockAndDoSelect(j);
    }

    @Override
    public int select() throws IOException {
        return select(0L);
    }

    @Override
    public int selectNow() throws IOException {
        return lockAndDoSelect(0L);
    }

    @Override
    public void implCloseSelector() throws IOException {
        wakeup();
        synchronized (this) {
            synchronized (this.publicKeys) {
                synchronized (this.publicSelectedKeys) {
                    implClose();
                }
            }
        }
    }

    public void putEventOps(SelectionKeyImpl selectionKeyImpl, int i) {
    }

    @Override
    protected final SelectionKey register(AbstractSelectableChannel abstractSelectableChannel, int i, Object obj) {
        if (!(abstractSelectableChannel instanceof SelChImpl)) {
            throw new IllegalSelectorException();
        }
        SelectionKeyImpl selectionKeyImpl = new SelectionKeyImpl((SelChImpl) abstractSelectableChannel, this);
        selectionKeyImpl.attach(obj);
        synchronized (this.publicKeys) {
            implRegister(selectionKeyImpl);
        }
        selectionKeyImpl.interestOps(i);
        return selectionKeyImpl;
    }

    void processDeregisterQueue() throws IOException {
        Set<SelectionKey> setCancelledKeys = cancelledKeys();
        synchronized (setCancelledKeys) {
            if (!setCancelledKeys.isEmpty()) {
                Iterator<SelectionKey> it = setCancelledKeys.iterator();
                while (it.hasNext()) {
                    try {
                        try {
                            implDereg((SelectionKeyImpl) it.next());
                        } catch (SocketException e) {
                            throw new IOException("Error deregistering key", e);
                        }
                    } finally {
                        it.remove();
                    }
                }
            }
        }
    }
}
