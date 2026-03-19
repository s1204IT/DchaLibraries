package android.media.browse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ParceledListSlice;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.media.IMediaBrowserService;
import android.service.media.IMediaBrowserServiceCallbacks;
import android.service.media.MediaBrowserService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MediaBrowser {
    private static final int CONNECT_STATE_CONNECTED = 2;
    private static final int CONNECT_STATE_CONNECTING = 1;
    private static final int CONNECT_STATE_DISCONNECTED = 0;
    private static final int CONNECT_STATE_SUSPENDED = 3;
    private static final boolean DBG = false;
    public static final String EXTRA_PAGE = "android.media.browse.extra.PAGE";
    public static final String EXTRA_PAGE_SIZE = "android.media.browse.extra.PAGE_SIZE";
    private static final String TAG = "MediaBrowser";
    private final ConnectionCallback mCallback;
    private final Context mContext;
    private Bundle mExtras;
    private MediaSession.Token mMediaSessionToken;
    private final Bundle mRootHints;
    private String mRootId;
    private IMediaBrowserService mServiceBinder;
    private IMediaBrowserServiceCallbacks mServiceCallbacks;
    private final ComponentName mServiceComponent;
    private MediaServiceConnection mServiceConnection;
    private final Handler mHandler = new Handler();
    private final ArrayMap<String, Subscription> mSubscriptions = new ArrayMap<>();
    private int mState = 0;

    public MediaBrowser(Context context, ComponentName serviceComponent, ConnectionCallback callback, Bundle rootHints) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (serviceComponent == null) {
            throw new IllegalArgumentException("service component must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("connection callback must not be null");
        }
        this.mContext = context;
        this.mServiceComponent = serviceComponent;
        this.mCallback = callback;
        this.mRootHints = rootHints != null ? new Bundle(rootHints) : null;
    }

    public void connect() {
        MediaServiceConnection mediaServiceConnection = null;
        if (this.mState != 0) {
            throw new IllegalStateException("connect() called while not disconnected (state=" + getStateLabel(this.mState) + ")");
        }
        if (this.mServiceBinder != null) {
            throw new RuntimeException("mServiceBinder should be null. Instead it is " + this.mServiceBinder);
        }
        if (this.mServiceCallbacks != null) {
            throw new RuntimeException("mServiceCallbacks should be null. Instead it is " + this.mServiceCallbacks);
        }
        this.mState = 1;
        Intent intent = new Intent(MediaBrowserService.SERVICE_INTERFACE);
        intent.setComponent(this.mServiceComponent);
        final MediaServiceConnection mediaServiceConnection2 = new MediaServiceConnection(this, mediaServiceConnection);
        this.mServiceConnection = mediaServiceConnection2;
        boolean bound = false;
        try {
            bound = this.mContext.bindService(intent, this.mServiceConnection, 1);
        } catch (Exception e) {
            Log.e(TAG, "Failed binding to service " + this.mServiceComponent);
        }
        if (bound) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaServiceConnection2 != MediaBrowser.this.mServiceConnection) {
                    return;
                }
                MediaBrowser.this.forceCloseConnection();
                MediaBrowser.this.mCallback.onConnectionFailed();
            }
        });
    }

    public void disconnect() {
        if (this.mServiceCallbacks != null) {
            try {
                this.mServiceBinder.disconnect(this.mServiceCallbacks);
            } catch (RemoteException e) {
                Log.w(TAG, "RemoteException during connect for " + this.mServiceComponent);
            }
        }
        forceCloseConnection();
    }

    private void forceCloseConnection() {
        if (this.mServiceConnection != null) {
            this.mContext.unbindService(this.mServiceConnection);
        }
        this.mState = 0;
        this.mServiceConnection = null;
        this.mServiceBinder = null;
        this.mServiceCallbacks = null;
        this.mRootId = null;
        this.mMediaSessionToken = null;
    }

    public boolean isConnected() {
        return this.mState == 2;
    }

    public ComponentName getServiceComponent() {
        if (!isConnected()) {
            throw new IllegalStateException("getServiceComponent() called while not connected (state=" + this.mState + ")");
        }
        return this.mServiceComponent;
    }

    public String getRoot() {
        if (!isConnected()) {
            throw new IllegalStateException("getRoot() called while not connected (state=" + getStateLabel(this.mState) + ")");
        }
        return this.mRootId;
    }

    public Bundle getExtras() {
        if (!isConnected()) {
            throw new IllegalStateException("getExtras() called while not connected (state=" + getStateLabel(this.mState) + ")");
        }
        return this.mExtras;
    }

    public MediaSession.Token getSessionToken() {
        if (!isConnected()) {
            throw new IllegalStateException("getSessionToken() called while not connected (state=" + this.mState + ")");
        }
        return this.mMediaSessionToken;
    }

    public void subscribe(String parentId, SubscriptionCallback callback) {
        subscribeInternal(parentId, null, callback);
    }

    public void subscribe(String parentId, Bundle options, SubscriptionCallback callback) {
        if (options == null) {
            throw new IllegalArgumentException("options are null");
        }
        subscribeInternal(parentId, new Bundle(options), callback);
    }

    public void unsubscribe(String parentId) {
        unsubscribeInternal(parentId, null);
    }

    public void unsubscribe(String parentId, SubscriptionCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }
        unsubscribeInternal(parentId, callback);
    }

    public void getItem(final String mediaId, final ItemCallback cb) {
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId is empty.");
        }
        if (cb == null) {
            throw new IllegalArgumentException("cb is null.");
        }
        if (this.mState != 2) {
            Log.i(TAG, "Not connected, unable to retrieve the MediaItem.");
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onError(mediaId);
                }
            });
            return;
        }
        ResultReceiver receiver = new ResultReceiver(this.mHandler) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode != 0 || resultData == null || !resultData.containsKey(MediaBrowserService.KEY_MEDIA_ITEM)) {
                    cb.onError(mediaId);
                    return;
                }
                ?? parcelable = resultData.getParcelable(MediaBrowserService.KEY_MEDIA_ITEM);
                if (!(parcelable instanceof MediaItem)) {
                    cb.onError(mediaId);
                } else {
                    cb.onItemLoaded(parcelable);
                }
            }
        };
        try {
            this.mServiceBinder.getMediaItem(mediaId, receiver, this.mServiceCallbacks);
        } catch (RemoteException e) {
            Log.i(TAG, "Remote error getting media item.");
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onError(mediaId);
                }
            });
        }
    }

    private void subscribeInternal(String parentId, Bundle options, SubscriptionCallback callback) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("parentId is empty.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }
        Subscription sub = this.mSubscriptions.get(parentId);
        if (sub == null) {
            sub = new Subscription();
            this.mSubscriptions.put(parentId, sub);
        }
        sub.putCallback(options, callback);
        if (this.mState != 2) {
            return;
        }
        if (options == null) {
            try {
                this.mServiceBinder.addSubscriptionDeprecated(parentId, this.mServiceCallbacks);
            } catch (RemoteException e) {
                Log.d(TAG, "addSubscription failed with RemoteException parentId=" + parentId);
                return;
            }
        }
        this.mServiceBinder.addSubscription(parentId, callback.mToken, options, this.mServiceCallbacks);
    }

    private void unsubscribeInternal(String parentId, SubscriptionCallback callback) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("parentId is empty.");
        }
        Subscription sub = this.mSubscriptions.get(parentId);
        if (sub == null) {
            return;
        }
        try {
            if (callback == null) {
                if (this.mState == 2) {
                    this.mServiceBinder.removeSubscriptionDeprecated(parentId, this.mServiceCallbacks);
                    this.mServiceBinder.removeSubscription(parentId, null, this.mServiceCallbacks);
                }
            } else {
                List<SubscriptionCallback> callbacks = sub.getCallbacks();
                List<Bundle> optionsList = sub.getOptionsList();
                for (int i = callbacks.size() - 1; i >= 0; i--) {
                    if (callbacks.get(i) == callback) {
                        if (this.mState == 2) {
                            this.mServiceBinder.removeSubscription(parentId, callback.mToken, this.mServiceCallbacks);
                        }
                        callbacks.remove(i);
                        optionsList.remove(i);
                    }
                }
            }
        } catch (RemoteException e) {
            Log.d(TAG, "removeSubscription failed with RemoteException parentId=" + parentId);
        }
        if (!sub.isEmpty() && callback != null) {
            return;
        }
        this.mSubscriptions.remove(parentId);
    }

    private static String getStateLabel(int state) {
        switch (state) {
            case 0:
                return "CONNECT_STATE_DISCONNECTED";
            case 1:
                return "CONNECT_STATE_CONNECTING";
            case 2:
                return "CONNECT_STATE_CONNECTED";
            case 3:
                return "CONNECT_STATE_SUSPENDED";
            default:
                return "UNKNOWN/" + state;
        }
    }

    private final void onServiceConnected(final IMediaBrowserServiceCallbacks callback, final String root, final MediaSession.Token session, final Bundle extra) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!MediaBrowser.this.isCurrent(callback, "onConnect")) {
                    return;
                }
                if (MediaBrowser.this.mState != 1) {
                    Log.w(MediaBrowser.TAG, "onConnect from service while mState=" + MediaBrowser.getStateLabel(MediaBrowser.this.mState) + "... ignoring");
                    return;
                }
                MediaBrowser.this.mRootId = root;
                MediaBrowser.this.mMediaSessionToken = session;
                MediaBrowser.this.mExtras = extra;
                MediaBrowser.this.mState = 2;
                MediaBrowser.this.mCallback.onConnected();
                for (Map.Entry<String, Subscription> subscriptionEntry : MediaBrowser.this.mSubscriptions.entrySet()) {
                    String id = subscriptionEntry.getKey();
                    Subscription sub = subscriptionEntry.getValue();
                    List<SubscriptionCallback> callbackList = sub.getCallbacks();
                    List<Bundle> optionsList = sub.getOptionsList();
                    for (int i = 0; i < callbackList.size(); i++) {
                        try {
                            MediaBrowser.this.mServiceBinder.addSubscription(id, callbackList.get(i).mToken, optionsList.get(i), MediaBrowser.this.mServiceCallbacks);
                        } catch (RemoteException e) {
                            Log.d(MediaBrowser.TAG, "addSubscription failed with RemoteException parentId=" + id);
                        }
                    }
                }
            }
        });
    }

    private final void onConnectionFailed(final IMediaBrowserServiceCallbacks callback) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.e(MediaBrowser.TAG, "onConnectFailed for " + MediaBrowser.this.mServiceComponent);
                if (!MediaBrowser.this.isCurrent(callback, "onConnectFailed")) {
                    return;
                }
                if (MediaBrowser.this.mState != 1) {
                    Log.w(MediaBrowser.TAG, "onConnect from service while mState=" + MediaBrowser.getStateLabel(MediaBrowser.this.mState) + "... ignoring");
                } else {
                    MediaBrowser.this.forceCloseConnection();
                    MediaBrowser.this.mCallback.onConnectionFailed();
                }
            }
        });
    }

    private final void onLoadChildren(final IMediaBrowserServiceCallbacks callback, final String parentId, final ParceledListSlice list, final Bundle options) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Subscription subscription;
                SubscriptionCallback subscriptionCallback;
                if (!MediaBrowser.this.isCurrent(callback, "onLoadChildren") || (subscription = (Subscription) MediaBrowser.this.mSubscriptions.get(parentId)) == null || (subscriptionCallback = subscription.getCallback(options)) == null) {
                    return;
                }
                List<MediaItem> list2 = list == null ? null : list.getList();
                if (options == null) {
                    if (list2 == null) {
                        subscriptionCallback.onError(parentId);
                        return;
                    } else {
                        subscriptionCallback.onChildrenLoaded(parentId, list2);
                        return;
                    }
                }
                if (list2 == null) {
                    subscriptionCallback.onError(parentId, options);
                } else {
                    subscriptionCallback.onChildrenLoaded(parentId, list2, options);
                }
            }
        });
    }

    private boolean isCurrent(IMediaBrowserServiceCallbacks callback, String funcName) {
        if (this.mServiceCallbacks != callback) {
            if (this.mState != 0) {
                Log.i(TAG, funcName + " for " + this.mServiceComponent + " with mServiceConnection=" + this.mServiceCallbacks + " this=" + this);
            }
            return false;
        }
        return true;
    }

    private ServiceCallbacks getNewServiceCallbacks() {
        return new ServiceCallbacks(this);
    }

    void dump() {
        Log.d(TAG, "MediaBrowser...");
        Log.d(TAG, "  mServiceComponent=" + this.mServiceComponent);
        Log.d(TAG, "  mCallback=" + this.mCallback);
        Log.d(TAG, "  mRootHints=" + this.mRootHints);
        Log.d(TAG, "  mState=" + getStateLabel(this.mState));
        Log.d(TAG, "  mServiceConnection=" + this.mServiceConnection);
        Log.d(TAG, "  mServiceBinder=" + this.mServiceBinder);
        Log.d(TAG, "  mServiceCallbacks=" + this.mServiceCallbacks);
        Log.d(TAG, "  mRootId=" + this.mRootId);
        Log.d(TAG, "  mMediaSessionToken=" + this.mMediaSessionToken);
    }

    public static class MediaItem implements Parcelable {
        public static final Parcelable.Creator<MediaItem> CREATOR = new Parcelable.Creator<MediaItem>() {
            @Override
            public MediaItem createFromParcel(Parcel in) {
                return new MediaItem(in, (MediaItem) null);
            }

            @Override
            public MediaItem[] newArray(int size) {
                return new MediaItem[size];
            }
        };
        public static final int FLAG_BROWSABLE = 1;
        public static final int FLAG_PLAYABLE = 2;
        private final MediaDescription mDescription;
        private final int mFlags;

        MediaItem(Parcel in, MediaItem mediaItem) {
            this(in);
        }

        public MediaItem(MediaDescription description, int flags) {
            if (description == null) {
                throw new IllegalArgumentException("description cannot be null");
            }
            if (TextUtils.isEmpty(description.getMediaId())) {
                throw new IllegalArgumentException("description must have a non-empty media id");
            }
            this.mFlags = flags;
            this.mDescription = description;
        }

        private MediaItem(Parcel in) {
            this.mFlags = in.readInt();
            this.mDescription = MediaDescription.CREATOR.createFromParcel(in);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(this.mFlags);
            this.mDescription.writeToParcel(out, flags);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("MediaItem{");
            sb.append("mFlags=").append(this.mFlags);
            sb.append(", mDescription=").append(this.mDescription);
            sb.append('}');
            return sb.toString();
        }

        public int getFlags() {
            return this.mFlags;
        }

        public boolean isBrowsable() {
            return (this.mFlags & 1) != 0;
        }

        public boolean isPlayable() {
            return (this.mFlags & 2) != 0;
        }

        public MediaDescription getDescription() {
            return this.mDescription;
        }

        public String getMediaId() {
            return this.mDescription.getMediaId();
        }
    }

    public static class ConnectionCallback {
        public void onConnected() {
        }

        public void onConnectionSuspended() {
        }

        public void onConnectionFailed() {
        }
    }

    public static abstract class SubscriptionCallback {
        Binder mToken = new Binder();

        public void onChildrenLoaded(String parentId, List<MediaItem> children) {
        }

        public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle options) {
        }

        public void onError(String parentId) {
        }

        public void onError(String parentId, Bundle options) {
        }
    }

    public static abstract class ItemCallback {
        public void onItemLoaded(MediaItem item) {
        }

        public void onError(String itemId) {
        }
    }

    private class MediaServiceConnection implements ServiceConnection {
        MediaServiceConnection(MediaBrowser this$0, MediaServiceConnection mediaServiceConnection) {
            this();
        }

        private MediaServiceConnection() {
        }

        @Override
        public void onServiceConnected(final ComponentName name, final IBinder binder) {
            postOrRun(new Runnable() {
                @Override
                public void run() {
                    if (!MediaServiceConnection.this.isCurrent("onServiceConnected")) {
                        return;
                    }
                    MediaBrowser.this.mServiceBinder = IMediaBrowserService.Stub.asInterface(binder);
                    MediaBrowser.this.mServiceCallbacks = MediaBrowser.this.getNewServiceCallbacks();
                    MediaBrowser.this.mState = 1;
                    try {
                        MediaBrowser.this.mServiceBinder.connect(MediaBrowser.this.mContext.getPackageName(), MediaBrowser.this.mRootHints, MediaBrowser.this.mServiceCallbacks);
                    } catch (RemoteException e) {
                        Log.w(MediaBrowser.TAG, "RemoteException during connect for " + MediaBrowser.this.mServiceComponent);
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            postOrRun(new Runnable() {
                @Override
                public void run() {
                    if (!MediaServiceConnection.this.isCurrent("onServiceDisconnected")) {
                        return;
                    }
                    MediaBrowser.this.mServiceBinder = null;
                    MediaBrowser.this.mServiceCallbacks = null;
                    MediaBrowser.this.mState = 3;
                    MediaBrowser.this.mCallback.onConnectionSuspended();
                }
            });
        }

        private void postOrRun(Runnable r) {
            if (Thread.currentThread() == MediaBrowser.this.mHandler.getLooper().getThread()) {
                r.run();
            } else {
                MediaBrowser.this.mHandler.post(r);
            }
        }

        private boolean isCurrent(String funcName) {
            if (MediaBrowser.this.mServiceConnection != this) {
                if (MediaBrowser.this.mState != 0) {
                    Log.i(MediaBrowser.TAG, funcName + " for " + MediaBrowser.this.mServiceComponent + " with mServiceConnection=" + MediaBrowser.this.mServiceConnection + " this=" + this);
                }
                return false;
            }
            return true;
        }
    }

    private static class ServiceCallbacks extends IMediaBrowserServiceCallbacks.Stub {
        private WeakReference<MediaBrowser> mMediaBrowser;

        public ServiceCallbacks(MediaBrowser mediaBrowser) {
            this.mMediaBrowser = new WeakReference<>(mediaBrowser);
        }

        @Override
        public void onConnect(String root, MediaSession.Token session, Bundle extras) {
            MediaBrowser mediaBrowser = this.mMediaBrowser.get();
            if (mediaBrowser == null) {
                return;
            }
            mediaBrowser.onServiceConnected(this, root, session, extras);
        }

        @Override
        public void onConnectFailed() {
            MediaBrowser mediaBrowser = this.mMediaBrowser.get();
            if (mediaBrowser == null) {
                return;
            }
            mediaBrowser.onConnectionFailed(this);
        }

        @Override
        public void onLoadChildren(String parentId, ParceledListSlice list) {
            onLoadChildrenWithOptions(parentId, list, null);
        }

        @Override
        public void onLoadChildrenWithOptions(String parentId, ParceledListSlice list, Bundle options) {
            MediaBrowser mediaBrowser = this.mMediaBrowser.get();
            if (mediaBrowser == null) {
                return;
            }
            mediaBrowser.onLoadChildren(this, parentId, list, options);
        }
    }

    private static class Subscription {
        private final List<SubscriptionCallback> mCallbacks = new ArrayList();
        private final List<Bundle> mOptionsList = new ArrayList();

        public boolean isEmpty() {
            return this.mCallbacks.isEmpty();
        }

        public List<Bundle> getOptionsList() {
            return this.mOptionsList;
        }

        public List<SubscriptionCallback> getCallbacks() {
            return this.mCallbacks;
        }

        public SubscriptionCallback getCallback(Bundle options) {
            for (int i = 0; i < this.mOptionsList.size(); i++) {
                if (MediaBrowserUtils.areSameOptions(this.mOptionsList.get(i), options)) {
                    return this.mCallbacks.get(i);
                }
            }
            return null;
        }

        public void putCallback(Bundle options, SubscriptionCallback callback) {
            for (int i = 0; i < this.mOptionsList.size(); i++) {
                if (MediaBrowserUtils.areSameOptions(this.mOptionsList.get(i), options)) {
                    this.mCallbacks.set(i, callback);
                    return;
                }
            }
            this.mCallbacks.add(callback);
            this.mOptionsList.add(options);
        }
    }
}
