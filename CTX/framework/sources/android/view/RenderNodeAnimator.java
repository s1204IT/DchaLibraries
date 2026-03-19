package android.view;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.util.SparseIntArray;
import com.android.internal.util.VirtualRefBasePtr;
import com.android.internal.view.animation.FallbackLUTInterpolator;
import com.android.internal.view.animation.HasNativeInterpolator;
import com.android.internal.view.animation.NativeInterpolatorFactory;
import java.util.ArrayList;

public class RenderNodeAnimator extends Animator {
    public static final int ALPHA = 11;
    public static final int LAST_VALUE = 11;
    public static final int PAINT_ALPHA = 1;
    public static final int PAINT_STROKE_WIDTH = 0;
    public static final int ROTATION = 5;
    public static final int ROTATION_X = 6;
    public static final int ROTATION_Y = 7;
    public static final int SCALE_X = 3;
    public static final int SCALE_Y = 4;
    private static final int STATE_DELAYED = 1;
    private static final int STATE_FINISHED = 3;
    private static final int STATE_PREPARE = 0;
    private static final int STATE_RUNNING = 2;
    public static final int TRANSLATION_X = 0;
    public static final int TRANSLATION_Y = 1;
    public static final int TRANSLATION_Z = 2;
    public static final int X = 8;
    public static final int Y = 9;
    public static final int Z = 10;
    private float mFinalValue;
    private TimeInterpolator mInterpolator;
    private VirtualRefBasePtr mNativePtr;
    private int mRenderProperty;
    private long mStartDelay;
    private long mStartTime;
    private int mState;
    private RenderNode mTarget;
    private final boolean mUiThreadHandlesDelay;
    private long mUnscaledDuration;
    private long mUnscaledStartDelay;
    private View mViewTarget;
    private static final SparseIntArray sViewPropertyAnimatorMap = new SparseIntArray(15) {
        {
            put(1, 0);
            put(2, 1);
            put(4, 2);
            put(8, 3);
            put(16, 4);
            put(32, 5);
            put(64, 6);
            put(128, 7);
            put(256, 8);
            put(512, 9);
            put(1024, 10);
            put(2048, 11);
        }
    };
    private static ThreadLocal<DelayedAnimationHelper> sAnimationHelper = new ThreadLocal<>();

    private static native long nCreateAnimator(int i, float f);

    private static native long nCreateCanvasPropertyFloatAnimator(long j, float f);

    private static native long nCreateCanvasPropertyPaintAnimator(long j, int i, float f);

    private static native long nCreateRevealAnimator(int i, int i2, float f, float f2);

    private static native void nEnd(long j);

    private static native long nGetDuration(long j);

    private static native void nSetAllowRunningAsync(long j, boolean z);

    private static native void nSetDuration(long j, long j2);

    private static native void nSetInterpolator(long j, long j2);

    private static native void nSetListener(long j, RenderNodeAnimator renderNodeAnimator);

    private static native void nSetStartDelay(long j, long j2);

    private static native void nSetStartValue(long j, float f);

    private static native void nStart(long j);

    public static int mapViewPropertyToRenderProperty(int i) {
        return sViewPropertyAnimatorMap.get(i);
    }

    public RenderNodeAnimator(int i, float f) {
        this.mRenderProperty = -1;
        this.mState = 0;
        this.mUnscaledDuration = 300L;
        this.mUnscaledStartDelay = 0L;
        this.mStartDelay = 0L;
        this.mRenderProperty = i;
        this.mFinalValue = f;
        this.mUiThreadHandlesDelay = true;
        init(nCreateAnimator(i, f));
    }

    public RenderNodeAnimator(CanvasProperty<Float> canvasProperty, float f) {
        this.mRenderProperty = -1;
        this.mState = 0;
        this.mUnscaledDuration = 300L;
        this.mUnscaledStartDelay = 0L;
        this.mStartDelay = 0L;
        init(nCreateCanvasPropertyFloatAnimator(canvasProperty.getNativeContainer(), f));
        this.mUiThreadHandlesDelay = false;
    }

    public RenderNodeAnimator(CanvasProperty<Paint> canvasProperty, int i, float f) {
        this.mRenderProperty = -1;
        this.mState = 0;
        this.mUnscaledDuration = 300L;
        this.mUnscaledStartDelay = 0L;
        this.mStartDelay = 0L;
        init(nCreateCanvasPropertyPaintAnimator(canvasProperty.getNativeContainer(), i, f));
        this.mUiThreadHandlesDelay = false;
    }

    public RenderNodeAnimator(int i, int i2, float f, float f2) {
        this.mRenderProperty = -1;
        this.mState = 0;
        this.mUnscaledDuration = 300L;
        this.mUnscaledStartDelay = 0L;
        this.mStartDelay = 0L;
        init(nCreateRevealAnimator(i, i2, f, f2));
        this.mUiThreadHandlesDelay = true;
    }

    private void init(long j) {
        this.mNativePtr = new VirtualRefBasePtr(j);
    }

    private void checkMutable() {
        if (this.mState != 0) {
            throw new IllegalStateException("Animator has already started, cannot change it now!");
        }
        if (this.mNativePtr == null) {
            throw new IllegalStateException("Animator's target has been destroyed (trying to modify an animation after activity destroy?)");
        }
    }

    static boolean isNativeInterpolator(TimeInterpolator timeInterpolator) {
        return timeInterpolator.getClass().isAnnotationPresent(HasNativeInterpolator.class);
    }

    private void applyInterpolator() {
        long jCreateNativeInterpolator;
        if (this.mInterpolator == null || this.mNativePtr == null) {
            return;
        }
        if (isNativeInterpolator(this.mInterpolator)) {
            jCreateNativeInterpolator = ((NativeInterpolatorFactory) this.mInterpolator).createNativeInterpolator();
        } else {
            jCreateNativeInterpolator = FallbackLUTInterpolator.createNativeInterpolator(this.mInterpolator, nGetDuration(this.mNativePtr.get()));
        }
        nSetInterpolator(this.mNativePtr.get(), jCreateNativeInterpolator);
    }

    @Override
    public void start() {
        if (this.mTarget == null) {
            throw new IllegalStateException("Missing target!");
        }
        if (this.mState != 0) {
            throw new IllegalStateException("Already started!");
        }
        this.mState = 1;
        applyInterpolator();
        if (this.mNativePtr == null) {
            cancel();
        } else if (this.mStartDelay <= 0 || !this.mUiThreadHandlesDelay) {
            nSetStartDelay(this.mNativePtr.get(), this.mStartDelay);
            doStart();
        } else {
            getHelper().addDelayedAnimation(this);
        }
    }

    private void doStart() {
        if (this.mRenderProperty == 11) {
            this.mViewTarget.ensureTransformationInfo();
            this.mViewTarget.mTransformationInfo.mAlpha = this.mFinalValue;
        }
        moveToRunningState();
        if (this.mViewTarget != null) {
            this.mViewTarget.invalidateViewProperty(true, false);
        }
    }

    private void moveToRunningState() {
        this.mState = 2;
        if (this.mNativePtr != null) {
            nStart(this.mNativePtr.get());
        }
        notifyStartListeners();
    }

    private void notifyStartListeners() {
        int size;
        ArrayList<Animator.AnimatorListener> arrayListCloneListeners = cloneListeners();
        if (arrayListCloneListeners != null) {
            size = arrayListCloneListeners.size();
        } else {
            size = 0;
        }
        for (int i = 0; i < size; i++) {
            arrayListCloneListeners.get(i).onAnimationStart(this);
        }
    }

    @Override
    public void cancel() {
        int size;
        if (this.mState != 0 && this.mState != 3) {
            if (this.mState == 1) {
                getHelper().removeDelayedAnimation(this);
                moveToRunningState();
            }
            ArrayList<Animator.AnimatorListener> arrayListCloneListeners = cloneListeners();
            if (arrayListCloneListeners != null) {
                size = arrayListCloneListeners.size();
            } else {
                size = 0;
            }
            for (int i = 0; i < size; i++) {
                arrayListCloneListeners.get(i).onAnimationCancel(this);
            }
            end();
        }
    }

    @Override
    public void end() {
        if (this.mState != 3) {
            if (this.mState < 2) {
                getHelper().removeDelayedAnimation(this);
                doStart();
            }
            if (this.mNativePtr != null) {
                nEnd(this.mNativePtr.get());
                if (this.mViewTarget != null) {
                    this.mViewTarget.invalidateViewProperty(true, false);
                    return;
                }
                return;
            }
            onFinished();
        }
    }

    @Override
    public void pause() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resume() {
        throw new UnsupportedOperationException();
    }

    public void setTarget(View view) {
        this.mViewTarget = view;
        setTarget(this.mViewTarget.mRenderNode);
    }

    public void setTarget(DisplayListCanvas displayListCanvas) {
        setTarget(displayListCanvas.mNode);
    }

    private void setTarget(RenderNode renderNode) {
        checkMutable();
        if (this.mTarget != null) {
            throw new IllegalStateException("Target already set!");
        }
        nSetListener(this.mNativePtr.get(), this);
        this.mTarget = renderNode;
        this.mTarget.addAnimator(this);
    }

    public void setStartValue(float f) {
        checkMutable();
        nSetStartValue(this.mNativePtr.get(), f);
    }

    @Override
    public void setStartDelay(long j) {
        checkMutable();
        if (j < 0) {
            throw new IllegalArgumentException("startDelay must be positive; " + j);
        }
        this.mUnscaledStartDelay = j;
        this.mStartDelay = (long) (ValueAnimator.getDurationScale() * j);
    }

    @Override
    public long getStartDelay() {
        return this.mUnscaledStartDelay;
    }

    @Override
    public RenderNodeAnimator setDuration(long j) {
        checkMutable();
        if (j < 0) {
            throw new IllegalArgumentException("duration must be positive; " + j);
        }
        this.mUnscaledDuration = j;
        nSetDuration(this.mNativePtr.get(), (long) (j * ValueAnimator.getDurationScale()));
        return this;
    }

    @Override
    public long getDuration() {
        return this.mUnscaledDuration;
    }

    @Override
    public long getTotalDuration() {
        return this.mUnscaledDuration + this.mUnscaledStartDelay;
    }

    @Override
    public boolean isRunning() {
        return this.mState == 1 || this.mState == 2;
    }

    @Override
    public boolean isStarted() {
        return this.mState != 0;
    }

    @Override
    public void setInterpolator(TimeInterpolator timeInterpolator) {
        checkMutable();
        this.mInterpolator = timeInterpolator;
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return this.mInterpolator;
    }

    protected void onFinished() {
        int size;
        if (this.mState == 0) {
            releaseNativePtr();
            return;
        }
        if (this.mState == 1) {
            getHelper().removeDelayedAnimation(this);
            notifyStartListeners();
        }
        this.mState = 3;
        ArrayList<Animator.AnimatorListener> arrayListCloneListeners = cloneListeners();
        if (arrayListCloneListeners != null) {
            size = arrayListCloneListeners.size();
        } else {
            size = 0;
        }
        for (int i = 0; i < size; i++) {
            arrayListCloneListeners.get(i).onAnimationEnd(this);
        }
        releaseNativePtr();
    }

    private void releaseNativePtr() {
        if (this.mNativePtr != null) {
            this.mNativePtr.release();
            this.mNativePtr = null;
        }
    }

    private ArrayList<Animator.AnimatorListener> cloneListeners() {
        ArrayList<Animator.AnimatorListener> listeners = getListeners();
        if (listeners != null) {
            return (ArrayList) listeners.clone();
        }
        return listeners;
    }

    long getNativeAnimator() {
        return this.mNativePtr.get();
    }

    private boolean processDelayed(long j) {
        if (this.mStartTime == 0) {
            this.mStartTime = j;
            return false;
        }
        if (j - this.mStartTime >= this.mStartDelay) {
            doStart();
            return true;
        }
        return false;
    }

    private static DelayedAnimationHelper getHelper() {
        DelayedAnimationHelper delayedAnimationHelper = sAnimationHelper.get();
        if (delayedAnimationHelper == null) {
            DelayedAnimationHelper delayedAnimationHelper2 = new DelayedAnimationHelper();
            sAnimationHelper.set(delayedAnimationHelper2);
            return delayedAnimationHelper2;
        }
        return delayedAnimationHelper;
    }

    private static class DelayedAnimationHelper implements Runnable {
        private boolean mCallbackScheduled;
        private ArrayList<RenderNodeAnimator> mDelayedAnims = new ArrayList<>();
        private final Choreographer mChoreographer = Choreographer.getInstance();

        public void addDelayedAnimation(RenderNodeAnimator renderNodeAnimator) {
            this.mDelayedAnims.add(renderNodeAnimator);
            scheduleCallback();
        }

        public void removeDelayedAnimation(RenderNodeAnimator renderNodeAnimator) {
            this.mDelayedAnims.remove(renderNodeAnimator);
        }

        private void scheduleCallback() {
            if (!this.mCallbackScheduled) {
                this.mCallbackScheduled = true;
                this.mChoreographer.postCallback(1, this, null);
            }
        }

        @Override
        public void run() {
            long frameTime = this.mChoreographer.getFrameTime();
            this.mCallbackScheduled = false;
            int i = 0;
            for (int i2 = 0; i2 < this.mDelayedAnims.size(); i2++) {
                RenderNodeAnimator renderNodeAnimator = this.mDelayedAnims.get(i2);
                if (!renderNodeAnimator.processDelayed(frameTime)) {
                    if (i != i2) {
                        this.mDelayedAnims.set(i, renderNodeAnimator);
                    }
                    i++;
                }
            }
            while (this.mDelayedAnims.size() > i) {
                this.mDelayedAnims.remove(this.mDelayedAnims.size() - 1);
            }
            if (this.mDelayedAnims.size() > 0) {
                scheduleCallback();
            }
        }
    }

    private static void callOnFinished(RenderNodeAnimator renderNodeAnimator) {
        renderNodeAnimator.onFinished();
    }

    @Override
    public Animator mo0clone() {
        throw new IllegalStateException("Cannot clone this animator");
    }

    @Override
    public void setAllowRunningAsynchronously(boolean z) {
        checkMutable();
        nSetAllowRunningAsync(this.mNativePtr.get(), z);
    }
}
