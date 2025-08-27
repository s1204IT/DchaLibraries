package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Trace;
import android.util.Log;
import android.util.MathUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.util.function.TriConsumer;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.R;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.stack.ViewState;
import com.android.systemui.util.AlarmTimeout;
import com.android.systemui.util.wakelock.DelayedWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class ScrimController implements ViewTreeObserver.OnPreDrawListener, ColorExtractor.OnColorsChangedListener, Dumpable {
    private static final boolean DEBUG = Log.isLoggable("ScrimController", 3);
    protected boolean mAnimateChange;
    private long mAnimationDelay;
    private boolean mBlankScreen;
    private Runnable mBlankingTransitionRunnable;
    private Callback mCallback;
    private final Context mContext;
    private int mCurrentBehindTint;
    private int mCurrentInFrontTint;
    private boolean mDarkenWhileDragging;
    private boolean mDeferFinishedListener;
    private final DozeParameters mDozeParameters;
    private boolean mKeyguardOccluded;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private ColorExtractor.GradientColors mLockColors;
    private boolean mNeedsDrawableColorUpdate;
    private float mNotificationDensity;
    private Runnable mOnAnimationFinished;
    private Runnable mPendingFrameCallback;
    private boolean mScreenBlankingCallbackCalled;
    private boolean mScreenOn;
    protected final ScrimView mScrimBehind;
    protected float mScrimBehindAlpha;
    protected float mScrimBehindAlphaResValue;
    protected final ScrimView mScrimInFront;
    private final TriConsumer<ScrimState, Float, ColorExtractor.GradientColors> mScrimStateListener;
    private final Consumer<Integer> mScrimVisibleListener;
    private int mScrimsVisibility;
    private ScrimState mState;
    private ColorExtractor.GradientColors mSystemColors;
    private final AlarmTimeout mTimeTicker;
    private boolean mTracking;
    private final UnlockMethodCache mUnlockMethodCache;
    private boolean mUpdatePending;
    private boolean mWakeLockHeld;
    private boolean mWallpaperSupportsAmbientMode;
    private boolean mWallpaperVisibilityTimedOut;
    protected float mScrimBehindAlphaKeyguard = 0.45f;
    private float mExpansionFraction = 1.0f;
    private boolean mExpansionAffectsAlpha = true;
    protected long mAnimationDuration = -1;
    private final Interpolator mInterpolator = new DecelerateInterpolator();
    private float mCurrentInFrontAlpha = -1.0f;
    private float mCurrentBehindAlpha = -1.0f;
    private final WakeLock mWakeLock = createWakeLock();
    private final SysuiColorExtractor mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);

    public ScrimController(ScrimView scrimView, ScrimView scrimView2, TriConsumer<ScrimState, Float, ColorExtractor.GradientColors> triConsumer, Consumer<Integer> consumer, DozeParameters dozeParameters, AlarmManager alarmManager) {
        this.mState = ScrimState.UNINITIALIZED;
        this.mScrimBehind = scrimView;
        this.mScrimInFront = scrimView2;
        this.mScrimStateListener = triConsumer;
        this.mScrimVisibleListener = consumer;
        this.mContext = scrimView.getContext();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mDarkenWhileDragging = !this.mUnlockMethodCache.canSkipBouncer();
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mScrimBehindAlphaResValue = this.mContext.getResources().getFloat(R.dimen.scrim_behind_alpha);
        this.mTimeTicker = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$ZxOK9HbkOUnaEI0FKoidLb2saOY
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                this.f$0.onHideWallpaperTimeout();
            }
        }, "hide_aod_wallpaper", new Handler());
        this.mScrimBehindAlpha = this.mScrimBehindAlphaResValue;
        this.mDozeParameters = dozeParameters;
        this.mColorExtractor.addOnColorsChangedListener(this);
        this.mLockColors = this.mColorExtractor.getColors(2, 1, true);
        this.mSystemColors = this.mColorExtractor.getColors(1, 1, true);
        this.mNeedsDrawableColorUpdate = true;
        ScrimState[] scrimStateArrValues = ScrimState.values();
        for (int i = 0; i < scrimStateArrValues.length; i++) {
            scrimStateArrValues[i].init(this.mScrimInFront, this.mScrimBehind, this.mDozeParameters);
            scrimStateArrValues[i].setScrimBehindAlphaKeyguard(this.mScrimBehindAlphaKeyguard);
        }
        this.mState = ScrimState.UNINITIALIZED;
        this.mScrimBehind.setDefaultFocusHighlightEnabled(false);
        this.mScrimInFront.setDefaultFocusHighlightEnabled(false);
        updateScrims();
    }

    public void transitionTo(ScrimState scrimState) {
        transitionTo(scrimState, null);
    }

    public void transitionTo(ScrimState scrimState, Callback callback) {
        if (scrimState == this.mState) {
            if (callback != null && this.mCallback != callback) {
                callback.onFinished();
                return;
            }
            return;
        }
        if (DEBUG) {
            Log.d("ScrimController", "State changed to: " + scrimState);
        }
        if (scrimState == ScrimState.UNINITIALIZED) {
            throw new IllegalArgumentException("Cannot change to UNINITIALIZED.");
        }
        ScrimState scrimState2 = this.mState;
        this.mState = scrimState;
        Trace.traceCounter(4096L, "scrim_state", this.mState.getIndex());
        if (this.mCallback != null) {
            this.mCallback.onCancelled();
        }
        this.mCallback = callback;
        scrimState.prepare(scrimState2);
        this.mScreenBlankingCallbackCalled = false;
        this.mAnimationDelay = 0L;
        this.mBlankScreen = scrimState.getBlanksScreen();
        this.mAnimateChange = scrimState.getAnimateChange();
        this.mAnimationDuration = scrimState.getAnimationDuration();
        this.mCurrentInFrontTint = scrimState.getFrontTint();
        this.mCurrentBehindTint = scrimState.getBehindTint();
        this.mCurrentInFrontAlpha = scrimState.getFrontAlpha();
        this.mCurrentBehindAlpha = scrimState.getBehindAlpha(this.mNotificationDensity);
        applyExpansionToAlpha();
        this.mScrimInFront.setFocusable(!scrimState.isLowPowerState());
        this.mScrimBehind.setFocusable(!scrimState.isLowPowerState());
        if (this.mPendingFrameCallback != null) {
            this.mScrimBehind.removeCallbacks(this.mPendingFrameCallback);
            this.mPendingFrameCallback = null;
        }
        if (getHandler().hasCallbacks(this.mBlankingTransitionRunnable)) {
            getHandler().removeCallbacks(this.mBlankingTransitionRunnable);
            this.mBlankingTransitionRunnable = null;
        }
        this.mNeedsDrawableColorUpdate = scrimState != ScrimState.BRIGHTNESS_MIRROR;
        if (this.mState.isLowPowerState()) {
            holdWakeLock();
        }
        if (this.mWallpaperSupportsAmbientMode && this.mDozeParameters.getAlwaysOn() && this.mState == ScrimState.AOD) {
            if (!this.mWallpaperVisibilityTimedOut) {
                this.mTimeTicker.schedule(this.mDozeParameters.getWallpaperAodDuration(), 1);
            }
        } else if (this.mState != ScrimState.PULSING) {
            this.mTimeTicker.cancel();
            this.mWallpaperVisibilityTimedOut = false;
        }
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition() && this.mState == ScrimState.UNLOCKED) {
            this.mScrimInFront.postOnAnimationDelayed(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$5DY8P9cXHTvbVZZOVB_VSCJUZk0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.scheduleUpdate();
                }
            }, 16L);
            this.mAnimationDelay = 100L;
        } else if ((!this.mDozeParameters.getAlwaysOn() && scrimState2 == ScrimState.AOD) || (this.mState == ScrimState.AOD && !this.mDozeParameters.getDisplayNeedsBlanking())) {
            onPreDraw();
        } else {
            scheduleUpdate();
        }
        dispatchScrimState(this.mScrimBehind.getViewAlpha());
    }

    public ScrimState getState() {
        return this.mState;
    }

    public void onTrackingStarted() {
        this.mTracking = true;
        this.mDarkenWhileDragging = true ^ this.mUnlockMethodCache.canSkipBouncer();
    }

    public void onExpandingFinished() {
        this.mTracking = false;
    }

    @VisibleForTesting
    protected void onHideWallpaperTimeout() {
        if (this.mState != ScrimState.AOD) {
            return;
        }
        holdWakeLock();
        this.mWallpaperVisibilityTimedOut = true;
        this.mAnimateChange = true;
        this.mAnimationDuration = this.mDozeParameters.getWallpaperFadeOutDuration();
        scheduleUpdate();
    }

    private void holdWakeLock() {
        if (!this.mWakeLockHeld) {
            if (this.mWakeLock != null) {
                this.mWakeLockHeld = true;
                this.mWakeLock.acquire();
            } else {
                Log.w("ScrimController", "Cannot hold wake lock, it has not been set yet");
            }
        }
    }

    public void setPanelExpansion(float f) {
        if (this.mExpansionFraction != f) {
            this.mExpansionFraction = f;
            if (!(this.mState == ScrimState.UNLOCKED || this.mState == ScrimState.KEYGUARD) || !this.mExpansionAffectsAlpha) {
                return;
            }
            applyExpansionToAlpha();
            if (this.mUpdatePending) {
                return;
            }
            setOrAdaptCurrentAnimation(this.mScrimBehind);
            setOrAdaptCurrentAnimation(this.mScrimInFront);
            dispatchScrimState(this.mScrimBehind.getViewAlpha());
        }
    }

    private void setOrAdaptCurrentAnimation(View view) {
        if (!isAnimating(view)) {
            updateScrimColor(view, getCurrentScrimAlpha(view), getCurrentScrimTint(view));
            return;
        }
        ValueAnimator valueAnimator = (ValueAnimator) view.getTag(R.id.scrim);
        float currentScrimAlpha = getCurrentScrimAlpha(view);
        view.setTag(R.id.scrim_alpha_start, Float.valueOf(((Float) view.getTag(R.id.scrim_alpha_start)).floatValue() + (currentScrimAlpha - ((Float) view.getTag(R.id.scrim_alpha_end)).floatValue())));
        view.setTag(R.id.scrim_alpha_end, Float.valueOf(currentScrimAlpha));
        valueAnimator.setCurrentPlayTime(valueAnimator.getCurrentPlayTime());
    }

    private void applyExpansionToAlpha() {
        if (!this.mExpansionAffectsAlpha) {
            return;
        }
        if (this.mState == ScrimState.UNLOCKED) {
            this.mCurrentBehindAlpha = ((float) Math.pow(getInterpolatedFraction(), 0.800000011920929d)) * 0.7f;
            this.mCurrentInFrontAlpha = 0.0f;
        } else if (this.mState == ScrimState.KEYGUARD) {
            float interpolatedFraction = getInterpolatedFraction();
            float behindAlpha = this.mState.getBehindAlpha(this.mNotificationDensity);
            if (this.mDarkenWhileDragging) {
                this.mCurrentBehindAlpha = MathUtils.lerp(0.7f, behindAlpha, interpolatedFraction);
                this.mCurrentInFrontAlpha = 0.0f;
            } else {
                this.mCurrentBehindAlpha = MathUtils.lerp(0.0f, behindAlpha, interpolatedFraction);
                this.mCurrentInFrontAlpha = 0.0f;
            }
        }
    }

    public void setNotificationCount(int i) {
        float fMin = Math.min(i / 3.0f, 1.0f);
        if (this.mNotificationDensity == fMin) {
            return;
        }
        this.mNotificationDensity = fMin;
        if (this.mState == ScrimState.KEYGUARD) {
            applyExpansionToAlpha();
            scheduleUpdate();
        }
    }

    public void setScrimBehindDrawable(Drawable drawable) {
        this.mScrimBehind.setDrawable(drawable);
    }

    public void setAodFrontScrimAlpha(float f) {
        if (this.mState == ScrimState.AOD && this.mDozeParameters.getAlwaysOn() && this.mCurrentInFrontAlpha != f) {
            this.mCurrentInFrontAlpha = f;
            scheduleUpdate();
        }
        ScrimState scrimState = this.mState;
        ScrimState.AOD.setAodFrontScrimAlpha(f);
    }

    protected void scheduleUpdate() {
        if (this.mUpdatePending) {
            return;
        }
        this.mScrimBehind.invalidate();
        this.mScrimBehind.getViewTreeObserver().addOnPreDrawListener(this);
        this.mUpdatePending = true;
    }

    protected void updateScrims() {
        ColorExtractor.GradientColors gradientColors;
        boolean z = false;
        if (this.mNeedsDrawableColorUpdate) {
            this.mNeedsDrawableColorUpdate = false;
            if (this.mState == ScrimState.KEYGUARD || this.mState == ScrimState.BOUNCER_SCRIMMED || this.mState == ScrimState.BOUNCER) {
                this.mScrimInFront.setColors(this.mLockColors, true);
                this.mScrimBehind.setColors(this.mLockColors, true);
                gradientColors = this.mLockColors;
            } else {
                boolean z2 = this.mScrimInFront.getViewAlpha() != 0.0f;
                boolean z3 = this.mScrimBehind.getViewAlpha() != 0.0f;
                this.mScrimInFront.setColors(this.mSystemColors, z2);
                this.mScrimBehind.setColors(this.mSystemColors, z3);
                gradientColors = this.mSystemColors;
            }
            this.mScrimBehindAlpha = Math.max(this.mScrimBehindAlphaResValue, ColorUtils.calculateMinimumBackgroundAlpha(gradientColors.supportsDarkText() ? -16777216 : -1, gradientColors.getMainColor(), 4.5f) / 255.0f);
            dispatchScrimState(this.mScrimBehind.getViewAlpha());
        }
        boolean z4 = this.mState == ScrimState.AOD && this.mWallpaperVisibilityTimedOut;
        if ((this.mState == ScrimState.PULSING || this.mState == ScrimState.AOD) && this.mKeyguardOccluded) {
            z = true;
        }
        if (z4 || z) {
            this.mCurrentBehindAlpha = 1.0f;
        }
        setScrimInFrontAlpha(this.mCurrentInFrontAlpha);
        setScrimBehindAlpha(this.mCurrentBehindAlpha);
        dispatchScrimsVisible();
    }

    private void dispatchScrimState(float f) {
        this.mScrimStateListener.accept(this.mState, Float.valueOf(f), this.mScrimInFront.getColors());
    }

    private void dispatchScrimsVisible() {
        int i;
        if (this.mScrimInFront.getViewAlpha() == 1.0f || this.mScrimBehind.getViewAlpha() == 1.0f) {
            i = 2;
        } else if (this.mScrimInFront.getViewAlpha() == 0.0f && this.mScrimBehind.getViewAlpha() == 0.0f) {
            i = 0;
        } else {
            i = 1;
        }
        if (this.mScrimsVisibility != i) {
            this.mScrimsVisibility = i;
            this.mScrimVisibleListener.accept(Integer.valueOf(i));
        }
    }

    private float getInterpolatedFraction() {
        if ((this.mExpansionFraction * 1.2f) - 0.2f <= 0.0f) {
            return 0.0f;
        }
        return (float) (1.0d - (0.5d * (1.0d - Math.cos(3.141590118408203d * Math.pow(1.0f - r0, 2.0d)))));
    }

    private void setScrimBehindAlpha(float f) {
        setScrimAlpha(this.mScrimBehind, f);
    }

    private void setScrimInFrontAlpha(float f) {
        setScrimAlpha(this.mScrimInFront, f);
    }

    private void setScrimAlpha(ScrimView scrimView, float f) {
        boolean z = false;
        if (f == 0.0f) {
            scrimView.setClickable(false);
        } else {
            if (this.mState != ScrimState.AOD && this.mState != ScrimState.PULSING) {
                z = true;
            }
            scrimView.setClickable(z);
        }
        updateScrim(scrimView, f);
    }

    private void updateScrimColor(View view, float f, int i) {
        float fMax = Math.max(0.0f, Math.min(1.0f, f));
        if (view instanceof ScrimView) {
            ScrimView scrimView = (ScrimView) view;
            Trace.traceCounter(4096L, view == this.mScrimInFront ? "front_scrim_alpha" : "back_scrim_alpha", (int) (255.0f * fMax));
            Trace.traceCounter(4096L, view == this.mScrimInFront ? "front_scrim_tint" : "back_scrim_tint", Color.alpha(i));
            scrimView.setTint(i);
            scrimView.setViewAlpha(fMax);
        } else {
            view.setAlpha(fMax);
        }
        dispatchScrimsVisible();
    }

    private void startScrimAnimation(final View view, float f) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        final int tint = view instanceof ScrimView ? ((ScrimView) view).getTint() : 0;
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$ScrimController$RfJJyPt1cPl4hraLjBCUJgqPhOM
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                ScrimController.lambda$startScrimAnimation$0(this.f$0, view, tint, valueAnimator);
            }
        });
        valueAnimatorOfFloat.setInterpolator(this.mInterpolator);
        valueAnimatorOfFloat.setStartDelay(this.mAnimationDelay);
        valueAnimatorOfFloat.setDuration(this.mAnimationDuration);
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.ScrimController.1
            final /* synthetic */ View val$scrim;

            AnonymousClass1(final View view2) {
                view = view2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                ScrimController.this.onFinished();
                view.setTag(R.id.scrim, null);
                ScrimController.this.dispatchScrimsVisible();
                if (!ScrimController.this.mDeferFinishedListener && ScrimController.this.mOnAnimationFinished != null) {
                    ScrimController.this.mOnAnimationFinished.run();
                    ScrimController.this.mOnAnimationFinished = null;
                }
            }
        });
        view2.setTag(R.id.scrim_alpha_start, Float.valueOf(f));
        view2.setTag(R.id.scrim_alpha_end, Float.valueOf(getCurrentScrimAlpha(view2)));
        view2.setTag(R.id.scrim, valueAnimatorOfFloat);
        valueAnimatorOfFloat.start();
    }

    public static /* synthetic */ void lambda$startScrimAnimation$0(ScrimController scrimController, View view, int i, ValueAnimator valueAnimator) {
        float fFloatValue = ((Float) view.getTag(R.id.scrim_alpha_start)).floatValue();
        float fFloatValue2 = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        scrimController.updateScrimColor(view, MathUtils.constrain(MathUtils.lerp(fFloatValue, scrimController.getCurrentScrimAlpha(view), fFloatValue2), 0.0f, 1.0f), ColorUtils.blendARGB(i, scrimController.getCurrentScrimTint(view), fFloatValue2));
        scrimController.dispatchScrimsVisible();
    }

    /* renamed from: com.android.systemui.statusbar.phone.ScrimController$1 */
    class AnonymousClass1 extends AnimatorListenerAdapter {
        final /* synthetic */ View val$scrim;

        AnonymousClass1(final View view2) {
            view = view2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            ScrimController.this.onFinished();
            view.setTag(R.id.scrim, null);
            ScrimController.this.dispatchScrimsVisible();
            if (!ScrimController.this.mDeferFinishedListener && ScrimController.this.mOnAnimationFinished != null) {
                ScrimController.this.mOnAnimationFinished.run();
                ScrimController.this.mOnAnimationFinished = null;
            }
        }
    }

    private float getCurrentScrimAlpha(View view) {
        if (view == this.mScrimInFront) {
            return this.mCurrentInFrontAlpha;
        }
        if (view == this.mScrimBehind) {
            return this.mCurrentBehindAlpha;
        }
        throw new IllegalArgumentException("Unknown scrim view");
    }

    private int getCurrentScrimTint(View view) {
        if (view == this.mScrimInFront) {
            return this.mCurrentInFrontTint;
        }
        if (view == this.mScrimBehind) {
            return this.mCurrentBehindTint;
        }
        throw new IllegalArgumentException("Unknown scrim view");
    }

    @Override // android.view.ViewTreeObserver.OnPreDrawListener
    public boolean onPreDraw() {
        this.mScrimBehind.getViewTreeObserver().removeOnPreDrawListener(this);
        this.mUpdatePending = false;
        if (this.mCallback != null) {
            this.mCallback.onStart();
        }
        updateScrims();
        if (this.mOnAnimationFinished != null && !isAnimating(this.mScrimInFront) && !isAnimating(this.mScrimBehind)) {
            this.mOnAnimationFinished.run();
            this.mOnAnimationFinished = null;
            return true;
        }
        return true;
    }

    private void onFinished() {
        if (this.mWakeLockHeld) {
            this.mWakeLock.release();
            this.mWakeLockHeld = false;
        }
        if (this.mCallback != null) {
            this.mCallback.onFinished();
            this.mCallback = null;
        }
        if (this.mState == ScrimState.UNLOCKED) {
            this.mCurrentInFrontTint = 0;
            this.mCurrentBehindTint = 0;
        }
    }

    private boolean isAnimating(View view) {
        return view.getTag(R.id.scrim) != null;
    }

    public void setDrawBehindAsSrc(boolean z) {
        this.mScrimBehind.setDrawAsSrc(z);
    }

    @VisibleForTesting
    void setOnAnimationFinished(Runnable runnable) {
        this.mOnAnimationFinished = runnable;
    }

    private void updateScrim(ScrimView scrimView, float f) {
        float viewAlpha = scrimView.getViewAlpha();
        ValueAnimator valueAnimator = (ValueAnimator) ViewState.getChildTag(scrimView, R.id.scrim);
        boolean z = false;
        if (valueAnimator != null) {
            if (this.mAnimateChange) {
                this.mDeferFinishedListener = true;
            }
            cancelAnimator(valueAnimator);
            this.mDeferFinishedListener = false;
        }
        if (this.mPendingFrameCallback != null) {
            return;
        }
        if (this.mBlankScreen) {
            blankDisplay();
            return;
        }
        if (!this.mScreenBlankingCallbackCalled && this.mCallback != null) {
            this.mCallback.onDisplayBlanked();
            this.mScreenBlankingCallbackCalled = true;
        }
        if (scrimView == this.mScrimBehind) {
            dispatchScrimState(f);
        }
        boolean z2 = f != viewAlpha;
        if (scrimView.getTint() != getCurrentScrimTint(scrimView)) {
            z = true;
        }
        if (z2 || z) {
            if (this.mAnimateChange) {
                startScrimAnimation(scrimView, viewAlpha);
                return;
            } else {
                updateScrimColor(scrimView, f, getCurrentScrimTint(scrimView));
                onFinished();
                return;
            }
        }
        onFinished();
    }

    @VisibleForTesting
    protected void cancelAnimator(ValueAnimator valueAnimator) {
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
    }

    private void blankDisplay() {
        updateScrimColor(this.mScrimInFront, 1.0f, -16777216);
        this.mPendingFrameCallback = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$ScrimController$kDQtFE2BFSUNax7wJ8UgLaBZyEo
            @Override // java.lang.Runnable
            public final void run() {
                ScrimController.lambda$blankDisplay$2(this.f$0);
            }
        };
        doOnTheNextFrame(this.mPendingFrameCallback);
    }

    public static /* synthetic */ void lambda$blankDisplay$2(final ScrimController scrimController) {
        if (scrimController.mCallback != null) {
            scrimController.mCallback.onDisplayBlanked();
            scrimController.mScreenBlankingCallbackCalled = true;
        }
        scrimController.mBlankingTransitionRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$ScrimController$xRo1WgiULtMHpaX-VTe04p0hBHI
            @Override // java.lang.Runnable
            public final void run() {
                ScrimController.lambda$blankDisplay$1(this.f$0);
            }
        };
        int i = scrimController.mScreenOn ? 32 : 500;
        if (DEBUG) {
            Log.d("ScrimController", "Fading out scrims with delay: " + i);
        }
        scrimController.getHandler().postDelayed(scrimController.mBlankingTransitionRunnable, i);
    }

    public static /* synthetic */ void lambda$blankDisplay$1(ScrimController scrimController) {
        scrimController.mBlankingTransitionRunnable = null;
        scrimController.mPendingFrameCallback = null;
        scrimController.mBlankScreen = false;
        scrimController.updateScrims();
    }

    @VisibleForTesting
    protected void doOnTheNextFrame(Runnable runnable) {
        this.mScrimBehind.postOnAnimationDelayed(runnable, 32L);
    }

    @VisibleForTesting
    protected Handler getHandler() {
        return Handler.getMain();
    }

    public void setExcludedBackgroundArea(Rect rect) {
        this.mScrimBehind.setExcludedArea(rect);
    }

    public int getBackgroundColor() {
        int mainColor = this.mLockColors.getMainColor();
        return Color.argb((int) (this.mScrimBehind.getViewAlpha() * Color.alpha(mainColor)), Color.red(mainColor), Color.green(mainColor), Color.blue(mainColor));
    }

    public void setScrimBehindChangeRunnable(Runnable runnable) {
        this.mScrimBehind.setChangeRunnable(runnable);
    }

    public void setCurrentUser(int i) {
    }

    public void onColorsChanged(ColorExtractor colorExtractor, int i) {
        if ((i & 2) != 0) {
            this.mLockColors = this.mColorExtractor.getColors(2, 1, true);
            this.mNeedsDrawableColorUpdate = true;
            scheduleUpdate();
        }
        if ((i & 1) != 0) {
            this.mSystemColors = this.mColorExtractor.getColors(1, 1, this.mState != ScrimState.UNLOCKED);
            this.mNeedsDrawableColorUpdate = true;
            scheduleUpdate();
        }
    }

    @VisibleForTesting
    protected WakeLock createWakeLock() {
        return new DelayedWakeLock(getHandler(), WakeLock.createPartial(this.mContext, "Scrims"));
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(" ScrimController: ");
        printWriter.print("  state: ");
        printWriter.println(this.mState);
        printWriter.print("  frontScrim:");
        printWriter.print(" viewAlpha=");
        printWriter.print(this.mScrimInFront.getViewAlpha());
        printWriter.print(" alpha=");
        printWriter.print(this.mCurrentInFrontAlpha);
        printWriter.print(" tint=0x");
        printWriter.println(Integer.toHexString(this.mScrimInFront.getTint()));
        printWriter.print("  backScrim:");
        printWriter.print(" viewAlpha=");
        printWriter.print(this.mScrimBehind.getViewAlpha());
        printWriter.print(" alpha=");
        printWriter.print(this.mCurrentBehindAlpha);
        printWriter.print(" tint=0x");
        printWriter.println(Integer.toHexString(this.mScrimBehind.getTint()));
        printWriter.print("   mTracking=");
        printWriter.println(this.mTracking);
    }

    public void setWallpaperSupportsAmbientMode(boolean z) {
        this.mWallpaperSupportsAmbientMode = z;
        for (ScrimState scrimState : ScrimState.values()) {
            scrimState.setWallpaperSupportsAmbientMode(z);
        }
    }

    public void onScreenTurnedOn() {
        this.mScreenOn = true;
        Handler handler = getHandler();
        if (handler.hasCallbacks(this.mBlankingTransitionRunnable)) {
            if (DEBUG) {
                Log.d("ScrimController", "Shorter blanking because screen turned on. All good.");
            }
            handler.removeCallbacks(this.mBlankingTransitionRunnable);
            this.mBlankingTransitionRunnable.run();
        }
    }

    public void onScreenTurnedOff() {
        this.mScreenOn = false;
    }

    public void setExpansionAffectsAlpha(boolean z) {
        this.mExpansionAffectsAlpha = z;
    }

    public void setKeyguardOccluded(boolean z) {
        this.mKeyguardOccluded = z;
        updateScrims();
    }

    public interface Callback {
        default void onStart() {
        }

        default void onDisplayBlanked() {
        }

        default void onFinished() {
        }

        default void onCancelled() {
        }
    }
}
