package android.support.v4.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.GuardedBy;
import android.support.v4.media.MediaLibraryService2;
import android.support.v4.media.MediaSession2;
import android.support.v4.media.MediaSession2ImplBase;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@TargetApi(19)
class MediaLibrarySessionImplBase extends MediaSession2ImplBase implements MediaLibraryService2.MediaLibrarySession.SupportLibraryImpl {
    private final MediaBrowserServiceCompat mBrowserServiceLegacyStub;

    @GuardedBy("mLock")
    private final ArrayMap<MediaSession2.ControllerInfo, Set<String>> mSubscriptions;

    MediaLibrarySessionImplBase(MediaLibraryService2.MediaLibrarySession instance, Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, MediaSession2.SessionCallback callback) {
        super(instance, context, id, player, playlistAgent, volumeProvider, sessionActivity, callbackExecutor, callback);
        this.mSubscriptions = new ArrayMap<>();
        this.mBrowserServiceLegacyStub = new MediaLibraryService2LegacyStub(this);
        this.mBrowserServiceLegacyStub.attachToBaseContext(context);
        this.mBrowserServiceLegacyStub.onCreate();
    }

    @Override
    public MediaLibraryService2.MediaLibrarySession getInstance() {
        return (MediaLibraryService2.MediaLibrarySession) super.getInstance();
    }

    @Override
    public MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback getCallback() {
        return (MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback) super.getCallback();
    }

    @Override
    public IBinder getLegacySessionBinder() {
        Intent intent = new Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE);
        return this.mBrowserServiceLegacyStub.onBind(intent);
    }

    @Override
    public void notifyChildrenChanged(final String parentId, final int itemCount, final Bundle extras) {
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        }
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount shouldn't be negative");
        }
        List<MediaSession2.ControllerInfo> controllers = getConnectedControllers();
        MediaSession2ImplBase.NotifyRunnable runnable = new MediaSession2ImplBase.NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onChildrenChanged(parentId, itemCount, extras);
            }
        };
        for (int i = 0; i < controllers.size(); i++) {
            if (isSubscribed(controllers.get(i), parentId)) {
                notifyToController(controllers.get(i), runnable);
            }
        }
    }

    @Override
    public void notifyChildrenChanged(final MediaSession2.ControllerInfo controller, final String parentId, final int itemCount, final Bundle extras) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        }
        if (TextUtils.isEmpty(parentId)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        }
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount shouldn't be negative");
        }
        notifyToController(controller, new MediaSession2ImplBase.NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                if (!MediaLibrarySessionImplBase.this.isSubscribed(controller, parentId)) {
                    if (MediaSession2ImplBase.DEBUG) {
                        Log.d("MS2ImplBase", "Skipping notifyChildrenChanged() to " + controller + " because it hasn't subscribed");
                        MediaLibrarySessionImplBase.this.dumpSubscription();
                        return;
                    }
                    return;
                }
                callback.onChildrenChanged(parentId, itemCount, extras);
            }
        });
    }

    @Override
    public void notifySearchResultChanged(MediaSession2.ControllerInfo controller, final String query, final int itemCount, final Bundle extras) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        }
        if (TextUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        }
        notifyToController(controller, new MediaSession2ImplBase.NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onSearchResultChanged(query, itemCount, extras);
            }
        });
    }

    @Override
    public void onGetLibraryRootOnExecutor(MediaSession2.ControllerInfo controller, final Bundle rootHints) {
        final MediaLibraryService2.LibraryRoot root = getCallback().onGetLibraryRoot(getInstance(), controller, rootHints);
        notifyToController(controller, new MediaSession2ImplBase.NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onGetLibraryRootDone(rootHints, root == null ? null : root.getRootId(), root != null ? root.getExtras() : null);
            }
        });
    }

    @Override
    public void onGetItemOnExecutor(MediaSession2.ControllerInfo controller, final String mediaId) {
        final MediaItem2 result = getCallback().onGetItem(getInstance(), controller, mediaId);
        notifyToController(controller, new MediaSession2ImplBase.NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onGetItemDone(mediaId, result);
            }
        });
    }

    @Override
    public void onGetChildrenOnExecutor(MediaSession2.ControllerInfo controller, final String parentId, final int page, final int pageSize, final Bundle extras) {
        final List<MediaItem2> result = getCallback().onGetChildren(getInstance(), controller, parentId, page, pageSize, extras);
        if (result != null && result.size() > pageSize) {
            throw new IllegalArgumentException("onGetChildren() shouldn't return media items more than pageSize. result.size()=" + result.size() + " pageSize=" + pageSize);
        }
        notifyToController(controller, new MediaSession2ImplBase.NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onGetChildrenDone(parentId, page, pageSize, result, extras);
            }
        });
    }

    @Override
    public void onSubscribeOnExecutor(MediaSession2.ControllerInfo controller, String parentId, Bundle option) {
        synchronized (this.mLock) {
            Set<String> subscription = this.mSubscriptions.get(controller);
            if (subscription == null) {
                subscription = new HashSet();
                this.mSubscriptions.put(controller, subscription);
            }
            subscription.add(parentId);
        }
        getCallback().onSubscribe(getInstance(), controller, parentId, option);
    }

    @Override
    public void onUnsubscribeOnExecutor(MediaSession2.ControllerInfo controller, String parentId) {
        getCallback().onUnsubscribe(getInstance(), controller, parentId);
        synchronized (this.mLock) {
            this.mSubscriptions.remove(controller);
        }
    }

    @Override
    public void onSearchOnExecutor(MediaSession2.ControllerInfo controller, String query, Bundle extras) {
        getCallback().onSearch(getInstance(), controller, query, extras);
    }

    @Override
    public void onGetSearchResultOnExecutor(MediaSession2.ControllerInfo controller, final String query, final int page, final int pageSize, final Bundle extras) {
        final List<MediaItem2> result = getCallback().onGetSearchResult(getInstance(), controller, query, page, pageSize, extras);
        if (result != null && result.size() > pageSize) {
            throw new IllegalArgumentException("onGetSearchResult() shouldn't return media items more than pageSize. result.size()=" + result.size() + " pageSize=" + pageSize);
        }
        notifyToController(controller, new MediaSession2ImplBase.NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onGetSearchResultDone(query, page, pageSize, result, extras);
            }
        });
    }

    private boolean isSubscribed(MediaSession2.ControllerInfo controller, String parentId) {
        synchronized (this.mLock) {
            Set<String> subscriptions = this.mSubscriptions.get(controller);
            if (subscriptions != null && subscriptions.contains(parentId)) {
                return true;
            }
            return false;
        }
    }

    private void dumpSubscription() {
        if (!DEBUG) {
            return;
        }
        synchronized (this.mLock) {
            Log.d("MS2ImplBase", "Dumping subscription, controller sz=" + this.mSubscriptions.size());
            for (int i = 0; i < this.mSubscriptions.size(); i++) {
                Log.d("MS2ImplBase", "  controller " + this.mSubscriptions.valueAt(i));
                for (String parentId : this.mSubscriptions.valueAt(i)) {
                    Log.d("MS2ImplBase", "  - " + parentId);
                }
            }
        }
    }
}
