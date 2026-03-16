package android.service.media;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.media.IMediaBrowserService;
import android.util.ArrayMap;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

public abstract class MediaBrowserService extends Service {
    private static final boolean DBG = false;
    public static final String SERVICE_INTERFACE = "android.media.browse.MediaBrowserService";
    private static final String TAG = "MediaBrowserService";
    private ServiceBinder mBinder;
    private final ArrayMap<IBinder, ConnectionRecord> mConnections = new ArrayMap<>();
    private final Handler mHandler = new Handler();
    MediaSession.Token mSession;

    public abstract BrowserRoot onGetRoot(String str, int i, Bundle bundle);

    public abstract void onLoadChildren(String str, Result<List<MediaBrowser.MediaItem>> result);

    private class ConnectionRecord {
        IMediaBrowserServiceCallbacks callbacks;
        String pkg;
        BrowserRoot root;
        Bundle rootHints;
        HashSet<String> subscriptions;

        private ConnectionRecord() {
            this.subscriptions = new HashSet<>();
        }
    }

    public class Result<T> {
        private Object mDebug;
        private boolean mDetachCalled;
        private boolean mSendResultCalled;

        Result(Object debug) {
            this.mDebug = debug;
        }

        public void sendResult(T result) {
            if (this.mSendResultCalled) {
                throw new IllegalStateException("sendResult() called twice for: " + this.mDebug);
            }
            this.mSendResultCalled = true;
            onResultSent(result);
        }

        public void detach() {
            if (this.mDetachCalled) {
                throw new IllegalStateException("detach() called when detach() had already been called for: " + this.mDebug);
            }
            if (this.mSendResultCalled) {
                throw new IllegalStateException("detach() called when sendResult() had already been called for: " + this.mDebug);
            }
            this.mDetachCalled = true;
        }

        boolean isDone() {
            return this.mDetachCalled || this.mSendResultCalled;
        }

        void onResultSent(T result) {
        }
    }

    private class ServiceBinder extends IMediaBrowserService.Stub {
        private ServiceBinder() {
        }

        @Override
        public void connect(final String pkg, final Bundle rootHints, final IMediaBrowserServiceCallbacks callbacks) {
            final int uid = Binder.getCallingUid();
            if (MediaBrowserService.this.isValidPackage(pkg, uid)) {
                MediaBrowserService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        IBinder b = callbacks.asBinder();
                        MediaBrowserService.this.mConnections.remove(b);
                        ConnectionRecord connection = new ConnectionRecord();
                        connection.pkg = pkg;
                        connection.rootHints = rootHints;
                        connection.callbacks = callbacks;
                        connection.root = MediaBrowserService.this.onGetRoot(pkg, uid, rootHints);
                        if (connection.root != null) {
                            try {
                                MediaBrowserService.this.mConnections.put(b, connection);
                                if (MediaBrowserService.this.mSession != null) {
                                    callbacks.onConnect(connection.root.getRootId(), MediaBrowserService.this.mSession, connection.root.getExtras());
                                    return;
                                }
                                return;
                            } catch (RemoteException e) {
                                Log.w(MediaBrowserService.TAG, "Calling onConnect() failed. Dropping client. pkg=" + pkg);
                                MediaBrowserService.this.mConnections.remove(b);
                                return;
                            }
                        }
                        Log.i(MediaBrowserService.TAG, "No root for client " + pkg + " from service " + getClass().getName());
                        try {
                            callbacks.onConnectFailed();
                        } catch (RemoteException e2) {
                            Log.w(MediaBrowserService.TAG, "Calling onConnectFailed() failed. Ignoring. pkg=" + pkg);
                        }
                    }
                });
                return;
            }
            throw new IllegalArgumentException("Package/uid mismatch: uid=" + uid + " package=" + pkg);
        }

        @Override
        public void disconnect(final IMediaBrowserServiceCallbacks callbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IBinder b = callbacks.asBinder();
                    ConnectionRecord old = (ConnectionRecord) MediaBrowserService.this.mConnections.remove(b);
                    if (old != null) {
                    }
                }
            });
        }

        @Override
        public void addSubscription(final String id, final IMediaBrowserServiceCallbacks callbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IBinder b = callbacks.asBinder();
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(b);
                    if (connection != null) {
                        MediaBrowserService.this.addSubscription(id, connection);
                    } else {
                        Log.w(MediaBrowserService.TAG, "addSubscription for callback that isn't registered id=" + id);
                    }
                }
            });
        }

        @Override
        public void removeSubscription(final String id, final IMediaBrowserServiceCallbacks callbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IBinder b = callbacks.asBinder();
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(b);
                    if (connection == null) {
                        Log.w(MediaBrowserService.TAG, "removeSubscription for callback that isn't registered id=" + id);
                    } else if (!connection.subscriptions.remove(id)) {
                        Log.w(MediaBrowserService.TAG, "removeSubscription called for " + id + " which is not subscribed");
                    }
                }
            });
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBinder = new ServiceBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return this.mBinder;
        }
        return null;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    public void setSessionToken(final MediaSession.Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Session token may not be null.");
        }
        if (this.mSession != null) {
            throw new IllegalStateException("The session token has already been set.");
        }
        this.mSession = token;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IBinder key : MediaBrowserService.this.mConnections.keySet()) {
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(key);
                    try {
                        connection.callbacks.onConnect(connection.root.getRootId(), token, connection.root.getExtras());
                    } catch (RemoteException e) {
                        Log.w(MediaBrowserService.TAG, "Connection for " + connection.pkg + " is no longer valid.");
                        MediaBrowserService.this.mConnections.remove(key);
                    }
                }
            }
        });
    }

    public MediaSession.Token getSessionToken() {
        return this.mSession;
    }

    public void notifyChildrenChanged(final String parentId) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId cannot be null in notifyChildrenChanged");
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IBinder binder : MediaBrowserService.this.mConnections.keySet()) {
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(binder);
                    if (connection.subscriptions.contains(parentId)) {
                        MediaBrowserService.this.performLoadChildren(parentId, connection);
                    }
                }
            }
        });
    }

    private boolean isValidPackage(String pkg, int uid) {
        if (pkg == null) {
            return false;
        }
        PackageManager pm = getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        for (String str : packages) {
            if (str.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    private void addSubscription(String id, ConnectionRecord connection) {
        boolean added = connection.subscriptions.add(id);
        if (added) {
            performLoadChildren(id, connection);
        }
    }

    private void performLoadChildren(final String parentId, final ConnectionRecord connection) {
        Result<List<MediaBrowser.MediaItem>> result = new Result<List<MediaBrowser.MediaItem>>(parentId) {
            @Override
            void onResultSent(List<MediaBrowser.MediaItem> list) {
                if (list != null) {
                    if (MediaBrowserService.this.mConnections.get(connection.callbacks.asBinder()) == connection) {
                        ParceledListSlice<MediaBrowser.MediaItem> pls = new ParceledListSlice<>(list);
                        try {
                            connection.callbacks.onLoadChildren(parentId, pls);
                            return;
                        } catch (RemoteException e) {
                            Log.w(MediaBrowserService.TAG, "Calling onLoadChildren() failed for id=" + parentId + " package=" + connection.pkg);
                            return;
                        }
                    }
                    return;
                }
                throw new IllegalStateException("onLoadChildren sent null list for id " + parentId);
            }
        };
        onLoadChildren(parentId, result);
        if (!result.isDone()) {
            throw new IllegalStateException("onLoadChildren must call detach() or sendResult() before returning for package=" + connection.pkg + " id=" + parentId);
        }
    }

    public static final class BrowserRoot {
        private final Bundle mExtras;
        private final String mRootId;

        public BrowserRoot(String rootId, Bundle extras) {
            if (rootId == null) {
                throw new IllegalArgumentException("The root id in BrowserRoot cannot be null. Use null for BrowserRoot instead.");
            }
            this.mRootId = rootId;
            this.mExtras = extras;
        }

        public String getRootId() {
            return this.mRootId;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }
    }
}
