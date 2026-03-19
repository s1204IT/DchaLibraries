package sun.nio.ch;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

public class SelectionKeyImpl extends AbstractSelectionKey {
    final SelChImpl channel;
    private int index;
    private volatile int interestOps;
    private int readyOps;
    public final SelectorImpl selector;

    SelectionKeyImpl(SelChImpl selChImpl, SelectorImpl selectorImpl) {
        this.channel = selChImpl;
        this.selector = selectorImpl;
    }

    @Override
    public SelectableChannel channel() {
        return (SelectableChannel) this.channel;
    }

    @Override
    public Selector selector() {
        return this.selector;
    }

    int getIndex() {
        return this.index;
    }

    void setIndex(int i) {
        this.index = i;
    }

    private void ensureValid() {
        if (!isValid()) {
            throw new CancelledKeyException();
        }
    }

    @Override
    public int interestOps() {
        ensureValid();
        return this.interestOps;
    }

    @Override
    public SelectionKey interestOps(int i) {
        ensureValid();
        return nioInterestOps(i);
    }

    @Override
    public int readyOps() {
        ensureValid();
        return this.readyOps;
    }

    public void nioReadyOps(int i) {
        this.readyOps = i;
    }

    public int nioReadyOps() {
        return this.readyOps;
    }

    public SelectionKey nioInterestOps(int i) {
        if (((~channel().validOps()) & i) != 0) {
            throw new IllegalArgumentException();
        }
        this.channel.translateAndSetInterestOps(i, this);
        this.interestOps = i;
        return this;
    }

    public int nioInterestOps() {
        return this.interestOps;
    }
}
