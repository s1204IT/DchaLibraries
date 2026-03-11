package android.support.design.widget;

import android.content.Context;
import android.support.design.animation.AnimationUtils;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

public class HideBottomViewOnScrollBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    private int height;

    public HideBottomViewOnScrollBehavior() {
        this.height = 0;
    }

    public HideBottomViewOnScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.height = 0;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        this.height = child.getMeasuredHeight();
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == 2;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (dyConsumed > 0) {
            slideDown(child);
        } else if (dyConsumed < 0) {
            slideUp(child);
        }
    }

    private void slideUp(V child) {
        child.clearAnimation();
        child.animate().translationY(0.0f).setInterpolator(AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR).setDuration(225L);
    }

    private void slideDown(V child) {
        child.clearAnimation();
        child.animate().translationY(this.height).setInterpolator(AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR).setDuration(175L);
    }
}
