package com.android.launcher3.util;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.Interpolator;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.Interpolators;

/* loaded from: classes.dex */
public class WallpaperOffsetInterpolator extends BroadcastReceiver {
    private static final int ANIMATION_DURATION = 250;
    private static final int MIN_PARALLAX_PAGE_SPAN = 4;
    private static final int MSG_APPLY_OFFSET = 3;
    private static final int MSG_JUMP_TO_FINAL = 5;
    private static final int MSG_SET_NUM_PARALLAX = 4;
    private static final int MSG_START_ANIMATION = 1;
    private static final int MSG_UPDATE_OFFSET = 2;
    private static final String TAG = "WPOffsetInterpolator";
    private static final int[] sTempInt = new int[2];
    private final Handler mHandler;
    private final boolean mIsRtl;
    private boolean mLockedToDefaultPage;
    private int mNumScreens;
    private boolean mRegistered = false;
    private boolean mWallpaperIsLiveWallpaper;
    private IBinder mWindowToken;
    private final Workspace mWorkspace;

    public WallpaperOffsetInterpolator(Workspace workspace) {
        this.mWorkspace = workspace;
        this.mIsRtl = Utilities.isRtl(workspace.getResources());
        this.mHandler = new OffsetHandler(workspace.getContext());
    }

    public void setLockToDefaultPage(boolean z) {
        this.mLockedToDefaultPage = z;
    }

    public boolean isLockedToDefaultPage() {
        return this.mLockedToDefaultPage;
    }

    private void wallpaperOffsetForScroll(int i, int i2, int[] iArr) {
        int iMax;
        int i3;
        int i4;
        int i5;
        iArr[1] = 1;
        if (this.mLockedToDefaultPage || i2 <= 1) {
            iArr[0] = this.mIsRtl ? 1 : 0;
            return;
        }
        if (!this.mWallpaperIsLiveWallpaper) {
            iMax = Math.max(4, i2);
        } else {
            iMax = i2;
        }
        if (this.mIsRtl) {
            i4 = (0 + i2) - 1;
            i3 = 0;
        } else {
            i3 = (0 + i2) - 1;
            i4 = 0;
        }
        int scrollForPage = this.mWorkspace.getScrollForPage(i4);
        int scrollForPage2 = this.mWorkspace.getScrollForPage(i3) - scrollForPage;
        if (scrollForPage2 <= 0) {
            iArr[0] = 0;
            return;
        }
        int iBoundToRange = Utilities.boundToRange((i - scrollForPage) - this.mWorkspace.getLayoutTransitionOffsetForPage(0), 0, scrollForPage2);
        iArr[1] = (iMax - 1) * scrollForPage2;
        if (this.mIsRtl) {
            i5 = iArr[1] - ((i2 - 1) * scrollForPage2);
        } else {
            i5 = 0;
        }
        iArr[0] = i5 + (iBoundToRange * (i2 - 1));
    }

    public float wallpaperOffsetForScroll(int i) {
        wallpaperOffsetForScroll(i, getNumScreensExcludingEmpty(), sTempInt);
        return sTempInt[0] / sTempInt[1];
    }

    private int getNumScreensExcludingEmpty() {
        int childCount = this.mWorkspace.getChildCount();
        if (childCount >= 4 && this.mWorkspace.hasExtraEmptyScreen()) {
            return childCount - 1;
        }
        return childCount;
    }

    public void syncWithScroll() {
        int numScreensExcludingEmpty = getNumScreensExcludingEmpty();
        wallpaperOffsetForScroll(this.mWorkspace.getScrollX(), numScreensExcludingEmpty, sTempInt);
        Message messageObtain = Message.obtain(this.mHandler, 2, sTempInt[0], sTempInt[1], this.mWindowToken);
        if (numScreensExcludingEmpty != this.mNumScreens) {
            if (this.mNumScreens > 0) {
                messageObtain.what = 1;
            }
            this.mNumScreens = numScreensExcludingEmpty;
            updateOffset();
        }
        messageObtain.sendToTarget();
    }

    private void updateOffset() {
        int iMax;
        if (this.mWallpaperIsLiveWallpaper) {
            iMax = this.mNumScreens;
        } else {
            iMax = Math.max(4, this.mNumScreens);
        }
        Message.obtain(this.mHandler, 4, iMax, 0, this.mWindowToken).sendToTarget();
    }

    public void jumpToFinal() {
        Message.obtain(this.mHandler, 5, this.mWindowToken).sendToTarget();
    }

    public void setWindowToken(IBinder iBinder) {
        this.mWindowToken = iBinder;
        if (this.mWindowToken == null && this.mRegistered) {
            this.mWorkspace.getContext().unregisterReceiver(this);
            this.mRegistered = false;
        } else if (this.mWindowToken != null && !this.mRegistered) {
            this.mWorkspace.getContext().registerReceiver(this, new IntentFilter("android.intent.action.WALLPAPER_CHANGED"));
            onReceive(this.mWorkspace.getContext(), null);
            this.mRegistered = true;
        }
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        this.mWallpaperIsLiveWallpaper = WallpaperManager.getInstance(this.mWorkspace.getContext()).getWallpaperInfo() != null;
        updateOffset();
    }

    private static class OffsetHandler extends Handler {
        private boolean mAnimating;
        private float mAnimationStartOffset;
        private long mAnimationStartTime;
        private float mCurrentOffset;
        private float mFinalOffset;
        private final Interpolator mInterpolator;
        private float mOffsetX;
        private final WallpaperManager mWM;

        public OffsetHandler(Context context) {
            super(UiThreadHelper.getBackgroundLooper());
            this.mCurrentOffset = 0.5f;
            this.mInterpolator = Interpolators.DEACCEL_1_5;
            this.mWM = WallpaperManager.getInstance(context);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            IBinder iBinder = (IBinder) message.obj;
            if (iBinder == null) {
            }
            switch (message.what) {
                case 1:
                    this.mAnimating = true;
                    this.mAnimationStartOffset = this.mCurrentOffset;
                    this.mAnimationStartTime = message.getWhen();
                case 2:
                    this.mFinalOffset = message.arg1 / message.arg2;
                case 3:
                    float f = this.mCurrentOffset;
                    if (this.mAnimating) {
                        long jUptimeMillis = SystemClock.uptimeMillis() - this.mAnimationStartTime;
                        this.mCurrentOffset = this.mAnimationStartOffset + ((this.mFinalOffset - this.mAnimationStartOffset) * this.mInterpolator.getInterpolation(jUptimeMillis / 250.0f));
                        this.mAnimating = jUptimeMillis < 250;
                    } else {
                        this.mCurrentOffset = this.mFinalOffset;
                    }
                    if (Float.compare(this.mCurrentOffset, f) != 0) {
                        setOffsetSafely(iBinder);
                        this.mWM.setWallpaperOffsetSteps(this.mOffsetX, 1.0f);
                    }
                    if (this.mAnimating) {
                        Message.obtain(this, 3, iBinder).sendToTarget();
                        break;
                    }
                    break;
                case 4:
                    this.mOffsetX = 1.0f / (message.arg1 - 1);
                    this.mWM.setWallpaperOffsetSteps(this.mOffsetX, 1.0f);
                    break;
                case 5:
                    if (Float.compare(this.mCurrentOffset, this.mFinalOffset) != 0) {
                        this.mCurrentOffset = this.mFinalOffset;
                        setOffsetSafely(iBinder);
                    }
                    this.mAnimating = false;
                    break;
            }
        }

        private void setOffsetSafely(IBinder iBinder) {
            try {
                this.mWM.setWallpaperOffsets(iBinder, this.mCurrentOffset, 0.5f);
            } catch (IllegalArgumentException e) {
                Log.e(WallpaperOffsetInterpolator.TAG, "Error updating wallpaper offset: " + e);
            }
        }
    }
}
