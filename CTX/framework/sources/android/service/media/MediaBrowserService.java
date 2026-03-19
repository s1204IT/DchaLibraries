package android.service.media;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowserUtils;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.media.IMediaBrowserService;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class MediaBrowserService extends Service {
    private static final boolean DBG = !"user".equals(Build.TYPE);
    public static final String KEY_MEDIA_ITEM = "media_item";
    private static final int RESULT_ERROR = -1;
    private static final int RESULT_FLAG_ON_LOAD_ITEM_NOT_IMPLEMENTED = 2;
    private static final int RESULT_FLAG_OPTION_NOT_HANDLED = 1;
    private static final int RESULT_OK = 0;
    public static final String SERVICE_INTERFACE = "android.media.browse.MediaBrowserService";
    private static final String TAG = "MediaBrowserService";
    private ServiceBinder mBinder;
    private ConnectionRecord mCurConnection;
    MediaSession.Token mSession;
    private final ArrayMap<IBinder, ConnectionRecord> mConnections = new ArrayMap<>();
    private final Handler mHandler = new Handler();

    @Retention(RetentionPolicy.SOURCE)
    private @interface ResultFlags {
    }

    public abstract BrowserRoot onGetRoot(String str, int i, Bundle bundle);

    public abstract void onLoadChildren(String str, Result<List<MediaBrowser.MediaItem>> result);

    private class ConnectionRecord implements IBinder.DeathRecipient {
        IMediaBrowserServiceCallbacks callbacks;
        int pid;
        String pkg;
        BrowserRoot root;
        Bundle rootHints;
        HashMap<String, List<Pair<IBinder, Bundle>>> subscriptions;
        int uid;

        private ConnectionRecord() {
            this.subscriptions = new HashMap<>();
        }

        @Override
        public void binderDied() {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MediaBrowserService.this.mConnections.remove(ConnectionRecord.this.callbacks.asBinder());
                }
            });
        }
    }

    public class Result<T> {
        private Object mDebug;
        private boolean mDetachCalled;
        private int mFlags;
        private boolean mSendResultCalled;

        Result(Object obj) {
            this.mDebug = obj;
        }

        public void sendResult(T t) {
            if (this.mSendResultCalled) {
                throw new IllegalStateException("sendResult() called twice for: " + this.mDebug);
            }
            this.mSendResultCalled = true;
            onResultSent(t, this.mFlags);
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

        void setFlags(int i) {
            this.mFlags = i;
        }

        void onResultSent(T t, int i) {
        }
    }

    private class ServiceBinder extends IMediaBrowserService.Stub {
        private ServiceBinder() {
        }

        @Override
        public void connect(final String str, final Bundle bundle, final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks) {
            final int callingPid = Binder.getCallingPid();
            final int callingUid = Binder.getCallingUid();
            if (MediaBrowserService.this.isValidPackage(str, callingUid)) {
                MediaBrowserService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        IBinder iBinderAsBinder = iMediaBrowserServiceCallbacks.asBinder();
                        MediaBrowserService.this.mConnections.remove(iBinderAsBinder);
                        ConnectionRecord connectionRecord = new ConnectionRecord();
                        connectionRecord.pkg = str;
                        connectionRecord.pid = callingPid;
                        connectionRecord.uid = callingUid;
                        connectionRecord.rootHints = bundle;
                        connectionRecord.callbacks = iMediaBrowserServiceCallbacks;
                        MediaBrowserService.this.mCurConnection = connectionRecord;
                        connectionRecord.root = MediaBrowserService.this.onGetRoot(str, callingUid, bundle);
                        MediaBrowserService.this.mCurConnection = null;
                        if (connectionRecord.root != null) {
                            try {
                                MediaBrowserService.this.mConnections.put(iBinderAsBinder, connectionRecord);
                                iBinderAsBinder.linkToDeath(connectionRecord, 0);
                                if (MediaBrowserService.this.mSession != null) {
                                    iMediaBrowserServiceCallbacks.onConnect(connectionRecord.root.getRootId(), MediaBrowserService.this.mSession, connectionRecord.root.getExtras());
                                    return;
                                }
                                return;
                            } catch (RemoteException e) {
                                Log.w(MediaBrowserService.TAG, "Calling onConnect() failed. Dropping client. pkg=" + str);
                                MediaBrowserService.this.mConnections.remove(iBinderAsBinder);
                                return;
                            }
                        }
                        Log.i(MediaBrowserService.TAG, "No root for client " + str + " from service " + getClass().getName());
                        try {
                            iMediaBrowserServiceCallbacks.onConnectFailed();
                        } catch (RemoteException e2) {
                            Log.w(MediaBrowserService.TAG, "Calling onConnectFailed() failed. Ignoring. pkg=" + str);
                        }
                    }
                });
                return;
            }
            throw new IllegalArgumentException("Package/uid mismatch: uid=" + callingUid + " package=" + str);
        }

        @Override
        public void disconnect(final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserService.this.mConnections.remove(iMediaBrowserServiceCallbacks.asBinder());
                    if (connectionRecord != null) {
                        connectionRecord.callbacks.asBinder().unlinkToDeath(connectionRecord, 0);
                    }
                }
            });
        }

        @Override
        public void addSubscriptionDeprecated(String str, IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks) {
        }

        @Override
        public void addSubscription(final String str, final IBinder iBinder, final Bundle bundle, final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserService.this.mConnections.get(iMediaBrowserServiceCallbacks.asBinder());
                    if (connectionRecord != null) {
                        MediaBrowserService.this.addSubscription(str, connectionRecord, iBinder, bundle);
                        return;
                    }
                    Log.w(MediaBrowserService.TAG, "addSubscription for callback that isn't registered id=" + str);
                }
            });
        }

        @Override
        public void removeSubscriptionDeprecated(String str, IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks) {
        }

        @Override
        public void removeSubscription(final String str, final IBinder iBinder, final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserService.this.mConnections.get(iMediaBrowserServiceCallbacks.asBinder());
                    if (connectionRecord != null) {
                        if (!MediaBrowserService.this.removeSubscription(str, connectionRecord, iBinder)) {
                            Log.w(MediaBrowserService.TAG, "removeSubscription called for " + str + " which is not subscribed");
                            return;
                        }
                        return;
                    }
                    Log.w(MediaBrowserService.TAG, "removeSubscription for callback that isn't registered id=" + str);
                }
            });
        }

        @Override
        public void getMediaItem(final String str, final ResultReceiver resultReceiver, final IMediaBrowserServiceCallbacks iMediaBrowserServiceCallbacks) {
            MediaBrowserService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserService.this.mConnections.get(iMediaBrowserServiceCallbacks.asBinder());
                    if (connectionRecord != null) {
                        MediaBrowserService.this.performLoadItem(str, connectionRecord, resultReceiver);
                        return;
                    }
                    Log.w(MediaBrowserService.TAG, "getMediaItem for callback that isn't registered id=" + str);
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
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    public void onLoadChildren(String str, Result<List<MediaBrowser.MediaItem>> result, Bundle bundle) {
        result.setFlags(1);
        onLoadChildren(str, result);
    }

    public void onLoadItem(String str, Result<MediaBrowser.MediaItem> result) {
        result.setFlags(2);
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
                Iterator it = MediaBrowserService.this.mConnections.values().iterator();
                while (it.hasNext()) {
                    ConnectionRecord connectionRecord = (ConnectionRecord) it.next();
                    try {
                        connectionRecord.callbacks.onConnect(connectionRecord.root.getRootId(), token, connectionRecord.root.getExtras());
                    } catch (RemoteException e) {
                        Log.w(MediaBrowserService.TAG, "Connection for " + connectionRecord.pkg + " is no longer valid.");
                        it.remove();
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
            throw new IllegalStateException("This should be called inside of onGetRoot or onLoadChildren or onLoadItem methods");
        }
        if (this.mCurConnection.rootHints == null) {
            return null;
        }
        return new Bundle(this.mCurConnection.rootHints);
    }

    public final MediaSessionManager.RemoteUserInfo getCurrentBrowserInfo() {
        if (this.mCurConnection == null) {
            throw new IllegalStateException("This should be called inside of onGetRoot or onLoadChildren or onLoadItem methods");
        }
        return new MediaSessionManager.RemoteUserInfo(this.mCurConnection.pkg, this.mCurConnection.pid, this.mCurConnection.uid, this.mCurConnection.callbacks.asBinder());
    }

    public void notifyChildrenChanged(String str) {
        notifyChildrenChangedInternal(str, null);
    }

    public void notifyChildrenChanged(String str, Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("options cannot be null in notifyChildrenChanged");
        }
        notifyChildrenChangedInternal(str, bundle);
    }

    private void notifyChildrenChangedInternal(final String str, final Bundle bundle) {
        if (str == null) {
            throw new IllegalArgumentException("parentId cannot be null in notifyChildrenChanged");
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator it = MediaBrowserService.this.mConnections.keySet().iterator();
                while (it.hasNext()) {
                    ConnectionRecord connectionRecord = (ConnectionRecord) MediaBrowserService.this.mConnections.get((IBinder) it.next());
                    List<Pair<IBinder, Bundle>> list = connectionRecord.subscriptions.get(str);
                    if (list != null) {
                        for (Pair<IBinder, Bundle> pair : list) {
                            if (MediaBrowserUtils.hasDuplicatedItems(bundle, pair.second)) {
                                MediaBrowserService.this.performLoadChildren(str, connectionRecord, pair.second);
                            }
                        }
                    }
                }
            }
        });
    }

    private boolean isValidPackage(String str, int i) {
        if (str == null) {
            return false;
        }
        for (String str2 : getPackageManager().getPackagesForUid(i)) {
            if (str2.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private void addSubscription(String str, ConnectionRecord connectionRecord, IBinder iBinder, Bundle bundle) {
        List<Pair<IBinder, Bundle>> arrayList = connectionRecord.subscriptions.get(str);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        for (Pair<IBinder, Bundle> pair : arrayList) {
            if (iBinder == pair.first && MediaBrowserUtils.areSameOptions(bundle, pair.second)) {
                return;
            }
        }
        arrayList.add(new Pair<>(iBinder, bundle));
        connectionRecord.subscriptions.put(str, arrayList);
        performLoadChildren(str, connectionRecord, bundle);
    }

    private boolean removeSubscription(String str, ConnectionRecord connectionRecord, IBinder iBinder) {
        boolean z = false;
        if (iBinder == null) {
            return connectionRecord.subscriptions.remove(str) != null;
        }
        List<Pair<IBinder, Bundle>> list = connectionRecord.subscriptions.get(str);
        if (list != null) {
            Iterator<Pair<IBinder, Bundle>> it = list.iterator();
            while (it.hasNext()) {
                if (iBinder == it.next().first) {
                    it.remove();
                    z = true;
                }
            }
            if (list.size() == 0) {
                connectionRecord.subscriptions.remove(str);
            }
        }
        return z;
    }

    private void performLoadChildren(final String str, final ConnectionRecord connectionRecord, final Bundle bundle) {
        Result<List<MediaBrowser.MediaItem>> result = new Result<List<MediaBrowser.MediaItem>>(str) {
            @Override
            void onResultSent(List<MediaBrowser.MediaItem> list, int i) {
                if (MediaBrowserService.this.mConnections.get(connectionRecord.callbacks.asBinder()) != connectionRecord) {
                    if (MediaBrowserService.DBG) {
                        Log.d(MediaBrowserService.TAG, "Not sending onLoadChildren result for connection that has been disconnected. pkg=" + connectionRecord.pkg + " id=" + str);
                        return;
                    }
                    return;
                }
                if ((i & 1) != 0) {
                    list = MediaBrowserService.this.applyOptions(list, bundle);
                }
                try {
                    connectionRecord.callbacks.onLoadChildrenWithOptions(str, list == null ? null : new ParceledListSlice(list), bundle);
                } catch (RemoteException e) {
                    Log.w(MediaBrowserService.TAG, "Calling onLoadChildren() failed for id=" + str + " package=" + connectionRecord.pkg);
                }
            }
        };
        this.mCurConnection = connectionRecord;
        if (bundle == null) {
            onLoadChildren(str, result);
        } else {
            onLoadChildren(str, result, bundle);
        }
        this.mCurConnection = null;
        if (!result.isDone()) {
            throw new IllegalStateException("onLoadChildren must call detach() or sendResult() before returning for package=" + connectionRecord.pkg + " id=" + str);
        }
    }

    private List<MediaBrowser.MediaItem> applyOptions(List<MediaBrowser.MediaItem> list, Bundle bundle) {
        if (list == null) {
            return null;
        }
        int i = bundle.getInt(MediaBrowser.EXTRA_PAGE, -1);
        int i2 = bundle.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1);
        if (i == -1 && i2 == -1) {
            return list;
        }
        int i3 = i2 * i;
        int size = i3 + i2;
        if (i < 0 || i2 < 1 || i3 >= list.size()) {
            return Collections.EMPTY_LIST;
        }
        if (size > list.size()) {
            size = list.size();
        }
        return list.subList(i3, size);
    }

    private void performLoadItem(final String str, final ConnectionRecord connectionRecord, final ResultReceiver resultReceiver) {
        Result<MediaBrowser.MediaItem> result = new Result<MediaBrowser.MediaItem>(str) {
            @Override
            void onResultSent(MediaBrowser.MediaItem mediaItem, int i) {
                if (MediaBrowserService.this.mConnections.get(connectionRecord.callbacks.asBinder()) != connectionRecord) {
                    if (MediaBrowserService.DBG) {
                        Log.d(MediaBrowserService.TAG, "Not sending onLoadItem result for connection that has been disconnected. pkg=" + connectionRecord.pkg + " id=" + str);
                        return;
                    }
                    return;
                }
                if ((i & 2) != 0) {
                    resultReceiver.send(-1, null);
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putParcelable(MediaBrowserService.KEY_MEDIA_ITEM, mediaItem);
                resultReceiver.send(0, bundle);
            }
        };
        this.mCurConnection = connectionRecord;
        onLoadItem(str, result);
        this.mCurConnection = null;
        if (!result.isDone()) {
            throw new IllegalStateException("onLoadItem must call detach() or sendResult() before returning for id=" + str);
        }
    }

    public static final class BrowserRoot {
        public static final String EXTRA_OFFLINE = "android.service.media.extra.OFFLINE";
        public static final String EXTRA_RECENT = "android.service.media.extra.RECENT";
        public static final String EXTRA_SUGGESTED = "android.service.media.extra.SUGGESTED";
        private final Bundle mExtras;
        private final String mRootId;

        public BrowserRoot(String str, Bundle bundle) {
            if (str == null) {
                throw new IllegalArgumentException("The root id in BrowserRoot cannot be null. Use null for BrowserRoot instead.");
            }
            this.mRootId = str;
            this.mExtras = bundle;
        }

        public String getRootId() {
            return this.mRootId;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }
    }
}
