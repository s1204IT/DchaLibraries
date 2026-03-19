package android.database;

import android.database.IContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;

public abstract class ContentObserver {
    Handler mHandler;
    private final Object mLock = new Object();
    private Transport mTransport;

    public ContentObserver(Handler handler) {
        this.mHandler = handler;
    }

    public IContentObserver getContentObserver() {
        Transport transport;
        synchronized (this.mLock) {
            if (this.mTransport == null) {
                this.mTransport = new Transport(this);
            }
            transport = this.mTransport;
        }
        return transport;
    }

    public IContentObserver releaseContentObserver() {
        Transport transport;
        synchronized (this.mLock) {
            transport = this.mTransport;
            if (transport != null) {
                transport.releaseContentObserver();
                this.mTransport = null;
            }
        }
        return transport;
    }

    public boolean deliverSelfNotifications() {
        return false;
    }

    public void onChange(boolean z) {
    }

    public void onChange(boolean z, Uri uri) {
        onChange(z);
    }

    public void onChange(boolean z, Uri uri, int i) {
        onChange(z, uri);
    }

    @Deprecated
    public final void dispatchChange(boolean z) {
        dispatchChange(z, null);
    }

    public final void dispatchChange(boolean z, Uri uri) {
        dispatchChange(z, uri, UserHandle.getCallingUserId());
    }

    private void dispatchChange(boolean z, Uri uri, int i) {
        if (this.mHandler == null) {
            onChange(z, uri, i);
        } else {
            this.mHandler.post(new NotificationRunnable(z, uri, i));
        }
    }

    private final class NotificationRunnable implements Runnable {
        private final boolean mSelfChange;
        private final Uri mUri;
        private final int mUserId;

        public NotificationRunnable(boolean z, Uri uri, int i) {
            this.mSelfChange = z;
            this.mUri = uri;
            this.mUserId = i;
        }

        @Override
        public void run() {
            ContentObserver.this.onChange(this.mSelfChange, this.mUri, this.mUserId);
        }
    }

    private static final class Transport extends IContentObserver.Stub {
        private ContentObserver mContentObserver;

        public Transport(ContentObserver contentObserver) {
            this.mContentObserver = contentObserver;
        }

        @Override
        public void onChange(boolean z, Uri uri, int i) {
            ContentObserver contentObserver = this.mContentObserver;
            if (contentObserver != null) {
                contentObserver.dispatchChange(z, uri, i);
            }
        }

        public void releaseContentObserver() {
            this.mContentObserver = null;
        }
    }
}
