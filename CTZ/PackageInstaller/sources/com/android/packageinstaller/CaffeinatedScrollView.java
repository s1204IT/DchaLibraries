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

    public CaffeinatedScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean awakenScrollBars() {
        return super.awakenScrollBars();
    }

    public void setFullScrollAction(Runnable runnable) {
        this.mFullScrollAction = runnable;
        this.mBottomSlop = (int) (4.0f * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        checkFullScrollAction();
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        checkFullScrollAction();
    }

    private void checkFullScrollAction() {
        if (this.mFullScrollAction != null && getChildAt(0).getBottom() - ((getScrollY() + getHeight()) - getPaddingBottom()) < this.mBottomSlop) {
            this.mFullScrollAction.run();
            this.mFullScrollAction = null;
        }
    }
}
