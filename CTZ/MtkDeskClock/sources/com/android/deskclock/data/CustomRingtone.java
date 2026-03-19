package com.android.deskclock.data;

import android.net.Uri;
import android.support.annotation.NonNull;

public final class CustomRingtone implements Comparable<CustomRingtone> {
    private final boolean mHasPermissions;
    private final long mId;
    private final String mTitle;
    private final Uri mUri;

    CustomRingtone(long j, Uri uri, String str, boolean z) {
        this.mId = j;
        this.mUri = uri;
        this.mTitle = str;
        this.mHasPermissions = z;
    }

    public long getId() {
        return this.mId;
    }

    public Uri getUri() {
        return this.mUri;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public boolean hasPermissions() {
        return this.mHasPermissions;
    }

    CustomRingtone setHasPermissions(boolean z) {
        if (this.mHasPermissions == z) {
            return this;
        }
        return new CustomRingtone(this.mId, this.mUri, this.mTitle, z);
    }

    @Override
    public int compareTo(@NonNull CustomRingtone customRingtone) {
        return String.CASE_INSENSITIVE_ORDER.compare(getTitle(), customRingtone.getTitle());
    }
}
