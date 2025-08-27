package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import java.util.Objects;

/* loaded from: classes.dex */
public class PhoneStatusBarView extends PanelBar {
    private static final boolean DEBUG = StatusBar.DEBUG;
    StatusBar mBar;
    private final PhoneStatusBarTransitions mBarTransitions;
    private DarkIconDispatcher.DarkReceiver mBattery;
    private int mCutoutSideNudge;
    private View mCutoutSpace;
    private DisplayCutout mDisplayCutout;
    private Runnable mHideExpandedRunnable;
    boolean mIsFullyOpenedPanel;
    private int mLastOrientation;
    private float mMinFraction;
    private float mPanelFraction;
    private ScrimController mScrimController;

    public PhoneStatusBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsFullyOpenedPanel = false;
        this.mHideExpandedRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.PhoneStatusBarView.1
            @Override // java.lang.Runnable
            public void run() {
                if (PhoneStatusBarView.this.mPanelFraction == 0.0f) {
                    PhoneStatusBarView.this.mBar.makeExpandedInvisible();
                }
            }
        };
        this.mCutoutSideNudge = 0;
        this.mBarTransitions = new PhoneStatusBarTransitions(this);
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setBar(StatusBar statusBar) {
        this.mBar = statusBar;
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar, android.view.View
    public void onFinishInflate() {
        this.mBarTransitions.init();
        this.mBattery = (DarkIconDispatcher.DarkReceiver) findViewById(R.id.battery);
        this.mCutoutSpace = findViewById(R.id.cutout_space_view);
        updateResources();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mBattery);
        if (updateOrientationAndCutout(getResources().getConfiguration().orientation)) {
            updateLayoutForCutout();
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this.mBattery);
        this.mDisplayCutout = null;
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (updateOrientationAndCutout(configuration.orientation)) {
            updateLayoutForCutout();
            requestLayout();
        }
    }

    @Override // android.view.View
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        if (updateOrientationAndCutout(this.mLastOrientation)) {
            updateLayoutForCutout();
            requestLayout();
        }
        return super.onApplyWindowInsets(windowInsets);
    }

    private boolean updateOrientationAndCutout(int i) {
        boolean z;
        if (i == Integer.MIN_VALUE || this.mLastOrientation == i) {
            z = false;
        } else {
            this.mLastOrientation = i;
            z = true;
        }
        if (Objects.equals(getRootWindowInsets().getDisplayCutout(), this.mDisplayCutout)) {
            return z;
        }
        this.mDisplayCutout = getRootWindowInsets().getDisplayCutout();
        return true;
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public boolean panelEnabled() {
        return this.mBar.panelsEnabled();
    }

    public boolean onRequestSendAccessibilityEventInternal(View view, AccessibilityEvent accessibilityEvent) {
        if (super.onRequestSendAccessibilityEventInternal(view, accessibilityEvent)) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(accessibilityEventObtain);
            dispatchPopulateAccessibilityEvent(accessibilityEventObtain);
            accessibilityEvent.appendRecord(accessibilityEventObtain);
            return true;
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onPanelPeeked() {
        super.onPanelPeeked();
        this.mBar.makeExpandedVisible(false);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onPanelCollapsed() {
        super.onPanelCollapsed();
        post(this.mHideExpandedRunnable);
        this.mIsFullyOpenedPanel = false;
    }

    public void removePendingHideExpandedRunnables() {
        removeCallbacks(this.mHideExpandedRunnable);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onPanelFullyOpened() {
        super.onPanelFullyOpened();
        if (!this.mIsFullyOpenedPanel) {
            this.mPanel.sendAccessibilityEvent(32);
        }
        this.mIsFullyOpenedPanel = true;
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mBar.interceptTouchEvent(motionEvent) || super.onTouchEvent(motionEvent);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onTrackingStarted() {
        super.onTrackingStarted();
        this.mBar.onTrackingStarted();
        this.mScrimController.onTrackingStarted();
        removePendingHideExpandedRunnables();
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onClosingFinished() {
        super.onClosingFinished();
        this.mBar.onClosingFinished();
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onTrackingStopped(boolean z) {
        super.onTrackingStopped(z);
        this.mBar.onTrackingStopped(z);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onExpandingFinished() {
        super.onExpandingFinished();
        this.mScrimController.onExpandingFinished();
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return this.mBar.interceptTouchEvent(motionEvent) || super.onInterceptTouchEvent(motionEvent);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void panelScrimMinFractionChanged(float f) {
        if (this.mMinFraction != f) {
            this.mMinFraction = f;
            updateScrimFraction();
        }
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void panelExpansionChanged(float f, boolean z) {
        super.panelExpansionChanged(f, z);
        this.mPanelFraction = f;
        updateScrimFraction();
        if ((f == 0.0f || f == 1.0f) && this.mBar.getNavigationBarView() != null) {
            this.mBar.getNavigationBarView().onPanelExpandedChange(z);
        }
    }

    private void updateScrimFraction() {
        float fMax = this.mPanelFraction;
        if (this.mMinFraction < 1.0f) {
            fMax = Math.max((this.mPanelFraction - this.mMinFraction) / (1.0f - this.mMinFraction), 0.0f);
        }
        this.mScrimController.setPanelExpansion(fMax);
    }

    public void updateResources() {
        this.mCutoutSideNudge = getResources().getDimensionPixelSize(R.dimen.display_cutout_margin_consumption);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
        setLayoutParams(layoutParams);
    }

    private void updateLayoutForCutout() {
        Pair<Integer, Integer> pairCornerCutoutMargins = cornerCutoutMargins(this.mDisplayCutout, getDisplay());
        updateCutoutLocation(pairCornerCutoutMargins);
        updateSafeInsets(pairCornerCutoutMargins);
    }

    private void updateCutoutLocation(Pair<Integer, Integer> pair) {
        if (this.mCutoutSpace == null) {
            return;
        }
        if (this.mDisplayCutout == null || this.mDisplayCutout.isEmpty() || this.mLastOrientation != 1 || pair != null) {
            this.mCutoutSpace.setVisibility(8);
            return;
        }
        this.mCutoutSpace.setVisibility(0);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mCutoutSpace.getLayoutParams();
        Rect rect = new Rect();
        ScreenDecorations.DisplayCutoutView.boundsFromDirection(this.mDisplayCutout, 48, rect);
        rect.left += this.mCutoutSideNudge;
        rect.right -= this.mCutoutSideNudge;
        layoutParams.width = rect.width();
        layoutParams.height = rect.height();
    }

    private void updateSafeInsets(Pair<Integer, Integer> pair) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        if (this.mDisplayCutout == null) {
            layoutParams.leftMargin = 0;
            layoutParams.rightMargin = 0;
            return;
        }
        layoutParams.leftMargin = this.mDisplayCutout.getSafeInsetLeft();
        layoutParams.rightMargin = this.mDisplayCutout.getSafeInsetRight();
        if (pair != null) {
            layoutParams.leftMargin = Math.max(layoutParams.leftMargin, ((Integer) pair.first).intValue());
            layoutParams.rightMargin = Math.max(layoutParams.rightMargin, ((Integer) pair.second).intValue());
            WindowInsets rootWindowInsets = getRootWindowInsets();
            int systemWindowInsetLeft = rootWindowInsets.getSystemWindowInsetLeft();
            int systemWindowInsetRight = rootWindowInsets.getSystemWindowInsetRight();
            if (layoutParams.leftMargin <= systemWindowInsetLeft) {
                layoutParams.leftMargin = 0;
            }
            if (layoutParams.rightMargin <= systemWindowInsetRight) {
                layoutParams.rightMargin = 0;
            }
        }
    }

    public static Pair<Integer, Integer> cornerCutoutMargins(DisplayCutout displayCutout, Display display) {
        if (displayCutout == null) {
            return null;
        }
        Point point = new Point();
        display.getRealSize(point);
        Rect rect = new Rect();
        ScreenDecorations.DisplayCutoutView.boundsFromDirection(displayCutout, 48, rect);
        if (rect.left <= 0) {
            return new Pair<>(Integer.valueOf(rect.right), 0);
        }
        if (rect.right < point.x) {
            return null;
        }
        return new Pair<>(0, Integer.valueOf(point.x - rect.left));
    }
}
