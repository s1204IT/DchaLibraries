package com.mediatek.gallerybasic.base;

import android.content.Context;
import android.content.res.Resources;
import com.mediatek.gallerybasic.gl.GLIdleExecuter;

public class MediaMember {
    protected Context mContext;
    protected GLIdleExecuter mGLExecuter;
    protected MediaCenter mMediaCenter;
    protected int mPriority = Integer.MIN_VALUE;
    protected Resources mResources;
    private int mType;

    public MediaMember(Context context, GLIdleExecuter gLIdleExecuter, Resources resources) {
        this.mContext = context;
        this.mGLExecuter = gLIdleExecuter;
        this.mResources = resources;
    }

    public MediaMember(Context context) {
        this.mContext = context;
    }

    public boolean isMatching(MediaData mediaData) {
        return true;
    }

    public Player getPlayer(MediaData mediaData, ThumbType thumbType) {
        return null;
    }

    public Generator getGenerator() {
        return null;
    }

    public Layer getLayer() {
        return null;
    }

    public ExtItem getItem(MediaData mediaData) {
        return new ExtItem(this.mContext, mediaData);
    }

    public final void setType(int i) {
        this.mType = i;
        onTypeObtained(this.mType);
    }

    public final int getType() {
        return this.mType;
    }

    public int getPriority() {
        return this.mPriority;
    }

    public boolean isShelled() {
        return false;
    }

    protected void onTypeObtained(int i) {
    }

    public void setMediaCenter(MediaCenter mediaCenter) {
        this.mMediaCenter = mediaCenter;
    }
}
