package androidx.car.widget;

import android.content.Context;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public final class PagedSmoothScroller extends LinearSmoothScroller {
    private final Interpolator mInterpolator;

    public PagedSmoothScroller(Context context) {
        super(context);
        this.mInterpolator = new DecelerateInterpolator(1.8f);
    }

    @Override
    protected int getVerticalSnapPreference() {
        return -1;
    }

    @Override
    protected void onTargetFound(View targetView, RecyclerView.State state, RecyclerView.SmoothScroller.Action action) {
        int time;
        int dy = calculateDyToMakeVisible(targetView, -1);
        if (dy != 0 && (time = calculateTimeForDeceleration(dy)) > 0) {
            action.update(0, -dy, time, this.mInterpolator);
        }
    }

    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return 150.0f / displayMetrics.densityDpi;
    }

    @Override
    protected int calculateTimeForDeceleration(int dx) {
        return (int) Math.ceil(calculateTimeForScrolling(dx) / 0.45f);
    }
}
