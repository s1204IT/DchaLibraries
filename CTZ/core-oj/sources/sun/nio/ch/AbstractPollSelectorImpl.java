package sun.nio.ch;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;

abstract class AbstractPollSelectorImpl extends SelectorImpl {
    static final boolean $assertionsDisabled = false;
    protected final int INIT_CAP;
    protected SelectionKeyImpl[] channelArray;
    protected int channelOffset;
    private Object closeLock;
    private boolean closed;
    PollArrayWrapper pollWrapper;
    protected int totalChannels;

    @Override
    protected abstract int doSelect(long j) throws IOException;

    protected abstract void implCloseInterrupt() throws IOException;

    AbstractPollSelectorImpl(SelectorProvider selectorProvider, int i, int i2) {
        super(selectorProvider);
        this.INIT_CAP = 10;
        this.channelOffset = 0;
        this.closed = false;
        this.closeLock = new Object();
        this.totalChannels = i;
        this.channelOffset = i2;
    }

    @Override
    public void putEventOps(SelectionKeyImpl selectionKeyImpl, int i) {
        synchronized (this.closeLock) {
            if (this.closed) {
                throw new ClosedSelectorException();
            }
            this.pollWrapper.putEventOps(selectionKeyImpl.getIndex(), i);
        }
    }

    @Override
    public Selector wakeup() {
        this.pollWrapper.interrupt();
        return this;
    }

    @Override
    protected void implClose() throws IOException {
        synchronized (this.closeLock) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            for (int i = this.channelOffset; i < this.totalChannels; i++) {
                SelectionKeyImpl selectionKeyImpl = this.channelArray[i];
                selectionKeyImpl.setIndex(-1);
                deregister(selectionKeyImpl);
                SelectableChannel selectableChannelChannel = this.channelArray[i].channel();
                if (!selectableChannelChannel.isOpen() && !selectableChannelChannel.isRegistered()) {
                    ((SelChImpl) selectableChannelChannel).kill();
                }
            }
            implCloseInterrupt();
            this.pollWrapper.free();
            this.pollWrapper = null;
            this.selectedKeys = null;
            this.channelArray = null;
            this.totalChannels = 0;
        }
    }

    protected int updateSelectedKeys() {
        int i = 0;
        for (int i2 = this.channelOffset; i2 < this.totalChannels; i2++) {
            int reventOps = this.pollWrapper.getReventOps(i2);
            if (reventOps != 0) {
                SelectionKeyImpl selectionKeyImpl = this.channelArray[i2];
                this.pollWrapper.putReventOps(i2, 0);
                if (this.selectedKeys.contains(selectionKeyImpl)) {
                    if (selectionKeyImpl.channel.translateAndSetReadyOps(reventOps, selectionKeyImpl)) {
                        i++;
                    }
                } else {
                    selectionKeyImpl.channel.translateAndSetReadyOps(reventOps, selectionKeyImpl);
                    if ((selectionKeyImpl.nioReadyOps() & selectionKeyImpl.nioInterestOps()) != 0) {
                        this.selectedKeys.add(selectionKeyImpl);
                        i++;
                    }
                }
            }
        }
        return i;
    }

    @Override
    protected void implRegister(SelectionKeyImpl selectionKeyImpl) {
        synchronized (this.closeLock) {
            if (this.closed) {
                throw new ClosedSelectorException();
            }
            if (this.channelArray.length == this.totalChannels) {
                int i = this.pollWrapper.totalChannels * 2;
                SelectionKeyImpl[] selectionKeyImplArr = new SelectionKeyImpl[i];
                for (int i2 = this.channelOffset; i2 < this.totalChannels; i2++) {
                    selectionKeyImplArr[i2] = this.channelArray[i2];
                }
                this.channelArray = selectionKeyImplArr;
                this.pollWrapper.grow(i);
            }
            this.channelArray[this.totalChannels] = selectionKeyImpl;
            selectionKeyImpl.setIndex(this.totalChannels);
            this.pollWrapper.addEntry(selectionKeyImpl.channel);
            this.totalChannels++;
            this.keys.add(selectionKeyImpl);
        }
    }

    @Override
    protected void implDereg(SelectionKeyImpl selectionKeyImpl) throws IOException {
        int index = selectionKeyImpl.getIndex();
        if (index != this.totalChannels - 1) {
            SelectionKeyImpl selectionKeyImpl2 = this.channelArray[this.totalChannels - 1];
            this.channelArray[index] = selectionKeyImpl2;
            selectionKeyImpl2.setIndex(index);
            this.pollWrapper.release(index);
            PollArrayWrapper.replaceEntry(this.pollWrapper, this.totalChannels - 1, this.pollWrapper, index);
        } else {
            this.pollWrapper.release(index);
        }
        this.channelArray[this.totalChannels - 1] = null;
        this.totalChannels--;
        PollArrayWrapper pollArrayWrapper = this.pollWrapper;
        pollArrayWrapper.totalChannels--;
        selectionKeyImpl.setIndex(-1);
        this.keys.remove(selectionKeyImpl);
        this.selectedKeys.remove(selectionKeyImpl);
        deregister(selectionKeyImpl);
        SelectableChannel selectableChannelChannel = selectionKeyImpl.channel();
        if (!selectableChannelChannel.isOpen() && !selectableChannelChannel.isRegistered()) {
            ((SelChImpl) selectableChannelChannel).kill();
        }
    }
}
