package android.service.media;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowserUtils;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.media.IMediaBrowserService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class MediaBrowserService extends Service {
    private static final boolean DBG = false;
    public static final String KEY_MEDIA_ITEM = "media_item";
    private static final int RESULT_FLAG_OPTION_NOT_HANDLED = 1;
    public static final String SERVICE_INTERFACE = "android.media.browse.MediaBrowserService";
    private static final String TAG = "MediaBrowserService";
    private ServiceBinder mBinder;
    private ConnectionRecord mCurConnection;
    MediaSession.Token mSession;
    private final ArrayMap<IBinder, ConnectionRecord> mConnections = new ArrayMap<>();
    private final Handler mHandler = new Handler();

    public abstract BrowserRoot onGetRoot(String str, int i, Bundle bundle);

    public abstract void onLoadChildren(String str, Result<List<MediaBrowser.MediaItem>> result);

    private class ConnectionRecord {
        IMediaBrowserServiceCallbacks callbacks;
        String pkg;
        BrowserRoot root;
        Bundle rootHints;
        HashMap<String, List<Pair<IBinder, Bundle>>> subscriptions;

        ConnectionRecord(MediaBrowserService this$0, ConnectionRecord connectionRecord) {
            this();
        }

        private ConnectionRecord() {
            this.subscriptions = new HashMap<>();
        }
    }

    public class Result<T> {
        private Object mDebug;
        private boolean mDetachCalled;
        private int mFlags;
        private boolean mSendResultCalled;

        Result(Object debug) {
            this.mDebug = debug;
        }

        public void sendResult(T result) {
            if (this.mSendResultCalled) {
                throw new IllegalStateException("sendResult() called twice for: " + this.mDebug);
            }
            this.mSendResultCalled = true;
            onResultSent(result, this.mFlags);
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
            if (this.mDetachCalled) {
                return true;
            }
            return this.mSendResultCalled;
        }

        void setFlags(int flags) {
            this.mFlags = flags;
        }

        void onResultSent(T result, int flags) {
        }
    }

    private class ServiceBinder extends IMediaBrowserService.Stub {
        ServiceBinder(MediaBrowserService this$0, ServiceBinder serviceBinder) {
            this();
        }

        private ServiceBinder() {
        }

        @Override
        public void connect(final String pkg, final Bundle rootHints, final IMediaBrowserServiceCallbacks callbacks) {
            final int uid = Binder.getCallingUid();
            if (!MediaBrowserService.this.isValidPackage(pkg, uid)) {
                throw new IllegalArgumentException("Package/uid mismatch: uid=" + uid + " package=" + pkg);
            }
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IBinder b = callbacks.asBinder();
                    MediaBrowserService.this.mConnections.remove(b);
                    ConnectionRecord connection = new ConnectionRecord(MediaBrowserService.this, null);
                    connection.pkg = pkg;
                    connection.rootHints = rootHints;
                    connection.callbacks = callbacks;
                    connection.root = MediaBrowserService.this.onGetRoot(pkg, uid, rootHints);
                    if (connection.root == null) {
                        Log.i(MediaBrowserService.TAG, "No root for client " + pkg + " from service " + getClass().getName());
                        try {
                            callbacks.onConnectFailed();
                            return;
                        } catch (RemoteException e) {
                            Log.w(MediaBrowserService.TAG, "Calling onConnectFailed() failed. Ignoring. pkg=" + pkg);
                            return;
                        }
                    }
                    try {
                        MediaBrowserService.this.mConnections.put(b, connection);
                        if (MediaBrowserService.this.mSession == null) {
                            return;
                        }
                        callbacks.onConnect(connection.root.getRootId(), MediaBrowserService.this.mSession, connection.root.getExtras());
                    } catch (RemoteException e2) {
                        Log.w(MediaBrowserService.TAG, "Calling onConnect() failed. Dropping client. pkg=" + pkg);
                        MediaBrowserService.this.mConnections.remove(b);
                    }
                }
            });
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
        public void addSubscriptionDeprecated(String id, IMediaBrowserServiceCallbacks callbacks) {
        }

        @Override
        public void addSubscription(final String id, final IBinder token, final Bundle options, final IMediaBrowserServiceCallbacks callbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IBinder b = callbacks.asBinder();
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(b);
                    if (connection == null) {
                        Log.w(MediaBrowserService.TAG, "addSubscription for callback that isn't registered id=" + id);
                    } else {
                        MediaBrowserService.this.addSubscription(id, connection, token, options);
                    }
                }
            });
        }

        @Override
        public void removeSubscriptionDeprecated(String id, IMediaBrowserServiceCallbacks callbacks) {
        }

        @Override
        public void removeSubscription(final String id, final IBinder token, final IMediaBrowserServiceCallbacks callbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IBinder b = callbacks.asBinder();
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(b);
                    if (connection == null) {
                        Log.w(MediaBrowserService.TAG, "removeSubscription for callback that isn't registered id=" + id);
                    } else {
                        if (MediaBrowserService.this.removeSubscription(id, connection, token)) {
                            return;
                        }
                        Log.w(MediaBrowserService.TAG, "removeSubscription called for " + id + " which is not subscribed");
                    }
                }
            });
        }

        @Override
        public void getMediaItem(final String mediaId, final ResultReceiver receiver, final IMediaBrowserServiceCallbacks callbacks) {
            if (TextUtils.isEmpty(mediaId) || receiver == null) {
                return;
            }
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IBinder b = callbacks.asBinder();
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(b);
                    if (connection == null) {
                        Log.w(MediaBrowserService.TAG, "getMediaItem for callback that isn't registered id=" + mediaId);
                    } else {
                        MediaBrowserService.this.performLoadItem(mediaId, connection, receiver);
                    }
                }
            });
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBinder = new ServiceBinder(this, null);
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

    public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result, Bundle options) {
        result.setFlags(1);
        onLoadChildren(parentId, result);
    }

    public void onLoadItem(String itemId, Result<MediaBrowser.MediaItem> result) {
        result.sendResult(null);
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

    public final Bundle getBrowserRootHints() {
        if (this.mCurConnection == null) {
            throw new IllegalStateException("This should be called inside of onLoadChildren or onLoadItem methods");
        }
        if (this.mCurConnection.rootHints == null) {
            return null;
        }
        return new Bundle(this.mCurConnection.rootHints);
    }

    public void notifyChildrenChanged(String parentId) {
        notifyChildrenChangedInternal(parentId, null);
    }

    public void notifyChildrenChanged(String parentId, Bundle options) {
        if (options == null) {
            throw new IllegalArgumentException("options cannot be null in notifyChildrenChanged");
        }
        notifyChildrenChangedInternal(parentId, options);
    }

    private void notifyChildrenChangedInternal(final String parentId, final Bundle options) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId cannot be null in notifyChildrenChanged");
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IBinder binder : MediaBrowserService.this.mConnections.keySet()) {
                    ConnectionRecord connection = (ConnectionRecord) MediaBrowserService.this.mConnections.get(binder);
                    List<Pair<IBinder, Bundle>> callbackList = connection.subscriptions.get(parentId);
                    if (callbackList != null) {
                        for (Pair<IBinder, Bundle> callback : callbackList) {
                            if (MediaBrowserUtils.hasDuplicatedItems(options, (Bundle) callback.second)) {
                                MediaBrowserService.this.performLoadChildren(parentId, connection, (Bundle) callback.second);
                            }
                        }
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

    private void addSubscription(String id, ConnectionRecord connection, IBinder token, Bundle options) {
        List<Pair<IBinder, Bundle>> callbackList = connection.subscriptions.get(id);
        if (callbackList == null) {
            callbackList = new ArrayList<>();
        }
        for (Pair<IBinder, Bundle> callback : callbackList) {
            if (token == callback.first && MediaBrowserUtils.areSameOptions(options, (Bundle) callback.second)) {
                return;
            }
        }
        callbackList.add(new Pair<>(token, options));
        connection.subscriptions.put(id, callbackList);
        performLoadChildren(id, connection, options);
    }

    private boolean removeSubscription(String id, ConnectionRecord connection, IBinder token) {
        if (token == null) {
            return connection.subscriptions.remove(id) != null;
        }
        boolean removed = false;
        List<Pair<IBinder, Bundle>> callbackList = connection.subscriptions.get(id);
        if (callbackList != null) {
            for (Pair<IBinder, Bundle> callback : callbackList) {
                if (token == callback.first) {
                    removed = true;
                    callbackList.remove(callback);
                }
            }
            if (callbackList.size() == 0) {
                connection.subscriptions.remove(id);
            }
        }
        return removed;
    }

    private void performLoadChildren(final String parentId, final ConnectionRecord connection, final Bundle options) {
        Result<List<MediaBrowser.MediaItem>> result = new Result<List<MediaBrowser.MediaItem>>(this, parentId) {
            @Override
            void onResultSent(List<MediaBrowser.MediaItem> list, int flag) {
                if (this.mConnections.get(connection.callbacks.asBinder()) != connection) {
                    return;
                }
                List<MediaBrowser.MediaItem> filteredList = (flag & 1) != 0 ? this.applyOptions(list, options) : list;
                try {
                    connection.callbacks.onLoadChildrenWithOptions(parentId, filteredList == null ? null : new ParceledListSlice(filteredList), options);
                } catch (RemoteException e) {
                    Log.w(MediaBrowserService.TAG, "Calling onLoadChildren() failed for id=" + parentId + " package=" + connection.pkg);
                }
            }
        };
        this.mCurConnection = connection;
        if (options == null) {
            onLoadChildren(parentId, result);
        } else {
            onLoadChildren(parentId, result, options);
        }
        this.mCurConnection = null;
        if (result.isDone()) {
        } else {
            throw new IllegalStateException("onLoadChildren must call detach() or sendResult() before returning for package=" + connection.pkg + " id=" + parentId);
        }
    }

    private List<MediaBrowser.MediaItem> applyOptions(List<MediaBrowser.MediaItem> list, Bundle options) {
        if (list == null) {
            return null;
        }
        int page = options.getInt(MediaBrowser.EXTRA_PAGE, -1);
        int pageSize = options.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1);
        if (page == -1 && pageSize == -1) {
            return list;
        }
        int fromIndex = pageSize * page;
        int toIndex = fromIndex + pageSize;
        if (page < 0 || pageSize < 1 || fromIndex >= list.size()) {
            return Collections.EMPTY_LIST;
        }
        if (toIndex > list.size()) {
            toIndex = list.size();
        }
        return list.subList(fromIndex, toIndex);
    }

    private void performLoadItem(String itemId, ConnectionRecord connection, final ResultReceiver receiver) {
        Result<MediaBrowser.MediaItem> result = new Result<MediaBrowser.MediaItem>(this, itemId) {
            @Override
            void onResultSent(MediaBrowser.MediaItem item, int flag) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(MediaBrowserService.KEY_MEDIA_ITEM, item);
                receiver.send(0, bundle);
            }
        };
        this.mCurConnection = connection;
        onLoadItem(itemId, result);
        this.mCurConnection = null;
        if (result.isDone()) {
        } else {
            throw new IllegalStateException("onLoadItem must call detach() or sendResult() before returning for id=" + itemId);
        }
    }

    public static final class BrowserRoot {
        public static final String EXTRA_OFFLINE = "android.service.media.extra.OFFLINE";
        public static final String EXTRA_RECENT = "android.service.media.extra.RECENT";
        public static final String EXTRA_SUGGESTED = "android.service.media.extra.SUGGESTED";
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
