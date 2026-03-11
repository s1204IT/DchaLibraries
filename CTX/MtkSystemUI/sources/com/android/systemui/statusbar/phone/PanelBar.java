package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public abstract class PanelBar extends FrameLayout {
    public static final String TAG = PanelBar.class.getSimpleName();
    private boolean mBouncerShowing;
    private boolean mExpanded;
    PanelView mPanel;
    private int mState;
    private boolean mTracking;

    public abstract void panelScrimMinFractionChanged(float f);

    public void go(int i) {
        this.mState = i;
    }

    public int getState() {
        return this.mState;
    }

    public PanelBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mState = 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setPanel(PanelView panelView) {
        this.mPanel = panelView;
        panelView.setBar(this);
    }

    public void setBouncerShowing(boolean z) {
        this.mBouncerShowing = z;
        int i = z ? 4 : 0;
        setImportantForAccessibility(i);
        updateVisibility();
        if (this.mPanel != null) {
            this.mPanel.setImportantForAccessibility(i);
        }
    }

    private void updateVisibility() {
        this.mPanel.setVisibility((this.mExpanded || this.mBouncerShowing) ? 0 : 4);
    }

    public boolean panelEnabled() {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!panelEnabled()) {
            if (motionEvent.getAction() == 0) {
                Log.v(TAG, String.format("onTouch: all panels disabled, ignoring touch at (%d,%d)", Integer.valueOf((int) motionEvent.getX()), Integer.valueOf((int) motionEvent.getY())));
            }
            return false;
        }
        if (motionEvent.getAction() == 0) {
            PanelView panelView = this.mPanel;
            if (panelView == null) {
                Log.v(TAG, String.format("onTouch: no panel for touch at (%d,%d)", Integer.valueOf((int) motionEvent.getX()), Integer.valueOf((int) motionEvent.getY())));
                return true;
            }
            if (!panelView.isEnabled()) {
                Log.v(TAG, String.format("onTouch: panel (%s) is disabled, ignoring touch at (%d,%d)", panelView, Integer.valueOf((int) motionEvent.getX()), Integer.valueOf((int) motionEvent.getY())));
                return true;
            }
        }
        return this.mPanel == null || this.mPanel.onTouchEvent(motionEvent);
    }

    public void panelExpansionChanged(float f, boolean z) {
        boolean z2;
        PanelView panelView = this.mPanel;
        this.mExpanded = z;
        updateVisibility();
        boolean z3 = true;
        if (!z) {
            z2 = true;
            z3 = false;
        } else {
            if (this.mState == 0) {
                go(1);
                onPanelPeeked();
            }
            if (panelView.getExpandedFraction() < 1.0f) {
                z3 = false;
            }
            z2 = false;
        }
        if (z3 && !this.mTracking) {
            go(2);
            onPanelFullyOpened();
        } else if (z2 && !this.mTracking && this.mState != 0) {
            go(0);
            onPanelCollapsed();
        }
    }

    public void collapsePanel(boolean z, boolean z2, float f) {
        boolean z3;
        PanelView panelView = this.mPanel;
        if (z && !panelView.isFullyCollapsed()) {
            panelView.collapse(z2, f);
            z3 = true;
        } else {
            panelView.resetViews();
            panelView.setExpandedFraction(0.0f);
            panelView.cancelPeek();
            z3 = false;
        }
        if (!z3 && this.mState != 0) {
            go(0);
            onPanelCollapsed();
        }
    }

    public void onPanelPeeked() {
    }

    public boolean isClosed() {
        return this.mState == 0;
    }

    public void onPanelCollapsed() {
    }

    public void onPanelFullyOpened() {
    }

    public void onTrackingStarted() {
        this.mTracking = true;
    }

    public void onTrackingStopped(boolean z) {
        this.mTracking = false;
    }

    public void onExpandingFinished() {
    }

    public void onClosingFinished() {
    }
}
