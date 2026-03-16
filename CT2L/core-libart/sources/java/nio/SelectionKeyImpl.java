package java.nio;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;

final class SelectionKeyImpl extends AbstractSelectionKey {
    private AbstractSelectableChannel channel;
    private int interestOps;
    private int readyOps;
    private SelectorImpl selector;

    public SelectionKeyImpl(AbstractSelectableChannel channel, int operations, Object attachment, SelectorImpl selector) {
        this.channel = channel;
        this.interestOps = operations;
        this.selector = selector;
        attach(attachment);
    }

    @Override
    public SelectableChannel channel() {
        return this.channel;
    }

    @Override
    public int interestOps() {
        int i;
        checkValid();
        synchronized (this.selector.keysLock) {
            i = this.interestOps;
        }
        return i;
    }

    int interestOpsNoCheck() {
        int i;
        synchronized (this.selector.keysLock) {
            i = this.interestOps;
        }
        return i;
    }

    @Override
    public SelectionKey interestOps(int operations) {
        checkValid();
        if (((channel().validOps() ^ (-1)) & operations) != 0) {
            throw new IllegalArgumentException();
        }
        synchronized (this.selector.keysLock) {
            this.interestOps = operations;
        }
        return this;
    }

    @Override
    public int readyOps() {
        checkValid();
        return this.readyOps;
    }

    @Override
    public Selector selector() {
        return this.selector;
    }

    void setReadyOps(int readyOps) {
        this.readyOps = this.interestOps & readyOps;
    }

    private void checkValid() {
        if (!isValid()) {
            throw new CancelledKeyException();
        }
    }

    boolean isConnected() {
        return !(this.channel instanceof SocketChannel) || ((SocketChannel) this.channel).isConnected();
    }
}
