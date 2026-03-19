package com.android.server.wm;

import android.os.Trace;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.Choreographer;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.ArrayList;

public class AppWindowAnimator {
    static final int PROLONG_ANIMATION_AT_END = 1;
    static final int PROLONG_ANIMATION_AT_START = 2;
    private static final int PROLONG_ANIMATION_DISABLED = 0;
    static final String TAG = "WindowManager";
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
    private boolean mClearProlongedAnimation;
    private int mProlongAnimation;
    final WindowManagerService mService;
    SurfaceControl thumbnail;
    Animation thumbnailAnimation;
    int thumbnailForceAboveLayer;
    int thumbnailLayer;
    int thumbnailTransactionSeq;
    boolean wasAnimating;
    final Transformation transformation = new Transformation();
    final Transformation thumbnailTransformation = new Transformation();
    ArrayList<WindowStateAnimator> mAllAppWinAnimators = new ArrayList<>();
    boolean usingTransferredAnimation = false;
    private boolean mSkipFirstFrame = false;
    private int mStackClip = 1;

    public AppWindowAnimator(AppWindowToken atoken) {
        this.mAppToken = atoken;
        this.mService = atoken.service;
        this.mAnimator = this.mService.mAnimator;
    }

    public void setAnimation(Animation anim, int width, int height, boolean skipFirstFrame, int stackClip) {
        if (WindowManagerService.localLOGV) {
            Slog.v(TAG, "Setting animation in " + this.mAppToken + ": " + anim + " wxh=" + width + "x" + height + " isVisible=" + this.mAppToken.isVisible());
        }
        this.animation = anim;
        this.animating = false;
        if (!anim.isInitialized()) {
            anim.initialize(width, height, width, height);
        }
        anim.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        anim.scaleCurrentDuration(this.mService.getTransitionAnimationScaleLocked());
        int zorder = anim.getZAdjustment();
        int adj = 0;
        if (zorder == 1) {
            adj = 1000;
        } else if (zorder == -1) {
            adj = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        }
        if (this.animLayerAdjustment != adj) {
            this.animLayerAdjustment = adj;
            updateLayers();
        }
        this.transformation.clear();
        this.transformation.setAlpha(this.mAppToken.isVisible() ? 1 : 0);
        this.hasTransformation = true;
        this.mStackClip = stackClip;
        this.mSkipFirstFrame = skipFirstFrame;
        if (!this.mAppToken.appFullscreen) {
            anim.setBackgroundColor(0);
        }
        if (this.mClearProlongedAnimation) {
            this.mProlongAnimation = 0;
        } else {
            this.mClearProlongedAnimation = true;
        }
        for (int i = this.mAppToken.allAppWindows.size() - 1; i >= 0; i--) {
            this.mAppToken.allAppWindows.get(i).resetJustMovedInStack();
        }
    }

    public void setDummyAnimation() {
        if (WindowManagerService.localLOGV) {
            Slog.v(TAG, "Setting dummy animation in " + this.mAppToken + " isVisible=" + this.mAppToken.isVisible());
        }
        this.animation = sDummyAnimation;
        this.hasTransformation = true;
        this.transformation.clear();
        this.transformation.setAlpha(this.mAppToken.isVisible() ? 1 : 0);
    }

    void setNullAnimation() {
        this.animation = null;
        this.usingTransferredAnimation = false;
    }

    public void clearAnimation() {
        if (this.animation != null) {
            this.animating = true;
        }
        clearThumbnail();
        setNullAnimation();
        if (this.mAppToken.deferClearAllDrawn) {
            this.mAppToken.clearAllDrawn();
        }
        this.mStackClip = 1;
    }

    public boolean isAnimating() {
        if (this.animation == null) {
            return this.mAppToken.inPendingTransaction;
        }
        return true;
    }

    public void clearThumbnail() {
        if (this.thumbnail != null) {
            this.thumbnail.hide();
            this.mService.mWindowPlacerLocked.destroyAfterTransaction(this.thumbnail);
            this.thumbnail = null;
        }
        this.deferThumbnailDestruction = false;
    }

    int getStackClip() {
        return this.mStackClip;
    }

    void transferCurrentAnimation(AppWindowAnimator toAppAnimator, WindowStateAnimator transferWinAnimator) {
        if (this.animation != null) {
            toAppAnimator.animation = this.animation;
            toAppAnimator.animating = this.animating;
            toAppAnimator.animLayerAdjustment = this.animLayerAdjustment;
            setNullAnimation();
            this.animLayerAdjustment = 0;
            toAppAnimator.updateLayers();
            updateLayers();
            toAppAnimator.usingTransferredAnimation = true;
        }
        if (transferWinAnimator == null) {
            return;
        }
        this.mAllAppWinAnimators.remove(transferWinAnimator);
        toAppAnimator.mAllAppWinAnimators.add(transferWinAnimator);
        toAppAnimator.hasTransformation = transferWinAnimator.mAppAnimator.hasTransformation;
        if (toAppAnimator.hasTransformation) {
            toAppAnimator.transformation.set(transferWinAnimator.mAppAnimator.transformation);
        } else {
            toAppAnimator.transformation.clear();
        }
        transferWinAnimator.mAppAnimator = toAppAnimator;
    }

    void updateLayers() {
        int windowCount = this.mAppToken.allAppWindows.size();
        int adj = this.animLayerAdjustment;
        this.thumbnailLayer = -1;
        WallpaperController wallpaperController = this.mService.mWallpaperControllerLocked;
        for (int i = 0; i < windowCount; i++) {
            WindowState w = this.mAppToken.allAppWindows.get(i);
            WindowStateAnimator winAnimator = w.mWinAnimator;
            winAnimator.mAnimLayer = w.mLayer + adj;
            if (winAnimator.mAnimLayer > this.thumbnailLayer) {
                this.thumbnailLayer = winAnimator.mAnimLayer;
            }
            if (WindowManagerDebugConfig.DEBUG_LAYERS) {
                Slog.v(TAG, "Updating layer " + w + ": " + winAnimator.mAnimLayer);
            }
            if (w == this.mService.mInputMethodTarget && !this.mService.mInputMethodTargetWaitingAnim) {
                this.mService.mLayersController.setInputMethodAnimLayerAdjustment(adj);
            }
            wallpaperController.setAnimLayerAdjustment(w, adj);
        }
    }

    private void stepThumbnailAnimation(long currentTime) {
        this.thumbnailTransformation.clear();
        long animationFrameTime = getAnimationFrameTime(this.thumbnailAnimation, currentTime);
        this.thumbnailAnimation.getTransformation(animationFrameTime, this.thumbnailTransformation);
        ScreenRotationAnimation screenRotationAnimation = this.mAnimator.getScreenRotationAnimationLocked(0);
        boolean screenAnimation = screenRotationAnimation != null ? screenRotationAnimation.isAnimating() : false;
        if (screenAnimation) {
            this.thumbnailTransformation.postCompose(screenRotationAnimation.getEnterTransformation());
        }
        float[] tmpFloats = this.mService.mTmpFloats;
        this.thumbnailTransformation.getMatrix().getValues(tmpFloats);
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            WindowManagerService.logSurface(this.thumbnail, "thumbnail", "POS " + tmpFloats[2] + ", " + tmpFloats[5]);
        }
        this.thumbnail.setPosition(tmpFloats[2], tmpFloats[5]);
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            WindowManagerService.logSurface(this.thumbnail, "thumbnail", "alpha=" + this.thumbnailTransformation.getAlpha() + " layer=" + this.thumbnailLayer + " matrix=[" + tmpFloats[0] + "," + tmpFloats[3] + "][" + tmpFloats[1] + "," + tmpFloats[4] + "]");
        }
        this.thumbnail.setAlpha(this.thumbnailTransformation.getAlpha());
        if (this.thumbnailForceAboveLayer > 0) {
            this.thumbnail.setLayer(this.thumbnailForceAboveLayer + 1);
        } else {
            this.thumbnail.setLayer((this.thumbnailLayer + 5) - 4);
        }
        this.thumbnail.setMatrix(tmpFloats[0], tmpFloats[3], tmpFloats[1], tmpFloats[4]);
        this.thumbnail.setWindowCrop(this.thumbnailTransformation.getClipRect());
    }

    private long getAnimationFrameTime(Animation animation, long currentTime) {
        if (this.mProlongAnimation == 2) {
            animation.setStartTime(currentTime);
            return 1 + currentTime;
        }
        return currentTime;
    }

    private boolean stepAnimation(long currentTime) {
        if (this.animation == null) {
            return false;
        }
        this.transformation.clear();
        long animationFrameTime = getAnimationFrameTime(this.animation, currentTime);
        boolean hasMoreFrames = this.animation.getTransformation(animationFrameTime, this.transformation);
        if (!hasMoreFrames) {
            if (this.deferThumbnailDestruction && !this.deferFinalFrameCleanup) {
                this.deferFinalFrameCleanup = true;
                hasMoreFrames = true;
            } else {
                this.deferFinalFrameCleanup = false;
                if (this.mProlongAnimation == 1) {
                    hasMoreFrames = true;
                } else {
                    setNullAnimation();
                    clearThumbnail();
                    if (WindowManagerDebugConfig.DEBUG_ANIM) {
                        Slog.v(TAG, "Finished animation in " + this.mAppToken + " @ " + currentTime);
                    }
                }
            }
        }
        this.hasTransformation = hasMoreFrames;
        return hasMoreFrames;
    }

    private long getStartTimeCorrection() {
        if (this.mSkipFirstFrame) {
            return (-Choreographer.getInstance().getFrameIntervalNanos()) / 1000000;
        }
        return 0L;
    }

    boolean stepAnimationLocked(long currentTime, int displayId) {
        if (this.mService.okToDisplay()) {
            if (this.animation == sDummyAnimation) {
                return false;
            }
            if ((this.mAppToken.allDrawn || this.animating || this.mAppToken.startingDisplayed) && this.animation != null) {
                if (!this.animating) {
                    if (WindowManagerDebugConfig.DEBUG_ANIM) {
                        Slog.v(TAG, "Starting animation in " + this.mAppToken + " @ " + currentTime + " scale=" + this.mService.getTransitionAnimationScaleLocked() + " allDrawn=" + this.mAppToken.allDrawn + " animating=" + this.animating);
                    }
                    long correction = getStartTimeCorrection();
                    this.animation.setStartTime(currentTime + correction);
                    this.animating = true;
                    if (this.thumbnail != null) {
                        this.thumbnail.show();
                        this.thumbnailAnimation.setStartTime(currentTime + correction);
                    }
                    this.mSkipFirstFrame = false;
                }
                if (stepAnimation(currentTime)) {
                    if (this.thumbnail != null) {
                        stepThumbnailAnimation(currentTime);
                    }
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
        this.mAnimator.setAppLayoutChanges(this, 8, "AppWindowToken", displayId);
        clearAnimation();
        this.animating = false;
        if (this.animLayerAdjustment != 0) {
            this.animLayerAdjustment = 0;
            updateLayers();
        }
        if (this.mService.mInputMethodTarget != null && this.mService.mInputMethodTarget.mAppToken == this.mAppToken) {
            this.mService.moveInputMethodWindowsIfNeededLocked(true);
        }
        Trace.traceBegin(4128L, "app animation done : " + this.mAppToken.toString());
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            Slog.v(TAG, "Animation done in " + this.mAppToken + ": reportedVisible=" + this.mAppToken.reportedVisible);
        }
        Trace.traceEnd(4128L);
        this.transformation.clear();
        int numAllAppWinAnimators = this.mAllAppWinAnimators.size();
        for (int i = 0; i < numAllAppWinAnimators; i++) {
            this.mAllAppWinAnimators.get(i).finishExit();
        }
        this.mService.mAppTransition.notifyAppTransitionFinishedLocked(this.mAppToken.token);
        if (this.mAppToken != null && this.mAppToken.startingWindow != null) {
            this.mService.cacheStartingWindow(this.mAppToken);
        }
        return false;
    }

    boolean showAllWindowsLocked() {
        boolean isAnimating = false;
        int NW = this.mAllAppWinAnimators.size();
        for (int i = 0; i < NW; i++) {
            WindowStateAnimator winAnimator = this.mAllAppWinAnimators.get(i);
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG, "performing show on: " + winAnimator);
            }
            winAnimator.performShowLocked();
            isAnimating |= winAnimator.isAnimationSet();
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

    void startProlongAnimation(int prolongType) {
        this.mProlongAnimation = prolongType;
        this.mClearProlongedAnimation = false;
    }

    void endProlongedAnimation() {
        this.mProlongAnimation = 0;
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
