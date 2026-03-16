package android.database;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;

public final class CursorToBulkCursorAdaptor extends BulkCursorNative implements IBinder.DeathRecipient {
    private static final String TAG = "Cursor";
    private CrossProcessCursor mCursor;
    private CursorWindow mFilledWindow;
    private final Object mLock = new Object();
    private ContentObserverProxy mObserver;
    private final String mProviderName;

    private static final class ContentObserverProxy extends ContentObserver {
        protected IContentObserver mRemote;

        public ContentObserverProxy(IContentObserver remoteObserver, IBinder.DeathRecipient recipient) {
            super(null);
            this.mRemote = remoteObserver;
            try {
                remoteObserver.asBinder().linkToDeath(recipient, 0);
            } catch (RemoteException e) {
            }
        }

        public boolean unlinkToDeath(IBinder.DeathRecipient recipient) {
            return this.mRemote.asBinder().unlinkToDeath(recipient, 0);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            try {
                this.mRemote.onChange(selfChange, uri, Process.myUid());
            } catch (RemoteException e) {
            }
        }
    }

    public CursorToBulkCursorAdaptor(Cursor cursor, IContentObserver observer, String providerName) {
        if (cursor instanceof CrossProcessCursor) {
            this.mCursor = (CrossProcessCursor) cursor;
        } else {
            this.mCursor = new CrossProcessCursorWrapper(cursor);
        }
        this.mProviderName = providerName;
        synchronized (this.mLock) {
            createAndRegisterObserverProxyLocked(observer);
        }
    }

    private void closeFilledWindowLocked() {
        if (this.mFilledWindow != null) {
            this.mFilledWindow.close();
            this.mFilledWindow = null;
        }
    }

    private void disposeLocked() {
        if (this.mCursor != null) {
            unregisterObserverProxyLocked();
            this.mCursor.close();
            this.mCursor = null;
        }
        closeFilledWindowLocked();
    }

    private void throwIfCursorIsClosed() {
        if (this.mCursor == null) {
            throw new StaleDataException("Attempted to access a cursor after it has been closed.");
        }
    }

    @Override
    public void binderDied() {
        synchronized (this.mLock) {
            disposeLocked();
        }
    }

    public BulkCursorDescriptor getBulkCursorDescriptor() {
        BulkCursorDescriptor d;
        synchronized (this.mLock) {
            throwIfCursorIsClosed();
            d = new BulkCursorDescriptor();
            d.cursor = this;
            d.columnNames = this.mCursor.getColumnNames();
            d.wantsAllOnMoveCalls = this.mCursor.getWantsAllOnMoveCalls();
            d.count = this.mCursor.getCount();
            d.window = this.mCursor.getWindow();
            if (d.window != null) {
                d.window.acquireReference();
            }
        }
        return d;
    }

    @Override
    public CursorWindow getWindow(int position) {
        CursorWindow window;
        synchronized (this.mLock) {
            throwIfCursorIsClosed();
            if (!this.mCursor.moveToPosition(position)) {
                closeFilledWindowLocked();
                window = null;
            } else {
                window = this.mCursor.getWindow();
                if (window != null) {
                    closeFilledWindowLocked();
                } else {
                    window = this.mFilledWindow;
                    if (window == null) {
                        this.mFilledWindow = new CursorWindow(this.mProviderName);
                        window = this.mFilledWindow;
                    } else if (position < window.getStartPosition() || position >= window.getStartPosition() + window.getNumRows()) {
                        window.clear();
                    }
                    this.mCursor.fillWindow(position, window);
                }
                if (window != null) {
                    window.acquireReference();
                }
            }
        }
        return window;
    }

    @Override
    public void onMove(int position) {
        synchronized (this.mLock) {
            throwIfCursorIsClosed();
            this.mCursor.onMove(this.mCursor.getPosition(), position);
        }
    }

    @Override
    public void deactivate() {
        synchronized (this.mLock) {
            if (this.mCursor != null) {
                unregisterObserverProxyLocked();
                this.mCursor.deactivate();
            }
            closeFilledWindowLocked();
        }
    }

    @Override
    public void close() {
        synchronized (this.mLock) {
            disposeLocked();
        }
    }

    @Override
    public int requery(IContentObserver observer) {
        int count;
        synchronized (this.mLock) {
            throwIfCursorIsClosed();
            closeFilledWindowLocked();
            try {
                if (!this.mCursor.requery()) {
                    count = -1;
                } else {
                    unregisterObserverProxyLocked();
                    createAndRegisterObserverProxyLocked(observer);
                    count = this.mCursor.getCount();
                }
            } catch (IllegalStateException e) {
                IllegalStateException leakProgram = new IllegalStateException(this.mProviderName + " Requery misuse db, mCursor isClosed:" + this.mCursor.isClosed(), e);
                throw leakProgram;
            }
        }
        return count;
    }

    private void createAndRegisterObserverProxyLocked(IContentObserver observer) {
        if (this.mObserver != null) {
            throw new IllegalStateException("an observer is already registered");
        }
        this.mObserver = new ContentObserverProxy(observer, this);
        this.mCursor.registerContentObserver(this.mObserver);
    }

    private void unregisterObserverProxyLocked() {
        if (this.mObserver != null) {
            this.mCursor.unregisterContentObserver(this.mObserver);
            this.mObserver.unlinkToDeath(this);
            this.mObserver = null;
        }
    }

    @Override
    public Bundle getExtras() {
        Bundle extras;
        synchronized (this.mLock) {
            throwIfCursorIsClosed();
            extras = this.mCursor.getExtras();
        }
        return extras;
    }

    @Override
    public Bundle respond(Bundle extras) {
        Bundle bundleRespond;
        synchronized (this.mLock) {
            throwIfCursorIsClosed();
            bundleRespond = this.mCursor.respond(extras);
        }
        return bundleRespond;
    }
}
