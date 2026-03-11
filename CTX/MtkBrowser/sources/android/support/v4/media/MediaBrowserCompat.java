package android.support.v4.media;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class MediaBrowserCompat {
    static final boolean DEBUG = Log.isLoggable("MediaBrowserCompat", 3);
    private final MediaBrowserImpl mImpl;

    public static abstract class CustomActionCallback {
        public void onError(String str, Bundle bundle, Bundle bundle2) {
        }

        public void onProgressUpdate(String str, Bundle bundle, Bundle bundle2) {
        }

        public void onResult(String str, Bundle bundle, Bundle bundle2) {
        }
    }

    private static class CustomActionResultReceiver extends ResultReceiver {
        private final String mAction;
        private final CustomActionCallback mCallback;
        private final Bundle mExtras;

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (this.mCallback == null) {
            }
            switch (i) {
                case -1:
                    this.mCallback.onError(this.mAction, this.mExtras, bundle);
                    break;
                case 0:
                    this.mCallback.onResult(this.mAction, this.mExtras, bundle);
                    break;
                case 1:
                    this.mCallback.onProgressUpdate(this.mAction, this.mExtras, bundle);
                    break;
                default:
                    Log.w("MediaBrowserCompat", "Unknown result code: " + i + " (extras=" + this.mExtras + ", resultData=" + bundle + ")");
                    break;
            }
        }
    }

    public static abstract class ItemCallback {
        public void onError(String str) {
        }

        public void onItemLoaded(MediaItem mediaItem) {
        }
    }

    private static class ItemReceiver extends ResultReceiver {
        private final ItemCallback mCallback;
        private final String mMediaId;

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (bundle != null) {
                bundle.setClassLoader(MediaBrowserCompat.class.getClassLoader());
            }
            if (i != 0 || bundle == null || !bundle.containsKey("media_item")) {
                this.mCallback.onError(this.mMediaId);
                return;
            }
            Parcelable parcelable = bundle.getParcelable("media_item");
            if (parcelable == null || (parcelable instanceof MediaItem)) {
                this.mCallback.onItemLoaded((MediaItem) parcelable);
            } else {
                this.mCallback.onError(this.mMediaId);
            }
        }
    }

    interface MediaBrowserImpl {
        void disconnect();
    }

    public static class MediaItem implements Parcelable {
        public static final Parcelable.Creator<MediaItem> CREATOR = new Parcelable.Creator<MediaItem>() {
            @Override
            public MediaItem createFromParcel(Parcel parcel) {
                return new MediaItem(parcel);
            }

            @Override
            public MediaItem[] newArray(int i) {
                return new MediaItem[i];
            }
        };
        private final MediaDescriptionCompat mDescription;
        private final int mFlags;

        MediaItem(Parcel parcel) {
            this.mFlags = parcel.readInt();
            this.mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(parcel);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "MediaItem{mFlags=" + this.mFlags + ", mDescription=" + this.mDescription + '}';
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mFlags);
            this.mDescription.writeToParcel(parcel, i);
        }
    }

    public static abstract class SearchCallback {
        public void onError(String str, Bundle bundle) {
        }

        public void onSearchResult(String str, Bundle bundle, List<MediaItem> list) {
        }
    }

    private static class SearchResultReceiver extends ResultReceiver {
        private final SearchCallback mCallback;
        private final Bundle mExtras;
        private final String mQuery;

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (bundle != null) {
                bundle.setClassLoader(MediaBrowserCompat.class.getClassLoader());
            }
            if (i != 0 || bundle == null || !bundle.containsKey("search_results")) {
                this.mCallback.onError(this.mQuery, this.mExtras);
                return;
            }
            Parcelable[] parcelableArray = bundle.getParcelableArray("search_results");
            ArrayList arrayList = null;
            if (parcelableArray != null) {
                ArrayList arrayList2 = new ArrayList();
                for (Parcelable parcelable : parcelableArray) {
                    arrayList2.add((MediaItem) parcelable);
                }
                arrayList = arrayList2;
            }
            this.mCallback.onSearchResult(this.mQuery, this.mExtras, arrayList);
        }
    }

    public void disconnect() {
        this.mImpl.disconnect();
    }
}
