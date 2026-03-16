package java.nio.channels.spi;

import java.nio.channels.SelectionKey;

public abstract class AbstractSelectionKey extends SelectionKey {
    boolean isValid = true;

    protected AbstractSelectionKey() {
    }

    @Override
    public final boolean isValid() {
        return this.isValid;
    }

    @Override
    public final void cancel() {
        if (this.isValid) {
            this.isValid = false;
            ((AbstractSelector) selector()).cancel(this);
        }
    }
}
