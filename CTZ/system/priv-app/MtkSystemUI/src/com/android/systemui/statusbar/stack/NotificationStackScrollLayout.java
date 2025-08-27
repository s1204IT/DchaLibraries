package com.android.systemui.statusbar.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Property;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.widget.OverScroller;
import android.widget.ScrollView;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.ExpandHelper;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FooterView;
import com.android.systemui.statusbar.NotificationBlockingHelperManager;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationGuts;
import com.android.systemui.statusbar.NotificationListContainer;
import com.android.systemui.statusbar.NotificationLogger;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.NotificationSnooze;
import com.android.systemui.statusbar.StackScrollerDecorView;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.VisibilityLocationProvider;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.HeadsUpAppearanceController;
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.HeadsUpUtil;
import com.android.systemui.statusbar.policy.ScrollAdapter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class NotificationStackScrollLayout extends ViewGroup implements ExpandHelper.Callback, SwipeHelper.Callback, NotificationMenuRowPlugin.OnMenuEventListener, ExpandableView.OnHeightChangedListener, NotificationListContainer, VisibilityLocationProvider, NotificationGroupManager.OnGroupChangeListener, ScrollAdapter {
    private static final Property<NotificationStackScrollLayout, Float> DARK_AMOUNT = new FloatProperty<NotificationStackScrollLayout>("darkAmount") { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.7
        AnonymousClass7(String str) {
            super(str);
        }

        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(NotificationStackScrollLayout notificationStackScrollLayout, float f) {
            notificationStackScrollLayout.setDarkAmount(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(NotificationStackScrollLayout notificationStackScrollLayout) {
            return Float.valueOf(notificationStackScrollLayout.getDarkAmount());
        }
    };
    private boolean mActivateNeedsAnimation;
    private int mActivePointerId;
    private ArrayList<View> mAddedHeadsUpChildren;
    private final AmbientState mAmbientState;
    private boolean mAnimateNextBackgroundBottom;
    private boolean mAnimateNextBackgroundTop;
    private Runnable mAnimateScroll;
    private ArrayList<AnimationEvent> mAnimationEvents;
    private HashSet<Runnable> mAnimationFinishedRunnables;
    private boolean mAnimationRunning;
    private boolean mAnimationsEnabled;
    private int mAntiBurnInOffsetX;
    private final Rect mBackgroundAnimationRect;
    private Rect mBackgroundBounds;
    private final Paint mBackgroundPaint;
    private final Path mBackgroundPath;
    private boolean mBackwardScrollable;
    private int mBgColor;
    private ObjectAnimator mBottomAnimator;
    private int mBottomInset;
    private int mBottomMargin;
    private int mCachedBackgroundColor;
    private boolean mChangePositionInProgress;
    boolean mCheckForLeavebehind;
    private boolean mChildTransferInProgress;
    private ArrayList<View> mChildrenChangingPositions;
    private HashSet<View> mChildrenToAddAnimated;
    private ArrayList<View> mChildrenToRemoveAnimated;
    private boolean mChildrenUpdateRequested;
    private ViewTreeObserver.OnPreDrawListener mChildrenUpdater;
    private HashSet<ExpandableView> mClearTransientViewsWhenFinished;
    private final Rect mClipRect;
    private int mCollapsedSize;
    private int mContentHeight;
    private boolean mContinuousShadowUpdate;
    private int mCornerRadius;
    private NotificationMenuRowPlugin mCurrMenuRow;
    private Rect mCurrentBounds;
    private int mCurrentStackHeight;
    private StackScrollState mCurrentStackScrollState;
    private float mDarkAmount;
    private ObjectAnimator mDarkAmountAnimator;
    private int mDarkAnimationOriginIndex;
    private boolean mDarkNeedsAnimation;
    private int mDarkSeparatorPadding;
    private int mDarkTopPadding;
    private float mDimAmount;
    private ValueAnimator mDimAnimator;
    private Animator.AnimatorListener mDimEndListener;
    private ValueAnimator.AnimatorUpdateListener mDimUpdateListener;
    private boolean mDimmedNeedsAnimation;
    private boolean mDisallowDismissInThisMotion;
    private boolean mDisallowScrollingInThisMotion;
    private boolean mDismissAllInProgress;
    private boolean mDontClampNextScroll;
    private boolean mDontReportNextOverScroll;
    private int mDownX;
    private ArrayList<View> mDragAnimPendingChildren;
    private boolean mDrawBackgroundAsSrc;
    protected EmptyShadeView mEmptyShadeView;
    private Rect mEndAnimationRect;
    private boolean mEverythingNeedsAnimation;
    private ExpandHelper mExpandHelper;
    private View mExpandedGroupView;
    private float mExpandedHeight;
    private ArrayList<BiConsumer<Float, Float>> mExpandedHeightListeners;
    private boolean mExpandedInThisMotion;
    private boolean mExpandingNotification;
    private boolean mFadeNotificationsOnDismiss;
    private boolean mFadingOut;
    private FalsingManager mFalsingManager;
    private Runnable mFinishScrollingCallback;
    private ActivatableNotificationView mFirstVisibleBackgroundChild;
    protected FooterView mFooterView;
    private boolean mForceNoOverlappingRendering;
    private View mForcedScroll;
    private boolean mForwardScrollable;
    private HashSet<View> mFromMoreCardAdditions;
    private boolean mGenerateChildOrderChangedEvent;
    private long mGoToFullShadeDelay;
    private boolean mGoToFullShadeNeedsAnimation;
    private boolean mGroupExpandedForMeasure;
    private NotificationGroupManager mGroupManager;
    private boolean mHeadsUpAnimatingAway;
    private HeadsUpAppearanceController mHeadsUpAppearanceController;
    private HashSet<Pair<ExpandableNotificationRow, Boolean>> mHeadsUpChangeAnimations;
    private boolean mHeadsUpGoingAwayAnimationsAllowed;
    private int mHeadsUpInset;
    private HeadsUpManagerPhone mHeadsUpManager;
    private boolean mHideSensitiveNeedsAnimation;
    private boolean mInHeadsUpPinnedMode;
    private int mIncreasedPaddingBetweenElements;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private int mIntrinsicContentHeight;
    private int mIntrinsicPadding;
    private boolean mIsBeingDragged;
    private boolean mIsClipped;
    private boolean mIsExpanded;
    private boolean mIsExpansionChanging;
    private int mLastMotionY;
    private ActivatableNotificationView mLastVisibleBackgroundChild;
    private NotificationLogger.OnChildLocationsChangedListener mListener;
    private ExpandableNotificationRow.LongPressListener mLongPressListener;
    private int mMaxDisplayedNotifications;
    private int mMaxLayoutHeight;
    private float mMaxOverScroll;
    private int mMaxScrollAfterExpand;
    private int mMaxTopPadding;
    private int mMaximumVelocity;
    private View mMenuExposedView;
    private int mMinInteractionHeight;
    private float mMinTopOverScrollToEscape;
    private int mMinimumVelocity;
    private boolean mNeedViewResizeAnimation;
    private View mNeedingPulseAnimation;
    private boolean mNeedsAnimation;
    private boolean mNoAmbient;
    private OnEmptySpaceClickListener mOnEmptySpaceClickListener;
    private ExpandableView.OnHeightChangedListener mOnHeightChangedListener;
    private boolean mOnlyScrollingInThisMotion;
    private float mOverScrolledBottomPixels;
    private float mOverScrolledTopPixels;
    private int mOverflingDistance;
    private OnOverscrollTopChangedListener mOverscrollTopChangedListener;
    private int mOwnScrollY;
    private int mPaddingBetweenElements;
    private boolean mPanelTracking;
    private boolean mParentNotFullyVisible;
    private boolean mPulsing;
    protected ViewGroup mQsContainer;
    private boolean mQsExpanded;
    private float mQsExpansionFraction;
    private Runnable mReclamp;
    private int mRegularTopPadding;
    private Rect mRequestedClipBounds;
    private NotificationRoundnessManager mRoundnessManager;
    private ViewTreeObserver.OnPreDrawListener mRunningAnimationUpdater;
    private ScrimController mScrimController;
    private boolean mScrollable;
    private boolean mScrolledToTopOnFirstDown;
    private OverScroller mScroller;
    protected boolean mScrollingEnabled;
    private final int mSeparatorThickness;
    private final int mSeparatorWidth;
    private ViewTreeObserver.OnPreDrawListener mShadowUpdater;
    private NotificationShelf mShelf;
    private final boolean mShouldDrawNotificationBackground;
    private boolean mShouldShowShelfOnly;
    private int mSidePaddings;
    private ArrayList<View> mSnappedBackChildren;
    private PorterDuffXfermode mSrcMode;
    protected final StackScrollAlgorithm mStackScrollAlgorithm;
    private float mStackTranslation;
    private Rect mStartAnimationRect;
    private final StackStateAnimator mStateAnimator;
    private StatusBar mStatusBar;
    private int mStatusBarHeight;
    private int mStatusBarState;
    private NotificationSwipeHelper mSwipeHelper;
    private ArrayList<View> mSwipedOutViews;
    private boolean mSwipingInProgress;
    private int[] mTempInt2;
    private final ArrayList<Pair<ExpandableNotificationRow, Boolean>> mTmpList;
    private ArrayList<ExpandableView> mTmpSortedChildren;
    private ObjectAnimator mTopAnimator;
    private int mTopPadding;
    private boolean mTopPaddingNeedsAnimation;
    private float mTopPaddingOverflow;
    private boolean mTouchIsClick;
    private int mTouchSlop;
    private boolean mTrackingHeadsUp;
    private View mTranslatingParentView;
    private boolean mUsingLightTheme;
    private VelocityTracker mVelocityTracker;
    private Comparator<ExpandableView> mViewPositionComparator;

    public interface OnEmptySpaceClickListener {
        void onEmptySpaceClicked(float f, float f2);
    }

    public interface OnOverscrollTopChangedListener {
        void flingTopOverscroll(float f, boolean z);

        void onOverscrollTopChanged(float f, boolean z);
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$1 */
    class AnonymousClass1 implements ViewTreeObserver.OnPreDrawListener {
        AnonymousClass1() {
        }

        @Override // android.view.ViewTreeObserver.OnPreDrawListener
        public boolean onPreDraw() {
            NotificationStackScrollLayout.this.updateForcedScroll();
            NotificationStackScrollLayout.this.updateChildren();
            NotificationStackScrollLayout.this.mChildrenUpdateRequested = false;
            NotificationStackScrollLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);
            return true;
        }
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$2 */
    class AnonymousClass2 implements ViewTreeObserver.OnPreDrawListener {
        AnonymousClass2() {
        }

        @Override // android.view.ViewTreeObserver.OnPreDrawListener
        public boolean onPreDraw() {
            NotificationStackScrollLayout.this.onPreDrawDuringAnimation();
            return true;
        }
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$3 */
    class AnonymousClass3 extends AnimatorListenerAdapter {
        AnonymousClass3() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            NotificationStackScrollLayout.this.mDimAnimator = null;
        }
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$4 */
    class AnonymousClass4 implements ValueAnimator.AnimatorUpdateListener {
        AnonymousClass4() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            NotificationStackScrollLayout.this.setDimAmount(((Float) valueAnimator.getAnimatedValue()).floatValue());
        }
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$5 */
    class AnonymousClass5 implements ViewTreeObserver.OnPreDrawListener {
        AnonymousClass5() {
        }

        @Override // android.view.ViewTreeObserver.OnPreDrawListener
        public boolean onPreDraw() {
            NotificationStackScrollLayout.this.updateViewShadows();
            return true;
        }
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$6 */
    class AnonymousClass6 implements Comparator<ExpandableView> {
        AnonymousClass6() {
        }

        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(ExpandableView expandableView, ExpandableView expandableView2) {
            float translationY = expandableView.getTranslationY() + expandableView.getActualHeight();
            float translationY2 = expandableView2.getTranslationY() + expandableView2.getActualHeight();
            if (translationY < translationY2) {
                return -1;
            }
            if (translationY > translationY2) {
                return 1;
            }
            return 0;
        }
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$7 */
    class AnonymousClass7 extends FloatProperty<NotificationStackScrollLayout> {
        AnonymousClass7(String str) {
            super(str);
        }

        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(NotificationStackScrollLayout notificationStackScrollLayout, float f) {
            notificationStackScrollLayout.setDarkAmount(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(NotificationStackScrollLayout notificationStackScrollLayout) {
            return Float.valueOf(notificationStackScrollLayout.getDarkAmount());
        }
    }

    public NotificationStackScrollLayout(Context context) {
        this(context, null);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attributeSet, int i, int i2) throws Resources.NotFoundException {
        super(context, attributeSet, i, i2);
        this.mCurrentStackHeight = Integer.MAX_VALUE;
        this.mBackgroundPaint = new Paint();
        this.mBackgroundPath = new Path();
        this.mActivePointerId = -1;
        this.mBottomInset = 0;
        this.mCurrentStackScrollState = new StackScrollState(this);
        this.mChildrenToAddAnimated = new HashSet<>();
        this.mAddedHeadsUpChildren = new ArrayList<>();
        this.mChildrenToRemoveAnimated = new ArrayList<>();
        this.mSnappedBackChildren = new ArrayList<>();
        this.mDragAnimPendingChildren = new ArrayList<>();
        this.mChildrenChangingPositions = new ArrayList<>();
        this.mFromMoreCardAdditions = new HashSet<>();
        this.mAnimationEvents = new ArrayList<>();
        this.mSwipedOutViews = new ArrayList<>();
        this.mStateAnimator = new StackStateAnimator(this);
        this.mIsExpanded = true;
        this.mChildrenUpdater = new ViewTreeObserver.OnPreDrawListener() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.1
            AnonymousClass1() {
            }

            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public boolean onPreDraw() {
                NotificationStackScrollLayout.this.updateForcedScroll();
                NotificationStackScrollLayout.this.updateChildren();
                NotificationStackScrollLayout.this.mChildrenUpdateRequested = false;
                NotificationStackScrollLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        };
        this.mTempInt2 = new int[2];
        this.mAnimationFinishedRunnables = new HashSet<>();
        this.mClearTransientViewsWhenFinished = new HashSet<>();
        this.mHeadsUpChangeAnimations = new HashSet<>();
        this.mRoundnessManager = new NotificationRoundnessManager();
        this.mTmpList = new ArrayList<>();
        this.mRunningAnimationUpdater = new ViewTreeObserver.OnPreDrawListener() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.2
            AnonymousClass2() {
            }

            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public boolean onPreDraw() {
                NotificationStackScrollLayout.this.onPreDrawDuringAnimation();
                return true;
            }
        };
        this.mBackgroundBounds = new Rect();
        this.mStartAnimationRect = new Rect();
        this.mEndAnimationRect = new Rect();
        this.mCurrentBounds = new Rect(-1, -1, -1, -1);
        this.mBottomAnimator = null;
        this.mTopAnimator = null;
        this.mFirstVisibleBackgroundChild = null;
        this.mLastVisibleBackgroundChild = null;
        this.mTmpSortedChildren = new ArrayList<>();
        this.mDimEndListener = new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.3
            AnonymousClass3() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                NotificationStackScrollLayout.this.mDimAnimator = null;
            }
        };
        this.mDimUpdateListener = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.4
            AnonymousClass4() {
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                NotificationStackScrollLayout.this.setDimAmount(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        };
        this.mShadowUpdater = new ViewTreeObserver.OnPreDrawListener() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.5
            AnonymousClass5() {
            }

            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public boolean onPreDraw() {
                NotificationStackScrollLayout.this.updateViewShadows();
                return true;
            }
        };
        this.mViewPositionComparator = new Comparator<ExpandableView>() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.6
            AnonymousClass6() {
            }

            /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
            @Override // java.util.Comparator
            public int compare(ExpandableView expandableView, ExpandableView expandableView2) {
                float translationY = expandableView.getTranslationY() + expandableView.getActualHeight();
                float translationY2 = expandableView2.getTranslationY() + expandableView2.getActualHeight();
                if (translationY < translationY2) {
                    return -1;
                }
                if (translationY > translationY2) {
                    return 1;
                }
                return 0;
            }
        };
        this.mSrcMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
        this.mDarkAmount = 0.0f;
        this.mMaxDisplayedNotifications = -1;
        this.mClipRect = new Rect();
        this.mHeadsUpGoingAwayAnimationsAllowed = true;
        this.mAnimateScroll = new Runnable() { // from class: com.android.systemui.statusbar.stack.-$$Lambda$NotificationStackScrollLayout$jMWJf69jcb2Er7Sg4rletxrm5pw
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.animateScroll();
            }
        };
        this.mBackgroundAnimationRect = new Rect();
        this.mExpandedHeightListeners = new ArrayList<>();
        this.mReclamp = new Runnable() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.8
            AnonymousClass8() {
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationStackScrollLayout.this.mScroller.startScroll(NotificationStackScrollLayout.this.mScrollX, NotificationStackScrollLayout.this.mOwnScrollY, 0, NotificationStackScrollLayout.this.getScrollRange() - NotificationStackScrollLayout.this.mOwnScrollY);
                NotificationStackScrollLayout.this.mDontReportNextOverScroll = true;
                NotificationStackScrollLayout.this.mDontClampNextScroll = true;
                NotificationStackScrollLayout.this.animateScroll();
            }
        };
        Resources resources = getResources();
        this.mAmbientState = new AmbientState(context);
        this.mBgColor = context.getColor(R.color.notification_shade_background_color);
        this.mExpandHelper = new ExpandHelper(getContext(), this, resources.getDimensionPixelSize(R.dimen.notification_min_height), resources.getDimensionPixelSize(R.dimen.notification_max_height));
        this.mExpandHelper.setEventSource(this);
        this.mExpandHelper.setScrollAdapter(this);
        this.mSwipeHelper = new NotificationSwipeHelper(0, this, getContext());
        this.mStackScrollAlgorithm = createStackScrollAlgorithm(context);
        initView(context);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mShouldDrawNotificationBackground = resources.getBoolean(R.bool.config_drawNotificationBackground);
        this.mFadeNotificationsOnDismiss = resources.getBoolean(R.bool.config_fadeNotificationsOnDismiss);
        this.mSeparatorWidth = resources.getDimensionPixelSize(R.dimen.widget_separator_width);
        this.mSeparatorThickness = resources.getDimensionPixelSize(R.dimen.widget_separator_thickness);
        this.mDarkSeparatorPadding = resources.getDimensionPixelSize(R.dimen.widget_bottom_separator_padding);
        this.mRoundnessManager.setAnimatedChildren(this.mChildrenToAddAnimated);
        this.mRoundnessManager.setOnRoundingChangedCallback(new Runnable() { // from class: com.android.systemui.statusbar.stack.-$$Lambda$NotificationStackScrollLayout$5BC2kaMAloRxl8auhuScFksFNjA
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.invalidate();
            }
        });
        final NotificationRoundnessManager notificationRoundnessManager = this.mRoundnessManager;
        Objects.requireNonNull(notificationRoundnessManager);
        addOnExpandedHeightListener(new BiConsumer() { // from class: com.android.systemui.statusbar.stack.-$$Lambda$bam6n-VlBWgcqhwrPMDo0VFka_4
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                notificationRoundnessManager.setExpanded(((Float) obj).floatValue(), ((Float) obj2).floatValue());
            }
        });
        final NotificationBlockingHelperManager notificationBlockingHelperManager = (NotificationBlockingHelperManager) Dependency.get(NotificationBlockingHelperManager.class);
        addOnExpandedHeightListener(new BiConsumer() { // from class: com.android.systemui.statusbar.stack.-$$Lambda$NotificationStackScrollLayout$ORqWctSAOFbrSZfQ2fKGWKeQ46w
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                notificationBlockingHelperManager.setNotificationShadeExpanded(((Float) obj).floatValue());
            }
        });
        updateWillNotDraw();
        this.mBackgroundPaint.setAntiAlias(true);
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public NotificationSwipeActionHelper getSwipeActionHelper() {
        return this.mSwipeHelper;
    }

    @Override // com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin.OnMenuEventListener
    public void onMenuClicked(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem) {
        if (this.mLongPressListener == null) {
            return;
        }
        if (view instanceof ExpandableNotificationRow) {
            MetricsLogger.action(this.mContext, 333, ((ExpandableNotificationRow) view).getStatusBarNotification().getPackageName());
        }
        this.mLongPressListener.onLongPress(view, i, i2, menuItem);
    }

    @Override // com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin.OnMenuEventListener
    public void onMenuReset(View view) {
        if (this.mTranslatingParentView != null && view == this.mTranslatingParentView) {
            this.mMenuExposedView = null;
            this.mTranslatingParentView = null;
        }
    }

    @Override // com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin.OnMenuEventListener
    public void onMenuShown(View view) {
        this.mMenuExposedView = this.mTranslatingParentView;
        if (view instanceof ExpandableNotificationRow) {
            MetricsLogger.action(this.mContext, 332, ((ExpandableNotificationRow) view).getStatusBarNotification().getPackageName());
        }
        this.mSwipeHelper.onMenuShown(view);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.mShouldDrawNotificationBackground) {
            if (this.mCurrentBounds.top < this.mCurrentBounds.bottom || this.mAmbientState.isDark()) {
                drawBackground(canvas);
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        int i = this.mSidePaddings;
        int width = getWidth() - this.mSidePaddings;
        int i2 = this.mCurrentBounds.top;
        int i3 = this.mCurrentBounds.bottom;
        int width2 = (getWidth() / 2) - (this.mSeparatorWidth / 2);
        int i4 = this.mSeparatorWidth + width2;
        int i5 = (int) (this.mRegularTopPadding + (this.mSeparatorThickness / 2.0f));
        int i6 = this.mSeparatorThickness + i5;
        if (!this.mAmbientState.hasPulsingNotifications()) {
            if (this.mAmbientState.isFullyDark()) {
                if (this.mFirstVisibleBackgroundChild != null) {
                    canvas.drawRect(width2, i5, i4, i6, this.mBackgroundPaint);
                }
            } else {
                float f = 1.0f - this.mDarkAmount;
                float interpolation = Interpolators.FAST_OUT_SLOW_IN.getInterpolation(f);
                float interpolation2 = Interpolators.FAST_OUT_SLOW_IN.getInterpolation(f * 2.0f);
                this.mBackgroundAnimationRect.set((int) MathUtils.lerp(width2, i, interpolation2), (int) MathUtils.lerp(i5, i2, interpolation), (int) MathUtils.lerp(i4, width, interpolation2), (int) MathUtils.lerp(i6, i3, interpolation));
                if (!this.mAmbientState.isDark() || this.mFirstVisibleBackgroundChild != null) {
                    canvas.drawRoundRect(this.mBackgroundAnimationRect.left, this.mBackgroundAnimationRect.top, this.mBackgroundAnimationRect.right, this.mBackgroundAnimationRect.bottom, this.mCornerRadius, this.mCornerRadius, this.mBackgroundPaint);
                }
            }
        }
        updateClipping();
    }

    private void updateBackgroundDimming() {
        if (!this.mShouldDrawNotificationBackground) {
            return;
        }
        int iBlendARGB = ColorUtils.blendARGB(ColorUtils.blendARGB(this.mScrimController.getBackgroundColor(), this.mBgColor, (0.7f + (0.3f * (1.0f - this.mDimAmount))) * (1.0f - this.mDarkAmount)), -1, Interpolators.DECELERATE_QUINT.getInterpolation(this.mDarkAmount));
        if (this.mCachedBackgroundColor != iBlendARGB) {
            this.mCachedBackgroundColor = iBlendARGB;
            this.mBackgroundPaint.setColor(iBlendARGB);
            invalidate();
        }
    }

    private void initView(Context context) {
        this.mScroller = new OverScroller(getContext());
        setDescendantFocusability(262144);
        setClipChildren(false);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mOverflingDistance = viewConfiguration.getScaledOverflingDistance();
        Resources resources = context.getResources();
        this.mCollapsedSize = resources.getDimensionPixelSize(R.dimen.notification_min_height);
        this.mStackScrollAlgorithm.initView(context);
        this.mAmbientState.reload(context);
        this.mPaddingBetweenElements = Math.max(1, resources.getDimensionPixelSize(R.dimen.notification_divider_height));
        this.mIncreasedPaddingBetweenElements = resources.getDimensionPixelSize(R.dimen.notification_divider_height_increased);
        this.mMinTopOverScrollToEscape = resources.getDimensionPixelSize(R.dimen.min_top_overscroll_to_qs);
        this.mStatusBarHeight = resources.getDimensionPixelSize(R.dimen.status_bar_height);
        this.mBottomMargin = resources.getDimensionPixelSize(R.dimen.notification_panel_margin_bottom);
        this.mSidePaddings = resources.getDimensionPixelSize(R.dimen.notification_side_paddings);
        this.mMinInteractionHeight = resources.getDimensionPixelSize(R.dimen.notification_min_interaction_height);
        this.mCornerRadius = resources.getDimensionPixelSize(Utils.getThemeAttr(this.mContext, android.R.attr.dialogCornerRadius));
        this.mHeadsUpInset = this.mStatusBarHeight + resources.getDimensionPixelSize(R.dimen.heads_up_status_bar_padding);
    }

    public void setDrawBackgroundAsSrc(boolean z) {
        this.mDrawBackgroundAsSrc = z;
        updateSrcDrawing();
    }

    private void updateSrcDrawing() {
        if (!this.mShouldDrawNotificationBackground) {
            return;
        }
        this.mBackgroundPaint.setXfermode((!this.mDrawBackgroundAsSrc || this.mFadingOut || this.mParentNotFullyVisible) ? null : this.mSrcMode);
        invalidate();
    }

    private void notifyHeightChangeListener(ExpandableView expandableView) {
        notifyHeightChangeListener(expandableView, false);
    }

    private void notifyHeightChangeListener(ExpandableView expandableView, boolean z) {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onHeightChanged(expandableView, z);
        }
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(i) - (this.mSidePaddings * 2), View.MeasureSpec.getMode(i));
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            measureChild(getChildAt(i3), iMakeMeasureSpec, i2);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        float width = getWidth() / 2.0f;
        for (int i5 = 0; i5 < getChildCount(); i5++) {
            float measuredWidth = r8.getMeasuredWidth() / 2.0f;
            getChildAt(i5).layout((int) (width - measuredWidth), 0, (int) (measuredWidth + width), r8.getMeasuredHeight());
        }
        setMaxLayoutHeight(getHeight());
        updateContentHeight();
        clampScrollPosition();
        requestChildrenUpdate();
        updateFirstAndLastBackgroundViews();
        updateAlgorithmLayoutMinHeight();
    }

    private void requestAnimationOnViewResize(ExpandableNotificationRow expandableNotificationRow) {
        if (this.mAnimationsEnabled) {
            if (this.mIsExpanded || (expandableNotificationRow != null && expandableNotificationRow.isPinned())) {
                this.mNeedViewResizeAnimation = true;
                this.mNeedsAnimation = true;
            }
        }
    }

    public void updateSpeedBumpIndex(int i, boolean z) {
        this.mAmbientState.setSpeedBumpIndex(i);
        this.mNoAmbient = z;
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void setChildLocationsChangedListener(NotificationLogger.OnChildLocationsChangedListener onChildLocationsChangedListener) {
        this.mListener = onChildLocationsChangedListener;
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer, com.android.systemui.statusbar.notification.VisibilityLocationProvider
    public boolean isInVisibleLocation(ExpandableNotificationRow expandableNotificationRow) {
        ExpandableViewState viewStateForView = this.mCurrentStackScrollState.getViewStateForView(expandableNotificationRow);
        if (viewStateForView == null || (viewStateForView.location & 5) == 0 || expandableNotificationRow.getVisibility() != 0) {
            return false;
        }
        return true;
    }

    private void setMaxLayoutHeight(int i) {
        this.mMaxLayoutHeight = i;
        this.mShelf.setMaxLayoutHeight(i);
        updateAlgorithmHeightAndPadding();
    }

    private void updateAlgorithmHeightAndPadding() {
        this.mTopPadding = (int) MathUtils.lerp(this.mRegularTopPadding, this.mDarkTopPadding, this.mDarkAmount);
        this.mAmbientState.setLayoutHeight(getLayoutHeight());
        updateAlgorithmLayoutMinHeight();
        this.mAmbientState.setTopPadding(this.mTopPadding);
    }

    private void updateAlgorithmLayoutMinHeight() {
        this.mAmbientState.setLayoutMinHeight((this.mQsExpanded || isHeadsUpTransition()) ? getLayoutMinHeight() : 0);
    }

    private void updateChildren() {
        float currVelocity;
        updateScrollStateForAddedChildren();
        AmbientState ambientState = this.mAmbientState;
        if (this.mScroller.isFinished()) {
            currVelocity = 0.0f;
        } else {
            currVelocity = this.mScroller.getCurrVelocity();
        }
        ambientState.setCurrentScrollVelocity(currVelocity);
        this.mAmbientState.setScrollY(this.mOwnScrollY);
        this.mStackScrollAlgorithm.getStackScrollState(this.mAmbientState, this.mCurrentStackScrollState);
        if (!isCurrentlyAnimating() && !this.mNeedsAnimation) {
            applyCurrentState();
        } else {
            startAnimationToState();
        }
    }

    private void onPreDrawDuringAnimation() {
        this.mShelf.updateAppearance();
        updateClippingToTopRoundedCorner();
        if (!this.mNeedsAnimation && !this.mChildrenUpdateRequested) {
            updateBackground();
        }
    }

    private void updateClippingToTopRoundedCorner() {
        Float fValueOf = Float.valueOf(this.mTopPadding + this.mStackTranslation + this.mAmbientState.getExpandAnimationTopChange());
        Float fValueOf2 = Float.valueOf(fValueOf.floatValue() + this.mCornerRadius);
        boolean z = true;
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                float translationY = expandableView.getTranslationY();
                float actualHeight = expandableView.getActualHeight() + translationY;
                expandableView.setDistanceToTopRoundness((!z || this.mOwnScrollY != 0) & (((fValueOf.floatValue() > translationY ? 1 : (fValueOf.floatValue() == translationY ? 0 : -1)) > 0 && (fValueOf.floatValue() > actualHeight ? 1 : (fValueOf.floatValue() == actualHeight ? 0 : -1)) < 0) || ((fValueOf2.floatValue() > translationY ? 1 : (fValueOf2.floatValue() == translationY ? 0 : -1)) >= 0 && (fValueOf2.floatValue() > actualHeight ? 1 : (fValueOf2.floatValue() == actualHeight ? 0 : -1)) <= 0)) ? Math.max(translationY - fValueOf.floatValue(), 0.0f) : -1.0f);
                z = false;
            }
        }
    }

    private void updateScrollStateForAddedChildren() {
        int i;
        if (this.mChildrenToAddAnimated.isEmpty()) {
            return;
        }
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i2);
            if (this.mChildrenToAddAnimated.contains(expandableView)) {
                int positionInLinearLayout = getPositionInLinearLayout(expandableView);
                float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
                if (increasedPaddingAmount == 1.0f) {
                    i = this.mIncreasedPaddingBetweenElements;
                } else if (increasedPaddingAmount != -1.0f) {
                    i = this.mPaddingBetweenElements;
                } else {
                    i = 0;
                }
                int intrinsicHeight = getIntrinsicHeight(expandableView) + i;
                if (positionInLinearLayout < this.mOwnScrollY) {
                    setOwnScrollY(this.mOwnScrollY + intrinsicHeight);
                }
            }
        }
        clampScrollPosition();
    }

    private void updateForcedScroll() {
        if (this.mForcedScroll != null && (!this.mForcedScroll.hasFocus() || !this.mForcedScroll.isAttachedToWindow())) {
            this.mForcedScroll = null;
        }
        if (this.mForcedScroll != null) {
            ExpandableView expandableView = (ExpandableView) this.mForcedScroll;
            int positionInLinearLayout = getPositionInLinearLayout(expandableView);
            int iTargetScrollForView = targetScrollForView(expandableView, positionInLinearLayout);
            int intrinsicHeight = positionInLinearLayout + expandableView.getIntrinsicHeight();
            int iMax = Math.max(0, Math.min(iTargetScrollForView, getScrollRange()));
            if (this.mOwnScrollY < iMax || intrinsicHeight < this.mOwnScrollY) {
                setOwnScrollY(iMax);
            }
        }
    }

    private void requestChildrenUpdate() {
        if (!this.mChildrenUpdateRequested) {
            getViewTreeObserver().addOnPreDrawListener(this.mChildrenUpdater);
            this.mChildrenUpdateRequested = true;
            invalidate();
        }
    }

    private boolean isCurrentlyAnimating() {
        return this.mStateAnimator.isRunning();
    }

    private void clampScrollPosition() {
        int scrollRange = getScrollRange();
        if (scrollRange < this.mOwnScrollY) {
            setOwnScrollY(scrollRange);
        }
    }

    public int getTopPadding() {
        return this.mTopPadding;
    }

    private void setTopPadding(int i, boolean z) {
        if (this.mRegularTopPadding != i) {
            this.mRegularTopPadding = i;
            this.mDarkTopPadding = i + this.mDarkSeparatorPadding;
            this.mAmbientState.setDarkTopPadding(this.mDarkTopPadding);
            updateAlgorithmHeightAndPadding();
            updateContentHeight();
            if (z && this.mAnimationsEnabled && this.mIsExpanded) {
                this.mTopPaddingNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
            notifyHeightChangeListener(null, z);
        }
    }

    public void setExpandedHeight(float f) {
        float expandTranslationStart;
        int pinnedHeadsUpHeight;
        this.mExpandedHeight = f;
        float fLerp = 0.0f;
        setIsExpanded(f > 0.0f);
        float minExpansionHeight = getMinExpansionHeight();
        if (f < minExpansionHeight) {
            this.mClipRect.left = 0;
            this.mClipRect.right = getWidth();
            this.mClipRect.top = 0;
            this.mClipRect.bottom = (int) f;
            setRequestedClipBounds(this.mClipRect);
            f = minExpansionHeight;
        } else {
            setRequestedClipBounds(null);
        }
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        float appearFraction = 1.0f;
        boolean z = f < appearEndPosition;
        this.mAmbientState.setAppearing(z);
        if (!z) {
            if (this.mShouldShowShelfOnly) {
                pinnedHeadsUpHeight = this.mTopPadding + this.mShelf.getIntrinsicHeight();
            } else if (this.mQsExpanded) {
                int i = (this.mContentHeight - this.mTopPadding) + this.mIntrinsicPadding;
                int intrinsicHeight = this.mMaxTopPadding + this.mShelf.getIntrinsicHeight();
                if (i > intrinsicHeight) {
                    pinnedHeadsUpHeight = (int) NotificationUtils.interpolate(i, intrinsicHeight, this.mQsExpansionFraction);
                } else {
                    pinnedHeadsUpHeight = intrinsicHeight;
                }
            } else {
                pinnedHeadsUpHeight = (int) f;
            }
        } else {
            appearFraction = getAppearFraction(f);
            if (appearFraction >= 0.0f) {
                expandTranslationStart = NotificationUtils.interpolate(getExpandTranslationStart(), 0.0f, appearFraction);
            } else {
                expandTranslationStart = (f - appearStartPosition) + getExpandTranslationStart();
            }
            if (isHeadsUpTransition()) {
                pinnedHeadsUpHeight = this.mFirstVisibleBackgroundChild.getPinnedHeadsUpHeight();
                fLerp = MathUtils.lerp(this.mHeadsUpInset - this.mTopPadding, 0.0f, appearFraction);
            } else {
                pinnedHeadsUpHeight = (int) (f - expandTranslationStart);
                fLerp = expandTranslationStart;
            }
        }
        if (pinnedHeadsUpHeight != this.mCurrentStackHeight) {
            this.mCurrentStackHeight = pinnedHeadsUpHeight;
            updateAlgorithmHeightAndPadding();
            requestChildrenUpdate();
        }
        setStackTranslation(fLerp);
        for (int i2 = 0; i2 < this.mExpandedHeightListeners.size(); i2++) {
            this.mExpandedHeightListeners.get(i2).accept(Float.valueOf(this.mExpandedHeight), Float.valueOf(appearFraction));
        }
    }

    private void setRequestedClipBounds(Rect rect) {
        this.mRequestedClipBounds = rect;
        updateClipping();
    }

    public int getIntrinsicContentHeight() {
        return this.mIntrinsicContentHeight;
    }

    public void updateClipping() {
        boolean z = false;
        boolean z2 = this.mDarkAmount > 0.0f && this.mDarkAmount < 1.0f;
        if (this.mRequestedClipBounds != null && !this.mInHeadsUpPinnedMode && !this.mHeadsUpAnimatingAway) {
            z = true;
        }
        if (this.mIsClipped != z) {
            this.mIsClipped = z;
            updateFadingState();
        }
        if (z2) {
            setClipBounds(this.mBackgroundAnimationRect);
        } else if (z) {
            setClipBounds(this.mRequestedClipBounds);
        } else {
            setClipBounds(null);
        }
    }

    private float getExpandTranslationStart() {
        return -this.mTopPadding;
    }

    private float getAppearStartPosition() {
        if (isHeadsUpTransition()) {
            return this.mHeadsUpInset + this.mFirstVisibleBackgroundChild.getPinnedHeadsUpHeight();
        }
        return getMinExpansionHeight();
    }

    private int getTopHeadsUpPinnedHeight() {
        ExpandableNotificationRow groupSummary;
        NotificationData.Entry topEntry = this.mHeadsUpManager.getTopEntry();
        if (topEntry == null) {
            return 0;
        }
        ExpandableNotificationRow expandableNotificationRow = topEntry.row;
        if (expandableNotificationRow.isChildInGroup() && (groupSummary = this.mGroupManager.getGroupSummary(expandableNotificationRow.getStatusBarNotification())) != null) {
            expandableNotificationRow = groupSummary;
        }
        return expandableNotificationRow.getPinnedHeadsUpHeight();
    }

    private float getAppearEndPosition() {
        int height;
        int notGoneChildCount = getNotGoneChildCount();
        if (this.mEmptyShadeView.getVisibility() == 8 && notGoneChildCount != 0) {
            if (isHeadsUpTransition() || (this.mHeadsUpManager.hasPinnedHeadsUp() && !this.mAmbientState.isDark())) {
                height = getTopHeadsUpPinnedHeight();
            } else {
                height = 0;
                if (notGoneChildCount >= 1 && this.mShelf.getVisibility() != 8) {
                    height = 0 + this.mShelf.getIntrinsicHeight();
                }
            }
        } else {
            height = this.mEmptyShadeView.getHeight();
        }
        return height + (onKeyguard() ? this.mTopPadding : this.mIntrinsicPadding);
    }

    private boolean isHeadsUpTransition() {
        return this.mTrackingHeadsUp && this.mFirstVisibleBackgroundChild != null && this.mAmbientState.isAboveShelf(this.mFirstVisibleBackgroundChild);
    }

    public float getAppearFraction(float f) {
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        return (f - appearStartPosition) / (appearEndPosition - appearStartPosition);
    }

    public float getStackTranslation() {
        return this.mStackTranslation;
    }

    private void setStackTranslation(float f) {
        if (f != this.mStackTranslation) {
            this.mStackTranslation = f;
            this.mAmbientState.setStackTranslation(f);
            requestChildrenUpdate();
        }
    }

    private int getLayoutHeight() {
        return Math.min(this.mMaxLayoutHeight, this.mCurrentStackHeight);
    }

    public int getFirstItemMinHeight() {
        ExpandableView firstChildNotGone = getFirstChildNotGone();
        return firstChildNotGone != null ? firstChildNotGone.getMinHeight() : this.mCollapsedSize;
    }

    public void setLongPressListener(ExpandableNotificationRow.LongPressListener longPressListener) {
        this.mLongPressListener = longPressListener;
    }

    public void setQsContainer(ViewGroup viewGroup) {
        this.mQsContainer = viewGroup;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public void onChildDismissed(View view) {
        ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
        if (!expandableNotificationRow.isDismissed()) {
            handleChildViewDismissed(view);
        }
        ViewGroup transientContainer = expandableNotificationRow.getTransientContainer();
        if (transientContainer != null) {
            transientContainer.removeTransientView(view);
        }
    }

    private void handleChildViewDismissed(View view) {
        if (this.mDismissAllInProgress) {
            return;
        }
        boolean zPerformDismissWithBlockingHelper = false;
        setSwipingInProgress(false);
        if (this.mDragAnimPendingChildren.contains(view)) {
            this.mDragAnimPendingChildren.remove(view);
        }
        this.mAmbientState.onDragFinished(view);
        updateContinuousShadowDrawing();
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
            if (expandableNotificationRow.isHeadsUp()) {
                this.mHeadsUpManager.addSwipedOutNotification(expandableNotificationRow.getStatusBarNotification().getKey());
            }
            zPerformDismissWithBlockingHelper = expandableNotificationRow.performDismissWithBlockingHelper(false);
        }
        if (!zPerformDismissWithBlockingHelper) {
            this.mSwipedOutViews.add(view);
        }
        this.mFalsingManager.onNotificationDismissed();
        if (this.mFalsingManager.shouldEnforceBouncer()) {
            this.mStatusBar.executeRunnableDismissingKeyguard(null, null, false, true, false);
        }
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public void onChildSnappedBack(View view, float f) {
        this.mAmbientState.onDragFinished(view);
        updateContinuousShadowDrawing();
        if (!this.mDragAnimPendingChildren.contains(view)) {
            if (this.mAnimationsEnabled) {
                this.mSnappedBackChildren.add(view);
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
        } else {
            this.mDragAnimPendingChildren.remove(view);
        }
        if (this.mCurrMenuRow != null && f == 0.0f) {
            this.mCurrMenuRow.resetMenu();
            this.mCurrMenuRow = null;
        }
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public boolean updateSwipeProgress(View view, boolean z, float f) {
        return !this.mFadeNotificationsOnDismiss;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public void onBeginDrag(View view) {
        this.mFalsingManager.onNotificatonStartDismissing();
        setSwipingInProgress(true);
        this.mAmbientState.onBeginDrag(view);
        updateContinuousShadowDrawing();
        if (this.mAnimationsEnabled && (this.mIsExpanded || !isPinnedHeadsUp(view))) {
            this.mDragAnimPendingChildren.add(view);
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public static boolean isPinnedHeadsUp(View view) {
        if (!(view instanceof ExpandableNotificationRow)) {
            return false;
        }
        ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
        return expandableNotificationRow.isHeadsUp() && expandableNotificationRow.isPinned();
    }

    private boolean isHeadsUp(View view) {
        if (view instanceof ExpandableNotificationRow) {
            return ((ExpandableNotificationRow) view).isHeadsUp();
        }
        return false;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public void onDragCancelled(View view) {
        this.mFalsingManager.onNotificatonStopDismissing();
        setSwipingInProgress(false);
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public float getFalsingThresholdFactor() {
        return this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public View getChildAtPosition(MotionEvent motionEvent) {
        ExpandableNotificationRow notificationParent;
        ExpandableView childAtPosition = getChildAtPosition(motionEvent.getX(), motionEvent.getY());
        return ((childAtPosition instanceof ExpandableNotificationRow) && (notificationParent = ((ExpandableNotificationRow) childAtPosition).getNotificationParent()) != null && notificationParent.areChildrenExpanded()) ? (notificationParent.areGutsExposed() || this.mMenuExposedView == notificationParent || (notificationParent.getNotificationChildren().size() == 1 && notificationParent.isClearable())) ? notificationParent : childAtPosition : childAtPosition;
    }

    public ExpandableView getClosestChildAtRawPosition(float f, float f2) {
        getLocationOnScreen(this.mTempInt2);
        float f3 = f2 - this.mTempInt2[1];
        int childCount = getChildCount();
        ExpandableView expandableView = null;
        float f4 = Float.MAX_VALUE;
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView2 = (ExpandableView) getChildAt(i);
            if (expandableView2.getVisibility() != 8 && !(expandableView2 instanceof StackScrollerDecorView)) {
                float translationY = expandableView2.getTranslationY();
                float fMin = Math.min(Math.abs((expandableView2.getClipTopAmount() + translationY) - f3), Math.abs(((translationY + expandableView2.getActualHeight()) - expandableView2.getClipBottomAmount()) - f3));
                if (fMin < f4) {
                    expandableView = expandableView2;
                    f4 = fMin;
                }
            }
        }
        return expandableView;
    }

    @Override // com.android.systemui.ExpandHelper.Callback
    public ExpandableView getChildAtRawPosition(float f, float f2) {
        getLocationOnScreen(this.mTempInt2);
        return getChildAtPosition(f - this.mTempInt2[0], f2 - this.mTempInt2[1]);
    }

    @Override // com.android.systemui.ExpandHelper.Callback
    public ExpandableView getChildAtPosition(float f, float f2) {
        return getChildAtPosition(f, f2, true);
    }

    private ExpandableView getChildAtPosition(float f, float f2, boolean z) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() == 0 && !(expandableView instanceof StackScrollerDecorView)) {
                float translationY = expandableView.getTranslationY();
                float clipTopAmount = expandableView.getClipTopAmount() + translationY;
                float actualHeight = (expandableView.getActualHeight() + translationY) - expandableView.getClipBottomAmount();
                int width = getWidth();
                if ((actualHeight - clipTopAmount >= this.mMinInteractionHeight || !z) && f2 >= clipTopAmount && f2 <= actualHeight && f >= 0 && f <= width) {
                    if (expandableView instanceof ExpandableNotificationRow) {
                        ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                        if (this.mIsExpanded || !expandableNotificationRow.isHeadsUp() || !expandableNotificationRow.isPinned() || this.mHeadsUpManager.getTopEntry().row == expandableNotificationRow || this.mGroupManager.getGroupSummary(this.mHeadsUpManager.getTopEntry().row.getStatusBarNotification()) == expandableNotificationRow) {
                            return expandableNotificationRow.getViewAtPosition(f2 - translationY);
                        }
                    } else {
                        return expandableView;
                    }
                }
            }
        }
        return null;
    }

    @Override // com.android.systemui.ExpandHelper.Callback
    public boolean canChildBeExpanded(View view) {
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
            if (expandableNotificationRow.isExpandable() && !expandableNotificationRow.areGutsExposed() && (this.mIsExpanded || !expandableNotificationRow.isPinned())) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.systemui.ExpandHelper.Callback
    public void setUserExpandedChild(View view, boolean z) {
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
            if (z && onKeyguard()) {
                expandableNotificationRow.setUserLocked(false);
                updateContentHeight();
                notifyHeightChangeListener(expandableNotificationRow);
            } else {
                expandableNotificationRow.setUserExpanded(z, true);
                expandableNotificationRow.onExpandedByGesture(z);
            }
        }
    }

    @Override // com.android.systemui.ExpandHelper.Callback
    public void setExpansionCancelled(View view) {
        if (view instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) view).setGroupExpansionChanging(false);
        }
    }

    @Override // com.android.systemui.ExpandHelper.Callback
    public void setUserLockedChild(View view, boolean z) {
        if (view instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) view).setUserLocked(z);
        }
        cancelLongPress();
        requestDisallowInterceptTouchEvent(true);
    }

    @Override // com.android.systemui.ExpandHelper.Callback
    public void expansionStateChanged(boolean z) {
        this.mExpandingNotification = z;
        if (!this.mExpandedInThisMotion) {
            this.mMaxScrollAfterExpand = this.mOwnScrollY;
            this.mExpandedInThisMotion = true;
        }
    }

    @Override // com.android.systemui.ExpandHelper.Callback
    public int getMaxExpandHeight(ExpandableView expandableView) {
        return expandableView.getMaxContentHeight();
    }

    public void setScrollingEnabled(boolean z) {
        this.mScrollingEnabled = z;
    }

    public void lockScrollTo(View view) {
        if (this.mForcedScroll == view) {
            return;
        }
        this.mForcedScroll = view;
        scrollTo(view);
    }

    public boolean scrollTo(View view) {
        ExpandableView expandableView = (ExpandableView) view;
        int positionInLinearLayout = getPositionInLinearLayout(view);
        int iTargetScrollForView = targetScrollForView(expandableView, positionInLinearLayout);
        int intrinsicHeight = positionInLinearLayout + expandableView.getIntrinsicHeight();
        if (this.mOwnScrollY >= iTargetScrollForView && intrinsicHeight >= this.mOwnScrollY) {
            return false;
        }
        this.mScroller.startScroll(this.mScrollX, this.mOwnScrollY, 0, iTargetScrollForView - this.mOwnScrollY);
        this.mDontReportNextOverScroll = true;
        animateScroll();
        return true;
    }

    private int targetScrollForView(ExpandableView expandableView, int i) {
        return (((i + expandableView.getIntrinsicHeight()) + getImeInset()) - getHeight()) + getTopPadding();
    }

    @Override // android.view.View
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mBottomInset = windowInsets.getSystemWindowInsetBottom();
        if (this.mOwnScrollY > getScrollRange()) {
            removeCallbacks(this.mReclamp);
            postDelayed(this.mReclamp, 50L);
        } else if (this.mForcedScroll != null) {
            scrollTo(this.mForcedScroll);
        }
        return windowInsets;
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$8 */
    class AnonymousClass8 implements Runnable {
        AnonymousClass8() {
        }

        @Override // java.lang.Runnable
        public void run() {
            NotificationStackScrollLayout.this.mScroller.startScroll(NotificationStackScrollLayout.this.mScrollX, NotificationStackScrollLayout.this.mOwnScrollY, 0, NotificationStackScrollLayout.this.getScrollRange() - NotificationStackScrollLayout.this.mOwnScrollY);
            NotificationStackScrollLayout.this.mDontReportNextOverScroll = true;
            NotificationStackScrollLayout.this.mDontClampNextScroll = true;
            NotificationStackScrollLayout.this.animateScroll();
        }
    }

    public void setExpandingEnabled(boolean z) {
        this.mExpandHelper.setEnabled(z);
    }

    private boolean isScrollingEnabled() {
        return this.mScrollingEnabled;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public boolean canChildBeDismissed(View view) {
        return StackScrollAlgorithm.canChildBeDismissed(view);
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public boolean isAntiFalsingNeeded() {
        return onKeyguard();
    }

    private boolean onKeyguard() {
        return this.mStatusBarState == 1;
    }

    private void setSwipingInProgress(boolean z) {
        this.mSwipingInProgress = z;
        if (z) {
            requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mStatusBarHeight = getResources().getDimensionPixelOffset(R.dimen.status_bar_height);
        this.mSwipeHelper.setDensityScale(getResources().getDisplayMetrics().density);
        this.mSwipeHelper.setPagingTouchSlop(ViewConfiguration.get(getContext()).getScaledPagingTouchSlop());
        initView(getContext());
    }

    public void dismissViewAnimated(View view, Runnable runnable, int i, long j) {
        this.mSwipeHelper.dismissChild(view, 0.0f, runnable, i, true, j, true);
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void snapViewIfNeeded(ExpandableNotificationRow expandableNotificationRow) {
        this.mSwipeHelper.snapChildIfNeeded(expandableNotificationRow, this.mIsExpanded || isPinnedHeadsUp(expandableNotificationRow), expandableNotificationRow.getProvider().isMenuVisible() ? expandableNotificationRow.getTranslation() : 0.0f);
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public ViewGroup getViewParentForNotification(NotificationData.Entry entry) {
        return this;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent;
        boolean zOnScrollTouch;
        boolean zOnTouchEvent2;
        boolean z = motionEvent.getActionMasked() == 3 || motionEvent.getActionMasked() == 1;
        handleEmptySpaceClick(motionEvent);
        if (this.mIsExpanded && !this.mSwipingInProgress && !this.mOnlyScrollingInThisMotion) {
            if (z) {
                this.mExpandHelper.onlyObserveMovements(false);
            }
            boolean z2 = this.mExpandingNotification;
            zOnTouchEvent = this.mExpandHelper.onTouchEvent(motionEvent);
            if (this.mExpandedInThisMotion && !this.mExpandingNotification && z2 && !this.mDisallowScrollingInThisMotion) {
                dispatchDownEventToScroller(motionEvent);
            }
        } else {
            zOnTouchEvent = false;
        }
        if (this.mIsExpanded && !this.mSwipingInProgress && !this.mExpandingNotification && !this.mDisallowScrollingInThisMotion) {
            zOnScrollTouch = onScrollTouch(motionEvent);
        } else {
            zOnScrollTouch = false;
        }
        if (!this.mIsBeingDragged && !this.mExpandingNotification && !this.mExpandedInThisMotion && !this.mOnlyScrollingInThisMotion && !this.mDisallowDismissInThisMotion) {
            zOnTouchEvent2 = this.mSwipeHelper.onTouchEvent(motionEvent);
        } else {
            zOnTouchEvent2 = false;
        }
        NotificationGuts exposedGuts = this.mStatusBar.getGutsManager().getExposedGuts();
        if (exposedGuts != null && !isTouchInView(motionEvent, exposedGuts) && (exposedGuts.getGutsContent() instanceof NotificationSnooze) && ((((NotificationSnooze) exposedGuts.getGutsContent()).isExpanded() && z) || (!zOnTouchEvent2 && zOnScrollTouch))) {
            checkSnoozeLeavebehind();
        }
        if (motionEvent.getActionMasked() == 1) {
            this.mCheckForLeavebehind = true;
        }
        return zOnTouchEvent2 || zOnScrollTouch || zOnTouchEvent || super.onTouchEvent(motionEvent);
    }

    private void dispatchDownEventToScroller(MotionEvent motionEvent) {
        MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
        motionEventObtain.setAction(0);
        onScrollTouch(motionEventObtain);
        motionEventObtain.recycle();
    }

    @Override // android.view.View
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if (!isScrollingEnabled() || !this.mIsExpanded || this.mSwipingInProgress || this.mExpandingNotification || this.mDisallowScrollingInThisMotion) {
            return false;
        }
        if ((motionEvent.getSource() & 2) != 0 && motionEvent.getAction() == 8 && !this.mIsBeingDragged) {
            float axisValue = motionEvent.getAxisValue(9);
            if (axisValue != 0.0f) {
                int verticalScrollFactor = (int) (axisValue * getVerticalScrollFactor());
                int scrollRange = getScrollRange();
                int i = this.mOwnScrollY;
                int i2 = i - verticalScrollFactor;
                if (i2 >= 0) {
                    if (i2 > scrollRange) {
                        i2 = scrollRange;
                    }
                } else {
                    i2 = 0;
                }
                if (i2 != i) {
                    setOwnScrollY(i2);
                    return true;
                }
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private boolean onScrollTouch(MotionEvent motionEvent) {
        float fOverScrollUp;
        if (!isScrollingEnabled()) {
            return false;
        }
        if (isInsideQsContainer(motionEvent) && !this.mIsBeingDragged) {
            return false;
        }
        this.mForcedScroll = null;
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(motionEvent);
        switch (motionEvent.getAction() & 255) {
            case 0:
                if (getChildCount() == 0 || !isInContentBounds(motionEvent)) {
                    return false;
                }
                setIsBeingDragged(!this.mScroller.isFinished());
                if (!this.mScroller.isFinished()) {
                    this.mScroller.forceFinished(true);
                }
                this.mLastMotionY = (int) motionEvent.getY();
                this.mDownX = (int) motionEvent.getX();
                this.mActivePointerId = motionEvent.getPointerId(0);
                return true;
            case 1:
                if (this.mIsBeingDragged) {
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                    int yVelocity = (int) velocityTracker.getYVelocity(this.mActivePointerId);
                    if (shouldOverScrollFling(yVelocity)) {
                        onOverScrollFling(true, yVelocity);
                    } else if (getChildCount() > 0) {
                        if (Math.abs(yVelocity) > this.mMinimumVelocity) {
                            if (getCurrentOverScrollAmount(true) == 0.0f || yVelocity > 0) {
                                fling(-yVelocity);
                            } else {
                                onOverScrollFling(false, yVelocity);
                            }
                        } else if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                            animateScroll();
                        }
                    }
                    this.mActivePointerId = -1;
                    endDrag();
                }
                return true;
            case 2:
                int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                if (iFindPointerIndex == -1) {
                    Log.e("StackScroller", "Invalid pointerId=" + this.mActivePointerId + " in onTouchEvent");
                } else {
                    int y = (int) motionEvent.getY(iFindPointerIndex);
                    int x = (int) motionEvent.getX(iFindPointerIndex);
                    int i = this.mLastMotionY - y;
                    int iAbs = Math.abs(x - this.mDownX);
                    int iAbs2 = Math.abs(i);
                    if (!this.mIsBeingDragged && iAbs2 > this.mTouchSlop && iAbs2 > iAbs) {
                        setIsBeingDragged(true);
                        i = i > 0 ? i - this.mTouchSlop : i + this.mTouchSlop;
                    }
                    if (this.mIsBeingDragged) {
                        this.mLastMotionY = y;
                        int scrollRange = getScrollRange();
                        if (this.mExpandedInThisMotion) {
                            scrollRange = Math.min(scrollRange, this.mMaxScrollAfterExpand);
                        }
                        if (i < 0) {
                            fOverScrollUp = overScrollDown(i);
                        } else {
                            fOverScrollUp = overScrollUp(i, scrollRange);
                        }
                        if (fOverScrollUp != 0.0f) {
                            customOverScrollBy((int) fOverScrollUp, this.mOwnScrollY, scrollRange, getHeight() / 2);
                            checkSnoozeLeavebehind();
                        }
                    }
                }
                return true;
            case 3:
                if (this.mIsBeingDragged && getChildCount() > 0) {
                    if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                        animateScroll();
                    }
                    this.mActivePointerId = -1;
                    endDrag();
                }
                return true;
            case 4:
            default:
                return true;
            case 5:
                int actionIndex = motionEvent.getActionIndex();
                this.mLastMotionY = (int) motionEvent.getY(actionIndex);
                this.mDownX = (int) motionEvent.getX(actionIndex);
                this.mActivePointerId = motionEvent.getPointerId(actionIndex);
                return true;
            case 6:
                onSecondaryPointerUp(motionEvent);
                this.mLastMotionY = (int) motionEvent.getY(motionEvent.findPointerIndex(this.mActivePointerId));
                this.mDownX = (int) motionEvent.getX(motionEvent.findPointerIndex(this.mActivePointerId));
                return true;
        }
    }

    protected boolean isInsideQsContainer(MotionEvent motionEvent) {
        return motionEvent.getY() < ((float) this.mQsContainer.getBottom());
    }

    private void onOverScrollFling(boolean z, int i) {
        if (this.mOverscrollTopChangedListener != null) {
            this.mOverscrollTopChangedListener.flingTopOverscroll(i, z);
        }
        this.mDontReportNextOverScroll = true;
        setOverScrollAmount(0.0f, true, false);
    }

    private float overScrollUp(int i, int i2) {
        int iMax = Math.max(i, 0);
        float currentOverScrollAmount = getCurrentOverScrollAmount(true);
        float f = currentOverScrollAmount - iMax;
        if (currentOverScrollAmount > 0.0f) {
            setOverScrollAmount(f, true, false);
        }
        float f2 = f < 0.0f ? -f : 0.0f;
        float f3 = this.mOwnScrollY + f2;
        float f4 = i2;
        if (f3 <= f4) {
            return f2;
        }
        if (!this.mExpandedInThisMotion) {
            setOverScrolledPixels((getCurrentOverScrolledPixels(false) + f3) - f4, false, false);
        }
        setOwnScrollY(i2);
        return 0.0f;
    }

    private float overScrollDown(int i) {
        int iMin = Math.min(i, 0);
        float currentOverScrollAmount = getCurrentOverScrollAmount(false);
        float f = iMin + currentOverScrollAmount;
        if (currentOverScrollAmount > 0.0f) {
            setOverScrollAmount(f, false, false);
        }
        if (f >= 0.0f) {
            f = 0.0f;
        }
        float f2 = this.mOwnScrollY + f;
        if (f2 >= 0.0f) {
            return f;
        }
        setOverScrolledPixels(getCurrentOverScrolledPixels(true) - f2, true, false);
        setOwnScrollY(0);
        return 0.0f;
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() & 65280) >> 8;
        if (motionEvent.getPointerId(action) == this.mActivePointerId) {
            int i = action == 0 ? 1 : 0;
            this.mLastMotionY = (int) motionEvent.getY(i);
            this.mActivePointerId = motionEvent.getPointerId(i);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    public void setFinishScrollingCallback(Runnable runnable) {
        this.mFinishScrollingCallback = runnable;
    }

    private void animateScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int i = this.mOwnScrollY;
            int currY = this.mScroller.getCurrY();
            if (i != currY) {
                int scrollRange = getScrollRange();
                if ((currY < 0 && i >= 0) || (currY > scrollRange && i <= scrollRange)) {
                    float currVelocity = this.mScroller.getCurrVelocity();
                    if (currVelocity >= this.mMinimumVelocity) {
                        this.mMaxOverScroll = (Math.abs(currVelocity) / 1000.0f) * this.mOverflingDistance;
                    }
                }
                if (this.mDontClampNextScroll) {
                    scrollRange = Math.max(scrollRange, i);
                }
                customOverScrollBy(currY - i, i, scrollRange, (int) this.mMaxOverScroll);
            }
            postOnAnimation(this.mAnimateScroll);
            return;
        }
        this.mDontClampNextScroll = false;
        if (this.mFinishScrollingCallback != null) {
            this.mFinishScrollingCallback.run();
        }
    }

    private boolean customOverScrollBy(int i, int i2, int i3, int i4) {
        int i5 = i2 + i;
        int i6 = -i4;
        int i7 = i3 + i4;
        boolean z = true;
        if (i5 <= i7) {
            if (i5 >= i6) {
                z = false;
                i6 = i5;
            }
        } else {
            i6 = i7;
        }
        onCustomOverScrolled(i6, z);
        return z;
    }

    public void setOverScrolledPixels(float f, boolean z, boolean z2) {
        setOverScrollAmount(f * getRubberBandFactor(z), z, z2, true);
    }

    public void setOverScrollAmount(float f, boolean z, boolean z2) {
        setOverScrollAmount(f, z, z2, true);
    }

    public void setOverScrollAmount(float f, boolean z, boolean z2, boolean z3) {
        setOverScrollAmount(f, z, z2, z3, isRubberbanded(z));
    }

    public void setOverScrollAmount(float f, boolean z, boolean z2, boolean z3, boolean z4) {
        if (z3) {
            this.mStateAnimator.cancelOverScrollAnimators(z);
        }
        setOverScrollAmountInternal(f, z, z2, z4);
    }

    private void setOverScrollAmountInternal(float f, boolean z, boolean z2, boolean z3) {
        float fMax = Math.max(0.0f, f);
        if (z2) {
            this.mStateAnimator.animateOverScrollToAmount(fMax, z, z3);
            return;
        }
        setOverScrolledPixels(fMax / getRubberBandFactor(z), z);
        this.mAmbientState.setOverScrollAmount(fMax, z);
        if (z) {
            notifyOverscrollTopListener(fMax, z3);
        }
        requestChildrenUpdate();
    }

    private void notifyOverscrollTopListener(float f, boolean z) {
        this.mExpandHelper.onlyObserveMovements(f > 1.0f);
        if (this.mDontReportNextOverScroll) {
            this.mDontReportNextOverScroll = false;
        } else if (this.mOverscrollTopChangedListener != null) {
            this.mOverscrollTopChangedListener.onOverscrollTopChanged(f, z);
        }
    }

    public void setOverscrollTopChangedListener(OnOverscrollTopChangedListener onOverscrollTopChangedListener) {
        this.mOverscrollTopChangedListener = onOverscrollTopChangedListener;
    }

    public float getCurrentOverScrollAmount(boolean z) {
        return this.mAmbientState.getOverScrollAmount(z);
    }

    public float getCurrentOverScrolledPixels(boolean z) {
        return z ? this.mOverScrolledTopPixels : this.mOverScrolledBottomPixels;
    }

    private void setOverScrolledPixels(float f, boolean z) {
        if (z) {
            this.mOverScrolledTopPixels = f;
        } else {
            this.mOverScrolledBottomPixels = f;
        }
    }

    private void onCustomOverScrolled(int i, boolean z) {
        if (!this.mScroller.isFinished()) {
            setOwnScrollY(i);
            if (z) {
                springBack();
                return;
            }
            float currentOverScrollAmount = getCurrentOverScrollAmount(true);
            if (this.mOwnScrollY < 0) {
                notifyOverscrollTopListener(-this.mOwnScrollY, isRubberbanded(true));
                return;
            } else {
                notifyOverscrollTopListener(currentOverScrollAmount, isRubberbanded(true));
                return;
            }
        }
        setOwnScrollY(i);
    }

    private void springBack() {
        float f;
        boolean z;
        int scrollRange = getScrollRange();
        boolean z2 = this.mOwnScrollY <= 0;
        boolean z3 = this.mOwnScrollY >= scrollRange;
        if (z2 || z3) {
            if (z2) {
                f = -this.mOwnScrollY;
                setOwnScrollY(0);
                this.mDontReportNextOverScroll = true;
                z = true;
            } else {
                float f2 = this.mOwnScrollY - scrollRange;
                setOwnScrollY(scrollRange);
                f = f2;
                z = false;
            }
            setOverScrollAmount(f, z, false);
            setOverScrollAmount(0.0f, z, true);
            this.mScroller.forceFinished(true);
        }
    }

    private int getScrollRange() {
        int iMax = Math.max(0, getContentHeight() - this.mMaxLayoutHeight);
        int imeInset = getImeInset();
        return iMax + Math.min(imeInset, Math.max(0, getContentHeight() - (getHeight() - imeInset)));
    }

    private int getImeInset() {
        return Math.max(0, this.mBottomInset - (getRootView().getHeight() - getHeight()));
    }

    public ExpandableView getFirstChildNotGone() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8 && childAt != this.mShelf) {
                return (ExpandableView) childAt;
            }
        }
        return null;
    }

    private View getFirstChildBelowTranlsationY(float f, boolean z) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8) {
                float translationY = childAt.getTranslationY();
                if (translationY >= f) {
                    return childAt;
                }
                if (!z && (childAt instanceof ExpandableNotificationRow)) {
                    ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) childAt;
                    if (expandableNotificationRow.isSummaryWithChildren() && expandableNotificationRow.areChildrenExpanded()) {
                        List<ExpandableNotificationRow> notificationChildren = expandableNotificationRow.getNotificationChildren();
                        for (int i2 = 0; i2 < notificationChildren.size(); i2++) {
                            ExpandableNotificationRow expandableNotificationRow2 = notificationChildren.get(i2);
                            if (expandableNotificationRow2.getTranslationY() + translationY >= f) {
                                return expandableNotificationRow2;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public View getLastChildNotGone() {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt.getVisibility() != 8 && childAt != this.mShelf) {
                return childAt;
            }
        }
        return null;
    }

    public int getNotGoneChildCount() {
        int childCount = getChildCount();
        int i = 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i2);
            if (expandableView.getVisibility() != 8 && !expandableView.willBeGone() && expandableView != this.mShelf) {
                i++;
            }
        }
        return i;
    }

    public int getContentHeight() {
        return this.mContentHeight;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2163=4] */
    private void updateContentHeight() {
        float fInterpolate;
        float fInterpolate2;
        float f = this.mPaddingBetweenElements;
        int i = this.mAmbientState.isFullyDark() ? hasPulsingNotifications() ? 1 : 0 : this.mMaxDisplayedNotifications;
        float f2 = f;
        int i2 = 0;
        int intrinsicHeight = 0;
        boolean z = false;
        float f3 = 0.0f;
        for (int i3 = 0; i3 < getChildCount(); i3++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i3);
            boolean z2 = expandableView == this.mFooterView && onKeyguard();
            if (expandableView.getVisibility() != 8 && !expandableView.hasNoContentHeight() && !z2) {
                boolean z3 = i != -1 && i2 >= i;
                boolean z4 = this.mAmbientState.isFullyDark() && hasPulsingNotifications() && (expandableView instanceof ExpandableNotificationRow) && !isPulsing(((ExpandableNotificationRow) expandableView).getEntry());
                if (z3 || z4) {
                    expandableView = this.mShelf;
                    z = true;
                }
                float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
                if (increasedPaddingAmount >= 0.0f) {
                    fInterpolate2 = (int) NotificationUtils.interpolate(f2, this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
                    fInterpolate = (int) NotificationUtils.interpolate(this.mPaddingBetweenElements, this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
                } else {
                    int iInterpolate = (int) NotificationUtils.interpolate(0.0f, this.mPaddingBetweenElements, 1.0f + increasedPaddingAmount);
                    float fInterpolate3 = f3 > 0.0f ? (int) NotificationUtils.interpolate(iInterpolate, this.mIncreasedPaddingBetweenElements, f3) : iInterpolate;
                    fInterpolate = iInterpolate;
                    fInterpolate2 = fInterpolate3;
                }
                if (intrinsicHeight != 0) {
                    intrinsicHeight = (int) (intrinsicHeight + fInterpolate2);
                }
                intrinsicHeight += expandableView.getIntrinsicHeight();
                i2++;
                if (z) {
                    break;
                }
                f2 = fInterpolate;
                f3 = increasedPaddingAmount;
            }
        }
        this.mIntrinsicContentHeight = intrinsicHeight;
        this.mContentHeight = intrinsicHeight + this.mTopPadding + this.mBottomMargin;
        updateScrollability();
        clampScrollPosition();
        this.mAmbientState.setLayoutMaxHeight(this.mContentHeight);
    }

    private boolean isPulsing(NotificationData.Entry entry) {
        return this.mAmbientState.isPulsing(entry);
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public boolean hasPulsingNotifications() {
        return this.mPulsing;
    }

    private void updateScrollability() {
        boolean z = !this.mQsExpanded && getScrollRange() > 0;
        if (z != this.mScrollable) {
            this.mScrollable = z;
            setFocusable(z);
            updateForwardAndBackwardScrollability();
        }
    }

    private void updateForwardAndBackwardScrollability() {
        boolean z = this.mScrollable && this.mOwnScrollY < getScrollRange();
        boolean z2 = this.mScrollable && this.mOwnScrollY > 0;
        boolean z3 = (z == this.mForwardScrollable && z2 == this.mBackwardScrollable) ? false : true;
        this.mForwardScrollable = z;
        this.mBackwardScrollable = z2;
        if (z3) {
            sendAccessibilityEvent(2048);
        }
    }

    private void updateBackground() {
        boolean z;
        if (!this.mShouldDrawNotificationBackground || this.mAmbientState.isFullyDark()) {
            return;
        }
        updateBackgroundBounds();
        if (!this.mCurrentBounds.equals(this.mBackgroundBounds)) {
            if (this.mAnimateNextBackgroundTop || this.mAnimateNextBackgroundBottom || areBoundsAnimating()) {
                z = true;
            } else {
                z = false;
            }
            if (!isExpanded()) {
                abortBackgroundAnimators();
                z = false;
            }
            if (z) {
                startBackgroundAnimation();
            } else {
                this.mCurrentBounds.set(this.mBackgroundBounds);
                applyCurrentBackgroundBounds();
            }
        } else {
            abortBackgroundAnimators();
        }
        this.mAnimateNextBackgroundBottom = false;
        this.mAnimateNextBackgroundTop = false;
    }

    private void abortBackgroundAnimators() {
        if (this.mBottomAnimator != null) {
            this.mBottomAnimator.cancel();
        }
        if (this.mTopAnimator != null) {
            this.mTopAnimator.cancel();
        }
    }

    private boolean areBoundsAnimating() {
        return (this.mBottomAnimator == null && this.mTopAnimator == null) ? false : true;
    }

    private void startBackgroundAnimation() {
        this.mCurrentBounds.left = this.mBackgroundBounds.left;
        this.mCurrentBounds.right = this.mBackgroundBounds.right;
        startBottomAnimation();
        startTopAnimation();
    }

    private void startTopAnimation() {
        int i = this.mEndAnimationRect.top;
        int i2 = this.mBackgroundBounds.top;
        ObjectAnimator objectAnimator = this.mTopAnimator;
        if (objectAnimator != null && i == i2) {
            return;
        }
        if (!this.mAnimateNextBackgroundTop) {
            if (objectAnimator != null) {
                int i3 = this.mStartAnimationRect.top;
                objectAnimator.getValues()[0].setIntValues(i3, i2);
                this.mStartAnimationRect.top = i3;
                this.mEndAnimationRect.top = i2;
                objectAnimator.setCurrentPlayTime(objectAnimator.getCurrentPlayTime());
                return;
            }
            setBackgroundTop(i2);
            return;
        }
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "backgroundTop", this.mCurrentBounds.top, i2);
        objectAnimatorOfInt.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        objectAnimatorOfInt.setDuration(360L);
        objectAnimatorOfInt.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.9
            AnonymousClass9() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                NotificationStackScrollLayout.this.mStartAnimationRect.top = -1;
                NotificationStackScrollLayout.this.mEndAnimationRect.top = -1;
                NotificationStackScrollLayout.this.mTopAnimator = null;
            }
        });
        objectAnimatorOfInt.start();
        this.mStartAnimationRect.top = this.mCurrentBounds.top;
        this.mEndAnimationRect.top = i2;
        this.mTopAnimator = objectAnimatorOfInt;
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$9 */
    class AnonymousClass9 extends AnimatorListenerAdapter {
        AnonymousClass9() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            NotificationStackScrollLayout.this.mStartAnimationRect.top = -1;
            NotificationStackScrollLayout.this.mEndAnimationRect.top = -1;
            NotificationStackScrollLayout.this.mTopAnimator = null;
        }
    }

    private void startBottomAnimation() {
        int i = this.mStartAnimationRect.bottom;
        int i2 = this.mEndAnimationRect.bottom;
        int i3 = this.mBackgroundBounds.bottom;
        ObjectAnimator objectAnimator = this.mBottomAnimator;
        if (objectAnimator != null && i2 == i3) {
            return;
        }
        if (!this.mAnimateNextBackgroundBottom) {
            if (objectAnimator != null) {
                objectAnimator.getValues()[0].setIntValues(i, i3);
                this.mStartAnimationRect.bottom = i;
                this.mEndAnimationRect.bottom = i3;
                objectAnimator.setCurrentPlayTime(objectAnimator.getCurrentPlayTime());
                return;
            }
            setBackgroundBottom(i3);
            return;
        }
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "backgroundBottom", this.mCurrentBounds.bottom, i3);
        objectAnimatorOfInt.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        objectAnimatorOfInt.setDuration(360L);
        objectAnimatorOfInt.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.10
            AnonymousClass10() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                NotificationStackScrollLayout.this.mStartAnimationRect.bottom = -1;
                NotificationStackScrollLayout.this.mEndAnimationRect.bottom = -1;
                NotificationStackScrollLayout.this.mBottomAnimator = null;
            }
        });
        objectAnimatorOfInt.start();
        this.mStartAnimationRect.bottom = this.mCurrentBounds.bottom;
        this.mEndAnimationRect.bottom = i3;
        this.mBottomAnimator = objectAnimatorOfInt;
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$10 */
    class AnonymousClass10 extends AnimatorListenerAdapter {
        AnonymousClass10() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            NotificationStackScrollLayout.this.mStartAnimationRect.bottom = -1;
            NotificationStackScrollLayout.this.mEndAnimationRect.bottom = -1;
            NotificationStackScrollLayout.this.mBottomAnimator = null;
        }
    }

    private void setBackgroundTop(int i) {
        this.mCurrentBounds.top = i;
        applyCurrentBackgroundBounds();
    }

    public void setBackgroundBottom(int i) {
        this.mCurrentBounds.bottom = i;
        applyCurrentBackgroundBounds();
    }

    private void applyCurrentBackgroundBounds() {
        Rect rect;
        if (!this.mShouldDrawNotificationBackground) {
            return;
        }
        boolean z = this.mDarkAmount != 0.0f || this.mAmbientState.isDark();
        ScrimController scrimController = this.mScrimController;
        if (this.mFadingOut || this.mParentNotFullyVisible || z || this.mIsClipped) {
            rect = null;
        } else {
            rect = this.mCurrentBounds;
        }
        scrimController.setExcludedBackgroundArea(rect);
        invalidate();
    }

    private void updateBackgroundBounds() {
        int iCeil;
        ActivatableNotificationView activatableNotificationView;
        int finalActualHeight;
        int iMax;
        int finalTranslationY;
        getLocationInWindow(this.mTempInt2);
        this.mBackgroundBounds.left = this.mTempInt2[0] + this.mSidePaddings;
        this.mBackgroundBounds.right = (this.mTempInt2[0] + getWidth()) - this.mSidePaddings;
        if (!this.mIsExpanded) {
            this.mBackgroundBounds.top = 0;
            this.mBackgroundBounds.bottom = 0;
            return;
        }
        if (this.mFirstVisibleBackgroundChild != null) {
            int iCeil2 = (int) Math.ceil(ViewState.getFinalTranslationY(r0));
            if (!this.mAnimateNextBackgroundTop && ((this.mTopAnimator != null || this.mCurrentBounds.top != iCeil2) && (this.mTopAnimator == null || this.mEndAnimationRect.top != iCeil2))) {
                iCeil = (int) Math.ceil(r0.getTranslationY());
            } else {
                iCeil = iCeil2;
            }
        } else {
            iCeil = 0;
        }
        if (this.mShelf.hasItemsInStableShelf() && this.mShelf.getVisibility() != 8) {
            activatableNotificationView = this.mShelf;
        } else {
            activatableNotificationView = this.mLastVisibleBackgroundChild;
        }
        if (activatableNotificationView != null) {
            if (activatableNotificationView == this.mShelf) {
                finalTranslationY = (int) this.mShelf.getTranslationY();
            } else {
                finalTranslationY = (int) ViewState.getFinalTranslationY(activatableNotificationView);
            }
            finalActualHeight = (finalTranslationY + ExpandableViewState.getFinalActualHeight(activatableNotificationView)) - activatableNotificationView.getClipBottomAmount();
            if (!this.mAnimateNextBackgroundBottom && ((this.mBottomAnimator != null || this.mCurrentBounds.bottom != finalActualHeight) && (this.mBottomAnimator == null || this.mEndAnimationRect.bottom != finalActualHeight))) {
                finalActualHeight = (int) ((activatableNotificationView.getTranslationY() + activatableNotificationView.getActualHeight()) - activatableNotificationView.getClipBottomAmount());
            }
        } else {
            iCeil = this.mTopPadding;
            finalActualHeight = iCeil;
        }
        if (this.mStatusBarState != 1) {
            iMax = (int) Math.max(this.mTopPadding + this.mStackTranslation, iCeil);
        } else {
            iMax = Math.max(0, iCeil);
        }
        this.mBackgroundBounds.top = iMax;
        this.mBackgroundBounds.bottom = Math.max(finalActualHeight, iMax);
    }

    private ActivatableNotificationView getLastChildWithBackground() {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt.getVisibility() != 8 && (childAt instanceof ActivatableNotificationView) && childAt != this.mShelf) {
                return (ActivatableNotificationView) childAt;
            }
        }
        return null;
    }

    private ActivatableNotificationView getFirstChildWithBackground() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8 && (childAt instanceof ActivatableNotificationView) && childAt != this.mShelf) {
                return (ActivatableNotificationView) childAt;
            }
        }
        return null;
    }

    protected void fling(int i) {
        if (getChildCount() > 0) {
            int scrollRange = getScrollRange();
            float currentOverScrollAmount = getCurrentOverScrollAmount(true);
            float currentOverScrollAmount2 = getCurrentOverScrollAmount(false);
            if (i < 0 && currentOverScrollAmount > 0.0f) {
                setOwnScrollY(this.mOwnScrollY - ((int) currentOverScrollAmount));
                this.mDontReportNextOverScroll = true;
                setOverScrollAmount(0.0f, true, false);
                this.mMaxOverScroll = ((Math.abs(i) / 1000.0f) * getRubberBandFactor(true) * this.mOverflingDistance) + currentOverScrollAmount;
            } else if (i > 0 && currentOverScrollAmount2 > 0.0f) {
                setOwnScrollY((int) (this.mOwnScrollY + currentOverScrollAmount2));
                setOverScrollAmount(0.0f, false, false);
                this.mMaxOverScroll = ((Math.abs(i) / 1000.0f) * getRubberBandFactor(false) * this.mOverflingDistance) + currentOverScrollAmount2;
            } else {
                this.mMaxOverScroll = 0.0f;
            }
            int iMax = Math.max(0, scrollRange);
            if (this.mExpandedInThisMotion) {
                iMax = Math.min(iMax, this.mMaxScrollAfterExpand);
            }
            this.mScroller.fling(this.mScrollX, this.mOwnScrollY, 1, i, 0, 0, 0, iMax, 0, (!this.mExpandedInThisMotion || this.mOwnScrollY < 0) ? 1073741823 : 0);
            animateScroll();
        }
    }

    private boolean shouldOverScrollFling(int i) {
        return this.mScrolledToTopOnFirstDown && !this.mExpandedInThisMotion && getCurrentOverScrollAmount(true) > this.mMinTopOverScrollToEscape && i > 0;
    }

    public void updateTopPadding(float f, boolean z, boolean z2) {
        int iClampPadding = (int) f;
        if (getLayoutMinHeight() + iClampPadding > getHeight()) {
            this.mTopPaddingOverflow = r0 - getHeight();
        } else {
            this.mTopPaddingOverflow = 0.0f;
        }
        if (!z2) {
            iClampPadding = clampPadding(iClampPadding);
        }
        setTopPadding(iClampPadding, z);
        setExpandedHeight(this.mExpandedHeight);
    }

    public void setMaxTopPadding(int i) {
        this.mMaxTopPadding = i;
    }

    public int getLayoutMinHeight() {
        if (isHeadsUpTransition()) {
            return getTopHeadsUpPinnedHeight();
        }
        if (this.mShelf.getVisibility() == 8) {
            return 0;
        }
        return this.mShelf.getIntrinsicHeight();
    }

    public float getTopPaddingOverflow() {
        return this.mTopPaddingOverflow;
    }

    public int getPeekHeight() {
        ExpandableView firstChildNotGone = getFirstChildNotGone();
        int collapsedHeight = firstChildNotGone != null ? firstChildNotGone.getCollapsedHeight() : this.mCollapsedSize;
        int intrinsicHeight = 0;
        if (this.mLastVisibleBackgroundChild != null && this.mShelf.getVisibility() != 8) {
            intrinsicHeight = this.mShelf.getIntrinsicHeight();
        }
        return this.mIntrinsicPadding + collapsedHeight + intrinsicHeight;
    }

    private int clampPadding(int i) {
        return Math.max(i, this.mIntrinsicPadding);
    }

    private float getRubberBandFactor(boolean z) {
        if (!z) {
            return 0.35f;
        }
        if (this.mExpandedInThisMotion) {
            return 0.15f;
        }
        if (this.mIsExpansionChanging || this.mPanelTracking) {
            return 0.21f;
        }
        if (!this.mScrolledToTopOnFirstDown) {
            return 0.35f;
        }
        return 1.0f;
    }

    private boolean isRubberbanded(boolean z) {
        return !z || this.mExpandedInThisMotion || this.mIsExpansionChanging || this.mPanelTracking || !this.mScrolledToTopOnFirstDown;
    }

    private void endDrag() {
        setIsBeingDragged(false);
        recycleVelocityTracker();
        if (getCurrentOverScrollAmount(true) > 0.0f) {
            setOverScrollAmount(0.0f, true, true);
        }
        if (getCurrentOverScrollAmount(false) > 0.0f) {
            setOverScrollAmount(0.0f, false, true);
        }
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean zOnInterceptTouchEvent;
        boolean zOnInterceptTouchEventScroll;
        boolean zOnInterceptTouchEvent2;
        initDownStates(motionEvent);
        handleEmptySpaceClick(motionEvent);
        if (!this.mSwipingInProgress && !this.mOnlyScrollingInThisMotion) {
            zOnInterceptTouchEvent = this.mExpandHelper.onInterceptTouchEvent(motionEvent);
        } else {
            zOnInterceptTouchEvent = false;
        }
        if (!this.mSwipingInProgress && !this.mExpandingNotification) {
            zOnInterceptTouchEventScroll = onInterceptTouchEventScroll(motionEvent);
        } else {
            zOnInterceptTouchEventScroll = false;
        }
        if (!this.mIsBeingDragged && !this.mExpandingNotification && !this.mExpandedInThisMotion && !this.mOnlyScrollingInThisMotion && !this.mDisallowDismissInThisMotion) {
            zOnInterceptTouchEvent2 = this.mSwipeHelper.onInterceptTouchEvent(motionEvent);
        } else {
            zOnInterceptTouchEvent2 = false;
        }
        boolean z = motionEvent.getActionMasked() == 1;
        if (!isTouchInView(motionEvent, this.mStatusBar.getGutsManager().getExposedGuts()) && z && !zOnInterceptTouchEvent2 && !zOnInterceptTouchEvent && !zOnInterceptTouchEventScroll) {
            this.mCheckForLeavebehind = false;
            this.mStatusBar.getGutsManager().closeAndSaveGuts(true, false, false, -1, -1, false);
        }
        if (motionEvent.getActionMasked() == 1) {
            this.mCheckForLeavebehind = true;
        }
        return zOnInterceptTouchEvent2 || zOnInterceptTouchEventScroll || zOnInterceptTouchEvent || super.onInterceptTouchEvent(motionEvent);
    }

    private void handleEmptySpaceClick(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 1:
                if (this.mStatusBarState != 1 && this.mTouchIsClick && isBelowLastNotification(this.mInitialTouchX, this.mInitialTouchY)) {
                    this.mOnEmptySpaceClickListener.onEmptySpaceClicked(this.mInitialTouchX, this.mInitialTouchY);
                    break;
                }
                break;
            case 2:
                if (this.mTouchIsClick) {
                    if (Math.abs(motionEvent.getY() - this.mInitialTouchY) > this.mTouchSlop || Math.abs(motionEvent.getX() - this.mInitialTouchX) > this.mTouchSlop) {
                        this.mTouchIsClick = false;
                        break;
                    }
                }
                break;
        }
    }

    private void initDownStates(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mExpandedInThisMotion = false;
            this.mOnlyScrollingInThisMotion = !this.mScroller.isFinished();
            this.mDisallowScrollingInThisMotion = false;
            this.mDisallowDismissInThisMotion = false;
            this.mTouchIsClick = true;
            this.mInitialTouchX = motionEvent.getX();
            this.mInitialTouchY = motionEvent.getY();
        }
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void setChildTransferInProgress(boolean z) {
        this.mChildTransferInProgress = z;
    }

    @Override // android.view.ViewGroup
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        if (!this.mChildTransferInProgress) {
            onViewRemovedInternal(view, this);
        }
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void cleanUpViewState(View view) {
        if (view == this.mTranslatingParentView) {
            this.mTranslatingParentView = null;
        }
        this.mCurrentStackScrollState.removeViewStateForView(view);
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void requestDisallowInterceptTouchEvent(boolean z) {
        super.requestDisallowInterceptTouchEvent(z);
        if (z) {
            cancelLongPress();
        }
    }

    private void onViewRemovedInternal(View view, ViewGroup viewGroup) {
        if (this.mChangePositionInProgress) {
            return;
        }
        ExpandableView expandableView = (ExpandableView) view;
        expandableView.setOnHeightChangedListener(null);
        this.mCurrentStackScrollState.removeViewStateForView(view);
        updateScrollStateForRemovedChild(expandableView);
        if (generateRemoveAnimation(view)) {
            if (!this.mSwipedOutViews.contains(view) || Math.abs(expandableView.getTranslation()) != expandableView.getWidth()) {
                viewGroup.addTransientView(view, 0);
                expandableView.setTransientContainer(viewGroup);
            }
        } else {
            this.mSwipedOutViews.remove(view);
        }
        updateAnimationState(false, view);
        focusNextViewIfFocused(view);
    }

    private void focusNextViewIfFocused(View view) {
        float translationY;
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
            if (expandableNotificationRow.shouldRefocusOnDismiss()) {
                View childAfterViewWhenDismissed = expandableNotificationRow.getChildAfterViewWhenDismissed();
                if (childAfterViewWhenDismissed == null) {
                    View groupParentWhenDismissed = expandableNotificationRow.getGroupParentWhenDismissed();
                    if (groupParentWhenDismissed != null) {
                        translationY = groupParentWhenDismissed.getTranslationY();
                    } else {
                        translationY = view.getTranslationY();
                    }
                    childAfterViewWhenDismissed = getFirstChildBelowTranlsationY(translationY, true);
                }
                if (childAfterViewWhenDismissed != null) {
                    childAfterViewWhenDismissed.requestAccessibilityFocus();
                }
            }
        }
    }

    private boolean isChildInGroup(View view) {
        return (view instanceof ExpandableNotificationRow) && this.mGroupManager.isChildInGroupWithSummary(((ExpandableNotificationRow) view).getStatusBarNotification());
    }

    private boolean generateRemoveAnimation(View view) {
        if (removeRemovedChildFromHeadsUpChangeAnimations(view)) {
            this.mAddedHeadsUpChildren.remove(view);
            return false;
        }
        if (isClickedHeadsUp(view)) {
            this.mClearTransientViewsWhenFinished.add((ExpandableView) view);
            return true;
        }
        if (!this.mIsExpanded || !this.mAnimationsEnabled || isChildInInvisibleGroup(view)) {
            return false;
        }
        if (!this.mChildrenToAddAnimated.contains(view)) {
            this.mChildrenToRemoveAnimated.add(view);
            this.mNeedsAnimation = true;
            return true;
        }
        this.mChildrenToAddAnimated.remove(view);
        this.mFromMoreCardAdditions.remove(view);
        return false;
    }

    private boolean isClickedHeadsUp(View view) {
        return HeadsUpUtil.isClickedHeadsUpNotification(view);
    }

    private boolean removeRemovedChildFromHeadsUpChangeAnimations(View view) {
        Iterator<Pair<ExpandableNotificationRow, Boolean>> it = this.mHeadsUpChangeAnimations.iterator();
        boolean z = false;
        while (it.hasNext()) {
            Pair<ExpandableNotificationRow, Boolean> next = it.next();
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) next.first;
            boolean zBooleanValue = ((Boolean) next.second).booleanValue();
            if (view == expandableNotificationRow) {
                this.mTmpList.add(next);
                z |= zBooleanValue;
            }
        }
        if (z) {
            this.mHeadsUpChangeAnimations.removeAll(this.mTmpList);
            ((ExpandableNotificationRow) view).setHeadsUpAnimatingAway(false);
        }
        this.mTmpList.clear();
        return z;
    }

    private boolean isChildInInvisibleGroup(View view) {
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
            ExpandableNotificationRow groupSummary = this.mGroupManager.getGroupSummary(expandableNotificationRow.getStatusBarNotification());
            return (groupSummary == null || groupSummary == expandableNotificationRow || expandableNotificationRow.getVisibility() != 4) ? false : true;
        }
        return false;
    }

    private void updateScrollStateForRemovedChild(ExpandableView expandableView) {
        int iInterpolate;
        int positionInLinearLayout = getPositionInLinearLayout(expandableView);
        float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
        if (increasedPaddingAmount < 0.0f) {
            iInterpolate = (int) NotificationUtils.interpolate(0.0f, this.mPaddingBetweenElements, 1.0f + increasedPaddingAmount);
        } else {
            iInterpolate = (int) NotificationUtils.interpolate(this.mPaddingBetweenElements, this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
        }
        int intrinsicHeight = getIntrinsicHeight(expandableView) + iInterpolate;
        if (positionInLinearLayout + intrinsicHeight <= this.mOwnScrollY) {
            setOwnScrollY(this.mOwnScrollY - intrinsicHeight);
        } else if (positionInLinearLayout < this.mOwnScrollY) {
            setOwnScrollY(positionInLinearLayout);
        }
    }

    private int getIntrinsicHeight(View view) {
        if (view instanceof ExpandableView) {
            return ((ExpandableView) view).getIntrinsicHeight();
        }
        return view.getHeight();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: com.android.systemui.statusbar.ExpandableNotificationRow */
    /* JADX DEBUG: Multi-variable search result rejected for r0v2, resolved type: com.android.systemui.statusbar.ExpandableNotificationRow */
    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: com.android.systemui.statusbar.ExpandableNotificationRow */
    /* JADX WARN: Multi-variable type inference failed */
    public int getPositionInLinearLayout(View view) {
        ExpandableNotificationRow expandableNotificationRow;
        float fInterpolate;
        float fInterpolate2;
        ExpandableNotificationRow expandableNotificationRow2 = null;
        if (isChildInGroup(view)) {
            expandableNotificationRow2 = (ExpandableNotificationRow) view;
            view = expandableNotificationRow2.getNotificationParent();
            expandableNotificationRow = view;
        } else {
            expandableNotificationRow = 0;
        }
        float f = this.mPaddingBetweenElements;
        float f2 = 0.0f;
        int intrinsicHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            boolean z = expandableView.getVisibility() != 8;
            if (z && !expandableView.hasNoContentHeight()) {
                float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
                if (increasedPaddingAmount < 0.0f) {
                    int iInterpolate = (int) NotificationUtils.interpolate(0.0f, this.mPaddingBetweenElements, 1.0f + increasedPaddingAmount);
                    float fInterpolate3 = f2 > 0.0f ? (int) NotificationUtils.interpolate(iInterpolate, this.mIncreasedPaddingBetweenElements, f2) : iInterpolate;
                    fInterpolate = iInterpolate;
                    fInterpolate2 = fInterpolate3;
                } else {
                    fInterpolate2 = (int) NotificationUtils.interpolate(f, this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
                    fInterpolate = (int) NotificationUtils.interpolate(this.mPaddingBetweenElements, this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
                }
                if (intrinsicHeight != 0) {
                    intrinsicHeight = (int) (intrinsicHeight + fInterpolate2);
                }
                f = fInterpolate;
                f2 = increasedPaddingAmount;
            }
            if (expandableView == view) {
                if (expandableNotificationRow != 0) {
                    return intrinsicHeight + expandableNotificationRow.getPositionOfChild(expandableNotificationRow2);
                }
                return intrinsicHeight;
            }
            if (z) {
                intrinsicHeight += getIntrinsicHeight(expandableView);
            }
        }
        return 0;
    }

    @Override // android.view.ViewGroup
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        onViewAddedInternal(view);
    }

    private void updateFirstAndLastBackgroundViews() {
        ActivatableNotificationView firstChildWithBackground = getFirstChildWithBackground();
        ActivatableNotificationView lastChildWithBackground = getLastChildWithBackground();
        if (this.mAnimationsEnabled && this.mIsExpanded) {
            this.mAnimateNextBackgroundTop = firstChildWithBackground != this.mFirstVisibleBackgroundChild;
            this.mAnimateNextBackgroundBottom = lastChildWithBackground != this.mLastVisibleBackgroundChild;
        } else {
            this.mAnimateNextBackgroundTop = false;
            this.mAnimateNextBackgroundBottom = false;
        }
        this.mFirstVisibleBackgroundChild = firstChildWithBackground;
        this.mLastVisibleBackgroundChild = lastChildWithBackground;
        this.mAmbientState.setLastVisibleBackgroundChild(lastChildWithBackground);
        this.mRoundnessManager.setFirstAndLastBackgroundChild(this.mFirstVisibleBackgroundChild, this.mLastVisibleBackgroundChild);
        invalidate();
    }

    private void onViewAddedInternal(View view) {
        updateHideSensitiveForChild(view);
        ((ExpandableView) view).setOnHeightChangedListener(this);
        generateAddAnimation(view, false);
        updateAnimationState(view);
        updateChronometerForChild(view);
    }

    private void updateHideSensitiveForChild(View view) {
        if (view instanceof ExpandableView) {
            ((ExpandableView) view).setHideSensitiveForIntrinsicHeight(this.mAmbientState.isHideSensitive());
        }
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void notifyGroupChildRemoved(View view, ViewGroup viewGroup) {
        onViewRemovedInternal(view, viewGroup);
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void notifyGroupChildAdded(View view) {
        onViewAddedInternal(view);
    }

    public void setAnimationsEnabled(boolean z) {
        this.mAnimationsEnabled = z;
        updateNotificationAnimationStates();
        if (!z) {
            this.mSwipedOutViews.clear();
            this.mChildrenToRemoveAnimated.clear();
            clearTemporaryViewsInGroup(this);
        }
    }

    private void updateNotificationAnimationStates() {
        boolean z = this.mAnimationsEnabled || hasPulsingNotifications();
        this.mShelf.setAnimationsEnabled(z);
        int childCount = getChildCount();
        boolean z2 = z;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            z2 &= this.mIsExpanded || isPinnedHeadsUp(childAt);
            updateAnimationState(z2, childAt);
        }
    }

    private void updateAnimationState(View view) {
        updateAnimationState((this.mAnimationsEnabled || hasPulsingNotifications()) && (this.mIsExpanded || isPinnedHeadsUp(view)), view);
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void setExpandingNotification(ExpandableNotificationRow expandableNotificationRow) {
        this.mAmbientState.setExpandingNotification(expandableNotificationRow);
        requestChildrenUpdate();
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void bindRow(final ExpandableNotificationRow expandableNotificationRow) {
        expandableNotificationRow.setHeadsUpAnimatingAwayListener(new Consumer() { // from class: com.android.systemui.statusbar.stack.-$$Lambda$NotificationStackScrollLayout$kf3mstlQ__SBMLTTAvwCr700C4g
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                NotificationStackScrollLayout.lambda$bindRow$1(this.f$0, expandableNotificationRow, (Boolean) obj);
            }
        });
    }

    public static /* synthetic */ void lambda$bindRow$1(NotificationStackScrollLayout notificationStackScrollLayout, ExpandableNotificationRow expandableNotificationRow, Boolean bool) {
        notificationStackScrollLayout.mRoundnessManager.onHeadsupAnimatingAwayChanged(expandableNotificationRow, bool.booleanValue());
        if (notificationStackScrollLayout.mHeadsUpAppearanceController != null) {
            notificationStackScrollLayout.mHeadsUpAppearanceController.updateHeader(expandableNotificationRow.getEntry());
        }
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void applyExpandAnimationParams(ActivityLaunchAnimator.ExpandAnimationParameters expandAnimationParameters) {
        this.mAmbientState.setExpandAnimationTopChange(expandAnimationParameters == null ? 0 : expandAnimationParameters.getTopChange());
        requestChildrenUpdate();
    }

    private void updateAnimationState(boolean z, View view) {
        if (view instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) view).setIconAnimationRunning(z);
        }
    }

    public boolean isAddOrRemoveAnimationPending() {
        return this.mNeedsAnimation && !(this.mChildrenToAddAnimated.isEmpty() && this.mChildrenToRemoveAnimated.isEmpty());
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void generateAddAnimation(View view, boolean z) {
        if (this.mIsExpanded && this.mAnimationsEnabled && !this.mChangePositionInProgress) {
            this.mChildrenToAddAnimated.add(view);
            if (z) {
                this.mFromMoreCardAdditions.add(view);
            }
            this.mNeedsAnimation = true;
        }
        if (isHeadsUp(view) && this.mAnimationsEnabled && !this.mChangePositionInProgress) {
            this.mAddedHeadsUpChildren.add(view);
            this.mChildrenToAddAnimated.remove(view);
        }
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void changeViewPosition(View view, int i) {
        int iIndexOfChild = indexOfChild(view);
        boolean z = false;
        if (iIndexOfChild == -1) {
            if ((view instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) view).getTransientContainer() != null) {
                z = true;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Attempting to re-position ");
            sb.append(z ? "transient" : "");
            sb.append(" view {");
            sb.append(view);
            sb.append("}");
            Log.e("StackScroller", sb.toString());
            return;
        }
        if (view != null && view.getParent() == this && iIndexOfChild != i) {
            this.mChangePositionInProgress = true;
            ExpandableView expandableView = (ExpandableView) view;
            expandableView.setChangingPosition(true);
            removeView(view);
            addView(view, i);
            expandableView.setChangingPosition(false);
            this.mChangePositionInProgress = false;
            if (this.mIsExpanded && this.mAnimationsEnabled && view.getVisibility() != 8) {
                this.mChildrenChangingPositions.add(view);
                this.mNeedsAnimation = true;
            }
        }
    }

    private void startAnimationToState() {
        if (this.mNeedsAnimation) {
            generateChildHierarchyEvents();
            this.mNeedsAnimation = false;
        }
        if (!this.mAnimationEvents.isEmpty() || isCurrentlyAnimating()) {
            setAnimationRunning(true);
            this.mStateAnimator.startAnimationForEvents(this.mAnimationEvents, this.mCurrentStackScrollState, this.mGoToFullShadeDelay);
            this.mAnimationEvents.clear();
            updateBackground();
            updateViewShadows();
            updateClippingToTopRoundedCorner();
        } else {
            applyCurrentState();
        }
        this.mGoToFullShadeDelay = 0L;
    }

    private void generateChildHierarchyEvents() {
        generateHeadsUpAnimationEvents();
        generateChildRemovalEvents();
        generateChildAdditionEvents();
        generatePositionChangeEvents();
        generateSnapBackEvents();
        generateDragEvents();
        generateTopPaddingEvent();
        generateActivateEvent();
        generateDimmedEvent();
        generateHideSensitiveEvent();
        generateDarkEvent();
        generateGoToFullShadeEvent();
        generateViewResizeEvent();
        generateGroupExpansionEvent();
        generateAnimateEverythingEvent();
        generatePulsingAnimationEvent();
        this.mNeedsAnimation = false;
    }

    private void generateHeadsUpAnimationEvents() {
        int i;
        Iterator<Pair<ExpandableNotificationRow, Boolean>> it = this.mHeadsUpChangeAnimations.iterator();
        while (it.hasNext()) {
            Pair<ExpandableNotificationRow, Boolean> next = it.next();
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) next.first;
            boolean zBooleanValue = ((Boolean) next.second).booleanValue();
            int i2 = 17;
            boolean z = false;
            boolean z2 = expandableNotificationRow.isPinned() && !this.mIsExpanded;
            if (!this.mIsExpanded && !zBooleanValue) {
                if (expandableNotificationRow.wasJustClicked()) {
                    i = 16;
                } else {
                    i = 15;
                }
                i2 = i;
                if (expandableNotificationRow.isChildInGroup()) {
                    expandableNotificationRow.setHeadsUpAnimatingAway(false);
                } else {
                    AnimationEvent animationEvent = new AnimationEvent(expandableNotificationRow, i2);
                    animationEvent.headsUpFromBottom = z;
                    this.mAnimationEvents.add(animationEvent);
                }
            } else {
                ExpandableViewState viewStateForView = this.mCurrentStackScrollState.getViewStateForView(expandableNotificationRow);
                if (viewStateForView != null) {
                    if (zBooleanValue && (this.mAddedHeadsUpChildren.contains(expandableNotificationRow) || z2)) {
                        i2 = (z2 || shouldHunAppearFromBottom(viewStateForView)) ? 14 : 0;
                        z = !z2;
                    }
                    AnimationEvent animationEvent2 = new AnimationEvent(expandableNotificationRow, i2);
                    animationEvent2.headsUpFromBottom = z;
                    this.mAnimationEvents.add(animationEvent2);
                }
            }
        }
        this.mHeadsUpChangeAnimations.clear();
        this.mAddedHeadsUpChildren.clear();
    }

    private boolean shouldHunAppearFromBottom(ExpandableViewState expandableViewState) {
        if (expandableViewState.yTranslation + expandableViewState.height < this.mAmbientState.getMaxHeadsUpTranslation()) {
            return false;
        }
        return true;
    }

    private void generateGroupExpansionEvent() {
        if (this.mExpandedGroupView != null) {
            this.mAnimationEvents.add(new AnimationEvent(this.mExpandedGroupView, 13));
            this.mExpandedGroupView = null;
        }
    }

    private void generateViewResizeEvent() {
        if (this.mNeedViewResizeAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 12));
        }
        this.mNeedViewResizeAnimation = false;
    }

    private void generateSnapBackEvents() {
        Iterator<View> it = this.mSnappedBackChildren.iterator();
        while (it.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent(it.next(), 5));
        }
        this.mSnappedBackChildren.clear();
    }

    private void generateDragEvents() {
        Iterator<View> it = this.mDragAnimPendingChildren.iterator();
        while (it.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent(it.next(), 4));
        }
        this.mDragAnimPendingChildren.clear();
    }

    private void generateChildRemovalEvents() {
        boolean z;
        ViewGroup transientContainer;
        Iterator<View> it = this.mChildrenToRemoveAnimated.iterator();
        while (it.hasNext()) {
            View next = it.next();
            boolean zContains = this.mSwipedOutViews.contains(next);
            float translationY = next.getTranslationY();
            int i = 1;
            if (next instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) next;
                if (expandableNotificationRow.isRemoved() && expandableNotificationRow.wasChildInGroupWhenRemoved()) {
                    translationY = expandableNotificationRow.getTranslationWhenRemoved();
                    z = false;
                } else {
                    z = true;
                }
                zContains |= Math.abs(expandableNotificationRow.getTranslation()) == ((float) expandableNotificationRow.getWidth());
            } else {
                z = true;
            }
            if (!zContains) {
                Rect clipBounds = next.getClipBounds();
                zContains = clipBounds != null && clipBounds.height() == 0;
                if (zContains && (next instanceof ExpandableView) && (transientContainer = ((ExpandableView) next).getTransientContainer()) != null) {
                    transientContainer.removeTransientView(next);
                }
            }
            if (zContains) {
                i = 2;
            }
            AnimationEvent animationEvent = new AnimationEvent(next, i);
            animationEvent.viewAfterChangingView = getFirstChildBelowTranlsationY(translationY, z);
            this.mAnimationEvents.add(animationEvent);
            this.mSwipedOutViews.remove(next);
        }
        this.mChildrenToRemoveAnimated.clear();
    }

    private void generatePositionChangeEvents() {
        Iterator<View> it = this.mChildrenChangingPositions.iterator();
        while (it.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent(it.next(), 8));
        }
        this.mChildrenChangingPositions.clear();
        if (this.mGenerateChildOrderChangedEvent) {
            this.mAnimationEvents.add(new AnimationEvent(null, 8));
            this.mGenerateChildOrderChangedEvent = false;
        }
    }

    private void generateChildAdditionEvents() {
        Iterator<View> it = this.mChildrenToAddAnimated.iterator();
        while (it.hasNext()) {
            View next = it.next();
            if (this.mFromMoreCardAdditions.contains(next)) {
                this.mAnimationEvents.add(new AnimationEvent(next, 0, 360L));
            } else {
                this.mAnimationEvents.add(new AnimationEvent(next, 0));
            }
        }
        this.mChildrenToAddAnimated.clear();
        this.mFromMoreCardAdditions.clear();
    }

    private void generateTopPaddingEvent() {
        AnimationEvent animationEvent;
        if (this.mTopPaddingNeedsAnimation) {
            if (this.mAmbientState.isDark()) {
                animationEvent = new AnimationEvent((View) null, 3, 550L);
            } else {
                animationEvent = new AnimationEvent(null, 3);
            }
            this.mAnimationEvents.add(animationEvent);
        }
        this.mTopPaddingNeedsAnimation = false;
    }

    private void generateActivateEvent() {
        if (this.mActivateNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 6));
        }
        this.mActivateNeedsAnimation = false;
    }

    private void generateAnimateEverythingEvent() {
        if (this.mEverythingNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 18));
        }
        this.mEverythingNeedsAnimation = false;
    }

    private void generateDimmedEvent() {
        if (this.mDimmedNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 7));
        }
        this.mDimmedNeedsAnimation = false;
    }

    private void generateHideSensitiveEvent() {
        if (this.mHideSensitiveNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 11));
        }
        this.mHideSensitiveNeedsAnimation = false;
    }

    private void generateDarkEvent() {
        if (this.mDarkNeedsAnimation) {
            AnimationEvent animationEvent = new AnimationEvent((View) null, 9, new AnimationFilter().animateDark().animateY(this.mShelf));
            animationEvent.darkAnimationOriginIndex = this.mDarkAnimationOriginIndex;
            this.mAnimationEvents.add(animationEvent);
            startDarkAmountAnimation();
        }
        this.mDarkNeedsAnimation = false;
    }

    private void generateGoToFullShadeEvent() {
        if (this.mGoToFullShadeNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 10));
        }
        this.mGoToFullShadeNeedsAnimation = false;
    }

    private boolean onInterceptTouchEventScroll(MotionEvent motionEvent) {
        if (!isScrollingEnabled()) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action == 2 && this.mIsBeingDragged) {
            return true;
        }
        int i = action & 255;
        if (i != 6) {
            switch (i) {
                case 0:
                    int y = (int) motionEvent.getY();
                    this.mScrolledToTopOnFirstDown = isScrolledToTop();
                    if (getChildAtPosition(motionEvent.getX(), y, false) == null) {
                        setIsBeingDragged(false);
                        recycleVelocityTracker();
                        break;
                    } else {
                        this.mLastMotionY = y;
                        this.mDownX = (int) motionEvent.getX();
                        this.mActivePointerId = motionEvent.getPointerId(0);
                        initOrResetVelocityTracker();
                        this.mVelocityTracker.addMovement(motionEvent);
                        setIsBeingDragged(!this.mScroller.isFinished());
                        break;
                    }
                case 1:
                case 3:
                    setIsBeingDragged(false);
                    this.mActivePointerId = -1;
                    recycleVelocityTracker();
                    if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                        animateScroll();
                        break;
                    }
                    break;
                case 2:
                    int i2 = this.mActivePointerId;
                    if (i2 != -1) {
                        int iFindPointerIndex = motionEvent.findPointerIndex(i2);
                        if (iFindPointerIndex == -1) {
                            Log.e("StackScroller", "Invalid pointerId=" + i2 + " in onInterceptTouchEvent");
                            break;
                        } else {
                            int y2 = (int) motionEvent.getY(iFindPointerIndex);
                            int x = (int) motionEvent.getX(iFindPointerIndex);
                            int iAbs = Math.abs(y2 - this.mLastMotionY);
                            int iAbs2 = Math.abs(x - this.mDownX);
                            if (iAbs > this.mTouchSlop && iAbs > iAbs2) {
                                setIsBeingDragged(true);
                                this.mLastMotionY = y2;
                                this.mDownX = x;
                                initVelocityTrackerIfNotExists();
                                this.mVelocityTracker.addMovement(motionEvent);
                                break;
                            }
                        }
                    }
                    break;
            }
        } else {
            onSecondaryPointerUp(motionEvent);
        }
        return this.mIsBeingDragged;
    }

    protected StackScrollAlgorithm createStackScrollAlgorithm(Context context) {
        return new StackScrollAlgorithm(context);
    }

    private boolean isInContentBounds(MotionEvent motionEvent) {
        return isInContentBounds(motionEvent.getY());
    }

    public boolean isInContentBounds(float f) {
        return f < ((float) (getHeight() - getEmptyBottomMargin()));
    }

    private void setIsBeingDragged(boolean z) {
        this.mIsBeingDragged = z;
        if (z) {
            requestDisallowInterceptTouchEvent(true);
            cancelLongPress();
        }
    }

    @Override // android.view.View
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (!z) {
            cancelLongPress();
        }
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void clearChildFocus(View view) {
        super.clearChildFocus(view);
        if (this.mForcedScroll == view) {
            this.mForcedScroll = null;
        }
    }

    public void requestDisallowLongPress() {
        cancelLongPress();
    }

    public void requestDisallowDismiss() {
        this.mDisallowDismissInThisMotion = true;
    }

    @Override // android.view.View
    public void cancelLongPress() {
        this.mSwipeHelper.cancelLongPress();
    }

    @Override // com.android.systemui.statusbar.policy.ScrollAdapter
    public boolean isScrolledToTop() {
        return this.mOwnScrollY == 0;
    }

    public boolean isScrolledToBottom() {
        return this.mOwnScrollY >= getScrollRange();
    }

    @Override // com.android.systemui.statusbar.policy.ScrollAdapter
    public View getHostView() {
        return this;
    }

    public int getEmptyBottomMargin() {
        return Math.max(this.mMaxLayoutHeight - this.mContentHeight, 0);
    }

    public void checkSnoozeLeavebehind() {
        if (this.mCheckForLeavebehind) {
            this.mStatusBar.getGutsManager().closeAndSaveGuts(true, false, false, -1, -1, false);
            this.mCheckForLeavebehind = false;
        }
    }

    public void resetCheckSnoozeLeavebehind() {
        this.mCheckForLeavebehind = true;
    }

    public void onExpansionStarted() {
        this.mIsExpansionChanging = true;
        this.mAmbientState.setExpansionChanging(true);
        checkSnoozeLeavebehind();
    }

    public void onExpansionStopped() {
        this.mIsExpansionChanging = false;
        resetCheckSnoozeLeavebehind();
        this.mAmbientState.setExpansionChanging(false);
        if (!this.mIsExpanded) {
            setOwnScrollY(0);
            this.mStatusBar.resetUserExpandedStates();
            clearTemporaryViews();
            clearUserLockedViews();
        }
    }

    private void clearUserLockedViews() {
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) expandableView).setUserLocked(false);
            }
        }
    }

    private void clearTemporaryViews() {
        clearTemporaryViewsInGroup(this);
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView instanceof ExpandableNotificationRow) {
                clearTemporaryViewsInGroup(((ExpandableNotificationRow) expandableView).getChildrenContainer());
            }
        }
    }

    private void clearTemporaryViewsInGroup(ViewGroup viewGroup) {
        while (viewGroup != null && viewGroup.getTransientViewCount() != 0) {
            viewGroup.removeTransientView(viewGroup.getTransientView(0));
        }
    }

    public void onPanelTrackingStarted() {
        this.mPanelTracking = true;
        this.mAmbientState.setPanelTracking(true);
    }

    public void onPanelTrackingStopped() {
        this.mPanelTracking = false;
        this.mAmbientState.setPanelTracking(false);
    }

    public void resetScrollPosition() {
        this.mScroller.abortAnimation();
        setOwnScrollY(0);
    }

    private void setIsExpanded(boolean z) {
        boolean z2 = z != this.mIsExpanded;
        this.mIsExpanded = z;
        this.mStackScrollAlgorithm.setIsExpanded(z);
        if (z2) {
            if (!this.mIsExpanded) {
                this.mGroupManager.collapseAllGroups();
                this.mExpandHelper.cancelImmediately();
            }
            updateNotificationAnimationStates();
            updateChronometers();
            requestChildrenUpdate();
        }
    }

    private void updateChronometers() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            updateChronometerForChild(getChildAt(i));
        }
    }

    private void updateChronometerForChild(View view) {
        if (view instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) view).setChronometerRunning(this.mIsExpanded);
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView.OnHeightChangedListener
    public void onHeightChanged(ExpandableView expandableView, boolean z) {
        ExpandableNotificationRow expandableNotificationRow;
        updateContentHeight();
        updateScrollPositionOnExpandInBottom(expandableView);
        clampScrollPosition();
        notifyHeightChangeListener(expandableView, z);
        if (expandableView instanceof ExpandableNotificationRow) {
            expandableNotificationRow = (ExpandableNotificationRow) expandableView;
        } else {
            expandableNotificationRow = null;
        }
        if (expandableNotificationRow != null && (expandableNotificationRow == this.mFirstVisibleBackgroundChild || expandableNotificationRow.getNotificationParent() == this.mFirstVisibleBackgroundChild)) {
            updateAlgorithmLayoutMinHeight();
        }
        if (z) {
            requestAnimationOnViewResize(expandableNotificationRow);
        }
        requestChildrenUpdate();
    }

    @Override // com.android.systemui.statusbar.ExpandableView.OnHeightChangedListener
    public void onReset(ExpandableView expandableView) {
        updateAnimationState(expandableView);
        updateChronometerForChild(expandableView);
    }

    private void updateScrollPositionOnExpandInBottom(ExpandableView expandableView) {
        if ((expandableView instanceof ExpandableNotificationRow) && !onKeyguard()) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
            if (!expandableNotificationRow.isUserLocked() || expandableNotificationRow == getFirstChildNotGone() || expandableNotificationRow.isSummaryWithChildren()) {
                return;
            }
            float translationY = expandableNotificationRow.getTranslationY() + expandableNotificationRow.getActualHeight();
            if (expandableNotificationRow.isChildInGroup()) {
                translationY += expandableNotificationRow.getNotificationParent().getTranslationY();
            }
            int intrinsicHeight = this.mMaxLayoutHeight + ((int) this.mStackTranslation);
            if (expandableNotificationRow != this.mLastVisibleBackgroundChild && this.mShelf.getVisibility() != 8) {
                intrinsicHeight -= this.mShelf.getIntrinsicHeight() + this.mPaddingBetweenElements;
            }
            float f = intrinsicHeight;
            if (translationY > f) {
                setOwnScrollY((int) ((this.mOwnScrollY + translationY) - f));
                this.mDisallowScrollingInThisMotion = true;
            }
        }
    }

    public void setOnHeightChangedListener(ExpandableView.OnHeightChangedListener onHeightChangedListener) {
        this.mOnHeightChangedListener = onHeightChangedListener;
    }

    public void setOnEmptySpaceClickListener(OnEmptySpaceClickListener onEmptySpaceClickListener) {
        this.mOnEmptySpaceClickListener = onEmptySpaceClickListener;
    }

    public void onChildAnimationFinished() {
        setAnimationRunning(false);
        requestChildrenUpdate();
        runAnimationFinishedRunnables();
        clearTransient();
        clearHeadsUpDisappearRunning();
    }

    private void clearHeadsUpDisappearRunning() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) childAt;
                expandableNotificationRow.setHeadsUpAnimatingAway(false);
                if (expandableNotificationRow.isSummaryWithChildren()) {
                    Iterator<ExpandableNotificationRow> it = expandableNotificationRow.getNotificationChildren().iterator();
                    while (it.hasNext()) {
                        it.next().setHeadsUpAnimatingAway(false);
                    }
                }
            }
        }
    }

    private void clearTransient() {
        Iterator<ExpandableView> it = this.mClearTransientViewsWhenFinished.iterator();
        while (it.hasNext()) {
            StackStateAnimator.removeTransientView(it.next());
        }
        this.mClearTransientViewsWhenFinished.clear();
    }

    private void runAnimationFinishedRunnables() {
        Iterator<Runnable> it = this.mAnimationFinishedRunnables.iterator();
        while (it.hasNext()) {
            it.next().run();
        }
        this.mAnimationFinishedRunnables.clear();
    }

    public void setDimmed(boolean z, boolean z2) {
        boolean zOnKeyguard = z & onKeyguard();
        this.mAmbientState.setDimmed(zOnKeyguard);
        if (z2 && this.mAnimationsEnabled) {
            this.mDimmedNeedsAnimation = true;
            this.mNeedsAnimation = true;
            animateDimmed(zOnKeyguard);
        } else {
            setDimAmount(zOnKeyguard ? 1.0f : 0.0f);
        }
        requestChildrenUpdate();
    }

    boolean isDimmed() {
        return this.mAmbientState.isDimmed();
    }

    private void setDimAmount(float f) {
        this.mDimAmount = f;
        updateBackgroundDimming();
    }

    private void animateDimmed(boolean z) {
        if (this.mDimAnimator != null) {
            this.mDimAnimator.cancel();
        }
        float f = z ? 1.0f : 0.0f;
        if (f == this.mDimAmount) {
            return;
        }
        this.mDimAnimator = TimeAnimator.ofFloat(this.mDimAmount, f);
        this.mDimAnimator.setDuration(220L);
        this.mDimAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        this.mDimAnimator.addListener(this.mDimEndListener);
        this.mDimAnimator.addUpdateListener(this.mDimUpdateListener);
        this.mDimAnimator.start();
    }

    public void setHideSensitive(boolean z, boolean z2) {
        if (z != this.mAmbientState.isHideSensitive()) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((ExpandableView) getChildAt(i)).setHideSensitiveForIntrinsicHeight(z);
            }
            this.mAmbientState.setHideSensitive(z);
            if (z2 && this.mAnimationsEnabled) {
                this.mHideSensitiveNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            updateContentHeight();
            requestChildrenUpdate();
        }
    }

    public void setActivatedChild(ActivatableNotificationView activatableNotificationView) {
        this.mAmbientState.setActivatedChild(activatableNotificationView);
        if (this.mAnimationsEnabled) {
            this.mActivateNeedsAnimation = true;
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public ActivatableNotificationView getActivatedChild() {
        return this.mAmbientState.getActivatedChild();
    }

    private void applyCurrentState() {
        this.mCurrentStackScrollState.apply();
        if (this.mListener != null) {
            this.mListener.onChildLocationsChanged();
        }
        runAnimationFinishedRunnables();
        setAnimationRunning(false);
        updateBackground();
        updateViewShadows();
        updateClippingToTopRoundedCorner();
    }

    private void updateViewShadows() {
        float translationZ;
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                this.mTmpSortedChildren.add(expandableView);
            }
        }
        Collections.sort(this.mTmpSortedChildren, this.mViewPositionComparator);
        ExpandableView expandableView2 = null;
        int i2 = 0;
        while (i2 < this.mTmpSortedChildren.size()) {
            ExpandableView expandableView3 = this.mTmpSortedChildren.get(i2);
            float translationZ2 = expandableView3.getTranslationZ();
            if (expandableView2 != null) {
                translationZ = expandableView2.getTranslationZ();
            } else {
                translationZ = translationZ2;
            }
            float f = translationZ - translationZ2;
            if (f <= 0.0f || f >= 0.1f) {
                expandableView3.setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
            } else {
                expandableView3.setFakeShadowIntensity(f / 0.1f, expandableView2.getOutlineAlpha(), (int) (((expandableView2.getTranslationY() + expandableView2.getActualHeight()) - expandableView3.getTranslationY()) - expandableView2.getExtraBottomPadding()), expandableView2.getOutlineTranslation());
            }
            i2++;
            expandableView2 = expandableView3;
        }
        this.mTmpSortedChildren.clear();
    }

    public void updateDecorViews(boolean z) {
        if (z == this.mUsingLightTheme) {
            return;
        }
        this.mUsingLightTheme = z;
        int colorAttr = Utils.getColorAttr(new ContextThemeWrapper(this.mContext, z ? 2131886646 : 2131886642), R.attr.wallpaperTextColor);
        this.mFooterView.setTextColor(colorAttr);
        this.mEmptyShadeView.setTextColor(colorAttr);
    }

    public void goToFullShade(long j) {
        this.mGoToFullShadeNeedsAnimation = true;
        this.mGoToFullShadeDelay = j;
        this.mNeedsAnimation = true;
        requestChildrenUpdate();
    }

    public void cancelExpandHelper() {
        this.mExpandHelper.cancel();
    }

    public void setIntrinsicPadding(int i) {
        this.mIntrinsicPadding = i;
        this.mAmbientState.setIntrinsicPadding(i);
    }

    public int getIntrinsicPadding() {
        return this.mIntrinsicPadding;
    }

    public float getNotificationsTopY() {
        return this.mTopPadding + getStackTranslation();
    }

    @Override // android.view.ViewGroup
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public void setDark(boolean z, boolean z2, PointF pointF) {
        if (this.mAmbientState.isDark() == z) {
            return;
        }
        this.mAmbientState.setDark(z);
        if (z2 && this.mAnimationsEnabled) {
            this.mDarkNeedsAnimation = true;
            this.mDarkAnimationOriginIndex = findDarkAnimationOriginIndex(pointF);
            this.mNeedsAnimation = true;
        } else {
            if (this.mDarkAmountAnimator != null) {
                this.mDarkAmountAnimator.cancel();
            }
            setDarkAmount(z ? 1.0f : 0.0f);
            updateBackground();
        }
        requestChildrenUpdate();
        applyCurrentBackgroundBounds();
        updateWillNotDraw();
        notifyHeightChangeListener(this.mShelf);
    }

    private void updateAntiBurnInTranslation() {
        setTranslationX(this.mAntiBurnInOffsetX * this.mDarkAmount);
    }

    private void updateWillNotDraw() {
        setWillNotDraw(!(this.mShouldDrawNotificationBackground));
    }

    private void setDarkAmount(float f) {
        this.mDarkAmount = f;
        boolean zIsFullyDark = this.mAmbientState.isFullyDark();
        this.mAmbientState.setDarkAmount(f);
        if (this.mAmbientState.isFullyDark() != zIsFullyDark) {
            updateContentHeight();
            DozeParameters dozeParameters = DozeParameters.getInstance(this.mContext);
            if (this.mAmbientState.isFullyDark() && dozeParameters.shouldControlScreenOff()) {
                this.mShelf.fadeInTranslating();
            }
        }
        updateAlgorithmHeightAndPadding();
        updateBackgroundDimming();
        updateAntiBurnInTranslation();
        requestChildrenUpdate();
    }

    public float getDarkAmount() {
        return this.mDarkAmount;
    }

    private void startDarkAmountAnimation() {
        Property<NotificationStackScrollLayout, Float> property = DARK_AMOUNT;
        float[] fArr = new float[2];
        fArr[0] = this.mDarkAmount;
        fArr[1] = this.mAmbientState.isDark() ? 1.0f : 0.0f;
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, property, fArr);
        objectAnimatorOfFloat.setDuration(500L);
        objectAnimatorOfFloat.setInterpolator(Interpolators.ALPHA_IN);
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.11
            AnonymousClass11() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                NotificationStackScrollLayout.this.mDarkAmountAnimator = null;
            }
        });
        if (this.mDarkAmountAnimator != null) {
            this.mDarkAmountAnimator.cancel();
        }
        this.mDarkAmountAnimator = objectAnimatorOfFloat;
        this.mDarkAmountAnimator.start();
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$11 */
    class AnonymousClass11 extends AnimatorListenerAdapter {
        AnonymousClass11() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            NotificationStackScrollLayout.this.mDarkAmountAnimator = null;
        }
    }

    private int findDarkAnimationOriginIndex(PointF pointF) {
        if (pointF == null || pointF.y < this.mTopPadding) {
            return -1;
        }
        if (pointF.y > getBottomMostNotificationBottom()) {
            return -2;
        }
        ExpandableView closestChildAtRawPosition = getClosestChildAtRawPosition(pointF.x, pointF.y);
        if (closestChildAtRawPosition == null) {
            return -1;
        }
        return getNotGoneIndex(closestChildAtRawPosition);
    }

    private int getNotGoneIndex(View view) {
        int childCount = getChildCount();
        int i = 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = getChildAt(i2);
            if (view == childAt) {
                return i;
            }
            if (childAt.getVisibility() != 8) {
                i++;
            }
        }
        return -1;
    }

    public void setFooterView(FooterView footerView) {
        int iIndexOfChild;
        if (this.mFooterView != null) {
            iIndexOfChild = indexOfChild(this.mFooterView);
            removeView(this.mFooterView);
        } else {
            iIndexOfChild = -1;
        }
        this.mFooterView = footerView;
        addView(this.mFooterView, iIndexOfChild);
    }

    public void setEmptyShadeView(EmptyShadeView emptyShadeView) {
        int iIndexOfChild;
        if (this.mEmptyShadeView != null) {
            iIndexOfChild = indexOfChild(this.mEmptyShadeView);
            removeView(this.mEmptyShadeView);
        } else {
            iIndexOfChild = -1;
        }
        this.mEmptyShadeView = emptyShadeView;
        addView(this.mEmptyShadeView, iIndexOfChild);
    }

    public void updateEmptyShadeView(boolean z) {
        this.mEmptyShadeView.setVisible(z, this.mIsExpanded && this.mAnimationsEnabled);
        int textResource = this.mEmptyShadeView.getTextResource();
        int i = this.mStatusBar.areNotificationsHidden() ? R.string.dnd_suppressing_shade_text : R.string.empty_shade_text;
        if (textResource != i) {
            this.mEmptyShadeView.setText(i);
        }
    }

    public void updateFooterView(boolean z, boolean z2) {
        if (this.mFooterView == null) {
            return;
        }
        boolean z3 = this.mIsExpanded && this.mAnimationsEnabled;
        this.mFooterView.setVisible(z, z3);
        this.mFooterView.setSecondaryVisible(z2, z3);
    }

    public void setDismissAllInProgress(boolean z) {
        this.mDismissAllInProgress = z;
        this.mAmbientState.setDismissAllInProgress(z);
        handleDismissAllClipping();
    }

    private void handleDismissAllClipping() {
        int childCount = getChildCount();
        boolean zCanChildBeDismissed = false;
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                if (this.mDismissAllInProgress && zCanChildBeDismissed) {
                    expandableView.setMinClipTopAmount(expandableView.getClipTopAmount());
                } else {
                    expandableView.setMinClipTopAmount(0);
                }
                zCanChildBeDismissed = canChildBeDismissed(expandableView);
            }
        }
    }

    public boolean isFooterViewNotGone() {
        return (this.mFooterView == null || this.mFooterView.getVisibility() == 8 || this.mFooterView.willBeGone()) ? false : true;
    }

    public boolean isFooterViewContentVisible() {
        return this.mFooterView != null && this.mFooterView.isContentVisible();
    }

    public int getFooterViewHeight() {
        if (this.mFooterView == null) {
            return 0;
        }
        return this.mFooterView.getHeight() + this.mPaddingBetweenElements;
    }

    public int getEmptyShadeViewHeight() {
        return this.mEmptyShadeView.getHeight();
    }

    public float getBottomMostNotificationBottom() {
        int childCount = getChildCount();
        float f = 0.0f;
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                float translationY = (expandableView.getTranslationY() + expandableView.getActualHeight()) - expandableView.getClipBottomAmount();
                if (translationY > f) {
                    f = translationY;
                }
            }
        }
        return f + getStackTranslation();
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }

    public void setGroupManager(NotificationGroupManager notificationGroupManager) {
        this.mGroupManager = notificationGroupManager;
    }

    public void onGoToKeyguard() {
        requestAnimateEverything();
    }

    private void requestAnimateEverything() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mEverythingNeedsAnimation = true;
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    public boolean isBelowLastNotification(float f, float f2) {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            ExpandableView expandableView = (ExpandableView) getChildAt(childCount);
            if (expandableView.getVisibility() != 8) {
                float y = expandableView.getY();
                if (y > f2) {
                    return false;
                }
                boolean z = f2 > (((float) expandableView.getActualHeight()) + y) - ((float) expandableView.getClipBottomAmount());
                if (expandableView == this.mFooterView) {
                    if (!z && !this.mFooterView.isOnEmptySpace(f - this.mFooterView.getX(), f2 - y)) {
                        return false;
                    }
                } else {
                    if (expandableView == this.mEmptyShadeView) {
                        return true;
                    }
                    if (!z) {
                        return false;
                    }
                }
            }
        }
        return f2 > ((float) this.mTopPadding) + this.mStackTranslation;
    }

    @Override // com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener
    public void onGroupExpansionChanged(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        boolean z2 = !this.mGroupExpandedForMeasure && this.mAnimationsEnabled && (this.mIsExpanded || expandableNotificationRow.isPinned());
        if (z2) {
            this.mExpandedGroupView = expandableNotificationRow;
            this.mNeedsAnimation = true;
        }
        expandableNotificationRow.setChildrenExpanded(z, z2);
        if (!this.mGroupExpandedForMeasure) {
            onHeightChanged(expandableNotificationRow, false);
        }
        runAfterAnimationFinished(new Runnable() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.12
            final /* synthetic */ ExpandableNotificationRow val$changedRow;

            AnonymousClass12(ExpandableNotificationRow expandableNotificationRow2) {
                expandableNotificationRow = expandableNotificationRow2;
            }

            @Override // java.lang.Runnable
            public void run() {
                expandableNotificationRow.onFinishedExpansionChange();
            }
        });
    }

    /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$12 */
    class AnonymousClass12 implements Runnable {
        final /* synthetic */ ExpandableNotificationRow val$changedRow;

        AnonymousClass12(ExpandableNotificationRow expandableNotificationRow2) {
            expandableNotificationRow = expandableNotificationRow2;
        }

        @Override // java.lang.Runnable
        public void run() {
            expandableNotificationRow.onFinishedExpansionChange();
        }
    }

    @Override // com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener
    public void onGroupCreatedFromChildren(NotificationGroupManager.NotificationGroup notificationGroup) {
        this.mStatusBar.requestNotificationUpdate();
    }

    public void onInitializeAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEventInternal(accessibilityEvent);
        accessibilityEvent.setScrollable(this.mScrollable);
        accessibilityEvent.setScrollX(this.mScrollX);
        accessibilityEvent.setScrollY(this.mOwnScrollY);
        accessibilityEvent.setMaxScrollX(this.mScrollX);
        accessibilityEvent.setMaxScrollY(getScrollRange());
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        if (this.mScrollable) {
            accessibilityNodeInfo.setScrollable(true);
            if (this.mBackwardScrollable) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP);
            }
            if (this.mForwardScrollable) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN);
            }
        }
        accessibilityNodeInfo.setClassName(ScrollView.class.getName());
    }

    /* JADX WARN: Code restructure failed: missing block: B:38:0x0021, code lost:
    
        if (r6 != 16908346) goto L45;
     */
    /* JADX WARN: Removed duplicated region for block: B:43:0x004e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        int iMax;
        if (super.performAccessibilityActionInternal(i, bundle)) {
            return true;
        }
        if (!isEnabled()) {
            return false;
        }
        int i2 = -1;
        if (i == 4096) {
            i2 = 1;
            iMax = Math.max(0, Math.min(this.mOwnScrollY + (i2 * ((((getHeight() - this.mPaddingBottom) - this.mTopPadding) - this.mPaddingTop) - this.mShelf.getIntrinsicHeight())), getScrollRange()));
            if (iMax != this.mOwnScrollY) {
                this.mScroller.startScroll(this.mScrollX, this.mOwnScrollY, 0, iMax - this.mOwnScrollY);
                animateScroll();
                return true;
            }
        } else if (i == 8192 || i == 16908344) {
            iMax = Math.max(0, Math.min(this.mOwnScrollY + (i2 * ((((getHeight() - this.mPaddingBottom) - this.mTopPadding) - this.mPaddingTop) - this.mShelf.getIntrinsicHeight())), getScrollRange()));
            if (iMax != this.mOwnScrollY) {
            }
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener
    public void onGroupsChanged() {
        this.mStatusBar.requestNotificationUpdate();
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void generateChildOrderChangedEvent() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mGenerateChildOrderChangedEvent = true;
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public int getContainerChildCount() {
        return getChildCount();
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public View getContainerChildAt(int i) {
        return getChildAt(i);
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void removeContainerView(View view) {
        removeView(view);
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void addContainerView(View view) {
        addView(view);
    }

    public void runAfterAnimationFinished(Runnable runnable) {
        this.mAnimationFinishedRunnables.add(runnable);
    }

    public void setHeadsUpManager(HeadsUpManagerPhone headsUpManagerPhone) {
        this.mHeadsUpManager = headsUpManagerPhone;
        this.mAmbientState.setHeadsUpManager(headsUpManagerPhone);
        this.mHeadsUpManager.addListener(this.mRoundnessManager);
    }

    public void generateHeadsUpAnimation(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        if (this.mAnimationsEnabled) {
            if (z || this.mHeadsUpGoingAwayAnimationsAllowed) {
                this.mHeadsUpChangeAnimations.add(new Pair<>(expandableNotificationRow, Boolean.valueOf(z)));
                this.mNeedsAnimation = true;
                if (!this.mIsExpanded && !z) {
                    expandableNotificationRow.setHeadsUpAnimatingAway(true);
                }
                requestChildrenUpdate();
            }
        }
    }

    public void setShadeExpanded(boolean z) {
        this.mAmbientState.setShadeExpanded(z);
        this.mStateAnimator.setShadeExpanded(z);
    }

    public void setHeadsUpBoundaries(int i, int i2) {
        this.mAmbientState.setMaxHeadsUpTranslation(i - i2);
        this.mStateAnimator.setHeadsUpAppearHeightBottom(i);
        requestChildrenUpdate();
    }

    public void setTrackingHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        this.mTrackingHeadsUp = expandableNotificationRow != null;
        this.mRoundnessManager.setTrackingHeadsUp(expandableNotificationRow);
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
        this.mScrimController.setScrimBehindChangeRunnable(new Runnable() { // from class: com.android.systemui.statusbar.stack.-$$Lambda$NotificationStackScrollLayout$3VfNfgMGO3-TgMmei2bv0geU0dY
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.updateBackgroundDimming();
            }
        });
    }

    public void forceNoOverlappingRendering(boolean z) {
        this.mForceNoOverlappingRendering = z;
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return !this.mForceNoOverlappingRendering && super.hasOverlappingRendering();
    }

    public void setAnimationRunning(boolean z) {
        if (z != this.mAnimationRunning) {
            if (z) {
                getViewTreeObserver().addOnPreDrawListener(this.mRunningAnimationUpdater);
            } else {
                getViewTreeObserver().removeOnPreDrawListener(this.mRunningAnimationUpdater);
            }
            this.mAnimationRunning = z;
            updateContinuousShadowDrawing();
        }
    }

    public boolean isExpanded() {
        return this.mIsExpanded;
    }

    public void setPulsing(boolean z, boolean z2) {
        if (!this.mPulsing && !z) {
            return;
        }
        this.mPulsing = z;
        this.mNeedingPulseAnimation = z2 ? getFirstChildNotGone() : null;
        this.mAmbientState.setPulsing(z);
        updateNotificationAnimationStates();
        updateAlgorithmHeightAndPadding();
        updateContentHeight();
        requestChildrenUpdate();
        notifyHeightChangeListener(null, z2);
        this.mNeedsAnimation |= z2;
    }

    private void generatePulsingAnimationEvent() {
        if (this.mNeedingPulseAnimation != null) {
            this.mAnimationEvents.add(new AnimationEvent(this.mNeedingPulseAnimation, this.mPulsing ? 19 : 20));
            this.mNeedingPulseAnimation = null;
        }
    }

    public void setFadingOut(boolean z) {
        if (z != this.mFadingOut) {
            this.mFadingOut = z;
            updateFadingState();
        }
    }

    public void setParentNotFullyVisible(boolean z) {
        if (this.mScrimController != null && z != this.mParentNotFullyVisible) {
            this.mParentNotFullyVisible = z;
            updateFadingState();
        }
    }

    private void updateFadingState() {
        applyCurrentBackgroundBounds();
        updateSrcDrawing();
    }

    @Override // android.view.View
    public void setAlpha(float f) {
        super.setAlpha(f);
        setFadingOut(f != 1.0f);
    }

    public void setQsExpanded(boolean z) {
        this.mQsExpanded = z;
        updateAlgorithmLayoutMinHeight();
        updateScrollability();
    }

    public void setQsExpansionFraction(float f) {
        this.mQsExpansionFraction = f;
    }

    public void setOwnScrollY(int i) {
        if (i != this.mOwnScrollY) {
            onScrollChanged(this.mScrollX, i, this.mScrollX, this.mOwnScrollY);
            this.mOwnScrollY = i;
            updateForwardAndBackwardScrollability();
            requestChildrenUpdate();
        }
    }

    public void setShelf(NotificationShelf notificationShelf) {
        int iIndexOfChild;
        if (this.mShelf != null) {
            iIndexOfChild = indexOfChild(this.mShelf);
            removeView(this.mShelf);
        } else {
            iIndexOfChild = -1;
        }
        this.mShelf = notificationShelf;
        addView(this.mShelf, iIndexOfChild);
        this.mAmbientState.setShelf(notificationShelf);
        this.mStateAnimator.setShelf(notificationShelf);
        notificationShelf.bind(this.mAmbientState, this);
    }

    public NotificationShelf getNotificationShelf() {
        return this.mShelf;
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void setMaxDisplayedNotifications(int i) {
        if (this.mMaxDisplayedNotifications != i) {
            this.mMaxDisplayedNotifications = i;
            updateContentHeight();
            notifyHeightChangeListener(this.mShelf);
        }
    }

    public void setShouldShowShelfOnly(boolean z) {
        this.mShouldShowShelfOnly = z;
        updateAlgorithmLayoutMinHeight();
    }

    public int getMinExpansionHeight() {
        return this.mShelf.getIntrinsicHeight() - ((this.mShelf.getIntrinsicHeight() - this.mStatusBarHeight) / 2);
    }

    public void setInHeadsUpPinnedMode(boolean z) {
        this.mInHeadsUpPinnedMode = z;
        updateClipping();
    }

    public void setHeadsUpAnimatingAway(boolean z) {
        this.mHeadsUpAnimatingAway = z;
        updateClipping();
    }

    public void setStatusBarState(int i) {
        this.mStatusBarState = i;
        this.mAmbientState.setStatusBarState(i);
    }

    public void setExpandingVelocity(float f) {
        this.mAmbientState.setExpandingVelocity(f);
    }

    public float getOpeningHeight() {
        if (this.mEmptyShadeView.getVisibility() == 8) {
            return getMinExpansionHeight();
        }
        return getAppearEndPosition();
    }

    public void setIsFullWidth(boolean z) {
        this.mAmbientState.setPanelFullWidth(z);
    }

    public void setUnlockHintRunning(boolean z) {
        this.mAmbientState.setUnlockHintRunning(z);
    }

    public void setQsCustomizerShowing(boolean z) {
        this.mAmbientState.setQsCustomizerShowing(z);
        requestChildrenUpdate();
    }

    public void setHeadsUpGoingAwayAnimationsAllowed(boolean z) {
        this.mHeadsUpGoingAwayAnimationsAllowed = z;
    }

    public void setAntiBurnInOffsetX(int i) {
        this.mAntiBurnInOffsetX = i;
        updateAntiBurnInTranslation();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        Object[] objArr = new Object[9];
        objArr[0] = getClass().getSimpleName();
        objArr[1] = this.mPulsing ? "T" : "f";
        objArr[2] = this.mAmbientState.isQsCustomizerShowing() ? "T" : "f";
        if (getVisibility() == 0) {
            str = "visible";
        } else {
            str = getVisibility() == 8 ? "gone" : "invisible";
        }
        objArr[3] = str;
        objArr[4] = Float.valueOf(getAlpha());
        objArr[5] = Integer.valueOf(this.mAmbientState.getScrollY());
        objArr[6] = Integer.valueOf(this.mMaxTopPadding);
        objArr[7] = this.mShouldShowShelfOnly ? "T" : "f";
        objArr[8] = Float.valueOf(this.mQsExpansionFraction);
        printWriter.println(String.format("[%s: pulsing=%s qsCustomizerShowing=%s visibility=%s alpha:%f scrollY:%d maxTopPadding:%d showShelfOnly=%s qsExpandFraction=%f]", objArr));
    }

    public boolean isFullyDark() {
        return this.mAmbientState.isFullyDark();
    }

    public void addOnExpandedHeightListener(BiConsumer<Float, Float> biConsumer) {
        this.mExpandedHeightListeners.add(biConsumer);
    }

    public void removeOnExpandedHeightListener(BiConsumer<Float, Float> biConsumer) {
        this.mExpandedHeightListeners.remove(biConsumer);
    }

    public void setHeadsUpAppearanceController(HeadsUpAppearanceController headsUpAppearanceController) {
        this.mHeadsUpAppearanceController = headsUpAppearanceController;
    }

    private class NotificationSwipeHelper extends SwipeHelper implements NotificationSwipeActionHelper {
        private Runnable mFalsingCheck;
        private Handler mHandler;

        public NotificationSwipeHelper(int i, SwipeHelper.Callback callback, Context context) {
            super(i, callback, context);
            this.mHandler = new Handler();
            this.mFalsingCheck = new Runnable() { // from class: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.NotificationSwipeHelper.1
                final /* synthetic */ NotificationStackScrollLayout val$this$0;

                AnonymousClass1(NotificationStackScrollLayout notificationStackScrollLayout) {
                    notificationStackScrollLayout = notificationStackScrollLayout;
                }

                @Override // java.lang.Runnable
                public void run() {
                    NotificationSwipeHelper.this.resetExposedMenuView(true, true);
                }
            };
        }

        /* renamed from: com.android.systemui.statusbar.stack.NotificationStackScrollLayout$NotificationSwipeHelper$1 */
        class AnonymousClass1 implements Runnable {
            final /* synthetic */ NotificationStackScrollLayout val$this$0;

            AnonymousClass1(NotificationStackScrollLayout notificationStackScrollLayout) {
                notificationStackScrollLayout = notificationStackScrollLayout;
            }

            @Override // java.lang.Runnable
            public void run() {
                NotificationSwipeHelper.this.resetExposedMenuView(true, true);
            }
        }

        @Override // com.android.systemui.SwipeHelper
        public void onDownUpdate(View view, MotionEvent motionEvent) {
            NotificationStackScrollLayout.this.mTranslatingParentView = view;
            if (NotificationStackScrollLayout.this.mCurrMenuRow != null) {
                NotificationStackScrollLayout.this.mCurrMenuRow.onTouchEvent(view, motionEvent, 0.0f);
            }
            NotificationStackScrollLayout.this.mCurrMenuRow = null;
            this.mHandler.removeCallbacks(this.mFalsingCheck);
            resetExposedMenuView(true, false);
            if (view instanceof ExpandableNotificationRow) {
                NotificationStackScrollLayout.this.mCurrMenuRow = ((ExpandableNotificationRow) view).createMenu();
                NotificationStackScrollLayout.this.mCurrMenuRow.setSwipeActionHelper(this);
                NotificationStackScrollLayout.this.mCurrMenuRow.setMenuClickListener(NotificationStackScrollLayout.this);
                NotificationStackScrollLayout.this.mCurrMenuRow.onTouchEvent(view, motionEvent, 0.0f);
            }
        }

        @Override // com.android.systemui.SwipeHelper
        public void onMoveUpdate(View view, MotionEvent motionEvent, float f, float f2) {
            this.mHandler.removeCallbacks(this.mFalsingCheck);
            if (NotificationStackScrollLayout.this.mCurrMenuRow != null) {
                NotificationStackScrollLayout.this.mCurrMenuRow.onTouchEvent(view, motionEvent, 0.0f);
            }
        }

        @Override // com.android.systemui.SwipeHelper
        public boolean handleUpEvent(MotionEvent motionEvent, View view, float f, float f2) {
            if (NotificationStackScrollLayout.this.mCurrMenuRow != null) {
                return NotificationStackScrollLayout.this.mCurrMenuRow.onTouchEvent(view, motionEvent, f);
            }
            return false;
        }

        @Override // com.android.systemui.SwipeHelper
        public void dismissChild(View view, float f, boolean z) {
            super.dismissChild(view, f, z);
            if (NotificationStackScrollLayout.this.mIsExpanded) {
                NotificationStackScrollLayout.this.handleChildViewDismissed(view);
            }
            NotificationStackScrollLayout.this.mStatusBar.getGutsManager().closeAndSaveGuts(true, false, false, -1, -1, false);
            handleMenuCoveredOrDismissed();
        }

        @Override // com.android.systemui.SwipeHelper
        public void snapChild(View view, float f, float f2) {
            super.snapChild(view, f, f2);
            NotificationStackScrollLayout.this.onDragCancelled(view);
            if (f == 0.0f) {
                handleMenuCoveredOrDismissed();
            }
        }

        @Override // com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper
        public void snooze(StatusBarNotification statusBarNotification, NotificationSwipeActionHelper.SnoozeOption snoozeOption) {
            NotificationStackScrollLayout.this.mStatusBar.setNotificationSnoozed(statusBarNotification, snoozeOption);
        }

        @Override // com.android.systemui.SwipeHelper, com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper
        public boolean isFalseGesture(MotionEvent motionEvent) {
            return super.isFalseGesture(motionEvent);
        }

        private void handleMenuCoveredOrDismissed() {
            if (NotificationStackScrollLayout.this.mMenuExposedView != null && NotificationStackScrollLayout.this.mMenuExposedView == NotificationStackScrollLayout.this.mTranslatingParentView) {
                NotificationStackScrollLayout.this.mMenuExposedView = null;
            }
        }

        @Override // com.android.systemui.SwipeHelper
        public Animator getViewTranslationAnimator(View view, float f, ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
            if (view instanceof ExpandableNotificationRow) {
                return ((ExpandableNotificationRow) view).getTranslateViewAnimator(f, animatorUpdateListener);
            }
            return super.getViewTranslationAnimator(view, f, animatorUpdateListener);
        }

        @Override // com.android.systemui.SwipeHelper
        public void setTranslation(View view, float f) {
            ((ExpandableView) view).setTranslation(f);
        }

        @Override // com.android.systemui.SwipeHelper
        public float getTranslation(View view) {
            return ((ExpandableView) view).getTranslation();
        }

        @Override // com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper
        public void dismiss(View view, float f) {
            dismissChild(view, f, !swipedFastEnough(0.0f, 0.0f));
        }

        @Override // com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper
        public void snap(View view, float f, float f2) {
            snapChild(view, f, f2);
        }

        @Override // com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper
        public boolean swipedFarEnough(float f, float f2) {
            return swipedFarEnough();
        }

        @Override // com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper
        public boolean swipedFastEnough(float f, float f2) {
            return swipedFastEnough();
        }

        @Override // com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper
        public float getMinDismissVelocity() {
            return getEscapeVelocity();
        }

        public void onMenuShown(View view) {
            NotificationStackScrollLayout.this.onDragCancelled(view);
            if (NotificationStackScrollLayout.this.isAntiFalsingNeeded()) {
                this.mHandler.removeCallbacks(this.mFalsingCheck);
                this.mHandler.postDelayed(this.mFalsingCheck, 4000L);
            }
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Removed duplicated region for block: B:29:0x001c  */
        /* JADX WARN: Type inference failed for: r0v13, types: [android.view.View] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void closeControlsIfOutsideTouch(MotionEvent motionEvent) {
            NotificationGuts notificationGuts;
            NotificationGuts exposedGuts = NotificationStackScrollLayout.this.mStatusBar.getGutsManager().getExposedGuts();
            if (exposedGuts != null) {
                boolean zIsLeavebehind = exposedGuts.getGutsContent().isLeavebehind();
                notificationGuts = exposedGuts;
                if (zIsLeavebehind) {
                    if (NotificationStackScrollLayout.this.mCurrMenuRow != null && NotificationStackScrollLayout.this.mCurrMenuRow.isMenuVisible() && NotificationStackScrollLayout.this.mTranslatingParentView != null) {
                        notificationGuts = NotificationStackScrollLayout.this.mTranslatingParentView;
                    } else {
                        notificationGuts = null;
                    }
                }
            }
            if (notificationGuts != null && !NotificationStackScrollLayout.this.isTouchInView(motionEvent, notificationGuts)) {
                NotificationStackScrollLayout.this.mStatusBar.getGutsManager().closeAndSaveGuts(false, false, true, -1, -1, false);
                resetExposedMenuView(true, true);
            }
        }

        public void resetExposedMenuView(boolean z, boolean z2) {
            if (NotificationStackScrollLayout.this.mMenuExposedView != null) {
                if (z2 || NotificationStackScrollLayout.this.mMenuExposedView != NotificationStackScrollLayout.this.mTranslatingParentView) {
                    View view = NotificationStackScrollLayout.this.mMenuExposedView;
                    if (!z) {
                        if (NotificationStackScrollLayout.this.mMenuExposedView instanceof ExpandableNotificationRow) {
                            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) NotificationStackScrollLayout.this.mMenuExposedView;
                            if (!expandableNotificationRow.isRemoved()) {
                                expandableNotificationRow.resetTranslation();
                            }
                        }
                    } else {
                        Animator viewTranslationAnimator = getViewTranslationAnimator(view, 0.0f, null);
                        if (viewTranslationAnimator != null) {
                            viewTranslationAnimator.start();
                        }
                    }
                    NotificationStackScrollLayout.this.mMenuExposedView = null;
                }
            }
        }
    }

    private boolean isTouchInView(MotionEvent motionEvent, View view) {
        int height;
        if (view == null) {
            return false;
        }
        if (view instanceof ExpandableView) {
            height = ((ExpandableView) view).getActualHeight();
        } else {
            height = view.getHeight();
        }
        int rawX = (int) motionEvent.getRawX();
        int rawY = (int) motionEvent.getRawY();
        view.getLocationOnScreen(this.mTempInt2);
        int i = this.mTempInt2[0];
        int i2 = this.mTempInt2[1];
        return new Rect(i, i2, view.getWidth() + i, height + i2).contains(rawX, rawY);
    }

    private void updateContinuousShadowDrawing() {
        boolean z = this.mAnimationRunning || !this.mAmbientState.getDraggedViews().isEmpty();
        if (z != this.mContinuousShadowUpdate) {
            if (z) {
                getViewTreeObserver().addOnPreDrawListener(this.mShadowUpdater);
            } else {
                getViewTreeObserver().removeOnPreDrawListener(this.mShadowUpdater);
            }
            this.mContinuousShadowUpdate = z;
        }
    }

    @Override // com.android.systemui.statusbar.NotificationListContainer
    public void resetExposedMenuView(boolean z, boolean z2) {
        this.mSwipeHelper.resetExposedMenuView(z, z2);
    }

    public void closeControlsIfOutsideTouch(MotionEvent motionEvent) {
        this.mSwipeHelper.closeControlsIfOutsideTouch(motionEvent);
    }

    static class AnimationEvent {
        static AnimationFilter[] FILTERS = {new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateDimmed().animateZ(), new AnimationFilter().animateShadowAlpha(), new AnimationFilter().animateShadowAlpha().animateHeight(), new AnimationFilter().animateZ(), new AnimationFilter().animateDimmed(), new AnimationFilter().animateAlpha().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), null, new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateDimmed().animateZ().hasDelays(), new AnimationFilter().animateHideSensitive(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateShadowAlpha().animateDark().animateDimmed().animateHideSensitive().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateY(), new AnimationFilter().animateAlpha().animateY()};
        static int[] LENGTHS = {464, 464, 360, 360, 360, 360, 220, 220, 360, 500, 448, 360, 360, 360, 550, 300, 300, 360, 360, 550, 275};
        final int animationType;
        final View changingView;
        int darkAnimationOriginIndex;
        final long eventStartTime;
        final AnimationFilter filter;
        boolean headsUpFromBottom;
        final long length;
        View viewAfterChangingView;

        AnimationEvent(View view, int i) {
            this(view, i, LENGTHS[i]);
        }

        AnimationEvent(View view, int i, AnimationFilter animationFilter) {
            this(view, i, LENGTHS[i], animationFilter);
        }

        AnimationEvent(View view, int i, long j) {
            this(view, i, j, FILTERS[i]);
        }

        AnimationEvent(View view, int i, long j, AnimationFilter animationFilter) {
            this.eventStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.changingView = view;
            this.animationType = i;
            this.length = j;
            this.filter = animationFilter;
        }

        static long combineLength(ArrayList<AnimationEvent> arrayList) {
            int size = arrayList.size();
            long jMax = 0;
            for (int i = 0; i < size; i++) {
                AnimationEvent animationEvent = arrayList.get(i);
                jMax = Math.max(jMax, animationEvent.length);
                if (animationEvent.animationType == 10) {
                    return animationEvent.length;
                }
            }
            return jMax;
        }
    }
}
