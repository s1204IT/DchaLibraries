package com.android.quickstep.fallback;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.R;
import com.android.launcher3.util.Themes;
import com.android.launcher3.util.TouchController;
import com.android.launcher3.views.BaseDragLayer;
import com.android.quickstep.RecentsActivity;

/* loaded from: classes.dex */
public class RecentsRootView extends BaseDragLayer<RecentsActivity> {
    private final RecentsActivity mActivity;
    private final Point mLastKnownSize;

    public RecentsRootView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, 1);
        this.mLastKnownSize = new Point(10, 10);
        this.mActivity = (RecentsActivity) BaseActivity.fromContext(context);
        setSystemUiVisibility(1792);
    }

    public Point getLastKnownSize() {
        return this.mLastKnownSize;
    }

    public void setup() {
        this.mControllers = new TouchController[]{new RecentsTaskController(this.mActivity)};
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        if (this.mLastKnownSize.x != size || this.mLastKnownSize.y != size2) {
            this.mLastKnownSize.set(size, size2);
            this.mActivity.onRootViewSizeChanged();
        }
        super.onMeasure(i, i2);
    }

    @Override // android.view.View
    @TargetApi(23)
    protected boolean fitSystemWindows(Rect rect) {
        this.mActivity.getDeviceProfile().updateInsets(rect);
        setInsets(rect);
        return true;
    }

    @Override // com.android.launcher3.InsettableFrameLayout, com.android.launcher3.Insettable
    public void setInsets(Rect rect) {
        if (!rect.equals(this.mInsets)) {
            super.setInsets(rect);
        }
        setBackground(rect.top == 0 ? null : Themes.getAttrDrawable(getContext(), R.attr.workspaceStatusBarScrim));
    }

    public void dispatchInsets() {
        this.mActivity.getDeviceProfile().updateInsets(this.mInsets);
        super.setInsets(this.mInsets);
    }
}
