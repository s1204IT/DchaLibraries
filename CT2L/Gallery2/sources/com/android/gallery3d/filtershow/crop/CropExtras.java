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
    private boolean mScaleUp;
    private boolean mSetAsWallpaper;
    private boolean mShowWhenLocked;
    private float mSpotlightX;
    private float mSpotlightY;

    public CropExtras(int outputX, int outputY, boolean scaleUp, int aspectX, int aspectY, boolean setAsWallpaper, boolean returnData, Uri extraOutput, String outputFormat, boolean showWhenLocked, float spotlightX, float spotlightY) {
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
        this.mOutputX = outputX;
        this.mOutputY = outputY;
        this.mScaleUp = scaleUp;
        this.mAspectX = aspectX;
        this.mAspectY = aspectY;
        this.mSetAsWallpaper = setAsWallpaper;
        this.mReturnData = returnData;
        this.mExtraOutput = extraOutput;
        this.mOutputFormat = outputFormat;
        this.mShowWhenLocked = showWhenLocked;
        this.mSpotlightX = spotlightX;
        this.mSpotlightY = spotlightY;
    }

    public int getOutputX() {
        return this.mOutputX;
    }

    public int getOutputY() {
        return this.mOutputY;
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
}
