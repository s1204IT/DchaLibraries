package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.android.systemui.R;

public class NotificationsQuickSettingsContainer extends FrameLayout implements ViewStub.OnInflateListener {
    private boolean mInflated;
    private View mKeyguardStatusBar;
    private View mScrollView;
    private View mStackScroller;
    private View mUserSwitcher;

    public NotificationsQuickSettingsContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mScrollView = findViewById(R.id.scroll_view);
        this.mStackScroller = findViewById(R.id.notification_stack_scroller);
        this.mKeyguardStatusBar = findViewById(R.id.keyguard_header);
        ViewStub userSwitcher = (ViewStub) findViewById(R.id.keyguard_user_switcher);
        userSwitcher.setOnInflateListener(this);
        this.mUserSwitcher = userSwitcher;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
        return insets;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean userSwitcherVisible = this.mInflated && this.mUserSwitcher.getVisibility() == 0;
        boolean statusBarVisible = this.mKeyguardStatusBar.getVisibility() == 0;
        if (child == this.mScrollView) {
            return super.drawChild(canvas, this.mStackScroller, drawingTime);
        }
        if (child == this.mStackScroller) {
            return super.drawChild(canvas, (userSwitcherVisible && statusBarVisible) ? this.mUserSwitcher : statusBarVisible ? this.mKeyguardStatusBar : userSwitcherVisible ? this.mUserSwitcher : this.mScrollView, drawingTime);
        }
        if (child == this.mUserSwitcher) {
            return super.drawChild(canvas, (userSwitcherVisible && statusBarVisible) ? this.mKeyguardStatusBar : this.mScrollView, drawingTime);
        }
        if (child == this.mKeyguardStatusBar) {
            return super.drawChild(canvas, (!userSwitcherVisible || statusBarVisible) ? this.mScrollView : this.mScrollView, drawingTime);
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void onInflate(ViewStub stub, View inflated) {
        if (stub == this.mUserSwitcher) {
            this.mUserSwitcher = inflated;
            this.mInflated = true;
        }
    }
}
