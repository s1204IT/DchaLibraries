package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import java.util.ArrayList;

public class PanelBar extends FrameLayout {
    public static final String TAG = PanelBar.class.getSimpleName();
    float mPanelExpandedFractionSum;
    PanelHolder mPanelHolder;
    ArrayList<PanelView> mPanels;
    private int mState;
    PanelView mTouchingPanel;
    private boolean mTracking;

    public void go(int state) {
        this.mState = state;
    }

    public PanelBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPanels = new ArrayList<>();
        this.mState = 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void addPanel(PanelView pv) {
        this.mPanels.add(pv);
        pv.setBar(this);
    }

    public void setPanelHolder(PanelHolder ph) {
        if (ph == null) {
            Log.e(TAG, "setPanelHolder: null PanelHolder", new Throwable());
            return;
        }
        ph.setBar(this);
        this.mPanelHolder = ph;
        int N = ph.getChildCount();
        for (int i = 0; i < N; i++) {
            View v = ph.getChildAt(i);
            if (v != null && (v instanceof PanelView)) {
                addPanel((PanelView) v);
            }
        }
    }

    public PanelView selectPanelForTouch(MotionEvent touch) {
        int N = this.mPanels.size();
        return this.mPanels.get((int) ((N * touch.getX()) / getMeasuredWidth()));
    }

    public boolean panelsEnabled() {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!panelsEnabled()) {
            if (event.getAction() == 0) {
                Log.v(TAG, String.format("onTouch: all panels disabled, ignoring touch at (%d,%d)", Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())));
            }
            return false;
        }
        if (event.getAction() == 0) {
            PanelView panel = selectPanelForTouch(event);
            if (panel == null) {
                Log.v(TAG, String.format("onTouch: no panel for touch at (%d,%d)", Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())));
                this.mTouchingPanel = null;
                return true;
            }
            boolean enabled = panel.isEnabled();
            if (!enabled) {
                Log.v(TAG, String.format("onTouch: panel (%s) is disabled, ignoring touch at (%d,%d)", panel, Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())));
                this.mTouchingPanel = null;
                return true;
            }
            startOpeningPanel(panel);
        }
        if (this.mTouchingPanel != null) {
            return this.mTouchingPanel.onTouchEvent(event);
        }
        return true;
    }

    public void startOpeningPanel(PanelView panel) {
        this.mTouchingPanel = panel;
        this.mPanelHolder.setSelectedPanel(this.mTouchingPanel);
        for (PanelView pv : this.mPanels) {
            if (pv != panel) {
                pv.collapse(false);
            }
        }
    }

    public void panelExpansionChanged(PanelView panel, float frac, boolean expanded) {
        boolean fullyClosed = true;
        PanelView fullyOpenedPanel = null;
        this.mPanelExpandedFractionSum = 0.0f;
        for (PanelView pv : this.mPanels) {
            boolean visible = pv.getExpandedHeight() > 0.0f;
            pv.setVisibility(visible ? 0 : 8);
            if (expanded) {
                if (this.mState == 0) {
                    go(1);
                    onPanelPeeked();
                }
                fullyClosed = false;
                float thisFrac = pv.getExpandedFraction();
                this.mPanelExpandedFractionSum = (visible ? thisFrac : 0.0f) + this.mPanelExpandedFractionSum;
                if (panel == pv && thisFrac == 1.0f) {
                    fullyOpenedPanel = panel;
                }
            }
        }
        this.mPanelExpandedFractionSum /= this.mPanels.size();
        if (fullyOpenedPanel != null && !this.mTracking) {
            go(2);
            onPanelFullyOpened(fullyOpenedPanel);
        } else if (fullyClosed && !this.mTracking && this.mState != 0) {
            go(0);
            onAllPanelsCollapsed();
        }
    }

    public void collapseAllPanels(boolean animate) {
        boolean waiting = false;
        for (PanelView pv : this.mPanels) {
            if (animate && !pv.isFullyCollapsed()) {
                pv.collapse(true);
                waiting = true;
            } else {
                pv.resetViews();
                pv.setExpandedFraction(0.0f);
                pv.setVisibility(8);
                pv.cancelPeek();
            }
        }
        if (!waiting && this.mState != 0) {
            go(0);
            onAllPanelsCollapsed();
        }
    }

    public void onPanelPeeked() {
    }

    public void onAllPanelsCollapsed() {
    }

    public void onPanelFullyOpened(PanelView openPanel) {
    }

    public void onTrackingStarted(PanelView panel) {
        this.mTracking = true;
    }

    public void onTrackingStopped(PanelView panel, boolean expand) {
        this.mTracking = false;
    }

    public void onExpandingFinished() {
    }

    public void onClosingFinished() {
    }
}
