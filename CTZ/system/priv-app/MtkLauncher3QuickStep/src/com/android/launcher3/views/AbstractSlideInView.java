package com.android.launcher3.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.touch.SwipeDetector;

/* loaded from: classes.dex */
public abstract class AbstractSlideInView extends AbstractFloatingView implements SwipeDetector.Listener {
    protected static Property<AbstractSlideInView, Float> TRANSLATION_SHIFT = new Property<AbstractSlideInView, Float>(Float.class, "translationShift") { // from class: com.android.launcher3.views.AbstractSlideInView.1
        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(AbstractSlideInView abstractSlideInView) {
            return Float.valueOf(abstractSlideInView.mTranslationShift);
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(AbstractSlideInView abstractSlideInView, Float f) {
            abstractSlideInView.setTranslationShift(f.floatValue());
        }
    };
    protected static final float TRANSLATION_SHIFT_CLOSED = 1.0f;
    protected static final float TRANSLATION_SHIFT_OPENED = 0.0f;
    protected View mContent;
    protected final Launcher mLauncher;
    protected boolean mNoIntercept;
    protected final ObjectAnimator mOpenCloseAnimator;
    protected Interpolator mScrollInterpolator;
    protected final SwipeDetector mSwipeDetector;
    protected float mTranslationShift;

    public AbstractSlideInView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mTranslationShift = 1.0f;
        this.mLauncher = Launcher.getLauncher(context);
        this.mScrollInterpolator = Interpolators.SCROLL_CUBIC;
        this.mSwipeDetector = new SwipeDetector(context, this, SwipeDetector.VERTICAL);
        this.mOpenCloseAnimator = LauncherAnimUtils.ofPropertyValuesHolder(this, new PropertyValuesHolder[0]);
        this.mOpenCloseAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.views.AbstractSlideInView.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                AbstractSlideInView.this.mSwipeDetector.finishedScrolling();
                AbstractSlideInView.this.announceAccessibilityChanges();
            }
        });
    }

    protected void setTranslationShift(float f) {
        this.mTranslationShift = f;
        this.mContent.setTranslationY(this.mTranslationShift * this.mContent.getHeight());
    }

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        int i;
        if (this.mNoIntercept) {
            return false;
        }
        if (this.mSwipeDetector.isIdleState()) {
            i = 2;
        } else {
            i = 0;
        }
        this.mSwipeDetector.setDetectableScrollConditions(i, false);
        this.mSwipeDetector.onTouchEvent(motionEvent);
        return this.mSwipeDetector.isDraggingOrSettling() || !this.mLauncher.getDragLayer().isEventOverView(this.mContent, motionEvent);
    }

    @Override // com.android.launcher3.AbstractFloatingView, com.android.launcher3.util.TouchController
    public boolean onControllerTouchEvent(MotionEvent motionEvent) {
        this.mSwipeDetector.onTouchEvent(motionEvent);
        if (motionEvent.getAction() == 1 && this.mSwipeDetector.isIdleState() && !this.mLauncher.getDragLayer().isEventOverView(this.mContent, motionEvent)) {
            close(true);
        }
        return true;
    }

    @Override // com.android.launcher3.touch.SwipeDetector.Listener
    public void onDragStart(boolean z) {
    }

    @Override // com.android.launcher3.touch.SwipeDetector.Listener
    public boolean onDrag(float f, float f2) {
        float height = this.mContent.getHeight();
        setTranslationShift(Utilities.boundToRange(f, 0.0f, height) / height);
        return true;
    }

    @Override // com.android.launcher3.touch.SwipeDetector.Listener
    public void onDragEnd(float f, boolean z) {
        if ((z && f > 0.0f) || this.mTranslationShift > 0.5f) {
            this.mScrollInterpolator = Interpolators.scrollInterpolatorForVelocity(f);
            this.mOpenCloseAnimator.setDuration(SwipeDetector.calculateDuration(f, 1.0f - this.mTranslationShift));
            close(true);
        } else {
            this.mOpenCloseAnimator.setValues(PropertyValuesHolder.ofFloat(TRANSLATION_SHIFT, 0.0f));
            this.mOpenCloseAnimator.setDuration(SwipeDetector.calculateDuration(f, this.mTranslationShift)).setInterpolator(Interpolators.DEACCEL);
            this.mOpenCloseAnimator.start();
        }
    }

    protected void handleClose(boolean z, long j) {
        if (this.mIsOpen && !z) {
            this.mOpenCloseAnimator.cancel();
            setTranslationShift(1.0f);
            onCloseComplete();
        } else {
            if (!this.mIsOpen || this.mOpenCloseAnimator.isRunning()) {
                return;
            }
            this.mOpenCloseAnimator.setValues(PropertyValuesHolder.ofFloat(TRANSLATION_SHIFT, 1.0f));
            this.mOpenCloseAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.views.AbstractSlideInView.3
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    AbstractSlideInView.this.onCloseComplete();
                }
            });
            if (this.mSwipeDetector.isIdleState()) {
                this.mOpenCloseAnimator.setDuration(j).setInterpolator(Interpolators.ACCEL);
            } else {
                this.mOpenCloseAnimator.setInterpolator(this.mScrollInterpolator);
            }
            this.mOpenCloseAnimator.start();
        }
    }

    protected void onCloseComplete() {
        this.mIsOpen = false;
        this.mLauncher.getDragLayer().removeView(this);
    }
}
