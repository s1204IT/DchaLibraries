package com.android.browser;

import android.content.res.Resources;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import com.android.browser.BrowserWebView;

/* loaded from: classes.dex */
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

    public UrlBarAutoShowManager(BaseUi baseUi) {
        this.mUi = baseUi;
        this.mSlop = ViewConfiguration.get(this.mUi.getActivity()).getScaledTouchSlop() * 2;
    }

    public void setTarget(BrowserWebView browserWebView) {
        if (this.mTarget == browserWebView) {
            return;
        }
        if (this.mTarget != null) {
            this.mTarget.setOnTouchListener(null);
            this.mTarget.setOnScrollChangedListener(null);
        }
        this.mTarget = browserWebView;
        if (this.mTarget != null) {
            this.mTarget.setOnTouchListener(this);
            this.mTarget.setOnScrollChangedListener(this);
        }
    }

    @Override // com.android.browser.BrowserWebView.OnScrollChangedListener
    public void onScrollChanged(int i, int i2, int i3, int i4) throws Resources.NotFoundException {
        this.mLastScrollTime = SystemClock.uptimeMillis();
        this.mIsScrolling = true;
        if (i2 != 0) {
            if (this.mUi.isTitleBarShowing()) {
                this.mUi.showTitleBarForDuration(Math.max(2000 - (this.mLastScrollTime - this.mTriggeredTime), SCROLL_TIMEOUT_DURATION));
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

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.View.OnTouchListener
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() > 1) {
            stopTracking();
        }
        switch (motionEvent.getAction()) {
            case 0:
                if (!this.mIsTracking && motionEvent.getPointerCount() == 1 && SystemClock.uptimeMillis() - this.mLastScrollTime >= IGNORE_INTERVAL) {
                    this.mStartTouchY = motionEvent.getY();
                    this.mStartTouchX = motionEvent.getX();
                    this.mIsTracking = true;
                    this.mHasTriggered = false;
                }
                return false;
            case 1:
            case 3:
                stopTracking();
                return false;
            case 2:
                if (this.mIsTracking && !this.mHasTriggered) {
                    WebView webView = (WebView) view;
                    float y = motionEvent.getY() - this.mStartTouchY;
                    float fAbs = Math.abs(y);
                    float fAbs2 = Math.abs(motionEvent.getX() - this.mStartTouchX);
                    if (fAbs > this.mSlop) {
                        this.mHasTriggered = true;
                        float fAtan2 = (float) Math.atan2(fAbs, fAbs2);
                        if (y > this.mSlop && fAtan2 > V_TRIGGER_ANGLE && !this.mUi.isTitleBarShowing() && (webView.getVisibleTitleHeight() == 0 || (!this.mIsScrolling && webView.getScrollY() > 0))) {
                            this.mTriggeredTime = SystemClock.uptimeMillis();
                            this.mUi.showTitleBar();
                        }
                        this.mUi.showBottomBarForDuration(2000L);
                    }
                }
                return false;
            default:
                return false;
        }
    }
}
