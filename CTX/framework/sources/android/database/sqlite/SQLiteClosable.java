package android.database.sqlite;

import java.io.Closeable;

public abstract class SQLiteClosable implements Closeable {
    private int mReferenceCount = 1;

    protected abstract void onAllReferencesReleased();

    @Deprecated
    protected void onAllReferencesReleasedFromContainer() {
        onAllReferencesReleased();
    }

    public void acquireReference() {
        synchronized (this) {
            if (this.mReferenceCount <= 0) {
                throw new IllegalStateException("attempt to re-open an already-closed object: " + this);
            }
            this.mReferenceCount++;
        }
    }

    public void releaseReference() {
        boolean z;
        synchronized (this) {
            z = true;
            int i = this.mReferenceCount - 1;
            this.mReferenceCount = i;
            if (i != 0) {
                z = false;
            }
        }
        if (z) {
            onAllReferencesReleased();
        }
    }

    @Deprecated
    public void releaseReferenceFromContainer() {
        boolean z;
        synchronized (this) {
            z = true;
            int i = this.mReferenceCount - 1;
            this.mReferenceCount = i;
            if (i != 0) {
                z = false;
            }
        }
        if (z) {
            onAllReferencesReleasedFromContainer();
        }
    }

    @Override
    public void close() {
        releaseReference();
    }
}
