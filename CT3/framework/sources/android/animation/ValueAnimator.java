package android.animation;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.net.ProxyInfo;
import android.os.Looper;
import android.os.Trace;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.HashMap;

public class ValueAnimator extends Animator implements AnimationHandler.AnimationFrameCallback {
    private static final boolean DEBUG = false;
    public static final int INFINITE = -1;
    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    private static final String TAG = "ValueAnimator";
    private long mPauseTime;
    private boolean mReversing;
    long mStartTime;
    boolean mStartTimeCommitted;
    PropertyValuesHolder[] mValues;
    HashMap<String, PropertyValuesHolder> mValuesMap;
    private static float sDurationScale = 1.0f;
    private static final TimeInterpolator sDefaultInterpolator = new AccelerateDecelerateInterpolator();
    float mSeekFraction = -1.0f;
    private boolean mResumed = false;
    private float mOverallFraction = 0.0f;
    private float mCurrentFraction = 0.0f;
    private long mLastFrameTime = 0;
    private boolean mRunning = false;
    private boolean mStarted = false;
    private boolean mStartListenersCalled = false;
    boolean mInitialized = false;
    private boolean mAnimationEndRequested = false;
    private long mDuration = 300;
    private long mStartDelay = 0;
    private int mRepeatCount = 0;
    private int mRepeatMode = 1;
    private TimeInterpolator mInterpolator = sDefaultInterpolator;
    ArrayList<AnimatorUpdateListener> mUpdateListeners = null;

    public interface AnimatorUpdateListener {
        void onAnimationUpdate(ValueAnimator valueAnimator);
    }

    public static void setDurationScale(float durationScale) {
        sDurationScale = durationScale;
    }

    public static float getDurationScale() {
        return sDurationScale;
    }

    public static ValueAnimator ofInt(int... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(values);
        return anim;
    }

    public static ValueAnimator ofArgb(int... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(values);
        anim.setEvaluator(ArgbEvaluator.getInstance());
        return anim;
    }

    public static ValueAnimator ofFloat(float... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setFloatValues(values);
        return anim;
    }

    public static ValueAnimator ofPropertyValuesHolder(PropertyValuesHolder... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setValues(values);
        return anim;
    }

    public static ValueAnimator ofObject(TypeEvaluator evaluator, Object... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setObjectValues(values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    public void setIntValues(int... values) {
        if (values == null || values.length == 0) {
            return;
        }
        if (this.mValues == null || this.mValues.length == 0) {
            setValues(PropertyValuesHolder.ofInt(ProxyInfo.LOCAL_EXCL_LIST, values));
        } else {
            PropertyValuesHolder valuesHolder = this.mValues[0];
            valuesHolder.setIntValues(values);
        }
        this.mInitialized = false;
    }

    public void setFloatValues(float... values) {
        if (values == null || values.length == 0) {
            return;
        }
        if (this.mValues == null || this.mValues.length == 0) {
            setValues(PropertyValuesHolder.ofFloat(ProxyInfo.LOCAL_EXCL_LIST, values));
        } else {
            PropertyValuesHolder valuesHolder = this.mValues[0];
            valuesHolder.setFloatValues(values);
        }
        this.mInitialized = false;
    }

    public void setObjectValues(Object... values) {
        if (values == null || values.length == 0) {
            return;
        }
        if (this.mValues == null || this.mValues.length == 0) {
            setValues(PropertyValuesHolder.ofObject(ProxyInfo.LOCAL_EXCL_LIST, (TypeEvaluator) null, values));
        } else {
            PropertyValuesHolder valuesHolder = this.mValues[0];
            valuesHolder.setObjectValues(values);
        }
        this.mInitialized = false;
    }

    public void setValues(PropertyValuesHolder... values) {
        int numValues = values.length;
        this.mValues = values;
        this.mValuesMap = new HashMap<>(numValues);
        for (PropertyValuesHolder valuesHolder : values) {
            this.mValuesMap.put(valuesHolder.getPropertyName(), valuesHolder);
        }
        this.mInitialized = false;
    }

    public PropertyValuesHolder[] getValues() {
        return this.mValues;
    }

    void initAnimation() {
        if (this.mInitialized) {
            return;
        }
        int numValues = this.mValues.length;
        for (int i = 0; i < numValues; i++) {
            this.mValues[i].init();
        }
        this.mInitialized = true;
    }

    @Override
    public ValueAnimator setDuration(long duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("Animators cannot have negative duration: " + duration);
        }
        this.mDuration = duration;
        return this;
    }

    private long getScaledDuration() {
        return (long) (this.mDuration * sDurationScale);
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

    public void setCurrentPlayTime(long playTime) {
        float fraction = this.mDuration > 0 ? playTime / this.mDuration : 1.0f;
        setCurrentFraction(fraction);
    }

    public void setCurrentFraction(float fraction) {
        initAnimation();
        float fraction2 = clampFraction(fraction);
        long seekTime = (long) (getScaledDuration() * fraction2);
        long currentTime = AnimationUtils.currentAnimationTimeMillis();
        this.mStartTime = currentTime - seekTime;
        this.mStartTimeCommitted = true;
        if (!isPulsingInternal()) {
            this.mSeekFraction = fraction2;
        }
        this.mOverallFraction = fraction2;
        float currentIterationFraction = getCurrentIterationFraction(fraction2);
        animateValue(currentIterationFraction);
    }

    private int getCurrentIteration(float fraction) {
        float fraction2 = clampFraction(fraction);
        double iteration = Math.floor(fraction2);
        if (fraction2 == iteration && fraction2 > 0.0f) {
            iteration -= 1.0d;
        }
        return (int) iteration;
    }

    private float getCurrentIterationFraction(float fraction) {
        float fraction2 = clampFraction(fraction);
        int iteration = getCurrentIteration(fraction2);
        float currentFraction = fraction2 - iteration;
        return shouldPlayBackward(iteration) ? 1.0f - currentFraction : currentFraction;
    }

    private float clampFraction(float fraction) {
        if (fraction < 0.0f) {
            return 0.0f;
        }
        if (this.mRepeatCount != -1) {
            return Math.min(fraction, this.mRepeatCount + 1);
        }
        return fraction;
    }

    private boolean shouldPlayBackward(int iteration) {
        if (iteration <= 0 || this.mRepeatMode != 2 || (iteration >= this.mRepeatCount + 1 && this.mRepeatCount != -1)) {
            return this.mReversing;
        }
        return this.mReversing ? iteration % 2 == 0 : iteration % 2 != 0;
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
        float durationScale = sDurationScale == 0.0f ? 1.0f : sDurationScale;
        return (long) ((AnimationUtils.currentAnimationTimeMillis() - this.mStartTime) / durationScale);
    }

    @Override
    public long getStartDelay() {
        return this.mStartDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        if (startDelay < 0) {
            Log.w(TAG, "Start delay should always be non-negative");
            startDelay = 0;
        }
        this.mStartDelay = startDelay;
    }

    public static long getFrameDelay() {
        AnimationHandler.getInstance();
        return AnimationHandler.getFrameDelay();
    }

    public static void setFrameDelay(long frameDelay) {
        AnimationHandler.getInstance();
        AnimationHandler.setFrameDelay(frameDelay);
    }

    public Object getAnimatedValue() {
        if (this.mValues == null || this.mValues.length <= 0) {
            return null;
        }
        return this.mValues[0].getAnimatedValue();
    }

    public Object getAnimatedValue(String propertyName) {
        PropertyValuesHolder valuesHolder = this.mValuesMap.get(propertyName);
        if (valuesHolder != null) {
            return valuesHolder.getAnimatedValue();
        }
        return null;
    }

    public void setRepeatCount(int value) {
        this.mRepeatCount = value;
    }

    public int getRepeatCount() {
        return this.mRepeatCount;
    }

    public void setRepeatMode(int value) {
        this.mRepeatMode = value;
    }

    public int getRepeatMode() {
        return this.mRepeatMode;
    }

    public void addUpdateListener(AnimatorUpdateListener listener) {
        if (this.mUpdateListeners == null) {
            this.mUpdateListeners = new ArrayList<>();
        }
        this.mUpdateListeners.add(listener);
    }

    public void removeAllUpdateListeners() {
        if (this.mUpdateListeners == null) {
            return;
        }
        this.mUpdateListeners.clear();
        this.mUpdateListeners = null;
    }

    public void removeUpdateListener(AnimatorUpdateListener listener) {
        if (this.mUpdateListeners == null) {
            return;
        }
        this.mUpdateListeners.remove(listener);
        if (this.mUpdateListeners.size() != 0) {
            return;
        }
        this.mUpdateListeners = null;
    }

    @Override
    public void setInterpolator(TimeInterpolator value) {
        if (value != null) {
            this.mInterpolator = value;
        } else {
            this.mInterpolator = new LinearInterpolator();
        }
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return this.mInterpolator;
    }

    public void setEvaluator(TypeEvaluator value) {
        if (value == null || this.mValues == null || this.mValues.length <= 0) {
            return;
        }
        this.mValues[0].setEvaluator(value);
    }

    private void notifyStartListeners() {
        if (this.mListeners != null && !this.mStartListenersCalled) {
            ArrayList<Animator.AnimatorListener> tmpListeners = (ArrayList) this.mListeners.clone();
            int numListeners = tmpListeners.size();
            for (int i = 0; i < numListeners; i++) {
                tmpListeners.get(i).onAnimationStart(this);
            }
        }
        this.mStartListenersCalled = true;
    }

    private void start(boolean playBackwards) {
        if (Looper.myLooper() == null) {
            throw new AndroidRuntimeException("Animators may only be run on Looper threads");
        }
        this.mReversing = playBackwards;
        if (playBackwards && this.mSeekFraction != -1.0f && this.mSeekFraction != 0.0f) {
            if (this.mRepeatCount == -1) {
                float fraction = (float) (((double) this.mSeekFraction) - Math.floor(this.mSeekFraction));
                this.mSeekFraction = 1.0f - fraction;
            } else {
                this.mSeekFraction = (this.mRepeatCount + 1) - this.mSeekFraction;
            }
        }
        this.mStarted = true;
        this.mPaused = false;
        this.mRunning = false;
        this.mLastFrameTime = 0L;
        AnimationHandler animationHandler = AnimationHandler.getInstance();
        animationHandler.addAnimationFrameCallback(this, (long) (this.mStartDelay * sDurationScale));
        if (this.mStartDelay != 0 && this.mSeekFraction < 0.0f) {
            return;
        }
        startAnimation();
        if (this.mSeekFraction == -1.0f) {
            setCurrentPlayTime(0L);
        } else {
            setCurrentFraction(this.mSeekFraction);
        }
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
            ArrayList<Animator.AnimatorListener> tmpListeners = (ArrayList) this.mListeners.clone();
            for (Animator.AnimatorListener listener : tmpListeners) {
                listener.onAnimationCancel(this);
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
        animateValue(shouldPlayBackward(this.mRepeatCount) ? 0.0f : 1.0f);
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
                AnimationHandler handler = AnimationHandler.getInstance();
                handler.addAnimationFrameCallback(this, 0L);
            }
        }
        super.resume();
    }

    @Override
    public void pause() {
        boolean previouslyPaused = this.mPaused;
        super.pause();
        if (previouslyPaused || !this.mPaused) {
            return;
        }
        this.mPauseTime = -1L;
        this.mResumed = false;
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
            long currentTime = AnimationUtils.currentAnimationTimeMillis();
            long currentPlayTime = currentTime - this.mStartTime;
            long timeLeft = getScaledDuration() - currentPlayTime;
            this.mStartTime = currentTime - timeLeft;
            this.mStartTimeCommitted = true;
            this.mReversing = this.mReversing ? false : true;
            return;
        }
        if (this.mStarted) {
            this.mReversing = this.mReversing ? false : true;
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
        AnimationHandler handler = AnimationHandler.getInstance();
        handler.removeCallback(this);
        this.mAnimationEndRequested = true;
        this.mPaused = false;
        if ((this.mStarted || this.mRunning) && this.mListeners != null) {
            if (!this.mRunning) {
                notifyStartListeners();
            }
            ArrayList<Animator.AnimatorListener> tmpListeners = (ArrayList) this.mListeners.clone();
            int numListeners = tmpListeners.size();
            for (int i = 0; i < numListeners; i++) {
                tmpListeners.get(i).onAnimationEnd(this);
            }
        }
        this.mRunning = false;
        this.mStarted = false;
        this.mStartListenersCalled = false;
        this.mReversing = false;
        this.mLastFrameTime = 0L;
        if (!Trace.isTagEnabled(8L)) {
            return;
        }
        Trace.asyncTraceEnd(8L, getNameForTrace(), System.identityHashCode(this));
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
        if (this.mListeners == null) {
            return;
        }
        notifyStartListeners();
    }

    private boolean isPulsingInternal() {
        return this.mLastFrameTime > 0;
    }

    String getNameForTrace() {
        return "animator";
    }

    @Override
    public void commitAnimationFrame(long frameTime) {
        if (this.mStartTimeCommitted) {
            return;
        }
        this.mStartTimeCommitted = true;
        long adjustment = frameTime - this.mLastFrameTime;
        if (adjustment <= 0) {
            return;
        }
        this.mStartTime += adjustment;
    }

    boolean animateBasedOnTime(long currentTime) {
        boolean done = false;
        if (this.mRunning) {
            long scaledDuration = getScaledDuration();
            float fraction = scaledDuration > 0 ? (currentTime - this.mStartTime) / scaledDuration : 1.0f;
            float lastFraction = this.mOverallFraction;
            boolean newIteration = ((int) fraction) > ((int) lastFraction);
            boolean lastIterationFinished = fraction >= ((float) (this.mRepeatCount + 1)) && this.mRepeatCount != -1;
            if (scaledDuration == 0) {
                done = true;
            } else if (newIteration && !lastIterationFinished) {
                if (this.mListeners != null) {
                    int numListeners = this.mListeners.size();
                    for (int i = 0; i < numListeners; i++) {
                        this.mListeners.get(i).onAnimationRepeat(this);
                    }
                }
            } else if (lastIterationFinished) {
                done = true;
            }
            this.mOverallFraction = clampFraction(fraction);
            float currentIterationFraction = getCurrentIterationFraction(this.mOverallFraction);
            animateValue(currentIterationFraction);
        }
        return done;
    }

    @Override
    public final void doAnimationFrame(long frameTime) {
        AnimationHandler handler = AnimationHandler.getInstance();
        if (this.mLastFrameTime == 0) {
            handler.addOneShotCommitCallback(this);
            if (this.mStartDelay > 0) {
                startAnimation();
            }
            if (this.mSeekFraction < 0.0f) {
                this.mStartTime = frameTime;
            } else {
                long seekTime = (long) (getScaledDuration() * this.mSeekFraction);
                this.mStartTime = frameTime - seekTime;
                this.mSeekFraction = -1.0f;
            }
            this.mStartTimeCommitted = false;
        }
        this.mLastFrameTime = frameTime;
        if (this.mPaused) {
            this.mPauseTime = frameTime;
            handler.removeCallback(this);
            return;
        }
        if (this.mResumed) {
            this.mResumed = false;
            if (this.mPauseTime > 0) {
                this.mStartTime += frameTime - this.mPauseTime;
                this.mStartTimeCommitted = false;
            }
            handler.addOneShotCommitCallback(this);
        }
        long currentTime = Math.max(frameTime, this.mStartTime);
        boolean finished = animateBasedOnTime(currentTime);
        if (!finished) {
            return;
        }
        endAnimation();
    }

    public float getAnimatedFraction() {
        return this.mCurrentFraction;
    }

    void animateValue(float fraction) {
        float fraction2 = this.mInterpolator.getInterpolation(fraction);
        this.mCurrentFraction = fraction2;
        int numValues = this.mValues.length;
        for (int i = 0; i < numValues; i++) {
            this.mValues[i].calculateValue(fraction2);
        }
        if (this.mUpdateListeners == null) {
            return;
        }
        int numListeners = this.mUpdateListeners.size();
        for (int i2 = 0; i2 < numListeners; i2++) {
            this.mUpdateListeners.get(i2).onAnimationUpdate(this);
        }
    }

    @Override
    public ValueAnimator m35clone() {
        ValueAnimator anim = (ValueAnimator) super.m35clone();
        if (this.mUpdateListeners != null) {
            anim.mUpdateListeners = new ArrayList<>(this.mUpdateListeners);
        }
        anim.mSeekFraction = -1.0f;
        anim.mReversing = false;
        anim.mInitialized = false;
        anim.mStarted = false;
        anim.mRunning = false;
        anim.mPaused = false;
        anim.mResumed = false;
        anim.mStartListenersCalled = false;
        anim.mStartTime = 0L;
        anim.mStartTimeCommitted = false;
        anim.mAnimationEndRequested = false;
        anim.mPauseTime = 0L;
        anim.mLastFrameTime = 0L;
        anim.mOverallFraction = 0.0f;
        anim.mCurrentFraction = 0.0f;
        PropertyValuesHolder[] oldValues = this.mValues;
        if (oldValues != null) {
            int numValues = oldValues.length;
            anim.mValues = new PropertyValuesHolder[numValues];
            anim.mValuesMap = new HashMap<>(numValues);
            for (int i = 0; i < numValues; i++) {
                PropertyValuesHolder newValuesHolder = oldValues[i].m81clone();
                anim.mValues[i] = newValuesHolder;
                anim.mValuesMap.put(newValuesHolder.getPropertyName(), newValuesHolder);
            }
        }
        return anim;
    }

    public static int getCurrentAnimationsCount() {
        return AnimationHandler.getAnimationCount();
    }

    public String toString() {
        String returnVal = "ValueAnimator@" + Integer.toHexString(hashCode());
        if (this.mValues != null) {
            for (int i = 0; i < this.mValues.length; i++) {
                returnVal = returnVal + "\n    " + this.mValues[i].toString();
            }
        }
        return returnVal;
    }

    @Override
    public void setAllowRunningAsynchronously(boolean mayRunAsync) {
    }
}
