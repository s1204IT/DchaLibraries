package com.android.gallery3d.filtershow.crop;

import android.net.Uri;

public class CropExtras {
    private int mAspectX;
    private int mAspectY;
    private Uri mExtraOutput;
    private String mOutputFormat;
    private int mOutputX;
    private int mOutputY;
    private boolean mReturnData;
    private boolean mReturnDataCompress;
    private boolean mScaleUp;
    private boolean mSetAsWallpaper;
    private boolean mShowWhenLocked;
    private float mSpotlightX;
    private float mSpotlightY;

    public CropExtras(int i, int i2, boolean z, int i3, int i4, boolean z2, boolean z3, Uri uri, String str, boolean z4, float f, float f2) {
        this.mOutputX = 0;
        this.mOutputY = 0;
        this.mScaleUp = true;
        this.mAspectX = 0;
        this.mAspectY = 0;
        this.mSetAsWallpaper = false;
        this.mReturnData = false;
        this.mExtraOutput = null;
        this.mOutputFormat = null;
        this.mShowWhenLocked = false;
        this.mSpotlightX = 0.0f;
        this.mSpotlightY = 0.0f;
        this.mReturnDataCompress = false;
        this.mOutputX = i;
        this.mOutputY = i2;
        this.mScaleUp = z;
        this.mAspectX = i3;
        this.mAspectY = i4;
        this.mSetAsWallpaper = z2;
        this.mReturnData = z3;
        this.mExtraOutput = uri;
        this.mOutputFormat = str;
        this.mShowWhenLocked = z4;
        this.mSpotlightX = f;
        this.mSpotlightY = f2;
    }

    public int getOutputX() {
        return this.mOutputX;
    }

    public int getOutputY() {
        return this.mOutputY;
    }

    public boolean getScaleUp() {
        return this.mScaleUp;
    }

    public int getAspectX() {
        return this.mAspectX;
    }

    public int getAspectY() {
        return this.mAspectY;
    }

    public boolean getSetAsWallpaper() {
        return this.mSetAsWallpaper;
    }

    public boolean getReturnData() {
        return this.mReturnData;
    }

    public Uri getExtraOutput() {
        return this.mExtraOutput;
    }

    public String getOutputFormat() {
        return this.mOutputFormat;
    }

    public boolean getShowWhenLocked() {
        return this.mShowWhenLocked;
    }

    public float getSpotlightX() {
        return this.mSpotlightX;
    }

    public float getSpotlightY() {
        return this.mSpotlightY;
    }

    public CropExtras(int i, int i2, boolean z, int i3, int i4, boolean z2, boolean z3, Uri uri, String str, boolean z4, float f, float f2, boolean z5) {
        this(i, i2, z, i3, i4, z2, z3, uri, str, z4, f, f2);
        this.mReturnDataCompress = z5;
    }

    public boolean getReturnDataCompressed() {
        return this.mReturnDataCompress;
    }
}
