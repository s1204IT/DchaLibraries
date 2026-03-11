package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public abstract class PanelBar extends FrameLayout {
    public static final String TAG = PanelBar.class.getSimpleName();
    PanelView mPanel;
    private int mState;
    private boolean mTracking;

    public abstract void panelScrimMinFractionChanged(float f);

    public void go(int state) {
        this.mState = state;
    }

    public PanelBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mState = 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setPanel(PanelView pv) {
        this.mPanel = pv;
        pv.setBar(this);
    }

    public void setBouncerShowing(boolean showing) {
        int important = showing ? 4 : 0;
        setImportantForAccessibility(important);
        if (this.mPanel != null) {
            this.mPanel.setImportantForAccessibility(important);
        }
    }

    public boolean panelEnabled() {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!panelEnabled()) {
            if (event.getAction() == 0) {
                Log.v(TAG, String.format("onTouch: all panels disabled, ignoring touch at (%d,%d)", Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())));
            }
            return false;
        }
        if (event.getAction() == 0) {
            PanelView panel = this.mPanel;
            if (panel == null) {
                Log.v(TAG, String.format("onTouch: no panel for touch at (%d,%d)", Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())));
                return true;
            }
            boolean enabled = panel.isEnabled();
            if (!enabled) {
                Log.v(TAG, String.format("onTouch: panel (%s) is disabled, ignoring touch at (%d,%d)", panel, Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())));
                return true;
            }
        }
        if (this.mPanel != null) {
            return this.mPanel.onTouchEvent(event);
        }
        return true;
    }

    public void panelExpansionChanged(float frac, boolean expanded) {
        boolean fullyClosed = true;
        boolean fullyOpened = false;
        PanelView pv = this.mPanel;
        pv.setVisibility(expanded ? 0 : 4);
        if (expanded) {
            if (this.mState == 0) {
                go(1);
                onPanelPeeked();
            }
            fullyClosed = false;
            float thisFrac = pv.getExpandedFraction();
            fullyOpened = thisFrac >= 1.0f;
        }
        if (fullyOpened && !this.mTracking) {
            go(2);
            onPanelFullyOpened();
        } else {
            if (!fullyClosed || this.mTracking || this.mState == 0) {
                return;
            }
            go(0);
            onPanelCollapsed();
        }
    }

    public void collapsePanel(boolean animate, boolean delayed, float speedUpFactor) {
        boolean waiting = false;
        PanelView pv = this.mPanel;
        if (animate && !pv.isFullyCollapsed()) {
            pv.collapse(delayed, speedUpFactor);
            waiting = true;
        } else {
            pv.resetViews();
            pv.setExpandedFraction(0.0f);
            pv.cancelPeek();
        }
        if (waiting || this.mState == 0) {
            return;
        }
        go(0);
        onPanelCollapsed();
    }

    public void onPanelPeeked() {
    }

    public void onPanelCollapsed() {
    }

    public void onPanelFullyOpened() {
    }

    public void onTrackingStarted() {
        this.mTracking = true;
    }

    public void onTrackingStopped(boolean expand) {
        this.mTracking = false;
    }

    public void onExpandingFinished() {
    }

    public void onClosingFinished() {
    }
}
