package com.android.server.wm;

import android.R;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;

class ScreenRotationAnimation {
    static final boolean DEBUG_STATE = false;
    static final boolean DEBUG_TRANSFORMS = false;
    static final int FREEZE_LAYER = 2000000;
    static final String TAG = "ScreenRotationAnimation";
    static final boolean TWO_PHASE_ANIMATION = false;
    static final boolean USE_CUSTOM_BLACK_FRAME = false;
    static final int mHwrotation = SystemProperties.getInt("ro.sf.hwrotation", 0);
    boolean mAnimRunning;
    final Context mContext;
    int mCurRotation;
    BlackFrame mCustomBlackFrame;
    final DisplayContent mDisplayContent;
    BlackFrame mEnteringBlackFrame;
    BlackFrame mExitingBlackFrame;
    boolean mFinishAnimReady;
    long mFinishAnimStartTime;
    Animation mFinishEnterAnimation;
    Animation mFinishExitAnimation;
    Animation mFinishFrameAnimation;
    boolean mForceDefaultOrientation;
    long mHalfwayPoint;
    int mHeight;
    Animation mLastRotateEnterAnimation;
    Animation mLastRotateExitAnimation;
    Animation mLastRotateFrameAnimation;
    private boolean mMoreFinishEnter;
    private boolean mMoreFinishExit;
    private boolean mMoreFinishFrame;
    private boolean mMoreRotateEnter;
    private boolean mMoreRotateExit;
    private boolean mMoreRotateFrame;
    private boolean mMoreStartEnter;
    private boolean mMoreStartExit;
    private boolean mMoreStartFrame;
    int mOriginalHeight;
    int mOriginalRotation;
    int mOriginalWidth;
    Animation mRotateEnterAnimation;
    Animation mRotateExitAnimation;
    Animation mRotateFrameAnimation;
    Animation mStartEnterAnimation;
    Animation mStartExitAnimation;
    Animation mStartFrameAnimation;
    boolean mStarted;
    SurfaceControl mSurfaceControl;
    int mWidth;
    Rect mOriginalDisplayRect = new Rect();
    Rect mCurrentDisplayRect = new Rect();
    final Transformation mStartExitTransformation = new Transformation();
    final Transformation mStartEnterTransformation = new Transformation();
    final Transformation mStartFrameTransformation = new Transformation();
    final Transformation mFinishExitTransformation = new Transformation();
    final Transformation mFinishEnterTransformation = new Transformation();
    final Transformation mFinishFrameTransformation = new Transformation();
    final Transformation mRotateExitTransformation = new Transformation();
    final Transformation mRotateEnterTransformation = new Transformation();
    final Transformation mRotateFrameTransformation = new Transformation();
    final Transformation mLastRotateExitTransformation = new Transformation();
    final Transformation mLastRotateEnterTransformation = new Transformation();
    final Transformation mLastRotateFrameTransformation = new Transformation();
    final Transformation mExitTransformation = new Transformation();
    final Transformation mEnterTransformation = new Transformation();
    final Transformation mFrameTransformation = new Transformation();
    final Matrix mFrameInitialMatrix = new Matrix();
    final Matrix mSnapshotInitialMatrix = new Matrix();
    final Matrix mSnapshotFinalMatrix = new Matrix();
    final Matrix mExitFrameFinalMatrix = new Matrix();
    final Matrix mTmpMatrix = new Matrix();
    final float[] mTmpFloats = new float[9];

    public void printTo(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mSurface=");
        pw.print(this.mSurfaceControl);
        pw.print(" mWidth=");
        pw.print(this.mWidth);
        pw.print(" mHeight=");
        pw.println(this.mHeight);
        pw.print(prefix);
        pw.print("mExitingBlackFrame=");
        pw.println(this.mExitingBlackFrame);
        if (this.mExitingBlackFrame != null) {
            this.mExitingBlackFrame.printTo(prefix + "  ", pw);
        }
        pw.print(prefix);
        pw.print("mEnteringBlackFrame=");
        pw.println(this.mEnteringBlackFrame);
        if (this.mEnteringBlackFrame != null) {
            this.mEnteringBlackFrame.printTo(prefix + "  ", pw);
        }
        pw.print(prefix);
        pw.print("mCurRotation=");
        pw.print(this.mCurRotation);
        pw.print(" mOriginalRotation=");
        pw.println(this.mOriginalRotation);
        pw.print(prefix);
        pw.print("mOriginalWidth=");
        pw.print(this.mOriginalWidth);
        pw.print(" mOriginalHeight=");
        pw.println(this.mOriginalHeight);
        pw.print(prefix);
        pw.print("mStarted=");
        pw.print(this.mStarted);
        pw.print(" mAnimRunning=");
        pw.print(this.mAnimRunning);
        pw.print(" mFinishAnimReady=");
        pw.print(this.mFinishAnimReady);
        pw.print(" mFinishAnimStartTime=");
        pw.println(this.mFinishAnimStartTime);
        pw.print(prefix);
        pw.print("mStartExitAnimation=");
        pw.print(this.mStartExitAnimation);
        pw.print(" ");
        this.mStartExitTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mStartEnterAnimation=");
        pw.print(this.mStartEnterAnimation);
        pw.print(" ");
        this.mStartEnterTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mStartFrameAnimation=");
        pw.print(this.mStartFrameAnimation);
        pw.print(" ");
        this.mStartFrameTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mFinishExitAnimation=");
        pw.print(this.mFinishExitAnimation);
        pw.print(" ");
        this.mFinishExitTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mFinishEnterAnimation=");
        pw.print(this.mFinishEnterAnimation);
        pw.print(" ");
        this.mFinishEnterTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mFinishFrameAnimation=");
        pw.print(this.mFinishFrameAnimation);
        pw.print(" ");
        this.mFinishFrameTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mRotateExitAnimation=");
        pw.print(this.mRotateExitAnimation);
        pw.print(" ");
        this.mRotateExitTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mRotateEnterAnimation=");
        pw.print(this.mRotateEnterAnimation);
        pw.print(" ");
        this.mRotateEnterTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mRotateFrameAnimation=");
        pw.print(this.mRotateFrameAnimation);
        pw.print(" ");
        this.mRotateFrameTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mExitTransformation=");
        this.mExitTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mEnterTransformation=");
        this.mEnterTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mFrameTransformation=");
        this.mEnterTransformation.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mFrameInitialMatrix=");
        this.mFrameInitialMatrix.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mSnapshotInitialMatrix=");
        this.mSnapshotInitialMatrix.printShortString(pw);
        pw.print(" mSnapshotFinalMatrix=");
        this.mSnapshotFinalMatrix.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mExitFrameFinalMatrix=");
        this.mExitFrameFinalMatrix.printShortString(pw);
        pw.println();
        pw.print(prefix);
        pw.print("mForceDefaultOrientation=");
        pw.print(this.mForceDefaultOrientation);
        if (this.mForceDefaultOrientation) {
            pw.print(" mOriginalDisplayRect=");
            pw.print(this.mOriginalDisplayRect.toShortString());
            pw.print(" mCurrentDisplayRect=");
            pw.println(this.mCurrentDisplayRect.toShortString());
        }
    }

    public ScreenRotationAnimation(Context context, DisplayContent displayContent, SurfaceSession session, boolean inTransaction, boolean forceDefaultOrientation, boolean isSecure) {
        int originalWidth;
        int originalHeight;
        this.mContext = context;
        this.mDisplayContent = displayContent;
        displayContent.getLogicalDisplayRect(this.mOriginalDisplayRect);
        Display display = displayContent.getDisplay();
        int originalRotation = display.getRotation();
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        if (forceDefaultOrientation) {
            this.mForceDefaultOrientation = true;
            originalWidth = displayContent.mBaseDisplayWidth;
            originalHeight = displayContent.mBaseDisplayHeight;
        } else {
            originalWidth = displayInfo.logicalWidth;
            originalHeight = displayInfo.logicalHeight;
        }
        if (mHwrotation == 0 || mHwrotation == 180) {
            if (originalRotation == 1 || originalRotation == 3) {
                this.mWidth = originalHeight;
                this.mHeight = originalWidth;
            } else {
                this.mWidth = originalWidth;
                this.mHeight = originalHeight;
            }
        } else if (originalRotation == 0 || originalRotation == 2) {
            this.mWidth = originalHeight;
            this.mHeight = originalWidth;
        } else {
            this.mWidth = originalWidth;
            this.mHeight = originalHeight;
        }
        this.mOriginalRotation = originalRotation;
        this.mOriginalWidth = originalWidth;
        this.mOriginalHeight = originalHeight;
        if (!inTransaction) {
            SurfaceControl.openTransaction();
        }
        int flags = isSecure ? 4 | 128 : 4;
        try {
            try {
                this.mSurfaceControl = new SurfaceControl(session, "ScreenshotSurface", this.mWidth, this.mHeight, -1, flags);
                Surface sur = new Surface();
                sur.copyFrom(this.mSurfaceControl);
                SurfaceControl.screenshot(SurfaceControl.getBuiltInDisplay(0), sur);
                this.mSurfaceControl.setLayerStack(display.getLayerStack());
                this.mSurfaceControl.setLayer(2000001);
                this.mSurfaceControl.setAlpha(0.0f);
                this.mSurfaceControl.show();
                sur.destroy();
            } catch (Surface.OutOfResourcesException e) {
                Slog.w(TAG, "Unable to allocate freeze surface", e);
            }
            setRotationInTransaction(originalRotation);
        } finally {
            if (!inTransaction) {
                SurfaceControl.closeTransaction();
            }
        }
    }

    boolean hasScreenshot() {
        return this.mSurfaceControl != null;
    }

    static int deltaRotation(int oldRotation, int newRotation) {
        int delta = newRotation - oldRotation;
        return delta < 0 ? delta + 4 : delta;
    }

    private void setSnapshotTransformInTransaction(Matrix matrix, float alpha) {
        if (this.mSurfaceControl != null) {
            matrix.getValues(this.mTmpFloats);
            float x = this.mTmpFloats[2];
            float y = this.mTmpFloats[5];
            if (this.mForceDefaultOrientation) {
                this.mDisplayContent.getLogicalDisplayRect(this.mCurrentDisplayRect);
                x -= this.mCurrentDisplayRect.left;
                y -= this.mCurrentDisplayRect.top;
            }
            this.mSurfaceControl.setPosition(x, y);
            this.mSurfaceControl.setMatrix(this.mTmpFloats[0], this.mTmpFloats[3], this.mTmpFloats[1], this.mTmpFloats[4]);
            this.mSurfaceControl.setAlpha(alpha);
        }
    }

    public static void createRotationMatrix(int rotation, int width, int height, Matrix outMatrix) {
        switch (rotation) {
            case 0:
                outMatrix.reset();
                break;
            case 1:
                outMatrix.setRotate(90.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(height, 0.0f);
                break;
            case 2:
                outMatrix.setRotate(180.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(width, height);
                break;
            case 3:
                outMatrix.setRotate(270.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(0.0f, width);
                break;
        }
    }

    private void setRotationInTransaction(int rotation) {
        this.mCurRotation = rotation;
        int newRotation = 0;
        switch (mHwrotation) {
            case 90:
                newRotation = 3;
                break;
            case 180:
                newRotation = 2;
                break;
            case 270:
                newRotation = 1;
                break;
        }
        int delta = deltaRotation(rotation, newRotation);
        createRotationMatrix(delta, this.mWidth, this.mHeight, this.mSnapshotInitialMatrix);
        setSnapshotTransformInTransaction(this.mSnapshotInitialMatrix, 1.0f);
    }

    public boolean setRotationInTransaction(int rotation, SurfaceSession session, long maxAnimationDuration, float animationScale, int finalWidth, int finalHeight) {
        setRotationInTransaction(rotation);
        return false;
    }

    private boolean startAnimation(SurfaceSession session, long maxAnimationDuration, float animationScale, int finalWidth, int finalHeight, boolean dismissing, int exitAnim, int enterAnim) {
        boolean customAnim;
        Rect outer;
        Rect inner;
        if (this.mSurfaceControl == null) {
            return false;
        }
        if (this.mStarted) {
            return true;
        }
        this.mStarted = true;
        int delta = deltaRotation(this.mCurRotation, this.mOriginalRotation);
        if (exitAnim != 0 && enterAnim != 0) {
            customAnim = true;
            this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, exitAnim);
            this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, enterAnim);
        } else {
            customAnim = false;
            switch (delta) {
                case 0:
                    this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.grow_fade_in);
                    this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.ft_avd_tooverflow_rectangle_path_3_animation);
                    break;
                case 1:
                    this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.ic_signal_wifi_transient_animation_2);
                    this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.ic_signal_wifi_transient_animation_1);
                    break;
                case 2:
                    this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.ic_bluetooth_transient_animation_0);
                    this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.grow_fade_in_from_bottom);
                    break;
                case 3:
                    this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.ic_hotspot_transient_animation_3);
                    this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.ic_hotspot_transient_animation_2);
                    break;
            }
        }
        this.mRotateEnterAnimation.initialize(finalWidth, finalHeight, this.mOriginalWidth, this.mOriginalHeight);
        this.mRotateExitAnimation.initialize(finalWidth, finalHeight, this.mOriginalWidth, this.mOriginalHeight);
        this.mAnimRunning = false;
        this.mFinishAnimReady = false;
        this.mFinishAnimStartTime = -1L;
        this.mRotateExitAnimation.restrictDuration(maxAnimationDuration);
        this.mRotateExitAnimation.scaleCurrentDuration(animationScale);
        this.mRotateEnterAnimation.restrictDuration(maxAnimationDuration);
        this.mRotateEnterAnimation.scaleCurrentDuration(animationScale);
        int layerStack = this.mDisplayContent.getDisplay().getLayerStack();
        if (!customAnim && this.mExitingBlackFrame == null) {
            SurfaceControl.openTransaction();
            try {
                createRotationMatrix(delta, this.mOriginalWidth, this.mOriginalHeight, this.mFrameInitialMatrix);
                if (this.mForceDefaultOrientation) {
                    outer = this.mCurrentDisplayRect;
                    inner = this.mOriginalDisplayRect;
                } else {
                    outer = new Rect((-this.mOriginalWidth) * 1, (-this.mOriginalHeight) * 1, this.mOriginalWidth * 2, this.mOriginalHeight * 2);
                    inner = new Rect(0, 0, this.mOriginalWidth, this.mOriginalHeight);
                }
                this.mExitingBlackFrame = new BlackFrame(session, outer, inner, 2000002, layerStack, this.mForceDefaultOrientation);
                this.mExitingBlackFrame.setMatrix(this.mFrameInitialMatrix);
            } catch (Surface.OutOfResourcesException e) {
                Slog.w(TAG, "Unable to allocate black surface", e);
            } finally {
            }
        }
        if (customAnim && this.mEnteringBlackFrame == null) {
            SurfaceControl.openTransaction();
            try {
                Rect outer2 = new Rect((-finalWidth) * 1, (-finalHeight) * 1, finalWidth * 2, finalHeight * 2);
                Rect inner2 = new Rect(0, 0, finalWidth, finalHeight);
                this.mEnteringBlackFrame = new BlackFrame(session, outer2, inner2, FREEZE_LAYER, layerStack, false);
            } catch (Surface.OutOfResourcesException e2) {
                Slog.w(TAG, "Unable to allocate black surface", e2);
            } finally {
            }
        }
        return true;
    }

    public boolean dismiss(SurfaceSession session, long maxAnimationDuration, float animationScale, int finalWidth, int finalHeight, int exitAnim, int enterAnim) {
        if (this.mSurfaceControl == null) {
            return false;
        }
        if (!this.mStarted) {
            startAnimation(session, maxAnimationDuration, animationScale, finalWidth, finalHeight, true, exitAnim, enterAnim);
        }
        if (!this.mStarted) {
            return false;
        }
        this.mFinishAnimReady = true;
        return true;
    }

    public void kill() {
        if (this.mSurfaceControl != null) {
            this.mSurfaceControl.destroy();
            this.mSurfaceControl = null;
        }
        if (this.mCustomBlackFrame != null) {
            this.mCustomBlackFrame.kill();
            this.mCustomBlackFrame = null;
        }
        if (this.mExitingBlackFrame != null) {
            this.mExitingBlackFrame.kill();
            this.mExitingBlackFrame = null;
        }
        if (this.mEnteringBlackFrame != null) {
            this.mEnteringBlackFrame.kill();
            this.mEnteringBlackFrame = null;
        }
        if (this.mRotateExitAnimation != null) {
            this.mRotateExitAnimation.cancel();
            this.mRotateExitAnimation = null;
        }
        if (this.mRotateEnterAnimation != null) {
            this.mRotateEnterAnimation.cancel();
            this.mRotateEnterAnimation = null;
        }
    }

    public boolean isAnimating() {
        return hasAnimations();
    }

    public boolean isRotating() {
        return this.mCurRotation != this.mOriginalRotation;
    }

    private boolean hasAnimations() {
        return (this.mRotateEnterAnimation == null && this.mRotateExitAnimation == null) ? false : true;
    }

    private boolean stepAnimation(long now) {
        if (now > this.mHalfwayPoint) {
            this.mHalfwayPoint = JobStatus.NO_LATEST_RUNTIME;
        }
        if (this.mFinishAnimReady && this.mFinishAnimStartTime < 0) {
            this.mFinishAnimStartTime = now;
        }
        if (this.mFinishAnimReady) {
            long j = now - this.mFinishAnimStartTime;
        }
        this.mMoreRotateExit = false;
        if (this.mRotateExitAnimation != null) {
            this.mMoreRotateExit = this.mRotateExitAnimation.getTransformation(now, this.mRotateExitTransformation);
        }
        this.mMoreRotateEnter = false;
        if (this.mRotateEnterAnimation != null) {
            this.mMoreRotateEnter = this.mRotateEnterAnimation.getTransformation(now, this.mRotateEnterTransformation);
        }
        if (!this.mMoreRotateExit && this.mRotateExitAnimation != null) {
            this.mRotateExitAnimation.cancel();
            this.mRotateExitAnimation = null;
            this.mRotateExitTransformation.clear();
        }
        if (!this.mMoreRotateEnter && this.mRotateEnterAnimation != null) {
            this.mRotateEnterAnimation.cancel();
            this.mRotateEnterAnimation = null;
            this.mRotateEnterTransformation.clear();
        }
        this.mExitTransformation.set(this.mRotateExitTransformation);
        this.mEnterTransformation.set(this.mRotateEnterTransformation);
        boolean more = this.mMoreRotateEnter || this.mMoreRotateExit || !this.mFinishAnimReady;
        this.mSnapshotFinalMatrix.setConcat(this.mExitTransformation.getMatrix(), this.mSnapshotInitialMatrix);
        return more;
    }

    void updateSurfacesInTransaction() {
        if (this.mStarted) {
            if (this.mSurfaceControl != null && !this.mMoreStartExit && !this.mMoreFinishExit && !this.mMoreRotateExit) {
                this.mSurfaceControl.hide();
            }
            if (this.mCustomBlackFrame != null) {
                if (!this.mMoreStartFrame && !this.mMoreFinishFrame && !this.mMoreRotateFrame) {
                    this.mCustomBlackFrame.hide();
                } else {
                    this.mCustomBlackFrame.setMatrix(this.mFrameTransformation.getMatrix());
                }
            }
            if (this.mExitingBlackFrame != null) {
                if (!this.mMoreStartExit && !this.mMoreFinishExit && !this.mMoreRotateExit) {
                    this.mExitingBlackFrame.hide();
                } else {
                    this.mExitFrameFinalMatrix.setConcat(this.mExitTransformation.getMatrix(), this.mFrameInitialMatrix);
                    this.mExitingBlackFrame.setMatrix(this.mExitFrameFinalMatrix);
                    if (this.mForceDefaultOrientation) {
                        this.mExitingBlackFrame.setAlpha(this.mExitTransformation.getAlpha());
                    }
                }
            }
            if (this.mEnteringBlackFrame != null) {
                if (!this.mMoreStartEnter && !this.mMoreFinishEnter && !this.mMoreRotateEnter) {
                    this.mEnteringBlackFrame.hide();
                } else {
                    this.mEnteringBlackFrame.setMatrix(this.mEnterTransformation.getMatrix());
                }
            }
            setSnapshotTransformInTransaction(this.mSnapshotFinalMatrix, this.mExitTransformation.getAlpha());
        }
    }

    public boolean stepAnimationLocked(long now) {
        if (!hasAnimations()) {
            this.mFinishAnimReady = false;
            return false;
        }
        if (!this.mAnimRunning) {
            if (this.mRotateEnterAnimation != null) {
                this.mRotateEnterAnimation.setStartTime(now);
            }
            if (this.mRotateExitAnimation != null) {
                this.mRotateExitAnimation.setStartTime(now);
            }
            this.mAnimRunning = true;
            this.mHalfwayPoint = (this.mRotateEnterAnimation.getDuration() / 2) + now;
        }
        return stepAnimation(now);
    }

    public Transformation getEnterTransformation() {
        return this.mEnterTransformation;
    }
}
