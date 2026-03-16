package com.android.systemui.statusbar.stack;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.SpeedBumpView;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.stack.StackScrollState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class StackStateAnimator {
    private ValueAnimator mBottomOverScrollAnimator;
    private long mCurrentAdditionalDelay;
    private int mCurrentLastNotAddedIndex;
    private long mCurrentLength;
    private final Interpolator mFastOutSlowInInterpolator;
    private final int mGoToFullShadeAppearingTranslation;
    public NotificationStackScrollLayout mHostLayout;
    private ValueAnimator mTopOverScrollAnimator;
    private ArrayList<NotificationStackScrollLayout.AnimationEvent> mNewEvents = new ArrayList<>();
    private ArrayList<View> mNewAddChildren = new ArrayList<>();
    private Set<Animator> mAnimatorSet = new HashSet();
    private Stack<AnimatorListenerAdapter> mAnimationListenerPool = new Stack<>();
    private AnimationFilter mAnimationFilter = new AnimationFilter();

    public StackStateAnimator(NotificationStackScrollLayout hostLayout) {
        this.mHostLayout = hostLayout;
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(hostLayout.getContext(), R.interpolator.fast_out_slow_in);
        this.mGoToFullShadeAppearingTranslation = hostLayout.getContext().getResources().getDimensionPixelSize(com.android.systemui.R.dimen.go_to_full_shade_appearing_translation);
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
            StackScrollState.ViewState viewState = finalState.getViewStateForView(child);
            if (viewState != null && child.getVisibility() != 8) {
                child.setClipBounds(null);
                startAnimations(child, viewState, finalState, i);
            }
        }
        if (!isRunning()) {
            onAnimationFinished();
        }
        this.mNewEvents.clear();
        this.mNewAddChildren.clear();
    }

    private int findLastNotAddedIndex(StackScrollState finalState) {
        int childCount = this.mHostLayout.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            ExpandableView child = (ExpandableView) this.mHostLayout.getChildAt(i);
            StackScrollState.ViewState viewState = finalState.getViewStateForView(child);
            if (viewState != null && child.getVisibility() != 8 && !this.mNewAddChildren.contains(child)) {
                return viewState.notGoneIndex;
            }
        }
        return -1;
    }

    private void startAnimations(ExpandableView child, StackScrollState.ViewState viewState, StackScrollState finalState, int i) {
        int childVisibility = child.getVisibility();
        boolean wasVisible = childVisibility == 0;
        float alpha = viewState.alpha;
        if (!wasVisible && alpha != 0.0f && !viewState.gone) {
            child.setVisibility(0);
        }
        boolean yTranslationChanging = child.getTranslationY() != viewState.yTranslation;
        boolean zTranslationChanging = child.getTranslationZ() != viewState.zTranslation;
        boolean scaleChanging = child.getScaleX() != viewState.scale;
        boolean alphaChanging = alpha != child.getAlpha();
        boolean heightChanging = viewState.height != child.getActualHeight();
        boolean darkChanging = viewState.dark != child.isDark();
        boolean topInsetChanging = viewState.clipTopAmount != child.getClipTopAmount();
        boolean wasAdded = this.mNewAddChildren.contains(child);
        boolean hasDelays = this.mAnimationFilter.hasDelays;
        boolean isDelayRelevant = yTranslationChanging || zTranslationChanging || scaleChanging || alphaChanging || heightChanging || topInsetChanging || darkChanging;
        long delay = 0;
        long duration = this.mCurrentLength;
        if ((hasDelays && isDelayRelevant) || wasAdded) {
            delay = this.mCurrentAdditionalDelay + calculateChildAnimationDelay(viewState, finalState);
        }
        if (wasAdded && this.mAnimationFilter.hasGoToFullShadeEvent) {
            child.setTranslationY(child.getTranslationY() + this.mGoToFullShadeAppearingTranslation);
            yTranslationChanging = true;
            float longerDurationFactor = viewState.notGoneIndex - this.mCurrentLastNotAddedIndex;
            duration = 514 + ((long) (100.0f * ((float) Math.pow(longerDurationFactor, 0.699999988079071d))));
        }
        if (yTranslationChanging) {
            if (wasAdded && !this.mAnimationFilter.hasGoToFullShadeEvent) {
                child.setTranslationY(viewState.yTranslation);
            } else {
                startYTranslationAnimation(child, viewState, duration, delay);
            }
        }
        if (zTranslationChanging) {
            if (wasAdded) {
                child.setTranslationZ(viewState.zTranslation);
            } else {
                startZTranslationAnimation(child, viewState, duration, delay);
            }
        }
        if (scaleChanging) {
            if (wasAdded) {
                child.setScaleX(viewState.scale);
                child.setScaleY(viewState.scale);
            } else {
                startScaleAnimation(child, viewState, duration);
            }
        }
        if (alphaChanging && child.getTranslationX() == 0.0f) {
            if (wasAdded) {
                child.setAlpha(viewState.alpha);
            } else {
                startAlphaAnimation(child, viewState, duration, delay);
            }
        }
        if (heightChanging && child.getActualHeight() != 0) {
            if (wasAdded) {
                child.setActualHeight(viewState.height, false);
            } else {
                startHeightAnimation(child, viewState, duration, delay);
            }
        }
        if (topInsetChanging) {
            if (wasAdded) {
                child.setClipTopAmount(viewState.clipTopAmount);
            } else {
                startInsetAnimation(child, viewState, duration, delay);
            }
        }
        child.setDimmed(viewState.dimmed, (!this.mAnimationFilter.animateDimmed || wasAdded || wasAdded) ? false : true);
        child.setDark(viewState.dark, this.mAnimationFilter.animateDark && !wasAdded, delay);
        child.setBelowSpeedBump(viewState.belowSpeedBump);
        child.setHideSensitive(viewState.hideSensitive, (!this.mAnimationFilter.animateHideSensitive || wasAdded || wasAdded) ? false : true, delay, duration);
        if (wasAdded) {
            child.performAddAnimation(delay, this.mCurrentLength);
        }
        if (child instanceof SpeedBumpView) {
            finalState.performSpeedBumpAnimation(i, (SpeedBumpView) child, viewState, delay + duration);
        }
    }

    private long calculateChildAnimationDelay(StackScrollState.ViewState viewState, StackScrollState finalState) {
        if (this.mAnimationFilter.hasDarkEvent) {
            return calculateDelayDark(viewState);
        }
        if (this.mAnimationFilter.hasGoToFullShadeEvent) {
            return calculateDelayGoToFullShade(viewState);
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
            View viewAfterChangingView = noNextView ? this.mHostLayout.getLastChildNotGone() : event.viewAfterChangingView;
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

    private long calculateDelayDark(StackScrollState.ViewState viewState) {
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

    private long calculateDelayGoToFullShade(StackScrollState.ViewState viewState) {
        float index = viewState.notGoneIndex;
        return (long) (48.0f * ((float) Math.pow(index, 0.699999988079071d)));
    }

    private void startHeightAnimation(final ExpandableView child, StackScrollState.ViewState viewState, long duration, long delay) {
        Integer previousStartValue = (Integer) getChildTag(child, com.android.systemui.R.id.height_animator_start_value_tag);
        Integer previousEndValue = (Integer) getChildTag(child, com.android.systemui.R.id.height_animator_end_value_tag);
        int newEndValue = viewState.height;
        if (previousEndValue == null || previousEndValue.intValue() != newEndValue) {
            ValueAnimator previousAnimator = (ValueAnimator) getChildTag(child, com.android.systemui.R.id.height_animator_tag);
            if (!this.mAnimationFilter.animateHeight) {
                if (previousAnimator != null) {
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    int relativeDiff = newEndValue - previousEndValue.intValue();
                    int newStartValue = previousStartValue.intValue() + relativeDiff;
                    values[0].setIntValues(newStartValue, newEndValue);
                    child.setTag(com.android.systemui.R.id.height_animator_start_value_tag, Integer.valueOf(newStartValue));
                    child.setTag(com.android.systemui.R.id.height_animator_end_value_tag, Integer.valueOf(newEndValue));
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
            animator.setInterpolator(this.mFastOutSlowInInterpolator);
            long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
            animator.setDuration(newDuration);
            if (delay > 0 && (previousAnimator == null || !previousAnimator.isRunning())) {
                animator.setStartDelay(delay);
            }
            animator.addListener(getGlobalAnimationFinishedListener());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    child.setTag(com.android.systemui.R.id.height_animator_tag, null);
                    child.setTag(com.android.systemui.R.id.height_animator_start_value_tag, null);
                    child.setTag(com.android.systemui.R.id.height_animator_end_value_tag, null);
                }
            });
            startAnimator(animator);
            child.setTag(com.android.systemui.R.id.height_animator_tag, animator);
            child.setTag(com.android.systemui.R.id.height_animator_start_value_tag, Integer.valueOf(child.getActualHeight()));
            child.setTag(com.android.systemui.R.id.height_animator_end_value_tag, Integer.valueOf(newEndValue));
        }
    }

    private void startInsetAnimation(final ExpandableView child, StackScrollState.ViewState viewState, long duration, long delay) {
        Integer previousStartValue = (Integer) getChildTag(child, com.android.systemui.R.id.top_inset_animator_start_value_tag);
        Integer previousEndValue = (Integer) getChildTag(child, com.android.systemui.R.id.top_inset_animator_end_value_tag);
        int newEndValue = viewState.clipTopAmount;
        if (previousEndValue == null || previousEndValue.intValue() != newEndValue) {
            ValueAnimator previousAnimator = (ValueAnimator) getChildTag(child, com.android.systemui.R.id.top_inset_animator_tag);
            if (!this.mAnimationFilter.animateTopInset) {
                if (previousAnimator != null) {
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    int relativeDiff = newEndValue - previousEndValue.intValue();
                    int newStartValue = previousStartValue.intValue() + relativeDiff;
                    values[0].setIntValues(newStartValue, newEndValue);
                    child.setTag(com.android.systemui.R.id.top_inset_animator_start_value_tag, Integer.valueOf(newStartValue));
                    child.setTag(com.android.systemui.R.id.top_inset_animator_end_value_tag, Integer.valueOf(newEndValue));
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
            animator.setInterpolator(this.mFastOutSlowInInterpolator);
            long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
            animator.setDuration(newDuration);
            if (delay > 0 && (previousAnimator == null || !previousAnimator.isRunning())) {
                animator.setStartDelay(delay);
            }
            animator.addListener(getGlobalAnimationFinishedListener());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    child.setTag(com.android.systemui.R.id.top_inset_animator_tag, null);
                    child.setTag(com.android.systemui.R.id.top_inset_animator_start_value_tag, null);
                    child.setTag(com.android.systemui.R.id.top_inset_animator_end_value_tag, null);
                }
            });
            startAnimator(animator);
            child.setTag(com.android.systemui.R.id.top_inset_animator_tag, animator);
            child.setTag(com.android.systemui.R.id.top_inset_animator_start_value_tag, Integer.valueOf(child.getClipTopAmount()));
            child.setTag(com.android.systemui.R.id.top_inset_animator_end_value_tag, Integer.valueOf(newEndValue));
        }
    }

    private void startAlphaAnimation(final ExpandableView child, StackScrollState.ViewState viewState, long duration, long delay) {
        Float previousStartValue = (Float) getChildTag(child, com.android.systemui.R.id.alpha_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(child, com.android.systemui.R.id.alpha_animator_end_value_tag);
        final float newEndValue = viewState.alpha;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(child, com.android.systemui.R.id.alpha_animator_tag);
            if (!this.mAnimationFilter.animateAlpha) {
                if (previousAnimator != null) {
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    float relativeDiff = newEndValue - previousEndValue.floatValue();
                    float newStartValue = previousStartValue.floatValue() + relativeDiff;
                    values[0].setFloatValues(newStartValue, newEndValue);
                    child.setTag(com.android.systemui.R.id.alpha_animator_start_value_tag, Float.valueOf(newStartValue));
                    child.setTag(com.android.systemui.R.id.alpha_animator_end_value_tag, Float.valueOf(newEndValue));
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                    return;
                }
                child.setAlpha(newEndValue);
                if (newEndValue == 0.0f) {
                    child.setVisibility(4);
                }
            }
            ObjectAnimator animator = ObjectAnimator.ofFloat(child, (Property<ExpandableView, Float>) View.ALPHA, child.getAlpha(), newEndValue);
            animator.setInterpolator(this.mFastOutSlowInInterpolator);
            child.setLayerType(2, null);
            animator.addListener(new AnimatorListenerAdapter() {
                public boolean mWasCancelled;

                @Override
                public void onAnimationEnd(Animator animation) {
                    child.setLayerType(0, null);
                    if (newEndValue == 0.0f && !this.mWasCancelled) {
                        child.setVisibility(4);
                    }
                    child.setTag(com.android.systemui.R.id.alpha_animator_tag, null);
                    child.setTag(com.android.systemui.R.id.alpha_animator_start_value_tag, null);
                    child.setTag(com.android.systemui.R.id.alpha_animator_end_value_tag, null);
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
            if (delay > 0 && (previousAnimator == null || !previousAnimator.isRunning())) {
                animator.setStartDelay(delay);
            }
            animator.addListener(getGlobalAnimationFinishedListener());
            startAnimator(animator);
            child.setTag(com.android.systemui.R.id.alpha_animator_tag, animator);
            child.setTag(com.android.systemui.R.id.alpha_animator_start_value_tag, Float.valueOf(child.getAlpha()));
            child.setTag(com.android.systemui.R.id.alpha_animator_end_value_tag, Float.valueOf(newEndValue));
        }
    }

    private void startZTranslationAnimation(final ExpandableView child, StackScrollState.ViewState viewState, long duration, long delay) {
        Float previousStartValue = (Float) getChildTag(child, com.android.systemui.R.id.translation_z_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(child, com.android.systemui.R.id.translation_z_animator_end_value_tag);
        float newEndValue = viewState.zTranslation;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(child, com.android.systemui.R.id.translation_z_animator_tag);
            if (!this.mAnimationFilter.animateZ) {
                if (previousAnimator != null) {
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    float relativeDiff = newEndValue - previousEndValue.floatValue();
                    float newStartValue = previousStartValue.floatValue() + relativeDiff;
                    values[0].setFloatValues(newStartValue, newEndValue);
                    child.setTag(com.android.systemui.R.id.translation_z_animator_start_value_tag, Float.valueOf(newStartValue));
                    child.setTag(com.android.systemui.R.id.translation_z_animator_end_value_tag, Float.valueOf(newEndValue));
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                    return;
                }
                child.setTranslationZ(newEndValue);
            }
            ObjectAnimator animator = ObjectAnimator.ofFloat(child, (Property<ExpandableView, Float>) View.TRANSLATION_Z, child.getTranslationZ(), newEndValue);
            animator.setInterpolator(this.mFastOutSlowInInterpolator);
            long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
            animator.setDuration(newDuration);
            if (delay > 0 && (previousAnimator == null || !previousAnimator.isRunning())) {
                animator.setStartDelay(delay);
            }
            animator.addListener(getGlobalAnimationFinishedListener());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    child.setTag(com.android.systemui.R.id.translation_z_animator_tag, null);
                    child.setTag(com.android.systemui.R.id.translation_z_animator_start_value_tag, null);
                    child.setTag(com.android.systemui.R.id.translation_z_animator_end_value_tag, null);
                }
            });
            startAnimator(animator);
            child.setTag(com.android.systemui.R.id.translation_z_animator_tag, animator);
            child.setTag(com.android.systemui.R.id.translation_z_animator_start_value_tag, Float.valueOf(child.getTranslationZ()));
            child.setTag(com.android.systemui.R.id.translation_z_animator_end_value_tag, Float.valueOf(newEndValue));
        }
    }

    private void startYTranslationAnimation(final ExpandableView child, StackScrollState.ViewState viewState, long duration, long delay) {
        Float previousStartValue = (Float) getChildTag(child, com.android.systemui.R.id.translation_y_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(child, com.android.systemui.R.id.translation_y_animator_end_value_tag);
        float newEndValue = viewState.yTranslation;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(child, com.android.systemui.R.id.translation_y_animator_tag);
            if (!this.mAnimationFilter.animateY) {
                if (previousAnimator != null) {
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    float relativeDiff = newEndValue - previousEndValue.floatValue();
                    float newStartValue = previousStartValue.floatValue() + relativeDiff;
                    values[0].setFloatValues(newStartValue, newEndValue);
                    child.setTag(com.android.systemui.R.id.translation_y_animator_start_value_tag, Float.valueOf(newStartValue));
                    child.setTag(com.android.systemui.R.id.translation_y_animator_end_value_tag, Float.valueOf(newEndValue));
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                    return;
                }
                child.setTranslationY(newEndValue);
                return;
            }
            ObjectAnimator animator = ObjectAnimator.ofFloat(child, (Property<ExpandableView, Float>) View.TRANSLATION_Y, child.getTranslationY(), newEndValue);
            animator.setInterpolator(this.mFastOutSlowInInterpolator);
            long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
            animator.setDuration(newDuration);
            if (delay > 0 && (previousAnimator == null || !previousAnimator.isRunning())) {
                animator.setStartDelay(delay);
            }
            animator.addListener(getGlobalAnimationFinishedListener());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    child.setTag(com.android.systemui.R.id.translation_y_animator_tag, null);
                    child.setTag(com.android.systemui.R.id.translation_y_animator_start_value_tag, null);
                    child.setTag(com.android.systemui.R.id.translation_y_animator_end_value_tag, null);
                }
            });
            startAnimator(animator);
            child.setTag(com.android.systemui.R.id.translation_y_animator_tag, animator);
            child.setTag(com.android.systemui.R.id.translation_y_animator_start_value_tag, Float.valueOf(child.getTranslationY()));
            child.setTag(com.android.systemui.R.id.translation_y_animator_end_value_tag, Float.valueOf(newEndValue));
        }
    }

    private void startScaleAnimation(final ExpandableView child, StackScrollState.ViewState viewState, long duration) {
        Float previousStartValue = (Float) getChildTag(child, com.android.systemui.R.id.scale_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(child, com.android.systemui.R.id.scale_animator_end_value_tag);
        float newEndValue = viewState.scale;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(child, com.android.systemui.R.id.scale_animator_tag);
            if (!this.mAnimationFilter.animateScale) {
                if (previousAnimator != null) {
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    float relativeDiff = newEndValue - previousEndValue.floatValue();
                    float newStartValue = previousStartValue.floatValue() + relativeDiff;
                    values[0].setFloatValues(newStartValue, newEndValue);
                    values[1].setFloatValues(newStartValue, newEndValue);
                    child.setTag(com.android.systemui.R.id.scale_animator_start_value_tag, Float.valueOf(newStartValue));
                    child.setTag(com.android.systemui.R.id.scale_animator_end_value_tag, Float.valueOf(newEndValue));
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                    return;
                }
                child.setScaleX(newEndValue);
                child.setScaleY(newEndValue);
            }
            PropertyValuesHolder holderX = PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, child.getScaleX(), newEndValue);
            PropertyValuesHolder holderY = PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, child.getScaleY(), newEndValue);
            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(child, holderX, holderY);
            animator.setInterpolator(this.mFastOutSlowInInterpolator);
            long newDuration = cancelAnimatorAndGetNewDuration(duration, previousAnimator);
            animator.setDuration(newDuration);
            animator.addListener(getGlobalAnimationFinishedListener());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    child.setTag(com.android.systemui.R.id.scale_animator_tag, null);
                    child.setTag(com.android.systemui.R.id.scale_animator_start_value_tag, null);
                    child.setTag(com.android.systemui.R.id.scale_animator_end_value_tag, null);
                }
            });
            startAnimator(animator);
            child.setTag(com.android.systemui.R.id.scale_animator_tag, animator);
            child.setTag(com.android.systemui.R.id.scale_animator_start_value_tag, Float.valueOf(child.getScaleX()));
            child.setTag(com.android.systemui.R.id.scale_animator_end_value_tag, Float.valueOf(newEndValue));
        }
    }

    private void startAnimator(ValueAnimator animator) {
        this.mAnimatorSet.add(animator);
        animator.start();
    }

    private AnimatorListenerAdapter getGlobalAnimationFinishedListener() {
        return !this.mAnimationListenerPool.empty() ? this.mAnimationListenerPool.pop() : new AnimatorListenerAdapter() {
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

    private static <T> T getChildTag(View view, int i) {
        return (T) view.getTag(i);
    }

    private long cancelAnimatorAndGetNewDuration(long duration, ValueAnimator previousAnimator) {
        if (previousAnimator != null) {
            long newDuration = Math.max(previousAnimator.getDuration() - previousAnimator.getCurrentPlayTime(), duration);
            previousAnimator.cancel();
            return newDuration;
        }
        return duration;
    }

    private void onAnimationFinished() {
        this.mHostLayout.onChildAnimationFinished();
    }

    private void processAnimationEvents(ArrayList<NotificationStackScrollLayout.AnimationEvent> animationEvents, StackScrollState finalState) {
        for (NotificationStackScrollLayout.AnimationEvent event : animationEvents) {
            final ExpandableView changingView = (ExpandableView) event.changingView;
            if (event.animationType == 0) {
                StackScrollState.ViewState viewState = finalState.getViewStateForView(changingView);
                if (viewState != null) {
                    if (changingView.getVisibility() == 8) {
                        finalState.removeViewStateForView(changingView);
                    } else {
                        changingView.setAlpha(viewState.alpha);
                        changingView.setTranslationY(viewState.yTranslation);
                        changingView.setTranslationZ(viewState.zTranslation);
                        changingView.setActualHeight(viewState.height, false);
                        this.mNewAddChildren.add(changingView);
                        this.mNewEvents.add(event);
                    }
                }
            } else {
                if (event.animationType == 1) {
                    if (changingView.getVisibility() == 8) {
                        this.mHostLayout.getOverlay().remove(changingView);
                    } else {
                        StackScrollState.ViewState viewState2 = finalState.getViewStateForView(event.viewAfterChangingView);
                        int actualHeight = changingView.getActualHeight();
                        float translationDirection = -1.0f;
                        if (viewState2 != null) {
                            float translationDirection2 = ((viewState2.yTranslation - (changingView.getTranslationY() + (actualHeight / 2.0f))) * 2.0f) / actualHeight;
                            translationDirection = Math.max(Math.min(translationDirection2, 1.0f), -1.0f);
                        }
                        changingView.performRemoveAnimation(464L, translationDirection, new Runnable() {
                            @Override
                            public void run() {
                                StackStateAnimator.this.mHostLayout.getOverlay().remove(changingView);
                            }
                        });
                    }
                } else if (event.animationType == 2) {
                    this.mHostLayout.getOverlay().remove(changingView);
                }
                this.mNewEvents.add(event);
            }
        }
    }

    public void animateOverScrollToAmount(float targetAmount, final boolean onTop, final boolean isRubberbanded) {
        float startOverScrollAmount = this.mHostLayout.getCurrentOverScrollAmount(onTop);
        if (targetAmount != startOverScrollAmount) {
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
            overScrollAnimator.setInterpolator(this.mFastOutSlowInInterpolator);
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
    }

    public void cancelOverScrollAnimators(boolean onTop) {
        ValueAnimator currentAnimator = onTop ? this.mTopOverScrollAnimator : this.mBottomOverScrollAnimator;
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
    }

    public static int getFinalActualHeight(ExpandableView view) {
        if (view == null) {
            return 0;
        }
        ValueAnimator heightAnimator = (ValueAnimator) getChildTag(view, com.android.systemui.R.id.height_animator_tag);
        if (heightAnimator == null) {
            return view.getActualHeight();
        }
        return ((Integer) getChildTag(view, com.android.systemui.R.id.height_animator_end_value_tag)).intValue();
    }
}
