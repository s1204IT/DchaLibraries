package com.android.server.wm;

import android.os.RemoteException;
import android.util.TimeUtils;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import java.io.PrintWriter;
import java.util.ArrayList;

public class AppWindowAnimator {
    static final String TAG = "AppWindowAnimator";
    static final Animation sDummyAnimation = new DummyAnimation();
    boolean allDrawn;
    int animLayerAdjustment;
    boolean animating;
    Animation animation;
    boolean deferFinalFrameCleanup;
    boolean deferThumbnailDestruction;
    boolean freezingScreen;
    boolean hasTransformation;
    int lastFreezeDuration;
    final WindowAnimator mAnimator;
    final AppWindowToken mAppToken;
    final WindowManagerService mService;
    SurfaceControl thumbnail;
    Animation thumbnailAnimation;
    int thumbnailForceAboveLayer;
    int thumbnailLayer;
    int thumbnailTransactionSeq;
    int thumbnailX;
    int thumbnailY;
    final Transformation transformation = new Transformation();
    final Transformation thumbnailTransformation = new Transformation();
    ArrayList<WindowStateAnimator> mAllAppWinAnimators = new ArrayList<>();

    public AppWindowAnimator(AppWindowToken atoken) {
        this.mAppToken = atoken;
        this.mService = atoken.service;
        this.mAnimator = atoken.mAnimator;
    }

    public void setAnimation(Animation anim, int width, int height) {
        this.animation = anim;
        this.animating = false;
        if (!anim.isInitialized()) {
            anim.initialize(width, height, width, height);
        }
        anim.restrictDuration(10000L);
        anim.scaleCurrentDuration(this.mService.getTransitionAnimationScaleLocked());
        int zorder = anim.getZAdjustment();
        int adj = 0;
        if (zorder == 1) {
            adj = 1000;
        } else if (zorder == -1) {
            adj = -1000;
        }
        if (this.animLayerAdjustment != adj) {
            this.animLayerAdjustment = adj;
            updateLayers();
        }
        this.transformation.clear();
        this.transformation.setAlpha(this.mAppToken.isVisible() ? 1.0f : 0.0f);
        this.hasTransformation = true;
        if (!this.mAppToken.appFullscreen) {
            anim.setBackgroundColor(0);
        }
    }

    public void setDummyAnimation() {
        this.animation = sDummyAnimation;
        this.hasTransformation = true;
        this.transformation.clear();
        this.transformation.setAlpha(this.mAppToken.isVisible() ? 1.0f : 0.0f);
    }

    public void clearAnimation() {
        if (this.animation != null) {
            this.animation = null;
            this.animating = true;
        }
        clearThumbnail();
        if (this.mAppToken.deferClearAllDrawn) {
            this.mAppToken.allDrawn = false;
            this.mAppToken.deferClearAllDrawn = false;
        }
    }

    public void clearThumbnail() {
        if (this.thumbnail != null) {
            this.thumbnail.destroy();
            this.thumbnail = null;
        }
        this.deferThumbnailDestruction = false;
    }

    void updateLayers() {
        int N = this.mAppToken.allAppWindows.size();
        int adj = this.animLayerAdjustment;
        this.thumbnailLayer = -1;
        for (int i = 0; i < N; i++) {
            WindowState w = this.mAppToken.allAppWindows.get(i);
            WindowStateAnimator winAnimator = w.mWinAnimator;
            winAnimator.mAnimLayer = w.mLayer + adj;
            if (winAnimator.mAnimLayer > this.thumbnailLayer) {
                this.thumbnailLayer = winAnimator.mAnimLayer;
            }
            if (w == this.mService.mInputMethodTarget && !this.mService.mInputMethodTargetWaitingAnim) {
                this.mService.setInputMethodAnimLayerAdjustment(adj);
            }
            if (w == this.mService.mWallpaperTarget && this.mService.mLowerWallpaperTarget == null) {
                this.mService.setWallpaperAnimLayerAdjustmentLocked(adj);
            }
        }
    }

    private void stepThumbnailAnimation(long currentTime) {
        this.thumbnailTransformation.clear();
        this.thumbnailAnimation.getTransformation(currentTime, this.thumbnailTransformation);
        this.thumbnailTransformation.getMatrix().preTranslate(this.thumbnailX, this.thumbnailY);
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(0);
        boolean screenAnimation = screenRotationAnimation != null && screenRotationAnimation.isAnimating();
        if (screenAnimation) {
            this.thumbnailTransformation.postCompose(screenRotationAnimation.getEnterTransformation());
        }
        float[] tmpFloats = this.mService.mTmpFloats;
        this.thumbnailTransformation.getMatrix().getValues(tmpFloats);
        this.thumbnail.setPosition(tmpFloats[2], tmpFloats[5]);
        this.thumbnail.setAlpha(this.thumbnailTransformation.getAlpha());
        if (this.thumbnailForceAboveLayer > 0) {
            this.thumbnail.setLayer(this.thumbnailForceAboveLayer + 1);
        } else {
            this.thumbnail.setLayer((this.thumbnailLayer + 5) - 4);
        }
        this.thumbnail.setMatrix(tmpFloats[0], tmpFloats[3], tmpFloats[1], tmpFloats[4]);
    }

    private boolean stepAnimation(long currentTime) {
        if (this.animation == null) {
            return false;
        }
        this.transformation.clear();
        boolean hasMoreFrames = this.animation.getTransformation(currentTime, this.transformation);
        if (!hasMoreFrames) {
            if (this.deferThumbnailDestruction && !this.deferFinalFrameCleanup) {
                this.deferFinalFrameCleanup = true;
                hasMoreFrames = true;
            } else {
                this.deferFinalFrameCleanup = false;
                this.animation = null;
                clearThumbnail();
            }
        }
        this.hasTransformation = hasMoreFrames;
        return hasMoreFrames;
    }

    boolean stepAnimationLocked(long currentTime) {
        if (this.mService.okToDisplay()) {
            if (this.animation == sDummyAnimation) {
                return false;
            }
            if ((this.mAppToken.allDrawn || this.animating || this.mAppToken.startingDisplayed) && this.animation != null) {
                if (!this.animating) {
                    this.animation.setStartTime(currentTime);
                    this.animating = true;
                    if (this.thumbnail != null) {
                        this.thumbnail.show();
                        this.thumbnailAnimation.setStartTime(currentTime);
                    }
                }
                if (stepAnimation(currentTime)) {
                    if (this.thumbnail == null) {
                        return true;
                    }
                    stepThumbnailAnimation(currentTime);
                    return true;
                }
            }
        } else if (this.animation != null) {
            this.animating = true;
            this.animation = null;
        }
        this.hasTransformation = false;
        if (!this.animating && this.animation == null) {
            return false;
        }
        this.mAnimator.setAppLayoutChanges(this, 8, "AppWindowToken");
        clearAnimation();
        this.animating = false;
        if (this.animLayerAdjustment != 0) {
            this.animLayerAdjustment = 0;
            updateLayers();
        }
        if (this.mService.mInputMethodTarget != null && this.mService.mInputMethodTarget.mAppToken == this.mAppToken) {
            this.mService.moveInputMethodWindowsIfNeededLocked(true);
        }
        this.transformation.clear();
        int numAllAppWinAnimators = this.mAllAppWinAnimators.size();
        for (int i = 0; i < numAllAppWinAnimators; i++) {
            this.mAllAppWinAnimators.get(i).finishExit();
        }
        if (this.mAppToken.mLaunchTaskBehind) {
            try {
                this.mService.mActivityManager.notifyLaunchTaskBehindComplete(this.mAppToken.token);
            } catch (RemoteException e) {
            }
            this.mAppToken.mLaunchTaskBehind = false;
        } else {
            this.mAppToken.updateReportedVisibilityLocked();
            if (this.mAppToken.mEnteringAnimation) {
                this.mAppToken.mEnteringAnimation = false;
                try {
                    this.mService.mActivityManager.notifyEnterAnimationComplete(this.mAppToken.token);
                } catch (RemoteException e2) {
                }
            }
        }
        return false;
    }

    boolean showAllWindowsLocked() {
        boolean isAnimating = false;
        int NW = this.mAllAppWinAnimators.size();
        for (int i = 0; i < NW; i++) {
            WindowStateAnimator winAnimator = this.mAllAppWinAnimators.get(i);
            winAnimator.performShowLocked();
            isAnimating |= winAnimator.isAnimating();
        }
        return isAnimating;
    }

    void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        pw.print(prefix);
        pw.print("mAppToken=");
        pw.println(this.mAppToken);
        pw.print(prefix);
        pw.print("mAnimator=");
        pw.println(this.mAnimator);
        pw.print(prefix);
        pw.print("freezingScreen=");
        pw.print(this.freezingScreen);
        pw.print(" allDrawn=");
        pw.print(this.allDrawn);
        pw.print(" animLayerAdjustment=");
        pw.println(this.animLayerAdjustment);
        if (this.lastFreezeDuration != 0) {
            pw.print(prefix);
            pw.print("lastFreezeDuration=");
            TimeUtils.formatDuration(this.lastFreezeDuration, pw);
            pw.println();
        }
        if (this.animating || this.animation != null) {
            pw.print(prefix);
            pw.print("animating=");
            pw.println(this.animating);
            pw.print(prefix);
            pw.print("animation=");
            pw.println(this.animation);
        }
        if (this.hasTransformation) {
            pw.print(prefix);
            pw.print("XForm: ");
            this.transformation.printShortString(pw);
            pw.println();
        }
        if (this.thumbnail != null) {
            pw.print(prefix);
            pw.print("thumbnail=");
            pw.print(this.thumbnail);
            pw.print(" x=");
            pw.print(this.thumbnailX);
            pw.print(" y=");
            pw.print(this.thumbnailY);
            pw.print(" layer=");
            pw.println(this.thumbnailLayer);
            pw.print(prefix);
            pw.print("thumbnailAnimation=");
            pw.println(this.thumbnailAnimation);
            pw.print(prefix);
            pw.print("thumbnailTransformation=");
            pw.println(this.thumbnailTransformation.toShortString());
        }
        for (int i = 0; i < this.mAllAppWinAnimators.size(); i++) {
            WindowStateAnimator wanim = this.mAllAppWinAnimators.get(i);
            pw.print(prefix);
            pw.print("App Win Anim #");
            pw.print(i);
            pw.print(": ");
            pw.println(wanim);
        }
    }

    static final class DummyAnimation extends Animation {
        DummyAnimation() {
        }

        @Override
        public boolean getTransformation(long currentTime, Transformation outTransformation) {
            return false;
        }
    }
}
