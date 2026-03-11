package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.R;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.qs.customize.QSCustomizer;

public class NotificationsQuickSettingsContainer extends FrameLayout implements ViewStub.OnInflateListener, AutoReinflateContainer.InflateListener {
    private int mBottomPadding;
    private boolean mCustomizerAnimating;
    private boolean mInflated;
    private View mKeyguardStatusBar;
    private AutoReinflateContainer mQsContainer;
    private boolean mQsExpanded;
    private View mStackScroller;
    private int mStackScrollerMargin;
    private View mUserSwitcher;

    public NotificationsQuickSettingsContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mQsContainer = (AutoReinflateContainer) findViewById(R.id.qs_auto_reinflate_container);
        this.mQsContainer.addInflateListener(this);
        this.mStackScroller = findViewById(R.id.notification_stack_scroller);
        this.mStackScrollerMargin = ((FrameLayout.LayoutParams) this.mStackScroller.getLayoutParams()).bottomMargin;
        this.mKeyguardStatusBar = findViewById(R.id.keyguard_header);
        ViewStub userSwitcher = (ViewStub) findViewById(R.id.keyguard_user_switcher);
        userSwitcher.setOnInflateListener(this);
        this.mUserSwitcher = userSwitcher;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadWidth(this.mQsContainer);
        reloadWidth(this.mStackScroller);
    }

    private void reloadWidth(View view) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.width = getContext().getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        view.setLayoutParams(params);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mBottomPadding = insets.getStableInsetBottom();
        setPadding(0, 0, 0, this.mBottomPadding);
        return insets;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean qsBottom = false;
        boolean userSwitcherVisible = this.mInflated && this.mUserSwitcher.getVisibility() == 0;
        boolean statusBarVisible = this.mKeyguardStatusBar.getVisibility() == 0;
        if (this.mQsExpanded && !this.mCustomizerAnimating) {
            qsBottom = true;
        }
        View stackQsTop = qsBottom ? this.mStackScroller : this.mQsContainer;
        View stackQsBottom = !qsBottom ? this.mStackScroller : this.mQsContainer;
        if (child == this.mQsContainer) {
            if (userSwitcherVisible && statusBarVisible) {
                stackQsBottom = this.mUserSwitcher;
            } else if (statusBarVisible) {
                stackQsBottom = this.mKeyguardStatusBar;
            } else if (userSwitcherVisible) {
                stackQsBottom = this.mUserSwitcher;
            }
            return super.drawChild(canvas, stackQsBottom, drawingTime);
        }
        if (child == this.mStackScroller) {
            if (userSwitcherVisible && statusBarVisible) {
                stackQsTop = this.mKeyguardStatusBar;
            } else if (statusBarVisible || userSwitcherVisible) {
                stackQsTop = stackQsBottom;
            }
            return super.drawChild(canvas, stackQsTop, drawingTime);
        }
        if (child == this.mUserSwitcher) {
            if (!userSwitcherVisible || !statusBarVisible) {
                stackQsBottom = stackQsTop;
            }
            return super.drawChild(canvas, stackQsBottom, drawingTime);
        }
        if (child == this.mKeyguardStatusBar) {
            return super.drawChild(canvas, stackQsTop, drawingTime);
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void onInflate(ViewStub stub, View inflated) {
        if (stub != this.mUserSwitcher) {
            return;
        }
        this.mUserSwitcher = inflated;
        this.mInflated = true;
    }

    @Override
    public void onInflated(View v) {
        QSCustomizer customizer = ((QSContainer) v).getCustomizer();
        customizer.setContainer(this);
    }

    public void setQsExpanded(boolean expanded) {
        if (this.mQsExpanded == expanded) {
            return;
        }
        this.mQsExpanded = expanded;
        invalidate();
    }

    public void setCustomizerAnimating(boolean isAnimating) {
        if (this.mCustomizerAnimating == isAnimating) {
            return;
        }
        this.mCustomizerAnimating = isAnimating;
        invalidate();
    }

    public void setCustomizerShowing(boolean isShowing) {
        if (isShowing) {
            setPadding(0, 0, 0, 0);
            setBottomMargin(this.mStackScroller, 0);
        } else {
            setPadding(0, 0, 0, this.mBottomPadding);
            setBottomMargin(this.mStackScroller, this.mStackScrollerMargin);
        }
    }

    private void setBottomMargin(View v, int bottomMargin) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
        params.bottomMargin = bottomMargin;
        v.setLayoutParams(params);
    }
}
