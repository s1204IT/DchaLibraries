package com.android.systemui.statusbar.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

public class StackStateAnimator {
    public static final int ANIMATION_DURATION_HEADS_UP_APPEAR_CLOSED = (int) (550.0f * HeadsUpAppearInterpolator.getFractionUntilOvershoot());
    private ValueAnimator mBottomOverScrollAnimator;
    private long mCurrentAdditionalDelay;
    private int mCurrentLastNotAddedIndex;
    private long mCurrentLength;
    private final int mGoToFullShadeAppearingTranslation;
    private int mHeadsUpAppearHeightBottom;
    public NotificationStackScrollLayout mHostLayout;
    private final int mPulsingAppearingTranslation;
    private boolean mShadeExpanded;
    private NotificationShelf mShelf;
    private ValueAnimator mTopOverScrollAnimator;
    private final ExpandableViewState mTmpState = new ExpandableViewState();
    private ArrayList<NotificationStackScrollLayout.AnimationEvent> mNewEvents = new ArrayList<>();
    private ArrayList<View> mNewAddChildren = new ArrayList<>();
    private HashSet<View> mHeadsUpAppearChildren = new HashSet<>();
    private HashSet<View> mHeadsUpDisappearChildren = new HashSet<>();
    private HashSet<Animator> mAnimatorSet = new HashSet<>();
    private Stack<AnimatorListenerAdapter> mAnimationListenerPool = new Stack<>();
    private AnimationFilter mAnimationFilter = new AnimationFilter();
    private ArrayList<ExpandableView> mTransientViewsToRemove = new ArrayList<>();
    private int[] mTmpLocation = new int[2];
    private final AnimationProperties mAnimationProperties = new AnimationProperties() {
        @Override
        public AnimationFilter getAnimationFilter() {
            return StackStateAnimator.this.mAnimationFilter;
        }

        @Override
        public AnimatorListenerAdapter getAnimationFinishListener() {
            return StackStateAnimator.this.getGlobalAnimationFinishedListener();
        }

        @Override
        public boolean wasAdded(View view) {
            return StackStateAnimator.this.mNewAddChildren.contains(view);
        }

        @Override
        public Interpolator getCustomInterpolator(View view, Property property) {
            if (StackStateAnimator.this.mHeadsUpAppearChildren.contains(view) && View.TRANSLATION_Y.equals(property)) {
                return Interpolators.HEADS_UP_APPEAR;
            }
            return null;
        }
    };

    public StackStateAnimator(NotificationStackScrollLayout notificationStackScrollLayout) {
        this.mHostLayout = notificationStackScrollLayout;
        this.mGoToFullShadeAppearingTranslation = notificationStackScrollLayout.getContext().getResources().getDimensionPixelSize(R.dimen.go_to_full_shade_appearing_translation);
        this.mPulsingAppearingTranslation = notificationStackScrollLayout.getContext().getResources().getDimensionPixelSize(R.dimen.pulsing_notification_appear_translation);
    }

    public boolean isRunning() {
        return !this.mAnimatorSet.isEmpty();
    }

    public void startAnimationForEvents(ArrayList<NotificationStackScrollLayout.AnimationEvent> arrayList, StackScrollState stackScrollState, long j) {
        processAnimationEvents(arrayList, stackScrollState);
        int childCount = this.mHostLayout.getChildCount();
        this.mAnimationFilter.applyCombination(this.mNewEvents);
        this.mCurrentAdditionalDelay = j;
        this.mCurrentLength = NotificationStackScrollLayout.AnimationEvent.combineLength(this.mNewEvents);
        this.mCurrentLastNotAddedIndex = findLastNotAddedIndex(stackScrollState);
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) this.mHostLayout.getChildAt(i);
            ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableView);
            if (viewStateForView != null && expandableView.getVisibility() != 8 && !applyWithoutAnimation(expandableView, viewStateForView, stackScrollState)) {
                initAnimationProperties(stackScrollState, expandableView, viewStateForView);
                viewStateForView.animateTo(expandableView, this.mAnimationProperties);
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

    private void initAnimationProperties(StackScrollState stackScrollState, ExpandableView expandableView, ExpandableViewState expandableViewState) {
        boolean zWasAdded = this.mAnimationProperties.wasAdded(expandableView);
        this.mAnimationProperties.duration = this.mCurrentLength;
        adaptDurationWhenGoingToFullShade(expandableView, expandableViewState, zWasAdded);
        this.mAnimationProperties.delay = 0L;
        if (!zWasAdded) {
            if (this.mAnimationFilter.hasDelays) {
                if (expandableViewState.yTranslation == expandableView.getTranslationY() && expandableViewState.zTranslation == expandableView.getTranslationZ() && expandableViewState.alpha == expandableView.getAlpha() && expandableViewState.height == expandableView.getActualHeight() && expandableViewState.clipTopAmount == expandableView.getClipTopAmount() && expandableViewState.dark == expandableView.isDark() && expandableViewState.shadowAlpha == expandableView.getShadowAlpha()) {
                    return;
                }
            } else {
                return;
            }
        }
        this.mAnimationProperties.delay = this.mCurrentAdditionalDelay + calculateChildAnimationDelay(expandableViewState, stackScrollState);
    }

    private void adaptDurationWhenGoingToFullShade(ExpandableView expandableView, ExpandableViewState expandableViewState, boolean z) {
        if (z && this.mAnimationFilter.hasGoToFullShadeEvent) {
            expandableView.setTranslationY(expandableView.getTranslationY() + this.mGoToFullShadeAppearingTranslation);
            this.mAnimationProperties.duration = 514 + ((long) (100.0f * ((float) Math.pow(expandableViewState.notGoneIndex - this.mCurrentLastNotAddedIndex, 0.699999988079071d))));
        }
    }

    private boolean applyWithoutAnimation(ExpandableView expandableView, ExpandableViewState expandableViewState, StackScrollState stackScrollState) {
        if (this.mShadeExpanded || ViewState.isAnimatingY(expandableView) || this.mHeadsUpDisappearChildren.contains(expandableView) || this.mHeadsUpAppearChildren.contains(expandableView) || NotificationStackScrollLayout.isPinnedHeadsUp(expandableView)) {
            return false;
        }
        expandableViewState.applyToView(expandableView);
        return true;
    }

    private int findLastNotAddedIndex(StackScrollState stackScrollState) {
        for (int childCount = this.mHostLayout.getChildCount() - 1; childCount >= 0; childCount--) {
            ExpandableView expandableView = (ExpandableView) this.mHostLayout.getChildAt(childCount);
            ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableView);
            if (viewStateForView != null && expandableView.getVisibility() != 8 && !this.mNewAddChildren.contains(expandableView)) {
                return viewStateForView.notGoneIndex;
            }
        }
        return -1;
    }

    private long calculateChildAnimationDelay(ExpandableViewState expandableViewState, StackScrollState stackScrollState) {
        View lastChildNotGone;
        if (this.mAnimationFilter.hasGoToFullShadeEvent) {
            return calculateDelayGoToFullShade(expandableViewState);
        }
        if (this.mAnimationFilter.customDelay != -1) {
            return this.mAnimationFilter.customDelay;
        }
        long jMax = 0;
        for (NotificationStackScrollLayout.AnimationEvent animationEvent : this.mNewEvents) {
            long j = 80;
            switch (animationEvent.animationType) {
                case 0:
                    jMax = Math.max(((long) (2 - Math.max(0, Math.min(2, Math.abs(expandableViewState.notGoneIndex - stackScrollState.getViewStateForView(animationEvent.changingView).notGoneIndex) - 1)))) * 80, jMax);
                    continue;
                case 2:
                    j = 32;
                    break;
            }
            int i = expandableViewState.notGoneIndex;
            if (animationEvent.viewAfterChangingView == null) {
                lastChildNotGone = this.mHostLayout.getLastChildNotGone();
            } else {
                lastChildNotGone = animationEvent.viewAfterChangingView;
            }
            if (lastChildNotGone != null) {
                int i2 = stackScrollState.getViewStateForView(lastChildNotGone).notGoneIndex;
                if (i >= i2) {
                    i++;
                }
                jMax = Math.max(((long) Math.max(0, Math.min(2, Math.abs(i - i2) - 1))) * j, jMax);
            }
        }
        return jMax;
    }

    private long calculateDelayGoToFullShade(ExpandableViewState expandableViewState) {
        int notGoneIndex = this.mShelf.getNotGoneIndex();
        float f = expandableViewState.notGoneIndex;
        float f2 = notGoneIndex;
        long jPow = 0;
        if (f > f2) {
            jPow = 0 + ((long) (((double) (((float) Math.pow(f - f2, 0.699999988079071d)) * 48.0f)) * 0.25d));
            f = f2;
        }
        return jPow + ((long) (((float) Math.pow(f, 0.699999988079071d)) * 48.0f));
    }

    public AnimatorListenerAdapter getGlobalAnimationFinishedListener() {
        if (!this.mAnimationListenerPool.empty()) {
            return this.mAnimationListenerPool.pop();
        }
        return new AnimatorListenerAdapter() {
            private boolean mWasCancelled;

            @Override
            public void onAnimationEnd(Animator animator) {
                StackStateAnimator.this.mAnimatorSet.remove(animator);
                if (StackStateAnimator.this.mAnimatorSet.isEmpty() && !this.mWasCancelled) {
                    StackStateAnimator.this.onAnimationFinished();
                }
                StackStateAnimator.this.mAnimationListenerPool.push(this);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                this.mWasCancelled = true;
            }

            @Override
            public void onAnimationStart(Animator animator) {
                this.mWasCancelled = false;
                StackStateAnimator.this.mAnimatorSet.add(animator);
            }
        };
    }

    public void onAnimationFinished() {
        this.mHostLayout.onChildAnimationFinished();
        for (ExpandableView expandableView : this.mTransientViewsToRemove) {
            expandableView.getTransientContainer().removeTransientView(expandableView);
        }
        this.mTransientViewsToRemove.clear();
    }

    private void processAnimationEvents(ArrayList<NotificationStackScrollLayout.AnimationEvent> arrayList, StackScrollState stackScrollState) {
        float fMax;
        for (NotificationStackScrollLayout.AnimationEvent animationEvent : arrayList) {
            final ExpandableView expandableView = (ExpandableView) animationEvent.changingView;
            if (animationEvent.animationType == 0) {
                ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableView);
                if (viewStateForView != null && !viewStateForView.gone) {
                    viewStateForView.applyToView(expandableView);
                    this.mNewAddChildren.add(expandableView);
                    this.mNewEvents.add(animationEvent);
                }
            } else {
                if (animationEvent.animationType == 1) {
                    if (expandableView.getVisibility() != 0) {
                        removeTransientView(expandableView);
                    } else {
                        ExpandableViewState viewStateForView2 = stackScrollState.getViewStateForView(animationEvent.viewAfterChangingView);
                        int actualHeight = expandableView.getActualHeight();
                        if (viewStateForView2 == null) {
                            fMax = -1.0f;
                        } else {
                            float translationY = expandableView.getTranslationY();
                            if ((expandableView instanceof ExpandableNotificationRow) && (animationEvent.viewAfterChangingView instanceof ExpandableNotificationRow)) {
                                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                                ExpandableNotificationRow expandableNotificationRow2 = (ExpandableNotificationRow) animationEvent.viewAfterChangingView;
                                if (expandableNotificationRow.isRemoved() && expandableNotificationRow.wasChildInGroupWhenRemoved() && !expandableNotificationRow2.isChildInGroup()) {
                                    translationY = expandableNotificationRow.getTranslationWhenRemoved();
                                }
                            }
                            float f = actualHeight;
                            fMax = Math.max(Math.min(((viewStateForView2.yTranslation - (translationY + (f / 2.0f))) * 2.0f) / f, 1.0f), -1.0f);
                        }
                        expandableView.performRemoveAnimation(464L, 0L, fMax, false, 0.0f, new Runnable() {
                            @Override
                            public void run() {
                                StackStateAnimator.removeTransientView(expandableView);
                            }
                        }, null);
                    }
                } else if (animationEvent.animationType == 2) {
                    if (Math.abs(expandableView.getTranslation()) == expandableView.getWidth() && expandableView.getTransientContainer() != null) {
                        expandableView.getTransientContainer().removeTransientView(expandableView);
                    }
                } else if (animationEvent.animationType == 13) {
                    ((ExpandableNotificationRow) animationEvent.changingView).prepareExpansionChanged(stackScrollState);
                } else {
                    float f2 = 0.0f;
                    if (animationEvent.animationType == 19) {
                        this.mTmpState.copyFrom(stackScrollState.getViewStateForView(expandableView));
                        this.mTmpState.yTranslation += this.mPulsingAppearingTranslation;
                        this.mTmpState.alpha = 0.0f;
                        this.mTmpState.applyToView(expandableView);
                    } else if (animationEvent.animationType == 20) {
                        ExpandableViewState viewStateForView3 = stackScrollState.getViewStateForView(expandableView);
                        viewStateForView3.yTranslation += this.mPulsingAppearingTranslation;
                        viewStateForView3.alpha = 0.0f;
                    } else if (animationEvent.animationType == 14) {
                        this.mTmpState.copyFrom(stackScrollState.getViewStateForView(expandableView));
                        if (animationEvent.headsUpFromBottom) {
                            this.mTmpState.yTranslation = this.mHeadsUpAppearHeightBottom;
                        } else {
                            this.mTmpState.yTranslation = 0.0f;
                            expandableView.performAddAnimation(0L, ANIMATION_DURATION_HEADS_UP_APPEAR_CLOSED, true);
                        }
                        this.mHeadsUpAppearChildren.add(expandableView);
                        this.mTmpState.applyToView(expandableView);
                    } else if (animationEvent.animationType == 15 || animationEvent.animationType == 16) {
                        this.mHeadsUpDisappearChildren.add(expandableView);
                        Runnable runnable = null;
                        int i = animationEvent.animationType == 16 ? com.android.systemui.plugins.R.styleable.AppCompatTheme_windowNoTitle : 0;
                        if (expandableView.getParent() == null) {
                            this.mHostLayout.addTransientView(expandableView, 0);
                            expandableView.setTransientContainer(this.mHostLayout);
                            this.mTmpState.initFrom(expandableView);
                            this.mTmpState.yTranslation = 0.0f;
                            this.mAnimationFilter.animateY = true;
                            this.mAnimationProperties.delay = i + com.android.systemui.plugins.R.styleable.AppCompatTheme_windowNoTitle;
                            this.mAnimationProperties.duration = 300L;
                            this.mTmpState.animateTo(expandableView, this.mAnimationProperties);
                            runnable = new Runnable() {
                                @Override
                                public final void run() {
                                    StackStateAnimator.removeTransientView(expandableView);
                                }
                            };
                        }
                        Runnable runnable2 = runnable;
                        if (expandableView instanceof ExpandableNotificationRow) {
                            ExpandableNotificationRow expandableNotificationRow3 = (ExpandableNotificationRow) expandableView;
                            z = expandableNotificationRow3.isDismissed() ? false : true;
                            StatusBarIconView statusBarIconView = expandableNotificationRow3.getEntry().icon;
                            if (statusBarIconView.getParent() != null) {
                                statusBarIconView.getLocationOnScreen(this.mTmpLocation);
                                float translationX = (this.mTmpLocation[0] - statusBarIconView.getTranslationX()) + ViewState.getFinalTranslationX(statusBarIconView) + (statusBarIconView.getWidth() * 0.25f);
                                this.mHostLayout.getLocationOnScreen(this.mTmpLocation);
                                f2 = translationX - this.mTmpLocation[0];
                            }
                        }
                        float f3 = f2;
                        if (z) {
                            expandableView.performRemoveAnimation(420L, i, 0.0f, true, f3, runnable2, getGlobalAnimationFinishedListener());
                        } else if (runnable2 != null) {
                            runnable2.run();
                        }
                    }
                }
                this.mNewEvents.add(animationEvent);
            }
        }
    }

    public static void removeTransientView(ExpandableView expandableView) {
        if (expandableView.getTransientContainer() != null) {
            expandableView.getTransientContainer().removeTransientView(expandableView);
        }
    }

    public void animateOverScrollToAmount(float f, final boolean z, final boolean z2) {
        float currentOverScrollAmount = this.mHostLayout.getCurrentOverScrollAmount(z);
        if (f == currentOverScrollAmount) {
            return;
        }
        cancelOverScrollAnimators(z);
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(currentOverScrollAmount, f);
        valueAnimatorOfFloat.setDuration(360L);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                StackStateAnimator.this.mHostLayout.setOverScrollAmount(((Float) valueAnimator.getAnimatedValue()).floatValue(), z, false, false, z2);
            }
        });
        valueAnimatorOfFloat.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (z) {
                    StackStateAnimator.this.mTopOverScrollAnimator = null;
                } else {
                    StackStateAnimator.this.mBottomOverScrollAnimator = null;
                }
            }
        });
        valueAnimatorOfFloat.start();
        if (z) {
            this.mTopOverScrollAnimator = valueAnimatorOfFloat;
        } else {
            this.mBottomOverScrollAnimator = valueAnimatorOfFloat;
        }
    }

    public void cancelOverScrollAnimators(boolean z) {
        ValueAnimator valueAnimator = z ? this.mTopOverScrollAnimator : this.mBottomOverScrollAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
    }

    public void setHeadsUpAppearHeightBottom(int i) {
        this.mHeadsUpAppearHeightBottom = i;
    }

    public void setShadeExpanded(boolean z) {
        this.mShadeExpanded = z;
    }

    public void setShelf(NotificationShelf notificationShelf) {
        this.mShelf = notificationShelf;
    }
}
