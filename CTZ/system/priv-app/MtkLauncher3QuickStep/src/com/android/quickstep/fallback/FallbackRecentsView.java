package com.android.quickstep.fallback;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.DeviceProfile;
import com.android.quickstep.RecentsActivity;
import com.android.quickstep.util.LayoutUtils;
import com.android.quickstep.views.RecentsView;

/* loaded from: classes.dex */
public class FallbackRecentsView extends RecentsView<RecentsActivity> {
    public FallbackRecentsView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FallbackRecentsView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setOverviewStateEnabled(true);
        getQuickScrubController().onFinishedTransitionToQuickScrub();
    }

    @Override // com.android.quickstep.views.RecentsView
    protected void onAllTasksRemoved() {
        ((RecentsActivity) this.mActivity).startHome();
    }

    @Override // com.android.quickstep.views.RecentsView, com.android.launcher3.PagedView, android.view.ViewGroup
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        updateEmptyMessage();
    }

    @Override // com.android.quickstep.views.RecentsView, com.android.launcher3.PagedView, android.view.ViewGroup
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        updateEmptyMessage();
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        maybeDrawEmptyMessage(canvas);
        super.draw(canvas);
    }

    @Override // com.android.quickstep.views.RecentsView
    protected void getTaskSize(DeviceProfile deviceProfile, Rect rect) throws Resources.NotFoundException {
        LayoutUtils.calculateFallbackTaskSize(getContext(), deviceProfile, rect);
    }

    @Override // com.android.quickstep.views.RecentsView
    public boolean shouldUseMultiWindowTaskSizeStrategy() {
        return false;
    }
}
