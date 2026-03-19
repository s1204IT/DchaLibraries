package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.InterruptibleChannel;
import sun.nio.ch.Interruptible;

public abstract class AbstractInterruptibleChannel implements Channel, InterruptibleChannel {
    private volatile Thread interrupted;
    private Interruptible interruptor;
    private final Object closeLock = new Object();
    private volatile boolean open = true;

    protected abstract void implCloseChannel() throws IOException;

    protected AbstractInterruptibleChannel() {
    }

    @Override
    public final void close() throws IOException {
        synchronized (this.closeLock) {
            if (this.open) {
                this.open = false;
                implCloseChannel();
            }
        }
    }

    @Override
    public final boolean isOpen() {
        return this.open;
    }

    protected final void begin() {
        if (this.interruptor == null) {
            this.interruptor = new Interruptible() {
                @Override
                public void interrupt(Thread thread) {
                    synchronized (AbstractInterruptibleChannel.this.closeLock) {
                        if (AbstractInterruptibleChannel.this.open) {
                            AbstractInterruptibleChannel.this.open = false;
                            AbstractInterruptibleChannel.this.interrupted = thread;
                            try {
                                AbstractInterruptibleChannel.this.implCloseChannel();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            };
        }
        blockedOn(this.interruptor);
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread.isInterrupted()) {
            this.interruptor.interrupt(threadCurrentThread);
        }
    }

    protected final void end(boolean z) throws AsynchronousCloseException {
        blockedOn(null);
        Thread thread = this.interrupted;
        if (thread != null && thread == Thread.currentThread()) {
            throw new ClosedByInterruptException();
        }
        if (!z && !this.open) {
            throw new AsynchronousCloseException();
        }
    }

    static void blockedOn(Interruptible interruptible) {
        Thread.currentThread().blockedOn(interruptible);
    }
}
