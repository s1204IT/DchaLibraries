package com.android.contacts.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toolbar;
import com.android.contacts.R;
import com.android.contacts.util.SchedulingUtils;

public class MultiShrinkScroller extends FrameLayout {
    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * t2 * t2 * t2) + 1.0f;
        }
    };
    private GradientDrawable mActionBarGradientDrawable;
    private View mActionBarGradientView;
    private final int mActionBarSize;
    private final float[] mAlphaMatrixValues;
    private int mCollapsedTitleBottomMargin;
    private int mCollapsedTitleStartMargin;
    private final ColorMatrix mColorMatrix;
    private final int mDismissDistanceOnRelease;
    private final int mDismissDistanceOnScroll;
    private final EdgeEffect mEdgeGlowBottom;
    private final EdgeEffect mEdgeGlowTop;
    private final int[] mGradientColors;
    private boolean mHasEverTouchedTheTop;
    private int mHeaderTintColor;
    private int mIntermediateHeaderHeight;
    private TextView mInvisiblePlaceholderTextView;
    private boolean mIsBeingDragged;
    private boolean mIsFullscreenDownwardsFling;
    private boolean mIsOpenContactSquare;
    private boolean mIsTouchDisabledForDismissAnimation;
    private final boolean mIsTwoPanel;
    private final float mLandscapePhotoRatio;
    private TextView mLargeTextView;
    private float[] mLastEventPosition;
    private MultiShrinkScrollerListener mListener;
    private int mMaximumHeaderHeight;
    private int mMaximumHeaderTextSize;
    private int mMaximumPortraitHeaderHeight;
    private final int mMaximumTitleMargin;
    private final int mMaximumVelocity;
    private int mMinimumHeaderHeight;
    private int mMinimumPortraitHeaderHeight;
    private final int mMinimumVelocity;
    private final ColorMatrix mMultiplyBlendMatrix;
    private final float[] mMultiplyBlendMatrixValues;
    private View mPhotoTouchInterceptOverlay;
    private QuickContactImageView mPhotoView;
    private View mPhotoViewContainer;
    private boolean mReceivedDown;
    private ScrollView mScrollView;
    private View mScrollViewChild;
    private final Scroller mScroller;
    private final Animator.AnimatorListener mSnapToBottomListener;
    private final int mSnapToTopSlopHeight;
    private View mStartColumn;
    private final PathInterpolator mTextSizePathInterpolator;
    private GradientDrawable mTitleGradientDrawable;
    private View mTitleGradientView;
    private View mToolbar;
    private final float mToolbarElevation;
    private final int mTouchSlop;
    private final int mTransparentStartHeight;
    private View mTransparentView;
    private VelocityTracker mVelocityTracker;
    private final ColorMatrix mWhitenessColorMatrix;

    public interface MultiShrinkScrollerListener {
        void onEnterFullscreen();

        void onEntranceAnimationDone();

        void onExitFullscreen();

        void onScrolledOffBottom();

        void onStartScrollOffBottom();

        void onTransparentViewHeightChange(float f);
    }

    public MultiShrinkScroller(Context context) {
        this(context, null);
    }

    public MultiShrinkScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiShrinkScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mLastEventPosition = new float[]{0.0f, 0.0f};
        this.mIsBeingDragged = false;
        this.mReceivedDown = false;
        this.mIsFullscreenDownwardsFling = false;
        this.mWhitenessColorMatrix = new ColorMatrix();
        this.mColorMatrix = new ColorMatrix();
        this.mAlphaMatrixValues = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
        this.mMultiplyBlendMatrix = new ColorMatrix();
        this.mMultiplyBlendMatrixValues = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
        this.mTextSizePathInterpolator = new PathInterpolator(0.16f, 0.4f, 0.2f, 1.0f);
        this.mGradientColors = new int[]{0, -2013265920};
        this.mTitleGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, this.mGradientColors);
        this.mActionBarGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, this.mGradientColors);
        this.mSnapToBottomListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (MultiShrinkScroller.this.getScrollUntilOffBottom() > 0 && MultiShrinkScroller.this.mListener != null) {
                    MultiShrinkScroller.this.mListener.onScrolledOffBottom();
                    MultiShrinkScroller.this.mListener = null;
                }
            }
        };
        ViewConfiguration configuration = ViewConfiguration.get(context);
        setFocusable(false);
        setWillNotDraw(false);
        this.mEdgeGlowBottom = new EdgeEffect(context);
        this.mEdgeGlowTop = new EdgeEffect(context);
        this.mScroller = new Scroller(context, sInterpolator);
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mTransparentStartHeight = (int) getResources().getDimension(R.dimen.quickcontact_starting_empty_height);
        this.mToolbarElevation = getResources().getDimension(R.dimen.quick_contact_toolbar_elevation);
        this.mIsTwoPanel = getResources().getBoolean(R.bool.quickcontact_two_panel);
        this.mMaximumTitleMargin = (int) getResources().getDimension(R.dimen.quickcontact_title_initial_margin);
        this.mDismissDistanceOnScroll = (int) getResources().getDimension(R.dimen.quickcontact_dismiss_distance_on_scroll);
        this.mDismissDistanceOnRelease = (int) getResources().getDimension(R.dimen.quickcontact_dismiss_distance_on_release);
        this.mSnapToTopSlopHeight = (int) getResources().getDimension(R.dimen.quickcontact_snap_to_top_slop_height);
        TypedValue photoRatio = new TypedValue();
        getResources().getValue(R.dimen.quickcontact_landscape_photo_ratio, photoRatio, true);
        this.mLandscapePhotoRatio = photoRatio.getFloat();
        TypedArray attributeArray = context.obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        this.mActionBarSize = attributeArray.getDimensionPixelSize(0, 0);
        this.mMinimumHeaderHeight = this.mActionBarSize;
        this.mMinimumPortraitHeaderHeight = this.mMinimumHeaderHeight;
        attributeArray.recycle();
    }

    public void initialize(MultiShrinkScrollerListener listener, boolean isOpenContactSquare) {
        this.mScrollView = (ScrollView) findViewById(R.id.content_scroller);
        this.mScrollViewChild = findViewById(R.id.card_container);
        this.mToolbar = findViewById(R.id.toolbar_parent);
        this.mPhotoViewContainer = findViewById(R.id.toolbar_parent);
        this.mTransparentView = findViewById(R.id.transparent_view);
        this.mLargeTextView = (TextView) findViewById(R.id.large_title);
        this.mInvisiblePlaceholderTextView = (TextView) findViewById(R.id.placeholder_textview);
        this.mStartColumn = findViewById(R.id.empty_start_column);
        if (this.mStartColumn != null) {
            this.mStartColumn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MultiShrinkScroller.this.scrollOffBottom();
                }
            });
            findViewById(R.id.empty_end_column).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MultiShrinkScroller.this.scrollOffBottom();
                }
            });
        }
        this.mListener = listener;
        this.mIsOpenContactSquare = isOpenContactSquare;
        this.mPhotoView = (QuickContactImageView) findViewById(R.id.photo);
        this.mTitleGradientView = findViewById(R.id.title_gradient);
        this.mTitleGradientView.setBackground(this.mTitleGradientDrawable);
        this.mActionBarGradientView = findViewById(R.id.action_bar_gradient);
        this.mActionBarGradientView.setBackground(this.mActionBarGradientDrawable);
        this.mCollapsedTitleStartMargin = ((Toolbar) findViewById(R.id.toolbar)).getContentInsetStart();
        this.mPhotoTouchInterceptOverlay = findViewById(R.id.photo_touch_intercept_overlay);
        if (!this.mIsTwoPanel) {
            this.mPhotoTouchInterceptOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MultiShrinkScroller.this.expandHeader();
                }
            });
        }
        SchedulingUtils.doOnPreDraw(this, false, new Runnable() {
            @Override
            public void run() {
                if (!MultiShrinkScroller.this.mIsTwoPanel) {
                    MultiShrinkScroller.this.mMaximumHeaderHeight = MultiShrinkScroller.this.mPhotoViewContainer.getWidth();
                    MultiShrinkScroller.this.mIntermediateHeaderHeight = (int) (MultiShrinkScroller.this.mMaximumHeaderHeight * 0.6f);
                }
                MultiShrinkScroller.this.mMaximumPortraitHeaderHeight = MultiShrinkScroller.this.mIsTwoPanel ? MultiShrinkScroller.this.getHeight() : MultiShrinkScroller.this.mPhotoViewContainer.getWidth();
                MultiShrinkScroller.this.setHeaderHeight(MultiShrinkScroller.this.getMaximumScrollableHeaderHeight());
                MultiShrinkScroller.this.mMaximumHeaderTextSize = MultiShrinkScroller.this.mLargeTextView.getHeight();
                if (MultiShrinkScroller.this.mIsTwoPanel) {
                    MultiShrinkScroller.this.mMaximumHeaderHeight = MultiShrinkScroller.this.getHeight();
                    MultiShrinkScroller.this.mMinimumHeaderHeight = MultiShrinkScroller.this.mMaximumHeaderHeight;
                    MultiShrinkScroller.this.mIntermediateHeaderHeight = MultiShrinkScroller.this.mMaximumHeaderHeight;
                    ViewGroup.LayoutParams photoLayoutParams = MultiShrinkScroller.this.mPhotoViewContainer.getLayoutParams();
                    photoLayoutParams.height = MultiShrinkScroller.this.mMaximumHeaderHeight;
                    photoLayoutParams.width = (int) (MultiShrinkScroller.this.mMaximumHeaderHeight * MultiShrinkScroller.this.mLandscapePhotoRatio);
                    MultiShrinkScroller.this.mPhotoViewContainer.setLayoutParams(photoLayoutParams);
                    FrameLayout.LayoutParams largeTextLayoutParams = (FrameLayout.LayoutParams) MultiShrinkScroller.this.mLargeTextView.getLayoutParams();
                    largeTextLayoutParams.width = (photoLayoutParams.width - largeTextLayoutParams.leftMargin) - largeTextLayoutParams.rightMargin;
                    largeTextLayoutParams.gravity = 8388691;
                    MultiShrinkScroller.this.mLargeTextView.setLayoutParams(largeTextLayoutParams);
                } else {
                    MultiShrinkScroller.this.mLargeTextView.setWidth(MultiShrinkScroller.this.mPhotoViewContainer.getWidth() - (MultiShrinkScroller.this.mMaximumTitleMargin * 2));
                }
                MultiShrinkScroller.this.calculateCollapsedLargeTitlePadding();
                MultiShrinkScroller.this.updateHeaderTextSizeAndMargin();
                MultiShrinkScroller.this.configureGradientViewHeights();
            }
        });
    }

    private void configureGradientViewHeights() {
        FrameLayout.LayoutParams actionBarGradientLayoutParams = (FrameLayout.LayoutParams) this.mActionBarGradientView.getLayoutParams();
        actionBarGradientLayoutParams.height = this.mActionBarSize;
        this.mActionBarGradientView.setLayoutParams(actionBarGradientLayoutParams);
        FrameLayout.LayoutParams titleGradientLayoutParams = (FrameLayout.LayoutParams) this.mTitleGradientView.getLayoutParams();
        FrameLayout.LayoutParams largeTextLayoutParms = (FrameLayout.LayoutParams) this.mLargeTextView.getLayoutParams();
        titleGradientLayoutParams.height = (int) ((this.mLargeTextView.getHeight() + largeTextLayoutParms.bottomMargin) * 1.25f);
        this.mTitleGradientView.setLayoutParams(titleGradientLayoutParams);
    }

    public void setTitle(String title) {
        this.mLargeTextView.setText(title);
        this.mPhotoTouchInterceptOverlay.setContentDescription(title);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(event);
        return shouldStartDrag(event);
    }

    private boolean shouldStartDrag(MotionEvent event) {
        if (this.mIsTouchDisabledForDismissAnimation) {
            return false;
        }
        if (this.mIsBeingDragged) {
            this.mIsBeingDragged = false;
            return false;
        }
        switch (event.getAction()) {
            case 0:
                updateLastEventPosition(event);
                if (!this.mScroller.isFinished()) {
                    startDrag();
                } else {
                    this.mReceivedDown = true;
                }
                break;
            case 2:
                if (motionShouldStartDrag(event)) {
                    updateLastEventPosition(event);
                    startDrag();
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mIsTouchDisabledForDismissAnimation) {
            return true;
        }
        int action = event.getAction();
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(event);
        if (!this.mIsBeingDragged) {
            if (shouldStartDrag(event) || action != 1 || !this.mReceivedDown) {
                return true;
            }
            this.mReceivedDown = false;
            return performClick();
        }
        switch (action) {
            case 1:
            case 3:
                stopDrag(action == 3);
                this.mReceivedDown = false;
                break;
            case 2:
                float delta = updatePositionAndComputeDelta(event);
                scrollTo(0, getScroll() + ((int) delta));
                this.mReceivedDown = false;
                if (this.mIsBeingDragged) {
                    int distanceFromMaxScrolling = getMaximumScrollUpwards() - getScroll();
                    if (delta > distanceFromMaxScrolling) {
                        this.mEdgeGlowBottom.onPull(delta / getHeight(), 1.0f - (event.getX() / getWidth()));
                    }
                    if (!this.mEdgeGlowBottom.isFinished()) {
                        postInvalidateOnAnimation();
                    }
                    if (shouldDismissOnScroll()) {
                        scrollOffBottom();
                    }
                }
                break;
        }
        return true;
    }

    public void setHeaderTintColor(int color) {
        this.mHeaderTintColor = color;
        updatePhotoTintAndDropShadow();
        int edgeEffectAlpha = Color.alpha(this.mEdgeGlowBottom.getColor());
        this.mEdgeGlowBottom.setColor((16777215 & color) | Color.argb(edgeEffectAlpha, 0, 0, 0));
        this.mEdgeGlowTop.setColor(this.mEdgeGlowBottom.getColor());
    }

    private void expandHeader() {
        if (getHeaderHeight() != this.mMaximumHeaderHeight) {
            ObjectAnimator animator = ObjectAnimator.ofInt(this, "headerHeight", this.mMaximumHeaderHeight);
            animator.setDuration(300L);
            animator.start();
            if (this.mScrollView.getScrollY() != 0) {
                ObjectAnimator.ofInt(this.mScrollView, "scrollY", -this.mScrollView.getScrollY()).start();
            }
        }
    }

    private void startDrag() {
        this.mIsBeingDragged = true;
        this.mScroller.abortAnimation();
    }

    private void stopDrag(boolean cancelled) {
        this.mIsBeingDragged = false;
        if (!cancelled && getChildCount() > 0) {
            float velocity = getCurrentVelocity();
            if (velocity > this.mMinimumVelocity || velocity < (-this.mMinimumVelocity)) {
                fling(-velocity);
                onDragFinished(this.mScroller.getFinalY() - this.mScroller.getStartY());
            } else {
                onDragFinished(0);
            }
        } else {
            onDragFinished(0);
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
        this.mEdgeGlowBottom.onRelease();
    }

    private void onDragFinished(int flingDelta) {
        if (getTransparentViewHeight() > 0 && !snapToTopOnDragFinished(flingDelta)) {
            snapToBottomOnDragFinished();
        }
    }

    private boolean snapToTopOnDragFinished(int flingDelta) {
        if (!this.mHasEverTouchedTheTop) {
            float predictedScrollPastTop = getTransparentViewHeight() - flingDelta;
            if (predictedScrollPastTop < (-this.mSnapToTopSlopHeight) || getTransparentViewHeight() > this.mTransparentStartHeight) {
                return false;
            }
            this.mScroller.forceFinished(true);
            smoothScrollBy(getTransparentViewHeight());
            return true;
        }
        if (getTransparentViewHeight() >= this.mDismissDistanceOnRelease) {
            return false;
        }
        this.mScroller.forceFinished(true);
        smoothScrollBy(getTransparentViewHeight());
        return true;
    }

    private void snapToBottomOnDragFinished() {
        if (this.mHasEverTouchedTheTop) {
            if (getTransparentViewHeight() > this.mDismissDistanceOnRelease) {
                scrollOffBottom();
            }
        } else if (getTransparentViewHeight() > this.mTransparentStartHeight) {
            scrollOffBottom();
        }
    }

    private boolean shouldDismissOnScroll() {
        return this.mHasEverTouchedTheTop && getTransparentViewHeight() > this.mDismissDistanceOnScroll;
    }

    public float getStartingTransparentHeightRatio() {
        return getTransparentHeightRatio(this.mTransparentStartHeight);
    }

    private float getTransparentHeightRatio(int transparentHeight) {
        float heightRatio = transparentHeight / getHeight();
        return 1.0f - Math.max(Math.min(1.0f, heightRatio), 0.0f);
    }

    public void scrollOffBottom() {
        this.mIsTouchDisabledForDismissAnimation = true;
        Interpolator interpolator = new AcceleratingFlingInterpolator(250, getCurrentVelocity(), getScrollUntilOffBottom());
        this.mScroller.forceFinished(true);
        ObjectAnimator translateAnimation = ObjectAnimator.ofInt(this, "scroll", getScroll() - getScrollUntilOffBottom());
        translateAnimation.setRepeatCount(0);
        translateAnimation.setInterpolator(interpolator);
        translateAnimation.setDuration(250L);
        translateAnimation.addListener(this.mSnapToBottomListener);
        translateAnimation.start();
        if (this.mListener != null) {
            this.mListener.onStartScrollOffBottom();
        }
    }

    public void scrollUpForEntranceAnimation(boolean scrollToCurrentPosition) {
        int currentPosition = getScroll();
        int bottomScrollPosition = (currentPosition - (getHeight() - getTransparentViewHeight())) + 1;
        Interpolator interpolator = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear_out_slow_in);
        final int desiredValue = currentPosition + (scrollToCurrentPosition ? currentPosition : getTransparentViewHeight());
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "scroll", bottomScrollPosition, desiredValue);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation.getAnimatedValue().equals(Integer.valueOf(desiredValue)) && MultiShrinkScroller.this.mListener != null) {
                    MultiShrinkScroller.this.mListener.onEntranceAnimationDone();
                }
            }
        });
        animator.start();
    }

    @Override
    public void scrollTo(int x, int y) {
        int delta = y - getScroll();
        boolean wasFullscreen = getScrollNeededToBeFullScreen() <= 0;
        if (delta > 0) {
            scrollUp(delta);
        } else {
            scrollDown(delta);
        }
        updatePhotoTintAndDropShadow();
        updateHeaderTextSizeAndMargin();
        boolean isFullscreen = getScrollNeededToBeFullScreen() <= 0;
        this.mHasEverTouchedTheTop |= isFullscreen;
        if (this.mListener != null) {
            if (wasFullscreen && !isFullscreen) {
                this.mListener.onExitFullscreen();
            } else if (!wasFullscreen && isFullscreen) {
                this.mListener.onEnterFullscreen();
            }
            if (!isFullscreen || !wasFullscreen) {
                this.mListener.onTransparentViewHeightChange(getTransparentHeightRatio(getTransparentViewHeight()));
            }
        }
    }

    public void setToolbarHeight(int delta) {
        ViewGroup.LayoutParams toolbarLayoutParams = this.mToolbar.getLayoutParams();
        toolbarLayoutParams.height = delta;
        this.mToolbar.setLayoutParams(toolbarLayoutParams);
        updatePhotoTintAndDropShadow();
        updateHeaderTextSizeAndMargin();
    }

    public int getToolbarHeight() {
        return this.mToolbar.getLayoutParams().height;
    }

    public void setHeaderHeight(int height) {
        ViewGroup.LayoutParams toolbarLayoutParams = this.mToolbar.getLayoutParams();
        toolbarLayoutParams.height = height;
        this.mToolbar.setLayoutParams(toolbarLayoutParams);
        updatePhotoTintAndDropShadow();
        updateHeaderTextSizeAndMargin();
    }

    public int getHeaderHeight() {
        return this.mToolbar.getLayoutParams().height;
    }

    public void setScroll(int scroll) {
        scrollTo(0, scroll);
    }

    public int getScroll() {
        return (((this.mTransparentStartHeight - getTransparentViewHeight()) + getMaximumScrollableHeaderHeight()) - getToolbarHeight()) + this.mScrollView.getScrollY();
    }

    private int getMaximumScrollableHeaderHeight() {
        return this.mIsOpenContactSquare ? this.mMaximumHeaderHeight : this.mIntermediateHeaderHeight;
    }

    private int getScroll_ignoreOversizedHeaderForSnapping() {
        return (this.mTransparentStartHeight - getTransparentViewHeight()) + Math.max(getMaximumScrollableHeaderHeight() - getToolbarHeight(), 0) + this.mScrollView.getScrollY();
    }

    public int getScrollNeededToBeFullScreen() {
        return getTransparentViewHeight();
    }

    private int getScrollUntilOffBottom() {
        return (getHeight() + getScroll_ignoreOversizedHeaderForSnapping()) - this.mTransparentStartHeight;
    }

    @Override
    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int oldScroll = getScroll();
            scrollTo(0, this.mScroller.getCurrY());
            int delta = this.mScroller.getCurrY() - oldScroll;
            int distanceFromMaxScrolling = getMaximumScrollUpwards() - getScroll();
            if (delta > distanceFromMaxScrolling && distanceFromMaxScrolling > 0) {
                this.mEdgeGlowBottom.onAbsorb((int) this.mScroller.getCurrVelocity());
            }
            if (this.mIsFullscreenDownwardsFling && getTransparentViewHeight() > 0) {
                scrollTo(0, getScroll() + getTransparentViewHeight());
                this.mEdgeGlowTop.onAbsorb((int) this.mScroller.getCurrVelocity());
                this.mScroller.abortAnimation();
                this.mIsFullscreenDownwardsFling = false;
            }
            if (!awakenScrollBars()) {
                postInvalidateOnAnimation();
            }
            if (this.mScroller.getCurrY() >= getMaximumScrollUpwards()) {
                this.mScroller.abortAnimation();
                this.mIsFullscreenDownwardsFling = false;
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        int width = (getWidth() - getPaddingLeft()) - getPaddingRight();
        int height = getHeight();
        if (!this.mEdgeGlowBottom.isFinished()) {
            int restoreCount = canvas.save();
            canvas.translate((-width) + getPaddingLeft(), (getMaximumScrollUpwards() + height) - getScroll());
            canvas.rotate(180.0f, width, 0.0f);
            if (this.mIsTwoPanel) {
                this.mEdgeGlowBottom.setSize(this.mScrollView.getWidth(), height);
                if (isLayoutRtl()) {
                    canvas.translate(this.mPhotoViewContainer.getWidth(), 0.0f);
                }
            } else {
                this.mEdgeGlowBottom.setSize(width, height);
            }
            if (this.mEdgeGlowBottom.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }
        if (!this.mEdgeGlowTop.isFinished()) {
            int restoreCount2 = canvas.save();
            if (this.mIsTwoPanel) {
                this.mEdgeGlowTop.setSize(this.mScrollView.getWidth(), height);
                if (!isLayoutRtl()) {
                    canvas.translate(this.mPhotoViewContainer.getWidth(), 0.0f);
                }
            } else {
                this.mEdgeGlowTop.setSize(width, height);
            }
            if (this.mEdgeGlowTop.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount2);
        }
    }

    private float getCurrentVelocity() {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
        return this.mVelocityTracker.getYVelocity();
    }

    private void fling(float velocity) {
        this.mScroller.fling(0, getScroll(), 0, (int) velocity, 0, 0, -2147483647, Integer.MAX_VALUE);
        if (velocity < 0.0f && this.mTransparentView.getHeight() <= 0) {
            this.mIsFullscreenDownwardsFling = true;
        }
        invalidate();
    }

    private int getMaximumScrollUpwards() {
        return !this.mIsTwoPanel ? ((this.mTransparentStartHeight + getMaximumScrollableHeaderHeight()) - getFullyCompressedHeaderHeight()) + Math.max(0, (this.mScrollViewChild.getHeight() - getHeight()) + getFullyCompressedHeaderHeight()) : this.mTransparentStartHeight + Math.max(0, this.mScrollViewChild.getHeight() - getHeight());
    }

    private int getTransparentViewHeight() {
        return this.mTransparentView.getLayoutParams().height;
    }

    private void setTransparentViewHeight(int height) {
        this.mTransparentView.getLayoutParams().height = height;
        this.mTransparentView.setLayoutParams(this.mTransparentView.getLayoutParams());
    }

    private void scrollUp(int delta) {
        if (getTransparentViewHeight() != 0) {
            int originalValue = getTransparentViewHeight();
            setTransparentViewHeight(getTransparentViewHeight() - delta);
            setTransparentViewHeight(Math.max(0, getTransparentViewHeight()));
            delta -= originalValue - getTransparentViewHeight();
        }
        ViewGroup.LayoutParams toolbarLayoutParams = this.mToolbar.getLayoutParams();
        if (toolbarLayoutParams.height > getFullyCompressedHeaderHeight()) {
            int originalValue2 = toolbarLayoutParams.height;
            toolbarLayoutParams.height -= delta;
            toolbarLayoutParams.height = Math.max(toolbarLayoutParams.height, getFullyCompressedHeaderHeight());
            this.mToolbar.setLayoutParams(toolbarLayoutParams);
            delta -= originalValue2 - toolbarLayoutParams.height;
        }
        this.mScrollView.scrollBy(0, delta);
    }

    private int getFullyCompressedHeaderHeight() {
        return Math.min(Math.max(this.mToolbar.getLayoutParams().height - getOverflowingChildViewSize(), this.mMinimumHeaderHeight), getMaximumScrollableHeaderHeight());
    }

    private int getOverflowingChildViewSize() {
        int usedScrollViewSpace = this.mScrollViewChild.getHeight();
        return (-getHeight()) + usedScrollViewSpace + this.mToolbar.getLayoutParams().height;
    }

    private void scrollDown(int delta) {
        if (this.mScrollView.getScrollY() > 0) {
            int originalValue = this.mScrollView.getScrollY();
            this.mScrollView.scrollBy(0, delta);
            delta -= this.mScrollView.getScrollY() - originalValue;
        }
        ViewGroup.LayoutParams toolbarLayoutParams = this.mToolbar.getLayoutParams();
        if (toolbarLayoutParams.height < getMaximumScrollableHeaderHeight()) {
            int originalValue2 = toolbarLayoutParams.height;
            toolbarLayoutParams.height -= delta;
            toolbarLayoutParams.height = Math.min(toolbarLayoutParams.height, getMaximumScrollableHeaderHeight());
            this.mToolbar.setLayoutParams(toolbarLayoutParams);
            delta -= originalValue2 - toolbarLayoutParams.height;
        }
        setTransparentViewHeight(getTransparentViewHeight() - delta);
        if (getScrollUntilOffBottom() <= 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (MultiShrinkScroller.this.mListener != null) {
                        MultiShrinkScroller.this.mListener.onScrolledOffBottom();
                        MultiShrinkScroller.this.mListener = null;
                    }
                }
            });
        }
    }

    private void updateHeaderTextSizeAndMargin() {
        if (!this.mIsTwoPanel) {
            if (isLayoutRtl()) {
                this.mLargeTextView.setPivotX(this.mLargeTextView.getWidth());
            } else {
                this.mLargeTextView.setPivotX(0.0f);
            }
            this.mLargeTextView.setPivotY(this.mLargeTextView.getHeight() / 2);
            int toolbarHeight = this.mToolbar.getLayoutParams().height;
            this.mPhotoTouchInterceptOverlay.setClickable(toolbarHeight != this.mMaximumHeaderHeight);
            if (toolbarHeight >= this.mMaximumHeaderHeight) {
                this.mLargeTextView.setScaleX(1.0f);
                this.mLargeTextView.setScaleY(1.0f);
                setInterpolatedTitleMargins(1.0f);
                return;
            }
            float ratio = (toolbarHeight - this.mMinimumHeaderHeight) / (this.mMaximumHeaderHeight - this.mMinimumHeaderHeight);
            float minimumSize = this.mInvisiblePlaceholderTextView.getHeight();
            float bezierOutput = this.mTextSizePathInterpolator.getInterpolation(ratio);
            float scale = (((this.mMaximumHeaderTextSize - minimumSize) * bezierOutput) + minimumSize) / this.mMaximumHeaderTextSize;
            float bezierOutput2 = Math.min(bezierOutput, 1.0f);
            float scale2 = Math.min(scale, 1.0f);
            this.mLargeTextView.setScaleX(scale2);
            this.mLargeTextView.setScaleY(scale2);
            setInterpolatedTitleMargins(bezierOutput2);
        }
    }

    private void calculateCollapsedLargeTitlePadding() {
        Rect largeTextViewRect = new Rect();
        this.mToolbar.getBoundsOnScreen(largeTextViewRect);
        Rect invisiblePlaceholderTextViewRect = new Rect();
        this.mInvisiblePlaceholderTextView.getBoundsOnScreen(invisiblePlaceholderTextViewRect);
        int desiredTopToCenter = ((invisiblePlaceholderTextViewRect.top + invisiblePlaceholderTextViewRect.bottom) / 2) - largeTextViewRect.top;
        this.mCollapsedTitleBottomMargin = desiredTopToCenter - (this.mLargeTextView.getHeight() / 2);
    }

    private void setInterpolatedTitleMargins(float x) {
        FrameLayout.LayoutParams titleLayoutParams = (FrameLayout.LayoutParams) this.mLargeTextView.getLayoutParams();
        LinearLayout.LayoutParams toolbarLayoutParams = (LinearLayout.LayoutParams) this.mToolbar.getLayoutParams();
        int startColumnWidth = this.mStartColumn == null ? 0 : this.mStartColumn.getWidth();
        titleLayoutParams.setMarginStart(((int) ((this.mCollapsedTitleStartMargin * (1.0f - x)) + (this.mMaximumTitleMargin * x))) + startColumnWidth);
        int pretendBottomMargin = (int) ((this.mCollapsedTitleBottomMargin * (1.0f - x)) + (this.mMaximumTitleMargin * x));
        titleLayoutParams.topMargin = ((getTransparentViewHeight() + toolbarLayoutParams.height) - pretendBottomMargin) - this.mMaximumHeaderTextSize;
        titleLayoutParams.bottomMargin = 0;
        this.mLargeTextView.setLayoutParams(titleLayoutParams);
    }

    private void updatePhotoTintAndDropShadow() {
        int gradientAlpha;
        Trace.beginSection("updatePhotoTintAndDropShadow");
        if (this.mIsTwoPanel && !this.mPhotoView.isBasedOffLetterTile()) {
            this.mTitleGradientDrawable.setAlpha(255);
            this.mActionBarGradientDrawable.setAlpha(255);
            return;
        }
        int toolbarHeight = getToolbarHeight();
        if (toolbarHeight <= this.mMinimumHeaderHeight && !this.mIsTwoPanel) {
            this.mPhotoViewContainer.setElevation(this.mToolbarElevation);
        } else {
            this.mPhotoViewContainer.setElevation(0.0f);
        }
        this.mPhotoView.clearColorFilter();
        this.mColorMatrix.reset();
        if (!this.mPhotoView.isBasedOffLetterTile()) {
            float ratio = calculateHeightRatioToBlendingStartHeight(toolbarHeight);
            float alpha = 1.0f - ((float) Math.min(Math.pow(ratio, 1.5d) * 2.0d, 1.0d));
            float tint = (float) Math.min(Math.pow(ratio, 1.5d) * 3.0d, 1.0d);
            this.mColorMatrix.setSaturation(alpha);
            this.mColorMatrix.postConcat(alphaMatrix(alpha, -1));
            this.mColorMatrix.postConcat(multiplyBlendMatrix(this.mHeaderTintColor, tint));
            gradientAlpha = (int) (255.0f * alpha);
        } else if (this.mIsTwoPanel) {
            this.mColorMatrix.reset();
            this.mColorMatrix.postConcat(alphaMatrix(0.8f, this.mHeaderTintColor));
            gradientAlpha = 0;
        } else {
            float ratio2 = calculateHeightRatioToFullyOpen(toolbarHeight);
            float intermediateRatio = calculateHeightRatioToFullyOpen((int) (this.mMaximumPortraitHeaderHeight * 0.6f));
            float slowingFactor = (float) (((double) ((1.0f - intermediateRatio) / intermediateRatio)) / (1.0d - Math.pow(0.19999998807907104d, 0.3333333432674408d)));
            float linearBeforeIntermediate = Math.max(1.0f - (((1.0f - ratio2) / intermediateRatio) / slowingFactor), 0.0f);
            float colorAlpha = 1.0f - ((float) Math.pow(linearBeforeIntermediate, 3.0d));
            this.mColorMatrix.postConcat(alphaMatrix(colorAlpha, this.mHeaderTintColor));
            gradientAlpha = 0;
        }
        this.mPhotoView.setColorFilter(new ColorMatrixColorFilter(this.mColorMatrix));
        this.mPhotoView.setTint(this.mHeaderTintColor);
        this.mTitleGradientDrawable.setAlpha(gradientAlpha);
        this.mActionBarGradientDrawable.setAlpha(gradientAlpha);
        Trace.endSection();
    }

    private float calculateHeightRatioToFullyOpen(int height) {
        return (height - this.mMinimumPortraitHeaderHeight) / (this.mMaximumPortraitHeaderHeight - this.mMinimumPortraitHeaderHeight);
    }

    private float calculateHeightRatioToBlendingStartHeight(int height) {
        float intermediateHeight = this.mMaximumPortraitHeaderHeight * 0.5f;
        float interpolatingHeightRange = intermediateHeight - this.mMinimumPortraitHeaderHeight;
        if (height > intermediateHeight) {
            return 0.0f;
        }
        return (intermediateHeight - height) / interpolatingHeightRange;
    }

    private ColorMatrix alphaMatrix(float alpha, int color) {
        this.mAlphaMatrixValues[0] = (Color.red(color) * alpha) / 255.0f;
        this.mAlphaMatrixValues[6] = (Color.green(color) * alpha) / 255.0f;
        this.mAlphaMatrixValues[12] = (Color.blue(color) * alpha) / 255.0f;
        this.mAlphaMatrixValues[4] = (1.0f - alpha) * 255.0f;
        this.mAlphaMatrixValues[9] = (1.0f - alpha) * 255.0f;
        this.mAlphaMatrixValues[14] = (1.0f - alpha) * 255.0f;
        this.mWhitenessColorMatrix.set(this.mAlphaMatrixValues);
        return this.mWhitenessColorMatrix;
    }

    private ColorMatrix multiplyBlendMatrix(int color, float alpha) {
        this.mMultiplyBlendMatrixValues[0] = multiplyBlend(Color.red(color), alpha);
        this.mMultiplyBlendMatrixValues[6] = multiplyBlend(Color.green(color), alpha);
        this.mMultiplyBlendMatrixValues[12] = multiplyBlend(Color.blue(color), alpha);
        this.mMultiplyBlendMatrix.set(this.mMultiplyBlendMatrixValues);
        return this.mMultiplyBlendMatrix;
    }

    private float multiplyBlend(int color, float alpha) {
        return ((color * alpha) / 255.0f) + (1.0f - alpha);
    }

    private void updateLastEventPosition(MotionEvent event) {
        this.mLastEventPosition[0] = event.getX();
        this.mLastEventPosition[1] = event.getY();
    }

    private boolean motionShouldStartDrag(MotionEvent event) {
        float deltaY = event.getY() - this.mLastEventPosition[1];
        return deltaY > ((float) this.mTouchSlop) || deltaY < ((float) (-this.mTouchSlop));
    }

    private float updatePositionAndComputeDelta(MotionEvent event) {
        float position = this.mLastEventPosition[1];
        updateLastEventPosition(event);
        float elasticityFactor = 1.0f;
        if (position < this.mLastEventPosition[1] && this.mHasEverTouchedTheTop) {
            elasticityFactor = 1.0f + (this.mTransparentView.getHeight() * 0.01f);
        }
        return (position - this.mLastEventPosition[1]) / elasticityFactor;
    }

    private void smoothScrollBy(int delta) {
        if (delta == 0) {
            throw new IllegalArgumentException("Smooth scrolling by delta=0 is pointless and harmful");
        }
        this.mScroller.startScroll(0, getScroll(), 0, delta);
        invalidate();
    }

    private static class AcceleratingFlingInterpolator implements Interpolator {
        private final float mDurationMs;
        private final float mNumberFrames;
        private final int mPixelsDelta;
        private final float mStartingSpeedPixelsPerFrame;

        public AcceleratingFlingInterpolator(int durationMs, float startingSpeedPixelsPerSecond, int pixelsDelta) {
            this.mStartingSpeedPixelsPerFrame = startingSpeedPixelsPerSecond / getRefreshRate();
            this.mDurationMs = durationMs;
            this.mPixelsDelta = pixelsDelta;
            this.mNumberFrames = this.mDurationMs / getFrameIntervalMs();
        }

        @Override
        public float getInterpolation(float input) {
            float animationIntervalNumber = this.mNumberFrames * input;
            float linearDelta = (this.mStartingSpeedPixelsPerFrame * animationIntervalNumber) / this.mPixelsDelta;
            return this.mStartingSpeedPixelsPerFrame > 0.0f ? Math.min((input * input) + linearDelta, 1.0f) : Math.min(((input - linearDelta) * input) + linearDelta, 1.0f);
        }

        private float getRefreshRate() {
            DisplayInfo di = DisplayManagerGlobal.getInstance().getDisplayInfo(0);
            return di.refreshRate;
        }

        public long getFrameIntervalMs() {
            return (long) (1000.0f / getRefreshRate());
        }
    }

    public void prepareForShrinkingScrollChild(int heightDelta) {
        this.mScrollView.suppressLayout(false);
        int newEmptyScrollViewSpace = (-getOverflowingChildViewSize()) + heightDelta;
        if (newEmptyScrollViewSpace > 0 && !this.mIsTwoPanel) {
            int newDesiredToolbarHeight = Math.min(getToolbarHeight() + newEmptyScrollViewSpace, getMaximumScrollableHeaderHeight());
            ObjectAnimator.ofInt(this, "toolbarHeight", newDesiredToolbarHeight).setDuration(300L).start();
        }
    }

    public void prepareForExpandingScrollChild() {
        this.mScrollView.suppressLayout(false);
    }
}
