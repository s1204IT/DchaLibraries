package android.support.v4.media;

import android.os.Bundle;
import android.text.TextUtils;
import java.util.UUID;

public class MediaItem2 {
    private DataSourceDesc mDataSourceDesc;
    private final int mFlags;
    private final String mId;
    private MediaMetadata2 mMetadata;
    private final UUID mUUID;

    private MediaItem2(String str, DataSourceDesc dataSourceDesc, MediaMetadata2 mediaMetadata2, int i, UUID uuid) {
        if (str == null) {
            throw new IllegalArgumentException("mediaId shouldn't be null");
        }
        if (mediaMetadata2 != null && !TextUtils.equals(str, mediaMetadata2.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaid");
        }
        this.mId = str;
        this.mDataSourceDesc = dataSourceDesc;
        this.mMetadata = mediaMetadata2;
        this.mFlags = i;
        this.mUUID = uuid == null ? UUID.randomUUID() : uuid;
    }

    public static MediaItem2 fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        return fromBundle(bundle, UUID.fromString(bundle.getString("android.media.mediaitem2.uuid")));
    }

    static MediaItem2 fromBundle(Bundle bundle, UUID uuid) {
        if (bundle == null) {
            return null;
        }
        String string = bundle.getString("android.media.mediaitem2.id");
        Bundle bundle2 = bundle.getBundle("android.media.mediaitem2.metadata");
        return new MediaItem2(string, null, bundle2 != null ? MediaMetadata2.fromBundle(bundle2) : null, bundle.getInt("android.media.mediaitem2.flags"), uuid);
    }

    public boolean equals(Object obj) {
        if (obj instanceof MediaItem2) {
            return this.mUUID.equals(((MediaItem2) obj).mUUID);
        }
        return false;
    }

    public int hashCode() {
        return this.mUUID.hashCode();
    }

    public String toString() {
        return "MediaItem2{mFlags=" + this.mFlags + ", mMetadata=" + this.mMetadata + '}';
    }
}
