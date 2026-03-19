package com.android.server.wm;

import android.R;
import android.content.Context;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;

class ScreenRotationAnimation {
    static final boolean DEBUG_STATE = false;
    static final boolean DEBUG_TRANSFORMS = false;
    static final int SCREEN_FREEZE_LAYER_BASE = 2010000;
    static final int SCREEN_FREEZE_LAYER_CUSTOM = 2010003;
    static final int SCREEN_FREEZE_LAYER_ENTER = 2010000;
    static final int SCREEN_FREEZE_LAYER_EXIT = 2010002;
    static final int SCREEN_FREEZE_LAYER_SCREENSHOT = 2010001;
    static final String TAG = "WindowManager";
    static final boolean TWO_PHASE_ANIMATION = false;
    static final boolean USE_CUSTOM_BLACK_FRAME = false;
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
    private final WindowManagerService mService;
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

    public void printTo(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("mSurface=");
        printWriter.print(this.mSurfaceControl);
        printWriter.print(" mWidth=");
        printWriter.print(this.mWidth);
        printWriter.print(" mHeight=");
        printWriter.println(this.mHeight);
        printWriter.print(str);
        printWriter.print("mExitingBlackFrame=");
        printWriter.println(this.mExitingBlackFrame);
        if (this.mExitingBlackFrame != null) {
            this.mExitingBlackFrame.printTo(str + "  ", printWriter);
        }
        printWriter.print(str);
        printWriter.print("mEnteringBlackFrame=");
        printWriter.println(this.mEnteringBlackFrame);
        if (this.mEnteringBlackFrame != null) {
            this.mEnteringBlackFrame.printTo(str + "  ", printWriter);
        }
        printWriter.print(str);
        printWriter.print("mCurRotation=");
        printWriter.print(this.mCurRotation);
        printWriter.print(" mOriginalRotation=");
        printWriter.println(this.mOriginalRotation);
        printWriter.print(str);
        printWriter.print("mOriginalWidth=");
        printWriter.print(this.mOriginalWidth);
        printWriter.print(" mOriginalHeight=");
        printWriter.println(this.mOriginalHeight);
        printWriter.print(str);
        printWriter.print("mStarted=");
        printWriter.print(this.mStarted);
        printWriter.print(" mAnimRunning=");
        printWriter.print(this.mAnimRunning);
        printWriter.print(" mFinishAnimReady=");
        printWriter.print(this.mFinishAnimReady);
        printWriter.print(" mFinishAnimStartTime=");
        printWriter.println(this.mFinishAnimStartTime);
        printWriter.print(str);
        printWriter.print("mStartExitAnimation=");
        printWriter.print(this.mStartExitAnimation);
        printWriter.print(" ");
        this.mStartExitTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mStartEnterAnimation=");
        printWriter.print(this.mStartEnterAnimation);
        printWriter.print(" ");
        this.mStartEnterTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mStartFrameAnimation=");
        printWriter.print(this.mStartFrameAnimation);
        printWriter.print(" ");
        this.mStartFrameTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mFinishExitAnimation=");
        printWriter.print(this.mFinishExitAnimation);
        printWriter.print(" ");
        this.mFinishExitTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mFinishEnterAnimation=");
        printWriter.print(this.mFinishEnterAnimation);
        printWriter.print(" ");
        this.mFinishEnterTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mFinishFrameAnimation=");
        printWriter.print(this.mFinishFrameAnimation);
        printWriter.print(" ");
        this.mFinishFrameTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mRotateExitAnimation=");
        printWriter.print(this.mRotateExitAnimation);
        printWriter.print(" ");
        this.mRotateExitTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mRotateEnterAnimation=");
        printWriter.print(this.mRotateEnterAnimation);
        printWriter.print(" ");
        this.mRotateEnterTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mRotateFrameAnimation=");
        printWriter.print(this.mRotateFrameAnimation);
        printWriter.print(" ");
        this.mRotateFrameTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mExitTransformation=");
        this.mExitTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mEnterTransformation=");
        this.mEnterTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mFrameTransformation=");
        this.mFrameTransformation.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mFrameInitialMatrix=");
        this.mFrameInitialMatrix.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mSnapshotInitialMatrix=");
        this.mSnapshotInitialMatrix.printShortString(printWriter);
        printWriter.print(" mSnapshotFinalMatrix=");
        this.mSnapshotFinalMatrix.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mExitFrameFinalMatrix=");
        this.mExitFrameFinalMatrix.printShortString(printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("mForceDefaultOrientation=");
        printWriter.print(this.mForceDefaultOrientation);
        if (this.mForceDefaultOrientation) {
            printWriter.print(" mOriginalDisplayRect=");
            printWriter.print(this.mOriginalDisplayRect.toShortString());
            printWriter.print(" mCurrentDisplayRect=");
            printWriter.println(this.mCurrentDisplayRect.toShortString());
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1133871366145L, this.mStarted);
        protoOutputStream.write(1133871366146L, this.mAnimRunning);
        protoOutputStream.end(jStart);
    }

    public ScreenRotationAnimation(Context context, DisplayContent displayContent, boolean z, boolean z2, WindowManagerService windowManagerService) {
        int i;
        int i2;
        this.mService = windowManagerService;
        this.mContext = context;
        this.mDisplayContent = displayContent;
        displayContent.getBounds(this.mOriginalDisplayRect);
        int rotation = displayContent.getDisplay().getRotation();
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        if (z) {
            this.mForceDefaultOrientation = true;
            i = displayContent.mBaseDisplayWidth;
            i2 = displayContent.mBaseDisplayHeight;
        } else {
            i = displayInfo.logicalWidth;
            i2 = displayInfo.logicalHeight;
        }
        if (rotation == 1 || rotation == 3) {
            this.mWidth = i2;
            this.mHeight = i;
        } else {
            this.mWidth = i;
            this.mHeight = i2;
        }
        this.mOriginalRotation = rotation;
        this.mOriginalWidth = i;
        this.mOriginalHeight = i2;
        this.mDisplayContent.mService.mPowerHalManager.setRotationBoost(true);
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        try {
            this.mSurfaceControl = displayContent.makeOverlay().setName("ScreenshotSurface").setSize(this.mWidth, this.mHeight).setSecure(z2).build();
            SurfaceControl.Transaction transaction2 = new SurfaceControl.Transaction();
            transaction2.setOverrideScalingMode(this.mSurfaceControl, 1);
            transaction2.apply(true);
            if (SurfaceControl.getBuiltInDisplay(0) != null) {
                Surface surface = new Surface();
                surface.copyFrom(this.mSurfaceControl);
                GraphicBuffer graphicBufferScreenshotToBufferWithSecureLayersUnsafe = SurfaceControl.screenshotToBufferWithSecureLayersUnsafe(new Rect(), 0, 0, 0, 0, false, 0);
                if (graphicBufferScreenshotToBufferWithSecureLayersUnsafe != null) {
                    try {
                        surface.attachAndQueueBuffer(graphicBufferScreenshotToBufferWithSecureLayersUnsafe);
                    } catch (RuntimeException e) {
                        Slog.w("WindowManager", "Failed to attach screenshot - " + e.getMessage());
                    }
                    if (graphicBufferScreenshotToBufferWithSecureLayersUnsafe.doesContainSecureLayers()) {
                        transaction.setSecure(this.mSurfaceControl, true);
                    }
                    transaction.setLayer(this.mSurfaceControl, SCREEN_FREEZE_LAYER_SCREENSHOT);
                    transaction.setAlpha(this.mSurfaceControl, 0.0f);
                    transaction.show(this.mSurfaceControl);
                } else {
                    Slog.w("WindowManager", "Unable to take screenshot of display 0");
                }
                surface.destroy();
            } else {
                Slog.w("WindowManager", "Built-in display 0 is null.");
            }
        } catch (Surface.OutOfResourcesException e2) {
            Slog.w("WindowManager", "Unable to allocate freeze surface", e2);
        }
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
            Slog.i("WindowManager", "  FREEZE " + this.mSurfaceControl + ": CREATE");
        }
        setRotation(transaction, rotation);
        transaction.apply();
    }

    boolean hasScreenshot() {
        return this.mSurfaceControl != null;
    }

    private void setSnapshotTransform(SurfaceControl.Transaction transaction, Matrix matrix, float f) {
        if (this.mSurfaceControl != null) {
            matrix.getValues(this.mTmpFloats);
            float f2 = this.mTmpFloats[2];
            float f3 = this.mTmpFloats[5];
            if (this.mForceDefaultOrientation) {
                this.mDisplayContent.getBounds(this.mCurrentDisplayRect);
                f2 -= this.mCurrentDisplayRect.left;
                f3 -= this.mCurrentDisplayRect.top;
            }
            transaction.setPosition(this.mSurfaceControl, f2, f3);
            transaction.setMatrix(this.mSurfaceControl, this.mTmpFloats[0], this.mTmpFloats[3], this.mTmpFloats[1], this.mTmpFloats[4]);
            transaction.setAlpha(this.mSurfaceControl, f);
        }
    }

    public static void createRotationMatrix(int i, int i2, int i3, Matrix matrix) {
        switch (i) {
            case 0:
                matrix.reset();
                break;
            case 1:
                matrix.setRotate(90.0f, 0.0f, 0.0f);
                matrix.postTranslate(i3, 0.0f);
                break;
            case 2:
                matrix.setRotate(180.0f, 0.0f, 0.0f);
                matrix.postTranslate(i2, i3);
                break;
            case 3:
                matrix.setRotate(270.0f, 0.0f, 0.0f);
                matrix.postTranslate(0.0f, i2);
                break;
        }
    }

    private void setRotation(SurfaceControl.Transaction transaction, int i) {
        this.mCurRotation = i;
        createRotationMatrix(DisplayContent.deltaRotation(i, 0), this.mWidth, this.mHeight, this.mSnapshotInitialMatrix);
        setSnapshotTransform(transaction, this.mSnapshotInitialMatrix, 1.0f);
    }

    public boolean setRotation(SurfaceControl.Transaction transaction, int i, long j, float f, int i2, int i3) {
        setRotation(transaction, i);
        return false;
    }

    private boolean startAnimation(SurfaceControl.Transaction transaction, long j, float f, int i, int i2, boolean z, int i3, int i4) {
        boolean z2;
        SurfaceControl.Transaction transaction2;
        Rect rect;
        Rect rect2;
        if (this.mSurfaceControl == null) {
            return false;
        }
        if (this.mStarted) {
            return true;
        }
        this.mStarted = true;
        int iDeltaRotation = DisplayContent.deltaRotation(this.mCurRotation, this.mOriginalRotation);
        if (i3 != 0 && i4 != 0) {
            this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, i3);
            this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, i4);
            z2 = true;
        } else {
            switch (iDeltaRotation) {
                case 0:
                    this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.popup_enter_material);
                    this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.overlay_task_fragment_open_from_top);
                    break;
                case 1:
                    this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.recent_enter);
                    this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.push_up_out);
                    break;
                case 2:
                    this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.progress_indeterminate_horizontal_rect2);
                    this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.progress_indeterminate_horizontal_rect1);
                    break;
                case 3:
                    this.mRotateExitAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.push_down_out_no_alpha);
                    this.mRotateEnterAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.push_down_out);
                    break;
            }
            z2 = false;
        }
        this.mRotateEnterAnimation.initialize(i, i2, this.mOriginalWidth, this.mOriginalHeight);
        this.mRotateExitAnimation.initialize(i, i2, this.mOriginalWidth, this.mOriginalHeight);
        this.mAnimRunning = false;
        this.mFinishAnimReady = false;
        this.mFinishAnimStartTime = -1L;
        this.mRotateExitAnimation.restrictDuration(j);
        this.mRotateExitAnimation.scaleCurrentDuration(f);
        this.mRotateEnterAnimation.restrictDuration(j);
        this.mRotateEnterAnimation.scaleCurrentDuration(f);
        this.mDisplayContent.getDisplay().getLayerStack();
        if (!z2 && this.mExitingBlackFrame == null) {
            try {
                createRotationMatrix(iDeltaRotation, this.mOriginalWidth, this.mOriginalHeight, this.mFrameInitialMatrix);
                if (this.mForceDefaultOrientation) {
                    rect = this.mCurrentDisplayRect;
                    rect2 = this.mOriginalDisplayRect;
                } else {
                    rect = new Rect((-this.mOriginalWidth) * 1, (-this.mOriginalHeight) * 1, this.mOriginalWidth * 2, this.mOriginalHeight * 2);
                    rect2 = new Rect(0, 0, this.mOriginalWidth, this.mOriginalHeight);
                }
                this.mExitingBlackFrame = new BlackFrame(transaction, rect, rect2, SCREEN_FREEZE_LAYER_EXIT, this.mDisplayContent, this.mForceDefaultOrientation);
                transaction2 = transaction;
            } catch (Surface.OutOfResourcesException e) {
                e = e;
                transaction2 = transaction;
            }
            try {
                this.mExitingBlackFrame.setMatrix(transaction2, this.mFrameInitialMatrix);
            } catch (Surface.OutOfResourcesException e2) {
                e = e2;
                Slog.w("WindowManager", "Unable to allocate black surface", e);
            }
        } else {
            transaction2 = transaction;
        }
        if (z2 && this.mEnteringBlackFrame == null) {
            try {
                this.mEnteringBlackFrame = new BlackFrame(transaction2, new Rect((-i) * 1, (-i2) * 1, i * 2, i2 * 2), new Rect(0, 0, i, i2), 2010000, this.mDisplayContent, false);
            } catch (Surface.OutOfResourcesException e3) {
                Slog.w("WindowManager", "Unable to allocate black surface", e3);
            }
        }
        return true;
    }

    public boolean dismiss(SurfaceControl.Transaction transaction, long j, float f, int i, int i2, int i3, int i4) {
        if (this.mSurfaceControl == null) {
            return false;
        }
        if (!this.mStarted) {
            startAnimation(transaction, j, f, i, i2, true, i3, i4);
        }
        if (!this.mStarted) {
            return false;
        }
        this.mFinishAnimReady = true;
        return true;
    }

    public void kill() {
        if (this.mSurfaceControl != null) {
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS || WindowManagerDebugConfig.SHOW_SURFACE_ALLOC) {
                Slog.i("WindowManager", "  FREEZE " + this.mSurfaceControl + ": DESTROY");
            }
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
        this.mDisplayContent.mService.mPowerHalManager.setRotationBoost(false);
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

    private boolean stepAnimation(long j) {
        if (j > this.mHalfwayPoint) {
            this.mHalfwayPoint = JobStatus.NO_LATEST_RUNTIME;
        }
        if (this.mFinishAnimReady && this.mFinishAnimStartTime < 0) {
            this.mFinishAnimStartTime = j;
        }
        if (this.mFinishAnimReady) {
            long j2 = this.mFinishAnimStartTime;
        }
        this.mMoreRotateExit = false;
        if (this.mRotateExitAnimation != null) {
            this.mMoreRotateExit = this.mRotateExitAnimation.getTransformation(j, this.mRotateExitTransformation);
        }
        this.mMoreRotateEnter = false;
        if (this.mRotateEnterAnimation != null) {
            this.mMoreRotateEnter = this.mRotateEnterAnimation.getTransformation(j, this.mRotateEnterTransformation);
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
        boolean z = this.mMoreRotateEnter || this.mMoreRotateExit || !this.mFinishAnimReady;
        this.mSnapshotFinalMatrix.setConcat(this.mExitTransformation.getMatrix(), this.mSnapshotInitialMatrix);
        return z;
    }

    void updateSurfaces(SurfaceControl.Transaction transaction) {
        if (!this.mStarted) {
            return;
        }
        if (this.mSurfaceControl != null && !this.mMoreStartExit && !this.mMoreFinishExit && !this.mMoreRotateExit) {
            transaction.hide(this.mSurfaceControl);
        }
        if (this.mCustomBlackFrame != null) {
            if (!this.mMoreStartFrame && !this.mMoreFinishFrame && !this.mMoreRotateFrame) {
                this.mCustomBlackFrame.hide(transaction);
            } else {
                this.mCustomBlackFrame.setMatrix(transaction, this.mFrameTransformation.getMatrix());
            }
        }
        if (this.mExitingBlackFrame != null) {
            if (!this.mMoreStartExit && !this.mMoreFinishExit && !this.mMoreRotateExit) {
                this.mExitingBlackFrame.hide(transaction);
            } else {
                this.mExitFrameFinalMatrix.setConcat(this.mExitTransformation.getMatrix(), this.mFrameInitialMatrix);
                this.mExitingBlackFrame.setMatrix(transaction, this.mExitFrameFinalMatrix);
                if (this.mForceDefaultOrientation) {
                    this.mExitingBlackFrame.setAlpha(transaction, this.mExitTransformation.getAlpha());
                }
            }
        }
        if (this.mEnteringBlackFrame != null) {
            if (!this.mMoreStartEnter && !this.mMoreFinishEnter && !this.mMoreRotateEnter) {
                this.mEnteringBlackFrame.hide(transaction);
            } else {
                this.mEnteringBlackFrame.setMatrix(transaction, this.mEnterTransformation.getMatrix());
            }
        }
        setSnapshotTransform(transaction, this.mSnapshotFinalMatrix, this.mExitTransformation.getAlpha());
    }

    public boolean stepAnimationLocked(long j) {
        if (!hasAnimations()) {
            this.mFinishAnimReady = false;
            return false;
        }
        if (!this.mAnimRunning) {
            if (this.mRotateEnterAnimation != null) {
                this.mRotateEnterAnimation.setStartTime(j);
            }
            if (this.mRotateExitAnimation != null) {
                this.mRotateExitAnimation.setStartTime(j);
            }
            this.mAnimRunning = true;
            this.mHalfwayPoint = (this.mRotateEnterAnimation.getDuration() / 2) + j;
        }
        return stepAnimation(j);
    }

    public Transformation getEnterTransformation() {
        return this.mEnterTransformation;
    }
}
