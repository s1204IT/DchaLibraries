package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import com.android.systemui.R;

public class PhoneStatusBarView extends PanelBar {
    private static final boolean DEBUG = PhoneStatusBar.DEBUG;
    PhoneStatusBar mBar;
    private final PhoneStatusBarTransitions mBarTransitions;
    PanelView mLastFullyOpenedPanel;
    PanelView mNotificationPanel;
    private ScrimController mScrimController;

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLastFullyOpenedPanel = null;
        getContext().getResources();
        this.mBarTransitions = new PhoneStatusBarTransitions(this);
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setBar(PhoneStatusBar bar) {
        this.mBar = bar;
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
    }

    @Override
    public void onFinishInflate() {
        this.mBarTransitions.init();
    }

    @Override
    public void addPanel(PanelView pv) {
        super.addPanel(pv);
        if (pv.getId() == R.id.notification_panel) {
            this.mNotificationPanel = pv;
        }
    }

    @Override
    public boolean panelsEnabled() {
        return this.mBar.panelsEnabled();
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (!super.onRequestSendAccessibilityEvent(child, event)) {
            return false;
        }
        AccessibilityEvent record = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(record);
        dispatchPopulateAccessibilityEvent(record);
        event.appendRecord(record);
        return true;
    }

    @Override
    public PanelView selectPanelForTouch(MotionEvent touch) {
        if (this.mNotificationPanel.getExpandedHeight() > 0.0f) {
            return null;
        }
        return this.mNotificationPanel;
    }

    @Override
    public void onPanelPeeked() {
        super.onPanelPeeked();
        this.mBar.makeExpandedVisible(false);
    }

    @Override
    public void onAllPanelsCollapsed() {
        super.onAllPanelsCollapsed();
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                PhoneStatusBarView.this.mBar.makeExpandedInvisible();
            }
        });
        this.mLastFullyOpenedPanel = null;
    }

    @Override
    public void onPanelFullyOpened(PanelView openPanel) {
        super.onPanelFullyOpened(openPanel);
        if (openPanel != this.mLastFullyOpenedPanel) {
            openPanel.sendAccessibilityEvent(32);
        }
        this.mLastFullyOpenedPanel = openPanel;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean barConsumedEvent = this.mBar.interceptTouchEvent(event);
        return barConsumedEvent || super.onTouchEvent(event);
    }

    @Override
    public void onTrackingStarted(PanelView panel) {
        super.onTrackingStarted(panel);
        this.mBar.onTrackingStarted();
        this.mScrimController.onTrackingStarted();
    }

    @Override
    public void onClosingFinished() {
        super.onClosingFinished();
        this.mBar.onClosingFinished();
    }

    @Override
    public void onTrackingStopped(PanelView panel, boolean expand) {
        super.onTrackingStopped(panel, expand);
        this.mBar.onTrackingStopped(expand);
    }

    @Override
    public void onExpandingFinished() {
        super.onExpandingFinished();
        this.mScrimController.onExpandingFinished();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mBar.interceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public void panelExpansionChanged(PanelView panel, float frac, boolean expanded) {
        super.panelExpansionChanged(panel, frac, expanded);
        this.mScrimController.setPanelExpansion(frac);
        this.mBar.updateCarrierLabelVisibility(false);
    }
}
