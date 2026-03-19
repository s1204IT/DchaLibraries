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

    public void initialize(BulkCursorDescriptor bulkCursorDescriptor) {
        this.mBulkCursor = bulkCursorDescriptor.cursor;
        this.mColumns = bulkCursorDescriptor.columnNames;
        this.mWantsAllOnMoveCalls = bulkCursorDescriptor.wantsAllOnMoveCalls;
        this.mCount = bulkCursorDescriptor.count;
        if (bulkCursorDescriptor.window != null) {
            setWindow(bulkCursorDescriptor.window);
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
    public boolean onMove(int i, int i2) {
        throwIfCursorIsClosed();
        try {
            if (this.mWindow == null || i2 < this.mWindow.getStartPosition() || i2 >= this.mWindow.getStartPosition() + this.mWindow.getNumRows()) {
                setWindow(this.mBulkCursor.getWindow(i2));
            } else if (this.mWantsAllOnMoveCalls) {
                this.mBulkCursor.onMove(i2);
            }
            if (this.mWindow == null) {
                return false;
            }
            return true;
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
                try {
                    this.mBulkCursor.close();
                } catch (RemoteException e) {
                    Log.w(TAG, "Remote process exception when closing");
                }
            } finally {
                this.mBulkCursor = null;
            }
        }
    }

    @Override
    public boolean requery() {
        throwIfCursorIsClosed();
        try {
            this.mCount = this.mBulkCursor.requery(getObserver());
            if (this.mCount != -1) {
                this.mPos = -1;
                closeWindow();
                super.requery();
                return true;
            }
            deactivate();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unable to requery because the remote process exception " + e.getMessage());
            deactivate();
            return false;
        }
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
    public Bundle respond(Bundle bundle) {
        throwIfCursorIsClosed();
        try {
            return this.mBulkCursor.respond(bundle);
        } catch (RemoteException e) {
            Log.w(TAG, "respond() threw RemoteException, returning an empty bundle.", e);
            return Bundle.EMPTY;
        }
    }
}
