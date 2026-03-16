package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.android.keyguard.ChallengeLayout;
import com.android.keyguard.SlidingChallengeLayout;

public class KeyguardViewStateManager implements ChallengeLayout.OnBouncerStateChangedListener, SlidingChallengeLayout.OnChallengeScrolledListener {
    private ChallengeLayout mChallengeLayout;
    private KeyguardHostView mKeyguardHostView;
    private KeyguardSecurityView mKeyguardSecurityContainer;
    private KeyguardWidgetPager mKeyguardWidgetPager;
    private int[] mTmpPoint = new int[2];
    private int[] mTmpLoc = new int[2];
    Handler mMainQueue = new Handler(Looper.myLooper());
    int mLastScrollState = 0;
    private int mPageListeningToSlider = -1;
    private int mCurrentPage = -1;
    private int mPageIndexOnPageBeginMoving = -1;
    int mChallengeTop = 0;
    private final Animator.AnimatorListener mPauseListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            KeyguardViewStateManager.this.mKeyguardSecurityContainer.onPause();
        }
    };
    private final Animator.AnimatorListener mResumeListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (((View) KeyguardViewStateManager.this.mKeyguardSecurityContainer).isShown()) {
                KeyguardViewStateManager.this.mKeyguardSecurityContainer.onResume(0);
            }
        }
    };
    private Runnable mHideHintsRunnable = new Runnable() {
        @Override
        public void run() {
            if (KeyguardViewStateManager.this.mKeyguardWidgetPager != null) {
                KeyguardViewStateManager.this.mKeyguardWidgetPager.hideOutlinesAndSidePages();
            }
        }
    };

    public KeyguardViewStateManager(KeyguardHostView hostView) {
        this.mKeyguardHostView = hostView;
    }

    public void setPagedView(KeyguardWidgetPager pagedView) {
        this.mKeyguardWidgetPager = pagedView;
        updateEdgeSwiping();
    }

    public void setChallengeLayout(ChallengeLayout layout) {
        this.mChallengeLayout = layout;
        updateEdgeSwiping();
    }

    private void updateEdgeSwiping() {
        if (this.mChallengeLayout != null && this.mKeyguardWidgetPager != null) {
            if (this.mChallengeLayout.isChallengeOverlapping()) {
                this.mKeyguardWidgetPager.setOnlyAllowEdgeSwipes(true);
            } else {
                this.mKeyguardWidgetPager.setOnlyAllowEdgeSwipes(false);
            }
        }
    }

    public boolean isChallengeShowing() {
        if (this.mChallengeLayout != null) {
            return this.mChallengeLayout.isChallengeShowing();
        }
        return false;
    }

    public boolean isChallengeOverlapping() {
        if (this.mChallengeLayout != null) {
            return this.mChallengeLayout.isChallengeOverlapping();
        }
        return false;
    }

    public void setSecurityViewContainer(KeyguardSecurityView container) {
        this.mKeyguardSecurityContainer = container;
    }

    public void showBouncer(boolean show) {
        this.mChallengeLayout.showBouncer();
    }

    public boolean isBouncing() {
        return this.mChallengeLayout.isBouncing();
    }

    public void fadeOutSecurity(int duration) {
        ((View) this.mKeyguardSecurityContainer).animate().alpha(0.0f).setDuration(duration).setListener(this.mPauseListener);
    }

    public void fadeInSecurity(int duration) {
        ((View) this.mKeyguardSecurityContainer).animate().alpha(1.0f).setDuration(duration).setListener(this.mResumeListener);
    }

    public void onPageBeginMoving() {
        if (this.mChallengeLayout.isChallengeOverlapping() && (this.mChallengeLayout instanceof SlidingChallengeLayout)) {
            SlidingChallengeLayout scl = (SlidingChallengeLayout) this.mChallengeLayout;
            scl.fadeOutChallenge();
            this.mPageIndexOnPageBeginMoving = this.mKeyguardWidgetPager.getCurrentPage();
        }
        if (this.mKeyguardHostView != null) {
            this.mKeyguardHostView.clearAppWidgetToShow();
            this.mKeyguardHostView.setOnDismissAction(null);
        }
        if (this.mHideHintsRunnable != null) {
            this.mMainQueue.removeCallbacks(this.mHideHintsRunnable);
            this.mHideHintsRunnable = null;
        }
    }

    public void onPageEndMoving() {
        this.mPageIndexOnPageBeginMoving = -1;
    }

    public void onPageSwitching(View newPage, int newPageIndex) {
        if (this.mKeyguardWidgetPager != null && (this.mChallengeLayout instanceof SlidingChallengeLayout)) {
            boolean isCameraPage = newPage instanceof CameraWidgetFrame;
            if (isCameraPage) {
                CameraWidgetFrame camera = (CameraWidgetFrame) newPage;
                camera.setUseFastTransition(this.mKeyguardWidgetPager.isWarping());
            }
            SlidingChallengeLayout scl = (SlidingChallengeLayout) this.mChallengeLayout;
            scl.setChallengeInteractive(!isCameraPage);
            int currentFlags = this.mKeyguardWidgetPager.getSystemUiVisibility();
            int newFlags = isCameraPage ? currentFlags | 33554432 : currentFlags & (-33554433);
            this.mKeyguardWidgetPager.setSystemUiVisibility(newFlags);
        }
        if (this.mPageIndexOnPageBeginMoving == this.mKeyguardWidgetPager.getNextPage() && (this.mChallengeLayout instanceof SlidingChallengeLayout)) {
            SlidingChallengeLayout scl2 = (SlidingChallengeLayout) this.mChallengeLayout;
            scl2.fadeInChallenge();
            this.mKeyguardWidgetPager.setWidgetToResetOnPageFadeOut(-1);
        }
        this.mPageIndexOnPageBeginMoving = -1;
    }

    public void onPageSwitched(View newPage, int newPageIndex) {
        if (this.mCurrentPage != newPageIndex) {
            if (this.mKeyguardWidgetPager != null && this.mChallengeLayout != null) {
                KeyguardWidgetFrame prevPage = this.mKeyguardWidgetPager.getWidgetPageAt(this.mCurrentPage);
                if (prevPage != null && this.mCurrentPage != this.mPageListeningToSlider && this.mCurrentPage != this.mKeyguardWidgetPager.getWidgetToResetOnPageFadeOut()) {
                    prevPage.resetSize();
                }
                KeyguardWidgetFrame newCurPage = this.mKeyguardWidgetPager.getWidgetPageAt(newPageIndex);
                boolean challengeOverlapping = this.mChallengeLayout.isChallengeOverlapping();
                if (challengeOverlapping && !newCurPage.isSmall() && this.mPageListeningToSlider != newPageIndex) {
                    newCurPage.shrinkWidget(true);
                }
            }
            this.mCurrentPage = newPageIndex;
        }
    }

    public void onPageBeginWarp() {
        fadeOutSecurity(100);
        View frame = this.mKeyguardWidgetPager.getPageAt(this.mKeyguardWidgetPager.getPageWarpIndex());
        ((KeyguardWidgetFrame) frame).showFrame(this);
    }

    public void onPageEndWarp() {
        fadeInSecurity(160);
        View frame = this.mKeyguardWidgetPager.getPageAt(this.mKeyguardWidgetPager.getPageWarpIndex());
        ((KeyguardWidgetFrame) frame).hideFrame(this);
    }

    private int getChallengeTopRelativeToFrame(KeyguardWidgetFrame frame, int top) {
        this.mTmpPoint[0] = 0;
        this.mTmpPoint[1] = top;
        mapPoint((View) this.mChallengeLayout, frame, this.mTmpPoint);
        return this.mTmpPoint[1];
    }

    private void mapPoint(View fromView, View toView, int[] pt) {
        fromView.getLocationInWindow(this.mTmpLoc);
        int x = this.mTmpLoc[0];
        int y = this.mTmpLoc[1];
        toView.getLocationInWindow(this.mTmpLoc);
        int vX = this.mTmpLoc[0];
        int vY = this.mTmpLoc[1];
        pt[0] = pt[0] + (x - vX);
        pt[1] = pt[1] + (y - vY);
    }

    private void userActivity() {
        if (this.mKeyguardHostView != null) {
            this.mKeyguardHostView.onUserActivityTimeoutChanged();
            this.mKeyguardHostView.userActivity();
        }
    }

    @Override
    public void onScrollStateChanged(int scrollState) {
        if (this.mKeyguardWidgetPager != null && this.mChallengeLayout != null) {
            boolean challengeOverlapping = this.mChallengeLayout.isChallengeOverlapping();
            if (scrollState == 0) {
                KeyguardWidgetFrame frame = this.mKeyguardWidgetPager.getWidgetPageAt(this.mPageListeningToSlider);
                if (frame != null) {
                    if (!challengeOverlapping) {
                        if (!this.mKeyguardWidgetPager.isPageMoving()) {
                            frame.resetSize();
                            userActivity();
                        } else {
                            this.mKeyguardWidgetPager.setWidgetToResetOnPageFadeOut(this.mPageListeningToSlider);
                        }
                    }
                    if (frame.isSmall()) {
                        frame.setFrameHeight(frame.getSmallFrameHeight());
                    }
                    if (scrollState != 3) {
                        frame.hideFrame(this);
                    }
                    updateEdgeSwiping();
                    if (this.mChallengeLayout.isChallengeShowing()) {
                        this.mKeyguardSecurityContainer.onResume(2);
                    } else {
                        this.mKeyguardSecurityContainer.onPause();
                    }
                    this.mPageListeningToSlider = -1;
                } else {
                    return;
                }
            } else if (this.mLastScrollState == 0) {
                this.mPageListeningToSlider = this.mKeyguardWidgetPager.getNextPage();
                KeyguardWidgetFrame frame2 = this.mKeyguardWidgetPager.getWidgetPageAt(this.mPageListeningToSlider);
                if (frame2 != null) {
                    if (!this.mChallengeLayout.isBouncing()) {
                        if (scrollState != 3) {
                            frame2.showFrame(this);
                        }
                        if (!frame2.isSmall()) {
                            this.mPageListeningToSlider = this.mKeyguardWidgetPager.getNextPage();
                            frame2.shrinkWidget(false);
                        }
                    } else if (!frame2.isSmall()) {
                        this.mPageListeningToSlider = this.mKeyguardWidgetPager.getNextPage();
                    }
                    this.mKeyguardSecurityContainer.onPause();
                } else {
                    return;
                }
            }
            this.mLastScrollState = scrollState;
        }
    }

    @Override
    public void onScrollPositionChanged(float scrollPosition, int challengeTop) {
        this.mChallengeTop = challengeTop;
        KeyguardWidgetFrame frame = this.mKeyguardWidgetPager.getWidgetPageAt(this.mPageListeningToSlider);
        if (frame != null && this.mLastScrollState != 3) {
            frame.adjustFrame(getChallengeTopRelativeToFrame(frame, this.mChallengeTop));
        }
    }

    public void showUsabilityHints() {
        this.mMainQueue.postDelayed(new Runnable() {
            @Override
            public void run() {
                KeyguardViewStateManager.this.mKeyguardSecurityContainer.showUsabilityHint();
            }
        }, 300L);
        if (this.mHideHintsRunnable != null) {
            this.mMainQueue.postDelayed(this.mHideHintsRunnable, 1000L);
        }
    }

    @Override
    public void onBouncerStateChanged(boolean bouncerActive) {
        if (bouncerActive) {
            this.mKeyguardWidgetPager.zoomOutToBouncer();
            return;
        }
        this.mKeyguardWidgetPager.zoomInFromBouncer();
        if (this.mKeyguardHostView != null) {
            this.mKeyguardHostView.setOnDismissAction(null);
        }
    }
}
