package com.android.quickstep.fallback;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.DeviceProfile;
import com.android.quickstep.RecentsActivity;
import com.android.quickstep.util.LayoutUtils;
import com.android.quickstep.views.RecentsView;

public class FallbackRecentsView extends RecentsView<RecentsActivity> {
    public FallbackRecentsView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FallbackRecentsView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setOverviewStateEnabled(true);
        getQuickScrubController().onFinishedTransitionToQuickScrub();
    }

    @Override
    protected void onAllTasksRemoved() {
        ((RecentsActivity) this.mActivity).startHome();
    }

    @Override
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        updateEmptyMessage();
    }

    @Override
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        updateEmptyMessage();
    }

    @Override
    public void draw(Canvas canvas) {
        maybeDrawEmptyMessage(canvas);
        super.draw(canvas);
    }

    @Override
    protected void getTaskSize(DeviceProfile deviceProfile, Rect rect) {
        LayoutUtils.calculateFallbackTaskSize(getContext(), deviceProfile, rect);
    }

    @Override
    public boolean shouldUseMultiWindowTaskSizeStrategy() {
        return false;
    }
}
