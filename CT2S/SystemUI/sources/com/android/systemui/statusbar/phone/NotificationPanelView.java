package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.util.Property;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.phone.KeyguardAffordanceHelper;
import com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm;
import com.android.systemui.statusbar.phone.ObservableScrollView;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class NotificationPanelView extends PanelView implements View.OnClickListener, ExpandableView.OnHeightChangedListener, KeyguardAffordanceHelper.Callback, ObservableScrollView.Listener, NotificationStackScrollLayout.OnEmptySpaceClickListener, NotificationStackScrollLayout.OnOverscrollTopChangedListener {
    private KeyguardAffordanceHelper mAfforanceHelper;
    private final Animator.AnimatorListener mAnimateHeaderSlidingInListener;
    private final Runnable mAnimateKeyguardBottomAreaInvisibleEndRunnable;
    private final Runnable mAnimateKeyguardStatusBarInvisibleEndRunnable;
    private final Runnable mAnimateKeyguardStatusViewInvisibleEndRunnable;
    private final Runnable mAnimateKeyguardStatusViewVisibleEndRunnable;
    private boolean mAnimateNextTopPaddingChange;
    private boolean mBlockTouches;
    private int mClockAnimationTarget;
    private ObjectAnimator mClockAnimator;
    private KeyguardClockPositionAlgorithm mClockPositionAlgorithm;
    private KeyguardClockPositionAlgorithm.Result mClockPositionResult;
    private TextView mClockView;
    private boolean mConflictingQsExpansionGesture;
    private Interpolator mDozeAnimationInterpolator;
    private boolean mDozing;
    private float mEmptyDragAmount;
    private Interpolator mFastOutLinearInterpolator;
    private Interpolator mFastOutSlowInInterpolator;
    private FlingAnimationUtils mFlingAnimationUtils;
    private StatusBarHeaderView mHeader;
    private boolean mHeaderAnimatingIn;
    private float mInitialHeightOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mIntercepting;
    private boolean mIsExpanding;
    private boolean mIsLaunchTransitionFinished;
    private boolean mIsLaunchTransitionRunning;
    private boolean mKeyguardShowing;
    private KeyguardStatusBarView mKeyguardStatusBar;
    private float mKeyguardStatusBarAnimateAlpha;
    private KeyguardStatusView mKeyguardStatusView;
    private boolean mKeyguardStatusViewAnimating;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mLastAnnouncementWasQuickSettings;
    private float mLastOverscroll;
    private float mLastTouchX;
    private float mLastTouchY;
    private Runnable mLaunchAnimationEndRunnable;
    private View mNotificationContainerParent;
    private int mNotificationScrimWaitDistance;
    private NotificationStackScrollLayout mNotificationStackScroller;
    private int mNotificationTopPadding;
    private int mNotificationsHeaderCollideDistance;
    private int mOldLayoutDirection;
    private boolean mOnlyAffordanceInThisMotion;
    private boolean mQsAnimatorExpand;
    private QSContainer mQsContainer;
    private ObjectAnimator mQsContainerAnimator;
    private final View.OnLayoutChangeListener mQsContainerAnimatorUpdater;
    private boolean mQsExpandImmediate;
    private boolean mQsExpanded;
    private boolean mQsExpandedWhenExpandingStarted;
    private ValueAnimator mQsExpansionAnimator;
    private boolean mQsExpansionEnabled;
    private boolean mQsExpansionFromOverscroll;
    private float mQsExpansionHeight;
    private int mQsFalsingThreshold;
    private boolean mQsFullyExpanded;
    private int mQsMaxExpansionHeight;
    private int mQsMinExpansionHeight;
    private View mQsNavbarScrim;
    private QSPanel mQsPanel;
    private int mQsPeekHeight;
    private boolean mQsScrimEnabled;
    private ValueAnimator mQsSizeChangeAnimator;
    private boolean mQsTouchAboveFalsingThreshold;
    private boolean mQsTracking;
    private View mReserveNotificationSpace;
    private ObservableScrollView mScrollView;
    private int mScrollYOverride;
    private SecureCameraLaunchManager mSecureCameraLaunchManager;
    private boolean mShadeEmpty;
    private boolean mStackScrollerOverscrolling;
    private final ViewTreeObserver.OnPreDrawListener mStartHeaderSlidingIn;
    private final ValueAnimator.AnimatorUpdateListener mStatusBarAnimateAlphaListener;
    private int mStatusBarMinHeight;
    private int mStatusBarState;
    private int mTopPaddingAdjustment;
    private int mTrackingPointer;
    private boolean mTwoFingerQsExpandPossible;
    private boolean mUnlockIconActive;
    private int mUnlockMoveDistance;
    private final Runnable mUpdateHeader;
    private VelocityTracker mVelocityTracker;

    public NotificationPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mQsExpansionEnabled = true;
        this.mClockAnimationTarget = -1;
        this.mClockPositionAlgorithm = new KeyguardClockPositionAlgorithm();
        this.mClockPositionResult = new KeyguardClockPositionAlgorithm.Result();
        this.mScrollYOverride = -1;
        this.mQsScrimEnabled = true;
        this.mKeyguardStatusBarAnimateAlpha = 1.0f;
        this.mAnimateKeyguardStatusViewInvisibleEndRunnable = new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
                NotificationPanelView.this.mKeyguardStatusView.setVisibility(8);
            }
        };
        this.mAnimateKeyguardStatusViewVisibleEndRunnable = new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
            }
        };
        this.mAnimateHeaderSlidingInListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mHeaderAnimatingIn = false;
                NotificationPanelView.this.mQsContainerAnimator = null;
                NotificationPanelView.this.mQsContainer.removeOnLayoutChangeListener(NotificationPanelView.this.mQsContainerAnimatorUpdater);
            }
        };
        this.mQsContainerAnimatorUpdater = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int oldHeight = oldBottom - oldTop;
                int height = bottom - top;
                if (height != oldHeight && NotificationPanelView.this.mQsContainerAnimator != null) {
                    PropertyValuesHolder[] values = NotificationPanelView.this.mQsContainerAnimator.getValues();
                    float newEndValue = ((NotificationPanelView.this.mHeader.getCollapsedHeight() + NotificationPanelView.this.mQsPeekHeight) - height) - top;
                    float newStartValue = (-height) - top;
                    values[0].setFloatValues(newStartValue, newEndValue);
                    NotificationPanelView.this.mQsContainerAnimator.setCurrentPlayTime(NotificationPanelView.this.mQsContainerAnimator.getCurrentPlayTime());
                }
            }
        };
        this.mStartHeaderSlidingIn = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                NotificationPanelView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                NotificationPanelView.this.mHeader.setTranslationY((-NotificationPanelView.this.mHeader.getCollapsedHeight()) - NotificationPanelView.this.mQsPeekHeight);
                NotificationPanelView.this.mHeader.animate().translationY(0.0f).setStartDelay(NotificationPanelView.this.mStatusBar.calculateGoingToFullShadeDelay()).setDuration(448L).setInterpolator(NotificationPanelView.this.mFastOutSlowInInterpolator).start();
                NotificationPanelView.this.mQsContainer.setY(-NotificationPanelView.this.mQsContainer.getHeight());
                NotificationPanelView.this.mQsContainerAnimator = ObjectAnimator.ofFloat(NotificationPanelView.this.mQsContainer, (Property<QSContainer, Float>) View.TRANSLATION_Y, NotificationPanelView.this.mQsContainer.getTranslationY(), ((NotificationPanelView.this.mHeader.getCollapsedHeight() + NotificationPanelView.this.mQsPeekHeight) - NotificationPanelView.this.mQsContainer.getHeight()) - NotificationPanelView.this.mQsContainer.getTop());
                NotificationPanelView.this.mQsContainerAnimator.setStartDelay(NotificationPanelView.this.mStatusBar.calculateGoingToFullShadeDelay());
                NotificationPanelView.this.mQsContainerAnimator.setDuration(448L);
                NotificationPanelView.this.mQsContainerAnimator.setInterpolator(NotificationPanelView.this.mFastOutSlowInInterpolator);
                NotificationPanelView.this.mQsContainerAnimator.addListener(NotificationPanelView.this.mAnimateHeaderSlidingInListener);
                NotificationPanelView.this.mQsContainerAnimator.start();
                NotificationPanelView.this.mQsContainer.addOnLayoutChangeListener(NotificationPanelView.this.mQsContainerAnimatorUpdater);
                return true;
            }
        };
        this.mAnimateKeyguardStatusBarInvisibleEndRunnable = new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mKeyguardStatusBar.setVisibility(4);
                NotificationPanelView.this.mKeyguardStatusBar.setAlpha(1.0f);
                NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = 1.0f;
            }
        };
        this.mStatusBarAnimateAlphaListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = NotificationPanelView.this.mKeyguardStatusBar.getAlpha();
            }
        };
        this.mAnimateKeyguardBottomAreaInvisibleEndRunnable = new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mKeyguardBottomArea.setVisibility(8);
            }
        };
        this.mUpdateHeader = new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mHeader.updateEverything();
            }
        };
        setWillNotDraw(true);
    }

    public void setStatusBar(PhoneStatusBar bar) {
        this.mStatusBar = bar;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHeader = (StatusBarHeaderView) findViewById(R.id.header);
        this.mHeader.setOnClickListener(this);
        this.mKeyguardStatusBar = (KeyguardStatusBarView) findViewById(R.id.keyguard_header);
        this.mKeyguardStatusView = (KeyguardStatusView) findViewById(R.id.keyguard_status_view);
        this.mQsContainer = (QSContainer) findViewById(R.id.quick_settings_container);
        this.mQsPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
        this.mClockView = (TextView) findViewById(R.id.clock_view);
        this.mScrollView = (ObservableScrollView) findViewById(R.id.scroll_view);
        this.mScrollView.setListener(this);
        this.mScrollView.setFocusable(false);
        this.mReserveNotificationSpace = findViewById(R.id.reserve_notification_space);
        this.mNotificationContainerParent = findViewById(R.id.notification_container_parent);
        this.mNotificationStackScroller = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationStackScroller.setOnHeightChangedListener(this);
        this.mNotificationStackScroller.setOverscrollTopChangedListener(this);
        this.mNotificationStackScroller.setOnEmptySpaceClickListener(this);
        this.mNotificationStackScroller.setScrollView(this.mScrollView);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.fast_out_slow_in);
        this.mFastOutLinearInterpolator = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.fast_out_linear_in);
        this.mDozeAnimationInterpolator = AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear_out_slow_in);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) findViewById(R.id.keyguard_bottom_area);
        this.mQsNavbarScrim = findViewById(R.id.qs_navbar_scrim);
        this.mAfforanceHelper = new KeyguardAffordanceHelper(this, getContext());
        this.mSecureCameraLaunchManager = new SecureCameraLaunchManager(getContext(), this.mKeyguardBottomArea);
        this.mQsContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int height = bottom - top;
                int oldHeight = oldBottom - oldTop;
                if (height != oldHeight) {
                    NotificationPanelView.this.onScrollChanged();
                }
            }
        });
    }

    @Override
    protected void loadDimens() {
        super.loadDimens();
        this.mNotificationTopPadding = getResources().getDimensionPixelSize(R.dimen.notifications_top_padding);
        this.mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.4f);
        this.mStatusBarMinHeight = getResources().getDimensionPixelSize(android.R.dimen.accessibility_focus_highlight_stroke_width);
        this.mQsPeekHeight = getResources().getDimensionPixelSize(R.dimen.qs_peek_height);
        this.mNotificationsHeaderCollideDistance = getResources().getDimensionPixelSize(R.dimen.header_notifications_collide_distance);
        this.mUnlockMoveDistance = getResources().getDimensionPixelOffset(R.dimen.unlock_move_distance);
        this.mClockPositionAlgorithm.loadDimens(getResources());
        this.mNotificationScrimWaitDistance = getResources().getDimensionPixelSize(R.dimen.notification_scrim_wait_distance);
        this.mQsFalsingThreshold = getResources().getDimensionPixelSize(R.dimen.qs_falsing_threshold);
    }

    public void updateResources() {
        int panelWidth = getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        int panelGravity = getResources().getInteger(R.integer.notification_panel_layout_gravity);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mHeader.getLayoutParams();
        if (lp.width != panelWidth) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            this.mHeader.setLayoutParams(lp);
            this.mHeader.post(this.mUpdateHeader);
        }
        FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) this.mNotificationStackScroller.getLayoutParams();
        if (lp2.width != panelWidth) {
            lp2.width = panelWidth;
            lp2.gravity = panelGravity;
            this.mNotificationStackScroller.setLayoutParams(lp2);
        }
        FrameLayout.LayoutParams lp3 = (FrameLayout.LayoutParams) this.mScrollView.getLayoutParams();
        if (lp3.width != panelWidth) {
            lp3.width = panelWidth;
            lp3.gravity = panelGravity;
            this.mScrollView.setLayoutParams(lp3);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mKeyguardStatusView.setPivotX(getWidth() / 2);
        this.mKeyguardStatusView.setPivotY(0.34521484f * this.mClockView.getTextSize());
        int oldMaxHeight = this.mQsMaxExpansionHeight;
        this.mQsMinExpansionHeight = this.mKeyguardShowing ? 0 : this.mHeader.getCollapsedHeight() + this.mQsPeekHeight;
        this.mQsMaxExpansionHeight = this.mHeader.getExpandedHeight() + this.mQsContainer.getDesiredHeight();
        positionClockAndNotifications();
        if (this.mQsExpanded && this.mQsFullyExpanded) {
            this.mQsExpansionHeight = this.mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
            if (this.mQsMaxExpansionHeight != oldMaxHeight) {
                startQsSizeChangeAnimation(oldMaxHeight, this.mQsMaxExpansionHeight);
            }
        } else if (!this.mQsExpanded) {
            setQsExpansion(this.mQsMinExpansionHeight + this.mLastOverscroll);
        }
        this.mNotificationStackScroller.setStackHeight(getExpandedHeight());
        updateHeader();
        this.mNotificationStackScroller.updateIsSmallScreen(this.mHeader.getCollapsedHeight() + this.mQsPeekHeight);
        if (this.mQsSizeChangeAnimator == null) {
            this.mQsContainer.setHeightOverride(this.mQsContainer.getDesiredHeight());
        }
    }

    @Override
    public void onAttachedToWindow() {
        this.mSecureCameraLaunchManager.create();
    }

    @Override
    public void onDetachedFromWindow() {
        this.mSecureCameraLaunchManager.destroy();
    }

    private void startQsSizeChangeAnimation(int oldHeight, int newHeight) {
        if (this.mQsSizeChangeAnimator != null) {
            oldHeight = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
            this.mQsSizeChangeAnimator.cancel();
        }
        this.mQsSizeChangeAnimator = ValueAnimator.ofInt(oldHeight, newHeight);
        this.mQsSizeChangeAnimator.setDuration(300L);
        this.mQsSizeChangeAnimator.setInterpolator(this.mFastOutSlowInInterpolator);
        this.mQsSizeChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationPanelView.this.requestScrollerTopPaddingUpdate(false);
                NotificationPanelView.this.requestPanelHeightUpdate();
                int height = ((Integer) NotificationPanelView.this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
                NotificationPanelView.this.mQsContainer.setHeightOverride(height - NotificationPanelView.this.mHeader.getExpandedHeight());
            }
        });
        this.mQsSizeChangeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mQsSizeChangeAnimator = null;
            }
        });
        this.mQsSizeChangeAnimator.start();
    }

    private void positionClockAndNotifications() {
        int stackScrollerPadding;
        boolean animate = this.mNotificationStackScroller.isAddOrRemoveAnimationPending();
        if (this.mStatusBarState != 1) {
            int bottom = this.mHeader.getCollapsedHeight();
            stackScrollerPadding = this.mStatusBarState == 0 ? this.mQsPeekHeight + bottom + this.mNotificationTopPadding : this.mKeyguardStatusBar.getHeight() + this.mNotificationTopPadding;
            this.mTopPaddingAdjustment = 0;
        } else {
            this.mClockPositionAlgorithm.setup(this.mStatusBar.getMaxKeyguardNotifications(), getMaxPanelHeight(), getExpandedHeight(), this.mNotificationStackScroller.getNotGoneChildCount(), getHeight(), this.mKeyguardStatusView.getHeight(), this.mEmptyDragAmount);
            this.mClockPositionAlgorithm.run(this.mClockPositionResult);
            if (animate || this.mClockAnimator != null) {
                startClockAnimation(this.mClockPositionResult.clockY);
            } else {
                this.mKeyguardStatusView.setY(this.mClockPositionResult.clockY);
            }
            updateClock(this.mClockPositionResult.clockAlpha, this.mClockPositionResult.clockScale);
            stackScrollerPadding = this.mClockPositionResult.stackScrollerPadding;
            this.mTopPaddingAdjustment = this.mClockPositionResult.stackScrollerPaddingAdjustment;
        }
        this.mNotificationStackScroller.setIntrinsicPadding(stackScrollerPadding);
        requestScrollerTopPaddingUpdate(animate);
    }

    private void startClockAnimation(int y) {
        if (this.mClockAnimationTarget != y) {
            this.mClockAnimationTarget = y;
            getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    NotificationPanelView.this.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (NotificationPanelView.this.mClockAnimator != null) {
                        NotificationPanelView.this.mClockAnimator.removeAllListeners();
                        NotificationPanelView.this.mClockAnimator.cancel();
                    }
                    NotificationPanelView.this.mClockAnimator = ObjectAnimator.ofFloat(NotificationPanelView.this.mKeyguardStatusView, (Property<KeyguardStatusView, Float>) View.Y, NotificationPanelView.this.mClockAnimationTarget);
                    NotificationPanelView.this.mClockAnimator.setInterpolator(NotificationPanelView.this.mFastOutSlowInInterpolator);
                    NotificationPanelView.this.mClockAnimator.setDuration(360L);
                    NotificationPanelView.this.mClockAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            NotificationPanelView.this.mClockAnimator = null;
                            NotificationPanelView.this.mClockAnimationTarget = -1;
                        }
                    });
                    NotificationPanelView.this.mClockAnimator.start();
                    return true;
                }
            });
        }
    }

    private void updateClock(float alpha, float scale) {
        if (!this.mKeyguardStatusViewAnimating) {
            this.mKeyguardStatusView.setAlpha(alpha);
        }
        this.mKeyguardStatusView.setScaleX(scale);
        this.mKeyguardStatusView.setScaleY(scale);
    }

    public void animateToFullShade(long delay) {
        this.mAnimateNextTopPaddingChange = true;
        this.mNotificationStackScroller.goToFullShade(delay);
        requestLayout();
    }

    public void setQsExpansionEnabled(boolean qsExpansionEnabled) {
        this.mQsExpansionEnabled = qsExpansionEnabled;
        this.mHeader.setClickable(qsExpansionEnabled);
    }

    @Override
    public void resetViews() {
        this.mIsLaunchTransitionFinished = false;
        this.mBlockTouches = false;
        this.mUnlockIconActive = false;
        this.mAfforanceHelper.reset(true);
        closeQs();
        this.mStatusBar.dismissPopups();
        this.mNotificationStackScroller.setOverScrollAmount(0.0f, true, false, true);
    }

    public void closeQs() {
        cancelAnimation();
        setQsExpansion(this.mQsMinExpansionHeight);
    }

    public void animateCloseQs() {
        if (this.mQsExpansionAnimator != null) {
            if (this.mQsAnimatorExpand) {
                float height = this.mQsExpansionHeight;
                this.mQsExpansionAnimator.cancel();
                setQsExpansion(height);
            } else {
                return;
            }
        }
        flingSettings(0.0f, false);
    }

    public void expandWithQs() {
        if (this.mQsExpansionEnabled) {
            this.mQsExpandImmediate = true;
        }
        expand();
    }

    @Override
    public void fling(float vel, boolean expand) {
        GestureRecorder gr = ((PhoneStatusBarView) this.mBar).mBar.getGestureRecorder();
        if (gr != null) {
            gr.tag("fling " + (vel > 0.0f ? "open" : "closed"), "notifications,v=" + vel);
        }
        super.fling(vel, expand);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != 32) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }
        event.getText().add(getKeyguardOrLockScreenString());
        this.mLastAnnouncementWasQuickSettings = false;
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mBlockTouches) {
            return false;
        }
        resetDownStates(event);
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        switch (event.getActionMasked()) {
            case 0:
                this.mIntercepting = true;
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                initVelocityTracker();
                trackMovement(event);
                if (shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, 0.0f)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (this.mQsExpansionAnimator != null) {
                    onQsExpansionStarted();
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    this.mQsTracking = true;
                    this.mIntercepting = false;
                    this.mNotificationStackScroller.removeLongPressCallback();
                }
                break;
            case 1:
            case 3:
                trackMovement(event);
                if (this.mQsTracking) {
                    flingQsWithCurrentVelocity(event.getActionMasked() == 3);
                    this.mQsTracking = false;
                }
                this.mIntercepting = false;
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                trackMovement(event);
                if (this.mQsTracking) {
                    setQsExpansion(this.mInitialHeightOnTouch + h);
                    trackMovement(event);
                    this.mIntercepting = false;
                    return true;
                }
                if (Math.abs(h) > this.mTouchSlop && Math.abs(h) > Math.abs(x - this.mInitialTouchX) && shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, h)) {
                    this.mQsTracking = true;
                    onQsExpansionStarted();
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    this.mIntercepting = false;
                    this.mNotificationStackScroller.removeLongPressCallback();
                    return true;
                }
                break;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) == upPointer ? 1 : 0;
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    this.mInitialTouchX = event.getX(newIndex);
                    this.mInitialTouchY = event.getY(newIndex);
                }
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    protected boolean isInContentBounds(float x, float y) {
        float yTransformed = y - this.mNotificationStackScroller.getY();
        float stackScrollerX = this.mNotificationStackScroller.getX();
        return this.mNotificationStackScroller.isInContentBounds(yTransformed) && stackScrollerX < x && x < ((float) this.mNotificationStackScroller.getWidth()) + stackScrollerX;
    }

    private void resetDownStates(MotionEvent event) {
        if (event.getActionMasked() == 0) {
            this.mOnlyAffordanceInThisMotion = false;
            this.mQsTouchAboveFalsingThreshold = this.mQsFullyExpanded;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (!this.mScrollView.isHandlingTouchEvent()) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private void flingQsWithCurrentVelocity(boolean isCancelMotionEvent) {
        float vel = getCurrentVelocity();
        flingSettings(vel, flingExpandsQs(vel) && !isCancelMotionEvent);
    }

    private boolean flingExpandsQs(float vel) {
        if (isBelowFalsingThreshold()) {
            return false;
        }
        return Math.abs(vel) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond() ? getQsExpansionFraction() > 0.5f : vel > 0.0f;
    }

    private boolean isBelowFalsingThreshold() {
        return !this.mQsTouchAboveFalsingThreshold && this.mStatusBarState == 1;
    }

    private float getQsExpansionFraction() {
        return Math.min(1.0f, (this.mQsExpansionHeight - this.mQsMinExpansionHeight) / (getTempQsMaxExpansion() - this.mQsMinExpansionHeight));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mBlockTouches) {
            return false;
        }
        resetDownStates(event);
        if ((!this.mIsExpanding || this.mHintAnimationRunning) && !this.mQsExpanded && this.mStatusBar.getBarState() != 0) {
            this.mAfforanceHelper.onTouchEvent(event);
        }
        if (this.mOnlyAffordanceInThisMotion) {
            return true;
        }
        if (event.getActionMasked() == 0 && getExpandedFraction() == 1.0f && this.mStatusBar.getBarState() != 1 && !this.mQsExpanded && this.mQsExpansionEnabled) {
            this.mQsTracking = true;
            this.mConflictingQsExpansionGesture = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = event.getX();
            this.mInitialTouchX = event.getY();
        }
        if (this.mExpandedHeight != 0.0f) {
            handleQsDown(event);
        }
        if (!this.mQsExpandImmediate && this.mQsTracking) {
            onQsTouch(event);
            if (!this.mConflictingQsExpansionGesture) {
                return true;
            }
        }
        if (event.getActionMasked() == 3 || event.getActionMasked() == 1) {
            this.mConflictingQsExpansionGesture = false;
        }
        if (event.getActionMasked() == 0 && this.mExpandedHeight == 0.0f && this.mQsExpansionEnabled) {
            this.mTwoFingerQsExpandPossible = true;
        }
        if (this.mTwoFingerQsExpandPossible && event.getActionMasked() == 5 && event.getPointerCount() == 2 && event.getY(event.getActionIndex()) < this.mStatusBarMinHeight) {
            this.mQsExpandImmediate = true;
            requestPanelHeightUpdate();
            setListening(true);
        }
        super.onTouchEvent(event);
        return true;
    }

    private boolean isInQsArea(float x, float y) {
        return x >= ((float) this.mScrollView.getLeft()) && x <= ((float) this.mScrollView.getRight()) && (y <= this.mNotificationStackScroller.getBottomMostNotificationBottom() || y <= this.mQsContainer.getY() + ((float) this.mQsContainer.getHeight()));
    }

    private void handleQsDown(MotionEvent event) {
        if (event.getActionMasked() == 0 && shouldQuickSettingsIntercept(event.getX(), event.getY(), -1.0f)) {
            this.mQsTracking = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = event.getX();
            this.mInitialTouchX = event.getY();
            if (this.mIsExpanding) {
                onExpandingFinished();
            }
        }
    }

    @Override
    protected boolean flingExpands(float vel, float vectorVel) {
        boolean expands = super.flingExpands(vel, vectorVel);
        if (this.mQsExpansionAnimator != null) {
            return true;
        }
        return expands;
    }

    @Override
    protected boolean hasConflictingGestures() {
        return this.mStatusBar.getBarState() != 0;
    }

    private void onQsTouch(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float y = event.getY(pointerIndex);
        float x = event.getX(pointerIndex);
        switch (event.getActionMasked()) {
            case 0:
                this.mQsTracking = true;
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                onQsExpansionStarted();
                this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                initVelocityTracker();
                trackMovement(event);
                break;
            case 1:
            case 3:
                this.mQsTracking = false;
                this.mTrackingPointer = -1;
                trackMovement(event);
                float fraction = getQsExpansionFraction();
                if ((fraction != 0.0f || y >= this.mInitialTouchY) && (fraction != 1.0f || y <= this.mInitialTouchY)) {
                    flingQsWithCurrentVelocity(event.getActionMasked() == 3);
                } else {
                    this.mScrollYOverride = -1;
                }
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                setQsExpansion(this.mInitialHeightOnTouch + h);
                if (h >= getFalsingThreshold()) {
                    this.mQsTouchAboveFalsingThreshold = true;
                }
                trackMovement(event);
                break;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) == upPointer ? 1 : 0;
                    float newY = event.getY(newIndex);
                    float newX = event.getX(newIndex);
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    this.mInitialTouchY = newY;
                    this.mInitialTouchX = newX;
                }
                break;
        }
    }

    private int getFalsingThreshold() {
        float factor = this.mStatusBar.isScreenOnComingFromTouch() ? 1.5f : 1.0f;
        return (int) (this.mQsFalsingThreshold * factor);
    }

    @Override
    public void onOverscrolled(float lastTouchX, float lastTouchY, int amount) {
        if (this.mIntercepting && shouldQuickSettingsIntercept(lastTouchX, lastTouchY, -1.0f)) {
            this.mQsTracking = true;
            onQsExpansionStarted(amount);
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = this.mLastTouchY;
            this.mInitialTouchX = this.mLastTouchX;
        }
    }

    @Override
    public void onOverscrollTopChanged(float amount, boolean isRubberbanded) {
        cancelAnimation();
        if (!this.mQsExpansionEnabled) {
            amount = 0.0f;
        }
        float rounded = amount >= 1.0f ? amount : 0.0f;
        this.mStackScrollerOverscrolling = rounded != 0.0f && isRubberbanded;
        this.mQsExpansionFromOverscroll = rounded != 0.0f;
        this.mLastOverscroll = rounded;
        updateQsState();
        setQsExpansion(this.mQsMinExpansionHeight + rounded);
    }

    @Override
    public void flingTopOverscroll(float velocity, boolean open) {
        this.mLastOverscroll = 0.0f;
        setQsExpansion(this.mQsExpansionHeight);
        if (!this.mQsExpansionEnabled && open) {
            velocity = 0.0f;
        }
        flingSettings(velocity, open && this.mQsExpansionEnabled, new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mStackScrollerOverscrolling = false;
                NotificationPanelView.this.mQsExpansionFromOverscroll = false;
                NotificationPanelView.this.updateQsState();
            }
        });
    }

    private void onQsExpansionStarted() {
        onQsExpansionStarted(0);
    }

    private void onQsExpansionStarted(int overscrollAmount) {
        cancelAnimation();
        float height = (this.mQsExpansionHeight - this.mScrollView.getScrollY()) - overscrollAmount;
        if (this.mScrollView.getScrollY() != 0) {
            this.mScrollYOverride = this.mScrollView.getScrollY();
        }
        this.mScrollView.scrollTo(0, 0);
        setQsExpansion(height);
        requestPanelHeightUpdate();
    }

    private void setQsExpanded(boolean expanded) {
        boolean changed = this.mQsExpanded != expanded;
        if (changed) {
            this.mQsExpanded = expanded;
            updateQsState();
            requestPanelHeightUpdate();
            this.mNotificationStackScroller.setInterceptDelegateEnabled(expanded);
            this.mStatusBar.setQsExpanded(expanded);
            this.mQsPanel.setExpanded(expanded);
        }
    }

    public void setBarState(int statusBarState, boolean keyguardFadingAway, boolean goingToFullShade) {
        boolean keyguardShowing = true;
        if (statusBarState != 1 && statusBarState != 2) {
            keyguardShowing = false;
        }
        if (!this.mKeyguardShowing && keyguardShowing) {
            setQsTranslation(this.mQsExpansionHeight);
            this.mHeader.setTranslationY(0.0f);
        }
        setKeyguardStatusViewVisibility(statusBarState, keyguardFadingAway, goingToFullShade);
        setKeyguardBottomAreaVisibility(statusBarState, goingToFullShade);
        if (goingToFullShade) {
            animateKeyguardStatusBarOut();
        } else {
            this.mKeyguardStatusBar.setAlpha(1.0f);
            this.mKeyguardStatusBar.setVisibility(keyguardShowing ? 0 : 4);
        }
        this.mStatusBarState = statusBarState;
        this.mKeyguardShowing = keyguardShowing;
        updateQsState();
        if (goingToFullShade) {
            animateHeaderSlidingIn();
        }
    }

    private void animateHeaderSlidingIn() {
        this.mHeaderAnimatingIn = true;
        getViewTreeObserver().addOnPreDrawListener(this.mStartHeaderSlidingIn);
    }

    private void animateKeyguardStatusBarOut() {
        this.mKeyguardStatusBar.animate().alpha(0.0f).setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).setInterpolator(PhoneStatusBar.ALPHA_OUT).setUpdateListener(this.mStatusBarAnimateAlphaListener).withEndAction(this.mAnimateKeyguardStatusBarInvisibleEndRunnable).start();
    }

    private void animateKeyguardStatusBarIn() {
        this.mKeyguardStatusBar.setVisibility(0);
        this.mKeyguardStatusBar.setAlpha(0.0f);
        this.mKeyguardStatusBar.animate().alpha(1.0f).setStartDelay(0L).setDuration(700L).setInterpolator(this.mDozeAnimationInterpolator).setUpdateListener(this.mStatusBarAnimateAlphaListener).start();
    }

    private void setKeyguardBottomAreaVisibility(int statusBarState, boolean goingToFullShade) {
        if (goingToFullShade) {
            this.mKeyguardBottomArea.animate().cancel();
            this.mKeyguardBottomArea.animate().alpha(0.0f).setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).setInterpolator(PhoneStatusBar.ALPHA_OUT).withEndAction(this.mAnimateKeyguardBottomAreaInvisibleEndRunnable).start();
        } else if (statusBarState == 1 || statusBarState == 2) {
            this.mKeyguardBottomArea.animate().cancel();
            this.mKeyguardBottomArea.setVisibility(0);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        } else {
            this.mKeyguardBottomArea.animate().cancel();
            this.mKeyguardBottomArea.setVisibility(8);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        }
    }

    private void setKeyguardStatusViewVisibility(int statusBarState, boolean keyguardFadingAway, boolean goingToFullShade) {
        if ((!keyguardFadingAway && this.mStatusBarState == 1 && statusBarState != 1) || goingToFullShade) {
            this.mKeyguardStatusView.animate().cancel();
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.animate().alpha(0.0f).setStartDelay(0L).setDuration(160L).setInterpolator(PhoneStatusBar.ALPHA_OUT).withEndAction(this.mAnimateKeyguardStatusViewInvisibleEndRunnable);
            if (keyguardFadingAway) {
                this.mKeyguardStatusView.animate().setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).start();
                return;
            }
            return;
        }
        if (this.mStatusBarState == 2 && statusBarState == 1) {
            this.mKeyguardStatusView.animate().cancel();
            this.mKeyguardStatusView.setVisibility(0);
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.setAlpha(0.0f);
            this.mKeyguardStatusView.animate().alpha(1.0f).setStartDelay(0L).setDuration(320L).setInterpolator(PhoneStatusBar.ALPHA_IN).withEndAction(this.mAnimateKeyguardStatusViewVisibleEndRunnable);
            return;
        }
        if (statusBarState == 1) {
            this.mKeyguardStatusView.animate().cancel();
            this.mKeyguardStatusViewAnimating = false;
            this.mKeyguardStatusView.setVisibility(0);
            this.mKeyguardStatusView.setAlpha(1.0f);
            return;
        }
        this.mKeyguardStatusView.animate().cancel();
        this.mKeyguardStatusViewAnimating = false;
        this.mKeyguardStatusView.setVisibility(8);
        this.mKeyguardStatusView.setAlpha(1.0f);
    }

    private void updateQsState() {
        boolean expandVisually = this.mQsExpanded || this.mStackScrollerOverscrolling;
        this.mHeader.setVisibility((this.mQsExpanded || !this.mKeyguardShowing) ? 0 : 4);
        this.mHeader.setExpanded(this.mKeyguardShowing || (this.mQsExpanded && !this.mStackScrollerOverscrolling));
        this.mNotificationStackScroller.setScrollingEnabled(this.mStatusBarState != 1 && (!this.mQsExpanded || this.mQsExpansionFromOverscroll));
        this.mQsPanel.setVisibility(expandVisually ? 0 : 4);
        this.mQsContainer.setVisibility((!this.mKeyguardShowing || expandVisually) ? 0 : 4);
        this.mScrollView.setTouchEnabled(this.mQsExpanded);
        updateEmptyShadeView();
        this.mQsNavbarScrim.setVisibility((this.mStatusBarState == 0 && this.mQsExpanded && !this.mStackScrollerOverscrolling && this.mQsScrimEnabled) ? 0 : 4);
        if (this.mKeyguardUserSwitcher != null && this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            this.mKeyguardUserSwitcher.hideIfNotSimple(true);
        }
    }

    private void setQsExpansion(float height) {
        float height2 = Math.min(Math.max(height, this.mQsMinExpansionHeight), this.mQsMaxExpansionHeight);
        this.mQsFullyExpanded = height2 == ((float) this.mQsMaxExpansionHeight);
        if (height2 > this.mQsMinExpansionHeight && !this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            setQsExpanded(true);
        } else if (height2 <= this.mQsMinExpansionHeight && this.mQsExpanded) {
            setQsExpanded(false);
            if (this.mLastAnnouncementWasQuickSettings && !this.mTracking) {
                announceForAccessibility(getKeyguardOrLockScreenString());
                this.mLastAnnouncementWasQuickSettings = false;
            }
        }
        this.mQsExpansionHeight = height2;
        this.mHeader.setExpansion(getHeaderExpansionFraction());
        setQsTranslation(height2);
        requestScrollerTopPaddingUpdate(false);
        updateNotificationScrim(height2);
        if (this.mKeyguardShowing) {
            updateHeaderKeyguard();
        }
        if (this.mStatusBarState == 0 && this.mQsExpanded && !this.mStackScrollerOverscrolling && this.mQsScrimEnabled) {
            this.mQsNavbarScrim.setAlpha(getQsExpansionFraction());
        }
        if (height2 != 0.0f && this.mQsFullyExpanded && !this.mLastAnnouncementWasQuickSettings) {
            announceForAccessibility(getContext().getString(R.string.accessibility_desc_quick_settings));
            this.mLastAnnouncementWasQuickSettings = true;
        }
    }

    private String getKeyguardOrLockScreenString() {
        return this.mStatusBarState == 1 ? getContext().getString(R.string.accessibility_desc_lock_screen) : getContext().getString(R.string.accessibility_desc_notification_shade);
    }

    private void updateNotificationScrim(float height) {
        int startDistance = this.mQsMinExpansionHeight + this.mNotificationScrimWaitDistance;
        float progress = (height - startDistance) / (this.mQsMaxExpansionHeight - startDistance);
        Math.max(0.0f, Math.min(progress, 1.0f));
    }

    private float getHeaderExpansionFraction() {
        if (this.mKeyguardShowing) {
            return 1.0f;
        }
        return getQsExpansionFraction();
    }

    private void setQsTranslation(float height) {
        if (!this.mHeaderAnimatingIn) {
            this.mQsContainer.setY((height - this.mQsContainer.getDesiredHeight()) + getHeaderTranslation());
        }
        if (this.mKeyguardShowing) {
            this.mHeader.setY(interpolate(getQsExpansionFraction(), -this.mHeader.getHeight(), 0.0f));
        }
    }

    private float calculateQsTopPadding() {
        if (this.mKeyguardShowing && (this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted))) {
            int maxNotifications = (this.mClockPositionResult.stackScrollerPadding - this.mClockPositionResult.stackScrollerPaddingAdjustment) - this.mNotificationTopPadding;
            int maxQs = getTempQsMaxExpansion();
            int max = this.mStatusBarState == 1 ? Math.max(maxNotifications, maxQs) : maxQs;
            return (int) interpolate(getExpandedFraction(), this.mQsMinExpansionHeight, max);
        }
        if (this.mQsSizeChangeAnimator != null) {
            return ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        }
        if (this.mKeyguardShowing && this.mScrollYOverride == -1) {
            return interpolate(getQsExpansionFraction(), this.mNotificationStackScroller.getIntrinsicPadding() - this.mNotificationTopPadding, this.mQsMaxExpansionHeight);
        }
        return this.mQsExpansionHeight;
    }

    private void requestScrollerTopPaddingUpdate(boolean animate) {
        boolean z = true;
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        float fCalculateQsTopPadding = calculateQsTopPadding();
        int scrollY = this.mScrollView.getScrollY();
        boolean z2 = this.mAnimateNextTopPaddingChange || animate;
        if (!this.mKeyguardShowing || (!this.mQsExpandImmediate && (!this.mIsExpanding || !this.mQsExpandedWhenExpandingStarted))) {
            z = false;
        }
        notificationStackScrollLayout.updateTopPadding(fCalculateQsTopPadding, scrollY, z2, z);
        this.mAnimateNextTopPaddingChange = false;
    }

    private void trackMovement(MotionEvent event) {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
        this.mLastTouchX = event.getX();
        this.mLastTouchY = event.getY();
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentVelocity() {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000);
        return this.mVelocityTracker.getYVelocity();
    }

    private void cancelAnimation() {
        if (this.mQsExpansionAnimator != null) {
            this.mQsExpansionAnimator.cancel();
        }
    }

    private void flingSettings(float vel, boolean expand) {
        flingSettings(vel, expand, null);
    }

    private void flingSettings(float vel, boolean expand, final Runnable onFinishRunnable) {
        float target = expand ? this.mQsMaxExpansionHeight : this.mQsMinExpansionHeight;
        if (target == this.mQsExpansionHeight) {
            this.mScrollYOverride = -1;
            if (onFinishRunnable != null) {
                onFinishRunnable.run();
                return;
            }
            return;
        }
        boolean belowFalsingThreshold = isBelowFalsingThreshold();
        if (belowFalsingThreshold) {
            vel = 0.0f;
        }
        this.mScrollView.setBlockFlinging(true);
        ValueAnimator animator = ValueAnimator.ofFloat(this.mQsExpansionHeight, target);
        this.mFlingAnimationUtils.apply(animator, this.mQsExpansionHeight, target, vel);
        if (belowFalsingThreshold) {
            animator.setDuration(350L);
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationPanelView.this.setQsExpansion(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mScrollView.setBlockFlinging(false);
                NotificationPanelView.this.mScrollYOverride = -1;
                NotificationPanelView.this.mQsExpansionAnimator = null;
                if (onFinishRunnable != null) {
                    onFinishRunnable.run();
                }
            }
        });
        animator.start();
        this.mQsExpansionAnimator = animator;
        this.mQsAnimatorExpand = expand;
    }

    private boolean shouldQuickSettingsIntercept(float x, float y, float yDiff) {
        if (!this.mQsExpansionEnabled) {
            return false;
        }
        View header = this.mKeyguardShowing ? this.mKeyguardStatusBar : this.mHeader;
        boolean onHeader = x >= ((float) header.getLeft()) && x <= ((float) header.getRight()) && y >= ((float) header.getTop()) && y <= ((float) header.getBottom());
        return this.mQsExpanded ? onHeader || (this.mScrollView.isScrolledToBottom() && yDiff < 0.0f && isInQsArea(x, y)) : onHeader;
    }

    @Override
    protected boolean isScrolledToBottom() {
        if (isInSettings()) {
            return this.mScrollView.isScrolledToBottom();
        }
        return this.mStatusBar.getBarState() == 1 || this.mNotificationStackScroller.isScrolledToBottom();
    }

    @Override
    protected int getMaxPanelHeight() {
        int maxHeight;
        int min = this.mStatusBarMinHeight;
        if (this.mStatusBar.getBarState() != 1 && this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            int minHeight = (int) ((this.mQsMinExpansionHeight + getOverExpansionAmount()) * 2.05f);
            min = Math.max(min, minHeight);
        }
        if (this.mQsExpandImmediate || this.mQsExpanded || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) {
            maxHeight = calculatePanelHeightQsExpanded();
        } else {
            maxHeight = calculatePanelHeightShade();
        }
        return Math.max(maxHeight, min);
    }

    private boolean isInSettings() {
        return this.mQsExpanded;
    }

    @Override
    protected void onHeightUpdated(float expandedHeight) {
        float t;
        if (!this.mQsExpanded || this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) {
            positionClockAndNotifications();
        }
        if (this.mQsExpandImmediate || (this.mQsExpanded && !this.mQsTracking && this.mQsExpansionAnimator == null && !this.mQsExpansionFromOverscroll)) {
            if (this.mKeyguardShowing) {
                t = expandedHeight / getMaxPanelHeight();
            } else {
                float panelHeightQsCollapsed = this.mNotificationStackScroller.getIntrinsicPadding() + this.mNotificationStackScroller.getMinStackHeight();
                float panelHeightQsExpanded = calculatePanelHeightQsExpanded();
                t = (expandedHeight - panelHeightQsCollapsed) / (panelHeightQsExpanded - panelHeightQsCollapsed);
            }
            setQsExpansion(this.mQsMinExpansionHeight + ((getTempQsMaxExpansion() - this.mQsMinExpansionHeight) * t));
        }
        this.mNotificationStackScroller.setStackHeight(expandedHeight);
        updateHeader();
        updateUnlockIcon();
        updateNotificationTranslucency();
    }

    private int getTempQsMaxExpansion() {
        int qsTempMaxExpansion = this.mQsMaxExpansionHeight;
        if (this.mScrollYOverride != -1) {
            return qsTempMaxExpansion - this.mScrollYOverride;
        }
        return qsTempMaxExpansion;
    }

    private int calculatePanelHeightShade() {
        int emptyBottomMargin = this.mNotificationStackScroller.getEmptyBottomMargin();
        int maxHeight = (this.mNotificationStackScroller.getHeight() - emptyBottomMargin) - this.mTopPaddingAdjustment;
        return (int) (maxHeight + this.mNotificationStackScroller.getTopPaddingOverflow());
    }

    private int calculatePanelHeightQsExpanded() {
        float notificationHeight = (this.mNotificationStackScroller.getHeight() - this.mNotificationStackScroller.getEmptyBottomMargin()) - this.mNotificationStackScroller.getTopPadding();
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0 && this.mShadeEmpty) {
            notificationHeight = this.mNotificationStackScroller.getEmptyShadeViewHeight() + this.mNotificationStackScroller.getBottomStackPeekSize() + this.mNotificationStackScroller.getCollapseSecondCardPadding();
        }
        int maxQsHeight = this.mQsMaxExpansionHeight;
        if (this.mQsSizeChangeAnimator != null) {
            maxQsHeight = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        }
        float totalHeight = Math.max(maxQsHeight + this.mNotificationStackScroller.getNotificationTopPadding(), this.mStatusBarState == 1 ? this.mClockPositionResult.stackScrollerPadding - this.mTopPaddingAdjustment : 0) + notificationHeight;
        if (totalHeight > this.mNotificationStackScroller.getHeight()) {
            float fullyCollapsedHeight = ((this.mNotificationStackScroller.getMinStackHeight() + maxQsHeight) + this.mNotificationStackScroller.getNotificationTopPadding()) - getScrollViewScrollY();
            totalHeight = Math.max(fullyCollapsedHeight, this.mNotificationStackScroller.getHeight());
        }
        return (int) totalHeight;
    }

    private int getScrollViewScrollY() {
        return (this.mScrollYOverride == -1 || this.mQsTracking) ? this.mScrollView.getScrollY() : this.mScrollYOverride;
    }

    private void updateNotificationTranslucency() {
        float alpha = (getNotificationsTopY() + this.mNotificationStackScroller.getItemHeight()) / ((this.mQsMinExpansionHeight + this.mNotificationStackScroller.getBottomStackPeekSize()) - this.mNotificationStackScroller.getCollapseSecondCardPadding());
        float alpha2 = (float) Math.pow(Math.max(0.0f, Math.min(alpha, 1.0f)), 0.75d);
        if (alpha2 != 1.0f && this.mNotificationStackScroller.getLayerType() != 2) {
            this.mNotificationStackScroller.setLayerType(2, null);
        } else if (alpha2 == 1.0f && this.mNotificationStackScroller.getLayerType() == 2) {
            this.mNotificationStackScroller.setLayerType(0, null);
        }
        this.mNotificationStackScroller.setAlpha(alpha2);
    }

    @Override
    protected float getOverExpansionAmount() {
        return this.mNotificationStackScroller.getCurrentOverScrollAmount(true);
    }

    @Override
    protected float getOverExpansionPixels() {
        return this.mNotificationStackScroller.getCurrentOverScrolledPixels(true);
    }

    private void updateUnlockIcon() {
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            boolean active = ((float) getMaxPanelHeight()) - getExpandedHeight() > ((float) this.mUnlockMoveDistance);
            KeyguardAffordanceView lockIcon = this.mKeyguardBottomArea.getLockIcon();
            if (active && !this.mUnlockIconActive && this.mTracking) {
                lockIcon.setImageAlpha(1.0f, true, 150L, this.mFastOutLinearInterpolator, null);
                lockIcon.setImageScale(1.2f, true, 150L, this.mFastOutLinearInterpolator);
            } else if (!active && this.mUnlockIconActive && this.mTracking) {
                lockIcon.setImageAlpha(0.5f, true, 150L, this.mFastOutLinearInterpolator, null);
                lockIcon.setImageScale(1.0f, true, 150L, this.mFastOutLinearInterpolator);
            }
            this.mUnlockIconActive = active;
        }
    }

    private void updateHeader() {
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            updateHeaderKeyguard();
        } else {
            updateHeaderShade();
        }
    }

    private void updateHeaderShade() {
        if (!this.mHeaderAnimatingIn) {
            this.mHeader.setTranslationY(getHeaderTranslation());
        }
        setQsTranslation(this.mQsExpansionHeight);
    }

    private float getHeaderTranslation() {
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            return 0.0f;
        }
        if (this.mNotificationStackScroller.getNotGoneChildCount() != 0) {
            return Math.min(0.0f, this.mNotificationStackScroller.getTranslationY()) / 2.05f;
        }
        if (this.mExpandedHeight / 2.05f < this.mQsMinExpansionHeight) {
            return (this.mExpandedHeight / 2.05f) - this.mQsMinExpansionHeight;
        }
        return 0.0f;
    }

    private void updateHeaderKeyguard() {
        float alphaNotifications;
        if (this.mStatusBar.getBarState() == 1) {
            alphaNotifications = getNotificationsTopY() / (this.mKeyguardStatusBar.getHeight() + this.mNotificationsHeaderCollideDistance);
        } else {
            alphaNotifications = getNotificationsTopY() / this.mKeyguardStatusBar.getHeight();
        }
        float alphaNotifications2 = (float) Math.pow(MathUtils.constrain(alphaNotifications, 0.0f, 1.0f), 0.75d);
        float alphaQsExpansion = 1.0f - Math.min(1.0f, getQsExpansionFraction() * 2.0f);
        this.mKeyguardStatusBar.setAlpha(Math.min(alphaNotifications2, alphaQsExpansion) * this.mKeyguardStatusBarAnimateAlpha);
        this.mKeyguardBottomArea.setAlpha(Math.min(1.0f - getQsExpansionFraction(), alphaNotifications2));
        setQsTranslation(this.mQsExpansionHeight);
    }

    private float getNotificationsTopY() {
        return this.mNotificationStackScroller.getNotGoneChildCount() == 0 ? getExpandedHeight() : this.mNotificationStackScroller.getNotificationsTopY();
    }

    @Override
    protected void onExpandingStarted() {
        super.onExpandingStarted();
        this.mNotificationStackScroller.onExpansionStarted();
        this.mIsExpanding = true;
        this.mQsExpandedWhenExpandingStarted = this.mQsFullyExpanded;
        if (this.mQsExpanded) {
            onQsExpansionStarted();
        }
    }

    @Override
    protected void onExpandingFinished() {
        super.onExpandingFinished();
        this.mNotificationStackScroller.onExpansionStopped();
        this.mIsExpanding = false;
        this.mScrollYOverride = -1;
        if (this.mExpandedHeight == 0.0f) {
            setListening(false);
        } else {
            setListening(true);
        }
        this.mQsExpandImmediate = false;
        this.mTwoFingerQsExpandPossible = false;
    }

    private void setListening(boolean listening) {
        this.mHeader.setListening(listening);
        this.mKeyguardStatusBar.setListening(listening);
        this.mQsPanel.setListening(listening);
    }

    @Override
    public void instantExpand() {
        super.instantExpand();
        setListening(true);
    }

    @Override
    protected void setOverExpansion(float overExpansion, boolean isPixels) {
        if (!this.mConflictingQsExpansionGesture && !this.mQsExpandImmediate && this.mStatusBar.getBarState() != 1) {
            this.mNotificationStackScroller.setOnHeightChangedListener(null);
            if (isPixels) {
                this.mNotificationStackScroller.setOverScrolledPixels(overExpansion, true, false);
            } else {
                this.mNotificationStackScroller.setOverScrollAmount(overExpansion, true, false);
            }
            this.mNotificationStackScroller.setOnHeightChangedListener(this);
        }
    }

    @Override
    protected void onTrackingStarted() {
        super.onTrackingStarted();
        if (this.mQsFullyExpanded) {
            this.mQsExpandImmediate = true;
        }
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            this.mAfforanceHelper.animateHideLeftRightIcon();
        }
    }

    @Override
    protected void onTrackingStopped(boolean expand) {
        super.onTrackingStopped(expand);
        if (expand) {
            this.mNotificationStackScroller.setOverScrolledPixels(0.0f, true, true);
        }
        if (expand && ((this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) && !this.mHintAnimationRunning)) {
            this.mAfforanceHelper.reset(true);
        }
        if (expand) {
            return;
        }
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            KeyguardAffordanceView lockIcon = this.mKeyguardBottomArea.getLockIcon();
            lockIcon.setImageAlpha(0.0f, true, 100L, this.mFastOutLinearInterpolator, null);
            lockIcon.setImageScale(2.0f, true, 100L, this.mFastOutLinearInterpolator);
        }
    }

    @Override
    public void onHeightChanged(ExpandableView view) {
        if (view != null || !this.mQsExpanded) {
            requestPanelHeightUpdate();
        }
    }

    @Override
    public void onReset(ExpandableView view) {
    }

    @Override
    public void onScrollChanged() {
        if (this.mQsExpanded) {
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mAfforanceHelper.onConfigurationChanged();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (layoutDirection != this.mOldLayoutDirection) {
            this.mAfforanceHelper.onRtlPropertiesChanged();
            this.mOldLayoutDirection = layoutDirection;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.mHeader) {
            onQsExpansionStarted();
            if (this.mQsExpanded) {
                flingSettings(0.0f, false);
            } else if (this.mQsExpansionEnabled) {
                flingSettings(0.0f, true);
            }
        }
    }

    @Override
    public void onAnimationToSideStarted(boolean rightPage, float translation, float vel) {
        boolean start;
        if (getLayoutDirection() == 1) {
            start = rightPage;
        } else {
            start = !rightPage;
        }
        this.mIsLaunchTransitionRunning = true;
        this.mLaunchAnimationEndRunnable = null;
        float displayDensity = this.mStatusBar.getDisplayDensity();
        int lengthDp = Math.abs((int) (translation / displayDensity));
        int velocityDp = Math.abs((int) (vel / displayDensity));
        if (start) {
            EventLogTags.writeSysuiLockscreenGesture(5, lengthDp, velocityDp);
            this.mKeyguardBottomArea.launchPhone();
        } else {
            EventLogTags.writeSysuiLockscreenGesture(4, lengthDp, velocityDp);
            this.mSecureCameraLaunchManager.startSecureCameraLaunch();
        }
        this.mStatusBar.startLaunchTransitionTimeout();
        this.mBlockTouches = true;
    }

    @Override
    public void onAnimationToSideEnded() {
        this.mIsLaunchTransitionRunning = false;
        this.mIsLaunchTransitionFinished = true;
        if (this.mLaunchAnimationEndRunnable != null) {
            this.mLaunchAnimationEndRunnable.run();
            this.mLaunchAnimationEndRunnable = null;
        }
    }

    @Override
    protected void onEdgeClicked(boolean right) {
        boolean start = true;
        if (!right || getRightIcon().getVisibility() == 0) {
            if ((right || getLeftIcon().getVisibility() == 0) && !isDozing()) {
                this.mHintAnimationRunning = true;
                this.mAfforanceHelper.startHintAnimation(right, new Runnable() {
                    @Override
                    public void run() {
                        NotificationPanelView.this.mHintAnimationRunning = false;
                        NotificationPanelView.this.mStatusBar.onHintFinished();
                    }
                });
                if (getLayoutDirection() == 1) {
                    start = right;
                } else if (right) {
                    start = false;
                }
                if (start) {
                    this.mStatusBar.onPhoneHintStarted();
                } else {
                    this.mStatusBar.onCameraHintStarted();
                }
            }
        }
    }

    @Override
    protected void startUnlockHintAnimation() {
        super.startUnlockHintAnimation();
        startHighlightIconAnimation(getCenterIcon());
    }

    private void startHighlightIconAnimation(final KeyguardAffordanceView icon) {
        icon.setImageAlpha(1.0f, true, 200L, this.mFastOutSlowInInterpolator, new Runnable() {
            @Override
            public void run() {
                icon.setImageAlpha(0.5f, true, 200L, NotificationPanelView.this.mFastOutSlowInInterpolator, null);
            }
        });
    }

    @Override
    public float getPageWidth() {
        return getWidth();
    }

    @Override
    public void onSwipingStarted() {
        this.mSecureCameraLaunchManager.onSwipingStarted();
        requestDisallowInterceptTouchEvent(true);
        this.mOnlyAffordanceInThisMotion = true;
    }

    @Override
    public KeyguardAffordanceView getLeftIcon() {
        return getLayoutDirection() == 1 ? this.mKeyguardBottomArea.getCameraView() : this.mKeyguardBottomArea.getPhoneView();
    }

    @Override
    public KeyguardAffordanceView getCenterIcon() {
        return this.mKeyguardBottomArea.getLockIcon();
    }

    @Override
    public KeyguardAffordanceView getRightIcon() {
        return getLayoutDirection() == 1 ? this.mKeyguardBottomArea.getPhoneView() : this.mKeyguardBottomArea.getCameraView();
    }

    @Override
    public View getLeftPreview() {
        return getLayoutDirection() == 1 ? this.mKeyguardBottomArea.getCameraPreview() : this.mKeyguardBottomArea.getPhonePreview();
    }

    @Override
    public View getRightPreview() {
        return getLayoutDirection() == 1 ? this.mKeyguardBottomArea.getPhonePreview() : this.mKeyguardBottomArea.getCameraPreview();
    }

    @Override
    public float getAffordanceFalsingFactor() {
        return this.mStatusBar.isScreenOnComingFromTouch() ? 1.5f : 1.0f;
    }

    @Override
    protected float getPeekHeight() {
        return this.mNotificationStackScroller.getNotGoneChildCount() > 0 ? this.mNotificationStackScroller.getPeekHeight() : this.mQsMinExpansionHeight * 2.05f;
    }

    @Override
    protected float getCannedFlingDurationFactor() {
        return this.mQsExpanded ? 0.7f : 0.6f;
    }

    @Override
    protected boolean fullyExpandedClearAllVisible() {
        return this.mNotificationStackScroller.isDismissViewNotGone() && this.mNotificationStackScroller.isScrolledToBottom() && !this.mQsExpandImmediate;
    }

    @Override
    protected boolean isClearAllVisible() {
        return this.mNotificationStackScroller.isDismissViewVisible();
    }

    @Override
    protected int getClearAllHeight() {
        return this.mNotificationStackScroller.getDismissViewHeight();
    }

    @Override
    protected boolean isTrackingBlocked() {
        return this.mConflictingQsExpansionGesture && this.mQsExpanded;
    }

    public void notifyVisibleChildrenChanged() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() != 0) {
            this.mReserveNotificationSpace.setVisibility(0);
        } else {
            this.mReserveNotificationSpace.setVisibility(8);
        }
    }

    public boolean isQsExpanded() {
        return this.mQsExpanded;
    }

    public boolean isQsDetailShowing() {
        return this.mQsPanel.isShowingDetail();
    }

    public void closeQsDetail() {
        this.mQsPanel.closeDetail();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public boolean isLaunchTransitionFinished() {
        return this.mIsLaunchTransitionFinished;
    }

    public boolean isLaunchTransitionRunning() {
        return this.mIsLaunchTransitionRunning;
    }

    public void setLaunchTransitionEndRunnable(Runnable r) {
        this.mLaunchAnimationEndRunnable = r;
    }

    public void setEmptyDragAmount(float amount) {
        float factor = 0.8f;
        if (this.mNotificationStackScroller.getNotGoneChildCount() > 0 || !this.mStatusBar.hasActiveNotifications()) {
            factor = 0.4f;
        }
        this.mEmptyDragAmount = amount * factor;
        positionClockAndNotifications();
    }

    private static float interpolate(float t, float start, float end) {
        return ((1.0f - t) * start) + (t * end);
    }

    public void setDozing(boolean dozing, boolean animate) {
        if (dozing != this.mDozing) {
            this.mDozing = dozing;
            if (this.mDozing) {
                this.mKeyguardStatusBar.setVisibility(4);
                this.mKeyguardBottomArea.setVisibility(4);
                return;
            }
            this.mKeyguardBottomArea.setVisibility(0);
            this.mKeyguardStatusBar.setVisibility(0);
            if (animate) {
                animateKeyguardStatusBarIn();
                this.mKeyguardBottomArea.startFinishDozeAnimation();
            }
        }
    }

    @Override
    public boolean isDozing() {
        return this.mDozing;
    }

    public void setShadeEmpty(boolean shadeEmpty) {
        this.mShadeEmpty = shadeEmpty;
        updateEmptyShadeView();
    }

    private void updateEmptyShadeView() {
        this.mNotificationStackScroller.updateEmptyShadeView(this.mShadeEmpty && !this.mQsExpanded);
    }

    public void setQsScrimEnabled(boolean qsScrimEnabled) {
        boolean changed = this.mQsScrimEnabled != qsScrimEnabled;
        this.mQsScrimEnabled = qsScrimEnabled;
        if (changed) {
            updateQsState();
        }
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void onScreenTurnedOn() {
        this.mKeyguardStatusView.refreshTime();
    }

    @Override
    public void onEmptySpaceClicked(float x, float y) {
        onEmptySpaceClick(x);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }
}
