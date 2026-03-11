package com.android.systemui.statusbar.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

public class StackStateAnimator {
    private ValueAnimator mBottomOverScrollAnimator;
    private long mCurrentAdditionalDelay;
    private int mCurrentLastNotAddedIndex;
    private long mCurrentLength;
    private final int mGoToFullShadeAppearingTranslation;
    private int mHeadsUpAppearHeightBottom;
    public NotificationStackScrollLayout mHostLayout;
    private boolean mShadeExpanded;
    private ValueAnimator mTopOverScrollAnimator;
    private final StackViewState mTmpState = new StackViewState();
    private ArrayList<NotificationStackScrollLayout.AnimationEvent> mNewEvents = new ArrayList<>();
    private ArrayList<View> mNewAddChildren = new ArrayList<>();
    private HashSet<View> mHeadsUpAppearChildren = new HashSet<>();
    private HashSet<View> mHeadsUpDisappearChildren = new HashSet<>();
    private HashSet<Animator> mAnimatorSet = new HashSet<>();
    private Stack<AnimatorListenerAdapter> mAnimationListenerPool = new Stack<>();
    private AnimationFilter mAnimationFilter = new AnimationFilter();
    private ArrayList<View> mChildrenToClearFromOverlay = new ArrayList<>();
    private final Interpolator mHeadsUpAppearInterpolator = new HeadsUpAppearInterpolator();

    public StackStateAnimator(NotificationStackScrollLayout hostLayout) {
        this.mHostLayout = hostLayout;
        this.mGoToFullShadeAppearingTranslation = hostLayout.getContext().getResources().getDimensionPixelSize(R.dimen.go_to_full_shade_appearing_translation);
    }

    public boolean isRunning() {
        return !this.mAnimatorSet.isEmpty();
    }

    public void startAnimationForEvents(ArrayList<NotificationStackScrollLayout.AnimationEvent> mAnimationEvents, StackScrollState finalState, long additionalDelay) {
        processAnimationEvents(mAnimationEvents, finalState);
        int childCount = this.mHostLayout.getChildCount();
        this.mAnimationFilter.applyCombination(this.mNewEvents);
        this.mCurrentAdditionalDelay = additionalDelay;
        this.mCurrentLength = NotificationStackScrollLayout.AnimationEvent.combineLength(this.mNewEvents);
        this.mCurrentLastNotAddedIndex = findLastNotAddedIndex(finalState);
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = (ExpandableView) this.mHostLayout.getChildAt(i);
            StackViewState viewState = finalState.getViewStateForView(child);
            if (viewState != null && child.getVisibility() != 8 && !applyWithoutAnimation(child, viewState, finalState)) {
                startStackAnimations(child, viewState, finalState, i, -1L);
            }
        }
        if (!isRunning()) {
            onAnimationFinished();
        }
        this.mHeadsUpAppearChildren.clear();
        this.mHeadsUpDisappearChildren.clear();
        this.mNewEvents.clear();
        this.mNewAddChildren.clear();
    }

    private boolean applyWithoutAnimation(ExpandableView child, StackViewState viewState, StackScrollState finalState) {
        if (this.mShadeExpanded || getChildTag(child, R.id.translation_y_animator_tag) != null || this.mHeadsUpDisappearChildren.contains(child) || this.mHeadsUpAppearChildren.contains(child) || NotificationStackScrollLayout.isPinnedHeadsUp(child)) {
            return false;
        }
        finalState.applyState(child, viewState);
        return true;
    }

    private int findLastNotAddedIndex(StackScrollState finalState) {
        int childCount = this.mHostLayout.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            ExpandableView child = (ExpandableView) this.mHostLayout.getChildAt(i);
            StackViewState viewState = finalState.getViewStateForView(child);
            if (viewState != null && child.getVisibility() != 8 && !this.mNewAddChildren.contains(child)) {
                return viewState.notGoneIndex;
            }
        }
        return -1;
    }

    public void startStackAnimations(ExpandableView child, StackViewState viewState, StackScrollState finalState, int i, long fixedDelay) {
        boolean z;
        boolean wasAdded = this.mNewAddChildren.contains(child);
        long duration = this.mCurrentLength;
        if (wasAdded && this.mAnimationFilter.hasGoToFullShadeEvent) {
            child.setTranslationY(child.getTranslationY() + this.mGoToFullShadeAppearingTranslation);
            float longerDurationFactor = viewState.notGoneIndex - this.mCurrentLastNotAddedIndex;
            duration = 514 + ((long) (100.0f * ((float) Math.pow(longerDurationFactor, 0.699999988079071d))));
        }
        boolean yTranslationChanging = child.getTranslationY() != viewState.yTranslation;
        boolean zTranslationChanging = child.getTranslationZ() != viewState.zTranslation;
        boolean alphaChanging = viewState.alpha != child.getAlpha();
        boolean heightChanging = viewState.height != child.getActualHeight();
        boolean shadowAlphaChanging = viewState.shadowAlpha != child.getShadowAlpha();
        boolean darkChanging = viewState.dark != child.isDark();
        boolean topInsetChanging = viewState.clipTopAmount != child.getClipTopAmount();
        boolean hasDelays = this.mAnimationFilter.hasDelays;
        if (yTranslationChanging || zTranslationChanging || alphaChanging || heightChanging || topInsetChanging || darkChanging) {
            z = true;
        } else {
            z = shadowAlphaChanging;
        }
        long delay = 0;
        if (fixedDelay != -1) {
            delay = fixedDelay;
        } else if ((hasDelays && z) || wasAdded) {
            delay = this.mCurrentAdditionalDelay + calculateChildAnimationDelay(viewState, finalState);
        }
        startViewAnimations(child, viewState, delay, duration);
        if (heightChanging) {
            startHeightAnimation(child, viewState, duration, delay);
        } else {
            abortAnimation(child, R.id.height_animator_tag);
        }
        if (shadowAlphaChanging) {
            startShadowAlphaAnimation(child, viewState, duration, delay);
        } else {
            abortAnimation(child, R.id.shadow_alpha_animator_tag);
        }
        if (topInsetChanging) {
            startInsetAnimation(child, viewState, duration, delay);
        } else {
            abortAnimation(child, R.id.top_inset_animator_tag);
        }
        child.setDimmed(viewState.dimmed, this.mAnimationFilter.animateDimmed);
        child.setBelowSpeedBump(viewState.belowSpeedBump);
        child.setHideSensitive(viewState.hideSensitive, this.mAnimationFilter.animateHideSensitive, delay, duration);
        child.setDark(viewState.dark, this.mAnimationFilter.animateDark, delay);
        if (wasAdded) {
            child.performAddAnimation(delay, this.mCurrentLength);
        }
        if (!(child instanceof ExpandableNotificationRow)) {
            return;
        }
        ExpandableNotificationRow row = (ExpandableNotificationRow) child;
        row.startChildAnimation(finalState, this, delay, duration);
    }

    public void startViewAnimations(View child, ViewState viewState, long delay, long duration) {
        boolean wasVisible = child.getVisibility() == 0;
        float alpha = viewState.alpha;
        if (!wasVisible && ((alpha != 0.0f || child.getAlpha() != 0.0f) && !viewState.gone && !viewState.hidden)) {
            child.setVisibility(0);
        }
        boolean yTranslationChanging = child.getTranslationY() != viewState.yTranslation;
        boolean zTranslationChanging = child.getTranslationZ() != viewState.zTranslation;
        float childAlpha = child.getAlpha();
        boolean alphaChanging = viewState.alpha != childAlpha;
        if (child instanceof ExpandableView) {
            alphaChanging &= !((ExpandableView) child).willBeGone();
        }
        if (yTranslationChanging) {
            startYTranslationAnimation(child, viewState, duration, delay);
        } else {
            abortAnimation(child, R.id.translation_y_animator_tag);
        }
        if (zTranslationChanging) {
            startZTranslationAnimation(child, viewState, duration, delay);
        } else {
            abortAnimation(child, R.id.translation_z_animator_tag);
        }
        if (alphaChanging && child.getTranslationX() == 0.0f) {
            startAlphaAnimation(child, viewState, duration, delay);
        } else {
            abortAnimation(child, R.id.alpha_animator_tag);
        }
    }

    private void abortAnimation(View child, int animatorTag) {
        Animator previousAnimator = (Animator) getChildTag(child, animatorTag);
        if (previousAnimator == null) {
            return;
        }
        previousAnimator.cancel();
    }

    private long calculateChildAnimationDelay(StackViewState viewState, StackScrollState finalState) {
        View viewAfterChangingView;
        if (this.mAnimationFilter.hasDarkEvent) {
            return calculateDelayDark(viewState);
        }
        if (this.mAnimationFilter.hasGoToFullShadeEvent) {
            return calculateDelayGoToFullShade(viewState);
        }
        if (this.mAnimationFilter.hasHeadsUpDisappearClickEvent) {
            return 120L;
        }
        long minDelay = 0;
        for (NotificationStackScrollLayout.AnimationEvent event : this.mNewEvents) {
            long delayPerElement = 80;
            switch (event.animationType) {
                case 0:
                    int ownIndex = viewState.notGoneIndex;
                    int changingIndex = finalState.getViewStateForView(event.changingView).notGoneIndex;
                    int difference = Math.abs(ownIndex - changingIndex);
                    long delay = ((long) (2 - Math.max(0, Math.min(2, difference - 1)))) * 80;
                    minDelay = Math.max(delay, minDelay);
                    continue;
                case 2:
                    delayPerElement = 32;
                    break;
            }
            int ownIndex2 = viewState.notGoneIndex;
            boolean noNextView = event.viewAfterChangingView == null;
            if (noNextView) {
                viewAfterChangingView = this.mHostLayout.getLastChildNotGone();
            } else {
                viewAfterChangingView = event.viewAfterChangingView;
            }
            int nextIndex = finalState.getViewStateForView(viewAfterChangingView).notGoneIndex;
            if (ownIndex2 >= nextIndex) {
                ownIndex2++;
            }
            int difference2 = Math.abs(ownIndex2 - nextIndex);
            long delay2 = ((long) Math.max(0, Math.min(2, difference2 - 1))) * delayPerElement;
            minDelay = Math.max(delay2, minDelay);
        }
        return minDelay;
    }

    private long calculateDelayDark(StackViewState viewState) {
        int referenceIndex;
        if (this.mAnimationFilter.darkAnimationOriginIndex == -1) {
            referenceIndex = 0;
        } else if (this.mAnimationFilter.darkAnimationOriginIndex == -2) {
            referenceIndex = this.mHostLayout.getNotGoneChildCount() - 1;
        } else {
            referenceIndex = this.mAnimationFilter.darkAnimationOriginIndex;
        }
        return Math.abs(referenceIndex - viewState.notGoneIndex) * 24;
    }

    private long calculateDelayGoToFullShade(StackViewState viewState) {
        float index = viewState.notGoneIndex;
        return (long) (48.0f * ((float) Math.pow(index, 0.699999988079071d)));
    }

    private void startShadowAlphaAnimation(final ExpandableView child, StackViewState viewState, long duration, long delay) {
        Float previousStartValue = (Float) getChildTag(child, R.id.shadow_alpha_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(child, R.id.shadow_alpha_animator_end_value_tag);
        float newEndValue = viewState.shadowAlpha;
        if (previousEndValue != null && previousEndValue.floatValue() == newEndValue) {
            return;
        }
        ValueAnimator previousAnimator = (ValueAnimator) getChildTag(child, R.id.shadow_alpha_animator_tag);
        if (!this.mAnimationFilter.animateShadowAlpha) {
            if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float relativeDiff = newEndValue - previousEndValue.floatValue();
                float newStartValue = previousStartValue.floatValue() + relativeDiff;
                values[0].setFloatValues(newStartValue, newEndValue);
                child.setTag(R.id.shadow_alpha_animator_start_value_tag, Float.valueOf(newStartValue));
                child.setTag(R.id.shadow_alpha_animator_end_value_tag, Float.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            }
            child.setShadowAlpha(newEndValue);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(child.getShadowAlpha(), newEndValue);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                child.setShadowAlpha(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
        animator.setDuration(newDuration);
        if (delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
            animator.setStartDelay(delay);
        }
        animator.addListener(getGlobalAnimationFinishedListener());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                child.setTag(R.id.shadow_alpha_animator_tag, null);
                child.setTag(R.id.shadow_alpha_animator_start_value_tag, null);
                child.setTag(R.id.shadow_alpha_animator_end_value_tag, null);
            }
        });
        startAnimator(animator);
        child.setTag(R.id.shadow_alpha_animator_tag, animator);
        child.setTag(R.id.shadow_alpha_animator_start_value_tag, Float.valueOf(child.getShadowAlpha()));
        child.setTag(R.id.shadow_alpha_animator_end_value_tag, Float.valueOf(newEndValue));
    }

    private void startHeightAnimation(final ExpandableView child, StackViewState viewState, long duration, long delay) {
        Integer previousStartValue = (Integer) getChildTag(child, R.id.height_animator_start_value_tag);
        Integer previousEndValue = (Integer) getChildTag(child, R.id.height_animator_end_value_tag);
        int newEndValue = viewState.height;
        if (previousEndValue != null && previousEndValue.intValue() == newEndValue) {
            return;
        }
        ValueAnimator previousAnimator = (ValueAnimator) getChildTag(child, R.id.height_animator_tag);
        if (!this.mAnimationFilter.animateHeight) {
            if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                int relativeDiff = newEndValue - previousEndValue.intValue();
                int newStartValue = previousStartValue.intValue() + relativeDiff;
                values[0].setIntValues(newStartValue, newEndValue);
                child.setTag(R.id.height_animator_start_value_tag, Integer.valueOf(newStartValue));
                child.setTag(R.id.height_animator_end_value_tag, Integer.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            }
            child.setActualHeight(newEndValue, false);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(child.getActualHeight(), newEndValue);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                child.setActualHeight(((Integer) animation.getAnimatedValue()).intValue(), false);
            }
        });
        animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
        animator.setDuration(newDuration);
        if (delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
            animator.setStartDelay(delay);
        }
        animator.addListener(getGlobalAnimationFinishedListener());
        animator.addListener(new AnimatorListenerAdapter() {
            boolean mWasCancelled;

            @Override
            public void onAnimationEnd(Animator animation) {
                child.setTag(R.id.height_animator_tag, null);
                child.setTag(R.id.height_animator_start_value_tag, null);
                child.setTag(R.id.height_animator_end_value_tag, null);
                child.setActualHeightAnimating(false);
                if (this.mWasCancelled || !(child instanceof ExpandableNotificationRow)) {
                    return;
                }
                ((ExpandableNotificationRow) child).setGroupExpansionChanging(false);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                this.mWasCancelled = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mWasCancelled = true;
            }
        });
        startAnimator(animator);
        child.setTag(R.id.height_animator_tag, animator);
        child.setTag(R.id.height_animator_start_value_tag, Integer.valueOf(child.getActualHeight()));
        child.setTag(R.id.height_animator_end_value_tag, Integer.valueOf(newEndValue));
        child.setActualHeightAnimating(true);
    }

    private void startInsetAnimation(final ExpandableView child, StackViewState viewState, long duration, long delay) {
        Integer previousStartValue = (Integer) getChildTag(child, R.id.top_inset_animator_start_value_tag);
        Integer previousEndValue = (Integer) getChildTag(child, R.id.top_inset_animator_end_value_tag);
        int newEndValue = viewState.clipTopAmount;
        if (previousEndValue != null && previousEndValue.intValue() == newEndValue) {
            return;
        }
        ValueAnimator previousAnimator = (ValueAnimator) getChildTag(child, R.id.top_inset_animator_tag);
        if (!this.mAnimationFilter.animateTopInset) {
            if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                int relativeDiff = newEndValue - previousEndValue.intValue();
                int newStartValue = previousStartValue.intValue() + relativeDiff;
                values[0].setIntValues(newStartValue, newEndValue);
                child.setTag(R.id.top_inset_animator_start_value_tag, Integer.valueOf(newStartValue));
                child.setTag(R.id.top_inset_animator_end_value_tag, Integer.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            }
            child.setClipTopAmount(newEndValue);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(child.getClipTopAmount(), newEndValue);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                child.setClipTopAmount(((Integer) animation.getAnimatedValue()).intValue());
            }
        });
        animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
        animator.setDuration(newDuration);
        if (delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
            animator.setStartDelay(delay);
        }
        animator.addListener(getGlobalAnimationFinishedListener());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                child.setTag(R.id.top_inset_animator_tag, null);
                child.setTag(R.id.top_inset_animator_start_value_tag, null);
                child.setTag(R.id.top_inset_animator_end_value_tag, null);
            }
        });
        startAnimator(animator);
        child.setTag(R.id.top_inset_animator_tag, animator);
        child.setTag(R.id.top_inset_animator_start_value_tag, Integer.valueOf(child.getClipTopAmount()));
        child.setTag(R.id.top_inset_animator_end_value_tag, Integer.valueOf(newEndValue));
    }

    private void startAlphaAnimation(final View child, ViewState viewState, long duration, long delay) {
        Float previousStartValue = (Float) getChildTag(child, R.id.alpha_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(child, R.id.alpha_animator_end_value_tag);
        final float newEndValue = viewState.alpha;
        if (previousEndValue != null && previousEndValue.floatValue() == newEndValue) {
            return;
        }
        ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(child, R.id.alpha_animator_tag);
        if (!this.mAnimationFilter.animateAlpha) {
            if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float relativeDiff = newEndValue - previousEndValue.floatValue();
                float newStartValue = previousStartValue.floatValue() + relativeDiff;
                values[0].setFloatValues(newStartValue, newEndValue);
                child.setTag(R.id.alpha_animator_start_value_tag, Float.valueOf(newStartValue));
                child.setTag(R.id.alpha_animator_end_value_tag, Float.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            }
            child.setAlpha(newEndValue);
            if (newEndValue == 0.0f) {
                child.setVisibility(4);
            }
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(child, (Property<View, Float>) View.ALPHA, child.getAlpha(), newEndValue);
        animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        child.setLayerType(2, null);
        animator.addListener(new AnimatorListenerAdapter() {
            public boolean mWasCancelled;

            @Override
            public void onAnimationEnd(Animator animation) {
                child.setLayerType(0, null);
                if (newEndValue == 0.0f && !this.mWasCancelled) {
                    child.setVisibility(4);
                }
                child.setTag(R.id.alpha_animator_tag, null);
                child.setTag(R.id.alpha_animator_start_value_tag, null);
                child.setTag(R.id.alpha_animator_end_value_tag, null);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mWasCancelled = true;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                this.mWasCancelled = false;
            }
        });
        long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
        animator.setDuration(newDuration);
        if (delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
            animator.setStartDelay(delay);
        }
        animator.addListener(getGlobalAnimationFinishedListener());
        startAnimator(animator);
        child.setTag(R.id.alpha_animator_tag, animator);
        child.setTag(R.id.alpha_animator_start_value_tag, Float.valueOf(child.getAlpha()));
        child.setTag(R.id.alpha_animator_end_value_tag, Float.valueOf(newEndValue));
    }

    private void startZTranslationAnimation(final View child, ViewState viewState, long duration, long delay) {
        Float previousStartValue = (Float) getChildTag(child, R.id.translation_z_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(child, R.id.translation_z_animator_end_value_tag);
        float newEndValue = viewState.zTranslation;
        if (previousEndValue != null && previousEndValue.floatValue() == newEndValue) {
            return;
        }
        ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(child, R.id.translation_z_animator_tag);
        if (!this.mAnimationFilter.animateZ) {
            if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float relativeDiff = newEndValue - previousEndValue.floatValue();
                float newStartValue = previousStartValue.floatValue() + relativeDiff;
                values[0].setFloatValues(newStartValue, newEndValue);
                child.setTag(R.id.translation_z_animator_start_value_tag, Float.valueOf(newStartValue));
                child.setTag(R.id.translation_z_animator_end_value_tag, Float.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            }
            child.setTranslationZ(newEndValue);
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(child, (Property<View, Float>) View.TRANSLATION_Z, child.getTranslationZ(), newEndValue);
        animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
        animator.setDuration(newDuration);
        if (delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
            animator.setStartDelay(delay);
        }
        animator.addListener(getGlobalAnimationFinishedListener());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                child.setTag(R.id.translation_z_animator_tag, null);
                child.setTag(R.id.translation_z_animator_start_value_tag, null);
                child.setTag(R.id.translation_z_animator_end_value_tag, null);
            }
        });
        startAnimator(animator);
        child.setTag(R.id.translation_z_animator_tag, animator);
        child.setTag(R.id.translation_z_animator_start_value_tag, Float.valueOf(child.getTranslationZ()));
        child.setTag(R.id.translation_z_animator_end_value_tag, Float.valueOf(newEndValue));
    }

    private void startYTranslationAnimation(final View child, ViewState viewState, long duration, long delay) {
        Float previousStartValue = (Float) getChildTag(child, R.id.translation_y_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(child, R.id.translation_y_animator_end_value_tag);
        float newEndValue = viewState.yTranslation;
        if (previousEndValue != null && previousEndValue.floatValue() == newEndValue) {
            return;
        }
        ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(child, R.id.translation_y_animator_tag);
        if (!this.mAnimationFilter.animateY) {
            if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float relativeDiff = newEndValue - previousEndValue.floatValue();
                float newStartValue = previousStartValue.floatValue() + relativeDiff;
                values[0].setFloatValues(newStartValue, newEndValue);
                child.setTag(R.id.translation_y_animator_start_value_tag, Float.valueOf(newStartValue));
                child.setTag(R.id.translation_y_animator_end_value_tag, Float.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                return;
            }
            child.setTranslationY(newEndValue);
            return;
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(child, (Property<View, Float>) View.TRANSLATION_Y, child.getTranslationY(), newEndValue);
        Interpolator interpolator = this.mHeadsUpAppearChildren.contains(child) ? this.mHeadsUpAppearInterpolator : Interpolators.FAST_OUT_SLOW_IN;
        animator.setInterpolator(interpolator);
        long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
        animator.setDuration(newDuration);
        if (delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
            animator.setStartDelay(delay);
        }
        animator.addListener(getGlobalAnimationFinishedListener());
        final boolean isHeadsUpDisappear = this.mHeadsUpDisappearChildren.contains(child);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                HeadsUpManager.setIsClickedNotification(child, false);
                child.setTag(R.id.translation_y_animator_tag, null);
                child.setTag(R.id.translation_y_animator_start_value_tag, null);
                child.setTag(R.id.translation_y_animator_end_value_tag, null);
                if (!isHeadsUpDisappear) {
                    return;
                }
                ((ExpandableNotificationRow) child).setHeadsupDisappearRunning(false);
            }
        });
        startAnimator(animator);
        child.setTag(R.id.translation_y_animator_tag, animator);
        child.setTag(R.id.translation_y_animator_start_value_tag, Float.valueOf(child.getTranslationY()));
        child.setTag(R.id.translation_y_animator_end_value_tag, Float.valueOf(newEndValue));
    }

    private void startAnimator(ValueAnimator animator) {
        this.mAnimatorSet.add(animator);
        animator.start();
    }

    private AnimatorListenerAdapter getGlobalAnimationFinishedListener() {
        if (!this.mAnimationListenerPool.empty()) {
            return this.mAnimationListenerPool.pop();
        }
        return new AnimatorListenerAdapter() {
            private boolean mWasCancelled;

            @Override
            public void onAnimationEnd(Animator animation) {
                StackStateAnimator.this.mAnimatorSet.remove(animation);
                if (StackStateAnimator.this.mAnimatorSet.isEmpty() && !this.mWasCancelled) {
                    StackStateAnimator.this.onAnimationFinished();
                }
                StackStateAnimator.this.mAnimationListenerPool.push(this);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mWasCancelled = true;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                this.mWasCancelled = false;
            }
        };
    }

    public static <T> T getChildTag(View view, int i) {
        return (T) view.getTag(i);
    }

    private long cancelAnimatorAndGetNewDuration(long duration, ValueAnimator previousAnimator) {
        if (previousAnimator == null) {
            return duration;
        }
        long newDuration = Math.max(previousAnimator.getDuration() - previousAnimator.getCurrentPlayTime(), duration);
        previousAnimator.cancel();
        return newDuration;
    }

    public void onAnimationFinished() {
        this.mHostLayout.onChildAnimationFinished();
        for (View v : this.mChildrenToClearFromOverlay) {
            removeFromOverlay(v);
        }
        this.mChildrenToClearFromOverlay.clear();
    }

    private void processAnimationEvents(ArrayList<NotificationStackScrollLayout.AnimationEvent> animationEvents, StackScrollState finalState) {
        for (NotificationStackScrollLayout.AnimationEvent event : animationEvents) {
            final ExpandableView changingView = (ExpandableView) event.changingView;
            if (event.animationType == 0) {
                StackViewState viewState = finalState.getViewStateForView(changingView);
                if (viewState != null) {
                    finalState.applyState(changingView, viewState);
                    this.mNewAddChildren.add(changingView);
                    this.mNewEvents.add(event);
                }
            } else {
                if (event.animationType == 1) {
                    if (changingView.getVisibility() == 8) {
                        removeFromOverlay(changingView);
                    } else {
                        StackViewState viewState2 = finalState.getViewStateForView(event.viewAfterChangingView);
                        int actualHeight = changingView.getActualHeight();
                        float translationDirection = -1.0f;
                        if (viewState2 != null) {
                            float translationDirection2 = ((viewState2.yTranslation - (changingView.getTranslationY() + (actualHeight / 2.0f))) * 2.0f) / actualHeight;
                            translationDirection = Math.max(Math.min(translationDirection2, 1.0f), -1.0f);
                        }
                        changingView.performRemoveAnimation(464L, translationDirection, new Runnable() {
                            @Override
                            public void run() {
                                StackStateAnimator.removeFromOverlay(changingView);
                            }
                        });
                    }
                } else if (event.animationType == 2) {
                    this.mHostLayout.getOverlay().remove(changingView);
                    if (Math.abs(changingView.getTranslation()) == changingView.getWidth() && changingView.getTransientContainer() != null) {
                        changingView.getTransientContainer().removeTransientView(changingView);
                    }
                } else if (event.animationType == 13) {
                    ExpandableNotificationRow row = (ExpandableNotificationRow) event.changingView;
                    row.prepareExpansionChanged(finalState);
                } else if (event.animationType == 14) {
                    this.mTmpState.copyFrom(finalState.getViewStateForView(changingView));
                    if (event.headsUpFromBottom) {
                        this.mTmpState.yTranslation = this.mHeadsUpAppearHeightBottom;
                    } else {
                        this.mTmpState.yTranslation = -this.mTmpState.height;
                    }
                    this.mHeadsUpAppearChildren.add(changingView);
                    finalState.applyState(changingView, this.mTmpState);
                } else if (event.animationType == 15 || event.animationType == 16) {
                    this.mHeadsUpDisappearChildren.add(changingView);
                    if (changingView.getParent() == null) {
                        this.mHostLayout.getOverlay().add(changingView);
                        this.mTmpState.initFrom(changingView);
                        this.mTmpState.yTranslation = -changingView.getActualHeight();
                        this.mAnimationFilter.animateY = true;
                        startViewAnimations(changingView, this.mTmpState, event.animationType == 16 ? 120 : 0, 230L);
                        this.mChildrenToClearFromOverlay.add(changingView);
                    }
                }
                this.mNewEvents.add(event);
            }
        }
    }

    public static void removeFromOverlay(View changingView) {
        ViewGroup parent = (ViewGroup) changingView.getParent();
        if (parent == null) {
            return;
        }
        parent.removeView(changingView);
    }

    public void animateOverScrollToAmount(float targetAmount, final boolean onTop, final boolean isRubberbanded) {
        float startOverScrollAmount = this.mHostLayout.getCurrentOverScrollAmount(onTop);
        if (targetAmount == startOverScrollAmount) {
            return;
        }
        cancelOverScrollAnimators(onTop);
        ValueAnimator overScrollAnimator = ValueAnimator.ofFloat(startOverScrollAmount, targetAmount);
        overScrollAnimator.setDuration(360L);
        overScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentOverScroll = ((Float) animation.getAnimatedValue()).floatValue();
                StackStateAnimator.this.mHostLayout.setOverScrollAmount(currentOverScroll, onTop, false, false, isRubberbanded);
            }
        });
        overScrollAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        overScrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onTop) {
                    StackStateAnimator.this.mTopOverScrollAnimator = null;
                } else {
                    StackStateAnimator.this.mBottomOverScrollAnimator = null;
                }
            }
        });
        overScrollAnimator.start();
        if (onTop) {
            this.mTopOverScrollAnimator = overScrollAnimator;
        } else {
            this.mBottomOverScrollAnimator = overScrollAnimator;
        }
    }

    public void cancelOverScrollAnimators(boolean onTop) {
        ValueAnimator currentAnimator = onTop ? this.mTopOverScrollAnimator : this.mBottomOverScrollAnimator;
        if (currentAnimator == null) {
            return;
        }
        currentAnimator.cancel();
    }

    public static int getFinalActualHeight(ExpandableView view) {
        if (view == null) {
            return 0;
        }
        ValueAnimator heightAnimator = (ValueAnimator) getChildTag(view, R.id.height_animator_tag);
        if (heightAnimator == null) {
            return view.getActualHeight();
        }
        return ((Integer) getChildTag(view, R.id.height_animator_end_value_tag)).intValue();
    }

    public static float getFinalTranslationY(View view) {
        if (view == null) {
            return 0.0f;
        }
        ValueAnimator yAnimator = (ValueAnimator) getChildTag(view, R.id.translation_y_animator_tag);
        if (yAnimator == null) {
            return view.getTranslationY();
        }
        return ((Float) getChildTag(view, R.id.translation_y_animator_end_value_tag)).floatValue();
    }

    public void setHeadsUpAppearHeightBottom(int headsUpAppearHeightBottom) {
        this.mHeadsUpAppearHeightBottom = headsUpAppearHeightBottom;
    }

    public void setShadeExpanded(boolean shadeExpanded) {
        this.mShadeExpanded = shadeExpanded;
    }
}
