package java.nio.channels.spi;

import java.nio.channels.SelectionKey;

public abstract class AbstractSelectionKey extends SelectionKey {
    private volatile boolean valid = true;

    protected AbstractSelectionKey() {
    }

    @Override
    public final boolean isValid() {
        return this.valid;
    }

    void invalidate() {
        this.valid = false;
    }

    @Override
    public final void cancel() {
        synchronized (this) {
            if (this.valid) {
                this.valid = false;
                ((AbstractSelector) selector()).cancel(this);
            }
        }
    }
}
