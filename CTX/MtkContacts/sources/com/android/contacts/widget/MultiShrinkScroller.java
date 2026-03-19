package com.android.contacts.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Trace;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toolbar;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.EdgeEffectCompat;
import com.android.contacts.util.SchedulingUtils;
import com.mediatek.contacts.ExtensionManager;

public class MultiShrinkScroller extends FrameLayout {
    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float f) {
            float f2 = f - 1.0f;
            return (f2 * f2 * f2 * f2 * f2) + 1.0f;
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
    private TextView mFullNameView;
    private final int[] mGradientColors;
    private boolean mHasEverTouchedTheTop;
    private int mHeaderTintColor;
    private int mIntermediateHeaderHeight;
    private TextView mInvisiblePlaceholderTextView;
    private boolean mIsBeingDragged;
    private boolean mIsFullscreenDownwardsFling;
    private boolean mIsOpenContactSquare;
    private boolean mIsTouchDisabledForDismissAnimation;
    private boolean mIsTouchDisabledForSuppressLayout;
    private boolean mIsTwoPanel;
    private final float mLandscapePhotoRatio;
    private float[] mLastEventPosition;
    private MultiShrinkScrollerListener mListener;
    private int mMaximumFullNameViewHeight;
    private int mMaximumHeaderHeight;
    private int mMaximumHeaderTextSize;
    private int mMaximumPhoneticNameViewHeight;
    private int mMaximumPortraitHeaderHeight;
    private final int mMaximumTitleMargin;
    private final int mMaximumVelocity;
    private int mMinimumHeaderHeight;
    private int mMinimumPortraitHeaderHeight;
    private final int mMinimumVelocity;
    private final ColorMatrix mMultiplyBlendMatrix;
    private final float[] mMultiplyBlendMatrixValues;
    private TextView mPhoneticNameView;
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
    private final Interpolator mTextSizePathInterpolator;
    private View mTitleAndPhoneticNameView;
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

    public MultiShrinkScroller(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MultiShrinkScroller(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mLastEventPosition = new float[]{ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT};
        this.mIsBeingDragged = false;
        this.mReceivedDown = false;
        this.mIsFullscreenDownwardsFling = false;
        this.mWhitenessColorMatrix = new ColorMatrix();
        this.mColorMatrix = new ColorMatrix();
        this.mAlphaMatrixValues = new float[]{ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, 1.0f, ContactPhotoManager.OFFSET_DEFAULT};
        this.mMultiplyBlendMatrix = new ColorMatrix();
        this.mMultiplyBlendMatrixValues = new float[]{ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT, 1.0f, ContactPhotoManager.OFFSET_DEFAULT};
        this.mTextSizePathInterpolator = PathInterpolatorCompat.create(0.16f, 0.4f, 0.2f, 1.0f);
        this.mGradientColors = new int[]{0, -2013265920};
        this.mTitleGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, this.mGradientColors);
        this.mActionBarGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, this.mGradientColors);
        this.mSnapToBottomListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (MultiShrinkScroller.this.getScrollUntilOffBottom() > 0 && MultiShrinkScroller.this.mListener != null) {
                    MultiShrinkScroller.this.mListener.onScrolledOffBottom();
                    MultiShrinkScroller.this.mListener = null;
                }
            }
        };
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        setFocusable(false);
        setWillNotDraw(false);
        this.mEdgeGlowBottom = new EdgeEffect(context);
        this.mEdgeGlowTop = new EdgeEffect(context);
        this.mScroller = new Scroller(context, sInterpolator);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mTransparentStartHeight = (int) getResources().getDimension(R.dimen.quickcontact_starting_empty_height);
        this.mToolbarElevation = getResources().getDimension(R.dimen.quick_contact_toolbar_elevation);
        this.mIsTwoPanel = getResources().getBoolean(R.bool.quickcontact_two_panel);
        this.mMaximumTitleMargin = (int) getResources().getDimension(R.dimen.quickcontact_title_initial_margin);
        this.mDismissDistanceOnScroll = (int) getResources().getDimension(R.dimen.quickcontact_dismiss_distance_on_scroll);
        this.mDismissDistanceOnRelease = (int) getResources().getDimension(R.dimen.quickcontact_dismiss_distance_on_release);
        this.mSnapToTopSlopHeight = (int) getResources().getDimension(R.dimen.quickcontact_snap_to_top_slop_height);
        TypedValue typedValue = new TypedValue();
        getResources().getValue(R.dimen.quickcontact_landscape_photo_ratio, typedValue, true);
        this.mLandscapePhotoRatio = typedValue.getFloat();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        this.mActionBarSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 0);
        this.mMinimumHeaderHeight = this.mActionBarSize;
        this.mMinimumPortraitHeaderHeight = this.mMinimumHeaderHeight;
        typedArrayObtainStyledAttributes.recycle();
    }

    public void initialize(MultiShrinkScrollerListener multiShrinkScrollerListener, boolean z, final int i, final boolean z2) {
        this.mScrollView = (ScrollView) findViewById(R.id.content_scroller);
        this.mScrollViewChild = findViewById(R.id.card_container);
        this.mToolbar = findViewById(R.id.toolbar_parent);
        this.mPhotoViewContainer = findViewById(R.id.toolbar_parent);
        this.mTransparentView = findViewById(R.id.transparent_view);
        this.mFullNameView = (TextView) findViewById(R.id.large_title);
        this.mPhoneticNameView = (TextView) findViewById(R.id.phonetic_name);
        this.mTitleAndPhoneticNameView = findViewById(R.id.title_and_phonetic_name);
        this.mInvisiblePlaceholderTextView = (TextView) findViewById(R.id.placeholder_textview);
        this.mStartColumn = findViewById(R.id.empty_start_column);
        if (this.mStartColumn != null) {
            this.mStartColumn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MultiShrinkScroller.this.scrollOffBottom();
                }
            });
            findViewById(R.id.empty_end_column).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MultiShrinkScroller.this.scrollOffBottom();
                }
            });
        }
        this.mListener = multiShrinkScrollerListener;
        this.mIsOpenContactSquare = z;
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
                public void onClick(View view) {
                    MultiShrinkScroller.this.expandHeader();
                }
            });
        }
        SchedulingUtils.doOnPreDraw(this, false, new Runnable() {
            @Override
            public void run() {
                if (MultiShrinkScroller.this.getContext() != null) {
                    MultiShrinkScroller.this.mIsTwoPanel = MultiShrinkScroller.this.getResources().getBoolean(R.bool.quickcontact_two_panel);
                    if (!MultiShrinkScroller.this.mIsTwoPanel) {
                        MultiShrinkScroller.this.mMaximumHeaderHeight = MultiShrinkScroller.this.mPhotoViewContainer.getWidth();
                        MultiShrinkScroller.this.mIntermediateHeaderHeight = (int) (MultiShrinkScroller.this.mMaximumHeaderHeight * 0.6f);
                    }
                    MultiShrinkScroller.this.mMaximumPortraitHeaderHeight = MultiShrinkScroller.this.mIsTwoPanel ? MultiShrinkScroller.this.getHeight() : MultiShrinkScroller.this.mPhotoViewContainer.getWidth();
                    MultiShrinkScroller.this.setHeaderHeight(MultiShrinkScroller.this.getMaximumScrollableHeaderHeight());
                    if (z2) {
                        MultiShrinkScroller.this.mMaximumHeaderTextSize = MultiShrinkScroller.this.mTitleAndPhoneticNameView.getHeight();
                        MultiShrinkScroller.this.mMaximumFullNameViewHeight = MultiShrinkScroller.this.mFullNameView.getHeight();
                        MultiShrinkScroller.this.mMaximumPhoneticNameViewHeight = (MultiShrinkScroller.this.mMaximumFullNameViewHeight * MultiShrinkScroller.this.getResources().getDimensionPixelSize(R.dimen.quickcontact_maximum_phonetic_name_size)) / MultiShrinkScroller.this.getResources().getDimensionPixelSize(R.dimen.quickcontact_maximum_title_size);
                    }
                    if (i > 0) {
                        MultiShrinkScroller.this.mMaximumHeaderTextSize = i;
                    }
                    if (MultiShrinkScroller.this.mIsTwoPanel) {
                        MultiShrinkScroller.this.mMaximumHeaderHeight = MultiShrinkScroller.this.getHeight();
                        MultiShrinkScroller.this.mMinimumHeaderHeight = MultiShrinkScroller.this.mMaximumHeaderHeight;
                        MultiShrinkScroller.this.mIntermediateHeaderHeight = MultiShrinkScroller.this.mMaximumHeaderHeight;
                        ViewGroup.LayoutParams layoutParams = MultiShrinkScroller.this.mPhotoViewContainer.getLayoutParams();
                        layoutParams.height = MultiShrinkScroller.this.mMaximumHeaderHeight;
                        layoutParams.width = (int) (MultiShrinkScroller.this.mMaximumHeaderHeight * MultiShrinkScroller.this.mLandscapePhotoRatio);
                        MultiShrinkScroller.this.mPhotoViewContainer.setLayoutParams(layoutParams);
                        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) MultiShrinkScroller.this.mTitleAndPhoneticNameView.getLayoutParams();
                        layoutParams2.width = (layoutParams.width - layoutParams2.leftMargin) - layoutParams2.rightMargin;
                        layoutParams2.gravity = 8388691;
                        MultiShrinkScroller.this.mTitleAndPhoneticNameView.setLayoutParams(layoutParams2);
                    } else {
                        MultiShrinkScroller.this.mFullNameView.setWidth(MultiShrinkScroller.this.mPhotoViewContainer.getWidth() - (MultiShrinkScroller.this.mMaximumTitleMargin * 2));
                        MultiShrinkScroller.this.mPhoneticNameView.setWidth(MultiShrinkScroller.this.mPhotoViewContainer.getWidth() - (2 * MultiShrinkScroller.this.mMaximumTitleMargin));
                    }
                    MultiShrinkScroller.this.calculateCollapsedLargeTitlePadding();
                    MultiShrinkScroller.this.updateHeaderTextSizeAndMargin();
                    MultiShrinkScroller.this.configureGradientViewHeights();
                }
            }
        });
        ExtensionManager.getInstance();
        ExtensionManager.getViewCustomExtension().getQuickContactScrollerCustom().createJoynIconView((FrameLayout) findViewById(R.id.placeholder_container), this.mFullNameView, ((Activity) getContext()).getIntent().getData());
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().getQuickContactRcsScroller().createRcsIconView((FrameLayout) findViewById(R.id.placeholder_container), this.mFullNameView, ((Activity) getContext()).getIntent().getData());
    }

    private void configureGradientViewHeights() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mActionBarGradientView.getLayoutParams();
        layoutParams.height = this.mActionBarSize;
        this.mActionBarGradientView.setLayoutParams(layoutParams);
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) this.mTitleGradientView.getLayoutParams();
        layoutParams2.height = (int) ((this.mMaximumHeaderTextSize + ((FrameLayout.LayoutParams) this.mTitleAndPhoneticNameView.getLayoutParams()).bottomMargin) * 1.25f);
        this.mTitleGradientView.setLayoutParams(layoutParams2);
    }

    public void setTitle(String str, boolean z) {
        this.mFullNameView.setText(str);
        if (z) {
            this.mFullNameView.setTextDirection(3);
        }
        this.mPhotoTouchInterceptOverlay.setContentDescription(str);
    }

    public void setPhoneticName(String str) {
        if (this.mPhoneticNameView.getVisibility() == 0 && str.equals(this.mPhoneticNameView.getText())) {
            return;
        }
        this.mPhoneticNameView.setText(str);
        this.mPhoneticNameView.setVisibility(0);
        initialize(this.mListener, this.mIsOpenContactSquare, (this.mMaximumFullNameViewHeight * this.mFullNameView.getLineCount()) + (this.mMaximumPhoneticNameViewHeight * this.mPhoneticNameView.getLineCount()), false);
    }

    public void setPhoneticNameGone() {
        if (this.mPhoneticNameView.getVisibility() == 8) {
            return;
        }
        this.mPhoneticNameView.setVisibility(8);
        initialize(this.mListener, this.mIsOpenContactSquare, this.mMaximumFullNameViewHeight * this.mFullNameView.getLineCount(), false);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        return shouldStartDrag(motionEvent);
    }

    private boolean shouldStartDrag(MotionEvent motionEvent) {
        if (this.mIsTouchDisabledForDismissAnimation || this.mIsTouchDisabledForSuppressLayout) {
            return false;
        }
        if (this.mIsBeingDragged) {
            this.mIsBeingDragged = false;
            return false;
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            updateLastEventPosition(motionEvent);
            if (!this.mScroller.isFinished()) {
                startDrag();
                return true;
            }
            this.mReceivedDown = true;
        } else if (action == 2 && motionShouldStartDrag(motionEvent)) {
            updateLastEventPosition(motionEvent);
            startDrag();
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mIsTouchDisabledForDismissAnimation || this.mIsTouchDisabledForSuppressLayout) {
            return true;
        }
        int action = motionEvent.getAction();
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        if (!this.mIsBeingDragged) {
            if (shouldStartDrag(motionEvent) || action != 1 || !this.mReceivedDown) {
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
                return true;
            case 2:
                float fUpdatePositionAndComputeDelta = updatePositionAndComputeDelta(motionEvent);
                scrollTo(0, getScroll() + ((int) fUpdatePositionAndComputeDelta));
                this.mReceivedDown = false;
                if (this.mIsBeingDragged) {
                    if (fUpdatePositionAndComputeDelta > getMaximumScrollUpwards() - getScroll()) {
                        EdgeEffectCompat.onPull(this.mEdgeGlowBottom, fUpdatePositionAndComputeDelta / getHeight(), 1.0f - (motionEvent.getX() / getWidth()));
                    }
                    if (!this.mEdgeGlowBottom.isFinished()) {
                        postInvalidateOnAnimation();
                    }
                    if (shouldDismissOnScroll()) {
                        scrollOffBottom();
                    }
                }
                return true;
            default:
                return true;
        }
    }

    public void setHeaderTintColor(int i) {
        this.mHeaderTintColor = i;
        updatePhotoTintAndDropShadow();
        if (CompatUtils.isLollipopCompatible()) {
            this.mEdgeGlowBottom.setColor((i & 16777215) | Color.argb(Color.alpha(this.mEdgeGlowBottom.getColor()), 0, 0, 0));
            this.mEdgeGlowTop.setColor(this.mEdgeGlowBottom.getColor());
        }
    }

    private void expandHeader() {
        if (getHeaderHeight() != this.mMaximumHeaderHeight) {
            ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "headerHeight", this.mMaximumHeaderHeight);
            objectAnimatorOfInt.setDuration(300L);
            objectAnimatorOfInt.start();
            if (this.mScrollView.getScrollY() != 0) {
                ObjectAnimator.ofInt(this.mScrollView, "scrollY", -this.mScrollView.getScrollY()).start();
            }
        }
    }

    private void startDrag() {
        this.mIsBeingDragged = true;
        this.mScroller.abortAnimation();
    }

    private void stopDrag(boolean z) {
        this.mIsBeingDragged = false;
        if (!z && getChildCount() > 0) {
            float currentVelocity = getCurrentVelocity();
            if (currentVelocity > this.mMinimumVelocity || currentVelocity < (-this.mMinimumVelocity)) {
                fling(-currentVelocity);
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

    private void onDragFinished(int i) {
        if (getTransparentViewHeight() > 0 && !snapToTopOnDragFinished(i)) {
            snapToBottomOnDragFinished();
        }
    }

    private boolean snapToTopOnDragFinished(int i) {
        if (!this.mHasEverTouchedTheTop) {
            if (getTransparentViewHeight() - i < (-this.mSnapToTopSlopHeight) || getTransparentViewHeight() > this.mTransparentStartHeight) {
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

    private float getTransparentHeightRatio(int i) {
        return 1.0f - Math.max(Math.min(1.0f, i / getHeight()), ContactPhotoManager.OFFSET_DEFAULT);
    }

    public void scrollOffBottom() {
        this.mIsTouchDisabledForDismissAnimation = true;
        AcceleratingFlingInterpolator acceleratingFlingInterpolator = new AcceleratingFlingInterpolator(250, getCurrentVelocity(), getScrollUntilOffBottom());
        this.mScroller.forceFinished(true);
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "scroll", getScroll() - getScrollUntilOffBottom());
        objectAnimatorOfInt.setRepeatCount(0);
        objectAnimatorOfInt.setInterpolator(acceleratingFlingInterpolator);
        objectAnimatorOfInt.setDuration(250L);
        objectAnimatorOfInt.addListener(this.mSnapToBottomListener);
        objectAnimatorOfInt.start();
        if (this.mListener != null) {
            this.mListener.onStartScrollOffBottom();
        }
    }

    public void scrollUpForEntranceAnimation(boolean z) {
        int scroll = getScroll();
        int height = (scroll - (getHeight() - getTransparentViewHeight())) + 1;
        Interpolator interpolatorLoadInterpolator = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear_out_slow_in);
        final int transparentViewHeight = scroll + (z ? scroll : getTransparentViewHeight());
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "scroll", height, transparentViewHeight);
        objectAnimatorOfInt.setInterpolator(interpolatorLoadInterpolator);
        objectAnimatorOfInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (valueAnimator.getAnimatedValue().equals(Integer.valueOf(transparentViewHeight)) && MultiShrinkScroller.this.mListener != null) {
                    MultiShrinkScroller.this.mListener.onEntranceAnimationDone();
                }
            }
        });
        objectAnimatorOfInt.start();
    }

    @Override
    public void scrollTo(int i, int i2) {
        int scroll = i2 - getScroll();
        boolean z = getScrollNeededToBeFullScreen() <= 0;
        if (scroll > 0) {
            scrollUp(scroll);
        } else {
            scrollDown(scroll);
        }
        updatePhotoTintAndDropShadow();
        updateHeaderTextSizeAndMargin();
        boolean z2 = getScrollNeededToBeFullScreen() <= 0;
        this.mHasEverTouchedTheTop |= z2;
        if (this.mListener != null) {
            if (z && !z2) {
                this.mListener.onExitFullscreen();
            } else if (!z && z2) {
                this.mListener.onEnterFullscreen();
            }
            if (!z2 || !z) {
                this.mListener.onTransparentViewHeightChange(getTransparentHeightRatio(getTransparentViewHeight()));
            }
        }
    }

    public void setToolbarHeight(int i) {
        ViewGroup.LayoutParams layoutParams = this.mToolbar.getLayoutParams();
        layoutParams.height = i;
        this.mToolbar.setLayoutParams(layoutParams);
        updatePhotoTintAndDropShadow();
        updateHeaderTextSizeAndMargin();
    }

    public int getToolbarHeight() {
        return this.mToolbar.getLayoutParams().height;
    }

    public void setHeaderHeight(int i) {
        ViewGroup.LayoutParams layoutParams = this.mToolbar.getLayoutParams();
        layoutParams.height = i;
        this.mToolbar.setLayoutParams(layoutParams);
        updatePhotoTintAndDropShadow();
        updateHeaderTextSizeAndMargin();
    }

    public int getHeaderHeight() {
        return this.mToolbar.getLayoutParams().height;
    }

    public void setScroll(int i) {
        scrollTo(0, i);
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
            int scroll = getScroll();
            scrollTo(0, this.mScroller.getCurrY());
            int currY = this.mScroller.getCurrY() - scroll;
            int maximumScrollUpwards = getMaximumScrollUpwards() - getScroll();
            if (currY > maximumScrollUpwards && maximumScrollUpwards > 0) {
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
            int iSave = canvas.save();
            canvas.translate((-width) + getPaddingLeft(), (getMaximumScrollUpwards() + height) - getScroll());
            canvas.rotate(180.0f, width, ContactPhotoManager.OFFSET_DEFAULT);
            if (this.mIsTwoPanel) {
                this.mEdgeGlowBottom.setSize(this.mScrollView.getWidth(), height);
                if (getLayoutDirection() == 1) {
                    canvas.translate(this.mPhotoViewContainer.getWidth(), ContactPhotoManager.OFFSET_DEFAULT);
                }
            } else {
                this.mEdgeGlowBottom.setSize(width, height);
            }
            if (this.mEdgeGlowBottom.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(iSave);
        }
        if (!this.mEdgeGlowTop.isFinished()) {
            int iSave2 = canvas.save();
            if (this.mIsTwoPanel) {
                this.mEdgeGlowTop.setSize(this.mScrollView.getWidth(), height);
                if (getLayoutDirection() != 1) {
                    canvas.translate(this.mPhotoViewContainer.getWidth(), ContactPhotoManager.OFFSET_DEFAULT);
                }
            } else {
                this.mEdgeGlowTop.setSize(width, height);
            }
            if (this.mEdgeGlowTop.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(iSave2);
        }
    }

    private float getCurrentVelocity() {
        if (this.mVelocityTracker == null) {
            return ContactPhotoManager.OFFSET_DEFAULT;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
        return this.mVelocityTracker.getYVelocity();
    }

    private void fling(float f) {
        this.mScroller.fling(0, getScroll(), 0, (int) f, 0, 0, -2147483647, Integer.MAX_VALUE);
        if (f < ContactPhotoManager.OFFSET_DEFAULT && this.mTransparentView.getHeight() <= 0) {
            this.mIsFullscreenDownwardsFling = true;
        }
        invalidate();
    }

    private int getMaximumScrollUpwards() {
        if (!this.mIsTwoPanel) {
            return ((this.mTransparentStartHeight + getMaximumScrollableHeaderHeight()) - getFullyCompressedHeaderHeight()) + Math.max(0, (this.mScrollViewChild.getHeight() - getHeight()) + getFullyCompressedHeaderHeight());
        }
        return this.mTransparentStartHeight + Math.max(0, this.mScrollViewChild.getHeight() - getHeight());
    }

    private int getTransparentViewHeight() {
        return this.mTransparentView.getLayoutParams().height;
    }

    private void setTransparentViewHeight(int i) {
        this.mTransparentView.getLayoutParams().height = i;
        this.mTransparentView.setLayoutParams(this.mTransparentView.getLayoutParams());
    }

    private void scrollUp(int i) {
        if (getTransparentViewHeight() != 0) {
            int transparentViewHeight = getTransparentViewHeight();
            setTransparentViewHeight(getTransparentViewHeight() - i);
            setTransparentViewHeight(Math.max(0, getTransparentViewHeight()));
            i -= transparentViewHeight - getTransparentViewHeight();
        }
        ViewGroup.LayoutParams layoutParams = this.mToolbar.getLayoutParams();
        if (layoutParams.height > getFullyCompressedHeaderHeight()) {
            int i2 = layoutParams.height;
            layoutParams.height -= i;
            layoutParams.height = Math.max(layoutParams.height, getFullyCompressedHeaderHeight());
            this.mToolbar.setLayoutParams(layoutParams);
            i -= i2 - layoutParams.height;
        }
        this.mScrollView.scrollBy(0, i);
    }

    private int getFullyCompressedHeaderHeight() {
        return Math.min(Math.max(this.mToolbar.getLayoutParams().height - getOverflowingChildViewSize(), this.mMinimumHeaderHeight), getMaximumScrollableHeaderHeight());
    }

    private int getOverflowingChildViewSize() {
        return (-getHeight()) + this.mScrollViewChild.getHeight() + this.mToolbar.getLayoutParams().height;
    }

    private void scrollDown(int i) {
        if (this.mScrollView.getScrollY() > 0) {
            int scrollY = this.mScrollView.getScrollY();
            this.mScrollView.scrollBy(0, i);
            i -= this.mScrollView.getScrollY() - scrollY;
        }
        ViewGroup.LayoutParams layoutParams = this.mToolbar.getLayoutParams();
        if (layoutParams.height < getMaximumScrollableHeaderHeight()) {
            int i2 = layoutParams.height;
            layoutParams.height -= i;
            layoutParams.height = Math.min(layoutParams.height, getMaximumScrollableHeaderHeight());
            this.mToolbar.setLayoutParams(layoutParams);
            i -= i2 - layoutParams.height;
        }
        setTransparentViewHeight(getTransparentViewHeight() - i);
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
        if (this.mIsTwoPanel) {
            return;
        }
        if (getLayoutDirection() == 1) {
            this.mTitleAndPhoneticNameView.setPivotX(this.mTitleAndPhoneticNameView.getWidth());
        } else {
            this.mTitleAndPhoneticNameView.setPivotX(ContactPhotoManager.OFFSET_DEFAULT);
        }
        this.mTitleAndPhoneticNameView.setPivotY(this.mMaximumHeaderTextSize / 2);
        int i = this.mToolbar.getLayoutParams().height;
        this.mPhotoTouchInterceptOverlay.setClickable(i != this.mMaximumHeaderHeight);
        if (i >= this.mMaximumHeaderHeight) {
            this.mTitleAndPhoneticNameView.setScaleX(1.0f);
            this.mTitleAndPhoneticNameView.setScaleY(1.0f);
            setInterpolatedTitleMargins(1.0f);
            return;
        }
        float height = this.mInvisiblePlaceholderTextView.getHeight();
        float interpolation = this.mTextSizePathInterpolator.getInterpolation((i - this.mMinimumHeaderHeight) / (this.mMaximumHeaderHeight - this.mMinimumHeaderHeight));
        float f = (height + ((this.mMaximumHeaderTextSize - height) * interpolation)) / this.mMaximumHeaderTextSize;
        float fMin = Math.min(interpolation, 1.0f);
        float fMin2 = Math.min(f, 1.0f);
        this.mTitleAndPhoneticNameView.setScaleX(fMin2);
        this.mTitleAndPhoneticNameView.setScaleY(fMin2);
        setInterpolatedTitleMargins(fMin);
        ExtensionManager.getInstance();
        ExtensionManager.getViewCustomExtension().getQuickContactScrollerCustom().updateJoynIconView();
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().getQuickContactRcsScroller().updateRcsIconView();
    }

    private void calculateCollapsedLargeTitlePadding() {
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        this.mInvisiblePlaceholderTextView.getLocationOnScreen(iArr);
        this.mToolbar.getLocationOnScreen(iArr2);
        this.mCollapsedTitleBottomMargin = ((iArr[1] + (this.mInvisiblePlaceholderTextView.getHeight() / 2)) - iArr2[1]) - (this.mMaximumHeaderTextSize / 2);
    }

    private void setInterpolatedTitleMargins(float f) {
        int width;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mTitleAndPhoneticNameView.getLayoutParams();
        LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) this.mToolbar.getLayoutParams();
        if (this.mStartColumn != null) {
            width = this.mStartColumn.getWidth();
        } else {
            width = 0;
        }
        float f2 = 1.0f - f;
        layoutParams.setMarginStart(((int) ((this.mCollapsedTitleStartMargin * f2) + (this.mMaximumTitleMargin * f))) + width);
        layoutParams.topMargin = ((getTransparentViewHeight() + layoutParams2.height) - ((int) ((this.mCollapsedTitleBottomMargin * f2) + (this.mMaximumTitleMargin * f)))) - this.mMaximumHeaderTextSize;
        layoutParams.bottomMargin = 0;
        this.mTitleAndPhoneticNameView.setLayoutParams(layoutParams);
    }

    private void updatePhotoTintAndDropShadow() {
        Trace.beginSection("updatePhotoTintAndDropShadow");
        this.mPhotoView.setTint(this.mHeaderTintColor);
        int toolbarHeight = getToolbarHeight();
        if (toolbarHeight <= this.mMinimumHeaderHeight && !this.mIsTwoPanel) {
            ViewCompat.setElevation(this.mPhotoViewContainer, this.mToolbarElevation);
        } else {
            ViewCompat.setElevation(this.mPhotoViewContainer, ContactPhotoManager.OFFSET_DEFAULT);
        }
        this.mPhotoView.clearColorFilter();
        this.mColorMatrix.reset();
        int i = 0;
        if (!this.mPhotoView.isBasedOffLetterTile()) {
            double dCalculateHeightRatioToBlendingStartHeight = calculateHeightRatioToBlendingStartHeight(toolbarHeight);
            float fMin = 1.0f - ((float) Math.min(Math.pow(dCalculateHeightRatioToBlendingStartHeight, 1.5d) * 2.0d, 1.0d));
            float fMin2 = (float) Math.min(Math.pow(dCalculateHeightRatioToBlendingStartHeight, 1.5d) * 3.0d, 1.0d);
            this.mColorMatrix.setSaturation(fMin);
            this.mColorMatrix.postConcat(alphaMatrix(fMin, -1));
            this.mColorMatrix.postConcat(multiplyBlendMatrix(this.mHeaderTintColor, fMin2));
            i = (int) (255.0f * fMin);
        } else if (this.mIsTwoPanel) {
            this.mColorMatrix.reset();
            this.mColorMatrix.postConcat(alphaMatrix(0.8f, this.mHeaderTintColor));
        } else {
            float fCalculateHeightRatioToFullyOpen = calculateHeightRatioToFullyOpen(toolbarHeight);
            float fCalculateHeightRatioToFullyOpen2 = calculateHeightRatioToFullyOpen((int) (this.mMaximumPortraitHeaderHeight * 0.6f));
            this.mColorMatrix.postConcat(alphaMatrix(1.0f - ((float) Math.pow(Math.max(1.0f - (((1.0f - fCalculateHeightRatioToFullyOpen) / fCalculateHeightRatioToFullyOpen2) / ((float) (((double) ((1.0f - fCalculateHeightRatioToFullyOpen2) / fCalculateHeightRatioToFullyOpen2)) / (1.0d - Math.pow(0.19999998807907104d, 0.3333333432674408d))))), ContactPhotoManager.OFFSET_DEFAULT), 3.0d)), this.mHeaderTintColor));
        }
        this.mPhotoView.setColorFilter(new ColorMatrixColorFilter(this.mColorMatrix));
        this.mTitleGradientDrawable.setAlpha(i);
        this.mActionBarGradientDrawable.setAlpha(i);
        Trace.endSection();
    }

    private float calculateHeightRatioToFullyOpen(int i) {
        return (i - this.mMinimumPortraitHeaderHeight) / (this.mMaximumPortraitHeaderHeight - this.mMinimumPortraitHeaderHeight);
    }

    private float calculateHeightRatioToBlendingStartHeight(int i) {
        float f = this.mMaximumPortraitHeaderHeight * 0.5f;
        float f2 = f - this.mMinimumPortraitHeaderHeight;
        float f3 = i;
        if (f3 > f) {
            return ContactPhotoManager.OFFSET_DEFAULT;
        }
        return (f - f3) / f2;
    }

    private ColorMatrix alphaMatrix(float f, int i) {
        this.mAlphaMatrixValues[0] = (Color.red(i) * f) / 255.0f;
        this.mAlphaMatrixValues[6] = (Color.green(i) * f) / 255.0f;
        this.mAlphaMatrixValues[12] = (Color.blue(i) * f) / 255.0f;
        float f2 = 255.0f * (1.0f - f);
        this.mAlphaMatrixValues[4] = f2;
        this.mAlphaMatrixValues[9] = f2;
        this.mAlphaMatrixValues[14] = f2;
        this.mWhitenessColorMatrix.set(this.mAlphaMatrixValues);
        return this.mWhitenessColorMatrix;
    }

    private ColorMatrix multiplyBlendMatrix(int i, float f) {
        this.mMultiplyBlendMatrixValues[0] = multiplyBlend(Color.red(i), f);
        this.mMultiplyBlendMatrixValues[6] = multiplyBlend(Color.green(i), f);
        this.mMultiplyBlendMatrixValues[12] = multiplyBlend(Color.blue(i), f);
        this.mMultiplyBlendMatrix.set(this.mMultiplyBlendMatrixValues);
        return this.mMultiplyBlendMatrix;
    }

    private float multiplyBlend(int i, float f) {
        return ((i * f) / 255.0f) + (1.0f - f);
    }

    private void updateLastEventPosition(MotionEvent motionEvent) {
        this.mLastEventPosition[0] = motionEvent.getX();
        this.mLastEventPosition[1] = motionEvent.getY();
    }

    private boolean motionShouldStartDrag(MotionEvent motionEvent) {
        float y = motionEvent.getY() - this.mLastEventPosition[1];
        return y > ((float) this.mTouchSlop) || y < ((float) (-this.mTouchSlop));
    }

    private float updatePositionAndComputeDelta(MotionEvent motionEvent) {
        float f = this.mLastEventPosition[1];
        updateLastEventPosition(motionEvent);
        float height = 1.0f;
        if (f < this.mLastEventPosition[1] && this.mHasEverTouchedTheTop) {
            height = 1.0f + (this.mTransparentView.getHeight() * 0.01f);
        }
        return (f - this.mLastEventPosition[1]) / height;
    }

    private void smoothScrollBy(int i) {
        if (i == 0) {
            throw new IllegalArgumentException("Smooth scrolling by delta=0 is pointless and harmful");
        }
        this.mScroller.startScroll(0, getScroll(), 0, i);
        invalidate();
    }

    private class AcceleratingFlingInterpolator implements Interpolator {
        private final float mDurationMs;
        private final float mNumberFrames;
        private final int mPixelsDelta;
        private final float mStartingSpeedPixelsPerFrame;

        public AcceleratingFlingInterpolator(int i, float f, int i2) {
            this.mStartingSpeedPixelsPerFrame = f / getRefreshRate();
            this.mDurationMs = i;
            this.mPixelsDelta = i2;
            this.mNumberFrames = this.mDurationMs / getFrameIntervalMs();
        }

        @Override
        public float getInterpolation(float f) {
            float f2 = ((this.mNumberFrames * f) * this.mStartingSpeedPixelsPerFrame) / this.mPixelsDelta;
            if (this.mStartingSpeedPixelsPerFrame > ContactPhotoManager.OFFSET_DEFAULT) {
                return Math.min((f * f) + f2, 1.0f);
            }
            return Math.min((f * (f - f2)) + f2, 1.0f);
        }

        private float getRefreshRate() {
            return ((DisplayManager) MultiShrinkScroller.this.getContext().getSystemService("display")).getDisplay(0).getRefreshRate();
        }

        public long getFrameIntervalMs() {
            return (long) (1000.0f / getRefreshRate());
        }
    }

    public void prepareForShrinkingScrollChild(int i) {
        int i2 = (-getOverflowingChildViewSize()) + i;
        if (i2 > 0 && !this.mIsTwoPanel) {
            ObjectAnimator.ofInt(this, "toolbarHeight", Math.min(getToolbarHeight() + i2, getMaximumScrollableHeaderHeight())).setDuration(300L).start();
        }
    }

    public void setDisableTouchesForSuppressLayout(boolean z) {
        this.mIsTouchDisabledForSuppressLayout = z;
    }
}
