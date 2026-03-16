package android.database;

import android.database.AbstractCursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

public final class BulkCursorToCursorAdaptor extends AbstractWindowedCursor {
    private static final String TAG = "BulkCursor";
    private IBulkCursor mBulkCursor;
    private String[] mColumns;
    private int mCount;
    private AbstractCursor.SelfContentObserver mObserverBridge = new AbstractCursor.SelfContentObserver(this);
    private boolean mWantsAllOnMoveCalls;

    public void initialize(BulkCursorDescriptor d) {
        this.mBulkCursor = d.cursor;
        this.mColumns = d.columnNames;
        this.mRowIdColumnIndex = DatabaseUtils.findRowIdColumnIndex(this.mColumns);
        this.mWantsAllOnMoveCalls = d.wantsAllOnMoveCalls;
        this.mCount = d.count;
        if (d.window != null) {
            setWindow(d.window);
        }
    }

    public IContentObserver getObserver() {
        return this.mObserverBridge.getContentObserver();
    }

    private void throwIfCursorIsClosed() {
        if (this.mBulkCursor == null) {
            throw new StaleDataException("Attempted to access a cursor after it has been closed.");
        }
    }

    @Override
    public int getCount() {
        throwIfCursorIsClosed();
        return this.mCount;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        throwIfCursorIsClosed();
        try {
            if (this.mWindow == null || newPosition < this.mWindow.getStartPosition() || newPosition >= this.mWindow.getStartPosition() + this.mWindow.getNumRows()) {
                setWindow(this.mBulkCursor.getWindow(newPosition));
            } else if (this.mWantsAllOnMoveCalls) {
                this.mBulkCursor.onMove(newPosition);
            }
            return this.mWindow != null;
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get window because the remote process is dead");
            return false;
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (this.mBulkCursor != null) {
            try {
                this.mBulkCursor.deactivate();
            } catch (RemoteException e) {
                Log.w(TAG, "Remote process exception when deactivating");
            }
        }
    }

    @Override
    public void close() {
        super.close();
        if (this.mBulkCursor != null) {
            try {
                this.mBulkCursor.close();
            } catch (RemoteException e) {
                Log.w(TAG, "Remote process exception when closing");
            } finally {
                this.mBulkCursor = null;
            }
        }
    }

    @Override
    public boolean requery() {
        boolean z = false;
        throwIfCursorIsClosed();
        try {
            this.mCount = this.mBulkCursor.requery(getObserver());
            if (this.mCount != -1) {
                this.mPos = -1;
                closeWindow();
                super.requery();
                z = true;
            } else {
                deactivate();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unable to requery because the remote process exception " + ex.getMessage());
            deactivate();
        }
        return z;
    }

    @Override
    public String[] getColumnNames() {
        throwIfCursorIsClosed();
        return this.mColumns;
    }

    @Override
    public Bundle getExtras() {
        throwIfCursorIsClosed();
        try {
            return this.mBulkCursor.getExtras();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Bundle respond(Bundle extras) {
        throwIfCursorIsClosed();
        try {
            return this.mBulkCursor.respond(extras);
        } catch (RemoteException e) {
            Log.w(TAG, "respond() threw RemoteException, returning an empty bundle.", e);
            return Bundle.EMPTY;
        }
    }
}
