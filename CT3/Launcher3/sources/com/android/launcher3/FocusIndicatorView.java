package com.android.launcher3;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.Property;
import android.view.View;

public class FocusIndicatorView extends View implements View.OnFocusChangeListener {
    private ObjectAnimator mCurrentAnimation;
    private final View.OnFocusChangeListener mHideIndicatorOnFocusListener;
    private final int[] mIndicatorPos;
    private boolean mInitiated;
    private View mLastFocusedView;
    private Pair<View, Boolean> mPendingCall;
    private ViewAnimState mTargetState;
    private final int[] mTargetViewPos;

    public FocusIndicatorView(Context context) {
        this(context, null);
    }

    public FocusIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIndicatorPos = new int[2];
        this.mTargetViewPos = new int[2];
        setAlpha(0.0f);
        setBackgroundColor(getResources().getColor(R.color.focused_background));
        this.mHideIndicatorOnFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    return;
                }
                FocusIndicatorView.this.endCurrentAnimation();
                FocusIndicatorView.this.setAlpha(0.0f);
            }
        };
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mLastFocusedView == null) {
            return;
        }
        this.mPendingCall = Pair.create(this.mLastFocusedView, Boolean.TRUE);
        invalidate();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        this.mPendingCall = null;
        if (!this.mInitiated && getWidth() == 0) {
            this.mPendingCall = Pair.create(v, Boolean.valueOf(hasFocus));
            invalidate();
            return;
        }
        if (!this.mInitiated) {
            computeLocationRelativeToParent(this, (View) getParent(), this.mIndicatorPos);
            this.mInitiated = true;
        }
        if (hasFocus) {
            int indicatorWidth = getWidth();
            int indicatorHeight = getHeight();
            endCurrentAnimation();
            ViewAnimState nextState = new ViewAnimState();
            nextState.scaleX = (v.getScaleX() * v.getWidth()) / indicatorWidth;
            nextState.scaleY = (v.getScaleY() * v.getHeight()) / indicatorHeight;
            computeLocationRelativeToParent(v, (View) getParent(), this.mTargetViewPos);
            nextState.x = (this.mTargetViewPos[0] - this.mIndicatorPos[0]) - (((1.0f - nextState.scaleX) * indicatorWidth) / 2.0f);
            nextState.y = (this.mTargetViewPos[1] - this.mIndicatorPos[1]) - (((1.0f - nextState.scaleY) * indicatorHeight) / 2.0f);
            if (getAlpha() > 0.2f) {
                this.mTargetState = nextState;
                this.mCurrentAnimation = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat((Property<?, Float>) View.ALPHA, 1.0f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_X, this.mTargetState.x), PropertyValuesHolder.ofFloat((Property<?, Float>) View.TRANSLATION_Y, this.mTargetState.y), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, this.mTargetState.scaleX), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, this.mTargetState.scaleY));
            } else {
                applyState(nextState);
                this.mCurrentAnimation = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat((Property<?, Float>) View.ALPHA, 1.0f));
            }
            this.mLastFocusedView = v;
        } else if (this.mLastFocusedView == v) {
            this.mLastFocusedView = null;
            endCurrentAnimation();
            this.mCurrentAnimation = LauncherAnimUtils.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat((Property<?, Float>) View.ALPHA, 0.0f));
        }
        if (this.mCurrentAnimation == null) {
            return;
        }
        this.mCurrentAnimation.setDuration(150L).start();
    }

    public void endCurrentAnimation() {
        if (this.mCurrentAnimation != null) {
            this.mCurrentAnimation.cancel();
            this.mCurrentAnimation = null;
        }
        if (this.mTargetState == null) {
            return;
        }
        applyState(this.mTargetState);
        this.mTargetState = null;
    }

    private void applyState(ViewAnimState state) {
        setTranslationX(state.x);
        setTranslationY(state.y);
        setScaleX(state.scaleX);
        setScaleY(state.scaleY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mPendingCall == null) {
            return;
        }
        onFocusChange((View) this.mPendingCall.first, ((Boolean) this.mPendingCall.second).booleanValue());
    }

    private static void computeLocationRelativeToParent(View v, View parent, int[] pos) {
        pos[1] = 0;
        pos[0] = 0;
        computeLocationRelativeToParentHelper(v, parent, pos);
        pos[0] = (int) (pos[0] + (((1.0f - v.getScaleX()) * v.getWidth()) / 2.0f));
        pos[1] = (int) (pos[1] + (((1.0f - v.getScaleY()) * v.getHeight()) / 2.0f));
    }

    private static void computeLocationRelativeToParentHelper(View child, View commonParent, int[] shift) {
        View parent = (View) child.getParent();
        shift[0] = shift[0] + child.getLeft();
        shift[1] = shift[1] + child.getTop();
        if (parent instanceof PagedView) {
            PagedView page = (PagedView) parent;
            shift[0] = shift[0] - page.getScrollForPage(page.indexOfChild(child));
        }
        if (parent == commonParent) {
            return;
        }
        computeLocationRelativeToParentHelper(parent, commonParent, shift);
    }

    static final class ViewAnimState {
        float scaleX;
        float scaleY;
        float x;
        float y;

        ViewAnimState() {
        }
    }
}
