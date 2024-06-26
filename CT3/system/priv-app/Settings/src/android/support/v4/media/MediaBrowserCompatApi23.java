package android.support.v4.media;

import android.media.browse.MediaBrowser;
import android.os.Parcel;
import android.support.annotation.NonNull;
/* loaded from: classes.dex */
class MediaBrowserCompatApi23 {

    /* loaded from: classes.dex */
    interface ItemCallback {
        void onError(@NonNull String str);

        void onItemLoaded(Parcel parcel);
    }

    MediaBrowserCompatApi23() {
    }

    public static Object createItemCallback(ItemCallback callback) {
        return new ItemCallbackProxy(callback);
    }

    /* loaded from: classes.dex */
    static class ItemCallbackProxy<T extends ItemCallback> extends MediaBrowser.ItemCallback {
        protected final T mItemCallback;

        public ItemCallbackProxy(T callback) {
            this.mItemCallback = callback;
        }

        @Override // android.media.browse.MediaBrowser.ItemCallback
        public void onItemLoaded(MediaBrowser.MediaItem item) {
            Parcel parcel = Parcel.obtain();
            item.writeToParcel(parcel, 0);
            this.mItemCallback.onItemLoaded(parcel);
        }

        @Override // android.media.browse.MediaBrowser.ItemCallback
        public void onError(@NonNull String itemId) {
            this.mItemCallback.onError(itemId);
        }
    }
}
