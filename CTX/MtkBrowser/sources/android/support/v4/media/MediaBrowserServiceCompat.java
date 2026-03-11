package android.support.v4.media;

import android.app.Service;
import android.content.Intent;
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
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompatApi21;
import android.support.v4.media.MediaBrowserServiceCompatApi23;
import android.support.v4.media.MediaBrowserServiceCompatApi26;
import android.support.v4.media.MediaSessionManager;
import android.support.v4.media.session.IMediaSession;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class MediaBrowserServiceCompat extends Service {
    static final boolean DEBUG = Log.isLoggable("MBServiceCompat", 3);
    final ArrayMap<IBinder, ConnectionRecord> mConnections;
    ConnectionRecord mCurConnection;
    final ServiceHandler mHandler;
    private MediaBrowserServiceImpl mImpl;
    MediaSessionCompat.Token mSession;

    public static final class BrowserRoot {
        private final Bundle mExtras;
        private final String mRootId;

        public BrowserRoot(String str, Bundle bundle) {
            if (str == null) {
                throw new IllegalArgumentException("The root id in BrowserRoot cannot be null. Use null for BrowserRoot instead.");
            }
            this.mRootId = str;
            this.mExtras = bundle;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public String getRootId() {
            return this.mRootId;
        }
    }

    private class ConnectionRecord implements IBinder.DeathRecipient {
        public final MediaSessionManager.RemoteUserInfo browserInfo;
        public final ServiceCallbacks callbacks;
        public final int pid;
        public final String pkg;
        public BrowserRoot root;
        public final Bundle rootHints;
        public final HashMap<String, List<Pair<IBinder, Bundle>>> subscriptions = new HashMap<>();
        final MediaBrowserServiceCompat this$0;
        public final int uid;

        ConnectionRecord(MediaBrowserServiceCompat mediaBrowserServiceCompat, String str, int i, int i2, Bundle bundle, ServiceCallbacks serviceCallbacks) {
            this.this$0 = mediaBrowserServiceCompat;
            this.pkg = str;
            this.pid = i;
            this.uid = i2;
            this.browserInfo = new MediaSessionManager.RemoteUserInfo(str, i, i2);
            this.rootHints = bundle;
            this.callbacks = serviceCallbacks;
        }

        @Override
        public void binderDied() {
            this.this$0.mHandler.post(new Runnable(this) {
                final ConnectionRecord this$1;

                {
                    this.this$1 = this;
                }

                @Override
                public void run() {
                    this.this$1.this$0.mConnections.remove(this.this$1.callbacks.asBinder());
                }
            });
        }
    }

    interface MediaBrowserServiceImpl {
        IBinder onBind(Intent intent);

        void onCreate();
    }

    class MediaBrowserServiceImplApi21 implements MediaBrowserServiceImpl, MediaBrowserServiceCompatApi21.ServiceCompatProxy {
        Messenger mMessenger;
        final List<Bundle> mRootExtrasList = new ArrayList();
        Object mServiceObj;
        final MediaBrowserServiceCompat this$0;

        MediaBrowserServiceImplApi21(MediaBrowserServiceCompat mediaBrowserServiceCompat) {
            this.this$0 = mediaBrowserServiceCompat;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return MediaBrowserServiceCompatApi21.onBind(this.mServiceObj, intent);
        }

        @Override
        public void onCreate() {
            this.mServiceObj = MediaBrowserServiceCompatApi21.createService(this.this$0, this);
            MediaBrowserServiceCompatApi21.onCreate(this.mServiceObj);
        }

        @Override
        public MediaBrowserServiceCompatApi21.BrowserRoot onGetRoot(String str, int i, Bundle bundle) {
            Bundle extras;
            if (bundle == null || bundle.getInt("extra_client_version", 0) == 0) {
                extras = null;
            } else {
                bundle.remove("extra_client_version");
                this.mMessenger = new Messenger(this.this$0.mHandler);
                Bundle bundle2 = new Bundle();
                bundle2.putInt("extra_service_version", 2);
                BundleCompat.putBinder(bundle2, "extra_messenger", this.mMessenger.getBinder());
                if (this.this$0.mSession != null) {
                    IMediaSession extraBinder = this.this$0.mSession.getExtraBinder();
                    BundleCompat.putBinder(bundle2, "extra_session_binder", extraBinder == null ? null : extraBinder.asBinder());
                    extras = bundle2;
                } else {
                    this.mRootExtrasList.add(bundle2);
                    extras = bundle2;
                }
            }
            this.this$0.mCurConnection = new ConnectionRecord(this.this$0, str, -1, i, bundle, null);
            BrowserRoot browserRootOnGetRoot = this.this$0.onGetRoot(str, i, bundle);
            this.this$0.mCurConnection = null;
            if (browserRootOnGetRoot == null) {
                return null;
            }
            if (extras == null) {
                extras = browserRootOnGetRoot.getExtras();
            } else if (browserRootOnGetRoot.getExtras() != null) {
                extras.putAll(browserRootOnGetRoot.getExtras());
            }
            return new MediaBrowserServiceCompatApi21.BrowserRoot(browserRootOnGetRoot.getRootId(), extras);
        }

        @Override
        public void onLoadChildren(String str, MediaBrowserServiceCompatApi21.ResultWrapper<List<Parcel>> resultWrapper) {
            this.this$0.onLoadChildren(str, new Result<List<MediaBrowserCompat.MediaItem>>(this, str, resultWrapper) {
                final MediaBrowserServiceImplApi21 this$1;
                final MediaBrowserServiceCompatApi21.ResultWrapper val$resultWrapper;

                {
                    this.this$1 = this;
                    this.val$resultWrapper = resultWrapper;
                }

                @Override
                public void onResultSent(List<MediaBrowserCompat.MediaItem> list) {
                    ArrayList arrayList = null;
                    if (list != null) {
                        ArrayList arrayList2 = new ArrayList();
                        for (MediaBrowserCompat.MediaItem mediaItem : list) {
                            Parcel parcelObtain = Parcel.obtain();
                            mediaItem.writeToParcel(parcelObtain, 0);
                            arrayList2.add(parcelObtain);
                        }
                        arrayList = arrayList2;
                    }
                    this.val$resultWrapper.sendResult(arrayList);
                }
            });
        }
    }

    class MediaBrowserServiceImplApi23 extends MediaBrowserServiceImplApi21 implements MediaBrowserServiceCompatApi23.ServiceCompatProxy {
        final MediaBrowserServiceCompat this$0;

        MediaBrowserServiceImplApi23(MediaBrowserServiceCompat mediaBrowserServiceCompat) {
            super(mediaBrowserServiceCompat);
            this.this$0 = mediaBrowserServiceCompat;
        }

        @Override
        public void onCreate() {
            this.mServiceObj = MediaBrowserServiceCompatApi23.createService(this.this$0, this);
            MediaBrowserServiceCompatApi21.onCreate(this.mServiceObj);
        }

        @Override
        public void onLoadItem(String str, MediaBrowserServiceCompatApi21.ResultWrapper<Parcel> resultWrapper) {
            this.this$0.onLoadItem(str, new Result<MediaBrowserCompat.MediaItem>(this, str, resultWrapper) {
                final MediaBrowserServiceImplApi23 this$1;
                final MediaBrowserServiceCompatApi21.ResultWrapper val$resultWrapper;

                {
                    this.this$1 = this;
                    this.val$resultWrapper = resultWrapper;
                }

                @Override
                public void onResultSent(MediaBrowserCompat.MediaItem mediaItem) {
                    if (mediaItem == null) {
                        this.val$resultWrapper.sendResult(null);
                        return;
                    }
                    Parcel parcelObtain = Parcel.obtain();
                    mediaItem.writeToParcel(parcelObtain, 0);
                    this.val$resultWrapper.sendResult(parcelObtain);
                }
            });
        }
    }

    class MediaBrowserServiceImplApi26 extends MediaBrowserServiceImplApi23 implements MediaBrowserServiceCompatApi26.ServiceCompatProxy {
        final MediaBrowserServiceCompat this$0;

        MediaBrowserServiceImplApi26(MediaBrowserServiceCompat mediaBrowserServiceCompat) {
            super(mediaBrowserServiceCompat);
            this.this$0 = mediaBrowserServiceCompat;
        }

        @Override
        public void onCreate() {
            this.mServiceObj = MediaBrowserServiceCompatApi26.createService(this.this$0, this);
            MediaBrowserServiceCompatApi21.onCreate(this.mServiceObj);
        }

        @Override
        public void onLoadChildren(String str, MediaBrowserServiceCompatApi26.ResultWrapper resultWrapper, Bundle bundle) {
            this.this$0.onLoadChildren(str, new Result<List<MediaBrowserCompat.MediaItem>>(this, str, resultWrapper) {
                final MediaBrowserServiceImplApi26 this$1;
                final MediaBrowserServiceCompatApi26.ResultWrapper val$resultWrapper;

                {
                    this.this$1 = this;
                    this.val$resultWrapper = resultWrapper;
                }

                @Override
                public void onResultSent(List<MediaBrowserCompat.MediaItem> list) {
                    ArrayList arrayList = null;
                    if (list != null) {
                        ArrayList arrayList2 = new ArrayList();
                        for (MediaBrowserCompat.MediaItem mediaItem : list) {
                            Parcel parcelObtain = Parcel.obtain();
                            mediaItem.writeToParcel(parcelObtain, 0);
                            arrayList2.add(parcelObtain);
                        }
                        arrayList = arrayList2;
                    }
                    this.val$resultWrapper.sendResult(arrayList, getFlags());
                }
            }, bundle);
        }
    }

    class MediaBrowserServiceImplApi28 extends MediaBrowserServiceImplApi26 {
        final MediaBrowserServiceCompat this$0;

        MediaBrowserServiceImplApi28(MediaBrowserServiceCompat mediaBrowserServiceCompat) {
            super(mediaBrowserServiceCompat);
            this.this$0 = mediaBrowserServiceCompat;
        }
    }

    class MediaBrowserServiceImplBase implements MediaBrowserServiceImpl {
        private Messenger mMessenger;
        final MediaBrowserServiceCompat this$0;

        MediaBrowserServiceImplBase(MediaBrowserServiceCompat mediaBrowserServiceCompat) {
            this.this$0 = mediaBrowserServiceCompat;
        }

        @Override
        public IBinder onBind(Intent intent) {
            if ("android.media.browse.MediaBrowserService".equals(intent.getAction())) {
                return this.mMessenger.getBinder();
            }
            return null;
        }

        @Override
        public void onCreate() {
            this.mMessenger = new Messenger(this.this$0.mHandler);
        }
    }

    public static class Result<T> {
        private final Object mDebug;
        private boolean mDetachCalled;
        private int mFlags;
        private boolean mSendErrorCalled;
        private boolean mSendResultCalled;

        Result(Object obj) {
            this.mDebug = obj;
        }

        int getFlags() {
            return this.mFlags;
        }

        boolean isDone() {
            return this.mDetachCalled || this.mSendResultCalled || this.mSendErrorCalled;
        }

        void onErrorSent(Bundle bundle) {
            throw new UnsupportedOperationException("It is not supported to send an error for " + this.mDebug);
        }

        void onResultSent(T t) {
        }

        public void sendError(Bundle bundle) {
            if (!this.mSendResultCalled && !this.mSendErrorCalled) {
                this.mSendErrorCalled = true;
                onErrorSent(bundle);
            } else {
                throw new IllegalStateException("sendError() called when either sendResult() or sendError() had already been called for: " + this.mDebug);
            }
        }

        public void sendResult(T t) {
            if (!this.mSendResultCalled && !this.mSendErrorCalled) {
                this.mSendResultCalled = true;
                onResultSent(t);
            } else {
                throw new IllegalStateException("sendResult() called when either sendResult() or sendError() had already been called for: " + this.mDebug);
            }
        }

        void setFlags(int i) {
            this.mFlags = i;
        }
    }

    private class ServiceBinderImpl {
        final MediaBrowserServiceCompat this$0;

        public void addSubscription(String str, IBinder iBinder, Bundle bundle, ServiceCallbacks serviceCallbacks) {
            this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks, str, iBinder, bundle) {
                final ServiceBinderImpl this$1;
                final ServiceCallbacks val$callbacks;
                final String val$id;
                final Bundle val$options;
                final IBinder val$token;

                {
                    this.this$1 = this;
                    this.val$callbacks = serviceCallbacks;
                    this.val$id = str;
                    this.val$token = iBinder;
                    this.val$options = bundle;
                }

                @Override
                public void run() {
                    ConnectionRecord connectionRecord = this.this$1.this$0.mConnections.get(this.val$callbacks.asBinder());
                    if (connectionRecord != null) {
                        this.this$1.this$0.addSubscription(this.val$id, connectionRecord, this.val$token, this.val$options);
                        return;
                    }
                    Log.w("MBServiceCompat", "addSubscription for callback that isn't registered id=" + this.val$id);
                }
            });
        }

        public void connect(String str, int i, int i2, Bundle bundle, ServiceCallbacks serviceCallbacks) {
            if (this.this$0.isValidPackage(str, i2)) {
                this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks, str, i, i2, bundle) {
                    final ServiceBinderImpl this$1;
                    final ServiceCallbacks val$callbacks;
                    final int val$pid;
                    final String val$pkg;
                    final Bundle val$rootHints;
                    final int val$uid;

                    {
                        this.this$1 = this;
                        this.val$callbacks = serviceCallbacks;
                        this.val$pkg = str;
                        this.val$pid = i;
                        this.val$uid = i2;
                        this.val$rootHints = bundle;
                    }

                    @Override
                    public void run() {
                        IBinder iBinderAsBinder = this.val$callbacks.asBinder();
                        this.this$1.this$0.mConnections.remove(iBinderAsBinder);
                        ConnectionRecord connectionRecord = new ConnectionRecord(this.this$1.this$0, this.val$pkg, this.val$pid, this.val$uid, this.val$rootHints, this.val$callbacks);
                        this.this$1.this$0.mCurConnection = connectionRecord;
                        connectionRecord.root = this.this$1.this$0.onGetRoot(this.val$pkg, this.val$uid, this.val$rootHints);
                        this.this$1.this$0.mCurConnection = null;
                        if (connectionRecord.root != null) {
                            try {
                                this.this$1.this$0.mConnections.put(iBinderAsBinder, connectionRecord);
                                iBinderAsBinder.linkToDeath(connectionRecord, 0);
                                if (this.this$1.this$0.mSession != null) {
                                    this.val$callbacks.onConnect(connectionRecord.root.getRootId(), this.this$1.this$0.mSession, connectionRecord.root.getExtras());
                                    return;
                                }
                                return;
                            } catch (RemoteException e) {
                                Log.w("MBServiceCompat", "Calling onConnect() failed. Dropping client. pkg=" + this.val$pkg);
                                this.this$1.this$0.mConnections.remove(iBinderAsBinder);
                                return;
                            }
                        }
                        Log.i("MBServiceCompat", "No root for client " + this.val$pkg + " from service " + getClass().getName());
                        try {
                            this.val$callbacks.onConnectFailed();
                        } catch (RemoteException e2) {
                            Log.w("MBServiceCompat", "Calling onConnectFailed() failed. Ignoring. pkg=" + this.val$pkg);
                        }
                    }
                });
                return;
            }
            throw new IllegalArgumentException("Package/uid mismatch: uid=" + i2 + " package=" + str);
        }

        public void disconnect(ServiceCallbacks serviceCallbacks) {
            this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks) {
                final ServiceBinderImpl this$1;
                final ServiceCallbacks val$callbacks;

                {
                    this.this$1 = this;
                    this.val$callbacks = serviceCallbacks;
                }

                @Override
                public void run() {
                    ConnectionRecord connectionRecordRemove = this.this$1.this$0.mConnections.remove(this.val$callbacks.asBinder());
                    if (connectionRecordRemove != null) {
                        connectionRecordRemove.callbacks.asBinder().unlinkToDeath(connectionRecordRemove, 0);
                    }
                }
            });
        }

        public void getMediaItem(String str, ResultReceiver resultReceiver, ServiceCallbacks serviceCallbacks) {
            if (TextUtils.isEmpty(str) || resultReceiver == null) {
                return;
            }
            this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks, str, resultReceiver) {
                final ServiceBinderImpl this$1;
                final ServiceCallbacks val$callbacks;
                final String val$mediaId;
                final ResultReceiver val$receiver;

                {
                    this.this$1 = this;
                    this.val$callbacks = serviceCallbacks;
                    this.val$mediaId = str;
                    this.val$receiver = resultReceiver;
                }

                @Override
                public void run() {
                    ConnectionRecord connectionRecord = this.this$1.this$0.mConnections.get(this.val$callbacks.asBinder());
                    if (connectionRecord != null) {
                        this.this$1.this$0.performLoadItem(this.val$mediaId, connectionRecord, this.val$receiver);
                        return;
                    }
                    Log.w("MBServiceCompat", "getMediaItem for callback that isn't registered id=" + this.val$mediaId);
                }
            });
        }

        public void registerCallbacks(ServiceCallbacks serviceCallbacks, String str, int i, int i2, Bundle bundle) {
            this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks, str, i, i2, bundle) {
                final ServiceBinderImpl this$1;
                final ServiceCallbacks val$callbacks;
                final int val$pid;
                final String val$pkg;
                final Bundle val$rootHints;
                final int val$uid;

                {
                    this.this$1 = this;
                    this.val$callbacks = serviceCallbacks;
                    this.val$pkg = str;
                    this.val$pid = i;
                    this.val$uid = i2;
                    this.val$rootHints = bundle;
                }

                @Override
                public void run() {
                    IBinder iBinderAsBinder = this.val$callbacks.asBinder();
                    this.this$1.this$0.mConnections.remove(iBinderAsBinder);
                    ConnectionRecord connectionRecord = new ConnectionRecord(this.this$1.this$0, this.val$pkg, this.val$pid, this.val$uid, this.val$rootHints, this.val$callbacks);
                    this.this$1.this$0.mConnections.put(iBinderAsBinder, connectionRecord);
                    try {
                        iBinderAsBinder.linkToDeath(connectionRecord, 0);
                    } catch (RemoteException e) {
                        Log.w("MBServiceCompat", "IBinder is already dead.");
                    }
                }
            });
        }

        public void removeSubscription(String str, IBinder iBinder, ServiceCallbacks serviceCallbacks) {
            this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks, str, iBinder) {
                final ServiceBinderImpl this$1;
                final ServiceCallbacks val$callbacks;
                final String val$id;
                final IBinder val$token;

                {
                    this.this$1 = this;
                    this.val$callbacks = serviceCallbacks;
                    this.val$id = str;
                    this.val$token = iBinder;
                }

                @Override
                public void run() {
                    ConnectionRecord connectionRecord = this.this$1.this$0.mConnections.get(this.val$callbacks.asBinder());
                    if (connectionRecord == null) {
                        Log.w("MBServiceCompat", "removeSubscription for callback that isn't registered id=" + this.val$id);
                        return;
                    }
                    if (this.this$1.this$0.removeSubscription(this.val$id, connectionRecord, this.val$token)) {
                        return;
                    }
                    Log.w("MBServiceCompat", "removeSubscription called for " + this.val$id + " which is not subscribed");
                }
            });
        }

        public void search(String str, Bundle bundle, ResultReceiver resultReceiver, ServiceCallbacks serviceCallbacks) {
            if (TextUtils.isEmpty(str) || resultReceiver == null) {
                return;
            }
            this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks, str, bundle, resultReceiver) {
                final ServiceBinderImpl this$1;
                final ServiceCallbacks val$callbacks;
                final Bundle val$extras;
                final String val$query;
                final ResultReceiver val$receiver;

                {
                    this.this$1 = this;
                    this.val$callbacks = serviceCallbacks;
                    this.val$query = str;
                    this.val$extras = bundle;
                    this.val$receiver = resultReceiver;
                }

                @Override
                public void run() {
                    ConnectionRecord connectionRecord = this.this$1.this$0.mConnections.get(this.val$callbacks.asBinder());
                    if (connectionRecord != null) {
                        this.this$1.this$0.performSearch(this.val$query, this.val$extras, connectionRecord, this.val$receiver);
                        return;
                    }
                    Log.w("MBServiceCompat", "search for callback that isn't registered query=" + this.val$query);
                }
            });
        }

        public void sendCustomAction(String str, Bundle bundle, ResultReceiver resultReceiver, ServiceCallbacks serviceCallbacks) {
            if (TextUtils.isEmpty(str) || resultReceiver == null) {
                return;
            }
            this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks, str, bundle, resultReceiver) {
                final ServiceBinderImpl this$1;
                final String val$action;
                final ServiceCallbacks val$callbacks;
                final Bundle val$extras;
                final ResultReceiver val$receiver;

                {
                    this.this$1 = this;
                    this.val$callbacks = serviceCallbacks;
                    this.val$action = str;
                    this.val$extras = bundle;
                    this.val$receiver = resultReceiver;
                }

                @Override
                public void run() {
                    ConnectionRecord connectionRecord = this.this$1.this$0.mConnections.get(this.val$callbacks.asBinder());
                    if (connectionRecord != null) {
                        this.this$1.this$0.performCustomAction(this.val$action, this.val$extras, connectionRecord, this.val$receiver);
                        return;
                    }
                    Log.w("MBServiceCompat", "sendCustomAction for callback that isn't registered action=" + this.val$action + ", extras=" + this.val$extras);
                }
            });
        }

        public void unregisterCallbacks(ServiceCallbacks serviceCallbacks) {
            this.this$0.mHandler.postOrRun(new Runnable(this, serviceCallbacks) {
                final ServiceBinderImpl this$1;
                final ServiceCallbacks val$callbacks;

                {
                    this.this$1 = this;
                    this.val$callbacks = serviceCallbacks;
                }

                @Override
                public void run() {
                    IBinder iBinderAsBinder = this.val$callbacks.asBinder();
                    ConnectionRecord connectionRecordRemove = this.this$1.this$0.mConnections.remove(iBinderAsBinder);
                    if (connectionRecordRemove != null) {
                        iBinderAsBinder.unlinkToDeath(connectionRecordRemove, 0);
                    }
                }
            });
        }
    }

    private interface ServiceCallbacks {
        IBinder asBinder();

        void onConnect(String str, MediaSessionCompat.Token token, Bundle bundle) throws RemoteException;

        void onConnectFailed() throws RemoteException;

        void onLoadChildren(String str, List<MediaBrowserCompat.MediaItem> list, Bundle bundle, Bundle bundle2) throws RemoteException;
    }

    private static class ServiceCallbacksCompat implements ServiceCallbacks {
        final Messenger mCallbacks;

        ServiceCallbacksCompat(Messenger messenger) {
            this.mCallbacks = messenger;
        }

        private void sendRequest(int i, Bundle bundle) throws RemoteException {
            Message messageObtain = Message.obtain();
            messageObtain.what = i;
            messageObtain.arg1 = 2;
            messageObtain.setData(bundle);
            this.mCallbacks.send(messageObtain);
        }

        @Override
        public IBinder asBinder() {
            return this.mCallbacks.getBinder();
        }

        @Override
        public void onConnect(String str, MediaSessionCompat.Token token, Bundle bundle) throws RemoteException {
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putInt("extra_service_version", 2);
            Bundle bundle2 = new Bundle();
            bundle2.putString("data_media_item_id", str);
            bundle2.putParcelable("data_media_session_token", token);
            bundle2.putBundle("data_root_hints", bundle);
            sendRequest(1, bundle2);
        }

        @Override
        public void onConnectFailed() throws RemoteException {
            sendRequest(2, null);
        }

        @Override
        public void onLoadChildren(String str, List<MediaBrowserCompat.MediaItem> list, Bundle bundle, Bundle bundle2) throws RemoteException {
            Bundle bundle3 = new Bundle();
            bundle3.putString("data_media_item_id", str);
            bundle3.putBundle("data_options", bundle);
            bundle3.putBundle("data_notify_children_changed_options", bundle2);
            if (list != null) {
                bundle3.putParcelableArrayList("data_media_item_list", list instanceof ArrayList ? (ArrayList) list : new ArrayList<>(list));
            }
            sendRequest(3, bundle3);
        }
    }

    private final class ServiceHandler extends Handler {
        private final ServiceBinderImpl mServiceBinderImpl;

        @Override
        public void handleMessage(Message message) {
            Bundle data = message.getData();
            switch (message.what) {
                case 1:
                    this.mServiceBinderImpl.connect(data.getString("data_package_name"), data.getInt("data_calling_pid"), data.getInt("data_calling_uid"), data.getBundle("data_root_hints"), new ServiceCallbacksCompat(message.replyTo));
                    break;
                case 2:
                    this.mServiceBinderImpl.disconnect(new ServiceCallbacksCompat(message.replyTo));
                    break;
                case 3:
                    this.mServiceBinderImpl.addSubscription(data.getString("data_media_item_id"), BundleCompat.getBinder(data, "data_callback_token"), data.getBundle("data_options"), new ServiceCallbacksCompat(message.replyTo));
                    break;
                case 4:
                    this.mServiceBinderImpl.removeSubscription(data.getString("data_media_item_id"), BundleCompat.getBinder(data, "data_callback_token"), new ServiceCallbacksCompat(message.replyTo));
                    break;
                case 5:
                    this.mServiceBinderImpl.getMediaItem(data.getString("data_media_item_id"), (ResultReceiver) data.getParcelable("data_result_receiver"), new ServiceCallbacksCompat(message.replyTo));
                    break;
                case 6:
                    this.mServiceBinderImpl.registerCallbacks(new ServiceCallbacksCompat(message.replyTo), data.getString("data_package_name"), data.getInt("data_calling_pid"), data.getInt("data_calling_uid"), data.getBundle("data_root_hints"));
                    break;
                case 7:
                    this.mServiceBinderImpl.unregisterCallbacks(new ServiceCallbacksCompat(message.replyTo));
                    break;
                case 8:
                    this.mServiceBinderImpl.search(data.getString("data_search_query"), data.getBundle("data_search_extras"), (ResultReceiver) data.getParcelable("data_result_receiver"), new ServiceCallbacksCompat(message.replyTo));
                    break;
                case 9:
                    this.mServiceBinderImpl.sendCustomAction(data.getString("data_custom_action"), data.getBundle("data_custom_action_extras"), (ResultReceiver) data.getParcelable("data_result_receiver"), new ServiceCallbacksCompat(message.replyTo));
                    break;
                default:
                    Log.w("MBServiceCompat", "Unhandled message: " + message + "\n  Service version: 2\n  Client version: " + message.arg1);
                    break;
            }
        }

        public void postOrRun(Runnable runnable) {
            if (Thread.currentThread() == getLooper().getThread()) {
                runnable.run();
            } else {
                post(runnable);
            }
        }

        @Override
        public boolean sendMessageAtTime(Message message, long j) {
            Bundle data = message.getData();
            data.setClassLoader(MediaBrowserCompat.class.getClassLoader());
            data.putInt("data_calling_uid", Binder.getCallingUid());
            data.putInt("data_calling_pid", Binder.getCallingPid());
            return super.sendMessageAtTime(message, j);
        }
    }

    void addSubscription(String str, ConnectionRecord connectionRecord, IBinder iBinder, Bundle bundle) {
        List<Pair<IBinder, Bundle>> list = connectionRecord.subscriptions.get(str);
        List<Pair<IBinder, Bundle>> arrayList = list == null ? new ArrayList() : list;
        for (Pair<IBinder, Bundle> pair : arrayList) {
            if (iBinder == pair.first && MediaBrowserCompatUtils.areSameOptions(bundle, pair.second)) {
                return;
            }
        }
        arrayList.add(new Pair<>(iBinder, bundle));
        connectionRecord.subscriptions.put(str, arrayList);
        performLoadChildren(str, connectionRecord, bundle, null);
    }

    List<MediaBrowserCompat.MediaItem> applyOptions(List<MediaBrowserCompat.MediaItem> list, Bundle bundle) {
        if (list == null) {
            return null;
        }
        int i = bundle.getInt("android.media.browse.extra.PAGE", -1);
        int i2 = bundle.getInt("android.media.browse.extra.PAGE_SIZE", -1);
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

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    boolean isValidPackage(String str, int i) {
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

    @Override
    public IBinder onBind(Intent intent) {
        return this.mImpl.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 28) {
            this.mImpl = new MediaBrowserServiceImplApi28(this);
        } else if (Build.VERSION.SDK_INT >= 26) {
            this.mImpl = new MediaBrowserServiceImplApi26(this);
        } else if (Build.VERSION.SDK_INT >= 23) {
            this.mImpl = new MediaBrowserServiceImplApi23(this);
        } else if (Build.VERSION.SDK_INT >= 21) {
            this.mImpl = new MediaBrowserServiceImplApi21(this);
        } else {
            this.mImpl = new MediaBrowserServiceImplBase(this);
        }
        this.mImpl.onCreate();
    }

    public void onCustomAction(String str, Bundle bundle, Result<Bundle> result) {
        result.sendError(null);
    }

    public abstract BrowserRoot onGetRoot(String str, int i, Bundle bundle);

    public abstract void onLoadChildren(String str, Result<List<MediaBrowserCompat.MediaItem>> result);

    public void onLoadChildren(String str, Result<List<MediaBrowserCompat.MediaItem>> result, Bundle bundle) {
        result.setFlags(1);
        onLoadChildren(str, result);
    }

    public void onLoadItem(String str, Result<MediaBrowserCompat.MediaItem> result) {
        result.setFlags(2);
        result.sendResult(null);
    }

    public void onSearch(String str, Bundle bundle, Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.setFlags(4);
        result.sendResult(null);
    }

    void performCustomAction(String str, Bundle bundle, ConnectionRecord connectionRecord, ResultReceiver resultReceiver) {
        Result<Bundle> result = new Result<Bundle>(this, str, resultReceiver) {
            final MediaBrowserServiceCompat this$0;
            final ResultReceiver val$receiver;

            {
                this.this$0 = this;
                this.val$receiver = resultReceiver;
            }

            @Override
            void onErrorSent(Bundle bundle2) {
                this.val$receiver.send(-1, bundle2);
            }

            @Override
            public void onResultSent(Bundle bundle2) {
                this.val$receiver.send(0, bundle2);
            }
        };
        this.mCurConnection = connectionRecord;
        onCustomAction(str, bundle, result);
        this.mCurConnection = null;
        if (result.isDone()) {
            return;
        }
        throw new IllegalStateException("onCustomAction must call detach() or sendResult() or sendError() before returning for action=" + str + " extras=" + bundle);
    }

    void performLoadChildren(String str, ConnectionRecord connectionRecord, Bundle bundle, Bundle bundle2) {
        Result<List<MediaBrowserCompat.MediaItem>> result = new Result<List<MediaBrowserCompat.MediaItem>>(this, str, connectionRecord, str, bundle, bundle2) {
            final MediaBrowserServiceCompat this$0;
            final ConnectionRecord val$connection;
            final Bundle val$notifyChildrenChangedOptions;
            final String val$parentId;
            final Bundle val$subscribeOptions;

            {
                this.this$0 = this;
                this.val$connection = connectionRecord;
                this.val$parentId = str;
                this.val$subscribeOptions = bundle;
                this.val$notifyChildrenChangedOptions = bundle2;
            }

            @Override
            public void onResultSent(List<MediaBrowserCompat.MediaItem> list) {
                if (this.this$0.mConnections.get(this.val$connection.callbacks.asBinder()) != this.val$connection) {
                    if (MediaBrowserServiceCompat.DEBUG) {
                        Log.d("MBServiceCompat", "Not sending onLoadChildren result for connection that has been disconnected. pkg=" + this.val$connection.pkg + " id=" + this.val$parentId);
                        return;
                    }
                    return;
                }
                if ((getFlags() & 1) != 0) {
                    list = this.this$0.applyOptions(list, this.val$subscribeOptions);
                }
                try {
                    this.val$connection.callbacks.onLoadChildren(this.val$parentId, list, this.val$subscribeOptions, this.val$notifyChildrenChangedOptions);
                } catch (RemoteException e) {
                    Log.w("MBServiceCompat", "Calling onLoadChildren() failed for id=" + this.val$parentId + " package=" + this.val$connection.pkg);
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
        if (result.isDone()) {
            return;
        }
        throw new IllegalStateException("onLoadChildren must call detach() or sendResult() before returning for package=" + connectionRecord.pkg + " id=" + str);
    }

    void performLoadItem(String str, ConnectionRecord connectionRecord, ResultReceiver resultReceiver) {
        Result<MediaBrowserCompat.MediaItem> result = new Result<MediaBrowserCompat.MediaItem>(this, str, resultReceiver) {
            final MediaBrowserServiceCompat this$0;
            final ResultReceiver val$receiver;

            {
                this.this$0 = this;
                this.val$receiver = resultReceiver;
            }

            @Override
            public void onResultSent(MediaBrowserCompat.MediaItem mediaItem) {
                if ((getFlags() & 2) != 0) {
                    this.val$receiver.send(-1, null);
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putParcelable("media_item", mediaItem);
                this.val$receiver.send(0, bundle);
            }
        };
        this.mCurConnection = connectionRecord;
        onLoadItem(str, result);
        this.mCurConnection = null;
        if (result.isDone()) {
            return;
        }
        throw new IllegalStateException("onLoadItem must call detach() or sendResult() before returning for id=" + str);
    }

    void performSearch(String str, Bundle bundle, ConnectionRecord connectionRecord, ResultReceiver resultReceiver) {
        Result<List<MediaBrowserCompat.MediaItem>> result = new Result<List<MediaBrowserCompat.MediaItem>>(this, str, resultReceiver) {
            final MediaBrowserServiceCompat this$0;
            final ResultReceiver val$receiver;

            {
                this.this$0 = this;
                this.val$receiver = resultReceiver;
            }

            @Override
            public void onResultSent(List<MediaBrowserCompat.MediaItem> list) {
                if ((getFlags() & 4) != 0 || list == null) {
                    this.val$receiver.send(-1, null);
                    return;
                }
                Bundle bundle2 = new Bundle();
                bundle2.putParcelableArray("search_results", (Parcelable[]) list.toArray(new MediaBrowserCompat.MediaItem[0]));
                this.val$receiver.send(0, bundle2);
            }
        };
        this.mCurConnection = connectionRecord;
        onSearch(str, bundle, result);
        this.mCurConnection = null;
        if (result.isDone()) {
            return;
        }
        throw new IllegalStateException("onSearch must call detach() or sendResult() before returning for query=" + str);
    }

    boolean removeSubscription(String str, ConnectionRecord connectionRecord, IBinder iBinder) {
        boolean z;
        if (iBinder == null) {
            return connectionRecord.subscriptions.remove(str) != null;
        }
        List<Pair<IBinder, Bundle>> list = connectionRecord.subscriptions.get(str);
        if (list != null) {
            Iterator<Pair<IBinder, Bundle>> it = list.iterator();
            z = false;
            while (it.hasNext()) {
                if (iBinder == it.next().first) {
                    it.remove();
                    z = true;
                }
            }
            if (list.size() == 0) {
                connectionRecord.subscriptions.remove(str);
            }
        } else {
            z = false;
        }
        return z;
    }
}
