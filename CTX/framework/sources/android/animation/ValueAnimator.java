package android.animation;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.os.Looper;
import android.os.Trace;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ValueAnimator extends Animator implements AnimationHandler.AnimationFrameCallback {
    private static final boolean DEBUG = false;
    public static final int INFINITE = -1;
    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    private static final String TAG = "ValueAnimator";
    private long mPauseTime;
    private boolean mReversing;
    boolean mStartTimeCommitted;
    PropertyValuesHolder[] mValues;
    HashMap<String, PropertyValuesHolder> mValuesMap;
    private static float sDurationScale = 1.0f;
    private static final TimeInterpolator sDefaultInterpolator = new AccelerateDecelerateInterpolator();
    long mStartTime = -1;
    float mSeekFraction = -1.0f;
    private boolean mResumed = false;
    private float mOverallFraction = 0.0f;
    private float mCurrentFraction = 0.0f;
    private long mLastFrameTime = -1;
    private long mFirstFrameTime = -1;
    private boolean mRunning = false;
    private boolean mStarted = false;
    private boolean mStartListenersCalled = false;
    boolean mInitialized = false;
    private boolean mAnimationEndRequested = false;
    private long mDuration = 300;
    private long mStartDelay = 0;
    private int mRepeatCount = 0;
    private int mRepeatMode = 1;
    private boolean mSelfPulse = true;
    private boolean mSuppressSelfPulseRequested = false;
    private TimeInterpolator mInterpolator = sDefaultInterpolator;
    ArrayList<AnimatorUpdateListener> mUpdateListeners = null;
    private float mDurationScale = -1.0f;

    public interface AnimatorUpdateListener {
        void onAnimationUpdate(ValueAnimator valueAnimator);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode {
    }

    public static void setDurationScale(float f) {
        sDurationScale = f;
    }

    public static float getDurationScale() {
        return sDurationScale;
    }

    public static boolean areAnimatorsEnabled() {
        return sDurationScale != 0.0f;
    }

    public static ValueAnimator ofInt(int... iArr) {
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setIntValues(iArr);
        return valueAnimator;
    }

    public static ValueAnimator ofArgb(int... iArr) {
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setIntValues(iArr);
        valueAnimator.setEvaluator(ArgbEvaluator.getInstance());
        return valueAnimator;
    }

    public static ValueAnimator ofFloat(float... fArr) {
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setFloatValues(fArr);
        return valueAnimator;
    }

    public static ValueAnimator ofPropertyValuesHolder(PropertyValuesHolder... propertyValuesHolderArr) {
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setValues(propertyValuesHolderArr);
        return valueAnimator;
    }

    public static ValueAnimator ofObject(TypeEvaluator typeEvaluator, Object... objArr) {
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setObjectValues(objArr);
        valueAnimator.setEvaluator(typeEvaluator);
        return valueAnimator;
    }

    public void setIntValues(int... iArr) {
        if (iArr == null || iArr.length == 0) {
            return;
        }
        if (this.mValues == null || this.mValues.length == 0) {
            setValues(PropertyValuesHolder.ofInt("", iArr));
        } else {
            this.mValues[0].setIntValues(iArr);
        }
        this.mInitialized = false;
    }

    public void setFloatValues(float... fArr) {
        if (fArr == null || fArr.length == 0) {
            return;
        }
        if (this.mValues == null || this.mValues.length == 0) {
            setValues(PropertyValuesHolder.ofFloat("", fArr));
        } else {
            this.mValues[0].setFloatValues(fArr);
        }
        this.mInitialized = false;
    }

    public void setObjectValues(Object... objArr) {
        if (objArr == null || objArr.length == 0) {
            return;
        }
        if (this.mValues == null || this.mValues.length == 0) {
            setValues(PropertyValuesHolder.ofObject("", (TypeEvaluator) null, objArr));
        } else {
            this.mValues[0].setObjectValues(objArr);
        }
        this.mInitialized = false;
    }

    public void setValues(PropertyValuesHolder... propertyValuesHolderArr) {
        int length = propertyValuesHolderArr.length;
        this.mValues = propertyValuesHolderArr;
        this.mValuesMap = new HashMap<>(length);
        for (PropertyValuesHolder propertyValuesHolder : propertyValuesHolderArr) {
            this.mValuesMap.put(propertyValuesHolder.getPropertyName(), propertyValuesHolder);
        }
        this.mInitialized = false;
    }

    public PropertyValuesHolder[] getValues() {
        return this.mValues;
    }

    void initAnimation() {
        if (!this.mInitialized) {
            int length = this.mValues.length;
            for (int i = 0; i < length; i++) {
                this.mValues[i].init();
            }
            this.mInitialized = true;
        }
    }

    @Override
    public ValueAnimator setDuration(long j) {
        if (j < 0) {
            throw new IllegalArgumentException("Animators cannot have negative duration: " + j);
        }
        this.mDuration = j;
        return this;
    }

    public void overrideDurationScale(float f) {
        this.mDurationScale = f;
    }

    private float resolveDurationScale() {
        return this.mDurationScale >= 0.0f ? this.mDurationScale : sDurationScale;
    }

    private long getScaledDuration() {
        return (long) (this.mDuration * resolveDurationScale());
    }

    @Override
    public long getDuration() {
        return this.mDuration;
    }

    @Override
    public long getTotalDuration() {
        if (this.mRepeatCount == -1) {
            return -1L;
        }
        return this.mStartDelay + (this.mDuration * ((long) (this.mRepeatCount + 1)));
    }

    public void setCurrentPlayTime(long j) {
        setCurrentFraction(this.mDuration > 0 ? j / this.mDuration : 1.0f);
    }

    public void setCurrentFraction(float f) {
        initAnimation();
        float fClampFraction = clampFraction(f);
        this.mStartTimeCommitted = true;
        if (isPulsingInternal()) {
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis() - ((long) (getScaledDuration() * fClampFraction));
        } else {
            this.mSeekFraction = fClampFraction;
        }
        this.mOverallFraction = fClampFraction;
        animateValue(getCurrentIterationFraction(fClampFraction, this.mReversing));
    }

    private int getCurrentIteration(float f) {
        float fClampFraction = clampFraction(f);
        double d = fClampFraction;
        double dFloor = Math.floor(d);
        if (d == dFloor && fClampFraction > 0.0f) {
            dFloor -= 1.0d;
        }
        return (int) dFloor;
    }

    private float getCurrentIterationFraction(float f, boolean z) {
        float fClampFraction = clampFraction(f);
        int currentIteration = getCurrentIteration(fClampFraction);
        float f2 = fClampFraction - currentIteration;
        return shouldPlayBackward(currentIteration, z) ? 1.0f - f2 : f2;
    }

    private float clampFraction(float f) {
        if (f < 0.0f) {
            return 0.0f;
        }
        if (this.mRepeatCount != -1) {
            return Math.min(f, this.mRepeatCount + 1);
        }
        return f;
    }

    private boolean shouldPlayBackward(int i, boolean z) {
        if (i <= 0 || this.mRepeatMode != 2 || (i >= this.mRepeatCount + 1 && this.mRepeatCount != -1)) {
            return z;
        }
        return z ? i % 2 == 0 : i % 2 != 0;
    }

    public long getCurrentPlayTime() {
        if (!this.mInitialized) {
            return 0L;
        }
        if (!this.mStarted && this.mSeekFraction < 0.0f) {
            return 0L;
        }
        if (this.mSeekFraction >= 0.0f) {
            return (long) (this.mDuration * this.mSeekFraction);
        }
        float fResolveDurationScale = resolveDurationScale();
        if (fResolveDurationScale == 0.0f) {
            fResolveDurationScale = 1.0f;
        }
        return (long) ((AnimationUtils.currentAnimationTimeMillis() - this.mStartTime) / fResolveDurationScale);
    }

    @Override
    public long getStartDelay() {
        return this.mStartDelay;
    }

    @Override
    public void setStartDelay(long j) {
        if (j < 0) {
            Log.w(TAG, "Start delay should always be non-negative");
            j = 0;
        }
        this.mStartDelay = j;
    }

    public static long getFrameDelay() {
        AnimationHandler.getInstance();
        return AnimationHandler.getFrameDelay();
    }

    public static void setFrameDelay(long j) {
        AnimationHandler.getInstance();
        AnimationHandler.setFrameDelay(j);
    }

    public Object getAnimatedValue() {
        if (this.mValues != null && this.mValues.length > 0) {
            return this.mValues[0].getAnimatedValue();
        }
        return null;
    }

    public Object getAnimatedValue(String str) {
        PropertyValuesHolder propertyValuesHolder = this.mValuesMap.get(str);
        if (propertyValuesHolder != null) {
            return propertyValuesHolder.getAnimatedValue();
        }
        return null;
    }

    public void setRepeatCount(int i) {
        this.mRepeatCount = i;
    }

    public int getRepeatCount() {
        return this.mRepeatCount;
    }

    public void setRepeatMode(int i) {
        this.mRepeatMode = i;
    }

    public int getRepeatMode() {
        return this.mRepeatMode;
    }

    public void addUpdateListener(AnimatorUpdateListener animatorUpdateListener) {
        if (this.mUpdateListeners == null) {
            this.mUpdateListeners = new ArrayList<>();
        }
        this.mUpdateListeners.add(animatorUpdateListener);
    }

    public void removeAllUpdateListeners() {
        if (this.mUpdateListeners == null) {
            return;
        }
        this.mUpdateListeners.clear();
        this.mUpdateListeners = null;
    }

    public void removeUpdateListener(AnimatorUpdateListener animatorUpdateListener) {
        if (this.mUpdateListeners == null) {
            return;
        }
        this.mUpdateListeners.remove(animatorUpdateListener);
        if (this.mUpdateListeners.size() == 0) {
            this.mUpdateListeners = null;
        }
    }

    @Override
    public void setInterpolator(TimeInterpolator timeInterpolator) {
        if (timeInterpolator != null) {
            this.mInterpolator = timeInterpolator;
        } else {
            this.mInterpolator = new LinearInterpolator();
        }
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return this.mInterpolator;
    }

    public void setEvaluator(TypeEvaluator typeEvaluator) {
        if (typeEvaluator != null && this.mValues != null && this.mValues.length > 0) {
            this.mValues[0].setEvaluator(typeEvaluator);
        }
    }

    private void notifyStartListeners() {
        if (this.mListeners != null && !this.mStartListenersCalled) {
            ArrayList arrayList = (ArrayList) this.mListeners.clone();
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                ((Animator.AnimatorListener) arrayList.get(i)).onAnimationStart(this, this.mReversing);
            }
        }
        this.mStartListenersCalled = true;
    }

    private void start(boolean z) {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        this.mReversing = z;
        this.mSelfPulse = !this.mSuppressSelfPulseRequested;
        if (z && this.mSeekFraction != -1.0f && this.mSeekFraction != 0.0f) {
            if (this.mRepeatCount == -1) {
                this.mSeekFraction = 1.0f - ((float) (((double) this.mSeekFraction) - Math.floor(this.mSeekFraction)));
            } else {
                this.mSeekFraction = (this.mRepeatCount + 1) - this.mSeekFraction;
            }
        }
        this.mStarted = true;
        this.mPaused = false;
        this.mRunning = false;
        this.mAnimationEndRequested = false;
        this.mLastFrameTime = -1L;
        this.mFirstFrameTime = -1L;
        this.mStartTime = -1L;
        addAnimationCallback(0L);
        if (this.mStartDelay == 0 || this.mSeekFraction >= 0.0f || this.mReversing) {
            startAnimation();
            if (this.mSeekFraction == -1.0f) {
                setCurrentPlayTime(0L);
            } else {
                setCurrentFraction(this.mSeekFraction);
            }
        }
    }

    @Override
    void startWithoutPulsing(boolean z) {
        this.mSuppressSelfPulseRequested = true;
        if (z) {
            reverse();
        } else {
            start();
        }
        this.mSuppressSelfPulseRequested = false;
    }

    @Override
    public void start() {
        start(false);
    }

    @Override
    public void cancel() {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        if (this.mAnimationEndRequested) {
            return;
        }
        if ((this.mStarted || this.mRunning) && this.mListeners != null) {
            if (!this.mRunning) {
                notifyStartListeners();
            }
            Iterator it = ((ArrayList) this.mListeners.clone()).iterator();
            while (it.hasNext()) {
                ((Animator.AnimatorListener) it.next()).onAnimationCancel(this);
            }
        }
        endAnimation();
    }

    @Override
    public void end() {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        if (!this.mRunning) {
            startAnimation();
            this.mStarted = true;
        } else if (!this.mInitialized) {
            initAnimation();
        }
        animateValue(shouldPlayBackward(this.mRepeatCount, this.mReversing) ? 0.0f : 1.0f);
        endAnimation();
    }

    @Override
    public void resume() {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be resumed from the same thread that the animator was started on");
        }
        if (this.mPaused && !this.mResumed) {
            this.mResumed = true;
            if (this.mPauseTime > 0) {
                addAnimationCallback(0L);
            }
        }
        super.resume();
    }

    @Override
    public void pause() {
        boolean z = this.mPaused;
        super.pause();
        if (!z && this.mPaused) {
            this.mPauseTime = -1L;
            this.mResumed = false;
        }
    }

    @Override
    public boolean isRunning() {
        return this.mRunning;
    }

    @Override
    public boolean isStarted() {
        return this.mStarted;
    }

    @Override
    public void reverse() {
        if (isPulsingInternal()) {
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
            this.mStartTime = jCurrentAnimationTimeMillis - (getScaledDuration() - (jCurrentAnimationTimeMillis - this.mStartTime));
            this.mStartTimeCommitted = true;
            this.mReversing = !this.mReversing;
            return;
        }
        if (this.mStarted) {
            this.mReversing = !this.mReversing;
            end();
        } else {
            start(true);
        }
    }

    @Override
    public boolean canReverse() {
        return true;
    }

    private void endAnimation() {
        if (this.mAnimationEndRequested) {
            return;
        }
        removeAnimationCallback();
        boolean z = true;
        this.mAnimationEndRequested = true;
        this.mPaused = false;
        if ((!this.mStarted && !this.mRunning) || this.mListeners == null) {
            z = false;
        }
        if (z && !this.mRunning) {
            notifyStartListeners();
        }
        this.mRunning = false;
        this.mStarted = false;
        this.mStartListenersCalled = false;
        this.mLastFrameTime = -1L;
        this.mFirstFrameTime = -1L;
        this.mStartTime = -1L;
        if (z && this.mListeners != null) {
            ArrayList arrayList = (ArrayList) this.mListeners.clone();
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                ((Animator.AnimatorListener) arrayList.get(i)).onAnimationEnd(this, this.mReversing);
            }
        }
        this.mReversing = false;
        if (Trace.isTagEnabled(8L)) {
            Trace.asyncTraceEnd(8L, getNameForTrace(), System.identityHashCode(this));
        }
    }

    private void startAnimation() {
        if (Trace.isTagEnabled(8L)) {
            Trace.asyncTraceBegin(8L, getNameForTrace(), System.identityHashCode(this));
        }
        this.mAnimationEndRequested = false;
        initAnimation();
        this.mRunning = true;
        if (this.mSeekFraction >= 0.0f) {
            this.mOverallFraction = this.mSeekFraction;
        } else {
            this.mOverallFraction = 0.0f;
        }
        if (this.mListeners != null) {
            notifyStartListeners();
        }
    }

    private boolean isPulsingInternal() {
        return this.mLastFrameTime >= 0;
    }

    String getNameForTrace() {
        return "animator";
    }

    @Override
    public void commitAnimationFrame(long j) {
        if (!this.mStartTimeCommitted) {
            this.mStartTimeCommitted = true;
            long j2 = j - this.mLastFrameTime;
            if (j2 > 0) {
                this.mStartTime += j2;
            }
        }
    }

    boolean animateBasedOnTime(long j) {
        boolean z = false;
        if (this.mRunning) {
            long scaledDuration = getScaledDuration();
            float f = scaledDuration > 0 ? (j - this.mStartTime) / scaledDuration : 1.0f;
            boolean z2 = ((int) f) > ((int) this.mOverallFraction);
            boolean z3 = f >= ((float) (this.mRepeatCount + 1)) && this.mRepeatCount != -1;
            if (scaledDuration != 0) {
                if (z2 && !z3) {
                    if (this.mListeners != null) {
                        int size = this.mListeners.size();
                        for (int i = 0; i < size; i++) {
                            this.mListeners.get(i).onAnimationRepeat(this);
                        }
                    }
                } else if (z3) {
                    z = true;
                }
                this.mOverallFraction = clampFraction(f);
                animateValue(getCurrentIterationFraction(this.mOverallFraction, this.mReversing));
            }
        }
        return z;
    }

    @Override
    void animateBasedOnPlayTime(long j, long j2, boolean z) {
        if (j < 0 || j2 < 0) {
            throw new UnsupportedOperationException("Error: Play time should never be negative.");
        }
        initAnimation();
        if (this.mRepeatCount > 0) {
            if (Math.min((int) (j / this.mDuration), this.mRepeatCount) != Math.min((int) (j2 / this.mDuration), this.mRepeatCount) && this.mListeners != null) {
                int size = this.mListeners.size();
                for (int i = 0; i < size; i++) {
                    this.mListeners.get(i).onAnimationRepeat(this);
                }
            }
        }
        if (this.mRepeatCount != -1 && j >= ((long) (this.mRepeatCount + 1)) * this.mDuration) {
            skipToEndValue(z);
        } else {
            animateValue(getCurrentIterationFraction(j / this.mDuration, z));
        }
    }

    @Override
    void skipToEndValue(boolean z) {
        float f;
        initAnimation();
        if (!z) {
            f = 1.0f;
        } else {
            f = 0.0f;
        }
        if (this.mRepeatCount % 2 == 1 && this.mRepeatMode == 2) {
            f = 0.0f;
        }
        animateValue(f);
    }

    @Override
    boolean isInitialized() {
        return this.mInitialized;
    }

    @Override
    public final boolean doAnimationFrame(long j) {
        if (this.mStartTime < 0) {
            this.mStartTime = this.mReversing ? j : ((long) (this.mStartDelay * resolveDurationScale())) + j;
        }
        if (this.mPaused) {
            this.mPauseTime = j;
            removeAnimationCallback();
            return false;
        }
        if (this.mResumed) {
            this.mResumed = false;
            if (this.mPauseTime > 0) {
                this.mStartTime += j - this.mPauseTime;
            }
        }
        if (!this.mRunning) {
            if (this.mStartTime > j && this.mSeekFraction == -1.0f) {
                return false;
            }
            this.mRunning = true;
            startAnimation();
        }
        if (this.mLastFrameTime < 0) {
            if (this.mSeekFraction >= 0.0f) {
                this.mStartTime = j - ((long) (getScaledDuration() * this.mSeekFraction));
                this.mSeekFraction = -1.0f;
            }
            this.mStartTimeCommitted = false;
        }
        this.mLastFrameTime = j;
        boolean zAnimateBasedOnTime = animateBasedOnTime(Math.max(j, this.mStartTime));
        if (zAnimateBasedOnTime) {
            endAnimation();
        }
        return zAnimateBasedOnTime;
    }

    @Override
    boolean pulseAnimationFrame(long j) {
        if (this.mSelfPulse) {
            return false;
        }
        return doAnimationFrame(j);
    }

    private void addOneShotCommitCallback() {
        if (!this.mSelfPulse) {
            return;
        }
        getAnimationHandler().addOneShotCommitCallback(this);
    }

    private void removeAnimationCallback() {
        if (!this.mSelfPulse) {
            return;
        }
        getAnimationHandler().removeCallback(this);
    }

    private void addAnimationCallback(long j) {
        if (!this.mSelfPulse) {
            return;
        }
        getAnimationHandler().addAnimationFrameCallback(this, j);
    }

    public float getAnimatedFraction() {
        return this.mCurrentFraction;
    }

    void animateValue(float f) {
        float interpolation = this.mInterpolator.getInterpolation(f);
        this.mCurrentFraction = interpolation;
        int length = this.mValues.length;
        for (int i = 0; i < length; i++) {
            this.mValues[i].calculateValue(interpolation);
        }
        if (this.mUpdateListeners != null) {
            int size = this.mUpdateListeners.size();
            for (int i2 = 0; i2 < size; i2++) {
                this.mUpdateListeners.get(i2).onAnimationUpdate(this);
            }
        }
    }

    @Override
    public ValueAnimator mo0clone() {
        ValueAnimator valueAnimator = (ValueAnimator) super.mo0clone();
        if (this.mUpdateListeners != null) {
            valueAnimator.mUpdateListeners = new ArrayList<>(this.mUpdateListeners);
        }
        valueAnimator.mSeekFraction = -1.0f;
        valueAnimator.mReversing = false;
        valueAnimator.mInitialized = false;
        valueAnimator.mStarted = false;
        valueAnimator.mRunning = false;
        valueAnimator.mPaused = false;
        valueAnimator.mResumed = false;
        valueAnimator.mStartListenersCalled = false;
        valueAnimator.mStartTime = -1L;
        valueAnimator.mStartTimeCommitted = false;
        valueAnimator.mAnimationEndRequested = false;
        valueAnimator.mPauseTime = -1L;
        valueAnimator.mLastFrameTime = -1L;
        valueAnimator.mFirstFrameTime = -1L;
        valueAnimator.mOverallFraction = 0.0f;
        valueAnimator.mCurrentFraction = 0.0f;
        valueAnimator.mSelfPulse = true;
        valueAnimator.mSuppressSelfPulseRequested = false;
        PropertyValuesHolder[] propertyValuesHolderArr = this.mValues;
        if (propertyValuesHolderArr != null) {
            int length = propertyValuesHolderArr.length;
            valueAnimator.mValues = new PropertyValuesHolder[length];
            valueAnimator.mValuesMap = new HashMap<>(length);
            for (int i = 0; i < length; i++) {
                PropertyValuesHolder propertyValuesHolderClone = propertyValuesHolderArr[i].mo6clone();
                valueAnimator.mValues[i] = propertyValuesHolderClone;
                valueAnimator.mValuesMap.put(propertyValuesHolderClone.getPropertyName(), propertyValuesHolderClone);
            }
        }
        return valueAnimator;
    }

    public static int getCurrentAnimationsCount() {
        return AnimationHandler.getAnimationCount();
    }

    public String toString() {
        String str = "ValueAnimator@" + Integer.toHexString(hashCode());
        if (this.mValues != null) {
            for (int i = 0; i < this.mValues.length; i++) {
                str = str + "\n    " + this.mValues[i].toString();
            }
        }
        return str;
    }

    @Override
    public void setAllowRunningAsynchronously(boolean z) {
    }

    public AnimationHandler getAnimationHandler() {
        return AnimationHandler.getInstance();
    }
}
