package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.BenesseExtension;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.DejankUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.AnimatableProperty;
import com.android.systemui.statusbar.notification.PropertyAnimator;
import com.android.systemui.statusbar.phone.KeyguardAffordanceHelper;
import com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/* loaded from: classes.dex */
public class NotificationPanelView extends PanelView implements View.OnClickListener, QS.HeightListener, ExpandableView.OnHeightChangedListener, KeyguardAffordanceHelper.Callback, OnHeadsUpChangedListener, NotificationStackScrollLayout.OnEmptySpaceClickListener, NotificationStackScrollLayout.OnOverscrollTopChangedListener {
    private final AnimatableProperty PANEL_ALPHA;
    private final AnimationProperties PANEL_ALPHA_IN_PROPERTIES;
    private final AnimationProperties PANEL_ALPHA_OUT_PROPERTIES;
    private final AccessibilityManager mAccessibilityManager;
    private KeyguardAffordanceHelper mAffordanceHelper;
    private final Paint mAlphaPaint;
    private int mAmbientIndicationBottomPadding;
    private final Runnable mAnimateKeyguardBottomAreaInvisibleEndRunnable;
    private final Runnable mAnimateKeyguardStatusBarInvisibleEndRunnable;
    private final Runnable mAnimateKeyguardStatusViewGoneEndRunnable;
    private final Runnable mAnimateKeyguardStatusViewInvisibleEndRunnable;
    private final Runnable mAnimateKeyguardStatusViewVisibleEndRunnable;
    private boolean mAnimateNextPositionUpdate;
    private AnimatorListenerAdapter mAnimatorListenerAdapter;
    private boolean mBlockTouches;
    private int mBouncerTop;
    private KeyguardClockPositionAlgorithm mClockPositionAlgorithm;
    private KeyguardClockPositionAlgorithm.Result mClockPositionResult;
    private boolean mClosingWithAlphaFadeOut;
    private boolean mCollapsedOnDown;
    private boolean mConflictingQsExpansionGesture;
    private int mCurrentPanelAlpha;
    private float mDarkAmount;
    private float mDarkAmountTarget;
    private ValueAnimator mDarkAnimator;
    private boolean mDozing;
    private boolean mDozingOnDown;
    private float mEmptyDragAmount;
    private float mExpandOffset;
    private boolean mExpandingFromHeadsUp;
    private FalsingManager mFalsingManager;
    private FlingAnimationUtils mFlingAnimationUtils;
    private final FragmentHostManager.FragmentListener mFragmentListener;
    private NotificationGroupManager mGroupManager;
    private boolean mHeadsUpAnimatingAway;
    private HeadsUpAppearanceController mHeadsUpAppearanceController;
    private Runnable mHeadsUpExistenceChangedRunnable;
    private HeadsUpTouchHelper mHeadsUpTouchHelper;
    private boolean mHideIconsDuringNotificationLaunch;
    private int mIndicationBottomPadding;
    private float mInitialHeightOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mIntercepting;
    private boolean mIsExpanding;
    private boolean mIsExpansionFromHeadsUp;
    private boolean mIsFullWidth;
    private boolean mIsLaunchTransitionFinished;
    private boolean mIsLaunchTransitionRunning;
    private boolean mKeyguardShowing;
    private KeyguardStatusBarView mKeyguardStatusBar;
    private float mKeyguardStatusBarAnimateAlpha;
    private KeyguardStatusView mKeyguardStatusView;
    private boolean mKeyguardStatusViewAnimating;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    private String mLastCameraLaunchSource;
    private int mLastOrientation;
    private float mLastOverscroll;
    private float mLastTouchX;
    private float mLastTouchY;
    private Runnable mLaunchAnimationEndRunnable;
    private boolean mLaunchingAffordance;
    private boolean mListenForHeadsUp;
    private LockscreenGestureLogger mLockscreenGestureLogger;
    private int mMaxFadeoutHeight;
    private int mNavigationBarBottomHeight;
    private boolean mNoVisibleNotifications;
    protected NotificationsQuickSettingsContainer mNotificationContainerParent;
    protected NotificationStackScrollLayout mNotificationStackScroller;
    private int mNotificationsHeaderCollideDistance;
    private int mOldLayoutDirection;
    private boolean mOnlyAffordanceInThisMotion;
    private int mPanelAlpha;
    private Runnable mPanelAlphaEndAction;
    private boolean mPanelExpanded;
    private int mPositionMinSideMargin;
    private final PowerManager mPowerManager;
    private boolean mPulsing;
    private QS mQs;
    private boolean mQsAnimatorExpand;
    private boolean mQsExpandImmediate;
    private boolean mQsExpanded;
    private boolean mQsExpandedWhenExpandingStarted;
    private ValueAnimator mQsExpansionAnimator;
    protected boolean mQsExpansionEnabled;
    private boolean mQsExpansionFromOverscroll;
    protected float mQsExpansionHeight;
    private int mQsFalsingThreshold;
    private FrameLayout mQsFrame;
    private boolean mQsFullyExpanded;
    protected int mQsMaxExpansionHeight;
    protected int mQsMinExpansionHeight;
    private View mQsNavbarScrim;
    private int mQsNotificationTopPadding;
    private int mQsPeekHeight;
    private boolean mQsScrimEnabled;
    private ValueAnimator mQsSizeChangeAnimator;
    private boolean mQsTouchAboveFalsingThreshold;
    private boolean mQsTracking;
    private VelocityTracker mQsVelocityTracker;
    private boolean mShowEmptyShadeView;
    private boolean mShowIconsWhenExpanded;
    private int mStackScrollerMeasuringPass;
    private boolean mStackScrollerOverscrolling;
    private final ValueAnimator.AnimatorUpdateListener mStatusBarAnimateAlphaListener;
    private int mStatusBarMinHeight;
    protected int mStatusBarState;
    private ArrayList<Consumer<ExpandableNotificationRow>> mTrackingHeadsUpListeners;
    private int mTrackingPointer;
    private boolean mTwoFingerQsExpandPossible;
    private boolean mUnlockIconActive;
    private int mUnlockMoveDistance;
    private boolean mUserSetupComplete;
    private ArrayList<Runnable> mVerticalTranslationListener;
    private static final Rect mDummyDirtyRect = new Rect(0, 0, 1, 1);
    private static final AnimationProperties CLOCK_ANIMATION_PROPERTIES = new AnimationProperties().setDuration(360);
    private static final FloatProperty<NotificationPanelView> SET_DARK_AMOUNT_PROPERTY = new FloatProperty<NotificationPanelView>("mDarkAmount") { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.1
        AnonymousClass1(String str) {
            super(str);
        }

        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(NotificationPanelView notificationPanelView, float f) {
            notificationPanelView.setDarkAmount(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(NotificationPanelView notificationPanelView) {
            return Float.valueOf(notificationPanelView.mDarkAmount);
        }
    };

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$1 */
    class AnonymousClass1 extends FloatProperty<NotificationPanelView> {
        AnonymousClass1(String str) {
            super(str);
        }

        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(NotificationPanelView notificationPanelView, float f) {
            notificationPanelView.setDarkAmount(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(NotificationPanelView notificationPanelView) {
            return Float.valueOf(notificationPanelView.mDarkAmount);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$2 */
    class AnonymousClass2 implements Runnable {
        AnonymousClass2() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.setHeadsUpAnimatingAway(false);
            NotificationPanelView.this.notifyBarPanelExpansionChanged();
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$3 */
    class AnonymousClass3 extends AnimatorListenerAdapter {
        AnonymousClass3() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (NotificationPanelView.this.mPanelAlphaEndAction != null) {
                NotificationPanelView.this.mPanelAlphaEndAction.run();
            }
        }
    }

    public NotificationPanelView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mQsExpansionEnabled = true;
        this.mClockPositionAlgorithm = new KeyguardClockPositionAlgorithm();
        this.mClockPositionResult = new KeyguardClockPositionAlgorithm.Result();
        this.mQsScrimEnabled = true;
        this.mKeyguardStatusBarAnimateAlpha = 1.0f;
        this.mLastOrientation = -1;
        this.mLastCameraLaunchSource = "lockscreen_affordance";
        this.mHeadsUpExistenceChangedRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.2
            AnonymousClass2() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationPanelView.this.setHeadsUpAnimatingAway(false);
                NotificationPanelView.this.notifyBarPanelExpansionChanged();
            }
        };
        this.mLockscreenGestureLogger = new LockscreenGestureLogger();
        this.mNoVisibleNotifications = true;
        this.mHideIconsDuringNotificationLaunch = true;
        this.mTrackingHeadsUpListeners = new ArrayList<>();
        this.mVerticalTranslationListener = new ArrayList<>();
        this.mAlphaPaint = new Paint();
        this.mAnimatorListenerAdapter = new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.3
            AnonymousClass3() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (NotificationPanelView.this.mPanelAlphaEndAction != null) {
                    NotificationPanelView.this.mPanelAlphaEndAction.run();
                }
            }
        };
        this.PANEL_ALPHA = AnimatableProperty.from("panelAlpha", new BiConsumer() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$aKsp0zdf_wKFZXD1TonJ2cFEsN4
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((NotificationPanelView) obj).setPanelAlphaInternal(((Float) obj2).floatValue());
            }
        }, new Function() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$SmdYpsZqQm1fpR9OgK3SiEL3pJQ
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return Float.valueOf(((NotificationPanelView) obj).getCurrentPanelAlpha());
            }
        }, R.id.panel_alpha_animator_tag, R.id.panel_alpha_animator_start_tag, R.id.panel_alpha_animator_end_tag);
        this.PANEL_ALPHA_OUT_PROPERTIES = new AnimationProperties().setDuration(150L).setCustomInterpolator(this.PANEL_ALPHA.getProperty(), Interpolators.ALPHA_OUT);
        this.PANEL_ALPHA_IN_PROPERTIES = new AnimationProperties().setDuration(200L).setAnimationFinishListener(this.mAnimatorListenerAdapter).setCustomInterpolator(this.PANEL_ALPHA.getProperty(), Interpolators.ALPHA_IN);
        this.mAnimateKeyguardStatusViewInvisibleEndRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.7
            AnonymousClass7() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
                NotificationPanelView.this.mKeyguardStatusView.setVisibility(4);
            }
        };
        this.mAnimateKeyguardStatusViewGoneEndRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.8
            AnonymousClass8() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
                NotificationPanelView.this.mKeyguardStatusView.setVisibility(8);
            }
        };
        this.mAnimateKeyguardStatusViewVisibleEndRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.9
            AnonymousClass9() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
            }
        };
        this.mAnimateKeyguardStatusBarInvisibleEndRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.10
            AnonymousClass10() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationPanelView.this.mKeyguardStatusBar.setVisibility(4);
                NotificationPanelView.this.mKeyguardStatusBar.setAlpha(1.0f);
                NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = 1.0f;
            }
        };
        this.mStatusBarAnimateAlphaListener = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.12
            AnonymousClass12() {
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                NotificationPanelView.this.updateHeaderKeyguardAlpha();
            }
        };
        this.mAnimateKeyguardBottomAreaInvisibleEndRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.13
            AnonymousClass13() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationPanelView.this.mKeyguardBottomArea.setVisibility(8);
            }
        };
        this.mFragmentListener = new AnonymousClass22();
        setWillNotDraw(true);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        setAccessibilityPaneTitle(determineAccessibilityPaneTitle());
        this.mAlphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        setPanelAlpha(255, false);
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
        this.mKeyguardBottomArea.setStatusBar(this.mStatusBar);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView, android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mKeyguardStatusBar = (KeyguardStatusBarView) findViewById(R.id.keyguard_header);
        this.mKeyguardStatusView = (KeyguardStatusView) findViewById(R.id.keyguard_status_view);
        this.mNotificationContainerParent = (NotificationsQuickSettingsContainer) findViewById(R.id.notification_container_parent);
        this.mNotificationStackScroller = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationStackScroller.setOnHeightChangedListener(this);
        this.mNotificationStackScroller.setOverscrollTopChangedListener(this);
        this.mNotificationStackScroller.setOnEmptySpaceClickListener(this);
        final NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        Objects.requireNonNull(notificationStackScrollLayout);
        addTrackingHeadsUpListener(new Consumer() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$xhbxghYggVvH-Fewrs8cwC_bnGY
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                notificationStackScrollLayout.setTrackingHeadsUp((ExpandableNotificationRow) obj);
            }
        });
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) findViewById(R.id.keyguard_bottom_area);
        this.mQsNavbarScrim = findViewById(R.id.qs_navbar_scrim);
        this.mLastOrientation = getResources().getConfiguration().orientation;
        initBottomArea();
        this.mQsFrame = (FrameLayout) findViewById(R.id.qs_frame);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView, android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        FragmentHostManager.get(this).addTagListener(QS.TAG, this.mFragmentListener);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        FragmentHostManager.get(this).removeTagListener(QS.TAG, this.mFragmentListener);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void loadDimens() {
        super.loadDimens();
        this.mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.4f);
        this.mStatusBarMinHeight = getResources().getDimensionPixelSize(android.R.dimen.indeterminate_progress_alpha_16);
        this.mQsPeekHeight = getResources().getDimensionPixelSize(R.dimen.qs_peek_height);
        this.mNotificationsHeaderCollideDistance = getResources().getDimensionPixelSize(R.dimen.header_notifications_collide_distance);
        this.mUnlockMoveDistance = getResources().getDimensionPixelOffset(R.dimen.unlock_move_distance);
        this.mClockPositionAlgorithm.loadDimens(getResources());
        this.mQsFalsingThreshold = getResources().getDimensionPixelSize(R.dimen.qs_falsing_threshold);
        this.mPositionMinSideMargin = getResources().getDimensionPixelSize(R.dimen.notification_panel_min_side_margin);
        this.mMaxFadeoutHeight = getResources().getDimensionPixelSize(R.dimen.max_notification_fadeout_height);
        this.mIndicationBottomPadding = getResources().getDimensionPixelSize(R.dimen.keyguard_indication_bottom_padding);
        this.mQsNotificationTopPadding = getResources().getDimensionPixelSize(R.dimen.qs_notification_padding);
    }

    public void updateResources() {
        Resources resources = getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.qs_panel_width);
        int integer = getResources().getInteger(R.integer.notification_panel_layout_gravity);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.mQsFrame.getLayoutParams();
        if (layoutParams.width != dimensionPixelSize || layoutParams.gravity != integer) {
            layoutParams.width = dimensionPixelSize;
            layoutParams.gravity = integer;
            this.mQsFrame.setLayoutParams(layoutParams);
        }
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.notification_panel_width);
        FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) this.mNotificationStackScroller.getLayoutParams();
        if (layoutParams2.width != dimensionPixelSize2 || layoutParams2.gravity != integer) {
            layoutParams2.width = dimensionPixelSize2;
            layoutParams2.gravity = integer;
            this.mNotificationStackScroller.setLayoutParams(layoutParams2);
        }
    }

    public void onThemeChanged() {
        int iIndexOfChild = indexOfChild(this.mKeyguardStatusView);
        removeView(this.mKeyguardStatusView);
        this.mKeyguardStatusView = (KeyguardStatusView) LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_status_view, (ViewGroup) this, false);
        addView(this.mKeyguardStatusView, iIndexOfChild);
        int iIndexOfChild2 = indexOfChild(this.mKeyguardBottomArea);
        removeView(this.mKeyguardBottomArea);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_bottom_area, (ViewGroup) this, false);
        addView(this.mKeyguardBottomArea, iIndexOfChild2);
        initBottomArea();
        setDarkAmount(this.mDarkAmount);
        setKeyguardStatusViewVisibility(this.mStatusBarState, false, false);
        setKeyguardBottomAreaVisibility(this.mStatusBarState, false);
    }

    private void initBottomArea() {
        this.mAffordanceHelper = new KeyguardAffordanceHelper(this, getContext());
        this.mKeyguardBottomArea.setAffordanceHelper(this.mAffordanceHelper);
        this.mKeyguardBottomArea.setStatusBar(this.mStatusBar);
        this.mKeyguardBottomArea.setUserSetupComplete(this.mUserSetupComplete);
    }

    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        this.mKeyguardBottomArea.setKeyguardIndicationController(keyguardIndicationController);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView, android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        setIsFullWidth(this.mNotificationStackScroller.getWidth() == getWidth());
        this.mKeyguardStatusView.setPivotX(getWidth() / 2);
        this.mKeyguardStatusView.setPivotY(0.34521484f * this.mKeyguardStatusView.getClockTextSize());
        int i5 = this.mQsMaxExpansionHeight;
        if (this.mQs != null) {
            this.mQsMinExpansionHeight = this.mKeyguardShowing ? 0 : this.mQs.getQsMinExpansionHeight();
            this.mQsMaxExpansionHeight = this.mQs.getDesiredHeight();
            this.mNotificationStackScroller.setMaxTopPadding(this.mQsMaxExpansionHeight + this.mQsNotificationTopPadding);
        }
        positionClockAndNotifications();
        if (this.mQsExpanded && this.mQsFullyExpanded) {
            this.mQsExpansionHeight = this.mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
            if (this.mQsMaxExpansionHeight != i5) {
                startQsSizeChangeAnimation(i5, this.mQsMaxExpansionHeight);
            }
        } else if (!this.mQsExpanded) {
            setQsExpansion(this.mQsMinExpansionHeight + this.mLastOverscroll);
        }
        updateExpandedHeight(getExpandedHeight());
        updateHeader();
        if (this.mQsSizeChangeAnimator == null && this.mQs != null) {
            this.mQs.setHeightOverride(this.mQs.getDesiredHeight());
        }
        updateMaxHeadsUpTranslation();
    }

    private void setIsFullWidth(boolean z) {
        this.mIsFullWidth = z;
        this.mNotificationStackScroller.setIsFullWidth(z);
    }

    private void startQsSizeChangeAnimation(int i, int i2) {
        if (this.mQsSizeChangeAnimator != null) {
            i = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
            this.mQsSizeChangeAnimator.cancel();
        }
        this.mQsSizeChangeAnimator = ValueAnimator.ofInt(i, i2);
        this.mQsSizeChangeAnimator.setDuration(300L);
        this.mQsSizeChangeAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        this.mQsSizeChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.4
            AnonymousClass4() {
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                NotificationPanelView.this.requestScrollerTopPaddingUpdate(false);
                NotificationPanelView.this.requestPanelHeightUpdate();
                NotificationPanelView.this.mQs.setHeightOverride(((Integer) NotificationPanelView.this.mQsSizeChangeAnimator.getAnimatedValue()).intValue());
            }
        });
        this.mQsSizeChangeAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.5
            AnonymousClass5() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                NotificationPanelView.this.mQsSizeChangeAnimator = null;
            }
        });
        this.mQsSizeChangeAnimator.start();
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$4 */
    class AnonymousClass4 implements ValueAnimator.AnimatorUpdateListener {
        AnonymousClass4() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            NotificationPanelView.this.requestScrollerTopPaddingUpdate(false);
            NotificationPanelView.this.requestPanelHeightUpdate();
            NotificationPanelView.this.mQs.setHeightOverride(((Integer) NotificationPanelView.this.mQsSizeChangeAnimator.getAnimatedValue()).intValue());
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$5 */
    class AnonymousClass5 extends AnimatorListenerAdapter {
        AnonymousClass5() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            NotificationPanelView.this.mQsSizeChangeAnimator = null;
        }
    }

    private void positionClockAndNotifications() {
        int height;
        boolean zIsAddOrRemoveAnimationPending = this.mNotificationStackScroller.isAddOrRemoveAnimationPending();
        boolean z = zIsAddOrRemoveAnimationPending || this.mAnimateNextPositionUpdate;
        if (this.mStatusBarState != 1) {
            height = (this.mQs != null ? this.mQs.getHeader().getHeight() : 0) + this.mQsPeekHeight + this.mQsNotificationTopPadding;
        } else {
            int height2 = getHeight();
            this.mClockPositionAlgorithm.setup(this.mStatusBarMinHeight, height2 - Math.max(this.mIndicationBottomPadding, this.mAmbientIndicationBottomPadding), this.mNotificationStackScroller.getIntrinsicContentHeight(), getExpandedFraction(), height2, this.mKeyguardStatusView.getHeight(), this.mDarkAmount, this.mStatusBar.isKeyguardCurrentlySecure(), this.mPulsing, this.mBouncerTop);
            this.mClockPositionAlgorithm.run(this.mClockPositionResult);
            PropertyAnimator.setProperty(this.mKeyguardStatusView, AnimatableProperty.X, this.mClockPositionResult.clockX, CLOCK_ANIMATION_PROPERTIES, z);
            PropertyAnimator.setProperty(this.mKeyguardStatusView, AnimatableProperty.Y, this.mClockPositionResult.clockY, CLOCK_ANIMATION_PROPERTIES, z);
            updateClock();
            height = this.mClockPositionResult.stackScrollerPadding;
            this.mNotificationStackScroller.setAntiBurnInOffsetX(this.mClockPositionResult.clockX);
        }
        this.mNotificationStackScroller.setIntrinsicPadding(height);
        this.mKeyguardBottomArea.setBurnInXOffset(this.mClockPositionResult.clockX);
        this.mStackScrollerMeasuringPass++;
        requestScrollerTopPaddingUpdate(zIsAddOrRemoveAnimationPending);
        this.mStackScrollerMeasuringPass = 0;
        this.mAnimateNextPositionUpdate = false;
    }

    public int computeMaxKeyguardNotifications(int i) {
        float intrinsicHeight;
        float minStackScrollerPadding = this.mClockPositionAlgorithm.getMinStackScrollerPadding();
        int iMax = Math.max(1, getResources().getDimensionPixelSize(R.dimen.notification_divider_height));
        NotificationShelf notificationShelf = this.mNotificationStackScroller.getNotificationShelf();
        if (notificationShelf.getVisibility() != 8) {
            intrinsicHeight = notificationShelf.getIntrinsicHeight() + iMax;
        } else {
            intrinsicHeight = 0.0f;
        }
        float height = (((this.mNotificationStackScroller.getHeight() - minStackScrollerPadding) - intrinsicHeight) - Math.max(this.mIndicationBottomPadding, this.mAmbientIndicationBottomPadding)) - this.mKeyguardStatusView.getLogoutButtonHeight();
        float minHeight = height;
        int i2 = 0;
        for (int i3 = 0; i3 < this.mNotificationStackScroller.getChildCount(); i3++) {
            ExpandableView expandableView = (ExpandableView) this.mNotificationStackScroller.getChildAt(i3);
            if (expandableView instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                if (!this.mGroupManager.isSummaryOfSuppressedGroup(expandableNotificationRow.getStatusBarNotification()) && this.mStatusBar.getNotificationLockscreenUserManager().shouldShowOnKeyguard(expandableNotificationRow.getStatusBarNotification()) && !expandableNotificationRow.isRemoved()) {
                    minHeight -= expandableView.getMinHeight(true) + iMax;
                    if (minHeight < 0.0f || i2 >= i) {
                        if (minHeight > (-intrinsicHeight)) {
                            for (int i4 = i3 + 1; i4 < this.mNotificationStackScroller.getChildCount(); i4++) {
                                if (this.mNotificationStackScroller.getChildAt(i4) instanceof ExpandableNotificationRow) {
                                    return i2;
                                }
                            }
                            return i2 + 1;
                        }
                        return i2;
                    }
                    i2++;
                }
            }
        }
        return i2;
    }

    public void setBouncerTop(int i) {
        this.mBouncerTop = i;
        positionClockAndNotifications();
    }

    private void updateClock() {
        if (!this.mKeyguardStatusViewAnimating) {
            this.mKeyguardStatusView.setAlpha(this.mClockPositionResult.clockAlpha);
        }
    }

    public void animateToFullShade(long j) {
        this.mNotificationStackScroller.goToFullShade(j);
        requestLayout();
        this.mAnimateNextPositionUpdate = true;
    }

    public void setQsExpansionEnabled(boolean z) {
        this.mQsExpansionEnabled = z;
        if (this.mQs == null) {
            return;
        }
        this.mQs.setHeaderClickable(z);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    public void resetViews() {
        this.mIsLaunchTransitionFinished = false;
        this.mBlockTouches = false;
        this.mUnlockIconActive = false;
        if (!this.mLaunchingAffordance) {
            this.mAffordanceHelper.reset(false);
            this.mLastCameraLaunchSource = "lockscreen_affordance";
        }
        closeQs();
        this.mStatusBar.getGutsManager().closeAndSaveGuts(true, true, true, -1, -1, true);
        this.mNotificationStackScroller.setOverScrollAmount(0.0f, true, false, true);
        this.mNotificationStackScroller.resetScrollPosition();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    public void collapse(boolean z, float f) {
        if (!canPanelBeCollapsed()) {
            return;
        }
        if (this.mQsExpanded) {
            this.mQsExpandImmediate = true;
            this.mNotificationStackScroller.setShouldShowShelfOnly(true);
        }
        super.collapse(z, f);
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
            float f = this.mQsExpansionHeight;
            this.mQsExpansionAnimator.cancel();
            setQsExpansion(f);
        }
        flingSettings(0.0f, false);
    }

    public void expandWithQs() {
        if (this.mQsExpansionEnabled) {
            this.mQsExpandImmediate = true;
            this.mNotificationStackScroller.setShouldShowShelfOnly(true);
        }
        expand(true);
    }

    public void expandWithoutQs() {
        if (isQsExpanded()) {
            flingSettings(0.0f, false);
        } else {
            expand(true);
        }
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    public void fling(float f, boolean z) {
        GestureRecorder gestureRecorder = ((PhoneStatusBarView) this.mBar).mBar.getGestureRecorder();
        if (gestureRecorder != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("fling ");
            sb.append(f > 0.0f ? "open" : "closed");
            gestureRecorder.tag(sb.toString(), "notifications,v=" + f);
        }
        super.fling(f, z);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void flingToHeight(float f, boolean z, float f2, float f3, boolean z2) {
        this.mHeadsUpTouchHelper.notifyFling(!z);
        setClosingWithAlphaFadeout(!z && getFadeoutAlpha() == 1.0f);
        super.flingToHeight(f, z, f2, f3, z2);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView, android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if ((BenesseExtension.getDchaState() != 0 && this.mStatusBarState != 1) || this.mBlockTouches || (this.mQsFullyExpanded && this.mQs.onInterceptTouchEvent(motionEvent))) {
            return false;
        }
        initDownStates(motionEvent);
        if (this.mBar.panelEnabled() && this.mHeadsUpTouchHelper.onInterceptTouchEvent(motionEvent)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open", 1);
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
            return true;
        }
        if (isFullyCollapsed() || !onQsIntercept(motionEvent)) {
            return super.onInterceptTouchEvent(motionEvent);
        }
        return true;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean onQsIntercept(MotionEvent motionEvent) {
        int iFindPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
        if (iFindPointerIndex < 0) {
            this.mTrackingPointer = motionEvent.getPointerId(0);
            iFindPointerIndex = 0;
        }
        float x = motionEvent.getX(iFindPointerIndex);
        float y = motionEvent.getY(iFindPointerIndex);
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 6) {
            switch (actionMasked) {
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
                        this.mNotificationStackScroller.cancelLongPress();
                        break;
                    }
                    break;
                case 1:
                case 3:
                    trackMovement(motionEvent);
                    if (this.mQsTracking) {
                        flingQsWithCurrentVelocity(y, motionEvent.getActionMasked() == 3);
                        this.mQsTracking = false;
                    }
                    this.mIntercepting = false;
                    break;
                case 2:
                    float f = y - this.mInitialTouchY;
                    trackMovement(motionEvent);
                    if (this.mQsTracking) {
                        setQsExpansion(f + this.mInitialHeightOnTouch);
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
                        this.mNotificationStackScroller.cancelLongPress();
                        return true;
                    }
                    break;
            }
        } else {
            int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
            if (this.mTrackingPointer == pointerId) {
                int i = motionEvent.getPointerId(0) != pointerId ? 0 : 1;
                this.mTrackingPointer = motionEvent.getPointerId(i);
                this.mInitialTouchX = motionEvent.getX(i);
                this.mInitialTouchY = motionEvent.getY(i);
            }
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean isInContentBounds(float f, float f2) {
        float x = this.mNotificationStackScroller.getX();
        return !this.mNotificationStackScroller.isBelowLastNotification(f - x, f2) && x < f && f < x + ((float) this.mNotificationStackScroller.getWidth());
    }

    private void initDownStates(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            boolean z = false;
            this.mOnlyAffordanceInThisMotion = false;
            this.mQsTouchAboveFalsingThreshold = this.mQsFullyExpanded;
            this.mDozingOnDown = isDozing();
            this.mCollapsedOnDown = isFullyCollapsed();
            if (this.mCollapsedOnDown && this.mHeadsUpManager.hasPinnedHeadsUp()) {
                z = true;
            }
            this.mListenForHeadsUp = z;
        }
    }

    private void flingQsWithCurrentVelocity(float f, boolean z) {
        float currentQSVelocity = getCurrentQSVelocity();
        boolean zFlingExpandsQs = flingExpandsQs(currentQSVelocity);
        if (zFlingExpandsQs) {
            logQsSwipeDown(f);
        }
        flingSettings(currentQSVelocity, zFlingExpandsQs && !z);
    }

    private void logQsSwipeDown(float f) {
        int i;
        float currentQSVelocity = getCurrentQSVelocity();
        if (this.mStatusBarState == 1) {
            i = 193;
        } else {
            i = 194;
        }
        this.mLockscreenGestureLogger.write(i, (int) ((f - this.mInitialTouchY) / this.mStatusBar.getDisplayDensity()), (int) (currentQSVelocity / this.mStatusBar.getDisplayDensity()));
    }

    private boolean flingExpandsQs(float f) {
        if (isFalseTouch()) {
            return false;
        }
        return Math.abs(f) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond() ? getQsExpansionFraction() > 0.5f : f > 0.0f;
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
        return Math.min(1.0f, (this.mQsExpansionHeight - this.mQsMinExpansionHeight) / (this.mQsMaxExpansionHeight - this.mQsMinExpansionHeight));
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected float getOpeningHeight() {
        return this.mNotificationStackScroller.getOpeningHeight();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (BenesseExtension.getDchaState() != 0 && this.mStatusBarState != 1) {
            return true;
        }
        boolean zOnTouchEvent = false;
        if (this.mBlockTouches || (this.mQs != null && this.mQs.isCustomizing())) {
            return false;
        }
        initDownStates(motionEvent);
        if (this.mListenForHeadsUp && !this.mHeadsUpTouchHelper.isTrackingHeadsUp() && this.mHeadsUpTouchHelper.onInterceptTouchEvent(motionEvent)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
        }
        if ((!this.mIsExpanding || this.mHintAnimationRunning) && !this.mQsExpanded && this.mStatusBar.getBarState() != 0 && !this.mDozing) {
            zOnTouchEvent = false | this.mAffordanceHelper.onTouchEvent(motionEvent);
        }
        if (this.mOnlyAffordanceInThisMotion) {
            return true;
        }
        boolean zOnTouchEvent2 = this.mHeadsUpTouchHelper.onTouchEvent(motionEvent) | zOnTouchEvent;
        if (!this.mHeadsUpTouchHelper.isTrackingHeadsUp() && handleQsTouch(motionEvent)) {
            return true;
        }
        if (motionEvent.getActionMasked() == 0 && isFullyCollapsed()) {
            MetricsLogger.count(this.mContext, "panel_open", 1);
            updateVerticalPanelPosition(motionEvent.getX());
            zOnTouchEvent2 = true;
        }
        boolean zOnTouchEvent3 = super.onTouchEvent(motionEvent) | zOnTouchEvent2;
        if (this.mDozing) {
            return zOnTouchEvent3;
        }
        return true;
    }

    private boolean handleQsTouch(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0 && getExpandedFraction() == 1.0f && this.mStatusBar.getBarState() != 1 && !this.mQsExpanded && this.mQsExpansionEnabled) {
            this.mQsTracking = true;
            this.mConflictingQsExpansionGesture = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = motionEvent.getX();
            this.mInitialTouchX = motionEvent.getY();
        }
        if (!isFullyCollapsed()) {
            handleQsDown(motionEvent);
        }
        if (!this.mQsExpandImmediate && this.mQsTracking) {
            onQsTouch(motionEvent);
            if (!this.mConflictingQsExpansionGesture) {
                return true;
            }
        }
        if (actionMasked == 3 || actionMasked == 1) {
            this.mConflictingQsExpansionGesture = false;
        }
        if (actionMasked == 0 && isFullyCollapsed() && this.mQsExpansionEnabled) {
            this.mTwoFingerQsExpandPossible = true;
        }
        if (this.mTwoFingerQsExpandPossible && isOpenQsEvent(motionEvent) && motionEvent.getY(motionEvent.getActionIndex()) < this.mStatusBarMinHeight) {
            MetricsLogger.count(this.mContext, "panel_open_qs", 1);
            this.mQsExpandImmediate = true;
            this.mNotificationStackScroller.setShouldShowShelfOnly(true);
            requestPanelHeightUpdate();
            setListening(true);
        }
        return false;
    }

    private boolean isInQsArea(float f, float f2) {
        return f >= this.mQsFrame.getX() && f <= this.mQsFrame.getX() + ((float) this.mQsFrame.getWidth()) && (f2 <= this.mNotificationStackScroller.getBottomMostNotificationBottom() || f2 <= this.mQs.getView().getY() + ((float) this.mQs.getView().getHeight()));
    }

    private boolean isOpenQsEvent(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        int actionMasked = motionEvent.getActionMasked();
        return (actionMasked == 5 && pointerCount == 2) || (actionMasked == 0 && (motionEvent.isButtonPressed(32) || motionEvent.isButtonPressed(64))) || (actionMasked == 0 && (motionEvent.isButtonPressed(2) || motionEvent.isButtonPressed(4)));
    }

    private void handleQsDown(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0 && shouldQuickSettingsIntercept(motionEvent.getX(), motionEvent.getY(), -1.0f)) {
            this.mFalsingManager.onQsDown();
            this.mQsTracking = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = motionEvent.getX();
            this.mInitialTouchX = motionEvent.getY();
            notifyExpandingFinished();
        }
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean flingExpands(float f, float f2, float f3, float f4) {
        boolean zFlingExpands = super.flingExpands(f, f2, f3, f4);
        if (this.mQsExpansionAnimator != null) {
            return true;
        }
        return zFlingExpands;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean hasConflictingGestures() {
        return this.mStatusBar.getBarState() != 0;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean shouldGestureIgnoreXTouchSlop(float f, float f2) {
        return !this.mAffordanceHelper.isOnAffordanceIcon(f, f2);
    }

    private void onQsTouch(MotionEvent motionEvent) {
        int iFindPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
        int i = 0;
        boolean z = false;
        if (iFindPointerIndex < 0) {
            this.mTrackingPointer = motionEvent.getPointerId(0);
            iFindPointerIndex = 0;
        }
        float y = motionEvent.getY(iFindPointerIndex);
        float x = motionEvent.getX(iFindPointerIndex);
        float f = y - this.mInitialTouchY;
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 6) {
            switch (actionMasked) {
                case 0:
                    this.mQsTracking = true;
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    onQsExpansionStarted();
                    this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                    initVelocityTracker();
                    trackMovement(motionEvent);
                    break;
                case 1:
                case 3:
                    this.mQsTracking = false;
                    this.mTrackingPointer = -1;
                    trackMovement(motionEvent);
                    if (getQsExpansionFraction() != 0.0f || y >= this.mInitialTouchY) {
                        if (motionEvent.getActionMasked() == 3) {
                            z = true;
                        }
                        flingQsWithCurrentVelocity(y, z);
                    }
                    if (this.mQsVelocityTracker != null) {
                        this.mQsVelocityTracker.recycle();
                        this.mQsVelocityTracker = null;
                        break;
                    }
                    break;
                case 2:
                    setQsExpansion(this.mInitialHeightOnTouch + f);
                    if (f >= getFalsingThreshold()) {
                        this.mQsTouchAboveFalsingThreshold = true;
                    }
                    trackMovement(motionEvent);
                    break;
            }
        }
        int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
        if (this.mTrackingPointer == pointerId) {
            if (motionEvent.getPointerId(0) == pointerId) {
                i = 1;
            }
            float y2 = motionEvent.getY(i);
            float x2 = motionEvent.getX(i);
            this.mTrackingPointer = motionEvent.getPointerId(i);
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = y2;
            this.mInitialTouchX = x2;
        }
    }

    private int getFalsingThreshold() {
        return (int) (this.mQsFalsingThreshold * (this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f));
    }

    @Override // com.android.systemui.statusbar.stack.NotificationStackScrollLayout.OnOverscrollTopChangedListener
    public void onOverscrollTopChanged(float f, boolean z) {
        cancelQsAnimation();
        if (!this.mQsExpansionEnabled) {
            f = 0.0f;
        }
        if (f < 1.0f) {
            f = 0.0f;
        }
        setOverScrolling(f != 0.0f && z);
        this.mQsExpansionFromOverscroll = f != 0.0f;
        this.mLastOverscroll = f;
        updateQsState();
        setQsExpansion(this.mQsMinExpansionHeight + f);
    }

    @Override // com.android.systemui.statusbar.stack.NotificationStackScrollLayout.OnOverscrollTopChangedListener
    public void flingTopOverscroll(float f, boolean z) {
        this.mLastOverscroll = 0.0f;
        this.mQsExpansionFromOverscroll = false;
        setQsExpansion(this.mQsExpansionHeight);
        if (!this.mQsExpansionEnabled && z) {
            f = 0.0f;
        }
        flingSettings(f, z && this.mQsExpansionEnabled, new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.6
            AnonymousClass6() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationPanelView.this.mStackScrollerOverscrolling = false;
                NotificationPanelView.this.setOverScrolling(false);
                NotificationPanelView.this.updateQsState();
            }
        }, false);
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$6 */
    class AnonymousClass6 implements Runnable {
        AnonymousClass6() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mStackScrollerOverscrolling = false;
            NotificationPanelView.this.setOverScrolling(false);
            NotificationPanelView.this.updateQsState();
        }
    }

    private void setOverScrolling(boolean z) {
        this.mStackScrollerOverscrolling = z;
        if (this.mQs == null) {
            return;
        }
        this.mQs.setOverscrolling(z);
    }

    private void onQsExpansionStarted() {
        onQsExpansionStarted(0);
    }

    protected void onQsExpansionStarted(int i) {
        cancelQsAnimation();
        cancelHeightAnimator();
        setQsExpansion(this.mQsExpansionHeight - i);
        requestPanelHeightUpdate();
        this.mNotificationStackScroller.checkSnoozeLeavebehind();
    }

    private void setQsExpanded(boolean z) {
        if (this.mQsExpanded != z) {
            this.mQsExpanded = z;
            updateQsState();
            requestPanelHeightUpdate();
            this.mFalsingManager.setQsExpanded(z);
            this.mStatusBar.setQsExpanded(z);
            this.mNotificationContainerParent.setQsExpanded(z);
        }
    }

    public void setBarState(int i, boolean z, boolean z2) {
        int i2 = this.mStatusBarState;
        boolean z3 = i == 1;
        setKeyguardStatusViewVisibility(i, z, z2);
        setKeyguardBottomAreaVisibility(i, z2);
        this.mStatusBarState = i;
        this.mKeyguardShowing = z3;
        if (this.mQs != null) {
            this.mQs.setKeyguardShowing(this.mKeyguardShowing);
        }
        if (i2 != 1 || (!z2 && i != 2)) {
            if (i2 == 2 && i == 1) {
                animateKeyguardStatusBarIn(360L);
                this.mQs.animateHeaderSlidingOut();
            } else {
                this.mKeyguardStatusBar.setAlpha(1.0f);
                this.mKeyguardStatusBar.setVisibility(z3 ? 0 : 4);
                if (z3 && i2 != this.mStatusBarState) {
                    this.mKeyguardBottomArea.onKeyguardShowingChanged();
                    if (this.mQs != null) {
                        this.mQs.hideImmediately();
                    }
                }
            }
        } else {
            animateKeyguardStatusBarOut();
            this.mQs.animateHeaderSlidingIn(this.mStatusBarState == 2 ? 0L : this.mStatusBar.calculateGoingToFullShadeDelay());
        }
        if (z3) {
            updateDozingVisibilities(false);
        }
        resetVerticalPanelPosition();
        updateQsState();
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$7 */
    class AnonymousClass7 implements Runnable {
        AnonymousClass7() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
            NotificationPanelView.this.mKeyguardStatusView.setVisibility(4);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$8 */
    class AnonymousClass8 implements Runnable {
        AnonymousClass8() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
            NotificationPanelView.this.mKeyguardStatusView.setVisibility(8);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$9 */
    class AnonymousClass9 implements Runnable {
        AnonymousClass9() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$10 */
    class AnonymousClass10 implements Runnable {
        AnonymousClass10() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mKeyguardStatusBar.setVisibility(4);
            NotificationPanelView.this.mKeyguardStatusBar.setAlpha(1.0f);
            NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = 1.0f;
        }
    }

    private void animateKeyguardStatusBarOut() {
        long keyguardFadingAwayDelay;
        long keyguardFadingAwayDuration;
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.mKeyguardStatusBar.getAlpha(), 0.0f);
        valueAnimatorOfFloat.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        if (this.mStatusBar.isKeyguardFadingAway()) {
            keyguardFadingAwayDelay = this.mStatusBar.getKeyguardFadingAwayDelay();
        } else {
            keyguardFadingAwayDelay = 0;
        }
        valueAnimatorOfFloat.setStartDelay(keyguardFadingAwayDelay);
        if (this.mStatusBar.isKeyguardFadingAway()) {
            keyguardFadingAwayDuration = this.mStatusBar.getKeyguardFadingAwayDuration() / 2;
        } else {
            keyguardFadingAwayDuration = 360;
        }
        valueAnimatorOfFloat.setDuration(keyguardFadingAwayDuration);
        valueAnimatorOfFloat.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.11
            AnonymousClass11() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                NotificationPanelView.this.mAnimateKeyguardStatusBarInvisibleEndRunnable.run();
            }
        });
        valueAnimatorOfFloat.start();
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$11 */
    class AnonymousClass11 extends AnimatorListenerAdapter {
        AnonymousClass11() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            NotificationPanelView.this.mAnimateKeyguardStatusBarInvisibleEndRunnable.run();
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$12 */
    class AnonymousClass12 implements ValueAnimator.AnimatorUpdateListener {
        AnonymousClass12() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            NotificationPanelView.this.updateHeaderKeyguardAlpha();
        }
    }

    private void animateKeyguardStatusBarIn(long j) {
        this.mKeyguardStatusBar.setVisibility(0);
        this.mKeyguardStatusBar.setAlpha(0.0f);
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        valueAnimatorOfFloat.setDuration(j);
        valueAnimatorOfFloat.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        valueAnimatorOfFloat.start();
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$13 */
    class AnonymousClass13 implements Runnable {
        AnonymousClass13() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mKeyguardBottomArea.setVisibility(8);
        }
    }

    private void setKeyguardBottomAreaVisibility(int i, boolean z) {
        this.mKeyguardBottomArea.animate().cancel();
        if (z) {
            this.mKeyguardBottomArea.animate().alpha(0.0f).setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(this.mAnimateKeyguardBottomAreaInvisibleEndRunnable).start();
            return;
        }
        if (i == 1 || i == 2) {
            this.mKeyguardBottomArea.setVisibility(0);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        } else {
            this.mKeyguardBottomArea.setVisibility(8);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        }
    }

    private void setKeyguardStatusViewVisibility(int i, boolean z, boolean z2) {
        this.mKeyguardStatusView.animate().cancel();
        this.mKeyguardStatusViewAnimating = false;
        if ((!z && this.mStatusBarState == 1 && i != 1) || z2) {
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.animate().alpha(0.0f).setStartDelay(0L).setDuration(160L).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(this.mAnimateKeyguardStatusViewGoneEndRunnable);
            if (z) {
                this.mKeyguardStatusView.animate().setStartDelay(this.mStatusBar.getKeyguardFadingAwayDelay()).setDuration(this.mStatusBar.getKeyguardFadingAwayDuration() / 2).start();
                return;
            }
            return;
        }
        if (this.mStatusBarState == 2 && i == 1) {
            this.mKeyguardStatusView.setVisibility(0);
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.setAlpha(0.0f);
            this.mKeyguardStatusView.animate().alpha(1.0f).setStartDelay(0L).setDuration(320L).setInterpolator(Interpolators.ALPHA_IN).withEndAction(this.mAnimateKeyguardStatusViewVisibleEndRunnable);
            return;
        }
        if (i == 1) {
            if (!z) {
                this.mKeyguardStatusView.setVisibility(0);
                this.mKeyguardStatusView.setAlpha(1.0f);
                return;
            } else {
                this.mKeyguardStatusViewAnimating = true;
                this.mKeyguardStatusView.animate().alpha(0.0f).translationYBy((-getHeight()) * 0.05f).setInterpolator(Interpolators.FAST_OUT_LINEAR_IN).setDuration(125L).setStartDelay(0L).withEndAction(this.mAnimateKeyguardStatusViewInvisibleEndRunnable).start();
                return;
            }
        }
        this.mKeyguardStatusView.setVisibility(8);
        this.mKeyguardStatusView.setAlpha(1.0f);
    }

    private void updateQsState() {
        this.mNotificationStackScroller.setQsExpanded(this.mQsExpanded);
        this.mNotificationStackScroller.setScrollingEnabled(this.mStatusBarState != 1 && (!this.mQsExpanded || this.mQsExpansionFromOverscroll));
        updateEmptyShadeView();
        this.mQsNavbarScrim.setVisibility((this.mStatusBarState == 0 && this.mQsExpanded && !this.mStackScrollerOverscrolling && this.mQsScrimEnabled) ? 0 : 4);
        if (this.mKeyguardUserSwitcher != null && this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            this.mKeyguardUserSwitcher.hideIfNotSimple(true);
        }
        if (this.mQs == null) {
            return;
        }
        this.mQs.setExpanded(this.mQsExpanded);
    }

    private void setQsExpansion(float f) {
        float fMin = Math.min(Math.max(f, this.mQsMinExpansionHeight), this.mQsMaxExpansionHeight);
        this.mQsFullyExpanded = fMin == ((float) this.mQsMaxExpansionHeight) && this.mQsMaxExpansionHeight != 0;
        if (fMin > this.mQsMinExpansionHeight && !this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            setQsExpanded(true);
        } else if (fMin <= this.mQsMinExpansionHeight && this.mQsExpanded) {
            setQsExpanded(false);
        }
        this.mQsExpansionHeight = fMin;
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
        if (this.mAccessibilityManager.isEnabled()) {
            setAccessibilityPaneTitle(determineAccessibilityPaneTitle());
        }
        if (this.mQsFullyExpanded && this.mFalsingManager.shouldEnforceBouncer()) {
            this.mStatusBar.executeRunnableDismissingKeyguard(null, null, false, true, false);
        }
    }

    protected void updateQsExpansion() {
        if (this.mQs == null) {
            return;
        }
        float qsExpansionFraction = getQsExpansionFraction();
        this.mQs.setQsExpansion(qsExpansionFraction, getHeaderTranslation());
        this.mNotificationStackScroller.setQsExpansionFraction(qsExpansionFraction);
    }

    private String determineAccessibilityPaneTitle() {
        if (this.mQs != null && this.mQs.isCustomizing()) {
            return getContext().getString(R.string.accessibility_desc_quick_settings_edit);
        }
        if (this.mQsExpansionHeight != 0.0f && this.mQsFullyExpanded) {
            return getContext().getString(R.string.accessibility_desc_quick_settings);
        }
        if (this.mStatusBarState == 1) {
            return getContext().getString(R.string.accessibility_desc_lock_screen);
        }
        return getContext().getString(R.string.accessibility_desc_notification_shade);
    }

    private float calculateQsTopPadding() {
        if (this.mKeyguardShowing && (this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted))) {
            int i = this.mClockPositionResult.stackScrollerPadding;
            int iMax = this.mQsMaxExpansionHeight + this.mQsNotificationTopPadding;
            if (this.mStatusBarState == 1) {
                iMax = Math.max(i, iMax);
            }
            return (int) interpolate(getExpandedFraction(), this.mQsMinExpansionHeight, iMax);
        }
        if (this.mQsSizeChangeAnimator != null) {
            return ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        }
        if (this.mKeyguardShowing) {
            return interpolate(getQsExpansionFraction(), this.mNotificationStackScroller.getIntrinsicPadding(), this.mQsMaxExpansionHeight + this.mQsNotificationTopPadding);
        }
        return this.mQsExpansionHeight + this.mQsNotificationTopPadding;
    }

    protected void requestScrollerTopPaddingUpdate(boolean z) {
        this.mNotificationStackScroller.updateTopPadding(calculateQsTopPadding(), z, this.mKeyguardShowing && (this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)));
    }

    private void trackMovement(MotionEvent motionEvent) {
        if (this.mQsVelocityTracker != null) {
            this.mQsVelocityTracker.addMovement(motionEvent);
        }
        this.mLastTouchX = motionEvent.getX();
        this.mLastTouchY = motionEvent.getY();
    }

    private void initVelocityTracker() {
        if (this.mQsVelocityTracker != null) {
            this.mQsVelocityTracker.recycle();
        }
        this.mQsVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentQSVelocity() {
        if (this.mQsVelocityTracker == null) {
            return 0.0f;
        }
        this.mQsVelocityTracker.computeCurrentVelocity(1000);
        return this.mQsVelocityTracker.getYVelocity();
    }

    private void cancelQsAnimation() {
        if (this.mQsExpansionAnimator != null) {
            this.mQsExpansionAnimator.cancel();
        }
    }

    public void flingSettings(float f, boolean z) {
        flingSettings(f, z, null, false);
    }

    protected void flingSettings(float f, boolean z, Runnable runnable, boolean z2) {
        boolean z3;
        float f2 = z ? this.mQsMaxExpansionHeight : this.mQsMinExpansionHeight;
        if (f2 == this.mQsExpansionHeight) {
            if (runnable != null) {
                runnable.run();
                return;
            }
            return;
        }
        if ((f <= 0.0f || z) && (f >= 0.0f || !z)) {
            z3 = false;
        } else {
            f = 0.0f;
            z3 = true;
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.mQsExpansionHeight, f2);
        if (z2) {
            valueAnimatorOfFloat.setInterpolator(Interpolators.TOUCH_RESPONSE);
            valueAnimatorOfFloat.setDuration(368L);
        } else {
            this.mFlingAnimationUtils.apply(valueAnimatorOfFloat, this.mQsExpansionHeight, f2, f);
        }
        if (z3) {
            valueAnimatorOfFloat.setDuration(350L);
        }
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.14
            AnonymousClass14() {
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                NotificationPanelView.this.setQsExpansion(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.15
            final /* synthetic */ Runnable val$onFinishRunnable;

            AnonymousClass15(Runnable runnable2) {
                runnable = runnable2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                NotificationPanelView.this.mNotificationStackScroller.resetCheckSnoozeLeavebehind();
                NotificationPanelView.this.mQsExpansionAnimator = null;
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        valueAnimatorOfFloat.start();
        this.mQsExpansionAnimator = valueAnimatorOfFloat;
        this.mQsAnimatorExpand = z;
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$14 */
    class AnonymousClass14 implements ValueAnimator.AnimatorUpdateListener {
        AnonymousClass14() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            NotificationPanelView.this.setQsExpansion(((Float) valueAnimator.getAnimatedValue()).floatValue());
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$15 */
    class AnonymousClass15 extends AnimatorListenerAdapter {
        final /* synthetic */ Runnable val$onFinishRunnable;

        AnonymousClass15(Runnable runnable2) {
            runnable = runnable2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            NotificationPanelView.this.mNotificationStackScroller.resetCheckSnoozeLeavebehind();
            NotificationPanelView.this.mQsExpansionAnimator = null;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    private boolean shouldQuickSettingsIntercept(float f, float f2, float f3) {
        if (!this.mQsExpansionEnabled || this.mCollapsedOnDown) {
            return false;
        }
        View header = this.mKeyguardShowing ? this.mKeyguardStatusBar : this.mQs.getHeader();
        boolean z = f >= this.mQsFrame.getX() && f <= this.mQsFrame.getX() + ((float) this.mQsFrame.getWidth()) && f2 >= ((float) header.getTop()) && f2 <= ((float) header.getBottom());
        if (this.mQsExpanded) {
            return z || (f3 < 0.0f && isInQsArea(f, f2));
        }
        return z;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean isScrolledToBottom() {
        return isInSettings() || this.mStatusBar.getBarState() == 1 || this.mNotificationStackScroller.isScrolledToBottom();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected int getMaxPanelHeight() {
        int iCalculatePanelHeightQsExpanded;
        int iMax = this.mStatusBarMinHeight;
        if (this.mStatusBar.getBarState() != 1 && this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            iMax = Math.max(iMax, (int) (this.mQsMinExpansionHeight + getOverExpansionAmount()));
        }
        if (this.mQsExpandImmediate || this.mQsExpanded || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) {
            iCalculatePanelHeightQsExpanded = calculatePanelHeightQsExpanded();
        } else {
            iCalculatePanelHeightQsExpanded = calculatePanelHeightShade();
        }
        return Math.max(iCalculatePanelHeightQsExpanded, iMax);
    }

    public boolean isInSettings() {
        return this.mQsExpanded;
    }

    public boolean isExpanding() {
        return this.mIsExpanding;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void onHeightUpdated(float f) {
        float fCalculatePanelHeightQsExpanded;
        if ((!this.mQsExpanded || this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) && this.mStackScrollerMeasuringPass <= 2) {
            positionClockAndNotifications();
        }
        if (this.mQsExpandImmediate || (this.mQsExpanded && !this.mQsTracking && this.mQsExpansionAnimator == null && !this.mQsExpansionFromOverscroll)) {
            if (this.mKeyguardShowing) {
                fCalculatePanelHeightQsExpanded = f / getMaxPanelHeight();
            } else {
                float intrinsicPadding = this.mNotificationStackScroller.getIntrinsicPadding() + this.mNotificationStackScroller.getLayoutMinHeight();
                fCalculatePanelHeightQsExpanded = (f - intrinsicPadding) / (calculatePanelHeightQsExpanded() - intrinsicPadding);
            }
            setQsExpansion(this.mQsMinExpansionHeight + (fCalculatePanelHeightQsExpanded * (this.mQsMaxExpansionHeight - this.mQsMinExpansionHeight)));
        }
        updateExpandedHeight(f);
        updateHeader();
        updateUnlockIcon();
        updateNotificationTranslucency();
        updatePanelExpanded();
        this.mNotificationStackScroller.setShadeExpanded(!isFullyCollapsed());
    }

    private void updatePanelExpanded() {
        boolean z = !isFullyCollapsed();
        if (this.mPanelExpanded != z) {
            this.mHeadsUpManager.setIsPanelExpanded(z);
            this.mStatusBar.setPanelExpanded(z);
            this.mPanelExpanded = z;
        }
    }

    private int calculatePanelHeightShade() {
        int height = (int) ((this.mNotificationStackScroller.getHeight() - this.mNotificationStackScroller.getEmptyBottomMargin()) + this.mNotificationStackScroller.getTopPaddingOverflow());
        if (this.mStatusBarState == 1) {
            return Math.max(height, this.mClockPositionAlgorithm.getExpandedClockPosition() + this.mKeyguardStatusView.getHeight() + this.mNotificationStackScroller.getIntrinsicContentHeight());
        }
        return height;
    }

    private int calculatePanelHeightQsExpanded() {
        float height = (this.mNotificationStackScroller.getHeight() - this.mNotificationStackScroller.getEmptyBottomMargin()) - this.mNotificationStackScroller.getTopPadding();
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0 && this.mShowEmptyShadeView) {
            height = this.mNotificationStackScroller.getEmptyShadeViewHeight();
        }
        int iIntValue = this.mQsMaxExpansionHeight;
        if (this.mKeyguardShowing) {
            iIntValue += this.mQsNotificationTopPadding;
        }
        if (this.mQsSizeChangeAnimator != null) {
            iIntValue = ((Integer) this.mQsSizeChangeAnimator.getAnimatedValue()).intValue();
        }
        float fMax = Math.max(iIntValue, this.mStatusBarState == 1 ? this.mClockPositionResult.stackScrollerPadding : 0) + height + this.mNotificationStackScroller.getTopPaddingOverflow();
        if (fMax > this.mNotificationStackScroller.getHeight()) {
            fMax = Math.max(iIntValue + this.mNotificationStackScroller.getLayoutMinHeight(), this.mNotificationStackScroller.getHeight());
        }
        return (int) fMax;
    }

    private void updateNotificationTranslucency() {
        float fadeoutAlpha;
        if (this.mClosingWithAlphaFadeOut && !this.mExpandingFromHeadsUp && !this.mHeadsUpManager.hasPinnedHeadsUp()) {
            fadeoutAlpha = getFadeoutAlpha();
        } else {
            fadeoutAlpha = 1.0f;
        }
        this.mNotificationStackScroller.setAlpha(fadeoutAlpha);
    }

    private float getFadeoutAlpha() {
        return (float) Math.pow(Math.max(0.0f, Math.min((getNotificationsTopY() + this.mNotificationStackScroller.getFirstItemMinHeight()) / this.mQsMinExpansionHeight, 1.0f)), 0.75d);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected float getOverExpansionAmount() {
        return this.mNotificationStackScroller.getCurrentOverScrollAmount(true);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected float getOverExpansionPixels() {
        return this.mNotificationStackScroller.getCurrentOverScrolledPixels(true);
    }

    private void updateUnlockIcon() {
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            boolean z = ((float) getMaxPanelHeight()) - getExpandedHeight() > ((float) this.mUnlockMoveDistance);
            LockIcon lockIcon = this.mKeyguardBottomArea.getLockIcon();
            if (z && !this.mUnlockIconActive && this.mTracking) {
                lockIcon.setImageAlpha(1.0f, true, 150L, Interpolators.FAST_OUT_LINEAR_IN, null);
                lockIcon.setImageScale(1.2f, true, 150L, Interpolators.FAST_OUT_LINEAR_IN);
            } else if (!z && this.mUnlockIconActive && this.mTracking) {
                lockIcon.setImageAlpha(lockIcon.getRestingAlpha(), true, 150L, Interpolators.FAST_OUT_LINEAR_IN, null);
                lockIcon.setImageScale(1.0f, true, 150L, Interpolators.FAST_OUT_LINEAR_IN);
            }
            this.mUnlockIconActive = z;
        }
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
        return Math.min(0.0f, MathUtils.lerp(-this.mQsMinExpansionHeight, 0.0f, Math.min(1.0f, this.mNotificationStackScroller.getAppearFraction(this.mExpandedHeight))) + this.mExpandOffset);
    }

    private float getKeyguardContentsAlpha() {
        float notificationsTopY;
        if (this.mStatusBar.getBarState() == 1) {
            notificationsTopY = getNotificationsTopY() / (this.mKeyguardStatusBar.getHeight() + this.mNotificationsHeaderCollideDistance);
        } else {
            notificationsTopY = getNotificationsTopY() / this.mKeyguardStatusBar.getHeight();
        }
        return (float) Math.pow(MathUtils.constrain(notificationsTopY, 0.0f, 1.0f), 0.75d);
    }

    private void updateHeaderKeyguardAlpha() {
        this.mKeyguardStatusBar.setAlpha(Math.min(getKeyguardContentsAlpha(), 1.0f - Math.min(1.0f, getQsExpansionFraction() * 2.0f)) * this.mKeyguardStatusBarAnimateAlpha);
        this.mKeyguardStatusBar.setVisibility((this.mKeyguardStatusBar.getAlpha() == 0.0f || this.mDozing) ? 4 : 0);
    }

    private void updateKeyguardBottomAreaAlpha() {
        int i;
        float fMin = Math.min(MathUtils.map(isUnlockHintRunning() ? 0.0f : 0.95f, 1.0f, 0.0f, 1.0f, getExpandedFraction()), 1.0f - getQsExpansionFraction());
        this.mKeyguardBottomArea.setAlpha(fMin);
        KeyguardBottomAreaView keyguardBottomAreaView = this.mKeyguardBottomArea;
        if (fMin == 0.0f) {
            i = 4;
        } else {
            i = 0;
        }
        keyguardBottomAreaView.setImportantForAccessibility(i);
        View ambientIndicationContainer = this.mStatusBar.getAmbientIndicationContainer();
        if (ambientIndicationContainer != null) {
            ambientIndicationContainer.setAlpha(fMin);
        }
    }

    private float getNotificationsTopY() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            return getExpandedHeight();
        }
        return this.mNotificationStackScroller.getNotificationsTopY();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void onExpandingStarted() {
        super.onExpandingStarted();
        this.mNotificationStackScroller.onExpansionStarted();
        this.mIsExpanding = true;
        this.mQsExpandedWhenExpandingStarted = this.mQsFullyExpanded;
        if (this.mQsExpanded) {
            onQsExpansionStarted();
        }
        if (this.mQs == null) {
            return;
        }
        this.mQs.setHeaderListening(true);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void onExpandingFinished() {
        super.onExpandingFinished();
        this.mNotificationStackScroller.onExpansionStopped();
        this.mHeadsUpManager.onExpandingFinished();
        this.mIsExpanding = false;
        if (isFullyCollapsed()) {
            DejankUtils.postAfterTraversal(new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.16
                AnonymousClass16() {
                }

                @Override // java.lang.Runnable
                public void run() {
                    NotificationPanelView.this.setListening(false);
                }
            });
            postOnAnimation(new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.17
                AnonymousClass17() {
                }

                @Override // java.lang.Runnable
                public void run() {
                    NotificationPanelView.this.getParent().invalidateChild(NotificationPanelView.this, NotificationPanelView.mDummyDirtyRect);
                }
            });
        } else {
            setListening(true);
        }
        this.mQsExpandImmediate = false;
        this.mNotificationStackScroller.setShouldShowShelfOnly(false);
        this.mTwoFingerQsExpandPossible = false;
        this.mIsExpansionFromHeadsUp = false;
        notifyListenersTrackingHeadsUp(null);
        this.mExpandingFromHeadsUp = false;
        setPanelScrimMinFraction(0.0f);
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$16 */
    class AnonymousClass16 implements Runnable {
        AnonymousClass16() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.setListening(false);
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$17 */
    class AnonymousClass17 implements Runnable {
        AnonymousClass17() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.getParent().invalidateChild(NotificationPanelView.this, NotificationPanelView.mDummyDirtyRect);
        }
    }

    private void notifyListenersTrackingHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        for (int i = 0; i < this.mTrackingHeadsUpListeners.size(); i++) {
            this.mTrackingHeadsUpListeners.get(i).accept(expandableNotificationRow);
        }
    }

    private void setListening(boolean z) {
        this.mKeyguardStatusBar.setListening(z);
        if (this.mQs == null) {
            return;
        }
        this.mQs.setListening(z);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    public void expand(boolean z) {
        super.expand(z);
        setListening(true);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void setOverExpansion(float f, boolean z) {
        if (!this.mConflictingQsExpansionGesture && !this.mQsExpandImmediate && this.mStatusBar.getBarState() != 1) {
            this.mNotificationStackScroller.setOnHeightChangedListener(null);
            if (z) {
                this.mNotificationStackScroller.setOverScrolledPixels(f, true, false);
            } else {
                this.mNotificationStackScroller.setOverScrollAmount(f, true, false);
            }
            this.mNotificationStackScroller.setOnHeightChangedListener(this);
        }
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void onTrackingStarted() {
        this.mFalsingManager.onTrackingStarted(this.mStatusBar.isKeyguardCurrentlySecure());
        super.onTrackingStarted();
        if (this.mQsFullyExpanded) {
            this.mQsExpandImmediate = true;
            this.mNotificationStackScroller.setShouldShowShelfOnly(true);
        }
        if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
            this.mAffordanceHelper.animateHideLeftRightIcon();
        }
        this.mNotificationStackScroller.onPanelTrackingStarted();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void onTrackingStopped(boolean z) {
        this.mFalsingManager.onTrackingStopped();
        super.onTrackingStopped(z);
        if (z) {
            this.mNotificationStackScroller.setOverScrolledPixels(0.0f, true, true);
        }
        this.mNotificationStackScroller.onPanelTrackingStopped();
        if (z && ((this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) && !this.mHintAnimationRunning)) {
            this.mAffordanceHelper.reset(true);
        }
        if (!z) {
            if (this.mStatusBar.getBarState() == 1 || this.mStatusBar.getBarState() == 2) {
                LockIcon lockIcon = this.mKeyguardBottomArea.getLockIcon();
                lockIcon.setImageAlpha(0.0f, true, 100L, Interpolators.FAST_OUT_LINEAR_IN, null);
                lockIcon.setImageScale(2.0f, true, 100L, Interpolators.FAST_OUT_LINEAR_IN);
            }
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView.OnHeightChangedListener
    public void onHeightChanged(ExpandableView expandableView, boolean z) {
        ExpandableNotificationRow expandableNotificationRow;
        if (expandableView == null && this.mQsExpanded) {
            return;
        }
        if (z && this.mDarkAmount == 0.0f) {
            this.mAnimateNextPositionUpdate = true;
        }
        ExpandableView firstChildNotGone = this.mNotificationStackScroller.getFirstChildNotGone();
        if (firstChildNotGone instanceof ExpandableNotificationRow) {
            expandableNotificationRow = (ExpandableNotificationRow) firstChildNotGone;
        } else {
            expandableNotificationRow = null;
        }
        if (expandableNotificationRow != null && (expandableView == expandableNotificationRow || expandableNotificationRow.getNotificationParent() == expandableNotificationRow)) {
            requestScrollerTopPaddingUpdate(false);
        }
        requestPanelHeightUpdate();
    }

    @Override // com.android.systemui.statusbar.ExpandableView.OnHeightChangedListener
    public void onReset(ExpandableView expandableView) {
    }

    @Override // com.android.systemui.plugins.qs.QS.HeightListener
    public void onQsHeightChanged() {
        this.mQsMaxExpansionHeight = this.mQs != null ? this.mQs.getDesiredHeight() : 0;
        if (this.mQsExpanded && this.mQsFullyExpanded) {
            this.mQsExpansionHeight = this.mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
        }
        if (this.mAccessibilityManager.isEnabled()) {
            setAccessibilityPaneTitle(determineAccessibilityPaneTitle());
        }
        this.mNotificationStackScroller.setMaxTopPadding(this.mQsMaxExpansionHeight + this.mQsNotificationTopPadding);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView, android.view.View
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mAffordanceHelper.onConfigurationChanged();
        if (configuration.orientation != this.mLastOrientation) {
            resetVerticalPanelPosition();
        }
        this.mLastOrientation = configuration.orientation;
    }

    @Override // android.view.View
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mNavigationBarBottomHeight = windowInsets.getStableInsetBottom();
        updateMaxHeadsUpTranslation();
        return windowInsets;
    }

    private void updateMaxHeadsUpTranslation() {
        this.mNotificationStackScroller.setHeadsUpBoundaries(getHeight(), this.mNavigationBarBottomHeight);
    }

    @Override // android.view.View
    public void onRtlPropertiesChanged(int i) {
        if (i != this.mOldLayoutDirection) {
            this.mAffordanceHelper.onRtlPropertiesChanged();
            this.mOldLayoutDirection = i;
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        onQsExpansionStarted();
        if (this.mQsExpanded) {
            flingSettings(0.0f, false, null, true);
        } else if (this.mQsExpansionEnabled) {
            this.mLockscreenGestureLogger.write(195, 0, 0);
            flingSettings(0.0f, true, null, true);
        }
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public void onAnimationToSideStarted(boolean z, float f, float f2) {
        if (getLayoutDirection() != 1) {
            if (z) {
                z = false;
            } else {
                z = true;
            }
        }
        this.mIsLaunchTransitionRunning = true;
        this.mLaunchAnimationEndRunnable = null;
        float displayDensity = this.mStatusBar.getDisplayDensity();
        int iAbs = Math.abs((int) (f / displayDensity));
        int iAbs2 = Math.abs((int) (f2 / displayDensity));
        if (z) {
            this.mLockscreenGestureLogger.write(190, iAbs, iAbs2);
            this.mFalsingManager.onLeftAffordanceOn();
            if (this.mFalsingManager.shouldEnforceBouncer()) {
                this.mStatusBar.executeRunnableDismissingKeyguard(new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.18
                    AnonymousClass18() {
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        NotificationPanelView.this.mKeyguardBottomArea.launchLeftAffordance();
                    }
                }, null, true, false, true);
            } else {
                this.mKeyguardBottomArea.launchLeftAffordance();
            }
        } else {
            if ("lockscreen_affordance".equals(this.mLastCameraLaunchSource)) {
                this.mLockscreenGestureLogger.write(189, iAbs, iAbs2);
            }
            this.mFalsingManager.onCameraOn();
            if (this.mFalsingManager.shouldEnforceBouncer()) {
                this.mStatusBar.executeRunnableDismissingKeyguard(new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.19
                    AnonymousClass19() {
                    }

                    @Override // java.lang.Runnable
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

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$18 */
    class AnonymousClass18 implements Runnable {
        AnonymousClass18() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mKeyguardBottomArea.launchLeftAffordance();
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$19 */
    class AnonymousClass19 implements Runnable {
        AnonymousClass19() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mKeyguardBottomArea.launchCamera(NotificationPanelView.this.mLastCameraLaunchSource);
        }
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public void onAnimationToSideEnded() {
        this.mIsLaunchTransitionRunning = false;
        this.mIsLaunchTransitionFinished = true;
        if (this.mLaunchAnimationEndRunnable != null) {
            this.mLaunchAnimationEndRunnable.run();
            this.mLaunchAnimationEndRunnable = null;
        }
        this.mStatusBar.readyForKeyguardDone();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void startUnlockHintAnimation() {
        if (this.mPowerManager.isPowerSaveMode()) {
            onUnlockHintStarted();
            onUnlockHintFinished();
        } else {
            super.startUnlockHintAnimation();
            startHighlightIconAnimation(getCenterIcon());
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$20 */
    class AnonymousClass20 implements Runnable {
        final /* synthetic */ KeyguardAffordanceView val$icon;

        AnonymousClass20(KeyguardAffordanceView keyguardAffordanceView) {
            keyguardAffordanceView = keyguardAffordanceView;
        }

        @Override // java.lang.Runnable
        public void run() {
            keyguardAffordanceView.setImageAlpha(keyguardAffordanceView.getRestingAlpha(), true, 200L, Interpolators.FAST_OUT_SLOW_IN, null);
        }
    }

    private void startHighlightIconAnimation(KeyguardAffordanceView keyguardAffordanceView) {
        keyguardAffordanceView.setImageAlpha(1.0f, true, 200L, Interpolators.FAST_OUT_SLOW_IN, new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.20
            final /* synthetic */ KeyguardAffordanceView val$icon;

            AnonymousClass20(KeyguardAffordanceView keyguardAffordanceView2) {
                keyguardAffordanceView = keyguardAffordanceView2;
            }

            @Override // java.lang.Runnable
            public void run() {
                keyguardAffordanceView.setImageAlpha(keyguardAffordanceView.getRestingAlpha(), true, 200L, Interpolators.FAST_OUT_SLOW_IN, null);
            }
        });
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public float getMaxTranslationDistance() {
        return (float) Math.hypot(getWidth(), getHeight());
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public void onSwipingStarted(boolean z) {
        this.mFalsingManager.onAffordanceSwipingStarted(z);
        if (getLayoutDirection() == 1) {
            z = !z;
        }
        if (z) {
            this.mKeyguardBottomArea.bindCameraPrewarmService();
        }
        requestDisallowInterceptTouchEvent(true);
        this.mOnlyAffordanceInThisMotion = true;
        this.mQsTracking = false;
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public void onSwipingAborted() {
        this.mFalsingManager.onAffordanceSwipingAborted();
        this.mKeyguardBottomArea.unbindCameraPrewarmService(false);
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public void onIconClicked(boolean z) {
        if (this.mHintAnimationRunning) {
            return;
        }
        this.mHintAnimationRunning = true;
        this.mAffordanceHelper.startHintAnimation(z, new Runnable() { // from class: com.android.systemui.statusbar.phone.NotificationPanelView.21
            AnonymousClass21() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationPanelView.this.mHintAnimationRunning = false;
                NotificationPanelView.this.mStatusBar.onHintFinished();
            }
        });
        if (getLayoutDirection() == 1) {
            if (z) {
                z = false;
            } else {
                z = true;
            }
        }
        if (z) {
            this.mStatusBar.onCameraHintStarted();
        } else if (this.mKeyguardBottomArea.isLeftVoiceAssist()) {
            this.mStatusBar.onVoiceAssistHintStarted();
        } else {
            this.mStatusBar.onPhoneHintStarted();
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$21 */
    class AnonymousClass21 implements Runnable {
        AnonymousClass21() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationPanelView.this.mHintAnimationRunning = false;
            NotificationPanelView.this.mStatusBar.onHintFinished();
        }
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void onUnlockHintFinished() {
        super.onUnlockHintFinished();
        this.mNotificationStackScroller.setUnlockHintRunning(false);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void onUnlockHintStarted() {
        super.onUnlockHintStarted();
        this.mNotificationStackScroller.setUnlockHintRunning(true);
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public KeyguardAffordanceView getLeftIcon() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getRightView();
        }
        return this.mKeyguardBottomArea.getLeftView();
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public KeyguardAffordanceView getCenterIcon() {
        return this.mKeyguardBottomArea.getLockIcon();
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public KeyguardAffordanceView getRightIcon() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getLeftView();
        }
        return this.mKeyguardBottomArea.getRightView();
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public View getLeftPreview() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getRightPreview();
        }
        return this.mKeyguardBottomArea.getLeftPreview();
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public View getRightPreview() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getLeftPreview();
        }
        return this.mKeyguardBottomArea.getRightPreview();
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public float getAffordanceFalsingFactor() {
        return this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback
    public boolean needsAntiFalsing() {
        return this.mStatusBarState == 1;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected float getPeekHeight() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() > 0) {
            return this.mNotificationStackScroller.getPeekHeight();
        }
        return this.mQsMinExpansionHeight;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean shouldUseDismissingAnimation() {
        return (this.mStatusBarState == 0 || (this.mStatusBar.isKeyguardCurrentlySecure() && isTracking())) ? false : true;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean fullyExpandedClearAllVisible() {
        return this.mNotificationStackScroller.isFooterViewNotGone() && this.mNotificationStackScroller.isScrolledToBottom() && !this.mQsExpandImmediate;
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean isClearAllVisible() {
        return this.mNotificationStackScroller.isFooterViewContentVisible();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected int getClearAllHeight() {
        return this.mNotificationStackScroller.getFooterViewHeight();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean isTrackingBlocked() {
        return this.mConflictingQsExpansionGesture && this.mQsExpanded;
    }

    public boolean isQsExpanded() {
        return this.mQsExpanded;
    }

    public boolean isQsDetailShowing() {
        return this.mQs.isShowingDetail();
    }

    public void closeQsDetail() {
        this.mQs.closeDetail();
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public boolean isLaunchTransitionFinished() {
        return this.mIsLaunchTransitionFinished;
    }

    public boolean isLaunchTransitionRunning() {
        return this.mIsLaunchTransitionRunning;
    }

    public void setLaunchTransitionEndRunnable(Runnable runnable) {
        this.mLaunchAnimationEndRunnable = runnable;
    }

    public void setEmptyDragAmount(float f) {
        float f2 = 0.4f;
        if (this.mNotificationStackScroller.getNotGoneChildCount() <= 0 && this.mStatusBar.hasActiveNotifications()) {
            f2 = 0.8f;
        }
        this.mEmptyDragAmount = f * f2;
        positionClockAndNotifications();
    }

    private static float interpolate(float f, float f2, float f3) {
        return ((1.0f - f) * f2) + (f * f3);
    }

    private void updateDozingVisibilities(boolean z) {
        if (this.mDozing) {
            this.mKeyguardStatusBar.setVisibility(4);
            this.mKeyguardBottomArea.setDozing(this.mDozing, z);
            return;
        }
        this.mKeyguardStatusBar.setVisibility(0);
        this.mKeyguardBottomArea.setDozing(this.mDozing, z);
        if (z) {
            animateKeyguardStatusBarIn(700L);
        }
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public void showEmptyShadeView(boolean z) {
        this.mShowEmptyShadeView = z;
        updateEmptyShadeView();
    }

    private void updateEmptyShadeView() {
        this.mNotificationStackScroller.updateEmptyShadeView(this.mShowEmptyShadeView && !this.mQsExpanded);
    }

    public void setQsScrimEnabled(boolean z) {
        boolean z2 = this.mQsScrimEnabled != z;
        this.mQsScrimEnabled = z;
        if (z2) {
            updateQsState();
        }
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void onScreenTurningOn() {
        this.mKeyguardStatusView.dozeTimeTick();
    }

    @Override // com.android.systemui.statusbar.stack.NotificationStackScrollLayout.OnEmptySpaceClickListener
    public void onEmptySpaceClicked(float f, float f2) {
        onEmptySpaceClick(f);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean onMiddleClicked() {
        switch (this.mStatusBar.getBarState()) {
            case 0:
                post(this.mPostCollapseRunnable);
                break;
            case 1:
                if (!this.mDozingOnDown) {
                    this.mLockscreenGestureLogger.write(188, 0, 0);
                    startUnlockHintAnimation();
                    break;
                }
                break;
            case 2:
                if (!this.mQsExpanded) {
                    this.mStatusBar.goToKeyguard();
                    break;
                }
                break;
        }
        return true;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mCurrentPanelAlpha != 255) {
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), this.mAlphaPaint);
        }
    }

    public float getCurrentPanelAlpha() {
        return this.mCurrentPanelAlpha;
    }

    public boolean setPanelAlpha(int i, boolean z) {
        if (this.mPanelAlpha != i) {
            this.mPanelAlpha = i;
            PropertyAnimator.setProperty(this, this.PANEL_ALPHA, i, i == 255 ? this.PANEL_ALPHA_IN_PROPERTIES : this.PANEL_ALPHA_OUT_PROPERTIES, z);
            return true;
        }
        return false;
    }

    public void setPanelAlphaInternal(float f) {
        this.mCurrentPanelAlpha = (int) f;
        this.mAlphaPaint.setARGB(this.mCurrentPanelAlpha, 255, 255, 255);
        invalidate();
    }

    public void setPanelAlphaEndAction(Runnable runnable) {
        this.mPanelAlphaEndAction = runnable;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinnedModeChanged(boolean z) {
        this.mNotificationStackScroller.setInHeadsUpPinnedMode(z);
        if (z) {
            this.mHeadsUpExistenceChangedRunnable.run();
            updateNotificationTranslucency();
        } else {
            setHeadsUpAnimatingAway(true);
            this.mNotificationStackScroller.runAfterAnimationFinished(this.mHeadsUpExistenceChangedRunnable);
        }
    }

    public void setHeadsUpAnimatingAway(boolean z) {
        this.mHeadsUpAnimatingAway = z;
        this.mNotificationStackScroller.setHeadsUpAnimatingAway(z);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinned(ExpandableNotificationRow expandableNotificationRow) {
        this.mNotificationStackScroller.generateHeadsUpAnimation(expandableNotificationRow, true);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpUnPinned(ExpandableNotificationRow expandableNotificationRow) {
        if (isFullyCollapsed() && expandableNotificationRow.isHeadsUp()) {
            this.mNotificationStackScroller.generateHeadsUpAnimation(expandableNotificationRow, false);
            expandableNotificationRow.setHeadsUpIsVisible();
        }
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean z) {
        this.mNotificationStackScroller.generateHeadsUpAnimation(entry.row, z);
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    public void setHeadsUpManager(HeadsUpManagerPhone headsUpManagerPhone) {
        super.setHeadsUpManager(headsUpManagerPhone);
        this.mHeadsUpTouchHelper = new HeadsUpTouchHelper(headsUpManagerPhone, this.mNotificationStackScroller, this);
    }

    public void setTrackedHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        if (expandableNotificationRow != null) {
            notifyListenersTrackingHeadsUp(expandableNotificationRow);
            this.mExpandingFromHeadsUp = true;
        }
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected void onClosingFinished() {
        super.onClosingFinished();
        resetVerticalPanelPosition();
        setClosingWithAlphaFadeout(false);
    }

    private void setClosingWithAlphaFadeout(boolean z) {
        this.mClosingWithAlphaFadeOut = z;
        this.mNotificationStackScroller.forceNoOverlappingRendering(z);
    }

    protected void updateVerticalPanelPosition(float f) {
        if (this.mNotificationStackScroller.getWidth() * 1.75f > getWidth()) {
            resetVerticalPanelPosition();
            return;
        }
        float width = this.mPositionMinSideMargin + (this.mNotificationStackScroller.getWidth() / 2);
        float width2 = (getWidth() - this.mPositionMinSideMargin) - (this.mNotificationStackScroller.getWidth() / 2);
        if (Math.abs(f - (getWidth() / 2)) < this.mNotificationStackScroller.getWidth() / 4) {
            f = getWidth() / 2;
        }
        setVerticalPanelTranslation(Math.min(width2, Math.max(width, f)) - (this.mNotificationStackScroller.getLeft() + (this.mNotificationStackScroller.getWidth() / 2)));
    }

    private void resetVerticalPanelPosition() {
        setVerticalPanelTranslation(0.0f);
    }

    protected void setVerticalPanelTranslation(float f) {
        this.mNotificationStackScroller.setTranslationX(f);
        this.mQsFrame.setTranslationX(f);
        int size = this.mVerticalTranslationListener.size();
        for (int i = 0; i < size; i++) {
            this.mVerticalTranslationListener.get(i).run();
        }
    }

    protected void updateExpandedHeight(float f) {
        if (this.mTracking) {
            this.mNotificationStackScroller.setExpandingVelocity(getCurrentExpandVelocity());
        }
        this.mNotificationStackScroller.setExpandedHeight(f);
        updateKeyguardBottomAreaAlpha();
        updateStatusBarIcons();
    }

    public boolean isFullWidth() {
        return this.mIsFullWidth;
    }

    private void updateStatusBarIcons() {
        boolean z = isFullWidth() && getExpandedHeight() < getOpeningHeight();
        if (z && this.mNoVisibleNotifications && isOnKeyguard()) {
            z = false;
        }
        if (z != this.mShowIconsWhenExpanded) {
            this.mShowIconsWhenExpanded = z;
            this.mStatusBar.recomputeDisableFlags(false);
        }
    }

    private boolean isOnKeyguard() {
        return this.mStatusBar.getBarState() == 1;
    }

    public void setPanelScrimMinFraction(float f) {
        this.mBar.panelScrimMinFractionChanged(f);
    }

    public void clearNotificationEffects() {
        this.mStatusBar.clearNotificationEffects();
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    protected boolean isPanelVisibleBecauseOfHeadsUp() {
        return this.mHeadsUpManager.hasPinnedHeadsUp() || this.mHeadsUpAnimatingAway;
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return !this.mDozing;
    }

    public void launchCamera(boolean z, int i) {
        if (i == 1) {
            this.mLastCameraLaunchSource = "power_double_tap";
        } else if (i == 0) {
            this.mLastCameraLaunchSource = "wiggle_gesture";
        } else if (i == 2) {
            this.mLastCameraLaunchSource = "lift_to_launch_ml";
        } else {
            this.mLastCameraLaunchSource = "lockscreen_affordance";
        }
        if (!isFullyCollapsed()) {
            this.mLaunchingAffordance = true;
            setLaunchingAffordance(true);
        } else {
            z = false;
        }
        this.mAffordanceHelper.launchAffordance(z, getLayoutDirection() == 1);
    }

    public void onAffordanceLaunchEnded() {
        this.mLaunchingAffordance = false;
        setLaunchingAffordance(false);
    }

    @Override // android.view.View
    public void setAlpha(float f) {
        super.setAlpha(f);
        updateFullyVisibleState(false);
    }

    public void notifyStartFading() {
        updateFullyVisibleState(true);
    }

    @Override // android.view.View
    public void setVisibility(int i) {
        super.setVisibility(i);
        updateFullyVisibleState(false);
    }

    private void updateFullyVisibleState(boolean z) {
        this.mNotificationStackScroller.setParentNotFullyVisible((!z && getAlpha() == 1.0f && getVisibility() == 0) ? false : true);
    }

    private void setLaunchingAffordance(boolean z) {
        getLeftIcon().setLaunchingAffordance(z);
        getRightIcon().setLaunchingAffordance(z);
        getCenterIcon().setLaunchingAffordance(z);
    }

    public boolean canCameraGestureBeLaunched(boolean z) {
        if (!this.mStatusBar.isCameraAllowedByAdmin()) {
            return false;
        }
        ResolveInfo resolveInfoResolveCameraIntent = this.mKeyguardBottomArea.resolveCameraIntent();
        String str = (resolveInfoResolveCameraIntent == null || resolveInfoResolveCameraIntent.activityInfo == null) ? null : resolveInfoResolveCameraIntent.activityInfo.packageName;
        if (str != null) {
            return (z || !isForegroundApp(str)) && !this.mAffordanceHelper.isSwipingInProgress();
        }
        return false;
    }

    private boolean isForegroundApp(String str) throws SecurityException {
        List<ActivityManager.RunningTaskInfo> runningTasks = ((ActivityManager) getContext().getSystemService(ActivityManager.class)).getRunningTasks(1);
        return !runningTasks.isEmpty() && str.equals(runningTasks.get(0).topActivity.getPackageName());
    }

    public void setGroupManager(NotificationGroupManager notificationGroupManager) {
        this.mGroupManager = notificationGroupManager;
    }

    public boolean hideStatusBarIconsWhenExpanded() {
        if (this.mLaunchingNotification) {
            return this.mHideIconsDuringNotificationLaunch;
        }
        if (this.mHeadsUpAppearanceController == null || !this.mHeadsUpAppearanceController.shouldBeVisible()) {
            return (isFullWidth() && this.mShowIconsWhenExpanded) ? false : true;
        }
        return false;
    }

    /* renamed from: com.android.systemui.statusbar.phone.NotificationPanelView$22 */
    class AnonymousClass22 implements FragmentHostManager.FragmentListener {
        AnonymousClass22() {
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: android.app.Fragment */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
        public void onFragmentViewCreated(String str, Fragment fragment) {
            NotificationPanelView.this.mQs = (QS) fragment;
            NotificationPanelView.this.mQs.setPanelView(NotificationPanelView.this);
            NotificationPanelView.this.mQs.setExpandClickListener(NotificationPanelView.this);
            NotificationPanelView.this.mQs.setHeaderClickable(NotificationPanelView.this.mQsExpansionEnabled);
            NotificationPanelView.this.mQs.setKeyguardShowing(NotificationPanelView.this.mKeyguardShowing);
            NotificationPanelView.this.mQs.setOverscrolling(NotificationPanelView.this.mStackScrollerOverscrolling);
            NotificationPanelView.this.mQs.getView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$NotificationPanelView$22$__XIt3ZYZTq_RPQPhfW5iii8b-o
                @Override // android.view.View.OnLayoutChangeListener
                public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    NotificationPanelView.AnonymousClass22.lambda$onFragmentViewCreated$0(this.f$0, view, i, i2, i3, i4, i5, i6, i7, i8);
                }
            });
            NotificationPanelView.this.mNotificationStackScroller.setQsContainer((ViewGroup) NotificationPanelView.this.mQs.getView());
            NotificationPanelView.this.updateQsExpansion();
        }

        public static /* synthetic */ void lambda$onFragmentViewCreated$0(AnonymousClass22 anonymousClass22, View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            if (i4 - i2 != i8 - i6) {
                NotificationPanelView.this.onQsHeightChanged();
            }
        }

        @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
        public void onFragmentViewDestroyed(String str, Fragment fragment) {
            if (fragment == NotificationPanelView.this.mQs) {
                NotificationPanelView.this.mQs = null;
            }
        }
    }

    @Override // com.android.systemui.statusbar.phone.PanelView
    public void setTouchDisabled(boolean z) {
        super.setTouchDisabled(z);
        if (z && this.mAffordanceHelper.isSwipingInProgress() && !this.mIsLaunchTransitionRunning) {
            this.mAffordanceHelper.reset(false);
        }
    }

    public void setDozing(boolean z, boolean z2) {
        if (z == this.mDozing) {
            return;
        }
        this.mDozing = z;
        if (this.mStatusBarState == 1 || this.mStatusBarState == 2) {
            updateDozingVisibilities(z2);
        }
        float f = z ? 1.0f : 0.0f;
        if (this.mDarkAnimator != null && this.mDarkAnimator.isRunning()) {
            if (z2 && this.mDarkAmountTarget == f) {
                return;
            } else {
                this.mDarkAnimator.cancel();
            }
        }
        this.mDarkAmountTarget = f;
        if (z2) {
            this.mDarkAnimator = ObjectAnimator.ofFloat(this, SET_DARK_AMOUNT_PROPERTY, f);
            this.mDarkAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            this.mDarkAnimator.setDuration(500L);
            this.mDarkAnimator.start();
            return;
        }
        setDarkAmount(f);
    }

    private void setDarkAmount(float f) {
        this.mDarkAmount = f;
        this.mKeyguardStatusView.setDarkAmount(this.mDarkAmount);
        this.mKeyguardBottomArea.setDarkAmount(this.mDarkAmount);
        positionClockAndNotifications();
    }

    public void setPulsing(boolean z) {
        this.mPulsing = z;
        boolean z2 = !DozeParameters.getInstance(this.mContext).getDisplayNeedsBlanking();
        if (z2) {
            this.mAnimateNextPositionUpdate = true;
        }
        this.mNotificationStackScroller.setPulsing(z, z2);
        this.mKeyguardStatusView.setPulsing(z, z2);
    }

    public void dozeTimeTick() {
        this.mKeyguardStatusView.dozeTimeTick();
        this.mKeyguardBottomArea.dozeTimeTick();
        if (this.mDarkAmount > 0.0f) {
            positionClockAndNotifications();
        }
    }

    public void setStatusAccessibilityImportance(int i) {
        this.mKeyguardStatusView.setImportantForAccessibility(i);
    }

    public void setUserSetupComplete(boolean z) {
        this.mUserSetupComplete = z;
        this.mKeyguardBottomArea.setUserSetupComplete(z);
    }

    public LockIcon getLockIcon() {
        return this.mKeyguardBottomArea.getLockIcon();
    }

    public void applyExpandAnimationParams(ActivityLaunchAnimator.ExpandAnimationParameters expandAnimationParameters) {
        boolean z;
        this.mExpandOffset = expandAnimationParameters != null ? expandAnimationParameters.getTopChange() : 0.0f;
        updateQsExpansion();
        if (expandAnimationParameters != null) {
            if (expandAnimationParameters.getProgress(14L, 100L) != 0.0f) {
                z = false;
            } else {
                z = true;
            }
            if (z != this.mHideIconsDuringNotificationLaunch) {
                this.mHideIconsDuringNotificationLaunch = z;
                if (!z) {
                    this.mStatusBar.recomputeDisableFlags(true);
                }
            }
        }
    }

    public void addTrackingHeadsUpListener(Consumer<ExpandableNotificationRow> consumer) {
        this.mTrackingHeadsUpListeners.add(consumer);
    }

    public void removeTrackingHeadsUpListener(Consumer<ExpandableNotificationRow> consumer) {
        this.mTrackingHeadsUpListeners.remove(consumer);
    }

    public void addVerticalTranslationListener(Runnable runnable) {
        this.mVerticalTranslationListener.add(runnable);
    }

    public void removeVerticalTranslationListener(Runnable runnable) {
        this.mVerticalTranslationListener.remove(runnable);
    }

    public void setHeadsUpAppearanceController(HeadsUpAppearanceController headsUpAppearanceController) {
        this.mHeadsUpAppearanceController = headsUpAppearanceController;
    }

    public void onBouncerPreHideAnimation() {
        setKeyguardStatusViewVisibility(this.mStatusBarState, true, false);
    }
}
