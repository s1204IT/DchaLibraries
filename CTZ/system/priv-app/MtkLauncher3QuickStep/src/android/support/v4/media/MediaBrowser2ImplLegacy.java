package android.support.v4.media;

import android.content.Context;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowser2;
import android.support.v4.media.MediaBrowserCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class MediaBrowser2ImplLegacy extends MediaController2ImplLegacy implements MediaBrowser2.SupportLibraryImpl {
    public static final String EXTRA_ITEM_COUNT = "android.media.browse.extra.ITEM_COUNT";
    @GuardedBy("mLock")
    private final HashMap<Bundle, MediaBrowserCompat> mBrowserCompats;
    @GuardedBy("mLock")
    private final HashMap<String, List<SubscribeCallback>> mSubscribeCallbacks;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MediaBrowser2ImplLegacy(@NonNull Context context, MediaBrowser2 instance, @NonNull SessionToken2 token, @NonNull Executor executor, @NonNull MediaBrowser2.BrowserCallback callback) {
        super(context, instance, token, executor, callback);
        this.mBrowserCompats = new HashMap<>();
        this.mSubscribeCallbacks = new HashMap<>();
    }

    @Override // android.support.v4.media.MediaController2ImplLegacy, android.support.v4.media.MediaController2.SupportLibraryImpl
    public MediaBrowser2 getInstance() {
        return (MediaBrowser2) super.getInstance();
    }

    @Override // android.support.v4.media.MediaController2ImplLegacy, java.lang.AutoCloseable
    public void close() {
        synchronized (this.mLock) {
            for (MediaBrowserCompat browser : this.mBrowserCompats.values()) {
                browser.disconnect();
            }
            this.mBrowserCompats.clear();
            super.close();
        }
    }

    @Override // android.support.v4.media.MediaBrowser2.SupportLibraryImpl
    public void getLibraryRoot(@Nullable final Bundle extras) {
        final MediaBrowserCompat browser = getBrowserCompat(extras);
        if (browser != null) {
            getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.1
                @Override // java.lang.Runnable
                public void run() {
                    MediaBrowser2ImplLegacy.this.getCallback().onGetLibraryRootDone(MediaBrowser2ImplLegacy.this.getInstance(), extras, browser.getRoot(), browser.getExtras());
                }
            });
        } else {
            getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.2
                @Override // java.lang.Runnable
                public void run() {
                    MediaBrowserCompat newBrowser = new MediaBrowserCompat(MediaBrowser2ImplLegacy.this.getContext(), MediaBrowser2ImplLegacy.this.getSessionToken().getComponentName(), new GetLibraryRootCallback(extras), extras);
                    synchronized (MediaBrowser2ImplLegacy.this.mLock) {
                        MediaBrowser2ImplLegacy.this.mBrowserCompats.put(extras, newBrowser);
                    }
                    newBrowser.connect();
                }
            });
        }
    }

    @Override // android.support.v4.media.MediaBrowser2.SupportLibraryImpl
    public void subscribe(@NonNull String parentId, @Nullable Bundle extras) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId shouldn't be null");
        }
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        SubscribeCallback callback = new SubscribeCallback();
        synchronized (this.mLock) {
            List<SubscribeCallback> list = this.mSubscribeCallbacks.get(parentId);
            if (list == null) {
                list = new ArrayList();
                this.mSubscribeCallbacks.put(parentId, list);
            }
            list.add(callback);
        }
        Bundle options = new Bundle();
        options.putBundle("android.support.v4.media.argument.EXTRAS", extras);
        browser.subscribe(parentId, options, callback);
    }

    @Override // android.support.v4.media.MediaBrowser2.SupportLibraryImpl
    public void unsubscribe(@NonNull String parentId) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId shouldn't be null");
        }
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        synchronized (this.mLock) {
            List<SubscribeCallback> list = this.mSubscribeCallbacks.get(parentId);
            if (list == null) {
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                browser.unsubscribe(parentId, list.get(i));
            }
        }
    }

    @Override // android.support.v4.media.MediaBrowser2.SupportLibraryImpl
    public void getChildren(@NonNull String parentId, int page, int pageSize, @Nullable Bundle extras) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId shouldn't be null");
        }
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Neither page nor pageSize should be less than 1");
        }
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        Bundle options = MediaUtils2.createBundle(extras);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        browser.subscribe(parentId, options, new GetChildrenCallback(parentId, page, pageSize));
    }

    @Override // android.support.v4.media.MediaBrowser2.SupportLibraryImpl
    public void getItem(@NonNull final String mediaId) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        browser.getItem(mediaId, new MediaBrowserCompat.ItemCallback() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.3
            @Override // android.support.v4.media.MediaBrowserCompat.ItemCallback
            public void onItemLoaded(final MediaBrowserCompat.MediaItem item) {
                MediaBrowser2ImplLegacy.this.getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.3.1
                    @Override // java.lang.Runnable
                    public void run() {
                        MediaBrowser2ImplLegacy.this.getCallback().onGetItemDone(MediaBrowser2ImplLegacy.this.getInstance(), mediaId, MediaUtils2.convertToMediaItem2(item));
                    }
                });
            }

            @Override // android.support.v4.media.MediaBrowserCompat.ItemCallback
            public void onError(String itemId) {
                MediaBrowser2ImplLegacy.this.getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.3.2
                    @Override // java.lang.Runnable
                    public void run() {
                        MediaBrowser2ImplLegacy.this.getCallback().onGetItemDone(MediaBrowser2ImplLegacy.this.getInstance(), mediaId, null);
                    }
                });
            }
        });
    }

    @Override // android.support.v4.media.MediaBrowser2.SupportLibraryImpl
    public void search(@NonNull String query, @Nullable Bundle extras) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        browser.search(query, extras, new MediaBrowserCompat.SearchCallback() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.4
            @Override // android.support.v4.media.MediaBrowserCompat.SearchCallback
            public void onSearchResult(final String query2, final Bundle extras2, final List<MediaBrowserCompat.MediaItem> items) {
                MediaBrowser2ImplLegacy.this.getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.4.1
                    @Override // java.lang.Runnable
                    public void run() {
                        MediaBrowser2ImplLegacy.this.getCallback().onSearchResultChanged(MediaBrowser2ImplLegacy.this.getInstance(), query2, items.size(), extras2);
                    }
                });
            }

            @Override // android.support.v4.media.MediaBrowserCompat.SearchCallback
            public void onError(String query2, Bundle extras2) {
            }
        });
    }

    @Override // android.support.v4.media.MediaBrowser2.SupportLibraryImpl
    public void getSearchResult(@NonNull String query, final int page, final int pageSize, @Nullable final Bundle extras) {
        MediaBrowserCompat browser = getBrowserCompat();
        if (browser == null) {
            return;
        }
        Bundle options = MediaUtils2.createBundle(extras);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE, page);
        options.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, pageSize);
        browser.search(query, options, new MediaBrowserCompat.SearchCallback() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.5
            @Override // android.support.v4.media.MediaBrowserCompat.SearchCallback
            public void onSearchResult(final String query2, Bundle extrasSent, final List<MediaBrowserCompat.MediaItem> items) {
                MediaBrowser2ImplLegacy.this.getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.5.1
                    @Override // java.lang.Runnable
                    public void run() {
                        List<MediaItem2> item2List = MediaUtils2.convertMediaItemListToMediaItem2List(items);
                        MediaBrowser2ImplLegacy.this.getCallback().onGetSearchResultDone(MediaBrowser2ImplLegacy.this.getInstance(), query2, page, pageSize, item2List, extras);
                    }
                });
            }

            @Override // android.support.v4.media.MediaBrowserCompat.SearchCallback
            public void onError(final String query2, Bundle extrasSent) {
                MediaBrowser2ImplLegacy.this.getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.5.2
                    @Override // java.lang.Runnable
                    public void run() {
                        MediaBrowser2ImplLegacy.this.getCallback().onGetSearchResultDone(MediaBrowser2ImplLegacy.this.getInstance(), query2, page, pageSize, null, extras);
                    }
                });
            }
        });
    }

    @Override // android.support.v4.media.MediaController2ImplLegacy, android.support.v4.media.MediaController2.SupportLibraryImpl
    public MediaBrowser2.BrowserCallback getCallback() {
        return (MediaBrowser2.BrowserCallback) super.getCallback();
    }

    private MediaBrowserCompat getBrowserCompat(Bundle extras) {
        MediaBrowserCompat mediaBrowserCompat;
        synchronized (this.mLock) {
            mediaBrowserCompat = this.mBrowserCompats.get(extras);
        }
        return mediaBrowserCompat;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Bundle getExtrasWithoutPagination(Bundle extras) {
        if (extras == null) {
            return null;
        }
        extras.setClassLoader(getContext().getClassLoader());
        try {
            extras.remove(MediaBrowserCompat.EXTRA_PAGE);
            extras.remove(MediaBrowserCompat.EXTRA_PAGE_SIZE);
        } catch (BadParcelableException e) {
        }
        return extras;
    }

    /* loaded from: classes.dex */
    private class GetLibraryRootCallback extends MediaBrowserCompat.ConnectionCallback {
        private final Bundle mExtras;

        GetLibraryRootCallback(Bundle extras) {
            this.mExtras = extras;
        }

        @Override // android.support.v4.media.MediaBrowserCompat.ConnectionCallback
        public void onConnected() {
            MediaBrowser2ImplLegacy.this.getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.GetLibraryRootCallback.1
                @Override // java.lang.Runnable
                public void run() {
                    MediaBrowserCompat browser;
                    synchronized (MediaBrowser2ImplLegacy.this.mLock) {
                        browser = (MediaBrowserCompat) MediaBrowser2ImplLegacy.this.mBrowserCompats.get(GetLibraryRootCallback.this.mExtras);
                    }
                    if (browser == null) {
                        return;
                    }
                    MediaBrowser2ImplLegacy.this.getCallback().onGetLibraryRootDone(MediaBrowser2ImplLegacy.this.getInstance(), GetLibraryRootCallback.this.mExtras, browser.getRoot(), browser.getExtras());
                }
            });
        }

        @Override // android.support.v4.media.MediaBrowserCompat.ConnectionCallback
        public void onConnectionSuspended() {
            MediaBrowser2ImplLegacy.this.close();
        }

        @Override // android.support.v4.media.MediaBrowserCompat.ConnectionCallback
        public void onConnectionFailed() {
            MediaBrowser2ImplLegacy.this.close();
        }
    }

    /* loaded from: classes.dex */
    private class SubscribeCallback extends MediaBrowserCompat.SubscriptionCallback {
        private SubscribeCallback() {
        }

        @Override // android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
        public void onError(String parentId) {
            onChildrenLoaded(parentId, null, null);
        }

        @Override // android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
        public void onError(String parentId, Bundle options) {
            onChildrenLoaded(parentId, null, options);
        }

        @Override // android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
        public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children) {
            onChildrenLoaded(parentId, children, null);
        }

        @Override // android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
        public void onChildrenLoaded(final String parentId, List<MediaBrowserCompat.MediaItem> children, Bundle options) {
            final int itemCount;
            if (options != null && options.containsKey(MediaBrowser2ImplLegacy.EXTRA_ITEM_COUNT)) {
                itemCount = options.getInt(MediaBrowser2ImplLegacy.EXTRA_ITEM_COUNT);
            } else if (children != null) {
                itemCount = children.size();
            } else {
                return;
            }
            final Bundle notifyChildrenChangedOptions = MediaBrowser2ImplLegacy.this.getBrowserCompat().getNotifyChildrenChangedOptions();
            MediaBrowser2ImplLegacy.this.getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.SubscribeCallback.1
                @Override // java.lang.Runnable
                public void run() {
                    MediaBrowser2ImplLegacy.this.getCallback().onChildrenChanged(MediaBrowser2ImplLegacy.this.getInstance(), parentId, itemCount, notifyChildrenChangedOptions);
                }
            });
        }
    }

    /* loaded from: classes.dex */
    private class GetChildrenCallback extends MediaBrowserCompat.SubscriptionCallback {
        private final int mPage;
        private final int mPageSize;
        private final String mParentId;

        GetChildrenCallback(String parentId, int page, int pageSize) {
            this.mParentId = parentId;
            this.mPage = page;
            this.mPageSize = pageSize;
        }

        @Override // android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
        public void onError(String parentId) {
            onChildrenLoaded(parentId, null, null);
        }

        @Override // android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
        public void onError(String parentId, Bundle options) {
            onChildrenLoaded(parentId, null, options);
        }

        @Override // android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
        public void onChildrenLoaded(String parentId, List<MediaBrowserCompat.MediaItem> children) {
            onChildrenLoaded(parentId, children, null);
        }

        @Override // android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
        public void onChildrenLoaded(final String parentId, List<MediaBrowserCompat.MediaItem> children, Bundle options) {
            final List<MediaItem2> items;
            if (children == null) {
                items = null;
            } else {
                items = new ArrayList<>();
                for (int i = 0; i < children.size(); i++) {
                    items.add(MediaUtils2.convertToMediaItem2(children.get(i)));
                }
            }
            final Bundle extras = MediaBrowser2ImplLegacy.this.getExtrasWithoutPagination(options);
            MediaBrowser2ImplLegacy.this.getCallbackExecutor().execute(new Runnable() { // from class: android.support.v4.media.MediaBrowser2ImplLegacy.GetChildrenCallback.1
                @Override // java.lang.Runnable
                public void run() {
                    MediaBrowserCompat browser = MediaBrowser2ImplLegacy.this.getBrowserCompat();
                    if (browser != null) {
                        MediaBrowser2ImplLegacy.this.getCallback().onGetChildrenDone(MediaBrowser2ImplLegacy.this.getInstance(), parentId, GetChildrenCallback.this.mPage, GetChildrenCallback.this.mPageSize, items, extras);
                        browser.unsubscribe(GetChildrenCallback.this.mParentId, GetChildrenCallback.this);
                    }
                }
            });
        }
    }
}
