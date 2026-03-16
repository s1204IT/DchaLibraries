package com.android.internal.widget.multiwaveview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.R;
import com.android.internal.widget.multiwaveview.Ease;
import java.util.ArrayList;

public class GlowPadView extends View {
    private static final boolean DEBUG = false;
    private static final int HIDE_ANIMATION_DELAY = 200;
    private static final int HIDE_ANIMATION_DURATION = 200;
    private static final int INITIAL_SHOW_HANDLE_DURATION = 200;
    private static final int RETURN_TO_HOME_DELAY = 1200;
    private static final int RETURN_TO_HOME_DURATION = 200;
    private static final int REVEAL_GLOW_DELAY = 0;
    private static final int REVEAL_GLOW_DURATION = 0;
    private static final float RING_SCALE_COLLAPSED = 0.5f;
    private static final float RING_SCALE_EXPANDED = 1.0f;
    private static final int SHOW_ANIMATION_DELAY = 50;
    private static final int SHOW_ANIMATION_DURATION = 200;
    private static final float SNAP_MARGIN_DEFAULT = 20.0f;
    private static final int STATE_FINISH = 5;
    private static final int STATE_FIRST_TOUCH = 2;
    private static final int STATE_IDLE = 0;
    private static final int STATE_SNAP = 4;
    private static final int STATE_START = 1;
    private static final int STATE_TRACKING = 3;
    private static final String TAG = "GlowPadView";
    private static final float TAP_RADIUS_SCALE_ACCESSIBILITY_ENABLED = 1.3f;
    private static final float TARGET_SCALE_COLLAPSED = 0.8f;
    private static final float TARGET_SCALE_EXPANDED = 1.0f;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private static final int WAVE_ANIMATION_DURATION = 1000;
    private int mActiveTarget;
    private boolean mAllowScaling;
    private boolean mAlwaysTrackFinger;
    private boolean mAnimatingTargets;
    private Tweener mBackgroundAnimator;
    private ArrayList<String> mDirectionDescriptions;
    private int mDirectionDescriptionsResourceId;
    private boolean mDragging;
    private int mFeedbackCount;
    private float mFirstItemOffset;
    private AnimationBundle mGlowAnimations;
    private float mGlowRadius;
    private int mGrabbedState;
    private int mGravity;
    private TargetDrawable mHandleDrawable;
    private int mHorizontalInset;
    private boolean mInitialLayout;
    private float mInnerRadius;
    private boolean mMagneticTargets;
    private int mMaxTargetHeight;
    private int mMaxTargetWidth;
    private int mNewTargetResources;
    private OnTriggerListener mOnTriggerListener;
    private float mOuterRadius;
    private TargetDrawable mOuterRing;
    private PointCloud mPointCloud;
    private int mPointerId;
    private Animator.AnimatorListener mResetListener;
    private Animator.AnimatorListener mResetListenerWithPing;
    private float mRingScaleFactor;
    private float mSnapMargin;
    private AnimationBundle mTargetAnimations;
    private ArrayList<String> mTargetDescriptions;
    private int mTargetDescriptionsResourceId;
    private ArrayList<TargetDrawable> mTargetDrawables;
    private int mTargetResourceId;
    private Animator.AnimatorListener mTargetUpdateListener;
    private ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private int mVerticalInset;
    private int mVibrationDuration;
    private Vibrator mVibrator;
    private AnimationBundle mWaveAnimations;
    private float mWaveCenterX;
    private float mWaveCenterY;

    public interface OnTriggerListener {
        public static final int CENTER_HANDLE = 1;
        public static final int NO_HANDLE = 0;

        void onFinishFinalAnimation();

        void onGrabbed(View view, int i);

        void onGrabbedStateChange(View view, int i);

        void onReleased(View view, int i);

        void onTrigger(View view, int i);
    }

    private class AnimationBundle extends ArrayList<Tweener> {
        private static final long serialVersionUID = -6319262269245852568L;
        private boolean mSuspended;

        private AnimationBundle() {
        }

        public void start() {
            if (!this.mSuspended) {
                int count = size();
                for (int i = 0; i < count; i++) {
                    Tweener anim = get(i);
                    anim.animator.start();
                }
            }
        }

        public void cancel() {
            int count = size();
            for (int i = 0; i < count; i++) {
                Tweener anim = get(i);
                anim.animator.cancel();
            }
            clear();
        }

        public void stop() {
            int count = size();
            for (int i = 0; i < count; i++) {
                Tweener anim = get(i);
                anim.animator.end();
            }
            clear();
        }

        public void setSuspended(boolean suspend) {
            this.mSuspended = suspend;
        }
    }

    public GlowPadView(Context context) {
        this(context, null);
    }

    public GlowPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTargetDrawables = new ArrayList<>();
        this.mWaveAnimations = new AnimationBundle();
        this.mTargetAnimations = new AnimationBundle();
        this.mGlowAnimations = new AnimationBundle();
        this.mFeedbackCount = 3;
        this.mVibrationDuration = 0;
        this.mActiveTarget = -1;
        this.mRingScaleFactor = 1.0f;
        this.mOuterRadius = 0.0f;
        this.mSnapMargin = 0.0f;
        this.mFirstItemOffset = 0.0f;
        this.mMagneticTargets = false;
        this.mResetListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                GlowPadView.this.switchToState(0, GlowPadView.this.mWaveCenterX, GlowPadView.this.mWaveCenterY);
                GlowPadView.this.dispatchOnFinishFinalAnimation();
            }
        };
        this.mResetListenerWithPing = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                GlowPadView.this.ping();
                GlowPadView.this.switchToState(0, GlowPadView.this.mWaveCenterX, GlowPadView.this.mWaveCenterY);
                GlowPadView.this.dispatchOnFinishFinalAnimation();
            }
        };
        this.mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                GlowPadView.this.invalidate();
            }
        };
        this.mTargetUpdateListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (GlowPadView.this.mNewTargetResources != 0) {
                    GlowPadView.this.internalSetTargetResources(GlowPadView.this.mNewTargetResources);
                    GlowPadView.this.mNewTargetResources = 0;
                    GlowPadView.this.hideTargets(false, false);
                }
                GlowPadView.this.mAnimatingTargets = false;
            }
        };
        this.mGravity = 48;
        this.mInitialLayout = true;
        Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlowPadView);
        this.mInnerRadius = a.getDimension(1, this.mInnerRadius);
        this.mOuterRadius = a.getDimension(8, this.mOuterRadius);
        this.mSnapMargin = a.getDimension(11, this.mSnapMargin);
        this.mFirstItemOffset = (float) Math.toRadians(a.getFloat(14, (float) Math.toDegrees(this.mFirstItemOffset)));
        this.mVibrationDuration = a.getInt(10, this.mVibrationDuration);
        this.mFeedbackCount = a.getInt(12, this.mFeedbackCount);
        this.mAllowScaling = a.getBoolean(16, false);
        TypedValue handle = a.peekValue(5);
        this.mHandleDrawable = new TargetDrawable(res, handle != null ? handle.resourceId : 0);
        this.mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
        this.mOuterRing = new TargetDrawable(res, getResourceId(a, 6));
        this.mAlwaysTrackFinger = a.getBoolean(13, false);
        this.mMagneticTargets = a.getBoolean(15, this.mMagneticTargets);
        int pointId = getResourceId(a, 7);
        Drawable pointDrawable = pointId != 0 ? context.getDrawable(pointId) : null;
        this.mGlowRadius = a.getDimension(9, 0.0f);
        this.mPointCloud = new PointCloud(pointDrawable);
        this.mPointCloud.makePointCloud(this.mInnerRadius, this.mOuterRadius);
        this.mPointCloud.glowManager.setRadius(this.mGlowRadius);
        TypedValue outValue = new TypedValue();
        if (a.getValue(4, outValue)) {
            internalSetTargetResources(outValue.resourceId);
        }
        if (this.mTargetDrawables == null || this.mTargetDrawables.size() == 0) {
            throw new IllegalStateException("Must specify at least one target drawable");
        }
        if (a.getValue(2, outValue)) {
            int resourceId = outValue.resourceId;
            if (resourceId == 0) {
                throw new IllegalStateException("Must specify target descriptions");
            }
            setTargetDescriptionsResourceId(resourceId);
        }
        if (a.getValue(3, outValue)) {
            int resourceId2 = outValue.resourceId;
            if (resourceId2 == 0) {
                throw new IllegalStateException("Must specify direction descriptions");
            }
            setDirectionDescriptionsResourceId(resourceId2);
        }
        this.mGravity = a.getInt(0, 48);
        a.recycle();
        setVibrateEnabled(this.mVibrationDuration > 0);
        assignDefaultsIfNeeded();
    }

    private int getResourceId(TypedArray a, int id) {
        TypedValue tv = a.peekValue(id);
        if (tv == null) {
            return 0;
        }
        return tv.resourceId;
    }

    private void dump() {
        Log.v(TAG, "Outer Radius = " + this.mOuterRadius);
        Log.v(TAG, "SnapMargin = " + this.mSnapMargin);
        Log.v(TAG, "FeedbackCount = " + this.mFeedbackCount);
        Log.v(TAG, "VibrationDuration = " + this.mVibrationDuration);
        Log.v(TAG, "GlowRadius = " + this.mGlowRadius);
        Log.v(TAG, "WaveCenterX = " + this.mWaveCenterX);
        Log.v(TAG, "WaveCenterY = " + this.mWaveCenterY);
    }

    public void suspendAnimations() {
        this.mWaveAnimations.setSuspended(true);
        this.mTargetAnimations.setSuspended(true);
        this.mGlowAnimations.setSuspended(true);
    }

    public void resumeAnimations() {
        this.mWaveAnimations.setSuspended(false);
        this.mTargetAnimations.setSuspended(false);
        this.mGlowAnimations.setSuspended(false);
        this.mWaveAnimations.start();
        this.mTargetAnimations.start();
        this.mGlowAnimations.start();
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return (int) (Math.max(this.mOuterRing.getWidth(), 2.0f * this.mOuterRadius) + this.mMaxTargetWidth);
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return (int) (Math.max(this.mOuterRing.getHeight(), 2.0f * this.mOuterRadius) + this.mMaxTargetHeight);
    }

    protected int getScaledSuggestedMinimumWidth() {
        return (int) ((this.mRingScaleFactor * Math.max(this.mOuterRing.getWidth(), 2.0f * this.mOuterRadius)) + this.mMaxTargetWidth);
    }

    protected int getScaledSuggestedMinimumHeight() {
        return (int) ((this.mRingScaleFactor * Math.max(this.mOuterRing.getHeight(), 2.0f * this.mOuterRadius)) + this.mMaxTargetHeight);
    }

    private int resolveMeasured(int measureSpec, int desired) {
        int specSize = View.MeasureSpec.getSize(measureSpec);
        switch (View.MeasureSpec.getMode(measureSpec)) {
            case Integer.MIN_VALUE:
                int result = Math.min(specSize, desired);
                return result;
            case 0:
                return desired;
            default:
                return specSize;
        }
    }

    private void switchToState(int state, float x, float y) {
        switch (state) {
            case 0:
                deactivateTargets();
                hideGlow(0, 0, 0.0f, null);
                startBackgroundAnimation(0, 0.0f);
                this.mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
                this.mHandleDrawable.setAlpha(1.0f);
                break;
            case 1:
                startBackgroundAnimation(0, 0.0f);
                break;
            case 2:
                this.mHandleDrawable.setAlpha(0.0f);
                deactivateTargets();
                showTargets(true);
                startBackgroundAnimation(200, 1.0f);
                setGrabbedState(1);
                if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                    announceTargets();
                }
                break;
            case 3:
                this.mHandleDrawable.setAlpha(0.0f);
                showGlow(0, 0, 1.0f, null);
                break;
            case 4:
                this.mHandleDrawable.setAlpha(0.0f);
                showGlow(0, 0, 0.0f, null);
                break;
            case 5:
                doFinish();
                break;
        }
    }

    private void showGlow(int duration, int delay, float finalAlpha, Animator.AnimatorListener finishListener) {
        this.mGlowAnimations.cancel();
        this.mGlowAnimations.add(Tweener.to(this.mPointCloud.glowManager, duration, "ease", Ease.Cubic.easeIn, "delay", Integer.valueOf(delay), "alpha", Float.valueOf(finalAlpha), "onUpdate", this.mUpdateListener, "onComplete", finishListener));
        this.mGlowAnimations.start();
    }

    private void hideGlow(int duration, int delay, float finalAlpha, Animator.AnimatorListener finishListener) {
        this.mGlowAnimations.cancel();
        this.mGlowAnimations.add(Tweener.to(this.mPointCloud.glowManager, duration, "ease", Ease.Quart.easeOut, "delay", Integer.valueOf(delay), "alpha", Float.valueOf(finalAlpha), "x", Float.valueOf(0.0f), "y", Float.valueOf(0.0f), "onUpdate", this.mUpdateListener, "onComplete", finishListener));
        this.mGlowAnimations.start();
    }

    private void deactivateTargets() {
        int count = this.mTargetDrawables.size();
        for (int i = 0; i < count; i++) {
            TargetDrawable target = this.mTargetDrawables.get(i);
            target.setState(TargetDrawable.STATE_INACTIVE);
        }
        this.mActiveTarget = -1;
    }

    private void dispatchTriggerEvent(int whichTarget) {
        vibrate();
        if (this.mOnTriggerListener != null) {
            this.mOnTriggerListener.onTrigger(this, whichTarget);
        }
    }

    private void dispatchOnFinishFinalAnimation() {
        if (this.mOnTriggerListener != null) {
            this.mOnTriggerListener.onFinishFinalAnimation();
        }
    }

    private void doFinish() {
        int activeTarget = this.mActiveTarget;
        boolean targetHit = activeTarget != -1;
        if (targetHit) {
            highlightSelected(activeTarget);
            hideGlow(200, RETURN_TO_HOME_DELAY, 0.0f, this.mResetListener);
            dispatchTriggerEvent(activeTarget);
            if (!this.mAlwaysTrackFinger) {
                this.mTargetAnimations.stop();
            }
        } else {
            hideGlow(200, 0, 0.0f, this.mResetListenerWithPing);
            hideTargets(true, false);
        }
        setGrabbedState(0);
    }

    private void highlightSelected(int activeTarget) {
        this.mTargetDrawables.get(activeTarget).setState(TargetDrawable.STATE_ACTIVE);
        hideUnselected(activeTarget);
    }

    private void hideUnselected(int active) {
        for (int i = 0; i < this.mTargetDrawables.size(); i++) {
            if (i != active) {
                this.mTargetDrawables.get(i).setAlpha(0.0f);
            }
        }
    }

    private void hideTargets(boolean animate, boolean expanded) {
        this.mTargetAnimations.cancel();
        this.mAnimatingTargets = animate;
        int duration = animate ? 200 : 0;
        int delay = animate ? 200 : 0;
        float targetScale = expanded ? 1.0f : TARGET_SCALE_COLLAPSED;
        int length = this.mTargetDrawables.size();
        TimeInterpolator interpolator = Ease.Cubic.easeOut;
        for (int i = 0; i < length; i++) {
            TargetDrawable target = this.mTargetDrawables.get(i);
            target.setState(TargetDrawable.STATE_INACTIVE);
            this.mTargetAnimations.add(Tweener.to(target, duration, "ease", interpolator, "alpha", Float.valueOf(0.0f), "scaleX", Float.valueOf(targetScale), "scaleY", Float.valueOf(targetScale), "delay", Integer.valueOf(delay), "onUpdate", this.mUpdateListener));
        }
        float ringScaleTarget = (expanded ? 1.0f : RING_SCALE_COLLAPSED) * this.mRingScaleFactor;
        this.mTargetAnimations.add(Tweener.to(this.mOuterRing, duration, "ease", interpolator, "alpha", Float.valueOf(0.0f), "scaleX", Float.valueOf(ringScaleTarget), "scaleY", Float.valueOf(ringScaleTarget), "delay", Integer.valueOf(delay), "onUpdate", this.mUpdateListener, "onComplete", this.mTargetUpdateListener));
        this.mTargetAnimations.start();
    }

    private void showTargets(boolean animate) {
        this.mTargetAnimations.stop();
        this.mAnimatingTargets = animate;
        int delay = animate ? 50 : 0;
        int duration = animate ? 200 : 0;
        int length = this.mTargetDrawables.size();
        for (int i = 0; i < length; i++) {
            TargetDrawable target = this.mTargetDrawables.get(i);
            target.setState(TargetDrawable.STATE_INACTIVE);
            this.mTargetAnimations.add(Tweener.to(target, duration, "ease", Ease.Cubic.easeOut, "alpha", Float.valueOf(1.0f), "scaleX", Float.valueOf(1.0f), "scaleY", Float.valueOf(1.0f), "delay", Integer.valueOf(delay), "onUpdate", this.mUpdateListener));
        }
        float ringScale = this.mRingScaleFactor * 1.0f;
        this.mTargetAnimations.add(Tweener.to(this.mOuterRing, duration, "ease", Ease.Cubic.easeOut, "alpha", Float.valueOf(1.0f), "scaleX", Float.valueOf(ringScale), "scaleY", Float.valueOf(ringScale), "delay", Integer.valueOf(delay), "onUpdate", this.mUpdateListener, "onComplete", this.mTargetUpdateListener));
        this.mTargetAnimations.start();
    }

    private void vibrate() {
        boolean hapticEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1, -2) != 0;
        if (this.mVibrator != null && hapticEnabled) {
            this.mVibrator.vibrate(this.mVibrationDuration, VIBRATION_ATTRIBUTES);
        }
    }

    private ArrayList<TargetDrawable> loadDrawableArray(int resourceId) {
        Resources res = getContext().getResources();
        TypedArray array = res.obtainTypedArray(resourceId);
        int count = array.length();
        ArrayList<TargetDrawable> drawables = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            TypedValue value = array.peekValue(i);
            TargetDrawable target = new TargetDrawable(res, value != null ? value.resourceId : 0);
            drawables.add(target);
        }
        array.recycle();
        return drawables;
    }

    private void internalSetTargetResources(int resourceId) {
        ArrayList<TargetDrawable> targets = loadDrawableArray(resourceId);
        this.mTargetDrawables = targets;
        this.mTargetResourceId = resourceId;
        int maxWidth = this.mHandleDrawable.getWidth();
        int maxHeight = this.mHandleDrawable.getHeight();
        int count = targets.size();
        for (int i = 0; i < count; i++) {
            TargetDrawable target = targets.get(i);
            maxWidth = Math.max(maxWidth, target.getWidth());
            maxHeight = Math.max(maxHeight, target.getHeight());
        }
        if (this.mMaxTargetWidth != maxWidth || this.mMaxTargetHeight != maxHeight) {
            this.mMaxTargetWidth = maxWidth;
            this.mMaxTargetHeight = maxHeight;
            requestLayout();
        } else {
            updateTargetPositions(this.mWaveCenterX, this.mWaveCenterY);
            updatePointCloudPosition(this.mWaveCenterX, this.mWaveCenterY);
        }
    }

    public void setTargetResources(int resourceId) {
        if (this.mAnimatingTargets) {
            this.mNewTargetResources = resourceId;
        } else {
            internalSetTargetResources(resourceId);
        }
    }

    public int getTargetResourceId() {
        return this.mTargetResourceId;
    }

    public void setTargetDescriptionsResourceId(int resourceId) {
        this.mTargetDescriptionsResourceId = resourceId;
        if (this.mTargetDescriptions != null) {
            this.mTargetDescriptions.clear();
        }
    }

    public int getTargetDescriptionsResourceId() {
        return this.mTargetDescriptionsResourceId;
    }

    public void setDirectionDescriptionsResourceId(int resourceId) {
        this.mDirectionDescriptionsResourceId = resourceId;
        if (this.mDirectionDescriptions != null) {
            this.mDirectionDescriptions.clear();
        }
    }

    public int getDirectionDescriptionsResourceId() {
        return this.mDirectionDescriptionsResourceId;
    }

    public void setVibrateEnabled(boolean enabled) {
        if (enabled && this.mVibrator == null) {
            this.mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        } else {
            this.mVibrator = null;
        }
    }

    public void ping() {
        if (this.mFeedbackCount > 0) {
            boolean doWaveAnimation = true;
            AnimationBundle waveAnimations = this.mWaveAnimations;
            if (waveAnimations.size() > 0 && waveAnimations.get(0).animator.isRunning()) {
                long t = waveAnimations.get(0).animator.getCurrentPlayTime();
                if (t < 500) {
                    doWaveAnimation = false;
                }
            }
            if (doWaveAnimation) {
                startWaveAnimation();
            }
        }
    }

    private void stopAndHideWaveAnimation() {
        this.mWaveAnimations.cancel();
        this.mPointCloud.waveManager.setAlpha(0.0f);
    }

    private void startWaveAnimation() {
        this.mWaveAnimations.cancel();
        this.mPointCloud.waveManager.setAlpha(1.0f);
        this.mPointCloud.waveManager.setRadius(this.mHandleDrawable.getWidth() / 2.0f);
        this.mWaveAnimations.add(Tweener.to(this.mPointCloud.waveManager, 1000L, "ease", Ease.Quad.easeOut, "delay", 0, "radius", Float.valueOf(this.mOuterRadius * 2.0f), "onUpdate", this.mUpdateListener, "onComplete", new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                GlowPadView.this.mPointCloud.waveManager.setRadius(0.0f);
                GlowPadView.this.mPointCloud.waveManager.setAlpha(0.0f);
            }
        }));
        this.mWaveAnimations.start();
    }

    public void reset(boolean animate) {
        this.mGlowAnimations.stop();
        this.mTargetAnimations.stop();
        startBackgroundAnimation(0, 0.0f);
        stopAndHideWaveAnimation();
        hideTargets(animate, false);
        hideGlow(0, 0, 0.0f, null);
        Tweener.reset();
    }

    private void startBackgroundAnimation(int duration, float alpha) {
        Drawable background = getBackground();
        if (this.mAlwaysTrackFinger && background != null) {
            if (this.mBackgroundAnimator != null) {
                this.mBackgroundAnimator.animator.cancel();
            }
            this.mBackgroundAnimator = Tweener.to(background, duration, "ease", Ease.Cubic.easeIn, "alpha", Integer.valueOf((int) (255.0f * alpha)), "delay", 50);
            this.mBackgroundAnimator.animator.start();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        boolean handled = false;
        switch (action) {
            case 0:
            case 5:
                handleDown(event);
                handleMove(event);
                handled = true;
                break;
            case 1:
            case 6:
                handleMove(event);
                handleUp(event);
                handled = true;
                break;
            case 2:
                handleMove(event);
                handled = true;
                break;
            case 3:
                handleMove(event);
                handleCancel(event);
                handled = true;
                break;
        }
        invalidate();
        if (handled) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateGlowPosition(float x, float y) {
        float dx = x - this.mOuterRing.getX();
        float dy = y - this.mOuterRing.getY();
        float dx2 = dx * (1.0f / this.mRingScaleFactor);
        float dy2 = dy * (1.0f / this.mRingScaleFactor);
        this.mPointCloud.glowManager.setX(this.mOuterRing.getX() + dx2);
        this.mPointCloud.glowManager.setY(this.mOuterRing.getY() + dy2);
    }

    private void handleDown(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        float eventX = event.getX(actionIndex);
        float eventY = event.getY(actionIndex);
        switchToState(1, eventX, eventY);
        if (!trySwitchToFirstTouchState(eventX, eventY)) {
            this.mDragging = false;
        } else {
            this.mPointerId = event.getPointerId(actionIndex);
            updateGlowPosition(eventX, eventY);
        }
    }

    private void handleUp(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        if (event.getPointerId(actionIndex) == this.mPointerId) {
            switchToState(5, event.getX(actionIndex), event.getY(actionIndex));
        }
    }

    private void handleCancel(MotionEvent event) {
        this.mActiveTarget = -1;
        int actionIndex = event.findPointerIndex(this.mPointerId);
        if (actionIndex == -1) {
            actionIndex = 0;
        }
        switchToState(5, event.getX(actionIndex), event.getY(actionIndex));
    }

    private void handleMove(MotionEvent event) {
        int activeTarget = -1;
        int historySize = event.getHistorySize();
        ArrayList<TargetDrawable> targets = this.mTargetDrawables;
        int ntargets = targets.size();
        float x = 0.0f;
        float y = 0.0f;
        float activeAngle = 0.0f;
        int actionIndex = event.findPointerIndex(this.mPointerId);
        if (actionIndex != -1) {
            int k = 0;
            while (k < historySize + 1) {
                float eventX = k < historySize ? event.getHistoricalX(actionIndex, k) : event.getX(actionIndex);
                float eventY = k < historySize ? event.getHistoricalY(actionIndex, k) : event.getY(actionIndex);
                float tx = eventX - this.mWaveCenterX;
                float ty = eventY - this.mWaveCenterY;
                float touchRadius = (float) Math.sqrt(dist2(tx, ty));
                float scale = touchRadius > this.mOuterRadius ? this.mOuterRadius / touchRadius : 1.0f;
                float limitX = tx * scale;
                float limitY = ty * scale;
                double angleRad = Math.atan2(-ty, tx);
                if (!this.mDragging) {
                    trySwitchToFirstTouchState(eventX, eventY);
                }
                if (this.mDragging) {
                    float snapRadius = (this.mRingScaleFactor * this.mOuterRadius) - this.mSnapMargin;
                    float snapDistance2 = snapRadius * snapRadius;
                    for (int i = 0; i < ntargets; i++) {
                        TargetDrawable target = targets.get(i);
                        double targetMinRad = ((double) this.mFirstItemOffset) + ((((((double) i) - 0.5d) * 2.0d) * 3.141592653589793d) / ((double) ntargets));
                        double targetMaxRad = ((double) this.mFirstItemOffset) + ((((((double) i) + 0.5d) * 2.0d) * 3.141592653589793d) / ((double) ntargets));
                        if (target.isEnabled()) {
                            boolean angleMatches = (angleRad > targetMinRad && angleRad <= targetMaxRad) || (6.283185307179586d + angleRad > targetMinRad && 6.283185307179586d + angleRad <= targetMaxRad) || (angleRad - 6.283185307179586d > targetMinRad && angleRad - 6.283185307179586d <= targetMaxRad);
                            if (angleMatches && dist2(tx, ty) > snapDistance2) {
                                activeTarget = i;
                                activeAngle = (float) (-angleRad);
                            }
                        }
                    }
                }
                x = limitX;
                y = limitY;
                k++;
            }
            if (this.mDragging) {
                if (activeTarget != -1) {
                    switchToState(4, x, y);
                    updateGlowPosition(x, y);
                } else {
                    switchToState(3, x, y);
                    updateGlowPosition(x, y);
                }
                if (this.mActiveTarget != activeTarget) {
                    if (this.mActiveTarget != -1) {
                        TargetDrawable target2 = targets.get(this.mActiveTarget);
                        if (target2.hasState(TargetDrawable.STATE_FOCUSED)) {
                            target2.setState(TargetDrawable.STATE_INACTIVE);
                        }
                        if (this.mMagneticTargets) {
                            updateTargetPosition(this.mActiveTarget, this.mWaveCenterX, this.mWaveCenterY);
                        }
                    }
                    if (activeTarget != -1) {
                        TargetDrawable target3 = targets.get(activeTarget);
                        if (target3.hasState(TargetDrawable.STATE_FOCUSED)) {
                            target3.setState(TargetDrawable.STATE_FOCUSED);
                        }
                        if (this.mMagneticTargets) {
                            updateTargetPosition(activeTarget, this.mWaveCenterX, this.mWaveCenterY, activeAngle);
                        }
                        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                            String targetContentDescription = getTargetDescription(activeTarget);
                            announceForAccessibility(targetContentDescription);
                        }
                    }
                }
                this.mActiveTarget = activeTarget;
            }
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (AccessibilityManager.getInstance(this.mContext).isTouchExplorationEnabled()) {
            int action = event.getAction();
            switch (action) {
                case 7:
                    event.setAction(2);
                    break;
                case 9:
                    event.setAction(0);
                    break;
                case 10:
                    event.setAction(1);
                    break;
            }
            onTouchEvent(event);
            event.setAction(action);
        }
        super.onHoverEvent(event);
        return true;
    }

    private void setGrabbedState(int newState) {
        if (newState != this.mGrabbedState) {
            if (newState != 0) {
                vibrate();
            }
            this.mGrabbedState = newState;
            if (this.mOnTriggerListener != null) {
                if (newState == 0) {
                    this.mOnTriggerListener.onReleased(this, 1);
                } else {
                    this.mOnTriggerListener.onGrabbed(this, 1);
                }
                this.mOnTriggerListener.onGrabbedStateChange(this, newState);
            }
        }
    }

    private boolean trySwitchToFirstTouchState(float x, float y) {
        float tx = x - this.mWaveCenterX;
        float ty = y - this.mWaveCenterY;
        if (!this.mAlwaysTrackFinger && dist2(tx, ty) > getScaledGlowRadiusSquared()) {
            return false;
        }
        switchToState(2, x, y);
        updateGlowPosition(tx, ty);
        this.mDragging = true;
        return true;
    }

    private void assignDefaultsIfNeeded() {
        if (this.mOuterRadius == 0.0f) {
            this.mOuterRadius = Math.max(this.mOuterRing.getWidth(), this.mOuterRing.getHeight()) / 2.0f;
        }
        if (this.mSnapMargin == 0.0f) {
            this.mSnapMargin = TypedValue.applyDimension(1, SNAP_MARGIN_DEFAULT, getContext().getResources().getDisplayMetrics());
        }
        if (this.mInnerRadius == 0.0f) {
            this.mInnerRadius = this.mHandleDrawable.getWidth() / 10.0f;
        }
    }

    private void computeInsets(int dx, int dy) {
        int layoutDirection = getLayoutDirection();
        int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, layoutDirection);
        switch (absoluteGravity & 7) {
            case 3:
                this.mHorizontalInset = 0;
                break;
            case 4:
            default:
                this.mHorizontalInset = dx / 2;
                break;
            case 5:
                this.mHorizontalInset = dx;
                break;
        }
        switch (absoluteGravity & 112) {
            case 48:
                this.mVerticalInset = 0;
                break;
            case 80:
                this.mVerticalInset = dy;
                break;
            default:
                this.mVerticalInset = dy / 2;
                break;
        }
    }

    private float computeScaleFactor(int desiredWidth, int desiredHeight, int actualWidth, int actualHeight) {
        if (!this.mAllowScaling) {
            return 1.0f;
        }
        int layoutDirection = getLayoutDirection();
        int absoluteGravity = Gravity.getAbsoluteGravity(this.mGravity, layoutDirection);
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        switch (absoluteGravity & 7) {
            case 3:
            case 5:
                break;
            case 4:
            default:
                if (desiredWidth > actualWidth) {
                    scaleX = ((actualWidth * 1.0f) - this.mMaxTargetWidth) / (desiredWidth - this.mMaxTargetWidth);
                }
                break;
        }
        switch (absoluteGravity & 112) {
            case 48:
            case 80:
                break;
            default:
                if (desiredHeight > actualHeight) {
                    scaleY = ((1.0f * actualHeight) - this.mMaxTargetHeight) / (desiredHeight - this.mMaxTargetHeight);
                }
                break;
        }
        return Math.min(scaleX, scaleY);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minimumWidth = getSuggestedMinimumWidth();
        int minimumHeight = getSuggestedMinimumHeight();
        int computedWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int computedHeight = resolveMeasured(heightMeasureSpec, minimumHeight);
        this.mRingScaleFactor = computeScaleFactor(minimumWidth, minimumHeight, computedWidth, computedHeight);
        int scaledWidth = getScaledSuggestedMinimumWidth();
        int scaledHeight = getScaledSuggestedMinimumHeight();
        computeInsets(computedWidth - scaledWidth, computedHeight - scaledHeight);
        setMeasuredDimension(computedWidth, computedHeight);
    }

    private float getRingWidth() {
        return this.mRingScaleFactor * Math.max(this.mOuterRing.getWidth(), 2.0f * this.mOuterRadius);
    }

    private float getRingHeight() {
        return this.mRingScaleFactor * Math.max(this.mOuterRing.getHeight(), 2.0f * this.mOuterRadius);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = right - left;
        int height = bottom - top;
        float placementWidth = getRingWidth();
        float placementHeight = getRingHeight();
        float newWaveCenterX = this.mHorizontalInset + (Math.max(width, this.mMaxTargetWidth + placementWidth) / 2.0f);
        float newWaveCenterY = this.mVerticalInset + (Math.max(height, this.mMaxTargetHeight + placementHeight) / 2.0f);
        if (this.mInitialLayout) {
            stopAndHideWaveAnimation();
            hideTargets(false, false);
            this.mInitialLayout = false;
        }
        this.mOuterRing.setPositionX(newWaveCenterX);
        this.mOuterRing.setPositionY(newWaveCenterY);
        this.mPointCloud.setScale(this.mRingScaleFactor);
        this.mHandleDrawable.setPositionX(newWaveCenterX);
        this.mHandleDrawable.setPositionY(newWaveCenterY);
        updateTargetPositions(newWaveCenterX, newWaveCenterY);
        updatePointCloudPosition(newWaveCenterX, newWaveCenterY);
        updateGlowPosition(newWaveCenterX, newWaveCenterY);
        this.mWaveCenterX = newWaveCenterX;
        this.mWaveCenterY = newWaveCenterY;
    }

    private void updateTargetPosition(int i, float centerX, float centerY) {
        float angle = getAngle(getSliceAngle(), i);
        updateTargetPosition(i, centerX, centerY, angle);
    }

    private void updateTargetPosition(int i, float centerX, float centerY, float angle) {
        float placementRadiusX = getRingWidth() / 2.0f;
        float placementRadiusY = getRingHeight() / 2.0f;
        if (i >= 0) {
            ArrayList<TargetDrawable> targets = this.mTargetDrawables;
            TargetDrawable targetIcon = targets.get(i);
            targetIcon.setPositionX(centerX);
            targetIcon.setPositionY(centerY);
            targetIcon.setX(((float) Math.cos(angle)) * placementRadiusX);
            targetIcon.setY(((float) Math.sin(angle)) * placementRadiusY);
        }
    }

    private void updateTargetPositions(float centerX, float centerY) {
        updateTargetPositions(centerX, centerY, false);
    }

    private void updateTargetPositions(float centerX, float centerY, boolean skipActive) {
        int size = this.mTargetDrawables.size();
        float alpha = getSliceAngle();
        for (int i = 0; i < size; i++) {
            if (!skipActive || i != this.mActiveTarget) {
                updateTargetPosition(i, centerX, centerY, getAngle(alpha, i));
            }
        }
    }

    private float getAngle(float alpha, int i) {
        return this.mFirstItemOffset + (i * alpha);
    }

    private float getSliceAngle() {
        return (float) ((-6.283185307179586d) / ((double) this.mTargetDrawables.size()));
    }

    private void updatePointCloudPosition(float centerX, float centerY) {
        this.mPointCloud.setCenter(centerX, centerY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.mPointCloud.draw(canvas);
        this.mOuterRing.draw(canvas);
        int ntargets = this.mTargetDrawables.size();
        for (int i = 0; i < ntargets; i++) {
            TargetDrawable target = this.mTargetDrawables.get(i);
            if (target != null) {
                target.draw(canvas);
            }
        }
        this.mHandleDrawable.draw(canvas);
    }

    public void setOnTriggerListener(OnTriggerListener listener) {
        this.mOnTriggerListener = listener;
    }

    private float square(float d) {
        return d * d;
    }

    private float dist2(float dx, float dy) {
        return (dx * dx) + (dy * dy);
    }

    private float getScaledGlowRadiusSquared() {
        float scaledTapRadius;
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            scaledTapRadius = TAP_RADIUS_SCALE_ACCESSIBILITY_ENABLED * this.mGlowRadius;
        } else {
            scaledTapRadius = this.mGlowRadius;
        }
        return square(scaledTapRadius);
    }

    private void announceTargets() {
        StringBuilder utterance = new StringBuilder();
        int targetCount = this.mTargetDrawables.size();
        for (int i = 0; i < targetCount; i++) {
            String targetDescription = getTargetDescription(i);
            String directionDescription = getDirectionDescription(i);
            if (!TextUtils.isEmpty(targetDescription) && !TextUtils.isEmpty(directionDescription)) {
                String text = String.format(directionDescription, targetDescription);
                utterance.append(text);
            }
        }
        if (utterance.length() > 0) {
            announceForAccessibility(utterance.toString());
        }
    }

    private String getTargetDescription(int index) {
        if (this.mTargetDescriptions == null || this.mTargetDescriptions.isEmpty()) {
            this.mTargetDescriptions = loadDescriptions(this.mTargetDescriptionsResourceId);
            if (this.mTargetDrawables.size() != this.mTargetDescriptions.size()) {
                Log.w(TAG, "The number of target drawables must be equal to the number of target descriptions.");
                return null;
            }
        }
        return this.mTargetDescriptions.get(index);
    }

    private String getDirectionDescription(int index) {
        if (this.mDirectionDescriptions == null || this.mDirectionDescriptions.isEmpty()) {
            this.mDirectionDescriptions = loadDescriptions(this.mDirectionDescriptionsResourceId);
            if (this.mTargetDrawables.size() != this.mDirectionDescriptions.size()) {
                Log.w(TAG, "The number of target drawables must be equal to the number of direction descriptions.");
                return null;
            }
        }
        return this.mDirectionDescriptions.get(index);
    }

    private ArrayList<String> loadDescriptions(int resourceId) {
        TypedArray array = getContext().getResources().obtainTypedArray(resourceId);
        int count = array.length();
        ArrayList<String> targetContentDescriptions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String contentDescription = array.getString(i);
            targetContentDescriptions.add(contentDescription);
        }
        array.recycle();
        return targetContentDescriptions;
    }

    public int getResourceIdForTarget(int index) {
        TargetDrawable drawable = this.mTargetDrawables.get(index);
        if (drawable == null) {
            return 0;
        }
        return drawable.getResourceId();
    }

    public void setEnableTarget(int resourceId, boolean enabled) {
        for (int i = 0; i < this.mTargetDrawables.size(); i++) {
            TargetDrawable target = this.mTargetDrawables.get(i);
            if (target.getResourceId() == resourceId) {
                target.setEnabled(enabled);
                return;
            }
        }
    }

    public int getTargetPosition(int resourceId) {
        for (int i = 0; i < this.mTargetDrawables.size(); i++) {
            TargetDrawable target = this.mTargetDrawables.get(i);
            if (target.getResourceId() == resourceId) {
                return i;
            }
        }
        return -1;
    }

    private boolean replaceTargetDrawables(Resources res, int existingResourceId, int newResourceId) {
        if (existingResourceId == 0 || newResourceId == 0) {
            return false;
        }
        boolean result = false;
        ArrayList<TargetDrawable> drawables = this.mTargetDrawables;
        int size = drawables.size();
        for (int i = 0; i < size; i++) {
            TargetDrawable target = drawables.get(i);
            if (target != null && target.getResourceId() == existingResourceId) {
                target.setDrawable(res, newResourceId);
                result = true;
            }
        }
        if (result) {
            requestLayout();
            return result;
        }
        return result;
    }

    public boolean replaceTargetDrawablesIfPresent(ComponentName component, String name, int existingResId) {
        int iconResId;
        if (existingResId == 0) {
            return false;
        }
        boolean replaced = false;
        if (component != null) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                Bundle metaData = packageManager.getActivityInfo(component, 128).metaData;
                if (metaData != null && (iconResId = metaData.getInt(name)) != 0) {
                    Resources res = packageManager.getResourcesForActivity(component);
                    replaced = replaceTargetDrawables(res, existingResId, iconResId);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Failed to swap drawable; " + component.flattenToShortString() + " not found", e);
            } catch (Resources.NotFoundException nfe) {
                Log.w(TAG, "Failed to swap drawable from " + component.flattenToShortString(), nfe);
            }
        }
        if (!replaced) {
            replaceTargetDrawables(this.mContext.getResources(), existingResId, existingResId);
            return replaced;
        }
        return replaced;
    }
}
