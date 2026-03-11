package android.support.v4.media.session;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.MediaDescriptionCompat;

public final class MediaSessionCompat$QueueItem implements Parcelable {
    public static final Parcelable.Creator<MediaSessionCompat$QueueItem> CREATOR = new Parcelable.Creator<MediaSessionCompat$QueueItem>() {
        @Override
        public MediaSessionCompat$QueueItem createFromParcel(Parcel p) {
            return new MediaSessionCompat$QueueItem(p, null);
        }

        @Override
        public MediaSessionCompat$QueueItem[] newArray(int size) {
            return new MediaSessionCompat$QueueItem[size];
        }
    };
    private final MediaDescriptionCompat mDescription;
    private final long mId;

    MediaSessionCompat$QueueItem(Parcel in, MediaSessionCompat$QueueItem mediaSessionCompat$QueueItem) {
        this(in);
    }

    private MediaSessionCompat$QueueItem(Parcel in) {
        this.mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(in);
        this.mId = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        this.mDescription.writeToParcel(dest, flags);
        dest.writeLong(this.mId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "MediaSession.QueueItem {Description=" + this.mDescription + ", Id=" + this.mId + " }";
    }
}
