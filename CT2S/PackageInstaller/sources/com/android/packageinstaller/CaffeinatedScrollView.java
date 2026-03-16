package com.android.packageinstaller;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ScrollView;

class CaffeinatedScrollView extends ScrollView {
    private int mBottomSlop;
    private Runnable mFullScrollAction;

    public CaffeinatedScrollView(Context context) {
        super(context);
    }

    public CaffeinatedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean awakenScrollBars() {
        return super.awakenScrollBars();
    }

    public void setFullScrollAction(Runnable action) {
        this.mFullScrollAction = action;
        this.mBottomSlop = (int) (4.0f * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        checkFullScrollAction();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        checkFullScrollAction();
    }

    private void checkFullScrollAction() {
        if (this.mFullScrollAction != null) {
            int daBottom = getChildAt(0).getBottom();
            int screenBottom = (getScrollY() + getHeight()) - getPaddingBottom();
            if (daBottom - screenBottom < this.mBottomSlop) {
                this.mFullScrollAction.run();
                this.mFullScrollAction = null;
            }
        }
    }
}
