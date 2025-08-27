package com.android.systemui.recents.views.grid;

import android.content.Context;
import android.util.AttributeSet;
import com.android.systemui.R;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.shared.recents.view.AnimateableViewBounds;

/* loaded from: classes.dex */
public class GridTaskView extends TaskView {
    private int mHeaderHeight;

    public GridTaskView(Context context) {
        this(context, null);
    }

    public GridTaskView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GridTaskView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public GridTaskView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mHeaderHeight = context.getResources().getDimensionPixelSize(R.dimen.recents_grid_task_view_header_height);
    }

    @Override // com.android.systemui.recents.views.TaskView, android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mThumbnailView.setSizeToFit(true);
        this.mThumbnailView.setOverlayHeaderOnThumbnailActionBar(false);
        this.mThumbnailView.updateThumbnailMatrix();
        this.mThumbnailView.setTranslationY(this.mHeaderHeight);
        this.mHeaderView.setShouldDarkenBackgroundColor(true);
    }

    @Override // com.android.systemui.recents.views.TaskView
    protected AnimateableViewBounds createOutlineProvider() {
        return new AnimateableGridViewBounds(this, this.mContext.getResources().getDimensionPixelSize(R.dimen.recents_task_view_shadow_rounded_corners_radius));
    }

    @Override // com.android.systemui.recents.views.TaskView
    protected void onConfigurationChanged() {
        super.onConfigurationChanged();
        this.mHeaderHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.recents_grid_task_view_header_height);
        this.mThumbnailView.setTranslationY(this.mHeaderHeight);
    }
}
