package com.android.settingslib.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Property;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.settingslib.R;

/* loaded from: classes.dex */
public class AppearAnimationUtils implements AppearAnimationCreator<View> {
    protected final float mDelayScale;
    private final long mDuration;
    private final Interpolator mInterpolator;
    protected RowTranslationScaler mRowTranslationScaler;
    private final float mStartTranslation;
    private final AppearAnimationProperties mProperties = new AppearAnimationProperties();
    protected boolean mAppearing = true;

    public interface RowTranslationScaler {
        float getRowTranslationScale(int i, int i2);
    }

    public AppearAnimationUtils(Context context, long j, float f, float f2, Interpolator interpolator) {
        this.mInterpolator = interpolator;
        this.mStartTranslation = context.getResources().getDimensionPixelOffset(R.dimen.appear_y_translation_start) * f;
        this.mDelayScale = f2;
        this.mDuration = j;
    }

    public void startAnimation(View[] viewArr, Runnable runnable) {
        startAnimation(viewArr, runnable, this);
    }

    public <T> void startAnimation2d(T[][] tArr, Runnable runnable, AppearAnimationCreator<T> appearAnimationCreator) {
        startAnimations(getDelays((Object[][]) tArr), (Object[][]) tArr, runnable, (AppearAnimationCreator) appearAnimationCreator);
    }

    public <T> void startAnimation(T[] tArr, Runnable runnable, AppearAnimationCreator<T> appearAnimationCreator) {
        startAnimations(getDelays(tArr), tArr, runnable, appearAnimationCreator);
    }

    private <T> void startAnimations(AppearAnimationProperties appearAnimationProperties, T[] tArr, Runnable runnable, AppearAnimationCreator<T> appearAnimationCreator) {
        Runnable runnable2;
        float rowTranslationScale;
        if (appearAnimationProperties.maxDelayRowIndex == -1 || appearAnimationProperties.maxDelayColIndex == -1) {
            runnable.run();
            return;
        }
        for (int i = 0; i < appearAnimationProperties.delays.length; i++) {
            long j = appearAnimationProperties.delays[i][0];
            if (appearAnimationProperties.maxDelayRowIndex != i || appearAnimationProperties.maxDelayColIndex != 0) {
                runnable2 = null;
            } else {
                runnable2 = runnable;
            }
            if (this.mRowTranslationScaler != null) {
                rowTranslationScale = this.mRowTranslationScaler.getRowTranslationScale(i, appearAnimationProperties.delays.length);
            } else {
                rowTranslationScale = 1.0f;
            }
            float f = rowTranslationScale * this.mStartTranslation;
            T t = tArr[i];
            long j2 = this.mDuration;
            if (!this.mAppearing) {
                f = -f;
            }
            appearAnimationCreator.createAnimation(t, j, j2, f, this.mAppearing, this.mInterpolator, runnable2);
        }
    }

    private <T> void startAnimations(AppearAnimationProperties appearAnimationProperties, T[][] tArr, Runnable runnable, AppearAnimationCreator<T> appearAnimationCreator) {
        float rowTranslationScale;
        Runnable runnable2;
        if (appearAnimationProperties.maxDelayRowIndex == -1 || appearAnimationProperties.maxDelayColIndex == -1) {
            runnable.run();
            return;
        }
        for (int i = 0; i < appearAnimationProperties.delays.length; i++) {
            long[] jArr = appearAnimationProperties.delays[i];
            if (this.mRowTranslationScaler != null) {
                rowTranslationScale = this.mRowTranslationScaler.getRowTranslationScale(i, appearAnimationProperties.delays.length);
            } else {
                rowTranslationScale = 1.0f;
            }
            float f = rowTranslationScale * this.mStartTranslation;
            for (int i2 = 0; i2 < jArr.length; i2++) {
                long j = jArr[i2];
                if (appearAnimationProperties.maxDelayRowIndex != i || appearAnimationProperties.maxDelayColIndex != i2) {
                    runnable2 = null;
                } else {
                    runnable2 = runnable;
                }
                appearAnimationCreator.createAnimation(tArr[i][i2], j, this.mDuration, this.mAppearing ? f : -f, this.mAppearing, this.mInterpolator, runnable2);
            }
        }
    }

    private <T> AppearAnimationProperties getDelays(T[] tArr) {
        this.mProperties.maxDelayColIndex = -1;
        this.mProperties.maxDelayRowIndex = -1;
        this.mProperties.delays = new long[tArr.length][];
        long j = -1;
        for (int i = 0; i < tArr.length; i++) {
            this.mProperties.delays[i] = new long[1];
            long jCalculateDelay = calculateDelay(i, 0);
            this.mProperties.delays[i][0] = jCalculateDelay;
            if (tArr[i] != null && jCalculateDelay > j) {
                this.mProperties.maxDelayColIndex = 0;
                this.mProperties.maxDelayRowIndex = i;
                j = jCalculateDelay;
            }
        }
        return this.mProperties;
    }

    private <T> AppearAnimationProperties getDelays(T[][] tArr) {
        this.mProperties.maxDelayColIndex = -1;
        this.mProperties.maxDelayRowIndex = -1;
        this.mProperties.delays = new long[tArr.length][];
        long j = -1;
        int i = 0;
        while (i < tArr.length) {
            T[] tArr2 = tArr[i];
            this.mProperties.delays[i] = new long[tArr2.length];
            long j2 = j;
            for (int i2 = 0; i2 < tArr2.length; i2++) {
                long jCalculateDelay = calculateDelay(i, i2);
                this.mProperties.delays[i][i2] = jCalculateDelay;
                if (tArr[i][i2] != null && jCalculateDelay > j2) {
                    this.mProperties.maxDelayColIndex = i2;
                    this.mProperties.maxDelayRowIndex = i;
                    j2 = jCalculateDelay;
                }
            }
            i++;
            j = j2;
        }
        return this.mProperties;
    }

    protected long calculateDelay(int i, int i2) {
        return (long) (((i * 40) + (i2 * (Math.pow(i, 0.4d) + 0.4d) * 20.0d)) * this.mDelayScale);
    }

    /* JADX DEBUG: Method merged with bridge method: createAnimation(Ljava/lang/Object;JJFZLandroid/view/animation/Interpolator;Ljava/lang/Runnable;)V */
    @Override // com.android.settingslib.animation.AppearAnimationCreator
    public void createAnimation(final View view, long j, long j2, float f, boolean z, Interpolator interpolator, final Runnable runnable) {
        RenderNodeAnimator renderNodeAnimatorOfFloat;
        if (view != null) {
            float f2 = 1.0f;
            view.setAlpha(z ? 0.0f : 1.0f);
            view.setTranslationY(z ? f : 0.0f);
            if (!z) {
                f2 = 0.0f;
            }
            if (view.isHardwareAccelerated()) {
                renderNodeAnimatorOfFloat = new RenderNodeAnimator(11, f2);
                renderNodeAnimatorOfFloat.setTarget(view);
            } else {
                renderNodeAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, view.getAlpha(), f2);
            }
            renderNodeAnimatorOfFloat.setInterpolator(interpolator);
            renderNodeAnimatorOfFloat.setDuration(j2);
            renderNodeAnimatorOfFloat.setStartDelay(j);
            if (view.hasOverlappingRendering()) {
                view.setLayerType(2, null);
                renderNodeAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.settingslib.animation.AppearAnimationUtils.1
                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        view.setLayerType(0, null);
                    }
                });
            }
            if (runnable != null) {
                renderNodeAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.settingslib.animation.AppearAnimationUtils.2
                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        runnable.run();
                    }
                });
            }
            renderNodeAnimatorOfFloat.start();
            startTranslationYAnimation(view, j, j2, z ? 0.0f : f, interpolator);
        }
    }

    public static void startTranslationYAnimation(View view, long j, long j2, float f, Interpolator interpolator) {
        RenderNodeAnimator renderNodeAnimatorOfFloat;
        if (view.isHardwareAccelerated()) {
            renderNodeAnimatorOfFloat = new RenderNodeAnimator(1, f);
            renderNodeAnimatorOfFloat.setTarget(view);
        } else {
            renderNodeAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.TRANSLATION_Y, view.getTranslationY(), f);
        }
        renderNodeAnimatorOfFloat.setInterpolator(interpolator);
        renderNodeAnimatorOfFloat.setDuration(j2);
        renderNodeAnimatorOfFloat.setStartDelay(j);
        renderNodeAnimatorOfFloat.start();
    }

    public class AppearAnimationProperties {
        public long[][] delays;
        public int maxDelayColIndex;
        public int maxDelayRowIndex;

        public AppearAnimationProperties() {
        }
    }
}
