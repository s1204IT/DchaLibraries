package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.BenesseExtension;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.MathUtils;
import android.util.Property;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.DejankUtils;
import com.android.systemui.EventLogTags;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.KeyguardAffordanceHelper;
import com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.List;

public class NotificationPanelView extends PanelView implements ExpandableView.OnHeightChangedListener, View.OnClickListener, NotificationStackScrollLayout.OnOverscrollTopChangedListener, KeyguardAffordanceHelper.Callback, NotificationStackScrollLayout.OnEmptySpaceClickListener, HeadsUpManager.OnHeadsUpChangedListener {
    private static final Rect mDummyDirtyRect = new Rect(0, 0, 1, 1);
    private KeyguardAffordanceHelper mAfforanceHelper;
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
    private boolean mClosingWithAlphaFadeOut;
    private boolean mCollapsedOnDown;
    private boolean mConflictingQsExpansionGesture;
    private boolean mDozing;
    private boolean mDozingOnDown;
    private float mEmptyDragAmount;
    private boolean mExpandingFromHeadsUp;
    private FalsingManager mFalsingManager;
    private FlingAnimationUtils mFlingAnimationUtils;
    private NotificationGroupManager mGroupManager;
    private boolean mHeadsUpAnimatingAway;
    private Runnable mHeadsUpExistenceChangedRunnable;
    private HeadsUpTouchHelper mHeadsUpTouchHelper;
    private float mInitialHeightOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mIntercepting;
    private boolean mIsExpanding;
    private boolean mIsExpansionFromHeadsUp;
    private boolean mIsLaunchTransitionFinished;
    private boolean mIsLaunchTransitionRunning;
    private boolean mKeyguardShowing;
    private KeyguardStatusBarView mKeyguardStatusBar;
    private float mKeyguardStatusBarAnimateAlpha;
    private KeyguardStatusView mKeyguardStatusView;
    private boolean mKeyguardStatusViewAnimating;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private boolean mLastAnnouncementWasQuickSettings;
    private String mLastCameraLaunchSource;
    private int mLastOrientation;
    private float mLastOverscroll;
    private float mLastTouchX;
    private float mLastTouchY;
    private Runnable mLaunchAnimationEndRunnable;
    private boolean mLaunchingAffordance;
    private boolean mListenForHeadsUp;
    private int mNavigationBarBottomHeight;
    protected NotificationsQuickSettingsContainer mNotificationContainerParent;
    private int mNotificationScrimWaitDistance;
    protected NotificationStackScrollLayout mNotificationStackScroller;
    private int mNotificationsHeaderCollideDistance;
    private int mOldLayoutDirection;
    private boolean mOnlyAffordanceInThisMotion;
    private boolean mPanelExpanded;
    private int mPositionMinSideMargin;
    private boolean mQsAnimatorExpand;
    private AutoReinflateContainer mQsAutoReinflateContainer;
    protected QSContainer mQsContainer;
    private boolean mQsExpandImmediate;
    private boolean mQsExpanded;
    private boolean mQsExpandedWhenExpandingStarted;
    private ValueAnimator mQsExpansionAnimator;
    protected boolean mQsExpansionEnabled;
    private boolean mQsExpansionFromOverscroll;
    protected float mQsExpansionHeight;
    private int mQsFalsingThreshold;
    private boolean mQsFullyExpanded;
    protected int mQsMaxExpansionHeight;
    protected int mQsMinExpansionHeight;
    private View mQsNavbarScrim;
    private int mQsPeekHeight;
    private boolean mQsScrimEnabled;
    private ValueAnimator mQsSizeChangeAnimator;
    private boolean mQsTouchAboveFalsingThreshold;
    private boolean mQsTracking;
    private boolean mShadeEmpty;
    private boolean mStackScrollerOverscrolling;
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
        this.mQsScrimEnabled = true;
        this.mKeyguardStatusBarAnimateAlpha = 1.0f;
        this.mLastOrientation = -1;
        this.mLastCameraLaunchSource = "lockscreen_affordance";
        this.mHeadsUpExistenceChangedRunnable = new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mHeadsUpAnimatingAway = false;
                NotificationPanelView.this.notifyBarPanelExpansionChanged();
            }
        };
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
                NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = ((Float) animation.getAnimatedValue()).floatValue();
                NotificationPanelView.this.updateHeaderKeyguardAlpha();
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
                NotificationPanelView.this.mQsContainer.getHeader().updateEverything();
            }
        };
        setWillNotDraw(true);
        this.mFalsingManager = FalsingManager.getInstance(context);
    }

    public void setStatusBar(PhoneStatusBar bar) {
        this.mStatusBar = bar;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mKeyguardStatusBar = (KeyguardStatusBarView) findViewById(R.id.keyguard_header);
        this.mKeyguardStatusView = (KeyguardStatusView) findViewById(R.id.keyguard_status_view);
        this.mClockView = (TextView) findViewById(R.id.clock_view);
        this.mNotificationContainerParent = (NotificationsQuickSettingsContainer) findViewById(R.id.notification_container_parent);
        this.mNotificationStackScroller = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationStackScroller.setOnHeightChangedListener(this);
        this.mNotificationStackScroller.setOverscrollTopChangedListener(this);
        this.mNotificationStackScroller.setOnEmptySpaceClickListener(this);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) findViewById(R.id.keyguard_bottom_area);
        this.mQsNavbarScrim = findViewById(R.id.qs_navbar_scrim);
        this.mAfforanceHelper = new KeyguardAffordanceHelper(this, getContext());
        this.mLastOrientation = getResources().getConfiguration().orientation;
        this.mQsAutoReinflateContainer = (AutoReinflateContainer) findViewById(R.id.qs_auto_reinflate_container);
        this.mQsAutoReinflateContainer.addInflateListener(new AutoReinflateContainer.InflateListener() {
            @Override
            public void onInflated(View v) {
                NotificationPanelView.this.mQsContainer = (QSContainer) v.findViewById(R.id.quick_settings_container);
                NotificationPanelView.this.mQsContainer.setPanelView(NotificationPanelView.this);
                NotificationPanelView.this.mQsContainer.getHeader().findViewById(R.id.expand_indicator).setOnClickListener(NotificationPanelView.this);
                NotificationPanelView.this.mQsContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v2, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        int height = bottom - top;
                        int oldHeight = oldBottom - oldTop;
                        if (height == oldHeight) {
                            return;
                        }
                        NotificationPanelView.this.onQsHeightChanged();
                    }
                });
                NotificationPanelView.this.mNotificationStackScroller.setQsContainer(NotificationPanelView.this.mQsContainer);
            }
        });
    }

    @Override
    protected void loadDimens() {
        super.loadDimens();
        this.mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.4f);
        this.mStatusBarMinHeight = getResources().getDimensionPixelSize(android.R.dimen.accessibility_touch_slop);
        this.mQsPeekHeight = getResources().getDimensionPixelSize(R.dimen.qs_peek_height);
        this.mNotificationsHeaderCollideDistance = getResources().getDimensionPixelSize(R.dimen.header_notifications_collide_distance);
        this.mUnlockMoveDistance = getResources().getDimensionPixelOffset(R.dimen.unlock_move_distance);
        this.mClockPositionAlgorithm.loadDimens(getResources());
        this.mNotificationScrimWaitDistance = getResources().getDimensionPixelSize(R.dimen.notification_scrim_wait_distance);
        this.mQsFalsingThreshold = getResources().getDimensionPixelSize(R.dimen.qs_falsing_threshold);
        this.mPositionMinSideMargin = getResources().getDimensionPixelSize(R.dimen.notification_panel_min_side_margin);
    }

    public void updateResources() {
        int panelWidth = getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        int panelGravity = getResources().getInteger(R.integer.notification_panel_layout_gravity);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mQsAutoReinflateContainer.getLayoutParams();
        if (lp.width != panelWidth) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            this.mQsAutoReinflateContainer.setLayoutParams(lp);
            this.mQsContainer.post(this.mUpdateHeader);
        }
        FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) this.mNotificationStackScroller.getLayoutParams();
        if (lp2.width == panelWidth) {
            return;
        }
        lp2.width = panelWidth;
        lp2.gravity = panelGravity;
        this.mNotificationStackScroller.setLayoutParams(lp2);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mKeyguardStatusView.setPivotX(getWidth() / 2);
        this.mKeyguardStatusView.setPivotY(this.mClockView.getTextSize() * 0.34521484f);
        int oldMaxHeight = this.mQsMaxExpansionHeight;
        this.mQsMinExpansionHeight = this.mKeyguardShowing ? 0 : this.mQsContainer.getQsMinExpansionHeight();
        this.mQsMaxExpansionHeight = this.mQsContainer.getDesiredHeight();
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
        updateStackHeight(getExpandedHeight());
        updateHeader();
        if (this.mQsSizeChangeAnimator == null) {
            this.mQsContainer.setHeightOverride(this.mQsContainer.getDesiredHeight());
        }
        updateMaxHeadsUpTranslation();
    }

    private void startQsSizeChangeAnimation(int oldHeight, int newHeight) {
        if (this.mQsSizeChangeAnimator != null) {
            oldHeight = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
            this.mQsSizeChangeAnimator.cancel();
        }
        this.mQsSizeChangeAnimator = ValueAnimator.ofInt(oldHeight, newHeight);
        this.mQsSizeChangeAnimator.setDuration(300L);
        this.mQsSizeChangeAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        this.mQsSizeChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationPanelView.this.requestScrollerTopPaddingUpdate(false);
                NotificationPanelView.this.requestPanelHeightUpdate();
                int height = ((Integer) NotificationPanelView.this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
                NotificationPanelView.this.mQsContainer.setHeightOverride(height);
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
            int bottom = this.mQsContainer.getHeader().getHeight();
            if (this.mStatusBarState == 0) {
                stackScrollerPadding = bottom + this.mQsPeekHeight;
            } else {
                stackScrollerPadding = this.mKeyguardStatusBar.getHeight();
            }
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

    public int computeMaxKeyguardNotifications(int maximum) {
        float minPadding = this.mClockPositionAlgorithm.getMinStackScrollerPadding(getHeight(), this.mKeyguardStatusView.getHeight());
        int notificationPadding = Math.max(1, getResources().getDimensionPixelSize(R.dimen.notification_divider_height));
        int overflowheight = getResources().getDimensionPixelSize(R.dimen.notification_summary_height);
        float bottomStackSize = this.mNotificationStackScroller.getKeyguardBottomStackSize();
        float availableSpace = ((this.mNotificationStackScroller.getHeight() - minPadding) - overflowheight) - bottomStackSize;
        int count = 0;
        for (int i = 0; i < this.mNotificationStackScroller.getChildCount(); i++) {
            ExpandableView child = (ExpandableView) this.mNotificationStackScroller.getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                boolean suppressedSummary = this.mGroupManager.isSummaryOfSuppressedGroup(row.getStatusBarNotification());
                if (!suppressedSummary && this.mStatusBar.shouldShowOnKeyguard(row.getStatusBarNotification()) && !row.isRemoved()) {
                    availableSpace -= child.getMinHeight() + notificationPadding;
                    if (availableSpace >= 0.0f && count < maximum) {
                        count++;
                    } else {
                        return count;
                    }
                }
            }
        }
        return count;
    }

    private void startClockAnimation(int y) {
        if (this.mClockAnimationTarget == y) {
            return;
        }
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
                NotificationPanelView.this.mClockAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
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
        this.mQsContainer.setHeaderClickable(qsExpansionEnabled);
    }

    @Override
    public void resetViews() {
        this.mIsLaunchTransitionFinished = false;
        this.mBlockTouches = false;
        this.mUnlockIconActive = false;
        if (!this.mLaunchingAffordance) {
            this.mAfforanceHelper.reset(false);
            this.mLastCameraLaunchSource = "lockscreen_affordance";
        }
        closeQs();
        this.mStatusBar.dismissPopups();
        this.mNotificationStackScroller.setOverScrollAmount(0.0f, true, false, true);
        this.mNotificationStackScroller.resetScrollPosition();
    }

    public void closeQs() {
        cancelQsAnimation();
        setQsExpansion(this.mQsMinExpansionHeight);
    }

    public void animateCloseQs() {
        if (this.mQsExpansionAnimator != null) {
            if (!this.mQsAnimatorExpand) {
                return;
            }
            float height = this.mQsExpansionHeight;
            this.mQsExpansionAnimator.cancel();
            setQsExpansion(height);
        }
        flingSettings(0.0f, false);
    }

    public void expandWithQs() {
        if (this.mQsExpansionEnabled) {
            this.mQsExpandImmediate = true;
        }
        expand(true);
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
    protected void flingToHeight(float vel, boolean expand, float target, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        boolean z = false;
        this.mHeadsUpTouchHelper.notifyFling(!expand);
        if (!expand && getFadeoutAlpha() == 1.0f) {
            z = true;
        }
        setClosingWithAlphaFadeout(z);
        super.flingToHeight(vel, expand, target, collapseSpeedUpFactor, expandBecauseOfFalsing);
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        if (event.getEventType() == 32) {
            event.getText().add(getKeyguardOrLockScreenString());
            this.mLastAnnouncementWasQuickSettings = false;
            return true;
        }
        return super.dispatchPopulateAccessibilityEventInternal(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (BenesseExtension.getDchaState() != 0 || this.mBlockTouches || this.mQsContainer.isCustomizing()) {
            return false;
        }
        initDownStates(event);
        if (this.mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open", 1);
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
            return true;
        }
        if (isFullyCollapsed() || !onQsIntercept(event)) {
            return super.onInterceptTouchEvent(event);
        }
        return true;
    }

    private boolean onQsIntercept(MotionEvent motionEvent) {
        int iFindPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
        if (iFindPointerIndex < 0) {
            iFindPointerIndex = 0;
            this.mTrackingPointer = motionEvent.getPointerId(0);
        }
        float x = motionEvent.getX(iFindPointerIndex);
        float y = motionEvent.getY(iFindPointerIndex);
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.mIntercepting = true;
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                initVelocityTracker();
                trackMovement(motionEvent);
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
                return false;
            case 1:
            case 3:
                trackMovement(motionEvent);
                if (this.mQsTracking) {
                    flingQsWithCurrentVelocity(y, motionEvent.getActionMasked() == 3);
                    this.mQsTracking = false;
                }
                this.mIntercepting = false;
                return false;
            case 2:
                float f = y - this.mInitialTouchY;
                trackMovement(motionEvent);
                if (this.mQsTracking) {
                    setQsExpansion(this.mInitialHeightOnTouch + f);
                    trackMovement(motionEvent);
                    this.mIntercepting = false;
                    return true;
                }
                if (Math.abs(f) > this.mTouchSlop && Math.abs(f) > Math.abs(x - this.mInitialTouchX) && shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, f)) {
                    this.mQsTracking = true;
                    onQsExpansionStarted();
                    notifyExpandingFinished();
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    this.mIntercepting = false;
                    this.mNotificationStackScroller.removeLongPressCallback();
                    return true;
                }
                return false;
            case 4:
            case 5:
            default:
                return false;
            case 6:
                int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                if (this.mTrackingPointer == pointerId) {
                    int i = motionEvent.getPointerId(0) != pointerId ? 0 : 1;
                    this.mTrackingPointer = motionEvent.getPointerId(i);
                    this.mInitialTouchX = motionEvent.getX(i);
                    this.mInitialTouchY = motionEvent.getY(i);
                }
                return false;
        }
    }

    @Override
    protected boolean isInContentBounds(float x, float y) {
        float stackScrollerX = this.mNotificationStackScroller.getX();
        return !this.mNotificationStackScroller.isBelowLastNotification(x - stackScrollerX, y) && stackScrollerX < x && x < ((float) this.mNotificationStackScroller.getWidth()) + stackScrollerX;
    }

    private void initDownStates(MotionEvent event) {
        if (event.getActionMasked() != 0) {
            return;
        }
        this.mOnlyAffordanceInThisMotion = false;
        this.mQsTouchAboveFalsingThreshold = this.mQsFullyExpanded;
        this.mDozingOnDown = isDozing();
        this.mCollapsedOnDown = isFullyCollapsed();
        this.mListenForHeadsUp = this.mCollapsedOnDown ? this.mHeadsUpManager.hasPinnedHeadsUp() : false;
    }

    private void flingQsWithCurrentVelocity(float y, boolean isCancelMotionEvent) {
        boolean z = false;
        float vel = getCurrentVelocity();
        boolean expandsQs = flingExpandsQs(vel);
        if (expandsQs) {
            logQsSwipeDown(y);
        }
        if (expandsQs && !isCancelMotionEvent) {
            z = true;
        }
        flingSettings(vel, z);
    }

    private void logQsSwipeDown(float y) {
        int gesture;
        float vel = getCurrentVelocity();
        if (this.mStatusBarState == 1) {
            gesture = 8;
        } else {
            gesture = 9;
        }
        EventLogTags.writeSysuiLockscreenGesture(gesture, (int) ((y - this.mInitialTouchY) / this.mStatusBar.getDisplayDensity()), (int) (vel / this.mStatusBar.getDisplayDensity()));
    }

    private boolean flingExpandsQs(float vel) {
        if (isFalseTouch()) {
            return false;
        }
        return Math.abs(vel) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond() ? getQsExpansionFraction() > 0.5f : vel > 0.0f;
    }

    private boolean isFalseTouch() {
        if (!needsAntiFalsing()) {
            return false;
        }
        if (this.mFalsingManager.isClassiferEnabled()) {
            return this.mFalsingManager.isFalseTouch();
        }
        return !this.mQsTouchAboveFalsingThreshold;
    }

    private float getQsExpansionFraction() {
        return Math.min(1.0f, (this.mQsExpansionHeight - this.mQsMinExpansionHeight) / (getTempQsMaxExpansion() - this.mQsMinExpansionHeight));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (BenesseExtension.getDchaState() != 0 || this.mBlockTouches || this.mQsContainer.isCustomizing()) {
            return false;
        }
        initDownStates(event);
        if (this.mListenForHeadsUp && !this.mHeadsUpTouchHelper.isTrackingHeadsUp() && this.mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
        }
        if ((!this.mIsExpanding || this.mHintAnimationRunning) && !this.mQsExpanded && this.mStatusBar.getBarState() != 0) {
            this.mAfforanceHelper.onTouchEvent(event);
        }
        if (this.mOnlyAffordanceInThisMotion) {
            return true;
        }
        this.mHeadsUpTouchHelper.onTouchEvent(event);
        if (!this.mHeadsUpTouchHelper.isTrackingHeadsUp() && handleQsTouch(event)) {
            return true;
        }
        if (event.getActionMasked() == 0 && isFullyCollapsed()) {
            MetricsLogger.count(this.mContext, "panel_open", 1);
            updateVerticalPanelPosition(event.getX());
        }
        super.onTouchEvent(event);
        return true;
    }

    private boolean handleQsTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 0 && getExpandedFraction() == 1.0f && this.mStatusBar.getBarState() != 1 && !this.mQsExpanded && this.mQsExpansionEnabled) {
            this.mQsTracking = true;
            this.mConflictingQsExpansionGesture = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = event.getX();
            this.mInitialTouchX = event.getY();
        }
        if (!isFullyCollapsed()) {
            handleQsDown(event);
        }
        if (!this.mQsExpandImmediate && this.mQsTracking) {
            onQsTouch(event);
            if (!this.mConflictingQsExpansionGesture) {
                return true;
            }
        }
        if (action == 3 || action == 1) {
            this.mConflictingQsExpansionGesture = false;
        }
        if (action == 0 && isFullyCollapsed() && this.mQsExpansionEnabled) {
            this.mTwoFingerQsExpandPossible = true;
        }
        if (this.mTwoFingerQsExpandPossible && isOpenQsEvent(event) && event.getY(event.getActionIndex()) < this.mStatusBarMinHeight) {
            MetricsLogger.count(this.mContext, "panel_open_qs", 1);
            this.mQsExpandImmediate = true;
            requestPanelHeightUpdate();
            setListening(true);
        }
        return false;
    }

    private boolean isInQsArea(float x, float y) {
        if (x < this.mQsAutoReinflateContainer.getX() || x > this.mQsAutoReinflateContainer.getX() + this.mQsAutoReinflateContainer.getWidth()) {
            return false;
        }
        return y <= this.mNotificationStackScroller.getBottomMostNotificationBottom() || y <= this.mQsContainer.getY() + ((float) this.mQsContainer.getHeight());
    }

    private boolean isOpenQsEvent(MotionEvent event) {
        boolean stylusButtonClickDrag;
        boolean mouseButtonClickDrag;
        int pointerCount = event.getPointerCount();
        int action = event.getActionMasked();
        boolean twoFingerDrag = action == 5 && pointerCount == 2;
        if (action != 0) {
            stylusButtonClickDrag = false;
        } else if (event.isButtonPressed(32)) {
            stylusButtonClickDrag = true;
        } else {
            stylusButtonClickDrag = event.isButtonPressed(64);
        }
        if (action != 0) {
            mouseButtonClickDrag = false;
        } else if (event.isButtonPressed(2)) {
            mouseButtonClickDrag = true;
        } else {
            mouseButtonClickDrag = event.isButtonPressed(4);
        }
        if (twoFingerDrag || stylusButtonClickDrag) {
            return true;
        }
        return mouseButtonClickDrag;
    }

    private void handleQsDown(MotionEvent event) {
        if (event.getActionMasked() != 0 || !shouldQuickSettingsIntercept(event.getX(), event.getY(), -1.0f)) {
            return;
        }
        this.mFalsingManager.onQsDown();
        this.mQsTracking = true;
        onQsExpansionStarted();
        this.mInitialHeightOnTouch = this.mQsExpansionHeight;
        this.mInitialTouchY = event.getX();
        this.mInitialTouchX = event.getY();
        notifyExpandingFinished();
    }

    @Override
    protected boolean flingExpands(float vel, float vectorVel, float x, float y) {
        boolean expands = super.flingExpands(vel, vectorVel, x, y);
        if (this.mQsExpansionAnimator != null) {
            return true;
        }
        return expands;
    }

    @Override
    protected boolean hasConflictingGestures() {
        return this.mStatusBar.getBarState() != 0;
    }

    @Override
    protected boolean shouldGestureIgnoreXTouchSlop(float x, float y) {
        return !this.mAfforanceHelper.isOnAffordanceIcon(x, y);
    }

    private void onQsTouch(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float y = event.getY(pointerIndex);
        float x = event.getX(pointerIndex);
        float h = y - this.mInitialTouchY;
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
                if (fraction != 0.0f || y >= this.mInitialTouchY) {
                    flingQsWithCurrentVelocity(y, event.getActionMasked() == 3);
                }
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                break;
            case 2:
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
        float factor = this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
        return (int) (this.mQsFalsingThreshold * factor);
    }

    @Override
    public void onOverscrollTopChanged(float amount, boolean isRubberbanded) {
        cancelQsAnimation();
        if (!this.mQsExpansionEnabled) {
            amount = 0.0f;
        }
        float rounded = amount >= 1.0f ? amount : 0.0f;
        if (rounded == 0.0f) {
            isRubberbanded = false;
        }
        setOverScrolling(isRubberbanded);
        this.mQsExpansionFromOverscroll = rounded != 0.0f;
        this.mLastOverscroll = rounded;
        updateQsState();
        setQsExpansion(this.mQsMinExpansionHeight + rounded);
    }

    @Override
    public void flingTopOverscroll(float velocity, boolean open) {
        this.mLastOverscroll = 0.0f;
        this.mQsExpansionFromOverscroll = false;
        setQsExpansion(this.mQsExpansionHeight);
        if (!this.mQsExpansionEnabled && open) {
            velocity = 0.0f;
        }
        flingSettings(velocity, open ? this.mQsExpansionEnabled : false, new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mStackScrollerOverscrolling = false;
                NotificationPanelView.this.setOverScrolling(false);
                NotificationPanelView.this.updateQsState();
            }
        }, false);
    }

    public void setOverScrolling(boolean overscrolling) {
        this.mStackScrollerOverscrolling = overscrolling;
        this.mQsContainer.setOverscrolling(overscrolling);
    }

    private void onQsExpansionStarted() {
        onQsExpansionStarted(0);
    }

    private void onQsExpansionStarted(int overscrollAmount) {
        cancelQsAnimation();
        cancelHeightAnimator();
        float height = this.mQsExpansionHeight - overscrollAmount;
        setQsExpansion(height);
        requestPanelHeightUpdate();
    }

    private void setQsExpanded(boolean expanded) {
        boolean changed = this.mQsExpanded != expanded;
        if (!changed) {
            return;
        }
        this.mQsExpanded = expanded;
        updateQsState();
        requestPanelHeightUpdate();
        this.mFalsingManager.setQsExpanded(expanded);
        this.mStatusBar.setQsExpanded(expanded);
        this.mNotificationContainerParent.setQsExpanded(expanded);
    }

    public void setBarState(int statusBarState, boolean keyguardFadingAway, boolean goingToFullShade) {
        int oldState = this.mStatusBarState;
        boolean keyguardShowing = statusBarState == 1;
        setKeyguardStatusViewVisibility(statusBarState, keyguardFadingAway, goingToFullShade);
        setKeyguardBottomAreaVisibility(statusBarState, goingToFullShade);
        this.mStatusBarState = statusBarState;
        this.mKeyguardShowing = keyguardShowing;
        this.mQsContainer.setKeyguardShowing(this.mKeyguardShowing);
        if (goingToFullShade || (oldState == 1 && statusBarState == 2)) {
            animateKeyguardStatusBarOut();
            long delay = this.mStatusBarState == 2 ? 0L : this.mStatusBar.calculateGoingToFullShadeDelay();
            this.mQsContainer.animateHeaderSlidingIn(delay);
        } else if (oldState == 2 && statusBarState == 1) {
            animateKeyguardStatusBarIn(360L);
            this.mQsContainer.animateHeaderSlidingOut();
        } else {
            this.mKeyguardStatusBar.setAlpha(1.0f);
            this.mKeyguardStatusBar.setVisibility(keyguardShowing ? 0 : 4);
            if (keyguardShowing && oldState != this.mStatusBarState) {
                this.mKeyguardBottomArea.updateLeftAffordance();
                this.mAfforanceHelper.updatePreviews();
            }
        }
        if (keyguardShowing) {
            updateDozingVisibilities(false);
        }
        resetVerticalPanelPosition();
        updateQsState();
    }

    private void animateKeyguardStatusBarOut() {
        long keyguardFadingAwayDelay;
        long keyguardFadingAwayDuration;
        ValueAnimator anim = ValueAnimator.ofFloat(this.mKeyguardStatusBar.getAlpha(), 0.0f);
        anim.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        if (this.mStatusBar.isKeyguardFadingAway()) {
            keyguardFadingAwayDelay = this.mStatusBar.getKeyguardFadingAwayDelay();
        } else {
            keyguardFadingAwayDelay = 0;
        }
        anim.setStartDelay(keyguardFadingAwayDelay);
        if (this.mStatusBar.isKeyguardFadingAway()) {
            keyguardFadingAwayDuration = this.mStatusBar.getKeyguardFadingAwayDuration() / 2;
        } else {
            keyguardFadingAwayDuration = 360;
        }
        anim.setDuration(keyguardFadingAwayDuration);
        anim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                NotificationPanelView.this.mAnimateKeyguardStatusBarInvisibleEndRunnable.run();
            }
        });
        anim.start();
    }

    private void animateKeyguardStatusBarIn(long duration) {
        this.mKeyguardStatusBar.setVisibility(0);
        this.mKeyguardStatusBar.setAlpha(0.0f);
        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        anim.setDuration(duration);
        anim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        anim.start();
    }

    private void setKeyguardBottomAreaVisibility(int statusBarState, boolean goingToFullShade) {
        this.mKeyguardBottomArea.animate().cancel();
        if (goingToFullShade) {
            this.mKeyguardBottomArea.animate().alpha(0.0f).setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(this.mAnimateKeyguardBottomAreaInvisibleEndRunnable).start();
            return;
        }
        if (statusBarState != 1 && statusBarState != 2) {
            this.mKeyguardBottomArea.setVisibility(8);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        } else {
            if (!this.mDozing) {
                this.mKeyguardBottomArea.setVisibility(0);
            }
            this.mKeyguardBottomArea.setAlpha(1.0f);
        }
    }

    private void setKeyguardStatusViewVisibility(int statusBarState, boolean keyguardFadingAway, boolean goingToFullShade) {
        if ((!keyguardFadingAway && this.mStatusBarState == 1 && statusBarState != 1) || goingToFullShade) {
            this.mKeyguardStatusView.animate().cancel();
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.animate().alpha(0.0f).setStartDelay(0L).setDuration(160L).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(this.mAnimateKeyguardStatusViewInvisibleEndRunnable);
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
            this.mKeyguardStatusView.animate().alpha(1.0f).setStartDelay(0L).setDuration(320L).setInterpolator(Interpolators.ALPHA_IN).withEndAction(this.mAnimateKeyguardStatusViewVisibleEndRunnable);
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

    public void updateQsState() {
        boolean z;
        this.mQsContainer.setExpanded(this.mQsExpanded);
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        if (this.mStatusBarState != 1) {
            z = this.mQsExpanded ? this.mQsExpansionFromOverscroll : true;
        } else {
            z = false;
        }
        notificationStackScrollLayout.setScrollingEnabled(z);
        updateEmptyShadeView();
        this.mQsNavbarScrim.setVisibility((this.mStatusBarState == 0 && this.mQsExpanded && !this.mStackScrollerOverscrolling && this.mQsScrimEnabled) ? 0 : 4);
        if (this.mKeyguardUserSwitcher == null || !this.mQsExpanded || this.mStackScrollerOverscrolling) {
            return;
        }
        this.mKeyguardUserSwitcher.hideIfNotSimple(true);
    }

    public void setQsExpansion(float height) {
        float height2 = Math.min(Math.max(height, this.mQsMinExpansionHeight), this.mQsMaxExpansionHeight);
        this.mQsFullyExpanded = height2 == ((float) this.mQsMaxExpansionHeight) && this.mQsMaxExpansionHeight != 0;
        if (height2 > this.mQsMinExpansionHeight && !this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            setQsExpanded(true);
        } else if (height2 <= this.mQsMinExpansionHeight && this.mQsExpanded) {
            setQsExpanded(false);
            if (this.mLastAnnouncementWasQuickSettings && !this.mTracking && !isCollapsing()) {
                announceForAccessibility(getKeyguardOrLockScreenString());
                this.mLastAnnouncementWasQuickSettings = false;
            }
        }
        this.mQsExpansionHeight = height2;
        updateQsExpansion();
        requestScrollerTopPaddingUpdate(false);
        if (this.mKeyguardShowing) {
            updateHeaderKeyguardAlpha();
        }
        if (this.mStatusBarState == 2 || this.mStatusBarState == 1) {
            updateKeyguardBottomAreaAlpha();
        }
        if (this.mStatusBarState == 0 && this.mQsExpanded && !this.mStackScrollerOverscrolling && this.mQsScrimEnabled) {
            this.mQsNavbarScrim.setAlpha(getQsExpansionFraction());
        }
        if (height2 != 0.0f && this.mQsFullyExpanded && !this.mLastAnnouncementWasQuickSettings) {
            announceForAccessibility(getContext().getString(R.string.accessibility_desc_quick_settings));
            this.mLastAnnouncementWasQuickSettings = true;
        }
        if (!this.mQsFullyExpanded || !this.mFalsingManager.shouldEnforceBouncer()) {
            return;
        }
        this.mStatusBar.executeRunnableDismissingKeyguard(null, null, false, true, false);
    }

    protected void updateQsExpansion() {
        this.mQsContainer.setQsExpansion(getQsExpansionFraction(), getHeaderTranslation());
    }

    private String getKeyguardOrLockScreenString() {
        if (this.mQsContainer.isCustomizing()) {
            return getContext().getString(R.string.accessibility_desc_quick_settings_edit);
        }
        if (this.mStatusBarState == 1) {
            return getContext().getString(R.string.accessibility_desc_lock_screen);
        }
        return getContext().getString(R.string.accessibility_desc_notification_shade);
    }

    private float calculateQsTopPadding() {
        int max;
        if (this.mKeyguardShowing && (this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted))) {
            int maxNotifications = this.mClockPositionResult.stackScrollerPadding - this.mClockPositionResult.stackScrollerPaddingAdjustment;
            int maxQs = getTempQsMaxExpansion();
            if (this.mStatusBarState == 1) {
                max = Math.max(maxNotifications, maxQs);
            } else {
                max = maxQs;
            }
            return (int) interpolate(getExpandedFraction(), this.mQsMinExpansionHeight, max);
        }
        if (this.mQsSizeChangeAnimator != null) {
            return ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        }
        if (this.mKeyguardShowing) {
            return interpolate(getQsExpansionFraction(), this.mNotificationStackScroller.getIntrinsicPadding(), this.mQsMaxExpansionHeight);
        }
        return this.mQsExpansionHeight;
    }

    protected void requestScrollerTopPaddingUpdate(boolean animate) {
        boolean z = true;
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        float fCalculateQsTopPadding = calculateQsTopPadding();
        if (this.mAnimateNextTopPaddingChange) {
            animate = true;
        }
        if (!this.mKeyguardShowing) {
            z = false;
        } else if (!this.mQsExpandImmediate) {
            z = this.mIsExpanding ? this.mQsExpandedWhenExpandingStarted : false;
        }
        notificationStackScrollLayout.updateTopPadding(fCalculateQsTopPadding, animate, z);
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

    private void cancelQsAnimation() {
        if (this.mQsExpansionAnimator == null) {
            return;
        }
        this.mQsExpansionAnimator.cancel();
    }

    private void flingSettings(float vel, boolean expand) {
        flingSettings(vel, expand, null, false);
    }

    private void flingSettings(float vel, boolean expand, final Runnable onFinishRunnable, boolean isClick) {
        float target = expand ? this.mQsMaxExpansionHeight : this.mQsMinExpansionHeight;
        if (target == this.mQsExpansionHeight) {
            if (onFinishRunnable != null) {
                onFinishRunnable.run();
                return;
            }
            return;
        }
        boolean belowFalsingThreshold = isFalseTouch();
        if (belowFalsingThreshold) {
            vel = 0.0f;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(this.mQsExpansionHeight, target);
        if (isClick) {
            animator.setInterpolator(Interpolators.TOUCH_RESPONSE);
            animator.setDuration(368L);
        } else {
            this.mFlingAnimationUtils.apply(animator, this.mQsExpansionHeight, target, vel);
        }
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
                NotificationPanelView.this.mQsExpansionAnimator = null;
                if (onFinishRunnable == null) {
                    return;
                }
                onFinishRunnable.run();
            }
        });
        animator.start();
        this.mQsExpansionAnimator = animator;
        this.mQsAnimatorExpand = expand;
    }

    private boolean shouldQuickSettingsIntercept(float x, float y, float yDiff) {
        if (!this.mQsExpansionEnabled || this.mCollapsedOnDown) {
            return false;
        }
        View header = this.mKeyguardShowing ? this.mKeyguardStatusBar : this.mQsContainer.getHeader();
        boolean onHeader = x >= this.mQsAutoReinflateContainer.getX() && x <= this.mQsAutoReinflateContainer.getX() + ((float) this.mQsAutoReinflateContainer.getWidth()) && y >= ((float) header.getTop()) && y <= ((float) header.getBottom());
        if (this.mQsExpanded) {
            if (onHeader) {
                return true;
            }
            if (yDiff < 0.0f) {
                return isInQsArea(x, y);
            }
            return false;
        }
        return onHeader;
    }

    @Override
    protected boolean isScrolledToBottom() {
        if (isInSettings() || this.mStatusBar.getBarState() == 1) {
            return true;
        }
        return this.mNotificationStackScroller.isScrolledToBottom();
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
                float panelHeightQsCollapsed = this.mNotificationStackScroller.getIntrinsicPadding() + this.mNotificationStackScroller.getLayoutMinHeight();
                float panelHeightQsExpanded = calculatePanelHeightQsExpanded();
                t = (expandedHeight - panelHeightQsCollapsed) / (panelHeightQsExpanded - panelHeightQsCollapsed);
            }
            setQsExpansion(this.mQsMinExpansionHeight + ((getTempQsMaxExpansion() - this.mQsMinExpansionHeight) * t));
        }
        updateStackHeight(expandedHeight);
        updateHeader();
        updateUnlockIcon();
        updateNotificationTranslucency();
        updatePanelExpanded();
        this.mNotificationStackScroller.setShadeExpanded(!isFullyCollapsed());
    }

    private void updatePanelExpanded() {
        boolean isExpanded = !isFullyCollapsed();
        if (this.mPanelExpanded == isExpanded) {
            return;
        }
        this.mHeadsUpManager.setIsExpanded(isExpanded);
        this.mStatusBar.setPanelExpanded(isExpanded);
        this.mPanelExpanded = isExpanded;
    }

    private int getTempQsMaxExpansion() {
        return this.mQsMaxExpansionHeight;
    }

    private int calculatePanelHeightShade() {
        int emptyBottomMargin = this.mNotificationStackScroller.getEmptyBottomMargin();
        int maxHeight = (this.mNotificationStackScroller.getHeight() - emptyBottomMargin) - this.mTopPaddingAdjustment;
        return (int) (maxHeight + this.mNotificationStackScroller.getTopPaddingOverflow());
    }

    private int calculatePanelHeightQsExpanded() {
        float notificationHeight = (this.mNotificationStackScroller.getHeight() - this.mNotificationStackScroller.getEmptyBottomMargin()) - this.mNotificationStackScroller.getTopPadding();
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0 && this.mShadeEmpty) {
            notificationHeight = this.mNotificationStackScroller.getEmptyShadeViewHeight() + this.mNotificationStackScroller.getBottomStackPeekSize() + this.mNotificationStackScroller.getBottomStackSlowDownHeight();
        }
        int maxQsHeight = this.mQsMaxExpansionHeight;
        if (this.mQsSizeChangeAnimator != null) {
            maxQsHeight = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        }
        float totalHeight = Math.max(maxQsHeight, this.mStatusBarState == 1 ? this.mClockPositionResult.stackScrollerPadding - this.mTopPaddingAdjustment : 0) + notificationHeight;
        if (totalHeight > this.mNotificationStackScroller.getHeight()) {
            float fullyCollapsedHeight = this.mNotificationStackScroller.getLayoutMinHeight() + maxQsHeight;
            totalHeight = Math.max(fullyCollapsedHeight, this.mNotificationStackScroller.getHeight());
        }
        return (int) totalHeight;
    }

    private void updateNotificationTranslucency() {
        float alpha = 1.0f;
        if (this.mClosingWithAlphaFadeOut && !this.mExpandingFromHeadsUp && !this.mHeadsUpManager.hasPinnedHeadsUp()) {
            alpha = getFadeoutAlpha();
        }
        this.mNotificationStackScroller.setAlpha(alpha);
    }

    private float getFadeoutAlpha() {
        float alpha = (getNotificationsTopY() + this.mNotificationStackScroller.getFirstItemMinHeight()) / ((this.mQsMinExpansionHeight + this.mNotificationStackScroller.getBottomStackPeekSize()) - this.mNotificationStackScroller.getBottomStackSlowDownHeight());
        return (float) Math.pow(Math.max(0.0f, Math.min(alpha, 1.0f)), 0.75d);
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
        if (this.mStatusBar.getBarState() != 1 && this.mStatusBar.getBarState() != 2) {
            return;
        }
        boolean active = ((float) getMaxPanelHeight()) - getExpandedHeight() > ((float) this.mUnlockMoveDistance);
        KeyguardAffordanceView lockIcon = this.mKeyguardBottomArea.getLockIcon();
        if (active && !this.mUnlockIconActive && this.mTracking) {
            lockIcon.setImageAlpha(1.0f, true, 150L, Interpolators.FAST_OUT_LINEAR_IN, null);
            lockIcon.setImageScale(1.2f, true, 150L, Interpolators.FAST_OUT_LINEAR_IN);
        } else if (!active && this.mUnlockIconActive && this.mTracking) {
            lockIcon.setImageAlpha(lockIcon.getRestingAlpha(), true, 150L, Interpolators.FAST_OUT_LINEAR_IN, null);
            lockIcon.setImageScale(1.0f, true, 150L, Interpolators.FAST_OUT_LINEAR_IN);
        }
        this.mUnlockIconActive = active;
    }

    private void updateHeader() {
        if (this.mStatusBar.getBarState() == 1) {
            updateHeaderKeyguardAlpha();
        }
        updateQsExpansion();
    }

    protected float getHeaderTranslation() {
        if (this.mStatusBar.getBarState() == 1) {
            return 0.0f;
        }
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            return Math.min(0.0f, (this.mExpandedHeight / 2.05f) - this.mQsMinExpansionHeight);
        }
        float stackTranslation = this.mNotificationStackScroller.getStackTranslation();
        float translation = stackTranslation / 2.05f;
        if (this.mHeadsUpManager.hasPinnedHeadsUp() || this.mIsExpansionFromHeadsUp) {
            translation = (this.mNotificationStackScroller.getTopPadding() + stackTranslation) - this.mQsMinExpansionHeight;
        }
        return Math.min(0.0f, translation);
    }

    private float getKeyguardContentsAlpha() {
        float alpha;
        if (this.mStatusBar.getBarState() == 1) {
            alpha = getNotificationsTopY() / (this.mKeyguardStatusBar.getHeight() + this.mNotificationsHeaderCollideDistance);
        } else {
            alpha = getNotificationsTopY() / this.mKeyguardStatusBar.getHeight();
        }
        return (float) Math.pow(MathUtils.constrain(alpha, 0.0f, 1.0f), 0.75d);
    }

    public void updateHeaderKeyguardAlpha() {
        float alphaQsExpansion = 1.0f - Math.min(1.0f, getQsExpansionFraction() * 2.0f);
        this.mKeyguardStatusBar.setAlpha(Math.min(getKeyguardContentsAlpha(), alphaQsExpansion) * this.mKeyguardStatusBarAnimateAlpha);
        this.mKeyguardStatusBar.setVisibility((this.mKeyguardStatusBar.getAlpha() == 0.0f || this.mDozing) ? 4 : 0);
    }

    private void updateKeyguardBottomAreaAlpha() {
        int i;
        float alpha = Math.min(getKeyguardContentsAlpha(), 1.0f - getQsExpansionFraction());
        this.mKeyguardBottomArea.setAlpha(alpha);
        KeyguardBottomAreaView keyguardBottomAreaView = this.mKeyguardBottomArea;
        if (alpha == 0.0f) {
            i = 4;
        } else {
            i = 0;
        }
        keyguardBottomAreaView.setImportantForAccessibility(i);
    }

    private float getNotificationsTopY() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            return getExpandedHeight();
        }
        return this.mNotificationStackScroller.getNotificationsTopY();
    }

    @Override
    protected void onExpandingStarted() {
        super.onExpandingStarted();
        this.mNotificationStackScroller.onExpansionStarted();
        this.mIsExpanding = true;
        this.mQsExpandedWhenExpandingStarted = this.mQsFullyExpanded;
        if (!this.mQsExpanded) {
            return;
        }
        onQsExpansionStarted();
    }

    @Override
    protected void onExpandingFinished() {
        super.onExpandingFinished();
        this.mNotificationStackScroller.onExpansionStopped();
        this.mHeadsUpManager.onExpandingFinished();
        this.mIsExpanding = false;
        if (isFullyCollapsed()) {
            DejankUtils.postAfterTraversal(new Runnable() {
                @Override
                public void run() {
                    NotificationPanelView.this.setListening(false);
                }
            });
            postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    NotificationPanelView.this.getParent().invalidateChild(NotificationPanelView.this, NotificationPanelView.mDummyDirtyRect);
                }
            });
        } else {
            setListening(true);
        }
        this.mQsExpandImmediate = false;
        this.mTwoFingerQsExpandPossible = false;
        this.mIsExpansionFromHeadsUp = false;
        this.mNotificationStackScroller.setTrackingHeadsUp(false);
        this.mExpandingFromHeadsUp = false;
        setPanelScrimMinFraction(0.0f);
    }

    public void setListening(boolean listening) {
        this.mQsContainer.setListening(listening);
        this.mKeyguardStatusBar.setListening(listening);
    }

    @Override
    public void expand(boolean animate) {
        super.expand(animate);
        setListening(true);
    }

    @Override
    protected void setOverExpansion(float overExpansion, boolean isPixels) {
        if (this.mConflictingQsExpansionGesture || this.mQsExpandImmediate || this.mStatusBar.getBarState() == 1) {
            return;
        }
        this.mNotificationStackScroller.setOnHeightChangedListener(null);
        if (isPixels) {
            this.mNotificationStackScroller.setOverScrolledPixels(overExpansion, true, false);
        } else {
            this.mNotificationStackScroller.setOverScrollAmount(overExpansion, true, false);
        }
        this.mNotificationStackScroller.setOnHeightChangedListener(this);
    }

    @Override
    protected void onTrackingStarted() {
        this.mFalsingManager.onTrackingStarted();
        super.onTrackingStarted();
        if (this.mQsFullyExpanded) {
            this.mQsExpandImmediate = true;
        }
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            this.mAfforanceHelper.animateHideLeftRightIcon();
        }
        this.mNotificationStackScroller.onPanelTrackingStarted();
    }

    @Override
    protected void onTrackingStopped(boolean expand) {
        this.mFalsingManager.onTrackingStopped();
        super.onTrackingStopped(expand);
        if (expand) {
            this.mNotificationStackScroller.setOverScrolledPixels(0.0f, true, true);
        }
        this.mNotificationStackScroller.onPanelTrackingStopped();
        if (expand && ((this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) && !this.mHintAnimationRunning)) {
            this.mAfforanceHelper.reset(true);
        }
        if (expand) {
            return;
        }
        if (this.mStatusBar.getBarState() != 1 && this.mStatusBar.getBarState() != 2) {
            return;
        }
        KeyguardAffordanceView lockIcon = this.mKeyguardBottomArea.getLockIcon();
        lockIcon.setImageAlpha(0.0f, true, 100L, Interpolators.FAST_OUT_LINEAR_IN, null);
        lockIcon.setImageScale(2.0f, true, 100L, Interpolators.FAST_OUT_LINEAR_IN);
    }

    @Override
    public void onHeightChanged(ExpandableView view, boolean needsAnimation) {
        if (view == null && this.mQsExpanded) {
            return;
        }
        requestPanelHeightUpdate();
    }

    @Override
    public void onReset(ExpandableView view) {
    }

    public void onQsHeightChanged() {
        this.mQsMaxExpansionHeight = this.mQsContainer.getDesiredHeight();
        if (!this.mQsExpanded || !this.mQsFullyExpanded) {
            return;
        }
        this.mQsExpansionHeight = this.mQsMaxExpansionHeight;
        requestScrollerTopPaddingUpdate(false);
        requestPanelHeightUpdate();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mAfforanceHelper.onConfigurationChanged();
        if (newConfig.orientation != this.mLastOrientation) {
            resetVerticalPanelPosition();
        }
        this.mLastOrientation = newConfig.orientation;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mNavigationBarBottomHeight = insets.getStableInsetBottom();
        updateMaxHeadsUpTranslation();
        return insets;
    }

    private void updateMaxHeadsUpTranslation() {
        this.mNotificationStackScroller.setHeadsUpBoundaries(getHeight(), this.mNavigationBarBottomHeight);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (layoutDirection == this.mOldLayoutDirection) {
            return;
        }
        this.mAfforanceHelper.onRtlPropertiesChanged();
        this.mOldLayoutDirection = layoutDirection;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.expand_indicator) {
            return;
        }
        onQsExpansionStarted();
        if (this.mQsExpanded) {
            flingSettings(0.0f, false, null, true);
        } else {
            if (!this.mQsExpansionEnabled) {
                return;
            }
            EventLogTags.writeSysuiLockscreenGesture(10, 0, 0);
            flingSettings(0.0f, true, null, true);
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
            this.mFalsingManager.onLeftAffordanceOn();
            if (this.mFalsingManager.shouldEnforceBouncer()) {
                this.mStatusBar.executeRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public void run() {
                        NotificationPanelView.this.mKeyguardBottomArea.launchLeftAffordance();
                    }
                }, null, true, false, true);
            } else {
                this.mKeyguardBottomArea.launchLeftAffordance();
            }
        } else {
            if ("lockscreen_affordance".equals(this.mLastCameraLaunchSource)) {
                EventLogTags.writeSysuiLockscreenGesture(4, lengthDp, velocityDp);
            }
            this.mFalsingManager.onCameraOn();
            if (this.mFalsingManager.shouldEnforceBouncer()) {
                this.mStatusBar.executeRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public void run() {
                        NotificationPanelView.this.mKeyguardBottomArea.launchCamera(NotificationPanelView.this.mLastCameraLaunchSource);
                    }
                }, null, true, false, true);
            } else {
                this.mKeyguardBottomArea.launchCamera(this.mLastCameraLaunchSource);
            }
        }
        this.mStatusBar.startLaunchTransitionTimeout();
        this.mBlockTouches = true;
    }

    @Override
    public void onAnimationToSideEnded() {
        this.mIsLaunchTransitionRunning = false;
        this.mIsLaunchTransitionFinished = true;
        if (this.mLaunchAnimationEndRunnable == null) {
            return;
        }
        this.mLaunchAnimationEndRunnable.run();
        this.mLaunchAnimationEndRunnable = null;
    }

    @Override
    protected void startUnlockHintAnimation() {
        super.startUnlockHintAnimation();
        startHighlightIconAnimation(getCenterIcon());
    }

    private void startHighlightIconAnimation(final KeyguardAffordanceView icon) {
        icon.setImageAlpha(1.0f, true, 200L, Interpolators.FAST_OUT_SLOW_IN, new Runnable() {
            @Override
            public void run() {
                icon.setImageAlpha(icon.getRestingAlpha(), true, 200L, Interpolators.FAST_OUT_SLOW_IN, null);
            }
        });
    }

    @Override
    public float getMaxTranslationDistance() {
        return (float) Math.hypot(getWidth(), getHeight());
    }

    @Override
    public void onSwipingStarted(boolean rightIcon) {
        this.mFalsingManager.onAffordanceSwipingStarted(rightIcon);
        boolean camera = getLayoutDirection() == 1 ? !rightIcon : rightIcon;
        if (camera) {
            this.mKeyguardBottomArea.bindCameraPrewarmService();
        }
        requestDisallowInterceptTouchEvent(true);
        this.mOnlyAffordanceInThisMotion = true;
        this.mQsTracking = false;
    }

    @Override
    public void onSwipingAborted() {
        this.mFalsingManager.onAffordanceSwipingAborted();
        this.mKeyguardBottomArea.unbindCameraPrewarmService(false);
    }

    @Override
    public void onIconClicked(boolean rightIcon) {
        if (this.mHintAnimationRunning) {
            return;
        }
        this.mHintAnimationRunning = true;
        this.mAfforanceHelper.startHintAnimation(rightIcon, new Runnable() {
            @Override
            public void run() {
                NotificationPanelView.this.mHintAnimationRunning = false;
                NotificationPanelView.this.mStatusBar.onHintFinished();
            }
        });
        if (getLayoutDirection() == 1) {
            rightIcon = !rightIcon;
        }
        if (rightIcon) {
            this.mStatusBar.onCameraHintStarted();
        } else if (this.mKeyguardBottomArea.isLeftVoiceAssist()) {
            this.mStatusBar.onVoiceAssistHintStarted();
        } else {
            this.mStatusBar.onPhoneHintStarted();
        }
    }

    @Override
    public KeyguardAffordanceView getLeftIcon() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getRightView();
        }
        return this.mKeyguardBottomArea.getLeftView();
    }

    @Override
    public KeyguardAffordanceView getCenterIcon() {
        return this.mKeyguardBottomArea.getLockIcon();
    }

    @Override
    public KeyguardAffordanceView getRightIcon() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getLeftView();
        }
        return this.mKeyguardBottomArea.getRightView();
    }

    @Override
    public View getLeftPreview() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getRightPreview();
        }
        return this.mKeyguardBottomArea.getLeftPreview();
    }

    @Override
    public View getRightPreview() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getLeftPreview();
        }
        return this.mKeyguardBottomArea.getRightPreview();
    }

    @Override
    public float getAffordanceFalsingFactor() {
        return this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }

    @Override
    public boolean needsAntiFalsing() {
        return this.mStatusBarState == 1;
    }

    @Override
    protected float getPeekHeight() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() > 0) {
            return this.mNotificationStackScroller.getPeekHeight();
        }
        return this.mQsMinExpansionHeight * 2.05f;
    }

    @Override
    protected float getCannedFlingDurationFactor() {
        if (this.mQsExpanded) {
            return 0.7f;
        }
        return 0.6f;
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
        if (this.mConflictingQsExpansionGesture) {
            return this.mQsExpanded;
        }
        return false;
    }

    public boolean isQsExpanded() {
        return this.mQsExpanded;
    }

    public boolean isQsDetailShowing() {
        return this.mQsContainer.isShowingDetail();
    }

    public void closeQsDetail() {
        this.mQsContainer.getQsPanel().closeDetail();
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
        if (dozing == this.mDozing) {
            return;
        }
        this.mDozing = dozing;
        if (this.mStatusBarState != 1) {
            return;
        }
        updateDozingVisibilities(animate);
    }

    private void updateDozingVisibilities(boolean animate) {
        if (this.mDozing) {
            this.mKeyguardStatusBar.setVisibility(4);
            this.mKeyguardBottomArea.setVisibility(4);
            return;
        }
        this.mKeyguardBottomArea.setVisibility(0);
        this.mKeyguardStatusBar.setVisibility(0);
        if (!animate) {
            return;
        }
        animateKeyguardStatusBarIn(700L);
        this.mKeyguardBottomArea.startFinishDozeAnimation();
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public void setShadeEmpty(boolean shadeEmpty) {
        this.mShadeEmpty = shadeEmpty;
        updateEmptyShadeView();
    }

    private void updateEmptyShadeView() {
        boolean z = false;
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        if (this.mShadeEmpty && !this.mQsExpanded) {
            z = true;
        }
        notificationStackScrollLayout.updateEmptyShadeView(z);
    }

    public void setQsScrimEnabled(boolean qsScrimEnabled) {
        boolean changed = this.mQsScrimEnabled != qsScrimEnabled;
        this.mQsScrimEnabled = qsScrimEnabled;
        if (!changed) {
            return;
        }
        updateQsState();
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void onScreenTurningOn() {
        this.mKeyguardStatusView.refreshTime();
    }

    @Override
    public void onEmptySpaceClicked(float x, float y) {
        onEmptySpaceClick(x);
    }

    @Override
    protected boolean onMiddleClicked() {
        switch (this.mStatusBar.getBarState()) {
            case 0:
                post(this.mPostCollapseRunnable);
                break;
            case 1:
                if (!this.mDozingOnDown) {
                    EventLogTags.writeSysuiLockscreenGesture(3, 0, 0);
                    startUnlockHintAnimation();
                }
                break;
            case 2:
                if (!this.mQsExpanded) {
                    this.mStatusBar.goToKeyguard();
                }
                break;
        }
        return true;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
        if (inPinnedMode) {
            this.mHeadsUpExistenceChangedRunnable.run();
            updateNotificationTranslucency();
        } else {
            this.mHeadsUpAnimatingAway = true;
            this.mNotificationStackScroller.runAfterAnimationFinished(this.mHeadsUpExistenceChangedRunnable);
        }
    }

    @Override
    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        this.mNotificationStackScroller.generateHeadsUpAnimation(headsUp, true);
    }

    @Override
    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    @Override
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        this.mNotificationStackScroller.generateHeadsUpAnimation(entry.row, isHeadsUp);
    }

    @Override
    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        super.setHeadsUpManager(headsUpManager);
        this.mHeadsUpTouchHelper = new HeadsUpTouchHelper(headsUpManager, this.mNotificationStackScroller, this);
    }

    public void setTrackingHeadsUp(boolean tracking) {
        if (!tracking) {
            return;
        }
        this.mNotificationStackScroller.setTrackingHeadsUp(true);
        this.mExpandingFromHeadsUp = true;
    }

    @Override
    protected void onClosingFinished() {
        super.onClosingFinished();
        resetVerticalPanelPosition();
        setClosingWithAlphaFadeout(false);
    }

    private void setClosingWithAlphaFadeout(boolean closing) {
        this.mClosingWithAlphaFadeOut = closing;
        this.mNotificationStackScroller.forceNoOverlappingRendering(closing);
    }

    protected void updateVerticalPanelPosition(float x) {
        if (this.mNotificationStackScroller.getWidth() <= 0 || this.mNotificationStackScroller.getWidth() * 1.75f > getWidth()) {
            resetVerticalPanelPosition();
            return;
        }
        float leftMost = this.mPositionMinSideMargin + (this.mNotificationStackScroller.getWidth() / 2);
        float rightMost = (getWidth() - this.mPositionMinSideMargin) - (this.mNotificationStackScroller.getWidth() / 2);
        if (Math.abs(x - (getWidth() / 2)) < this.mNotificationStackScroller.getWidth() / 4) {
            x = getWidth() / 2;
        }
        setVerticalPanelTranslation(Math.min(rightMost, Math.max(leftMost, x)) - (this.mNotificationStackScroller.getLeft() + (this.mNotificationStackScroller.getWidth() / 2)));
    }

    private void resetVerticalPanelPosition() {
        setVerticalPanelTranslation(0.0f);
    }

    protected void setVerticalPanelTranslation(float translation) {
        this.mNotificationStackScroller.setTranslationX(translation);
        this.mQsAutoReinflateContainer.setTranslationX(translation);
    }

    protected void updateStackHeight(float stackHeight) {
        this.mNotificationStackScroller.setStackHeight(stackHeight);
        updateKeyguardBottomAreaAlpha();
    }

    public void setPanelScrimMinFraction(float minFraction) {
        this.mBar.panelScrimMinFractionChanged(minFraction);
    }

    public void clearNotificationEffects() {
        this.mStatusBar.clearNotificationEffects();
    }

    @Override
    protected boolean isPanelVisibleBecauseOfHeadsUp() {
        if (this.mHeadsUpManager.hasPinnedHeadsUp()) {
            return true;
        }
        return this.mHeadsUpAnimatingAway;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return !this.mDozing;
    }

    public void launchCamera(boolean animate, int source) {
        if (source == 1) {
            this.mLastCameraLaunchSource = "power_double_tap";
        } else if (source == 0) {
            this.mLastCameraLaunchSource = "wiggle_gesture";
        } else {
            this.mLastCameraLaunchSource = "lockscreen_affordance";
        }
        if (!isFullyCollapsed()) {
            this.mLaunchingAffordance = true;
            setLaunchingAffordance(true);
        } else {
            animate = false;
        }
        this.mAfforanceHelper.launchAffordance(animate, getLayoutDirection() == 1);
    }

    public void onAffordanceLaunchEnded() {
        this.mLaunchingAffordance = false;
        setLaunchingAffordance(false);
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        this.mNotificationStackScroller.setParentFadingOut(alpha != 1.0f);
    }

    private void setLaunchingAffordance(boolean launchingAffordance) {
        getLeftIcon().setLaunchingAffordance(launchingAffordance);
        getRightIcon().setLaunchingAffordance(launchingAffordance);
        getCenterIcon().setLaunchingAffordance(launchingAffordance);
    }

    public boolean canCameraGestureBeLaunched(boolean keyguardIsShowing) {
        String packageToLaunch = null;
        if (!this.mStatusBar.isCameraAllowedByAdmin()) {
            EventLog.writeEvent(1397638484, "63787722", -1, "");
            return false;
        }
        ResolveInfo resolveInfo = this.mKeyguardBottomArea.resolveCameraIntent();
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            packageToLaunch = resolveInfo.activityInfo.packageName;
        }
        if (packageToLaunch != null) {
            return (keyguardIsShowing || !isForegroundApp(packageToLaunch)) && !this.mAfforanceHelper.isSwipingInProgress();
        }
        return false;
    }

    private boolean isForegroundApp(String pkgName) {
        ActivityManager am = (ActivityManager) getContext().getSystemService(ActivityManager.class);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks.isEmpty()) {
            return false;
        }
        return pkgName.equals(tasks.get(0).topActivity.getPackageName());
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
    }
}
