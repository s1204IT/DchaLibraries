package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.EventLog;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class PanelHolder extends FrameLayout {
    private PanelBar mBar;
    private int mSelectedPanelIndex;

    public PanelHolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSelectedPanelIndex = -1;
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setChildrenDrawingOrderEnabled(true);
    }

    public int getPanelIndex(PanelView pv) {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            PanelView v = (PanelView) getChildAt(i);
            if (pv == v) {
                return i;
            }
        }
        return -1;
    }

    public void setSelectedPanel(PanelView pv) {
        this.mSelectedPanelIndex = getPanelIndex(pv);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (this.mSelectedPanelIndex != -1) {
            if (i == childCount - 1) {
                return this.mSelectedPanelIndex;
            }
            if (i >= this.mSelectedPanelIndex) {
                return i + 1;
            }
            return i;
        }
        return i;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != 2) {
            EventLog.writeEvent(36040, Integer.valueOf(event.getActionMasked()), Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY()));
        }
        return false;
    }

    public void setBar(PanelBar panelBar) {
        this.mBar = panelBar;
    }
}
