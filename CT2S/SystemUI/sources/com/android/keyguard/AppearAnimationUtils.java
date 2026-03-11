package com.android.keyguard;

import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class AppearAnimationUtils implements AppearAnimationCreator<View> {
    protected boolean mAppearing;
    protected final float mDelayScale;
    private final long mDuration;
    private final Interpolator mInterpolator;
    private final AppearAnimationProperties mProperties;
    protected boolean mScaleTranslationWithRow;
    private final float mStartTranslation;

    public AppearAnimationUtils(Context ctx) {
        this(ctx, 220L, 1.0f, 1.0f, AnimationUtils.loadInterpolator(ctx, android.R.interpolator.linear_out_slow_in));
    }

    public AppearAnimationUtils(Context ctx, long duration, float translationScaleFactor, float delayScaleFactor, Interpolator interpolator) {
        this.mProperties = new AppearAnimationProperties();
        this.mInterpolator = interpolator;
        this.mStartTranslation = ctx.getResources().getDimensionPixelOffset(R.dimen.appear_y_translation_start) * translationScaleFactor;
        this.mDelayScale = delayScaleFactor;
        this.mDuration = duration;
        this.mScaleTranslationWithRow = false;
        this.mAppearing = true;
    }

    public void startAnimation(View[][] objects, Runnable finishListener) {
        startAnimation((Object[][]) objects, finishListener, (AppearAnimationCreator) this);
    }

    public void startAnimation(View[] objects, Runnable finishListener) {
        startAnimation(objects, finishListener, this);
    }

    public <T> void startAnimation(T[][] objects, Runnable finishListener, AppearAnimationCreator<T> creator) {
        AppearAnimationProperties properties = getDelays((Object[][]) objects);
        startAnimations(properties, (Object[][]) objects, finishListener, (AppearAnimationCreator) creator);
    }

    public <T> void startAnimation(T[] objects, Runnable finishListener, AppearAnimationCreator<T> creator) {
        AppearAnimationProperties properties = getDelays(objects);
        startAnimations(properties, objects, finishListener, creator);
    }

    private <T> void startAnimations(AppearAnimationProperties properties, T[] objects, Runnable finishListener, AppearAnimationCreator<T> creator) {
        if (properties.maxDelayRowIndex == -1 || properties.maxDelayColIndex == -1) {
            finishListener.run();
            return;
        }
        for (int row = 0; row < properties.delays.length; row++) {
            long[] columns = properties.delays[row];
            long delay = columns[0];
            Runnable endRunnable = null;
            if (properties.maxDelayRowIndex == row && properties.maxDelayColIndex == 0) {
                endRunnable = finishListener;
            }
            creator.createAnimation(objects[row], delay, this.mDuration, this.mStartTranslation, true, this.mInterpolator, endRunnable);
        }
    }

    private <T> void startAnimations(AppearAnimationProperties properties, T[][] objects, Runnable finishListener, AppearAnimationCreator<T> creator) {
        if (properties.maxDelayRowIndex == -1 || properties.maxDelayColIndex == -1) {
            finishListener.run();
            return;
        }
        for (int row = 0; row < properties.delays.length; row++) {
            long[] columns = properties.delays[row];
            float translation = this.mScaleTranslationWithRow ? (float) ((Math.pow(properties.delays.length - row, 2.0d) / ((double) properties.delays.length)) * ((double) this.mStartTranslation)) : this.mStartTranslation;
            for (int col = 0; col < columns.length; col++) {
                long delay = columns[col];
                Runnable endRunnable = null;
                if (properties.maxDelayRowIndex == row && properties.maxDelayColIndex == col) {
                    endRunnable = finishListener;
                }
                creator.createAnimation(objects[row][col], delay, this.mDuration, this.mAppearing ? translation : -translation, this.mAppearing, this.mInterpolator, endRunnable);
            }
        }
    }

    private <T> AppearAnimationProperties getDelays(T[] items) {
        long maxDelay = -1;
        this.mProperties.maxDelayColIndex = -1;
        this.mProperties.maxDelayRowIndex = -1;
        this.mProperties.delays = new long[items.length][];
        for (int row = 0; row < items.length; row++) {
            this.mProperties.delays[row] = new long[1];
            long delay = calculateDelay(row, 0);
            this.mProperties.delays[row][0] = delay;
            if (items[row] != null && delay > maxDelay) {
                maxDelay = delay;
                this.mProperties.maxDelayColIndex = 0;
                this.mProperties.maxDelayRowIndex = row;
            }
        }
        return this.mProperties;
    }

    private <T> AppearAnimationProperties getDelays(T[][] items) {
        long maxDelay = -1;
        this.mProperties.maxDelayColIndex = -1;
        this.mProperties.maxDelayRowIndex = -1;
        this.mProperties.delays = new long[items.length][];
        for (int row = 0; row < items.length; row++) {
            T[] columns = items[row];
            this.mProperties.delays[row] = new long[columns.length];
            for (int col = 0; col < columns.length; col++) {
                long delay = calculateDelay(row, col);
                this.mProperties.delays[row][col] = delay;
                if (items[row][col] != null && delay > maxDelay) {
                    maxDelay = delay;
                    this.mProperties.maxDelayColIndex = col;
                    this.mProperties.maxDelayRowIndex = row;
                }
            }
        }
        return this.mProperties;
    }

    protected long calculateDelay(int row, int col) {
        return (long) ((((double) (row * 40)) + (((double) col) * (Math.pow(row, 0.4d) + 0.4d) * 20.0d)) * ((double) this.mDelayScale));
    }

    public Interpolator getInterpolator() {
        return this.mInterpolator;
    }

    public float getStartTranslation() {
        return this.mStartTranslation;
    }

    @Override
    public void createAnimation(View view, long delay, long duration, float translationY, boolean appearing, Interpolator interpolator, Runnable endRunnable) {
        if (view != null) {
            view.setAlpha(appearing ? 0.0f : 1.0f);
            view.setTranslationY(appearing ? translationY : 0.0f);
            view.animate().alpha(appearing ? 1.0f : 0.0f).translationY(appearing ? 0.0f : translationY).setInterpolator(interpolator).setDuration(duration).setStartDelay(delay);
            if (view.hasOverlappingRendering()) {
                view.animate().withLayer();
            }
            if (endRunnable != null) {
                view.animate().withEndAction(endRunnable);
            }
        }
    }

    public class AppearAnimationProperties {
        public long[][] delays;
        public int maxDelayColIndex;
        public int maxDelayRowIndex;

        public AppearAnimationProperties() {
        }
    }
}
