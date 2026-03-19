package sun.nio.ch;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ShutdownChannelGroupException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class Port extends AsynchronousChannelGroupImpl {
    protected final Map<Integer, PollableChannel> fdToChannel;
    protected final ReadWriteLock fdToChannelLock;

    interface PollableChannel extends Closeable {
        void onEvent(int i, boolean z);
    }

    abstract void startPoll(int i, int i2);

    Port(AsynchronousChannelProvider asynchronousChannelProvider, ThreadPool threadPool) {
        super(asynchronousChannelProvider, threadPool);
        this.fdToChannelLock = new ReentrantReadWriteLock();
        this.fdToChannel = new HashMap();
    }

    final void register(int i, PollableChannel pollableChannel) {
        this.fdToChannelLock.writeLock().lock();
        try {
            if (isShutdown()) {
                throw new ShutdownChannelGroupException();
            }
            this.fdToChannel.put(Integer.valueOf(i), pollableChannel);
        } finally {
            this.fdToChannelLock.writeLock().unlock();
        }
    }

    protected void preUnregister(int i) {
    }

    final void unregister(int i) {
        boolean z;
        preUnregister(i);
        this.fdToChannelLock.writeLock().lock();
        try {
            this.fdToChannel.remove(Integer.valueOf(i));
            if (this.fdToChannel.isEmpty()) {
                z = true;
            } else {
                z = false;
            }
            if (z && isShutdown()) {
                try {
                    shutdownNow();
                } catch (IOException e) {
                }
            }
        } finally {
            this.fdToChannelLock.writeLock().unlock();
        }
    }

    @Override
    final boolean isEmpty() {
        this.fdToChannelLock.writeLock().lock();
        try {
            return this.fdToChannel.isEmpty();
        } finally {
            this.fdToChannelLock.writeLock().unlock();
        }
    }

    @Override
    final Object attachForeignChannel(final Channel channel, FileDescriptor fileDescriptor) {
        int iFdVal = IOUtil.fdVal(fileDescriptor);
        register(iFdVal, new PollableChannel() {
            @Override
            public void onEvent(int i, boolean z) {
            }

            @Override
            public void close() throws IOException {
                channel.close();
            }
        });
        return Integer.valueOf(iFdVal);
    }

    @Override
    final void detachForeignChannel(Object obj) {
        unregister(((Integer) obj).intValue());
    }

    @Override
    final void closeAllChannels() {
        int i;
        PollableChannel[] pollableChannelArr = new PollableChannel[128];
        do {
            this.fdToChannelLock.writeLock().lock();
            try {
                Iterator<Integer> it = this.fdToChannel.keySet().iterator();
                i = 0;
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    int i2 = i + 1;
                    pollableChannelArr[i] = this.fdToChannel.get(it.next());
                    if (i2 < 128) {
                        i = i2;
                    } else {
                        i = i2;
                        break;
                    }
                }
                for (int i3 = 0; i3 < i; i3++) {
                    try {
                        pollableChannelArr[i3].close();
                    } catch (IOException e) {
                    }
                }
            } finally {
                this.fdToChannelLock.writeLock().unlock();
            }
        } while (i > 0);
    }
}
