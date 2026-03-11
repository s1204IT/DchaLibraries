package com.android.browser;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import com.android.browser.BrowserWebView;

public class UrlBarAutoShowManager implements View.OnTouchListener, BrowserWebView.OnScrollChangedListener {
    private boolean mHasTriggered;
    private boolean mIsScrolling;
    private boolean mIsTracking;
    private long mLastScrollTime;
    private int mSlop;
    private float mStartTouchX;
    private float mStartTouchY;
    private BrowserWebView mTarget;
    private long mTriggeredTime;
    private BaseUi mUi;
    private static float V_TRIGGER_ANGLE = 0.9f;
    private static long SCROLL_TIMEOUT_DURATION = 150;
    private static long IGNORE_INTERVAL = 250;

    public UrlBarAutoShowManager(BaseUi ui) {
        this.mUi = ui;
        ViewConfiguration config = ViewConfiguration.get(this.mUi.getActivity());
        this.mSlop = config.getScaledTouchSlop() * 2;
    }

    public void setTarget(BrowserWebView v) {
        if (this.mTarget != v) {
            if (this.mTarget != null) {
                this.mTarget.setOnTouchListener(null);
                this.mTarget.setOnScrollChangedListener(null);
            }
            this.mTarget = v;
            if (this.mTarget != null) {
                this.mTarget.setOnTouchListener(this);
                this.mTarget.setOnScrollChangedListener(this);
            }
        }
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        this.mLastScrollTime = SystemClock.uptimeMillis();
        this.mIsScrolling = true;
        if (t != 0) {
            if (this.mUi.isTitleBarShowing()) {
                long remaining = this.mLastScrollTime - this.mTriggeredTime;
                this.mUi.showTitleBarForDuration(Math.max(1500 - remaining, SCROLL_TIMEOUT_DURATION));
                return;
            }
            return;
        }
        this.mUi.suggestHideTitleBar();
    }

    void stopTracking() {
        if (this.mIsTracking) {
            this.mIsTracking = false;
            this.mIsScrolling = false;
            if (this.mUi.isTitleBarShowing()) {
                this.mUi.showTitleBarForDuration();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() > 1) {
            stopTracking();
        }
        switch (event.getAction()) {
            case 0:
                if (!this.mIsTracking && event.getPointerCount() == 1) {
                    long sinceLastScroll = SystemClock.uptimeMillis() - this.mLastScrollTime;
                    if (sinceLastScroll >= IGNORE_INTERVAL) {
                        this.mStartTouchY = event.getY();
                        this.mStartTouchX = event.getX();
                        this.mIsTracking = true;
                        this.mHasTriggered = false;
                    }
                    break;
                }
                break;
            case 1:
            case 3:
                stopTracking();
                break;
            case 2:
                if (this.mIsTracking && !this.mHasTriggered) {
                    WebView web = (WebView) v;
                    float dy = event.getY() - this.mStartTouchY;
                    float ady = Math.abs(dy);
                    float adx = Math.abs(event.getX() - this.mStartTouchX);
                    if (ady > this.mSlop) {
                        this.mHasTriggered = true;
                        float angle = (float) Math.atan2(ady, adx);
                        if (dy > this.mSlop && angle > V_TRIGGER_ANGLE && !this.mUi.isTitleBarShowing()) {
                            if (web.getVisibleTitleHeight() == 0 || (!this.mIsScrolling && web.getScrollY() > 0)) {
                                this.mTriggeredTime = SystemClock.uptimeMillis();
                                this.mUi.showTitleBar();
                            }
                            break;
                        }
                    }
                }
                break;
        }
        return false;
    }
}
