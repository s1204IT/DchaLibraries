package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextClock;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.ChallengeLayout;
import com.android.keyguard.PagedView;
import java.util.ArrayList;
import java.util.TimeZone;

public class KeyguardWidgetPager extends PagedView implements View.OnLongClickListener, ChallengeLayout.OnBouncerStateChangedListener, PagedView.PageSwitchListener {
    private static float CAMERA_DISTANCE = 10000.0f;
    protected static float OVERSCROLL_MAX_ROTATION = 30.0f;
    private float BOUNCER_SCALE_FACTOR;
    private View mAddWidgetView;
    private final Handler mBackgroundWorkerHandler;
    private final HandlerThread mBackgroundWorkerThread;
    private int mBouncerZoomInOutDuration;
    private Callbacks mCallbacks;
    private boolean mCenterSmallWidgetsVertically;
    protected AnimatorSet mChildrenOutlineFadeAnimation;
    private boolean mHasMeasure;
    private int mLastHeightMeasureSpec;
    private int mLastWidthMeasureSpec;
    private LockPatternUtils mLockPatternUtils;
    private int mPage;
    protected int mScreenCenter;
    protected boolean mShowingInitialHints;
    protected KeyguardViewStateManager mViewStateManager;
    private int mWidgetToResetAfterFadeOut;
    ZInterpolator mZInterpolator;
    boolean showHintsAfterLayout;

    public interface Callbacks {
        void onAddView(View view);

        void onRemoveView(View view, boolean z);

        void onRemoveViewAnimationCompleted();

        void onUserActivityTimeoutChanged();

        void userActivity();
    }

    public KeyguardWidgetPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardWidgetPager(Context context) {
        this(null, null, 0);
    }

    public KeyguardWidgetPager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mZInterpolator = new ZInterpolator(0.5f);
        this.mHasMeasure = false;
        this.showHintsAfterLayout = false;
        this.mPage = 0;
        this.mShowingInitialHints = false;
        this.mBouncerZoomInOutDuration = 250;
        this.BOUNCER_SCALE_FACTOR = 0.67f;
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
        setPageSwitchListener(this);
        this.mBackgroundWorkerThread = new HandlerThread("KeyguardWidgetPager Worker");
        this.mBackgroundWorkerThread.start();
        this.mBackgroundWorkerHandler = new Handler(this.mBackgroundWorkerThread.getLooper());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mBackgroundWorkerThread.quit();
    }

    public void setViewStateManager(KeyguardViewStateManager viewStateManager) {
        this.mViewStateManager = viewStateManager;
    }

    public void setLockPatternUtils(LockPatternUtils l) {
        this.mLockPatternUtils = l;
    }

    @Override
    public void onPageSwitching(View newPage, int newPageIndex) {
        if (this.mViewStateManager != null) {
            this.mViewStateManager.onPageSwitching(newPage, newPageIndex);
        }
    }

    @Override
    public void onPageSwitched(View newPage, int newPageIndex) {
        boolean showingClock = false;
        if (newPage instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) newPage;
            if (vg.getChildAt(0) instanceof KeyguardStatusView) {
                showingClock = true;
            }
        }
        if (newPage != null && findClockInHierarchy(newPage) == 3) {
            showingClock = true;
        }
        if (showingClock) {
            setSystemUiVisibility(getSystemUiVisibility() | 8388608);
        } else {
            setSystemUiVisibility(getSystemUiVisibility() & (-8388609));
        }
        if (this.mPage != newPageIndex) {
            int oldPageIndex = this.mPage;
            this.mPage = newPageIndex;
            userActivity();
            KeyguardWidgetFrame oldWidgetPage = getWidgetPageAt(oldPageIndex);
            if (oldWidgetPage != null) {
                oldWidgetPage.onActive(false);
            }
            KeyguardWidgetFrame newWidgetPage = getWidgetPageAt(newPageIndex);
            if (newWidgetPage != null) {
                newWidgetPage.onActive(true);
                newWidgetPage.setImportantForAccessibility(1);
                newWidgetPage.requestAccessibilityFocus();
            }
            if (this.mParent != null && AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(4096);
                onInitializeAccessibilityEvent(event);
                onPopulateAccessibilityEvent(event);
                this.mParent.requestSendAccessibilityEvent(this, event);
            }
        }
        if (this.mViewStateManager != null) {
            this.mViewStateManager.onPageSwitched(newPage, newPageIndex);
        }
    }

    @Override
    public void onPageBeginWarp() {
        showOutlinesAndSidePages();
        this.mViewStateManager.onPageBeginWarp();
    }

    @Override
    public void onPageEndWarp() {
        int duration = getPageWarpIndex() == getNextPage() ? 0 : -1;
        animateOutlinesAndSidePages(false, duration);
        this.mViewStateManager.onPageEndWarp();
    }

    @Override
    public void sendAccessibilityEvent(int eventType) {
        if (eventType != 4096 || isPageMoving()) {
            super.sendAccessibilityEvent(eventType);
        }
    }

    private void updateWidgetFramesImportantForAccessibility() {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            KeyguardWidgetFrame frame = getWidgetPageAt(i);
            updateWidgetFrameImportantForAccessibility(frame);
        }
    }

    private void updateWidgetFrameImportantForAccessibility(KeyguardWidgetFrame frame) {
        if (frame.getContentAlpha() <= 0.0f) {
            frame.setImportantForAccessibility(2);
        } else {
            frame.setImportantForAccessibility(1);
        }
    }

    private void userActivity() {
        if (this.mCallbacks != null) {
            this.mCallbacks.onUserActivityTimeoutChanged();
            this.mCallbacks.userActivity();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return captureUserInteraction(ev) || super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return captureUserInteraction(ev) || super.onInterceptTouchEvent(ev);
    }

    private boolean captureUserInteraction(MotionEvent ev) {
        KeyguardWidgetFrame currentWidgetPage = getWidgetPageAt(getCurrentPage());
        return currentWidgetPage != null && currentWidgetPage.onUserInteraction(ev);
    }

    public void showPagingFeedback() {
    }

    public long getUserActivityTimeout() {
        View page = getPageAt(this.mPage);
        if (page instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) page;
            View view = vg.getChildAt(0);
            if (!(view instanceof KeyguardStatusView) && !(view instanceof KeyguardMultiUserSelectorView)) {
                return 30000L;
            }
        }
        return -1L;
    }

    public void setCallbacks(Callbacks callbacks) {
        this.mCallbacks = callbacks;
    }

    public void addWidget(View widget) {
        addWidget(widget, -1);
    }

    @Override
    public void onRemoveView(View v, boolean deletePermanently) {
        final int appWidgetId = ((KeyguardWidgetFrame) v).getContentAppWidgetId();
        if (this.mCallbacks != null) {
            this.mCallbacks.onRemoveView(v, deletePermanently);
        }
        this.mBackgroundWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                KeyguardWidgetPager.this.mLockPatternUtils.removeAppWidget(appWidgetId);
            }
        });
    }

    @Override
    public void onRemoveViewAnimationCompleted() {
        if (this.mCallbacks != null) {
            this.mCallbacks.onRemoveViewAnimationCompleted();
        }
    }

    @Override
    public void onAddView(View v, final int index) {
        final int appWidgetId = ((KeyguardWidgetFrame) v).getContentAppWidgetId();
        final int[] pagesRange = new int[this.mTempVisiblePagesRange.length];
        getVisiblePages(pagesRange);
        boundByReorderablePages(true, pagesRange);
        if (this.mCallbacks != null) {
            this.mCallbacks.onAddView(v);
        }
        this.mBackgroundWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                KeyguardWidgetPager.this.mLockPatternUtils.addAppWidget(appWidgetId, index - pagesRange[0]);
            }
        });
    }

    public void addWidget(View widget, int pageIndex) {
        KeyguardWidgetFrame frame;
        if (!(widget instanceof KeyguardWidgetFrame)) {
            frame = new KeyguardWidgetFrame(getContext());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
            lp.gravity = 48;
            widget.setPadding(0, 0, 0, 0);
            frame.addView(widget, lp);
            if (widget instanceof AppWidgetHostView) {
                AppWidgetHostView awhv = (AppWidgetHostView) widget;
                AppWidgetProviderInfo info = awhv.getAppWidgetInfo();
                if ((info.resizeMode & 2) != 0) {
                    frame.setWidgetLockedSmall(false);
                } else {
                    frame.setWidgetLockedSmall(true);
                    if (this.mCenterSmallWidgetsVertically) {
                        lp.gravity = 17;
                    }
                }
            }
        } else {
            frame = (KeyguardWidgetFrame) widget;
        }
        ViewGroup.LayoutParams pageLp = new ViewGroup.LayoutParams(-1, -1);
        frame.setOnLongClickListener(this);
        frame.setWorkerHandler(this.mBackgroundWorkerHandler);
        if (pageIndex == -1) {
            addView(frame, pageLp);
        } else {
            addView(frame, pageIndex, pageLp);
        }
        View content = widget == frame ? frame.getContent() : widget;
        if (content != null) {
            CharSequence contentDescription = this.mContext.getString(R.string.keyguard_accessibility_widget, content.getContentDescription());
            frame.setContentDescription(contentDescription);
        }
        updateWidgetFrameImportantForAccessibility(frame);
    }

    @Override
    public void addView(View child, int index) {
        enforceKeyguardWidgetFrame(child);
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int width, int height) {
        enforceKeyguardWidgetFrame(child);
        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        enforceKeyguardWidgetFrame(child);
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        enforceKeyguardWidgetFrame(child);
        super.addView(child, index, params);
    }

    private void enforceKeyguardWidgetFrame(View child) {
        if (!(child instanceof KeyguardWidgetFrame)) {
            throw new IllegalArgumentException("KeyguardWidgetPager children must be KeyguardWidgetFrames");
        }
    }

    public KeyguardWidgetFrame getWidgetPageAt(int index) {
        return (KeyguardWidgetFrame) getChildAt(index);
    }

    @Override
    protected void onUnhandledTap(MotionEvent ev) {
        showPagingFeedback();
    }

    @Override
    protected void onPageBeginMoving() {
        if (this.mViewStateManager != null) {
            this.mViewStateManager.onPageBeginMoving();
        }
        if (!isReordering(false)) {
            showOutlinesAndSidePages();
        }
        userActivity();
    }

    @Override
    protected void onPageEndMoving() {
        if (this.mViewStateManager != null) {
            this.mViewStateManager.onPageEndMoving();
        }
        if (!isReordering(false)) {
            hideOutlinesAndSidePages();
        }
    }

    protected void enablePageContentLayers() {
        int children = getChildCount();
        for (int i = 0; i < children; i++) {
            getWidgetPageAt(i).enableHardwareLayersForContent();
        }
    }

    protected void disablePageContentLayers() {
        int children = getChildCount();
        for (int i = 0; i < children; i++) {
            getWidgetPageAt(i).disableHardwareLayersForContent();
        }
    }

    static class ZInterpolator implements TimeInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            this.focalLength = foc;
        }

        @Override
        public float getInterpolation(float input) {
            return (1.0f - (this.focalLength / (this.focalLength + input))) / (1.0f - (this.focalLength / (this.focalLength + 1.0f)));
        }
    }

    @Override
    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    private void updatePageAlphaValues(int screenCenter) {
    }

    public float getAlphaForPage(int screenCenter, int index, boolean showSidePages) {
        return isWarping() ? index == getPageWarpIndex() ? 1.0f : 0.0f : (showSidePages || index == this.mCurrentPage) ? 1.0f : 0.0f;
    }

    public float getOutlineAlphaForPage(int screenCenter, int index, boolean showSidePages) {
        if (showSidePages) {
            return getAlphaForPage(screenCenter, index, showSidePages) * 0.6f;
        }
        return 0.0f;
    }

    protected boolean isOverScrollChild(int index, float scrollProgress) {
        boolean isInOverscroll = this.mOverScrollX < 0 || this.mOverScrollX > this.mMaxScrollX;
        if (isInOverscroll) {
            if (index == 0 && scrollProgress < 0.0f) {
                return true;
            }
            if (index == getChildCount() - 1 && scrollProgress > 0.0f) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void screenScrolled(int screenCenter) {
        this.mScreenCenter = screenCenter;
        updatePageAlphaValues(screenCenter);
        for (int i = 0; i < getChildCount(); i++) {
            KeyguardWidgetFrame v = getWidgetPageAt(i);
            if (v != this.mDragView && v != null) {
                float scrollProgress = getScrollProgress(screenCenter, v, i);
                v.setCameraDistance(this.mDensity * CAMERA_DISTANCE);
                if (isOverScrollChild(i, scrollProgress)) {
                    float pivotX = v.getMeasuredWidth() / 2;
                    float pivotY = v.getMeasuredHeight() / 2;
                    v.setPivotX(pivotX);
                    v.setPivotY(pivotY);
                    v.setRotationY((-OVERSCROLL_MAX_ROTATION) * scrollProgress);
                    v.setOverScrollAmount(Math.abs(scrollProgress), scrollProgress < 0.0f);
                } else {
                    v.setRotationY(0.0f);
                    v.setOverScrollAmount(0.0f, false);
                }
                float alpha = v.getAlpha();
                if (alpha == 0.0f) {
                    v.setVisibility(4);
                } else if (v.getVisibility() != 0) {
                    v.setVisibility(0);
                }
            }
        }
    }

    public boolean isWidgetPage(int pageIndex) {
        View v;
        if (pageIndex < 0 || pageIndex >= getChildCount() || (v = getChildAt(pageIndex)) == null || !(v instanceof KeyguardWidgetFrame)) {
            return false;
        }
        KeyguardWidgetFrame kwf = (KeyguardWidgetFrame) v;
        return kwf.getContentAppWidgetId() != 0;
    }

    @Override
    void boundByReorderablePages(boolean isReordering, int[] range) {
        if (isReordering) {
            while (range[1] >= range[0] && !isWidgetPage(range[1])) {
                range[1] = range[1] - 1;
            }
            while (range[0] <= range[1] && !isWidgetPage(range[0])) {
                range[0] = range[0] + 1;
            }
        }
    }

    protected void reorderStarting() {
        showOutlinesAndSidePages();
    }

    @Override
    protected void onStartReordering() {
        super.onStartReordering();
        enablePageContentLayers();
        reorderStarting();
    }

    @Override
    protected void onEndReordering() {
        super.onEndReordering();
        hideOutlinesAndSidePages();
    }

    void showOutlinesAndSidePages() {
        animateOutlinesAndSidePages(true);
    }

    void hideOutlinesAndSidePages() {
        animateOutlinesAndSidePages(false);
    }

    void updateChildrenContentAlpha(float sidePageAlpha) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            KeyguardWidgetFrame child = getWidgetPageAt(i);
            if (i != this.mCurrentPage) {
                child.setBackgroundAlpha(sidePageAlpha);
                child.setContentAlpha(0.0f);
            } else {
                child.setBackgroundAlpha(0.0f);
                child.setContentAlpha(1.0f);
            }
        }
    }

    @Override
    void setCurrentPage(int currentPage) {
        super.setCurrentPage(currentPage);
        updateChildrenContentAlpha(0.0f);
        updateWidgetFramesImportantForAccessibility();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mHasMeasure = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.mLastWidthMeasureSpec = widthMeasureSpec;
        this.mLastHeightMeasureSpec = heightMeasureSpec;
        View parent = (View) getParent();
        if (parent.getParent() instanceof SlidingChallengeLayout) {
            SlidingChallengeLayout scl = (SlidingChallengeLayout) parent.getParent();
            int top = scl.getMaxChallengeTop();
            int maxChallengeTop = top - getPaddingTop();
            boolean challengeShowing = scl.isChallengeShowing();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                KeyguardWidgetFrame frame = getWidgetPageAt(i);
                frame.setMaxChallengeTop(maxChallengeTop);
                if (challengeShowing && i == this.mCurrentPage && !this.mHasMeasure) {
                    frame.shrinkWidget(true);
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mHasMeasure = true;
    }

    void animateOutlinesAndSidePages(boolean show) {
        animateOutlinesAndSidePages(show, -1);
    }

    public void setWidgetToResetOnPageFadeOut(int widget) {
        this.mWidgetToResetAfterFadeOut = widget;
    }

    public int getWidgetToResetOnPageFadeOut() {
        return this.mWidgetToResetAfterFadeOut;
    }

    void animateOutlinesAndSidePages(final boolean show, int duration) {
        float finalContentAlpha;
        if (this.mChildrenOutlineFadeAnimation != null) {
            this.mChildrenOutlineFadeAnimation.cancel();
            this.mChildrenOutlineFadeAnimation = null;
        }
        int count = getChildCount();
        ArrayList<Animator> anims = new ArrayList<>();
        if (duration == -1) {
            duration = show ? 100 : 375;
        }
        int curPage = getNextPage();
        for (int i = 0; i < count; i++) {
            if (show) {
                finalContentAlpha = getAlphaForPage(this.mScreenCenter, i, true);
            } else if (!show && i == curPage) {
                finalContentAlpha = 1.0f;
            } else {
                finalContentAlpha = 0.0f;
            }
            KeyguardWidgetFrame child = getWidgetPageAt(i);
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("contentAlpha", finalContentAlpha);
            ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(child, alpha);
            anims.add(a);
            float finalOutlineAlpha = show ? getOutlineAlphaForPage(this.mScreenCenter, i, true) : 0.0f;
            child.fadeFrame(this, show, finalOutlineAlpha, duration);
        }
        this.mChildrenOutlineFadeAnimation = new AnimatorSet();
        this.mChildrenOutlineFadeAnimation.playTogether(anims);
        this.mChildrenOutlineFadeAnimation.setDuration(duration);
        this.mChildrenOutlineFadeAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (show) {
                    KeyguardWidgetPager.this.enablePageContentLayers();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show) {
                    KeyguardWidgetPager.this.disablePageContentLayers();
                    KeyguardWidgetFrame frame = KeyguardWidgetPager.this.getWidgetPageAt(KeyguardWidgetPager.this.mWidgetToResetAfterFadeOut);
                    if (frame != null && (frame != KeyguardWidgetPager.this.getWidgetPageAt(KeyguardWidgetPager.this.mCurrentPage) || !KeyguardWidgetPager.this.mViewStateManager.isChallengeOverlapping())) {
                        frame.resetSize();
                    }
                    KeyguardWidgetPager.this.mWidgetToResetAfterFadeOut = -1;
                    KeyguardWidgetPager.this.mShowingInitialHints = false;
                }
                KeyguardWidgetPager.this.updateWidgetFramesImportantForAccessibility();
            }
        });
        this.mChildrenOutlineFadeAnimation.start();
    }

    @Override
    public boolean onLongClick(View v) {
        boolean isChallengeOverlapping = this.mViewStateManager.isChallengeShowing() && this.mViewStateManager.isChallengeOverlapping();
        return !isChallengeOverlapping && startReordering();
    }

    public void removeWidget(View view) {
        if (view instanceof KeyguardWidgetFrame) {
            removeView(view);
            return;
        }
        int pos = getWidgetPageIndex(view);
        if (pos != -1) {
            KeyguardWidgetFrame frame = (KeyguardWidgetFrame) getChildAt(pos);
            frame.removeView(view);
            removeView(frame);
            return;
        }
        Slog.w("KeyguardWidgetPager", "removeWidget() can't find:" + view);
    }

    public int getWidgetPageIndex(View view) {
        return view instanceof KeyguardWidgetFrame ? indexOfChild(view) : indexOfChild((KeyguardWidgetFrame) view.getParent());
    }

    @Override
    protected void setPageHoveringOverDeleteDropTarget(int viewIndex, boolean isHovering) {
        KeyguardWidgetFrame child = getWidgetPageAt(viewIndex);
        child.setIsHoveringOverDeleteDropTarget(isHovering);
    }

    @Override
    public void onBouncerStateChanged(boolean bouncerActive) {
        if (bouncerActive) {
            zoomOutToBouncer();
        } else {
            zoomInFromBouncer();
        }
    }

    void setBouncerAnimationDuration(int duration) {
        this.mBouncerZoomInOutDuration = duration;
    }

    void zoomInFromBouncer() {
        if (this.mZoomInOutAnim != null && this.mZoomInOutAnim.isRunning()) {
            this.mZoomInOutAnim.cancel();
        }
        View currentPage = getPageAt(getCurrentPage());
        if (currentPage.getScaleX() < 1.0f || currentPage.getScaleY() < 1.0f) {
            this.mZoomInOutAnim = new AnimatorSet();
            this.mZoomInOutAnim.playTogether(ObjectAnimator.ofFloat(currentPage, "scaleX", 1.0f), ObjectAnimator.ofFloat(currentPage, "scaleY", 1.0f));
            this.mZoomInOutAnim.setDuration(this.mBouncerZoomInOutDuration);
            this.mZoomInOutAnim.setInterpolator(new DecelerateInterpolator(1.5f));
            this.mZoomInOutAnim.start();
        }
        if (currentPage instanceof KeyguardWidgetFrame) {
            ((KeyguardWidgetFrame) currentPage).onBouncerShowing(false);
        }
    }

    void zoomOutToBouncer() {
        if (this.mZoomInOutAnim != null && this.mZoomInOutAnim.isRunning()) {
            this.mZoomInOutAnim.cancel();
        }
        int curPage = getCurrentPage();
        View currentPage = getPageAt(curPage);
        if (shouldSetTopAlignedPivotForWidget(curPage)) {
            currentPage.setPivotY(0.0f);
            currentPage.setPivotX(0.0f);
            currentPage.setPivotX(currentPage.getMeasuredWidth() / 2);
        }
        if (currentPage != null && currentPage.getScaleX() >= 1.0f && currentPage.getScaleY() >= 1.0f) {
            this.mZoomInOutAnim = new AnimatorSet();
            this.mZoomInOutAnim.playTogether(ObjectAnimator.ofFloat(currentPage, "scaleX", this.BOUNCER_SCALE_FACTOR), ObjectAnimator.ofFloat(currentPage, "scaleY", this.BOUNCER_SCALE_FACTOR));
            this.mZoomInOutAnim.setDuration(this.mBouncerZoomInOutDuration);
            this.mZoomInOutAnim.setInterpolator(new DecelerateInterpolator(1.5f));
            this.mZoomInOutAnim.start();
        }
        if (currentPage instanceof KeyguardWidgetFrame) {
            ((KeyguardWidgetFrame) currentPage).onBouncerShowing(true);
        }
    }

    void setAddWidgetEnabled(boolean enabled) {
        View addWidget;
        if (this.mAddWidgetView != null && enabled) {
            addView(this.mAddWidgetView, 0);
            measure(this.mLastWidthMeasureSpec, this.mLastHeightMeasureSpec);
            setCurrentPage(this.mCurrentPage + 1);
            this.mAddWidgetView = null;
            return;
        }
        if (this.mAddWidgetView == null && !enabled && (addWidget = findViewById(R.id.keyguard_add_widget)) != null) {
            this.mAddWidgetView = addWidget;
            removeView(addWidget);
        }
    }

    boolean isCameraPage(int pageIndex) {
        View v = getChildAt(pageIndex);
        return v != null && (v instanceof CameraWidgetFrame);
    }

    @Override
    protected boolean shouldSetTopAlignedPivotForWidget(int childIndex) {
        return !isCameraPage(childIndex) && super.shouldSetTopAlignedPivotForWidget(childIndex);
    }

    private static int findClockInHierarchy(View view) {
        if (view instanceof TextClock) {
            return getClockFlags((TextClock) view);
        }
        if (view instanceof ViewGroup) {
            int flags = 0;
            ViewGroup group = (ViewGroup) view;
            int size = group.getChildCount();
            for (int i = 0; i < size; i++) {
                flags |= findClockInHierarchy(group.getChildAt(i));
            }
            return flags;
        }
        return 0;
    }

    private static int getClockFlags(TextClock clock) {
        int flags = 0;
        String timeZone = clock.getTimeZone();
        if (timeZone != null && !TimeZone.getDefault().equals(TimeZone.getTimeZone(timeZone))) {
            return 0;
        }
        CharSequence format = clock.getFormat();
        char hour = clock.is24HourModeEnabled() ? 'k' : 'h';
        if (DateFormat.hasDesignator(format, hour)) {
            flags = 0 | 1;
        }
        if (DateFormat.hasDesignator(format, 'm')) {
            flags |= 2;
        }
        return flags;
    }
}
