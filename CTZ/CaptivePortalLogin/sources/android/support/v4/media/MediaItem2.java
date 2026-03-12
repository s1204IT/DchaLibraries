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

    private MediaItem2(String mediaId, DataSourceDesc dsd, MediaMetadata2 metadata, int flags, UUID uuid) {
        if (mediaId == null) {
            throw new IllegalArgumentException("mediaId shouldn't be null");
        }
        if (metadata != null && !TextUtils.equals(mediaId, metadata.getMediaId())) {
            throw new IllegalArgumentException("metadata's id should be matched with the mediaid");
        }
        this.mId = mediaId;
        this.mDataSourceDesc = dsd;
        this.mMetadata = metadata;
        this.mFlags = flags;
        this.mUUID = uuid == null ? UUID.randomUUID() : uuid;
    }

    public static MediaItem2 fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        String uuidString = bundle.getString("android.media.mediaitem2.uuid");
        return fromBundle(bundle, UUID.fromString(uuidString));
    }

    static MediaItem2 fromBundle(Bundle bundle, UUID uuid) {
        MediaMetadata2 mediaMetadata2FromBundle = null;
        if (bundle == null) {
            return null;
        }
        String id = bundle.getString("android.media.mediaitem2.id");
        Bundle metadataBundle = bundle.getBundle("android.media.mediaitem2.metadata");
        if (metadataBundle != null) {
            mediaMetadata2FromBundle = MediaMetadata2.fromBundle(metadataBundle);
        }
        MediaMetadata2 metadata = mediaMetadata2FromBundle;
        int flags = bundle.getInt("android.media.mediaitem2.flags");
        return new MediaItem2(id, null, metadata, flags, uuid);
    }

    public String toString() {
        return "MediaItem2{mFlags=" + this.mFlags + ", mMetadata=" + this.mMetadata + '}';
    }

    public int hashCode() {
        return this.mUUID.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MediaItem2)) {
            return false;
        }
        MediaItem2 other = (MediaItem2) obj;
        return this.mUUID.equals(other.mUUID);
    }
}
