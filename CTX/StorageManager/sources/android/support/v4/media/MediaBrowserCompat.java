package android.support.v4.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaBrowserCompatApi21;
import android.support.v4.media.MediaBrowserCompatApi26;
import android.support.v4.media.session.IMediaSession;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MediaBrowserCompat {
    static final boolean DEBUG = Log.isLoggable("MediaBrowserCompat", 3);
    private final MediaBrowserImpl mImpl;

    interface MediaBrowserImpl {
        void connect();

        void disconnect();

        MediaSessionCompat.Token getSessionToken();
    }

    interface MediaBrowserServiceCallbackImpl {
        void onConnectionFailed(Messenger messenger);

        void onLoadChildren(Messenger messenger, String str, List list, Bundle bundle, Bundle bundle2);

        void onServiceConnected(Messenger messenger, String str, MediaSessionCompat.Token token, Bundle bundle);
    }

    public MediaBrowserCompat(Context context, ComponentName serviceComponent, ConnectionCallback callback, Bundle rootHints) {
        if (Build.VERSION.SDK_INT >= 26) {
            this.mImpl = new MediaBrowserImplApi26(context, serviceComponent, callback, rootHints);
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            this.mImpl = new MediaBrowserImplApi23(context, serviceComponent, callback, rootHints);
        } else if (Build.VERSION.SDK_INT >= 21) {
            this.mImpl = new MediaBrowserImplApi21(context, serviceComponent, callback, rootHints);
        } else {
            this.mImpl = new MediaBrowserImplBase(context, serviceComponent, callback, rootHints);
        }
    }

    public void connect() {
        this.mImpl.connect();
    }

    public void disconnect() {
        this.mImpl.disconnect();
    }

    public MediaSessionCompat.Token getSessionToken() {
        return this.mImpl.getSessionToken();
    }

    public static class MediaItem implements Parcelable {
        public static final Parcelable.Creator<MediaItem> CREATOR = new Parcelable.Creator<MediaItem>() {
            @Override
            public MediaItem createFromParcel(Parcel in) {
                return new MediaItem(in);
            }

            @Override
            public MediaItem[] newArray(int size) {
                return new MediaItem[size];
            }
        };
        private final MediaDescriptionCompat mDescription;
        private final int mFlags;

        public static MediaItem fromMediaItem(Object itemObj) {
            if (itemObj == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            int flags = MediaBrowserCompatApi21.MediaItem.getFlags(itemObj);
            MediaDescriptionCompat description = MediaDescriptionCompat.fromMediaDescription(MediaBrowserCompatApi21.MediaItem.getDescription(itemObj));
            return new MediaItem(description, flags);
        }

        public static List<MediaItem> fromMediaItemList(List<?> itemList) {
            if (itemList == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            List<MediaItem> items = new ArrayList<>(itemList.size());
            for (Object itemObj : itemList) {
                items.add(fromMediaItem(itemObj));
            }
            return items;
        }

        public MediaItem(MediaDescriptionCompat description, int flags) {
            if (description == null) {
                throw new IllegalArgumentException("description cannot be null");
            }
            if (TextUtils.isEmpty(description.getMediaId())) {
                throw new IllegalArgumentException("description must have a non-empty media id");
            }
            this.mFlags = flags;
            this.mDescription = description;
        }

        MediaItem(Parcel in) {
            this.mFlags = in.readInt();
            this.mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(in);
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
            return "MediaItem{mFlags=" + this.mFlags + ", mDescription=" + this.mDescription + '}';
        }
    }

    public static class ConnectionCallback {
        ConnectionCallbackInternal mConnectionCallbackInternal;
        final Object mConnectionCallbackObj;

        interface ConnectionCallbackInternal {
            void onConnected();

            void onConnectionFailed();

            void onConnectionSuspended();
        }

        public ConnectionCallback() {
            if (Build.VERSION.SDK_INT >= 21) {
                this.mConnectionCallbackObj = MediaBrowserCompatApi21.createConnectionCallback(new StubApi21());
            } else {
                this.mConnectionCallbackObj = null;
            }
        }

        public void onConnected() {
        }

        public void onConnectionSuspended() {
        }

        public void onConnectionFailed() {
        }

        void setInternalConnectionCallback(ConnectionCallbackInternal connectionCallbackInternal) {
            this.mConnectionCallbackInternal = connectionCallbackInternal;
        }

        private class StubApi21 implements MediaBrowserCompatApi21.ConnectionCallback {
            StubApi21() {
            }

            @Override
            public void onConnected() {
                if (ConnectionCallback.this.mConnectionCallbackInternal != null) {
                    ConnectionCallback.this.mConnectionCallbackInternal.onConnected();
                }
                ConnectionCallback.this.onConnected();
            }

            @Override
            public void onConnectionSuspended() {
                if (ConnectionCallback.this.mConnectionCallbackInternal != null) {
                    ConnectionCallback.this.mConnectionCallbackInternal.onConnectionSuspended();
                }
                ConnectionCallback.this.onConnectionSuspended();
            }

            @Override
            public void onConnectionFailed() {
                if (ConnectionCallback.this.mConnectionCallbackInternal != null) {
                    ConnectionCallback.this.mConnectionCallbackInternal.onConnectionFailed();
                }
                ConnectionCallback.this.onConnectionFailed();
            }
        }
    }

    public static abstract class SubscriptionCallback {
        private final Object mSubscriptionCallbackObj;
        WeakReference<Subscription> mSubscriptionRef;
        private final IBinder mToken = new Binder();

        public SubscriptionCallback() {
            if (Build.VERSION.SDK_INT >= 26) {
                this.mSubscriptionCallbackObj = MediaBrowserCompatApi26.createSubscriptionCallback(new StubApi26());
            } else if (Build.VERSION.SDK_INT >= 21) {
                this.mSubscriptionCallbackObj = MediaBrowserCompatApi21.createSubscriptionCallback(new StubApi21());
            } else {
                this.mSubscriptionCallbackObj = null;
            }
        }

        public void onChildrenLoaded(String parentId, List<MediaItem> children) {
        }

        public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle options) {
        }

        public void onError(String parentId) {
        }

        public void onError(String parentId, Bundle options) {
        }

        private class StubApi21 implements MediaBrowserCompatApi21.SubscriptionCallback {
            StubApi21() {
            }

            @Override
            public void onChildrenLoaded(String parentId, List<?> children) {
                Subscription sub = SubscriptionCallback.this.mSubscriptionRef == null ? null : SubscriptionCallback.this.mSubscriptionRef.get();
                if (sub == null) {
                    SubscriptionCallback.this.onChildrenLoaded(parentId, MediaItem.fromMediaItemList(children));
                    return;
                }
                List<MediaItem> itemList = MediaItem.fromMediaItemList(children);
                List<SubscriptionCallback> callbacks = sub.getCallbacks();
                List<Bundle> optionsList = sub.getOptionsList();
                for (int i = 0; i < callbacks.size(); i++) {
                    Bundle options = optionsList.get(i);
                    if (options == null) {
                        SubscriptionCallback.this.onChildrenLoaded(parentId, itemList);
                    } else {
                        SubscriptionCallback.this.onChildrenLoaded(parentId, applyOptions(itemList, options), options);
                    }
                }
            }

            @Override
            public void onError(String parentId) {
                SubscriptionCallback.this.onError(parentId);
            }

            List<MediaItem> applyOptions(List<MediaItem> list, Bundle options) {
                if (list == null) {
                    return null;
                }
                int page = options.getInt("android.media.browse.extra.PAGE", -1);
                int pageSize = options.getInt("android.media.browse.extra.PAGE_SIZE", -1);
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
        }

        private class StubApi26 extends StubApi21 implements MediaBrowserCompatApi26.SubscriptionCallback {
            StubApi26() {
                super();
            }

            @Override
            public void onChildrenLoaded(String parentId, List<?> children, Bundle options) {
                SubscriptionCallback.this.onChildrenLoaded(parentId, MediaItem.fromMediaItemList(children), options);
            }

            @Override
            public void onError(String parentId, Bundle options) {
                SubscriptionCallback.this.onError(parentId, options);
            }
        }
    }

    public static abstract class ItemCallback {
        public void onItemLoaded(MediaItem item) {
        }

        public void onError(String itemId) {
        }
    }

    public static abstract class SearchCallback {
        public void onSearchResult(String query, Bundle extras, List<MediaItem> items) {
        }

        public void onError(String query, Bundle extras) {
        }
    }

    public static abstract class CustomActionCallback {
        public void onProgressUpdate(String action, Bundle extras, Bundle data) {
        }

        public void onResult(String action, Bundle extras, Bundle resultData) {
        }

        public void onError(String action, Bundle extras, Bundle data) {
        }
    }

    static class MediaBrowserImplBase implements MediaBrowserImpl, MediaBrowserServiceCallbackImpl {
        final ConnectionCallback mCallback;
        Messenger mCallbacksMessenger;
        final Context mContext;
        private Bundle mExtras;
        private MediaSessionCompat.Token mMediaSessionToken;
        private Bundle mNotifyChildrenChangedOptions;
        final Bundle mRootHints;
        private String mRootId;
        ServiceBinderWrapper mServiceBinderWrapper;
        final ComponentName mServiceComponent;
        MediaServiceConnection mServiceConnection;
        final CallbackHandler mHandler = new CallbackHandler(this);
        private final ArrayMap<String, Subscription> mSubscriptions = new ArrayMap<>();
        int mState = 1;

        public MediaBrowserImplBase(Context context, ComponentName serviceComponent, ConnectionCallback callback, Bundle rootHints) {
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
            this.mRootHints = rootHints == null ? null : new Bundle(rootHints);
        }

        @Override
        public void connect() {
            if (this.mState != 0 && this.mState != 1) {
                throw new IllegalStateException("connect() called while neigther disconnecting nor disconnected (state=" + getStateLabel(this.mState) + ")");
            }
            this.mState = 2;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (MediaBrowserImplBase.this.mState == 0) {
                        return;
                    }
                    MediaBrowserImplBase.this.mState = 2;
                    if (MediaBrowserCompat.DEBUG && MediaBrowserImplBase.this.mServiceConnection != null) {
                        throw new RuntimeException("mServiceConnection should be null. Instead it is " + MediaBrowserImplBase.this.mServiceConnection);
                    }
                    if (MediaBrowserImplBase.this.mServiceBinderWrapper != null) {
                        throw new RuntimeException("mServiceBinderWrapper should be null. Instead it is " + MediaBrowserImplBase.this.mServiceBinderWrapper);
                    }
                    if (MediaBrowserImplBase.this.mCallbacksMessenger != null) {
                        throw new RuntimeException("mCallbacksMessenger should be null. Instead it is " + MediaBrowserImplBase.this.mCallbacksMessenger);
                    }
                    Intent intent = new Intent("android.media.browse.MediaBrowserService");
                    intent.setComponent(MediaBrowserImplBase.this.mServiceComponent);
                    MediaBrowserImplBase.this.mServiceConnection = MediaBrowserImplBase.this.new MediaServiceConnection();
                    boolean bound = false;
                    try {
                        bound = MediaBrowserImplBase.this.mContext.bindService(intent, MediaBrowserImplBase.this.mServiceConnection, 1);
                    } catch (Exception e) {
                        Log.e("MediaBrowserCompat", "Failed binding to service " + MediaBrowserImplBase.this.mServiceComponent);
                    }
                    if (!bound) {
                        MediaBrowserImplBase.this.forceCloseConnection();
                        MediaBrowserImplBase.this.mCallback.onConnectionFailed();
                    }
                    if (MediaBrowserCompat.DEBUG) {
                        Log.d("MediaBrowserCompat", "connect...");
                        MediaBrowserImplBase.this.dump();
                    }
                }
            });
        }

        @Override
        public void disconnect() {
            this.mState = 0;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (MediaBrowserImplBase.this.mCallbacksMessenger != null) {
                        try {
                            MediaBrowserImplBase.this.mServiceBinderWrapper.disconnect(MediaBrowserImplBase.this.mCallbacksMessenger);
                        } catch (RemoteException e) {
                            Log.w("MediaBrowserCompat", "RemoteException during connect for " + MediaBrowserImplBase.this.mServiceComponent);
                        }
                    }
                    int state = MediaBrowserImplBase.this.mState;
                    MediaBrowserImplBase.this.forceCloseConnection();
                    if (state != 0) {
                        MediaBrowserImplBase.this.mState = state;
                    }
                    if (MediaBrowserCompat.DEBUG) {
                        Log.d("MediaBrowserCompat", "disconnect...");
                        MediaBrowserImplBase.this.dump();
                    }
                }
            });
        }

        void forceCloseConnection() {
            if (this.mServiceConnection != null) {
                this.mContext.unbindService(this.mServiceConnection);
            }
            this.mState = 1;
            this.mServiceConnection = null;
            this.mServiceBinderWrapper = null;
            this.mCallbacksMessenger = null;
            this.mHandler.setCallbacksMessenger(null);
            this.mRootId = null;
            this.mMediaSessionToken = null;
        }

        public boolean isConnected() {
            return this.mState == 3;
        }

        @Override
        public MediaSessionCompat.Token getSessionToken() {
            if (!isConnected()) {
                throw new IllegalStateException("getSessionToken() called while not connected(state=" + this.mState + ")");
            }
            return this.mMediaSessionToken;
        }

        @Override
        public void onServiceConnected(Messenger callback, String root, MediaSessionCompat.Token session, Bundle extra) {
            if (!isCurrent(callback, "onConnect")) {
                return;
            }
            if (this.mState != 2) {
                Log.w("MediaBrowserCompat", "onConnect from service while mState=" + getStateLabel(this.mState) + "... ignoring");
                return;
            }
            this.mRootId = root;
            this.mMediaSessionToken = session;
            this.mExtras = extra;
            this.mState = 3;
            if (MediaBrowserCompat.DEBUG) {
                Log.d("MediaBrowserCompat", "ServiceCallbacks.onConnect...");
                dump();
            }
            this.mCallback.onConnected();
            try {
                for (Map.Entry<String, Subscription> subscriptionEntry : this.mSubscriptions.entrySet()) {
                    String id = subscriptionEntry.getKey();
                    Subscription sub = subscriptionEntry.getValue();
                    List<SubscriptionCallback> callbackList = sub.getCallbacks();
                    List<Bundle> optionsList = sub.getOptionsList();
                    for (int i = 0; i < callbackList.size(); i++) {
                        this.mServiceBinderWrapper.addSubscription(id, callbackList.get(i).mToken, optionsList.get(i), this.mCallbacksMessenger);
                    }
                }
            } catch (RemoteException e) {
                Log.d("MediaBrowserCompat", "addSubscription failed with RemoteException.");
            }
        }

        @Override
        public void onConnectionFailed(Messenger callback) {
            Log.e("MediaBrowserCompat", "onConnectFailed for " + this.mServiceComponent);
            if (!isCurrent(callback, "onConnectFailed")) {
                return;
            }
            if (this.mState != 2) {
                Log.w("MediaBrowserCompat", "onConnect from service while mState=" + getStateLabel(this.mState) + "... ignoring");
                return;
            }
            forceCloseConnection();
            this.mCallback.onConnectionFailed();
        }

        @Override
        public void onLoadChildren(Messenger callback, String parentId, List list, Bundle options, Bundle notifyChildrenChangedOptions) {
            if (!isCurrent(callback, "onLoadChildren")) {
                return;
            }
            if (MediaBrowserCompat.DEBUG) {
                Log.d("MediaBrowserCompat", "onLoadChildren for " + this.mServiceComponent + " id=" + parentId);
            }
            Subscription subscription = this.mSubscriptions.get(parentId);
            if (subscription == null) {
                if (MediaBrowserCompat.DEBUG) {
                    Log.d("MediaBrowserCompat", "onLoadChildren for id that isn't subscribed id=" + parentId);
                    return;
                }
                return;
            }
            SubscriptionCallback subscriptionCallback = subscription.getCallback(this.mContext, options);
            if (subscriptionCallback != null) {
                if (options == null) {
                    if (list == null) {
                        subscriptionCallback.onError(parentId);
                        return;
                    }
                    this.mNotifyChildrenChangedOptions = notifyChildrenChangedOptions;
                    subscriptionCallback.onChildrenLoaded(parentId, list);
                    this.mNotifyChildrenChangedOptions = null;
                    return;
                }
                if (list == null) {
                    subscriptionCallback.onError(parentId, options);
                    return;
                }
                this.mNotifyChildrenChangedOptions = notifyChildrenChangedOptions;
                subscriptionCallback.onChildrenLoaded(parentId, list, options);
                this.mNotifyChildrenChangedOptions = null;
            }
        }

        private static String getStateLabel(int state) {
            switch (state) {
                case 0:
                    return "CONNECT_STATE_DISCONNECTING";
                case 1:
                    return "CONNECT_STATE_DISCONNECTED";
                case 2:
                    return "CONNECT_STATE_CONNECTING";
                case 3:
                    return "CONNECT_STATE_CONNECTED";
                case 4:
                    return "CONNECT_STATE_SUSPENDED";
                default:
                    return "UNKNOWN/" + state;
            }
        }

        private boolean isCurrent(Messenger callback, String funcName) {
            if (this.mCallbacksMessenger == callback && this.mState != 0 && this.mState != 1) {
                return true;
            }
            if (this.mState != 0 && this.mState != 1) {
                Log.i("MediaBrowserCompat", funcName + " for " + this.mServiceComponent + " with mCallbacksMessenger=" + this.mCallbacksMessenger + " this=" + this);
                return false;
            }
            return false;
        }

        void dump() {
            Log.d("MediaBrowserCompat", "MediaBrowserCompat...");
            Log.d("MediaBrowserCompat", "  mServiceComponent=" + this.mServiceComponent);
            Log.d("MediaBrowserCompat", "  mCallback=" + this.mCallback);
            Log.d("MediaBrowserCompat", "  mRootHints=" + this.mRootHints);
            Log.d("MediaBrowserCompat", "  mState=" + getStateLabel(this.mState));
            Log.d("MediaBrowserCompat", "  mServiceConnection=" + this.mServiceConnection);
            Log.d("MediaBrowserCompat", "  mServiceBinderWrapper=" + this.mServiceBinderWrapper);
            Log.d("MediaBrowserCompat", "  mCallbacksMessenger=" + this.mCallbacksMessenger);
            Log.d("MediaBrowserCompat", "  mRootId=" + this.mRootId);
            Log.d("MediaBrowserCompat", "  mMediaSessionToken=" + this.mMediaSessionToken);
        }

        private class MediaServiceConnection implements ServiceConnection {
            MediaServiceConnection() {
            }

            @Override
            public void onServiceConnected(final ComponentName name, final IBinder binder) {
                postOrRun(new Runnable() {
                    @Override
                    public void run() {
                        if (MediaBrowserCompat.DEBUG) {
                            Log.d("MediaBrowserCompat", "MediaServiceConnection.onServiceConnected name=" + name + " binder=" + binder);
                            MediaBrowserImplBase.this.dump();
                        }
                        if (!MediaServiceConnection.this.isCurrent("onServiceConnected")) {
                            return;
                        }
                        MediaBrowserImplBase.this.mServiceBinderWrapper = new ServiceBinderWrapper(binder, MediaBrowserImplBase.this.mRootHints);
                        MediaBrowserImplBase.this.mCallbacksMessenger = new Messenger(MediaBrowserImplBase.this.mHandler);
                        MediaBrowserImplBase.this.mHandler.setCallbacksMessenger(MediaBrowserImplBase.this.mCallbacksMessenger);
                        MediaBrowserImplBase.this.mState = 2;
                        try {
                            if (MediaBrowserCompat.DEBUG) {
                                Log.d("MediaBrowserCompat", "ServiceCallbacks.onConnect...");
                                MediaBrowserImplBase.this.dump();
                            }
                            MediaBrowserImplBase.this.mServiceBinderWrapper.connect(MediaBrowserImplBase.this.mContext, MediaBrowserImplBase.this.mCallbacksMessenger);
                        } catch (RemoteException e) {
                            Log.w("MediaBrowserCompat", "RemoteException during connect for " + MediaBrowserImplBase.this.mServiceComponent);
                            if (MediaBrowserCompat.DEBUG) {
                                Log.d("MediaBrowserCompat", "ServiceCallbacks.onConnect...");
                                MediaBrowserImplBase.this.dump();
                            }
                        }
                    }
                });
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
                postOrRun(new Runnable() {
                    @Override
                    public void run() {
                        if (MediaBrowserCompat.DEBUG) {
                            Log.d("MediaBrowserCompat", "MediaServiceConnection.onServiceDisconnected name=" + name + " this=" + this + " mServiceConnection=" + MediaBrowserImplBase.this.mServiceConnection);
                            MediaBrowserImplBase.this.dump();
                        }
                        if (!MediaServiceConnection.this.isCurrent("onServiceDisconnected")) {
                            return;
                        }
                        MediaBrowserImplBase.this.mServiceBinderWrapper = null;
                        MediaBrowserImplBase.this.mCallbacksMessenger = null;
                        MediaBrowserImplBase.this.mHandler.setCallbacksMessenger(null);
                        MediaBrowserImplBase.this.mState = 4;
                        MediaBrowserImplBase.this.mCallback.onConnectionSuspended();
                    }
                });
            }

            private void postOrRun(Runnable r) {
                if (Thread.currentThread() == MediaBrowserImplBase.this.mHandler.getLooper().getThread()) {
                    r.run();
                } else {
                    MediaBrowserImplBase.this.mHandler.post(r);
                }
            }

            boolean isCurrent(String funcName) {
                if (MediaBrowserImplBase.this.mServiceConnection == this && MediaBrowserImplBase.this.mState != 0 && MediaBrowserImplBase.this.mState != 1) {
                    return true;
                }
                if (MediaBrowserImplBase.this.mState != 0 && MediaBrowserImplBase.this.mState != 1) {
                    Log.i("MediaBrowserCompat", funcName + " for " + MediaBrowserImplBase.this.mServiceComponent + " with mServiceConnection=" + MediaBrowserImplBase.this.mServiceConnection + " this=" + this);
                    return false;
                }
                return false;
            }
        }
    }

    static class MediaBrowserImplApi21 implements ConnectionCallback.ConnectionCallbackInternal, MediaBrowserImpl, MediaBrowserServiceCallbackImpl {
        protected final Object mBrowserObj;
        protected Messenger mCallbacksMessenger;
        final Context mContext;
        private MediaSessionCompat.Token mMediaSessionToken;
        private Bundle mNotifyChildrenChangedOptions;
        protected final Bundle mRootHints;
        protected ServiceBinderWrapper mServiceBinderWrapper;
        protected int mServiceVersion;
        protected final CallbackHandler mHandler = new CallbackHandler(this);
        private final ArrayMap<String, Subscription> mSubscriptions = new ArrayMap<>();

        MediaBrowserImplApi21(Context context, ComponentName serviceComponent, ConnectionCallback callback, Bundle rootHints) {
            this.mContext = context;
            rootHints = rootHints == null ? new Bundle() : rootHints;
            rootHints.putInt("extra_client_version", 1);
            this.mRootHints = new Bundle(rootHints);
            callback.setInternalConnectionCallback(this);
            this.mBrowserObj = MediaBrowserCompatApi21.createBrowser(context, serviceComponent, callback.mConnectionCallbackObj, this.mRootHints);
        }

        @Override
        public void connect() {
            MediaBrowserCompatApi21.connect(this.mBrowserObj);
        }

        @Override
        public void disconnect() {
            if (this.mServiceBinderWrapper != null && this.mCallbacksMessenger != null) {
                try {
                    this.mServiceBinderWrapper.unregisterCallbackMessenger(this.mCallbacksMessenger);
                } catch (RemoteException e) {
                    Log.i("MediaBrowserCompat", "Remote error unregistering client messenger.");
                }
            }
            MediaBrowserCompatApi21.disconnect(this.mBrowserObj);
        }

        @Override
        public MediaSessionCompat.Token getSessionToken() {
            if (this.mMediaSessionToken == null) {
                this.mMediaSessionToken = MediaSessionCompat.Token.fromToken(MediaBrowserCompatApi21.getSessionToken(this.mBrowserObj));
            }
            return this.mMediaSessionToken;
        }

        @Override
        public void onConnected() {
            Bundle extras = MediaBrowserCompatApi21.getExtras(this.mBrowserObj);
            if (extras == null) {
                return;
            }
            this.mServiceVersion = extras.getInt("extra_service_version", 0);
            IBinder serviceBinder = BundleCompat.getBinder(extras, "extra_messenger");
            if (serviceBinder != null) {
                this.mServiceBinderWrapper = new ServiceBinderWrapper(serviceBinder, this.mRootHints);
                this.mCallbacksMessenger = new Messenger(this.mHandler);
                this.mHandler.setCallbacksMessenger(this.mCallbacksMessenger);
                try {
                    this.mServiceBinderWrapper.registerCallbackMessenger(this.mContext, this.mCallbacksMessenger);
                } catch (RemoteException e) {
                    Log.i("MediaBrowserCompat", "Remote error registering client messenger.");
                }
            }
            IMediaSession sessionToken = IMediaSession.Stub.asInterface(BundleCompat.getBinder(extras, "extra_session_binder"));
            if (sessionToken != null) {
                this.mMediaSessionToken = MediaSessionCompat.Token.fromToken(MediaBrowserCompatApi21.getSessionToken(this.mBrowserObj), sessionToken);
            }
        }

        @Override
        public void onConnectionSuspended() {
            this.mServiceBinderWrapper = null;
            this.mCallbacksMessenger = null;
            this.mMediaSessionToken = null;
            this.mHandler.setCallbacksMessenger(null);
        }

        @Override
        public void onConnectionFailed() {
        }

        @Override
        public void onServiceConnected(Messenger callback, String root, MediaSessionCompat.Token session, Bundle extra) {
        }

        @Override
        public void onConnectionFailed(Messenger callback) {
        }

        @Override
        public void onLoadChildren(Messenger callback, String parentId, List list, Bundle options, Bundle notifyChildrenChangedOptions) {
            if (this.mCallbacksMessenger != callback) {
                return;
            }
            Subscription subscription = this.mSubscriptions.get(parentId);
            if (subscription == null) {
                if (MediaBrowserCompat.DEBUG) {
                    Log.d("MediaBrowserCompat", "onLoadChildren for id that isn't subscribed id=" + parentId);
                    return;
                }
                return;
            }
            SubscriptionCallback subscriptionCallback = subscription.getCallback(this.mContext, options);
            if (subscriptionCallback != null) {
                if (options == null) {
                    if (list == null) {
                        subscriptionCallback.onError(parentId);
                        return;
                    }
                    this.mNotifyChildrenChangedOptions = notifyChildrenChangedOptions;
                    subscriptionCallback.onChildrenLoaded(parentId, list);
                    this.mNotifyChildrenChangedOptions = null;
                    return;
                }
                if (list == null) {
                    subscriptionCallback.onError(parentId, options);
                    return;
                }
                this.mNotifyChildrenChangedOptions = notifyChildrenChangedOptions;
                subscriptionCallback.onChildrenLoaded(parentId, list, options);
                this.mNotifyChildrenChangedOptions = null;
            }
        }
    }

    static class MediaBrowserImplApi23 extends MediaBrowserImplApi21 {
        MediaBrowserImplApi23(Context context, ComponentName serviceComponent, ConnectionCallback callback, Bundle rootHints) {
            super(context, serviceComponent, callback, rootHints);
        }
    }

    static class MediaBrowserImplApi26 extends MediaBrowserImplApi23 {
        MediaBrowserImplApi26(Context context, ComponentName serviceComponent, ConnectionCallback callback, Bundle rootHints) {
            super(context, serviceComponent, callback, rootHints);
        }
    }

    private static class Subscription {
        private final List<SubscriptionCallback> mCallbacks = new ArrayList();
        private final List<Bundle> mOptionsList = new ArrayList();

        public List<Bundle> getOptionsList() {
            return this.mOptionsList;
        }

        public List<SubscriptionCallback> getCallbacks() {
            return this.mCallbacks;
        }

        public SubscriptionCallback getCallback(Context context, Bundle options) {
            if (options != null) {
                options.setClassLoader(context.getClassLoader());
            }
            for (int i = 0; i < this.mOptionsList.size(); i++) {
                if (MediaBrowserCompatUtils.areSameOptions(this.mOptionsList.get(i), options)) {
                    return this.mCallbacks.get(i);
                }
            }
            return null;
        }
    }

    private static class CallbackHandler extends Handler {
        private final WeakReference<MediaBrowserServiceCallbackImpl> mCallbackImplRef;
        private WeakReference<Messenger> mCallbacksMessengerRef;

        CallbackHandler(MediaBrowserServiceCallbackImpl callbackImpl) {
            this.mCallbackImplRef = new WeakReference<>(callbackImpl);
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mCallbacksMessengerRef == null || this.mCallbacksMessengerRef.get() == null || this.mCallbackImplRef.get() == null) {
                return;
            }
            Bundle data = msg.getData();
            data.setClassLoader(MediaSessionCompat.class.getClassLoader());
            MediaBrowserServiceCallbackImpl serviceCallback = this.mCallbackImplRef.get();
            Messenger callbacksMessenger = this.mCallbacksMessengerRef.get();
            try {
                switch (msg.what) {
                    case 1:
                        serviceCallback.onServiceConnected(callbacksMessenger, data.getString("data_media_item_id"), (MediaSessionCompat.Token) data.getParcelable("data_media_session_token"), data.getBundle("data_root_hints"));
                        break;
                    case 2:
                        serviceCallback.onConnectionFailed(callbacksMessenger);
                        break;
                    case 3:
                        serviceCallback.onLoadChildren(callbacksMessenger, data.getString("data_media_item_id"), data.getParcelableArrayList("data_media_item_list"), data.getBundle("data_options"), data.getBundle("data_notify_children_changed_options"));
                        break;
                    default:
                        Log.w("MediaBrowserCompat", "Unhandled message: " + msg + "\n  Client version: 1\n  Service version: " + msg.arg1);
                        break;
                }
            } catch (BadParcelableException e) {
                Log.e("MediaBrowserCompat", "Could not unparcel the data.");
                if (msg.what == 1) {
                    serviceCallback.onConnectionFailed(callbacksMessenger);
                }
            }
        }

        void setCallbacksMessenger(Messenger callbacksMessenger) {
            this.mCallbacksMessengerRef = new WeakReference<>(callbacksMessenger);
        }
    }

    private static class ServiceBinderWrapper {
        private Messenger mMessenger;
        private Bundle mRootHints;

        public ServiceBinderWrapper(IBinder target, Bundle rootHints) {
            this.mMessenger = new Messenger(target);
            this.mRootHints = rootHints;
        }

        void connect(Context context, Messenger callbacksMessenger) throws RemoteException {
            Bundle data = new Bundle();
            data.putString("data_package_name", context.getPackageName());
            data.putBundle("data_root_hints", this.mRootHints);
            sendRequest(1, data, callbacksMessenger);
        }

        void disconnect(Messenger callbacksMessenger) throws RemoteException {
            sendRequest(2, null, callbacksMessenger);
        }

        void addSubscription(String parentId, IBinder callbackToken, Bundle options, Messenger callbacksMessenger) throws RemoteException {
            Bundle data = new Bundle();
            data.putString("data_media_item_id", parentId);
            BundleCompat.putBinder(data, "data_callback_token", callbackToken);
            data.putBundle("data_options", options);
            sendRequest(3, data, callbacksMessenger);
        }

        void registerCallbackMessenger(Context context, Messenger callbackMessenger) throws RemoteException {
            Bundle data = new Bundle();
            data.putString("data_package_name", context.getPackageName());
            data.putBundle("data_root_hints", this.mRootHints);
            sendRequest(6, data, callbackMessenger);
        }

        void unregisterCallbackMessenger(Messenger callbackMessenger) throws RemoteException {
            sendRequest(7, null, callbackMessenger);
        }

        private void sendRequest(int what, Bundle data, Messenger cbMessenger) throws RemoteException {
            Message msg = Message.obtain();
            msg.what = what;
            msg.arg1 = 1;
            msg.setData(data);
            msg.replyTo = cbMessenger;
            this.mMessenger.send(msg);
        }
    }

    private static class ItemReceiver extends ResultReceiver {
        private final ItemCallback mCallback;
        private final String mMediaId;

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultData != null) {
                resultData.setClassLoader(MediaBrowserCompat.class.getClassLoader());
            }
            if (resultCode != 0 || resultData == null || !resultData.containsKey("media_item")) {
                this.mCallback.onError(this.mMediaId);
                return;
            }
            Parcelable item = resultData.getParcelable("media_item");
            if (item == null || (item instanceof MediaItem)) {
                this.mCallback.onItemLoaded((MediaItem) item);
            } else {
                this.mCallback.onError(this.mMediaId);
            }
        }
    }

    private static class SearchResultReceiver extends ResultReceiver {
        private final SearchCallback mCallback;
        private final Bundle mExtras;
        private final String mQuery;

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultData != null) {
                resultData.setClassLoader(MediaBrowserCompat.class.getClassLoader());
            }
            if (resultCode != 0 || resultData == null || !resultData.containsKey("search_results")) {
                this.mCallback.onError(this.mQuery, this.mExtras);
                return;
            }
            Parcelable[] items = resultData.getParcelableArray("search_results");
            List<MediaItem> results = null;
            if (items != null) {
                results = new ArrayList<>();
                for (Parcelable item : items) {
                    results.add((MediaItem) item);
                }
            }
            this.mCallback.onSearchResult(this.mQuery, this.mExtras, results);
        }
    }

    private static class CustomActionResultReceiver extends ResultReceiver {
        private final String mAction;
        private final CustomActionCallback mCallback;
        private final Bundle mExtras;

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (this.mCallback == null) {
            }
            switch (resultCode) {
                case -1:
                    this.mCallback.onError(this.mAction, this.mExtras, resultData);
                    break;
                case 0:
                    this.mCallback.onResult(this.mAction, this.mExtras, resultData);
                    break;
                case 1:
                    this.mCallback.onProgressUpdate(this.mAction, this.mExtras, resultData);
                    break;
                default:
                    Log.w("MediaBrowserCompat", "Unknown result code: " + resultCode + " (extras=" + this.mExtras + ", resultData=" + resultData + ")");
                    break;
            }
        }
    }
}
