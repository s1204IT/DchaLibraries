package com.android.packageinstaller.permission.model;

public final class Permission {
    private final String mAppOp;
    private boolean mAppOpAllowed;
    private int mFlags;
    private boolean mGranted;
    private boolean mIsEphemeral;
    private boolean mIsRuntimeOnly;
    private final String mName;

    public Permission(String str, boolean z, String str2, boolean z2, int i, int i2) {
        this.mName = str;
        this.mGranted = z;
        this.mAppOp = str2;
        this.mAppOpAllowed = z2;
        this.mFlags = i;
        this.mIsEphemeral = (i2 & 4096) != 0;
        this.mIsRuntimeOnly = (i2 & 8192) != 0;
    }

    public String getName() {
        return this.mName;
    }

    public String getAppOp() {
        return this.mAppOp;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public boolean hasAppOp() {
        return this.mAppOp != null;
    }

    public boolean isGranted() {
        return this.mGranted;
    }

    public boolean isReviewRequired() {
        return (this.mFlags & 64) != 0;
    }

    public void resetReviewRequired() {
        this.mFlags &= -65;
    }

    public void setGranted(boolean z) {
        this.mGranted = z;
    }

    public boolean isAppOpAllowed() {
        return this.mAppOpAllowed;
    }

    public boolean isUserFixed() {
        return (this.mFlags & 2) != 0;
    }

    public void setUserFixed(boolean z) {
        if (z) {
            this.mFlags |= 2;
        } else {
            this.mFlags &= -3;
        }
    }

    public boolean isSystemFixed() {
        return (this.mFlags & 16) != 0;
    }

    public boolean isPolicyFixed() {
        return (this.mFlags & 4) != 0;
    }

    public boolean isUserSet() {
        return (this.mFlags & 1) != 0;
    }

    public boolean isGrantedByDefault() {
        return (this.mFlags & 32) != 0;
    }

    public void setUserSet(boolean z) {
        if (z) {
            this.mFlags |= 1;
        } else {
            this.mFlags &= -2;
        }
    }

    public void setPolicyFixed(boolean z) {
        if (z) {
            this.mFlags |= 4;
        } else {
            this.mFlags &= -5;
        }
    }

    public boolean shouldRevokeOnUpgrade() {
        return (this.mFlags & 8) != 0;
    }

    public void setRevokeOnUpgrade(boolean z) {
        if (z) {
            this.mFlags |= 8;
        } else {
            this.mFlags &= -9;
        }
    }

    public void setAppOpAllowed(boolean z) {
        this.mAppOpAllowed = z;
    }

    public boolean isEphemeral() {
        return this.mIsEphemeral;
    }

    public boolean isRuntimeOnly() {
        return this.mIsRuntimeOnly;
    }

    public boolean isGrantingAllowed(boolean z, boolean z2) {
        return (!z || isEphemeral()) && (z2 || !isRuntimeOnly());
    }
}
