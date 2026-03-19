package android.content;

import android.database.Cursor;
import android.os.RemoteException;

public abstract class CursorEntityIterator implements EntityIterator {
    private final Cursor mCursor;
    private boolean mIsClosed = false;

    public abstract Entity getEntityAndIncrementCursor(Cursor cursor) throws RemoteException;

    public CursorEntityIterator(Cursor cursor) {
        this.mCursor = cursor;
        this.mCursor.moveToFirst();
    }

    @Override
    public final boolean hasNext() {
        if (this.mIsClosed) {
            throw new IllegalStateException("calling hasNext() when the iterator is closed");
        }
        return !this.mCursor.isAfterLast();
    }

    @Override
    public Entity next() {
        if (this.mIsClosed) {
            throw new IllegalStateException("calling next() when the iterator is closed");
        }
        if (!hasNext()) {
            throw new IllegalStateException("you may only call next() if hasNext() is true");
        }
        try {
            return getEntityAndIncrementCursor(this.mCursor);
        } catch (RemoteException e) {
            throw new RuntimeException("caught a remote exception, this process will die soon", e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported by EntityIterators");
    }

    @Override
    public final void reset() {
        if (this.mIsClosed) {
            throw new IllegalStateException("calling reset() when the iterator is closed");
        }
        this.mCursor.moveToFirst();
    }

    @Override
    public final void close() {
        if (this.mIsClosed) {
            throw new IllegalStateException("closing when already closed");
        }
        this.mIsClosed = true;
        this.mCursor.close();
    }
}
