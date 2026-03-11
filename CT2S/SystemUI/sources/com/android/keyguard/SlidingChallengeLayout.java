package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatProperty;
import android.util.Log;
import android.util.Property;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import com.android.keyguard.ChallengeLayout;

public class SlidingChallengeLayout extends ViewGroup implements ChallengeLayout {
    private int mActivePointerId;
    private boolean mBlockDrag;
    private ChallengeLayout.OnBouncerStateChangedListener mBouncerListener;
    private int mChallengeBottomBound;
    private boolean mChallengeInteractiveExternal;
    private boolean mChallengeInteractiveInternal;
    private float mChallengeOffset;
    private boolean mChallengeShowing;
    private boolean mChallengeShowingTargetState;
    private KeyguardSecurityContainer mChallengeView;
    private DisplayMetrics mDisplayMetrics;
    private int mDragHandleClosedAbove;
    private int mDragHandleClosedBelow;
    private int mDragHandleEdgeSlop;
    private int mDragHandleOpenAbove;
    private int mDragHandleOpenBelow;
    private boolean mDragging;
    private boolean mEdgeCaptured;
    private boolean mEnableChallengeDragging;
    private final Runnable mEndScrollRunnable;
    private final View.OnClickListener mExpandChallengeClickListener;
    private View mExpandChallengeView;
    private ObjectAnimator mFader;
    float mFrameAnimationTarget;
    private int mGestureStartChallengeBottom;
    private float mGestureStartX;
    private float mGestureStartY;
    float mHandleAlpha;
    private ObjectAnimator mHandleAnimation;
    private boolean mHasGlowpad;
    private boolean mHasLayout;
    private final Rect mInsets;
    private boolean mIsBouncing;
    private int mMaxVelocity;
    private int mMinVelocity;
    private final View.OnClickListener mScrimClickListener;
    private View mScrimView;
    private OnChallengeScrolledListener mScrollListener;
    private int mScrollState;
    private final Scroller mScroller;
    private int mTouchSlop;
    private int mTouchSlopSquare;
    private VelocityTracker mVelocityTracker;
    private boolean mWasChallengeShowing;
    private View mWidgetsView;
    static final Property<SlidingChallengeLayout, Float> HANDLE_ALPHA = new FloatProperty<SlidingChallengeLayout>("handleAlpha") {
        @Override
        public void setValue(SlidingChallengeLayout view, float value) {
            view.mHandleAlpha = value;
            view.invalidate();
        }

        @Override
        public Float get(SlidingChallengeLayout view) {
            return Float.valueOf(view.mHandleAlpha);
        }
    };
    private static final Interpolator sMotionInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * t2 * t2 * t2) + 1.0f;
        }
    };
    private static final Interpolator sHandleFadeInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            return t * t;
        }
    };

    public interface OnChallengeScrolledListener {
        void onScrollPositionChanged(float f, int i);

        void onScrollStateChanged(int i);
    }

    public SlidingChallengeLayout(Context context) {
        this(context, null);
    }

    public SlidingChallengeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingChallengeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mChallengeOffset = 1.0f;
        this.mChallengeShowing = true;
        this.mChallengeShowingTargetState = true;
        this.mWasChallengeShowing = true;
        this.mIsBouncing = false;
        this.mActivePointerId = -1;
        this.mFrameAnimationTarget = Float.MIN_VALUE;
        this.mInsets = new Rect();
        this.mChallengeInteractiveExternal = true;
        this.mChallengeInteractiveInternal = true;
        this.mEndScrollRunnable = new Runnable() {
            @Override
            public void run() {
                SlidingChallengeLayout.this.completeChallengeScroll();
            }
        };
        this.mScrimClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SlidingChallengeLayout.this.hideBouncer();
            }
        };
        this.mExpandChallengeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SlidingChallengeLayout.this.isChallengeShowing()) {
                    SlidingChallengeLayout.this.showChallenge(true);
                }
            }
        };
        this.mScroller = new Scroller(context, sMotionInterpolator);
        ViewConfiguration vc = ViewConfiguration.get(context);
        this.mMinVelocity = vc.getScaledMinimumFlingVelocity();
        this.mMaxVelocity = vc.getScaledMaximumFlingVelocity();
        Resources res = getResources();
        this.mDragHandleEdgeSlop = res.getDimensionPixelSize(R.dimen.kg_edge_swipe_region_size);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mTouchSlopSquare = this.mTouchSlop * this.mTouchSlop;
        this.mDisplayMetrics = res.getDisplayMetrics();
        float density = this.mDisplayMetrics.density;
        this.mDragHandleClosedAbove = (int) ((8.0f * density) + 0.5f);
        this.mDragHandleClosedBelow = (int) ((0.0f * density) + 0.5f);
        this.mDragHandleOpenAbove = (int) ((8.0f * density) + 0.5f);
        this.mDragHandleOpenBelow = (int) ((0.0f * density) + 0.5f);
        this.mChallengeBottomBound = res.getDimensionPixelSize(R.dimen.kg_widget_pager_bottom_padding);
        setWillNotDraw(false);
        setSystemUiVisibility(768);
    }

    public void setEnableChallengeDragging(boolean enabled) {
        this.mEnableChallengeDragging = enabled;
    }

    public void setInsets(Rect insets) {
        this.mInsets.set(insets);
    }

    public void setHandleAlpha(float alpha) {
        if (this.mExpandChallengeView != null) {
            this.mExpandChallengeView.setAlpha(alpha);
        }
    }

    public void setChallengeInteractive(boolean interactive) {
        this.mChallengeInteractiveExternal = interactive;
        if (this.mExpandChallengeView != null) {
            this.mExpandChallengeView.setEnabled(interactive);
        }
    }

    void animateHandle(boolean visible) {
        if (this.mHandleAnimation != null) {
            this.mHandleAnimation.cancel();
            this.mHandleAnimation = null;
        }
        float targetAlpha = visible ? 1.0f : 0.0f;
        if (targetAlpha != this.mHandleAlpha) {
            this.mHandleAnimation = ObjectAnimator.ofFloat(this, HANDLE_ALPHA, targetAlpha);
            this.mHandleAnimation.setInterpolator(sHandleFadeInterpolator);
            this.mHandleAnimation.setDuration(250L);
            this.mHandleAnimation.start();
        }
    }

    private void sendInitialListenerUpdates() {
        if (this.mScrollListener != null) {
            int challengeTop = this.mChallengeView != null ? this.mChallengeView.getTop() : 0;
            this.mScrollListener.onScrollPositionChanged(this.mChallengeOffset, challengeTop);
            this.mScrollListener.onScrollStateChanged(this.mScrollState);
        }
    }

    public void setOnChallengeScrolledListener(OnChallengeScrolledListener listener) {
        this.mScrollListener = listener;
        if (this.mHasLayout) {
            sendInitialListenerUpdates();
        }
    }

    @Override
    public void setOnBouncerStateChangedListener(ChallengeLayout.OnBouncerStateChangedListener listener) {
        this.mBouncerListener = listener;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mHasLayout = false;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.mEndScrollRunnable);
        this.mHasLayout = false;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (this.mIsBouncing && child != this.mChallengeView) {
            hideBouncer();
        }
        super.requestChildFocus(child, focused);
    }

    float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((float) (((double) (f - 0.5f)) * 0.4712389167638204d));
    }

    void setScrollState(int state) {
        if (this.mScrollState != state) {
            this.mScrollState = state;
            animateHandle(state == 0 && !this.mChallengeShowing);
            if (this.mScrollListener != null) {
                this.mScrollListener.onScrollStateChanged(state);
            }
        }
    }

    void completeChallengeScroll() {
        setChallengeShowing(this.mChallengeShowingTargetState);
        this.mChallengeOffset = this.mChallengeShowing ? 1.0f : 0.0f;
        setScrollState(0);
        this.mChallengeInteractiveInternal = true;
        this.mChallengeView.setLayerType(0, null);
    }

    void setScrimView(View scrim) {
        if (this.mScrimView != null) {
            this.mScrimView.setOnClickListener(null);
        }
        this.mScrimView = scrim;
        if (this.mScrimView != null) {
            this.mScrimView.setVisibility(this.mIsBouncing ? 0 : 8);
            this.mScrimView.setFocusable(true);
            this.mScrimView.setOnClickListener(this.mScrimClickListener);
        }
    }

    void animateChallengeTo(int y, int velocity) {
        int duration;
        if (this.mChallengeView != null) {
            cancelTransitionsInProgress();
            this.mChallengeInteractiveInternal = false;
            enableHardwareLayerForChallengeView();
            int sy = this.mChallengeView.getBottom();
            int dy = y - sy;
            if (dy == 0) {
                completeChallengeScroll();
                return;
            }
            setScrollState(2);
            int childHeight = this.mChallengeView.getHeight();
            int halfHeight = childHeight / 2;
            float distanceRatio = Math.min(1.0f, (Math.abs(dy) * 1.0f) / childHeight);
            float distance = halfHeight + (halfHeight * distanceInfluenceForSnapDuration(distanceRatio));
            int velocity2 = Math.abs(velocity);
            if (velocity2 > 0) {
                duration = Math.round(1000.0f * Math.abs(distance / velocity2)) * 4;
            } else {
                float childDelta = Math.abs(dy) / childHeight;
                duration = (int) ((childDelta + 1.0f) * 100.0f);
            }
            this.mScroller.startScroll(0, sy, 0, dy, Math.min(duration, 600));
            postInvalidateOnAnimation();
        }
    }

    private void setChallengeShowing(boolean showChallenge) {
        if (this.mChallengeShowing != showChallenge) {
            this.mChallengeShowing = showChallenge;
            if (this.mExpandChallengeView != null && this.mChallengeView != null) {
                if (this.mChallengeShowing) {
                    this.mExpandChallengeView.setVisibility(4);
                    this.mChallengeView.setVisibility(0);
                    if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                        this.mChallengeView.requestAccessibilityFocus();
                        this.mChallengeView.announceForAccessibility(this.mContext.getString(R.string.keyguard_accessibility_unlock_area_expanded));
                        return;
                    }
                    return;
                }
                this.mExpandChallengeView.setVisibility(0);
                this.mChallengeView.setVisibility(4);
                if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
                    this.mExpandChallengeView.requestAccessibilityFocus();
                    this.mChallengeView.announceForAccessibility(this.mContext.getString(R.string.keyguard_accessibility_unlock_area_collapsed));
                }
            }
        }
    }

    @Override
    public boolean isChallengeShowing() {
        return this.mChallengeShowing;
    }

    @Override
    public boolean isChallengeOverlapping() {
        return this.mChallengeShowing;
    }

    @Override
    public boolean isBouncing() {
        return this.mIsBouncing;
    }

    @Override
    public int getBouncerAnimationDuration() {
        return 250;
    }

    @Override
    public void showBouncer() {
        if (!this.mIsBouncing) {
            setSystemUiVisibility(getSystemUiVisibility() | 33554432);
            this.mWasChallengeShowing = this.mChallengeShowing;
            this.mIsBouncing = true;
            showChallenge(true);
            if (this.mScrimView != null) {
                Animator anim = ObjectAnimator.ofFloat(this.mScrimView, "alpha", 1.0f);
                anim.setDuration(250L);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        SlidingChallengeLayout.this.mScrimView.setVisibility(0);
                    }
                });
                anim.start();
            }
            if (this.mChallengeView != null) {
                this.mChallengeView.showBouncer(250);
            }
            if (this.mBouncerListener != null) {
                this.mBouncerListener.onBouncerStateChanged(true);
            }
        }
    }

    public void hideBouncer() {
        if (this.mIsBouncing) {
            setSystemUiVisibility(getSystemUiVisibility() & (-33554433));
            if (!this.mWasChallengeShowing) {
                showChallenge(false);
            }
            this.mIsBouncing = false;
            if (this.mScrimView != null) {
                Animator anim = ObjectAnimator.ofFloat(this.mScrimView, "alpha", 0.0f);
                anim.setDuration(250L);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        SlidingChallengeLayout.this.mScrimView.setVisibility(8);
                    }
                });
                anim.start();
            }
            if (this.mChallengeView != null) {
                this.mChallengeView.hideBouncer(250);
            }
            if (this.mBouncerListener != null) {
                this.mBouncerListener.onBouncerStateChanged(false);
            }
        }
    }

    private int getChallengeMargin(boolean expanded) {
        if (expanded && this.mHasGlowpad) {
            return 0;
        }
        return this.mDragHandleEdgeSlop;
    }

    private float getChallengeAlpha() {
        float x = this.mChallengeOffset - 1.0f;
        return (x * x * x) + 1.0f;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean allowIntercept) {
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getActionMasked();
        switch (action) {
            case 0:
                this.mGestureStartX = ev.getX();
                this.mGestureStartY = ev.getY();
                this.mBlockDrag = false;
                break;
            case 1:
            case 3:
                resetTouch();
                break;
            case 2:
                int count = ev.getPointerCount();
                for (int i = 0; i < count; i++) {
                    float x = ev.getX(i);
                    float y = ev.getY(i);
                    if (!this.mIsBouncing && this.mActivePointerId == -1 && ((crossedDragHandle(x, y, this.mGestureStartY) && shouldEnableChallengeDragging()) || (isInChallengeView(x, y) && this.mScrollState == 2))) {
                        this.mActivePointerId = ev.getPointerId(i);
                        this.mGestureStartX = x;
                        this.mGestureStartY = y;
                        this.mGestureStartChallengeBottom = getChallengeBottom();
                        this.mDragging = true;
                        enableHardwareLayerForChallengeView();
                    } else if (this.mChallengeShowing && isInChallengeView(x, y) && shouldEnableChallengeDragging()) {
                        this.mBlockDrag = true;
                    }
                }
                break;
        }
        if (this.mBlockDrag || isChallengeInteractionBlocked()) {
            this.mActivePointerId = -1;
            this.mDragging = false;
        }
        return this.mDragging;
    }

    private boolean shouldEnableChallengeDragging() {
        return this.mEnableChallengeDragging || !this.mChallengeShowing;
    }

    private boolean isChallengeInteractionBlocked() {
        return (this.mChallengeInteractiveExternal && this.mChallengeInteractiveInternal) ? false : true;
    }

    private void resetTouch() {
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
        this.mActivePointerId = -1;
        this.mBlockDrag = false;
        this.mDragging = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getActionMasked();
        switch (action) {
            case 0:
                this.mBlockDrag = false;
                this.mGestureStartX = ev.getX();
                this.mGestureStartY = ev.getY();
                return true;
            case 1:
                if (this.mDragging && !isChallengeInteractionBlocked()) {
                    this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaxVelocity);
                    showChallenge((int) this.mVelocityTracker.getYVelocity(this.mActivePointerId));
                }
                resetTouch();
                return true;
            case 2:
                if (!this.mDragging && !this.mBlockDrag && !this.mIsBouncing) {
                    int count = ev.getPointerCount();
                    int i = 0;
                    while (true) {
                        if (i < count) {
                            float x = ev.getX(i);
                            float y = ev.getY(i);
                            if ((!isInDragHandle(x, y) && !crossedDragHandle(x, y, this.mGestureStartY) && (!isInChallengeView(x, y) || this.mScrollState != 2)) || this.mActivePointerId != -1 || isChallengeInteractionBlocked()) {
                                i++;
                            } else {
                                this.mGestureStartX = x;
                                this.mGestureStartY = y;
                                this.mActivePointerId = ev.getPointerId(i);
                                this.mGestureStartChallengeBottom = getChallengeBottom();
                                this.mDragging = true;
                                enableHardwareLayerForChallengeView();
                            }
                        }
                    }
                }
                if (this.mDragging) {
                    setScrollState(1);
                    int index = ev.findPointerIndex(this.mActivePointerId);
                    if (index < 0) {
                        resetTouch();
                        showChallenge(0);
                    } else {
                        float pos = Math.min(ev.getY(index) - this.mGestureStartY, getLayoutBottom() - this.mChallengeBottomBound);
                        moveChallengeTo(this.mGestureStartChallengeBottom + ((int) pos));
                    }
                }
                return true;
            case 3:
                if (this.mDragging && !isChallengeInteractionBlocked()) {
                    showChallenge(0);
                }
                resetTouch();
                return true;
            case 4:
            case 5:
            default:
                return true;
            case 6:
                if (this.mActivePointerId == ev.getPointerId(ev.getActionIndex())) {
                }
                return true;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        boolean handled = false;
        if (action == 0) {
            this.mEdgeCaptured = false;
        }
        if (this.mWidgetsView != null && !this.mIsBouncing && (this.mEdgeCaptured || isEdgeSwipeBeginEvent(ev))) {
            handled = this.mEdgeCaptured | this.mWidgetsView.dispatchTouchEvent(ev);
            this.mEdgeCaptured = handled;
        }
        if (!handled && !this.mEdgeCaptured) {
            handled = super.dispatchTouchEvent(ev);
        }
        if (action == 1 || action == 3) {
            this.mEdgeCaptured = false;
        }
        return handled;
    }

    private boolean isEdgeSwipeBeginEvent(MotionEvent ev) {
        if (ev.getActionMasked() != 0) {
            return false;
        }
        float x = ev.getX();
        return x < ((float) this.mDragHandleEdgeSlop) || x >= ((float) (getWidth() - this.mDragHandleEdgeSlop));
    }

    private int getDragHandleSizeAbove() {
        return isChallengeShowing() ? this.mDragHandleOpenAbove : this.mDragHandleClosedAbove;
    }

    private int getDragHandleSizeBelow() {
        return isChallengeShowing() ? this.mDragHandleOpenBelow : this.mDragHandleClosedBelow;
    }

    private boolean isInChallengeView(float x, float y) {
        return isPointInView(x, y, this.mChallengeView);
    }

    private boolean isInDragHandle(float x, float y) {
        return isPointInView(x, y, this.mExpandChallengeView);
    }

    private boolean isPointInView(float x, float y, View view) {
        return view != null && x >= ((float) view.getLeft()) && y >= ((float) view.getTop()) && x < ((float) view.getRight()) && y < ((float) view.getBottom());
    }

    private boolean crossedDragHandle(float x, float y, float initialY) {
        int challengeTop = this.mChallengeView.getTop();
        boolean horizOk = x >= 0.0f && x < ((float) getWidth());
        boolean vertOk = this.mChallengeShowing ? initialY < ((float) (challengeTop - getDragHandleSizeAbove())) && y > ((float) (getDragHandleSizeBelow() + challengeTop)) : initialY > ((float) (getDragHandleSizeBelow() + challengeTop)) && y < ((float) (challengeTop - getDragHandleSizeAbove()));
        return horizOk && vertOk;
    }

    private int makeChildMeasureSpec(int maxSize, int childDimen) {
        int mode;
        int size;
        switch (childDimen) {
            case -2:
                mode = Integer.MIN_VALUE;
                size = maxSize;
                break;
            case -1:
                mode = 1073741824;
                size = maxSize;
                break;
            default:
                mode = 1073741824;
                size = Math.min(maxSize, childDimen);
                break;
        }
        return View.MeasureSpec.makeMeasureSpec(size, mode);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (View.MeasureSpec.getMode(widthSpec) != 1073741824 || View.MeasureSpec.getMode(heightSpec) != 1073741824) {
            throw new IllegalArgumentException("SlidingChallengeLayout must be measured with an exact size");
        }
        int width = View.MeasureSpec.getSize(widthSpec);
        int height = View.MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(width, height);
        int insetHeight = (height - this.mInsets.top) - this.mInsets.bottom;
        int insetHeightSpec = View.MeasureSpec.makeMeasureSpec(insetHeight, 1073741824);
        View oldChallengeView = this.mChallengeView;
        View oldExpandChallengeView = this.mChallengeView;
        this.mChallengeView = null;
        this.mExpandChallengeView = null;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.childType == 2) {
                if (this.mChallengeView != null) {
                    throw new IllegalStateException("There may only be one child with layout_isChallenge=\"true\"");
                }
                if (!(child instanceof KeyguardSecurityContainer)) {
                    throw new IllegalArgumentException("Challenge must be a KeyguardSecurityContainer");
                }
                this.mChallengeView = (KeyguardSecurityContainer) child;
                if (this.mChallengeView != oldChallengeView) {
                    this.mChallengeView.setVisibility(this.mChallengeShowing ? 0 : 4);
                }
                if (!this.mHasLayout) {
                    this.mHasGlowpad = child.findViewById(R.id.keyguard_selector_view) != null;
                    int challengeMargin = getChallengeMargin(true);
                    lp.rightMargin = challengeMargin;
                    lp.leftMargin = challengeMargin;
                }
            } else if (lp.childType == 6) {
                if (this.mExpandChallengeView != null) {
                    throw new IllegalStateException("There may only be one child with layout_childType=\"expandChallengeHandle\"");
                }
                this.mExpandChallengeView = child;
                if (this.mExpandChallengeView != oldExpandChallengeView) {
                    this.mExpandChallengeView.setVisibility(this.mChallengeShowing ? 4 : 0);
                    this.mExpandChallengeView.setOnClickListener(this.mExpandChallengeClickListener);
                }
            } else if (lp.childType == 4) {
                setScrimView(child);
            } else if (lp.childType == 5) {
                this.mWidgetsView = child;
            }
        }
        if (this.mChallengeView != null && this.mChallengeView.getVisibility() != 8) {
            int challengeHeightSpec = insetHeightSpec;
            View root = getRootView();
            if (root != null) {
                LayoutParams lp2 = (LayoutParams) this.mChallengeView.getLayoutParams();
                int windowHeight = (this.mDisplayMetrics.heightPixels - root.getPaddingTop()) - this.mInsets.top;
                int diff = windowHeight - insetHeight;
                int maxChallengeHeight = lp2.maxHeight - diff;
                if (maxChallengeHeight > 0) {
                    challengeHeightSpec = makeChildMeasureSpec(maxChallengeHeight, lp2.height);
                }
            }
            measureChildWithMargins(this.mChallengeView, widthSpec, 0, challengeHeightSpec, 0);
        }
        for (int i2 = 0; i2 < count; i2++) {
            View child2 = getChildAt(i2);
            if (child2.getVisibility() != 8 && child2 != this.mChallengeView) {
                int parentWidthSpec = widthSpec;
                int parentHeightSpec = insetHeightSpec;
                LayoutParams lp3 = (LayoutParams) child2.getLayoutParams();
                if (lp3.childType == 5) {
                    View root2 = getRootView();
                    if (root2 != null) {
                        int windowWidth = this.mDisplayMetrics.widthPixels;
                        int windowHeight2 = (this.mDisplayMetrics.heightPixels - root2.getPaddingTop()) - this.mInsets.top;
                        parentWidthSpec = View.MeasureSpec.makeMeasureSpec(windowWidth, 1073741824);
                        parentHeightSpec = View.MeasureSpec.makeMeasureSpec(windowHeight2, 1073741824);
                    }
                } else if (lp3.childType == 4) {
                    parentWidthSpec = widthSpec;
                    parentHeightSpec = heightSpec;
                }
                measureChildWithMargins(child2, parentWidthSpec, 0, parentHeightSpec, 0);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int width = r - l;
        int height = b - t;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.childType == 2) {
                    int center = ((paddingLeft + width) - paddingRight) / 2;
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    int left = center - (childWidth / 2);
                    int layoutBottom = ((height - paddingBottom) - lp.bottomMargin) - this.mInsets.bottom;
                    int bottom = layoutBottom + ((int) ((childHeight - this.mChallengeBottomBound) * (1.0f - this.mChallengeOffset)));
                    child.setAlpha(getChallengeAlpha());
                    child.layout(left, bottom - childHeight, left + childWidth, bottom);
                } else if (lp.childType == 6) {
                    int center2 = ((paddingLeft + width) - paddingRight) / 2;
                    int left2 = center2 - (child.getMeasuredWidth() / 2);
                    int right = left2 + child.getMeasuredWidth();
                    int bottom2 = ((height - paddingBottom) - lp.bottomMargin) - this.mInsets.bottom;
                    int top = bottom2 - child.getMeasuredHeight();
                    child.layout(left2, top, right, bottom2);
                } else if (lp.childType == 4) {
                    child.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
                } else {
                    child.layout(lp.leftMargin + paddingLeft, lp.topMargin + paddingTop + this.mInsets.top, child.getMeasuredWidth() + paddingLeft, child.getMeasuredHeight() + paddingTop + this.mInsets.top);
                }
            }
        }
        if (!this.mHasLayout) {
            this.mHasLayout = true;
        }
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (this.mChallengeView == null || !this.mChallengeView.requestFocus(direction, previouslyFocusedRect)) {
            return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
        }
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (!this.mScroller.isFinished()) {
            if (this.mChallengeView == null) {
                Log.e("SlidingChallengeLayout", "Challenge view missing in computeScroll");
                this.mScroller.abortAnimation();
                return;
            }
            this.mScroller.computeScrollOffset();
            moveChallengeTo(this.mScroller.getCurrY());
            if (this.mScroller.isFinished()) {
                post(this.mEndScrollRunnable);
            }
        }
    }

    private void cancelTransitionsInProgress() {
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
            completeChallengeScroll();
        }
        if (this.mFader != null) {
            this.mFader.cancel();
        }
    }

    public void fadeInChallenge() {
        fadeChallenge(true);
    }

    public void fadeOutChallenge() {
        fadeChallenge(false);
    }

    public void fadeChallenge(final boolean show) {
        if (this.mChallengeView != null) {
            cancelTransitionsInProgress();
            float alpha = show ? 1.0f : 0.0f;
            int duration = show ? 160 : 100;
            this.mFader = ObjectAnimator.ofFloat(this.mChallengeView, "alpha", alpha);
            this.mFader.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    SlidingChallengeLayout.this.onFadeStart(show);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    SlidingChallengeLayout.this.onFadeEnd(show);
                }
            });
            this.mFader.setDuration(duration);
            this.mFader.start();
        }
    }

    private int getMaxChallengeBottom() {
        if (this.mChallengeView == null) {
            return 0;
        }
        int layoutBottom = getLayoutBottom();
        int challengeHeight = this.mChallengeView.getMeasuredHeight();
        return (layoutBottom + challengeHeight) - this.mChallengeBottomBound;
    }

    private int getMinChallengeBottom() {
        return getLayoutBottom();
    }

    public void onFadeStart(boolean show) {
        this.mChallengeInteractiveInternal = false;
        enableHardwareLayerForChallengeView();
        if (show) {
            moveChallengeTo(getMinChallengeBottom());
        }
        setScrollState(3);
    }

    private void enableHardwareLayerForChallengeView() {
        if (this.mChallengeView.isHardwareAccelerated()) {
            this.mChallengeView.setLayerType(2, null);
        }
    }

    public void onFadeEnd(boolean show) {
        this.mChallengeInteractiveInternal = true;
        setChallengeShowing(show);
        if (!show) {
            moveChallengeTo(getMaxChallengeBottom());
        }
        this.mChallengeView.setLayerType(0, null);
        this.mFader = null;
        setScrollState(0);
    }

    public int getMaxChallengeTop() {
        if (this.mChallengeView == null) {
            return 0;
        }
        int layoutBottom = getLayoutBottom();
        int challengeHeight = this.mChallengeView.getMeasuredHeight();
        return (layoutBottom - challengeHeight) - this.mInsets.top;
    }

    private boolean moveChallengeTo(int bottom) {
        if (this.mChallengeView == null || !this.mHasLayout) {
            return false;
        }
        int layoutBottom = getLayoutBottom();
        int challengeHeight = this.mChallengeView.getHeight();
        int bottom2 = Math.max(getMinChallengeBottom(), Math.min(bottom, getMaxChallengeBottom()));
        float offset = 1.0f - ((bottom2 - layoutBottom) / (challengeHeight - this.mChallengeBottomBound));
        this.mChallengeOffset = offset;
        if (offset > 0.0f && !this.mChallengeShowing) {
            setChallengeShowing(true);
        }
        this.mChallengeView.layout(this.mChallengeView.getLeft(), bottom2 - this.mChallengeView.getHeight(), this.mChallengeView.getRight(), bottom2);
        this.mChallengeView.setAlpha(getChallengeAlpha());
        if (this.mScrollListener != null) {
            this.mScrollListener.onScrollPositionChanged(offset, this.mChallengeView.getTop());
        }
        postInvalidateOnAnimation();
        return true;
    }

    private int getLayoutBottom() {
        int bottomMargin = this.mChallengeView == null ? 0 : ((LayoutParams) this.mChallengeView.getLayoutParams()).bottomMargin;
        int layoutBottom = ((getMeasuredHeight() - getPaddingBottom()) - bottomMargin) - this.mInsets.bottom;
        return layoutBottom;
    }

    private int getChallengeBottom() {
        if (this.mChallengeView == null) {
            return 0;
        }
        return this.mChallengeView.getBottom();
    }

    public void showChallenge(boolean show) {
        showChallenge(show, 0);
        if (!show) {
            this.mBlockDrag = true;
        }
    }

    private void showChallenge(int velocity) {
        boolean show;
        if (Math.abs(velocity) > this.mMinVelocity) {
            show = velocity < 0;
        } else {
            show = this.mChallengeOffset >= 0.5f;
        }
        showChallenge(show, velocity);
    }

    private void showChallenge(boolean show, int velocity) {
        if (this.mChallengeView == null) {
            setChallengeShowing(false);
            return;
        }
        if (this.mHasLayout) {
            this.mChallengeShowingTargetState = show;
            int layoutBottom = getLayoutBottom();
            if (!show) {
                layoutBottom = (this.mChallengeView.getHeight() + layoutBottom) - this.mChallengeBottomBound;
            }
            animateChallengeTo(layoutBottom, velocity);
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams ? new LayoutParams((LayoutParams) p) : p instanceof ViewGroup.MarginLayoutParams ? new LayoutParams((ViewGroup.MarginLayoutParams) p) : new LayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public int childType;
        public int maxHeight;

        public LayoutParams() {
            this(-1, -2);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.childType = 0;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.childType = 0;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            this.childType = 0;
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            this.childType = 0;
            this.childType = source.childType;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.childType = 0;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.SlidingChallengeLayout_Layout);
            this.childType = a.getInt(R.styleable.SlidingChallengeLayout_Layout_layout_childType, 0);
            this.maxHeight = a.getDimensionPixelSize(R.styleable.SlidingChallengeLayout_Layout_layout_maxHeight, 0);
            a.recycle();
        }
    }
}
