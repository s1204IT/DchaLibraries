package com.android.bluetooth.opp;

import android.net.Uri;

public class BluetoothOppShareInfo {
    public int mConfirm;
    public long mCurrentBytes;
    public String mDestination;
    public int mDirection;
    public String mFilename;
    public String mHint;
    public int mId;
    public boolean mMediaScanned;
    public String mMimetype;
    public int mStatus;
    public long mTimestamp;
    public long mTotalBytes;
    public Uri mUri;
    public int mVisibility;

    public BluetoothOppShareInfo(int i, Uri uri, String str, String str2, String str3, int i2, String str4, int i3, int i4, int i5, long j, long j2, long j3, boolean z) {
        this.mId = i;
        this.mUri = uri;
        this.mHint = str;
        this.mFilename = str2;
        this.mMimetype = str3;
        this.mDirection = i2;
        this.mDestination = str4;
        this.mVisibility = i3;
        this.mConfirm = i4;
        this.mStatus = i5;
        this.mTotalBytes = j;
        this.mCurrentBytes = j2;
        this.mTimestamp = j3;
        this.mMediaScanned = z;
    }

    public boolean isReadyToStart() {
        return this.mDirection == 0 ? this.mStatus == 190 && this.mUri != null : this.mDirection == 1 && this.mStatus == 190;
    }

    public boolean hasCompletionNotification() {
        return BluetoothShare.isStatusCompleted(this.mStatus) && this.mVisibility == 0;
    }

    public boolean isObsolete() {
        if (192 == this.mStatus) {
            return true;
        }
        return false;
    }
}
