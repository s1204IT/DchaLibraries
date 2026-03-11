package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import com.android.launcher3.DragController;

public class SearchDropTargetBar extends FrameLayout implements DragController.DragListener {
    boolean mAccessibilityEnabled;
    private AnimatorSet mCurrentAnimation;
    private boolean mDeferOnDragEnd;
    private ButtonDropTarget mDeleteDropTarget;
    View mDropTargetBar;
    private ButtonDropTarget mInfoDropTarget;
    View mQSB;
    private State mState;
    private ButtonDropTarget mUninstallDropTarget;
    private static final TimeInterpolator MOVE_DOWN_INTERPOLATOR = new DecelerateInterpolator(0.6f);
    private static final TimeInterpolator MOVE_UP_INTERPOLATOR = new DecelerateInterpolator(1.5f);
    private static final TimeInterpolator DEFAULT_INTERPOLATOR = new AccelerateInterpolator();
    private static int DEFAULT_DRAG_FADE_DURATION = 175;

    public enum State {
        INVISIBLE(0.0f, 0.0f, 0.0f),
        INVISIBLE_TRANSLATED(0.0f, 0.0f, -1.0f),
        SEARCH_BAR(1.0f, 0.0f, 0.0f),
        DROP_TARGET(0.0f, 1.0f, 0.0f);

        private final float mDropTargetBarAlpha;
        private final float mSearchBarAlpha;
        private final float mTranslation;

        public static State[] valuesCustom() {
            return values();
        }

        State(float sbAlpha, float dtbAlpha, float translation) {
            this.mSearchBarAlpha = sbAlpha;
            this.mDropTargetBarAlpha = dtbAlpha;
            this.mTranslation = translation;
        }
    }

    public SearchDropTargetBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchDropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mState = State.SEARCH_BAR;
        this.mDeferOnDragEnd = false;
        this.mAccessibilityEnabled = false;
    }

    public void setup(Launcher launcher, DragController dragController) {
        dragController.addDragListener(this);
        dragController.setFlingToDeleteDropTarget(this.mDeleteDropTarget);
        dragController.addDragListener(this.mInfoDropTarget);
        dragController.addDragListener(this.mDeleteDropTarget);
        dragController.addDragListener(this.mUninstallDropTarget);
        dragController.addDropTarget(this.mInfoDropTarget);
        dragController.addDropTarget(this.mDeleteDropTarget);
        dragController.addDropTarget(this.mUninstallDropTarget);
        this.mInfoDropTarget.setLauncher(launcher);
        this.mDeleteDropTarget.setLauncher(launcher);
        this.mUninstallDropTarget.setLauncher(launcher);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDropTargetBar = findViewById(R.id.drag_target_bar);
        this.mInfoDropTarget = (ButtonDropTarget) this.mDropTargetBar.findViewById(R.id.info_target_text);
        this.mDeleteDropTarget = (ButtonDropTarget) this.mDropTargetBar.findViewById(R.id.delete_target_text);
        this.mUninstallDropTarget = (ButtonDropTarget) this.mDropTargetBar.findViewById(R.id.uninstall_target_text);
        this.mInfoDropTarget.setSearchDropTargetBar(this);
        this.mDeleteDropTarget.setSearchDropTargetBar(this);
        this.mUninstallDropTarget.setSearchDropTargetBar(this);
        this.mDropTargetBar.setAlpha(0.0f);
        AlphaUpdateListener.updateVisibility(this.mDropTargetBar, this.mAccessibilityEnabled);
    }

    public void setQsbSearchBar(View qsb) {
        this.mQSB = qsb;
    }

    public void animateToState(State newState, int duration) {
        animateToState(newState, duration, null);
    }

    public void animateToState(State newState, int duration, AnimatorSet animation) {
        TimeInterpolator timeInterpolator;
        if (this.mState == newState) {
            return;
        }
        this.mState = newState;
        AccessibilityManager am = (AccessibilityManager) getContext().getSystemService("accessibility");
        this.mAccessibilityEnabled = am.isEnabled();
        if (this.mCurrentAnimation != null) {
            this.mCurrentAnimation.cancel();
            this.mCurrentAnimation = null;
        }
        this.mCurrentAnimation = null;
        if (duration > 0) {
            this.mCurrentAnimation = new AnimatorSet();
            this.mCurrentAnimation.setDuration(duration);
            animateAlpha(this.mDropTargetBar, this.mState.mDropTargetBarAlpha, DEFAULT_INTERPOLATOR);
        } else {
            this.mDropTargetBar.setAlpha(this.mState.mDropTargetBarAlpha);
            AlphaUpdateListener.updateVisibility(this.mDropTargetBar, this.mAccessibilityEnabled);
        }
        if (this.mQSB != null) {
            boolean isVertical = ((Launcher) getContext()).getDeviceProfile().isVerticalBarLayout();
            float translation = isVertical ? 0.0f : this.mState.mTranslation * getMeasuredHeight();
            if (duration > 0) {
                int translationChange = Float.compare(this.mQSB.getTranslationY(), translation);
                View view = this.mQSB;
                float f = this.mState.mSearchBarAlpha;
                if (translationChange == 0) {
                    timeInterpolator = DEFAULT_INTERPOLATOR;
                } else {
                    timeInterpolator = translationChange < 0 ? MOVE_DOWN_INTERPOLATOR : MOVE_UP_INTERPOLATOR;
                }
                animateAlpha(view, f, timeInterpolator);
                if (translationChange != 0) {
                    this.mCurrentAnimation.play(ObjectAnimator.ofFloat(this.mQSB, (Property<View, Float>) View.TRANSLATION_Y, translation));
                }
            } else {
                this.mQSB.setTranslationY(translation);
                this.mQSB.setAlpha(this.mState.mSearchBarAlpha);
                AlphaUpdateListener.updateVisibility(this.mQSB, this.mAccessibilityEnabled);
            }
        }
        if (duration <= 0) {
            return;
        }
        if (animation != null) {
            animation.play(this.mCurrentAnimation);
        } else {
            this.mCurrentAnimation.start();
        }
    }

    private void animateAlpha(View v, float alpha, TimeInterpolator interpolator) {
        if (Float.compare(v.getAlpha(), alpha) == 0) {
            return;
        }
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, (Property<View, Float>) View.ALPHA, alpha);
        anim.setInterpolator(interpolator);
        anim.addListener(new ViewVisiblilyUpdateHandler(v));
        this.mCurrentAnimation.play(anim);
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        animateToState(State.DROP_TARGET, DEFAULT_DRAG_FADE_DURATION);
    }

    public void deferOnDragEnd() {
        this.mDeferOnDragEnd = true;
    }

    @Override
    public void onDragEnd() {
        if (!this.mDeferOnDragEnd) {
            animateToState(State.SEARCH_BAR, DEFAULT_DRAG_FADE_DURATION);
        } else {
            this.mDeferOnDragEnd = false;
        }
    }

    public Rect getSearchBarBounds() {
        if (this.mQSB == null) {
            return null;
        }
        int[] pos = new int[2];
        this.mQSB.getLocationOnScreen(pos);
        Rect rect = new Rect();
        rect.left = pos[0];
        rect.top = pos[1];
        rect.right = pos[0] + this.mQSB.getWidth();
        rect.bottom = pos[1] + this.mQSB.getHeight();
        return rect;
    }

    public void enableAccessibleDrag(boolean enable) {
        if (this.mQSB != null) {
            this.mQSB.setVisibility(enable ? 8 : 0);
        }
        this.mInfoDropTarget.enableAccessibleDrag(enable);
        this.mDeleteDropTarget.enableAccessibleDrag(enable);
        this.mUninstallDropTarget.enableAccessibleDrag(enable);
    }

    private class ViewVisiblilyUpdateHandler extends AnimatorListenerAdapter {
        private final View mView;

        ViewVisiblilyUpdateHandler(View v) {
            this.mView = v;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            this.mView.setVisibility(0);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            AlphaUpdateListener.updateVisibility(this.mView, SearchDropTargetBar.this.mAccessibilityEnabled);
        }
    }
}
