package com.android.systemui.statusbar;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.MathUtils;
import android.util.Property;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.widget.CachingIconView;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationGuts;
import com.android.systemui.statusbar.notification.AboveShelfChangedListener;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.HybridNotificationView;
import com.android.systemui.statusbar.notification.NotificationInflater;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.stack.AmbientState;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ExpandableViewState;
import com.android.systemui.statusbar.stack.NotificationChildrenContainer;
import com.android.systemui.statusbar.stack.StackScrollState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class ExpandableNotificationRow extends ActivatableNotificationView implements PluginListener<NotificationMenuRowPlugin> {
    private static final Property<ExpandableNotificationRow, Float> TRANSLATE_CONTENT = new FloatProperty<ExpandableNotificationRow>("translate") { // from class: com.android.systemui.statusbar.ExpandableNotificationRow.2
        AnonymousClass2(String str) {
            super(str);
        }

        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(ExpandableNotificationRow expandableNotificationRow, float f) {
            expandableNotificationRow.setTranslation(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(ExpandableNotificationRow expandableNotificationRow) {
            return Float.valueOf(expandableNotificationRow.getTranslation());
        }
    };
    private boolean mAboveShelf;
    private AboveShelfChangedListener mAboveShelfChangedListener;
    private String mAppName;
    private View mChildAfterViewWhenDismissed;
    private boolean mChildIsExpanding;
    private NotificationChildrenContainer mChildrenContainer;
    private ViewStub mChildrenContainerStub;
    private boolean mChildrenExpanded;
    private float mContentTransformationAmount;
    private boolean mDark;
    private boolean mDismissed;
    private boolean mEnableNonGroupedNotificationExpand;
    private NotificationData.Entry mEntry;
    private boolean mExpandAnimationRunning;
    private View.OnClickListener mExpandClickListener;
    private boolean mExpandable;
    private boolean mExpandedWhenPinned;
    private FalsingManager mFalsingManager;
    private boolean mForceUnlocked;
    private boolean mGroupExpansionChanging;
    private NotificationGroupManager mGroupManager;
    private View mGroupParentWhenDismissed;
    private NotificationGuts mGuts;
    private ViewStub mGutsStub;
    private boolean mHasUserChangedExpansion;
    private float mHeaderVisibleAmount;
    private Consumer<Boolean> mHeadsUpAnimatingAwayListener;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHeadsupDisappearRunning;
    private boolean mHideSensitiveForIntrinsicHeight;
    private boolean mIconAnimationRunning;
    private int mIconTransformContentShift;
    private int mIconTransformContentShiftNoIcon;
    private boolean mIconsVisible;
    private int mIncreasedPaddingBetweenElements;
    private boolean mIsBlockingHelperShowing;
    private boolean mIsColorized;
    private boolean mIsHeadsUp;
    private boolean mIsLastChild;
    private boolean mIsLowPriority;
    private boolean mIsPinned;
    private boolean mIsSummaryWithChildren;
    private boolean mIsSystemChildExpanded;
    private boolean mIsSystemExpanded;
    private boolean mJustClicked;
    private boolean mKeepInParent;
    private boolean mLastChronometerRunning;
    private LayoutListener mLayoutListener;
    private NotificationContentView[] mLayouts;
    private ExpansionLogger mLogger;
    private String mLoggingKey;
    private LongPressListener mLongPressListener;
    private boolean mLowPriorityStateUpdated;
    private int mMaxHeadsUpHeight;
    private int mMaxHeadsUpHeightBeforeP;
    private int mMaxHeadsUpHeightIncreased;
    private int mMaxHeadsUpHeightLegacy;
    private NotificationMenuRowPlugin mMenuRow;
    private boolean mMustStayOnScreen;
    private int mNotificationAmbientHeight;
    private int mNotificationColor;
    private int mNotificationColorAmbient;
    private final NotificationInflater mNotificationInflater;
    private int mNotificationLaunchHeight;
    private int mNotificationMaxHeight;
    private int mNotificationMinHeight;
    private int mNotificationMinHeightBeforeP;
    private int mNotificationMinHeightLarge;
    private int mNotificationMinHeightLegacy;
    private ExpandableNotificationRow mNotificationParent;
    private NotificationViewState mNotificationViewState;
    private View.OnClickListener mOnAppOpsClickListener;
    private View.OnClickListener mOnClickListener;
    private Runnable mOnDismissRunnable;
    private OnExpandClickListener mOnExpandClickListener;
    private boolean mOnKeyguard;
    private NotificationContentView mPrivateLayout;
    private NotificationContentView mPublicLayout;
    private boolean mRefocusOnDismiss;
    private boolean mRemoved;
    private BooleanSupplier mSecureStateProvider;
    private boolean mSensitive;
    private boolean mSensitiveHiddenInGeneral;
    private boolean mShowAmbient;
    private boolean mShowGroupBackgroundWhenExpanded;
    private boolean mShowNoBackground;
    private boolean mShowingPublic;
    private boolean mShowingPublicInitialized;
    private StatusBarNotification mStatusBarNotification;
    private SystemNotificationAsyncTask mSystemNotificationAsyncTask;
    private Animator mTranslateAnim;
    private ArrayList<View> mTranslateableViews;
    private float mTranslationWhenRemoved;
    private boolean mUseIncreasedCollapsedHeight;
    private boolean mUseIncreasedHeadsUpHeight;
    private boolean mUserExpanded;
    private boolean mUserLocked;
    private boolean mWasChildInGroupWhenRemoved;

    public interface ExpansionLogger {
        void logNotificationExpansion(String str, boolean z, boolean z2);
    }

    public interface LayoutListener {
        void onLayout();
    }

    public interface LongPressListener {
        boolean onLongPress(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem);
    }

    public interface OnAppOpsClickListener {
        boolean onClick(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem);
    }

    public interface OnExpandClickListener {
        void onExpandClicked(NotificationData.Entry entry, boolean z);
    }

    /* renamed from: com.android.systemui.statusbar.ExpandableNotificationRow$1 */
    class AnonymousClass1 implements View.OnClickListener {
        AnonymousClass1() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            boolean z;
            if (ExpandableNotificationRow.this.shouldShowPublic() || ((ExpandableNotificationRow.this.mIsLowPriority && !ExpandableNotificationRow.this.isExpanded()) || !ExpandableNotificationRow.this.mGroupManager.isSummaryOfGroup(ExpandableNotificationRow.this.mStatusBarNotification))) {
                if (ExpandableNotificationRow.this.mEnableNonGroupedNotificationExpand) {
                    if (view.isAccessibilityFocused()) {
                        ExpandableNotificationRow.this.mPrivateLayout.setFocusOnVisibilityChange();
                    }
                    if (ExpandableNotificationRow.this.isPinned()) {
                        z = !ExpandableNotificationRow.this.mExpandedWhenPinned;
                        ExpandableNotificationRow.this.mExpandedWhenPinned = z;
                    } else {
                        z = !ExpandableNotificationRow.this.isExpanded();
                        ExpandableNotificationRow.this.setUserExpanded(z);
                    }
                    ExpandableNotificationRow.this.notifyHeightChanged(true);
                    ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, z);
                    MetricsLogger.action(ExpandableNotificationRow.this.mContext, 407, z);
                    return;
                }
                return;
            }
            ExpandableNotificationRow.this.mGroupExpansionChanging = true;
            boolean zIsGroupExpanded = ExpandableNotificationRow.this.mGroupManager.isGroupExpanded(ExpandableNotificationRow.this.mStatusBarNotification);
            boolean z2 = ExpandableNotificationRow.this.mGroupManager.toggleGroupExpansion(ExpandableNotificationRow.this.mStatusBarNotification);
            ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, z2);
            MetricsLogger.action(ExpandableNotificationRow.this.mContext, 408, z2);
            ExpandableNotificationRow.this.onExpansionChanged(true, zIsGroupExpanded);
        }
    }

    /* renamed from: com.android.systemui.statusbar.ExpandableNotificationRow$2 */
    class AnonymousClass2 extends FloatProperty<ExpandableNotificationRow> {
        AnonymousClass2(String str) {
            super(str);
        }

        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(ExpandableNotificationRow expandableNotificationRow, float f) {
            expandableNotificationRow.setTranslation(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(ExpandableNotificationRow expandableNotificationRow) {
            return Float.valueOf(expandableNotificationRow.getTranslation());
        }
    }

    private static Boolean isSystemNotification(Context context, StatusBarNotification statusBarNotification) throws PackageManager.NameNotFoundException {
        PackageManager packageManagerForUser = StatusBar.getPackageManagerForUser(context, statusBarNotification.getUser().getIdentifier());
        try {
            return Boolean.valueOf(Utils.isSystemPackage(context.getResources(), packageManagerForUser, packageManagerForUser.getPackageInfo(statusBarNotification.getPackageName(), 64)));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ExpandableNotifRow", "cacheIsSystemNotification: Could not find package info");
            return null;
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isGroupExpansionChanging() {
        if (isChildInGroup()) {
            return this.mNotificationParent.isGroupExpansionChanging();
        }
        return this.mGroupExpansionChanging;
    }

    public void setGroupExpansionChanging(boolean z) {
        this.mGroupExpansionChanging = z;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void setActualHeightAnimating(boolean z) {
        if (this.mPrivateLayout != null) {
            this.mPrivateLayout.setContentHeightAnimating(z);
        }
    }

    public NotificationContentView getPrivateLayout() {
        return this.mPrivateLayout;
    }

    public NotificationContentView getPublicLayout() {
        return this.mPublicLayout;
    }

    public void setIconAnimationRunning(boolean z) {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            setIconAnimationRunning(z, notificationContentView);
        }
        if (this.mIsSummaryWithChildren) {
            setIconAnimationRunningForChild(z, this.mChildrenContainer.getHeaderView());
            setIconAnimationRunningForChild(z, this.mChildrenContainer.getLowPriorityHeaderView());
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).setIconAnimationRunning(z);
            }
        }
        this.mIconAnimationRunning = z;
    }

    private void setIconAnimationRunning(boolean z, NotificationContentView notificationContentView) {
        if (notificationContentView != null) {
            View contractedChild = notificationContentView.getContractedChild();
            View expandedChild = notificationContentView.getExpandedChild();
            View headsUpChild = notificationContentView.getHeadsUpChild();
            setIconAnimationRunningForChild(z, contractedChild);
            setIconAnimationRunningForChild(z, expandedChild);
            setIconAnimationRunningForChild(z, headsUpChild);
        }
    }

    private void setIconAnimationRunningForChild(boolean z, View view) {
        if (view != null) {
            setIconRunning((ImageView) view.findViewById(R.id.icon), z);
            setIconRunning((ImageView) view.findViewById(R.id.mediaProjection), z);
        }
    }

    private void setIconRunning(ImageView imageView, boolean z) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                if (z) {
                    animationDrawable.start();
                    return;
                } else {
                    animationDrawable.stop();
                    return;
                }
            }
            if (drawable instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) drawable;
                if (z) {
                    animatedVectorDrawable.start();
                } else {
                    animatedVectorDrawable.stop();
                }
            }
        }
    }

    public void updateNotification(NotificationData.Entry entry) {
        this.mEntry = entry;
        this.mStatusBarNotification = entry.notification;
        this.mNotificationInflater.inflateNotificationViews();
        cacheIsSystemNotification();
    }

    private void cacheIsSystemNotification() {
        if (this.mEntry != null && this.mEntry.mIsSystemNotification == null && this.mSystemNotificationAsyncTask.getStatus() == AsyncTask.Status.PENDING) {
            this.mSystemNotificationAsyncTask.execute(new Void[0]);
        }
    }

    public boolean getIsNonblockable() {
        boolean zIsNonblockablePackage = ((NotificationBlockingHelperManager) Dependency.get(NotificationBlockingHelperManager.class)).isNonblockablePackage(this.mStatusBarNotification.getPackageName());
        if (this.mEntry != null && this.mEntry.mIsSystemNotification == null) {
            this.mSystemNotificationAsyncTask.cancel(true);
            this.mEntry.mIsSystemNotification = isSystemNotification(this.mContext, this.mStatusBarNotification);
        }
        if (zIsNonblockablePackage || this.mEntry == null || this.mEntry.mIsSystemNotification == null || !this.mEntry.mIsSystemNotification.booleanValue() || this.mEntry.channel == null || this.mEntry.channel.isBlockableSystem()) {
            return zIsNonblockablePackage;
        }
        return true;
    }

    public void onNotificationUpdated() {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.onNotificationUpdated(this.mEntry);
        }
        this.mIsColorized = this.mStatusBarNotification.getNotification().isColorized();
        this.mShowingPublicInitialized = false;
        updateNotificationColor();
        if (this.mMenuRow != null) {
            this.mMenuRow.onNotificationUpdated(this.mStatusBarNotification);
            this.mMenuRow.setAppName(this.mAppName);
        }
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.recreateNotificationHeader(this.mExpandClickListener);
            this.mChildrenContainer.onNotificationUpdated();
        }
        if (this.mIconAnimationRunning) {
            setIconAnimationRunning(true);
        }
        if (this.mNotificationParent != null) {
            this.mNotificationParent.updateChildrenHeaderAppearance();
        }
        onChildrenCountChanged();
        this.mPublicLayout.updateExpandButtons(true);
        updateLimits();
        updateIconVisibilities();
        updateShelfIconColor();
        updateRippleAllowed();
    }

    @VisibleForTesting
    void updateShelfIconColor() {
        StatusBarIconView statusBarIconView = this.mEntry.expandedIcon;
        boolean z = true;
        int contrastedColor = 0;
        if (!Boolean.TRUE.equals(statusBarIconView.getTag(com.android.systemui.R.id.icon_is_pre_L)) || NotificationUtils.isGrayscale(statusBarIconView, NotificationColorUtil.getInstance(this.mContext))) {
            NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
            if (visibleNotificationHeader != null) {
                contrastedColor = visibleNotificationHeader.getOriginalIconColor();
            } else {
                NotificationData.Entry entry = this.mEntry;
                Context context = this.mContext;
                if (!this.mIsLowPriority || isExpanded()) {
                    z = false;
                }
                contrastedColor = entry.getContrastedColor(context, z, getBackgroundColorWithoutTint());
            }
        }
        statusBarIconView.setStaticDrawableColor(contrastedColor);
    }

    public void setAboveShelfChangedListener(AboveShelfChangedListener aboveShelfChangedListener) {
        this.mAboveShelfChangedListener = aboveShelfChangedListener;
    }

    public void setSecureStateProvider(BooleanSupplier booleanSupplier) {
        this.mSecureStateProvider = booleanSupplier;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    public boolean isDimmable() {
        if (!getShowingLayout().isDimmable()) {
            return false;
        }
        return super.isDimmable();
    }

    private void updateLimits() {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            updateLimitsForView(notificationContentView);
        }
    }

    private void updateLimitsForView(NotificationContentView notificationContentView) {
        int i;
        int iMax;
        boolean z = false;
        boolean z2 = notificationContentView.getContractedChild().getId() != 16909350;
        boolean z3 = this.mEntry.targetSdk < 24;
        boolean z4 = this.mEntry.targetSdk < 28;
        if (z2 && z4 && !this.mIsSummaryWithChildren) {
            i = z3 ? this.mNotificationMinHeightLegacy : this.mNotificationMinHeightBeforeP;
        } else if (this.mUseIncreasedCollapsedHeight && notificationContentView == this.mPrivateLayout) {
            i = this.mNotificationMinHeightLarge;
        } else {
            i = this.mNotificationMinHeight;
        }
        if (notificationContentView.getHeadsUpChild() != null && notificationContentView.getHeadsUpChild().getId() != 16909350) {
            z = true;
        }
        if (z && z4) {
            iMax = z3 ? this.mMaxHeadsUpHeightLegacy : this.mMaxHeadsUpHeightBeforeP;
        } else if (this.mUseIncreasedHeadsUpHeight && notificationContentView == this.mPrivateLayout) {
            iMax = this.mMaxHeadsUpHeightIncreased;
        } else {
            iMax = this.mMaxHeadsUpHeight;
        }
        NotificationViewWrapper visibleWrapper = notificationContentView.getVisibleWrapper(2);
        if (visibleWrapper != null) {
            iMax = Math.max(iMax, visibleWrapper.getMinLayoutHeight());
        }
        notificationContentView.setHeights(i, iMax, this.mNotificationMaxHeight, this.mNotificationAmbientHeight);
    }

    public StatusBarNotification getStatusBarNotification() {
        return this.mStatusBarNotification;
    }

    public NotificationData.Entry getEntry() {
        return this.mEntry;
    }

    public boolean isHeadsUp() {
        return this.mIsHeadsUp;
    }

    public void setHeadsUp(boolean z) {
        boolean zIsAboveShelf = isAboveShelf();
        int intrinsicHeight = getIntrinsicHeight();
        this.mIsHeadsUp = z;
        this.mPrivateLayout.setHeadsUp(z);
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateGroupOverflow();
        }
        if (intrinsicHeight != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
        if (z) {
            this.mMustStayOnScreen = true;
            setAboveShelf(true);
        } else if (isAboveShelf() != zIsAboveShelf) {
            this.mAboveShelfChangedListener.onAboveShelfStateChanged(!zIsAboveShelf);
        }
    }

    public void setGroupManager(NotificationGroupManager notificationGroupManager) {
        this.mGroupManager = notificationGroupManager;
        this.mPrivateLayout.setGroupManager(notificationGroupManager);
    }

    public void setRemoteInputController(RemoteInputController remoteInputController) {
        this.mPrivateLayout.setRemoteInputController(remoteInputController);
    }

    public void setAppName(String str) {
        this.mAppName = str;
        if (this.mMenuRow != null && this.mMenuRow.getMenuView() != null) {
            this.mMenuRow.setAppName(this.mAppName);
        }
    }

    public void setHeaderVisibleAmount(float f) {
        if (this.mHeaderVisibleAmount != f) {
            this.mHeaderVisibleAmount = f;
            this.mPrivateLayout.setHeaderVisibleAmount(f);
            if (this.mChildrenContainer != null) {
                this.mChildrenContainer.setHeaderVisibleAmount(f);
            }
            notifyHeightChanged(false);
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public float getHeaderVisibleAmount() {
        return this.mHeaderVisibleAmount;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void setHeadsUpIsVisible() {
        super.setHeadsUpIsVisible();
        this.mMustStayOnScreen = false;
    }

    public void addChildNotification(ExpandableNotificationRow expandableNotificationRow, int i) {
        if (this.mChildrenContainer == null) {
            this.mChildrenContainerStub.inflate();
        }
        this.mChildrenContainer.addNotification(expandableNotificationRow, i);
        onChildrenCountChanged();
        expandableNotificationRow.setIsChildInGroup(true, this);
    }

    public void removeChildNotification(ExpandableNotificationRow expandableNotificationRow) {
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.removeNotification(expandableNotificationRow);
        }
        onChildrenCountChanged();
        expandableNotificationRow.setIsChildInGroup(false, null);
        expandableNotificationRow.setBottomRoundness(0.0f, false);
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isChildInGroup() {
        return this.mNotificationParent != null;
    }

    public boolean isOnlyChildInGroup() {
        return this.mGroupManager.isOnlyChildInGroup(getStatusBarNotification());
    }

    public ExpandableNotificationRow getNotificationParent() {
        return this.mNotificationParent;
    }

    public void setIsChildInGroup(boolean z, ExpandableNotificationRow expandableNotificationRow) {
        boolean z2 = StatusBar.ENABLE_CHILD_NOTIFICATIONS && z;
        if (this.mExpandAnimationRunning && !z && this.mNotificationParent != null) {
            this.mNotificationParent.setChildIsExpanding(false);
            this.mNotificationParent.setExtraWidthForClipping(0.0f);
            this.mNotificationParent.setMinimumHeightForClipping(0);
        }
        if (!z2) {
            expandableNotificationRow = null;
        }
        this.mNotificationParent = expandableNotificationRow;
        this.mPrivateLayout.setIsChildInGroup(z2);
        this.mNotificationInflater.setIsChildInGroup(z2);
        resetBackgroundAlpha();
        updateBackgroundForGroupState();
        updateClickAndFocus();
        if (this.mNotificationParent != null) {
            setOverrideTintColor(0, 0.0f);
            setDistanceToTopRoundness(-1.0f);
            this.mNotificationParent.updateBackgroundForGroupState();
        }
        updateIconVisibilities();
        updateBackgroundClipping();
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() != 0 || !isChildInGroup() || isGroupExpanded()) {
            return super.onTouchEvent(motionEvent);
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    protected boolean handleSlideBack() {
        if (this.mMenuRow != null && this.mMenuRow.isMenuVisible()) {
            animateTranslateNotification(0.0f);
            return true;
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    protected boolean shouldHideBackground() {
        return super.shouldHideBackground() || this.mShowNoBackground;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isSummaryWithChildren() {
        return this.mIsSummaryWithChildren;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean areChildrenExpanded() {
        return this.mChildrenExpanded;
    }

    public List<ExpandableNotificationRow> getNotificationChildren() {
        if (this.mChildrenContainer == null) {
            return null;
        }
        return this.mChildrenContainer.getNotificationChildren();
    }

    public boolean applyChildOrder(List<ExpandableNotificationRow> list, VisualStabilityManager visualStabilityManager, VisualStabilityManager.Callback callback) {
        return this.mChildrenContainer != null && this.mChildrenContainer.applyChildOrder(list, visualStabilityManager, callback);
    }

    public void getChildrenStates(StackScrollState stackScrollState, AmbientState ambientState) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.getState(stackScrollState, stackScrollState.getViewStateForView(this), ambientState);
        }
    }

    public void applyChildrenState(StackScrollState stackScrollState) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.applyState(stackScrollState);
        }
    }

    public void prepareExpansionChanged(StackScrollState stackScrollState) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.prepareExpansionChanged(stackScrollState);
        }
    }

    public void startChildAnimation(StackScrollState stackScrollState, AnimationProperties animationProperties) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.startAnimationToState(stackScrollState, animationProperties);
        }
    }

    public ExpandableNotificationRow getViewAtPosition(float f) {
        if (!this.mIsSummaryWithChildren || !this.mChildrenExpanded) {
            return this;
        }
        ExpandableNotificationRow viewAtPosition = this.mChildrenContainer.getViewAtPosition(f);
        return viewAtPosition == null ? this : viewAtPosition;
    }

    public NotificationGuts getGuts() {
        return this.mGuts;
    }

    public void setPinned(boolean z) {
        int intrinsicHeight = getIntrinsicHeight();
        boolean zIsAboveShelf = isAboveShelf();
        this.mIsPinned = z;
        if (intrinsicHeight != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
        if (z) {
            setIconAnimationRunning(true);
            this.mExpandedWhenPinned = false;
        } else if (this.mExpandedWhenPinned) {
            setUserExpanded(true);
        }
        setChronometerRunning(this.mLastChronometerRunning);
        if (isAboveShelf() != zIsAboveShelf) {
            this.mAboveShelfChangedListener.onAboveShelfStateChanged(true ^ zIsAboveShelf);
        }
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    public boolean isPinned() {
        return this.mIsPinned;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public int getPinnedHeadsUpHeight() {
        return getPinnedHeadsUpHeight(true);
    }

    private int getPinnedHeadsUpHeight(boolean z) {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getIntrinsicHeight();
        }
        if (this.mExpandedWhenPinned) {
            return Math.max(getMaxExpandHeight(), getHeadsUpHeight());
        }
        if (z) {
            return Math.max(getCollapsedHeight(), getHeadsUpHeight());
        }
        return getHeadsUpHeight();
    }

    public void setJustClicked(boolean z) {
        this.mJustClicked = z;
    }

    public boolean wasJustClicked() {
        return this.mJustClicked;
    }

    public void setChronometerRunning(boolean z) {
        this.mLastChronometerRunning = z;
        setChronometerRunning(z, this.mPrivateLayout);
        setChronometerRunning(z, this.mPublicLayout);
        if (this.mChildrenContainer != null) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).setChronometerRunning(z);
            }
        }
    }

    private void setChronometerRunning(boolean z, NotificationContentView notificationContentView) {
        if (notificationContentView != null) {
            boolean z2 = z || isPinned();
            View contractedChild = notificationContentView.getContractedChild();
            View expandedChild = notificationContentView.getExpandedChild();
            View headsUpChild = notificationContentView.getHeadsUpChild();
            setChronometerRunningForChild(z2, contractedChild);
            setChronometerRunningForChild(z2, expandedChild);
            setChronometerRunningForChild(z2, headsUpChild);
        }
    }

    private void setChronometerRunningForChild(boolean z, View view) {
        if (view != null) {
            View viewFindViewById = view.findViewById(R.id.ask_checkbox);
            if (viewFindViewById instanceof Chronometer) {
                ((Chronometer) viewFindViewById).setStarted(z);
            }
        }
    }

    public NotificationHeaderView getNotificationHeader() {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getHeaderView();
        }
        return this.mPrivateLayout.getNotificationHeader();
    }

    public NotificationHeaderView getVisibleNotificationHeader() {
        if (this.mIsSummaryWithChildren && !shouldShowPublic()) {
            return this.mChildrenContainer.getVisibleHeader();
        }
        return getShowingLayout().getVisibleNotificationHeader();
    }

    public NotificationHeaderView getContractedNotificationHeader() {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getHeaderView();
        }
        return this.mPrivateLayout.getContractedNotificationHeader();
    }

    public void setOnExpandClickListener(OnExpandClickListener onExpandClickListener) {
        this.mOnExpandClickListener = onExpandClickListener;
    }

    public void setLongPressListener(LongPressListener longPressListener) {
        this.mLongPressListener = longPressListener;
    }

    @Override // android.view.View
    public void setOnClickListener(View.OnClickListener onClickListener) {
        super.setOnClickListener(onClickListener);
        this.mOnClickListener = onClickListener;
        updateClickAndFocus();
    }

    private void updateClickAndFocus() {
        boolean z = !isChildInGroup() || isGroupExpanded();
        boolean z2 = this.mOnClickListener != null && z;
        if (isFocusable() != z) {
            setFocusable(z);
        }
        if (isClickable() != z2) {
            setClickable(z2);
        }
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void setGutsView(NotificationMenuRowPlugin.MenuItem menuItem) {
        if (this.mGuts != null && (menuItem.getGutsView() instanceof NotificationGuts.GutsContent)) {
            ((NotificationGuts.GutsContent) menuItem.getGutsView()).setGutsParent(this.mGuts);
            this.mGuts.setGutsContent((NotificationGuts.GutsContent) menuItem.getGutsView());
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener((PluginListener) this, NotificationMenuRowPlugin.class, false);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
    }

    /* JADX DEBUG: Method merged with bridge method: onPluginConnected(Lcom/android/systemui/plugins/Plugin;Landroid/content/Context;)V */
    @Override // com.android.systemui.plugins.PluginListener
    public void onPluginConnected(NotificationMenuRowPlugin notificationMenuRowPlugin, Context context) {
        boolean z = this.mMenuRow.getMenuView() != null;
        if (z) {
            removeView(this.mMenuRow.getMenuView());
        }
        this.mMenuRow = notificationMenuRowPlugin;
        if (this.mMenuRow.useDefaultMenuItems()) {
            ArrayList<NotificationMenuRowPlugin.MenuItem> arrayList = new ArrayList<>();
            arrayList.add(NotificationMenuRow.createInfoItem(this.mContext));
            arrayList.add(NotificationMenuRow.createSnoozeItem(this.mContext));
            arrayList.add(NotificationMenuRow.createAppOpsItem(this.mContext));
            this.mMenuRow.setMenuItems(arrayList);
        }
        if (z) {
            createMenu();
        }
    }

    /* JADX DEBUG: Method merged with bridge method: onPluginDisconnected(Lcom/android/systemui/plugins/Plugin;)V */
    @Override // com.android.systemui.plugins.PluginListener
    public void onPluginDisconnected(NotificationMenuRowPlugin notificationMenuRowPlugin) {
        boolean z = this.mMenuRow.getMenuView() != null;
        this.mMenuRow = new NotificationMenuRow(this.mContext);
        if (z) {
            createMenu();
        }
    }

    public NotificationMenuRowPlugin createMenu() {
        if (this.mMenuRow.getMenuView() == null) {
            this.mMenuRow.createMenu(this, this.mStatusBarNotification);
            this.mMenuRow.setAppName(this.mAppName);
            addView(this.mMenuRow.getMenuView(), 0, new FrameLayout.LayoutParams(-1, -1));
        }
        return this.mMenuRow;
    }

    public NotificationMenuRowPlugin getProvider() {
        return this.mMenuRow;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, com.android.systemui.statusbar.ExpandableOutlineView
    public void onDensityOrFontScaleChanged() {
        super.onDensityOrFontScaleChanged();
        initDimens();
        initBackground();
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.reInflateViews(this.mExpandClickListener, this.mEntry.notification);
        }
        if (this.mGuts != null) {
            NotificationGuts notificationGuts = this.mGuts;
            int iIndexOfChild = indexOfChild(notificationGuts);
            removeView(notificationGuts);
            this.mGuts = (NotificationGuts) LayoutInflater.from(this.mContext).inflate(com.android.systemui.R.layout.notification_guts, (ViewGroup) this, false);
            this.mGuts.setVisibility(notificationGuts.getVisibility());
            addView(this.mGuts, iIndexOfChild);
        }
        View menuView = this.mMenuRow.getMenuView();
        if (menuView != null) {
            int iIndexOfChild2 = indexOfChild(menuView);
            removeView(menuView);
            this.mMenuRow.createMenu(this, this.mStatusBarNotification);
            this.mMenuRow.setAppName(this.mAppName);
            addView(this.mMenuRow.getMenuView(), iIndexOfChild2);
        }
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.initView();
            notificationContentView.reInflateViews();
        }
        this.mNotificationInflater.onDensityOrFontScaleChanged();
        onNotificationUpdated();
    }

    @Override // android.view.View
    public void onConfigurationChanged(Configuration configuration) {
        if (this.mMenuRow.getMenuView() != null) {
            this.mMenuRow.onConfigurationChanged();
        }
    }

    public void setContentBackground(int i, boolean z, NotificationContentView notificationContentView) {
        if (getShowingLayout() == notificationContentView) {
            setTintColor(i, z);
        }
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    protected void setBackgroundTintColor(int i) {
        super.setBackgroundTintColor(i);
        NotificationContentView showingLayout = getShowingLayout();
        if (showingLayout != null) {
            showingLayout.setBackgroundTintColor(i);
        }
    }

    public void closeRemoteInput() {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.closeRemoteInput();
        }
    }

    public void setSingleLineWidthIndention(int i) {
        this.mPrivateLayout.setSingleLineWidthIndention(i);
    }

    public int getNotificationColor() {
        return this.mNotificationColor;
    }

    private void updateNotificationColor() {
        this.mNotificationColor = NotificationColorUtil.resolveContrastColor(this.mContext, getStatusBarNotification().getNotification().color, getBackgroundColorWithoutTint());
        this.mNotificationColorAmbient = NotificationColorUtil.resolveAmbientColor(this.mContext, getStatusBarNotification().getNotification().color);
    }

    public HybridNotificationView getSingleLineView() {
        return this.mPrivateLayout.getSingleLineView();
    }

    public HybridNotificationView getAmbientSingleLineView() {
        return getShowingLayout().getAmbientSingleLineChild();
    }

    public boolean isOnKeyguard() {
        return this.mOnKeyguard;
    }

    public void removeAllChildren() {
        ArrayList arrayList = new ArrayList(this.mChildrenContainer.getNotificationChildren());
        for (int i = 0; i < arrayList.size(); i++) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) arrayList.get(i);
            if (!expandableNotificationRow.keepInParent()) {
                this.mChildrenContainer.removeNotification(expandableNotificationRow);
                expandableNotificationRow.setIsChildInGroup(false, null);
            }
        }
        onChildrenCountChanged();
    }

    public void setDismissed(boolean z) {
        List<ExpandableNotificationRow> notificationChildren;
        int iIndexOf;
        setLongPressListener(null);
        this.mDismissed = true;
        this.mGroupParentWhenDismissed = this.mNotificationParent;
        this.mRefocusOnDismiss = z;
        this.mChildAfterViewWhenDismissed = null;
        this.mEntry.icon.setDismissed();
        if (isChildInGroup() && (iIndexOf = (notificationChildren = this.mNotificationParent.getNotificationChildren()).indexOf(this)) != -1 && iIndexOf < notificationChildren.size() - 1) {
            this.mChildAfterViewWhenDismissed = notificationChildren.get(iIndexOf + 1);
        }
    }

    public boolean isDismissed() {
        return this.mDismissed;
    }

    public boolean keepInParent() {
        return this.mKeepInParent;
    }

    public void setKeepInParent(boolean z) {
        this.mKeepInParent = z;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isRemoved() {
        return this.mRemoved;
    }

    public void setRemoved() {
        this.mRemoved = true;
        this.mTranslationWhenRemoved = getTranslationY();
        this.mWasChildInGroupWhenRemoved = isChildInGroup();
        if (isChildInGroup()) {
            this.mTranslationWhenRemoved += getNotificationParent().getTranslationY();
        }
        this.mPrivateLayout.setRemoved();
    }

    public boolean wasChildInGroupWhenRemoved() {
        return this.mWasChildInGroupWhenRemoved;
    }

    public float getTranslationWhenRemoved() {
        return this.mTranslationWhenRemoved;
    }

    public NotificationChildrenContainer getChildrenContainer() {
        return this.mChildrenContainer;
    }

    public void setHeadsUpAnimatingAway(boolean z) {
        boolean z2;
        boolean zIsAboveShelf = isAboveShelf();
        if (z == this.mHeadsupDisappearRunning) {
            z2 = false;
        } else {
            z2 = true;
        }
        this.mHeadsupDisappearRunning = z;
        this.mPrivateLayout.setHeadsUpAnimatingAway(z);
        if (z2 && this.mHeadsUpAnimatingAwayListener != null) {
            this.mHeadsUpAnimatingAwayListener.accept(Boolean.valueOf(z));
        }
        if (isAboveShelf() != zIsAboveShelf) {
            this.mAboveShelfChangedListener.onAboveShelfStateChanged(!zIsAboveShelf);
        }
    }

    public void setHeadsUpAnimatingAwayListener(Consumer<Boolean> consumer) {
        this.mHeadsUpAnimatingAwayListener = consumer;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    public boolean isHeadsUpAnimatingAway() {
        return this.mHeadsupDisappearRunning;
    }

    public View getChildAfterViewWhenDismissed() {
        return this.mChildAfterViewWhenDismissed;
    }

    public View getGroupParentWhenDismissed() {
        return this.mGroupParentWhenDismissed;
    }

    public boolean performDismissWithBlockingHelper(boolean z) {
        boolean zPerhapsShowBlockingHelper = ((NotificationBlockingHelperManager) Dependency.get(NotificationBlockingHelperManager.class)).perhapsShowBlockingHelper(this, this.mMenuRow);
        ((MetricsLogger) Dependency.get(MetricsLogger.class)).count("notification_dismissed", 1);
        performDismiss(z);
        return zPerhapsShowBlockingHelper;
    }

    public void performDismiss(boolean z) {
        if (isOnlyChildInGroup()) {
            ExpandableNotificationRow logicalGroupSummary = this.mGroupManager.getLogicalGroupSummary(getStatusBarNotification());
            if (logicalGroupSummary.isClearable()) {
                logicalGroupSummary.performDismiss(z);
            }
        }
        setDismissed(z);
        if (isClearable() && this.mOnDismissRunnable != null) {
            this.mOnDismissRunnable.run();
        }
    }

    public void setBlockingHelperShowing(boolean z) {
        this.mIsBlockingHelperShowing = z;
    }

    public boolean isBlockingHelperShowing() {
        return this.mIsBlockingHelperShowing;
    }

    public void setOnDismissRunnable(Runnable runnable) {
        this.mOnDismissRunnable = runnable;
    }

    public View getNotificationIcon() {
        NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
        if (visibleNotificationHeader != null) {
            return visibleNotificationHeader.getIcon();
        }
        return null;
    }

    public boolean isShowingIcon() {
        return (areGutsExposed() || getVisibleNotificationHeader() == null) ? false : true;
    }

    public void setContentTransformationAmount(float f, boolean z) {
        boolean z2 = (z != this.mIsLastChild) | (this.mContentTransformationAmount != f);
        this.mIsLastChild = z;
        this.mContentTransformationAmount = f;
        if (z2) {
            updateContentTransformation();
        }
    }

    public void setIconsVisible(boolean z) {
        if (z != this.mIconsVisible) {
            this.mIconsVisible = z;
            updateIconVisibilities();
        }
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    protected void onBelowSpeedBumpChanged() {
        updateIconVisibilities();
    }

    private void updateContentTransformation() {
        if (this.mExpandAnimationRunning) {
            return;
        }
        float f = (-this.mContentTransformationAmount) * this.mIconTransformContentShift;
        float interpolation = 1.0f;
        if (this.mIsLastChild) {
            interpolation = Interpolators.ALPHA_OUT.getInterpolation(Math.min((1.0f - this.mContentTransformationAmount) / 0.5f, 1.0f));
            f *= 0.4f;
        }
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setAlpha(interpolation);
            notificationContentView.setTranslationY(f);
        }
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setAlpha(interpolation);
            this.mChildrenContainer.setTranslationY(f);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:26:0x0011  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void updateIconVisibilities() {
        boolean z;
        if (!isChildInGroup()) {
            isBelowSpeedBump();
            z = this.mIconsVisible;
        }
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setIconsVisible(z);
        }
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setIconsVisible(z);
        }
    }

    public int getRelativeTopPadding(View view) {
        int top = 0;
        while (view.getParent() instanceof ViewGroup) {
            top += view.getTop();
            view = (View) view.getParent();
            if (view instanceof ExpandableNotificationRow) {
                return top;
            }
        }
        return top;
    }

    public float getContentTranslation() {
        return this.mPrivateLayout.getTranslationY();
    }

    public void setIsLowPriority(boolean z) {
        this.mIsLowPriority = z;
        this.mPrivateLayout.setIsLowPriority(z);
        this.mNotificationInflater.setIsLowPriority(this.mIsLowPriority);
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setIsLowPriority(z);
        }
    }

    public void setLowPriorityStateUpdated(boolean z) {
        this.mLowPriorityStateUpdated = z;
    }

    public boolean hasLowPriorityStateUpdated() {
        return this.mLowPriorityStateUpdated;
    }

    public boolean isLowPriority() {
        return this.mIsLowPriority;
    }

    public void setUseIncreasedCollapsedHeight(boolean z) {
        this.mUseIncreasedCollapsedHeight = z;
        this.mNotificationInflater.setUsesIncreasedHeight(z);
    }

    public void setUseIncreasedHeadsUpHeight(boolean z) {
        this.mUseIncreasedHeadsUpHeight = z;
        this.mNotificationInflater.setUsesIncreasedHeadsUpHeight(z);
    }

    public void setRemoteViewClickHandler(RemoteViews.OnClickHandler onClickHandler) {
        this.mNotificationInflater.setRemoteViewClickHandler(onClickHandler);
    }

    public void setInflationCallback(NotificationInflater.InflationCallback inflationCallback) {
        this.mNotificationInflater.setInflationCallback(inflationCallback);
    }

    public void setNeedsRedaction(boolean z) {
        this.mNotificationInflater.setRedactAmbient(z);
    }

    @VisibleForTesting
    public NotificationInflater getNotificationInflater() {
        return this.mNotificationInflater;
    }

    public int getNotificationColorAmbient() {
        return this.mNotificationColorAmbient;
    }

    public ExpandableNotificationRow(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHeaderVisibleAmount = 1.0f;
        this.mLastChronometerRunning = true;
        this.mExpandClickListener = new View.OnClickListener() { // from class: com.android.systemui.statusbar.ExpandableNotificationRow.1
            AnonymousClass1() {
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                boolean z;
                if (ExpandableNotificationRow.this.shouldShowPublic() || ((ExpandableNotificationRow.this.mIsLowPriority && !ExpandableNotificationRow.this.isExpanded()) || !ExpandableNotificationRow.this.mGroupManager.isSummaryOfGroup(ExpandableNotificationRow.this.mStatusBarNotification))) {
                    if (ExpandableNotificationRow.this.mEnableNonGroupedNotificationExpand) {
                        if (view.isAccessibilityFocused()) {
                            ExpandableNotificationRow.this.mPrivateLayout.setFocusOnVisibilityChange();
                        }
                        if (ExpandableNotificationRow.this.isPinned()) {
                            z = !ExpandableNotificationRow.this.mExpandedWhenPinned;
                            ExpandableNotificationRow.this.mExpandedWhenPinned = z;
                        } else {
                            z = !ExpandableNotificationRow.this.isExpanded();
                            ExpandableNotificationRow.this.setUserExpanded(z);
                        }
                        ExpandableNotificationRow.this.notifyHeightChanged(true);
                        ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, z);
                        MetricsLogger.action(ExpandableNotificationRow.this.mContext, 407, z);
                        return;
                    }
                    return;
                }
                ExpandableNotificationRow.this.mGroupExpansionChanging = true;
                boolean zIsGroupExpanded = ExpandableNotificationRow.this.mGroupManager.isGroupExpanded(ExpandableNotificationRow.this.mStatusBarNotification);
                boolean z2 = ExpandableNotificationRow.this.mGroupManager.toggleGroupExpansion(ExpandableNotificationRow.this.mStatusBarNotification);
                ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, z2);
                MetricsLogger.action(ExpandableNotificationRow.this.mContext, 408, z2);
                ExpandableNotificationRow.this.onExpansionChanged(true, zIsGroupExpanded);
            }
        };
        this.mIconsVisible = true;
        this.mSystemNotificationAsyncTask = new SystemNotificationAsyncTask();
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mNotificationInflater = new NotificationInflater(this);
        this.mMenuRow = new NotificationMenuRow(this.mContext);
        initDimens();
    }

    private void initDimens() {
        this.mNotificationMinHeightLegacy = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_min_height_legacy);
        this.mNotificationMinHeightBeforeP = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_min_height_before_p);
        this.mNotificationMinHeight = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_min_height);
        this.mNotificationMinHeightLarge = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_min_height_increased);
        this.mNotificationMaxHeight = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_max_height);
        this.mNotificationAmbientHeight = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_ambient_height);
        this.mMaxHeadsUpHeightLegacy = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_max_heads_up_height_legacy);
        this.mMaxHeadsUpHeightBeforeP = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_max_heads_up_height_before_p);
        this.mMaxHeadsUpHeight = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_max_heads_up_height);
        this.mMaxHeadsUpHeightIncreased = NotificationUtils.getFontScaledHeight(this.mContext, com.android.systemui.R.dimen.notification_max_heads_up_height_increased);
        Resources resources = getResources();
        this.mIncreasedPaddingBetweenElements = resources.getDimensionPixelSize(com.android.systemui.R.dimen.notification_divider_height_increased);
        this.mIconTransformContentShiftNoIcon = resources.getDimensionPixelSize(com.android.systemui.R.dimen.notification_icon_transform_content_shift);
        this.mEnableNonGroupedNotificationExpand = resources.getBoolean(com.android.systemui.R.bool.config_enableNonGroupedNotificationExpand);
        this.mShowGroupBackgroundWhenExpanded = resources.getBoolean(com.android.systemui.R.bool.config_showGroupNotificationBgWhenExpanded);
    }

    public void reset() {
        this.mShowingPublicInitialized = false;
        onHeightReset();
        requestLayout();
    }

    public void showAppOpsIcons(ArraySet<Integer> arraySet) {
        if (this.mIsSummaryWithChildren && this.mChildrenContainer.getHeaderView() != null) {
            this.mChildrenContainer.getHeaderView().showAppOpsIcons(arraySet);
        }
        this.mPrivateLayout.showAppOpsIcons(arraySet);
        this.mPublicLayout.showAppOpsIcons(arraySet);
    }

    public View.OnClickListener getAppOpsOnClickListener() {
        return this.mOnAppOpsClickListener;
    }

    protected void setAppOpsOnClickListener(final OnAppOpsClickListener onAppOpsClickListener) {
        this.mOnAppOpsClickListener = new View.OnClickListener() { // from class: com.android.systemui.statusbar.-$$Lambda$ExpandableNotificationRow$-ffBYE9PVKSnfJECJygWoqM2POs
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ExpandableNotificationRow.lambda$setAppOpsOnClickListener$0(this.f$0, onAppOpsClickListener, view);
            }
        };
    }

    public static /* synthetic */ void lambda$setAppOpsOnClickListener$0(ExpandableNotificationRow expandableNotificationRow, OnAppOpsClickListener onAppOpsClickListener, View view) {
        expandableNotificationRow.createMenu();
        NotificationMenuRowPlugin.MenuItem appOpsMenuItem = expandableNotificationRow.getProvider().getAppOpsMenuItem(expandableNotificationRow.mContext);
        if (appOpsMenuItem != null) {
            onAppOpsClickListener.onClick(expandableNotificationRow, view.getWidth() / 2, view.getHeight() / 2, appOpsMenuItem);
        }
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPublicLayout = (NotificationContentView) findViewById(com.android.systemui.R.id.expandedPublic);
        this.mPrivateLayout = (NotificationContentView) findViewById(com.android.systemui.R.id.expanded);
        this.mLayouts = new NotificationContentView[]{this.mPrivateLayout, this.mPublicLayout};
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setExpandClickListener(this.mExpandClickListener);
            notificationContentView.setContainingNotification(this);
        }
        this.mGutsStub = (ViewStub) findViewById(com.android.systemui.R.id.notification_guts_stub);
        this.mGutsStub.setOnInflateListener(new ViewStub.OnInflateListener() { // from class: com.android.systemui.statusbar.ExpandableNotificationRow.3
            AnonymousClass3() {
            }

            @Override // android.view.ViewStub.OnInflateListener
            public void onInflate(ViewStub viewStub, View view) {
                ExpandableNotificationRow.this.mGuts = (NotificationGuts) view;
                ExpandableNotificationRow.this.mGuts.setClipTopAmount(ExpandableNotificationRow.this.getClipTopAmount());
                ExpandableNotificationRow.this.mGuts.setActualHeight(ExpandableNotificationRow.this.getActualHeight());
                ExpandableNotificationRow.this.mGutsStub = null;
            }
        });
        this.mChildrenContainerStub = (ViewStub) findViewById(com.android.systemui.R.id.child_container_stub);
        this.mChildrenContainerStub.setOnInflateListener(new ViewStub.OnInflateListener() { // from class: com.android.systemui.statusbar.ExpandableNotificationRow.4
            AnonymousClass4() {
            }

            @Override // android.view.ViewStub.OnInflateListener
            public void onInflate(ViewStub viewStub, View view) {
                ExpandableNotificationRow.this.mChildrenContainer = (NotificationChildrenContainer) view;
                ExpandableNotificationRow.this.mChildrenContainer.setIsLowPriority(ExpandableNotificationRow.this.mIsLowPriority);
                ExpandableNotificationRow.this.mChildrenContainer.setContainingNotification(ExpandableNotificationRow.this);
                ExpandableNotificationRow.this.mChildrenContainer.onNotificationUpdated();
                if (ExpandableNotificationRow.this.mShouldTranslateContents) {
                    ExpandableNotificationRow.this.mTranslateableViews.add(ExpandableNotificationRow.this.mChildrenContainer);
                }
            }
        });
        if (this.mShouldTranslateContents) {
            this.mTranslateableViews = new ArrayList<>();
            for (int i = 0; i < getChildCount(); i++) {
                this.mTranslateableViews.add(getChildAt(i));
            }
            this.mTranslateableViews.remove(this.mChildrenContainerStub);
            this.mTranslateableViews.remove(this.mGutsStub);
        }
    }

    /* renamed from: com.android.systemui.statusbar.ExpandableNotificationRow$3 */
    class AnonymousClass3 implements ViewStub.OnInflateListener {
        AnonymousClass3() {
        }

        @Override // android.view.ViewStub.OnInflateListener
        public void onInflate(ViewStub viewStub, View view) {
            ExpandableNotificationRow.this.mGuts = (NotificationGuts) view;
            ExpandableNotificationRow.this.mGuts.setClipTopAmount(ExpandableNotificationRow.this.getClipTopAmount());
            ExpandableNotificationRow.this.mGuts.setActualHeight(ExpandableNotificationRow.this.getActualHeight());
            ExpandableNotificationRow.this.mGutsStub = null;
        }
    }

    /* renamed from: com.android.systemui.statusbar.ExpandableNotificationRow$4 */
    class AnonymousClass4 implements ViewStub.OnInflateListener {
        AnonymousClass4() {
        }

        @Override // android.view.ViewStub.OnInflateListener
        public void onInflate(ViewStub viewStub, View view) {
            ExpandableNotificationRow.this.mChildrenContainer = (NotificationChildrenContainer) view;
            ExpandableNotificationRow.this.mChildrenContainer.setIsLowPriority(ExpandableNotificationRow.this.mIsLowPriority);
            ExpandableNotificationRow.this.mChildrenContainer.setContainingNotification(ExpandableNotificationRow.this);
            ExpandableNotificationRow.this.mChildrenContainer.onNotificationUpdated();
            if (ExpandableNotificationRow.this.mShouldTranslateContents) {
                ExpandableNotificationRow.this.mTranslateableViews.add(ExpandableNotificationRow.this.mChildrenContainer);
            }
        }
    }

    private void doLongClickCallback() {
        doLongClickCallback(getWidth() / 2, getHeight() / 2);
    }

    public void doLongClickCallback(int i, int i2) {
        createMenu();
        doLongClickCallback(i, i2, getProvider().getLongpressMenuItem(this.mContext));
    }

    private void doLongClickCallback(int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem) {
        if (this.mLongPressListener != null && menuItem != null) {
            this.mLongPressListener.onLongPress(this, i, i2, menuItem);
        }
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (KeyEvent.isConfirmKey(i)) {
            keyEvent.startTracking();
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (KeyEvent.isConfirmKey(i)) {
            if (!keyEvent.isCanceled()) {
                performClick();
                return true;
            }
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        if (KeyEvent.isConfirmKey(i)) {
            doLongClickCallback();
            return true;
        }
        return false;
    }

    public void resetTranslation() {
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.cancel();
        }
        if (!this.mShouldTranslateContents) {
            setTranslationX(0.0f);
        } else if (this.mTranslateableViews != null) {
            for (int i = 0; i < this.mTranslateableViews.size(); i++) {
                this.mTranslateableViews.get(i).setTranslationX(0.0f);
            }
            invalidateOutline();
            getEntry().expandedIcon.setScrollX(0);
        }
        this.mMenuRow.resetMenu();
    }

    void onGutsOpened() {
        resetTranslation();
        updateContentAccessibilityImportanceForGuts(false);
    }

    void onGutsClosed() {
        updateContentAccessibilityImportanceForGuts(true);
    }

    private void updateContentAccessibilityImportanceForGuts(boolean z) {
        if (this.mChildrenContainer != null) {
            updateChildAccessibilityImportance(this.mChildrenContainer, z);
        }
        if (this.mLayouts != null) {
            for (NotificationContentView notificationContentView : this.mLayouts) {
                updateChildAccessibilityImportance(notificationContentView, z);
            }
        }
        if (z) {
            requestAccessibilityFocus();
        }
    }

    private void updateChildAccessibilityImportance(View view, boolean z) {
        int i;
        if (z) {
            i = 0;
        } else {
            i = 4;
        }
        view.setImportantForAccessibility(i);
    }

    public CharSequence getActiveRemoteInputText() {
        return this.mPrivateLayout.getActiveRemoteInputText();
    }

    public void animateTranslateNotification(float f) {
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.cancel();
        }
        this.mTranslateAnim = getTranslateViewAnimator(f, null);
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.start();
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void setTranslation(float f) {
        if (areGutsExposed()) {
            return;
        }
        if (!this.mShouldTranslateContents) {
            setTranslationX(f);
        } else if (this.mTranslateableViews != null) {
            for (int i = 0; i < this.mTranslateableViews.size(); i++) {
                if (this.mTranslateableViews.get(i) != null) {
                    this.mTranslateableViews.get(i).setTranslationX(f);
                }
            }
            invalidateOutline();
            getEntry().expandedIcon.setScrollX((int) (-f));
        }
        if (this.mMenuRow.getMenuView() != null) {
            this.mMenuRow.onTranslationUpdate(f);
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public float getTranslation() {
        if (!this.mShouldTranslateContents) {
            return getTranslationX();
        }
        if (this.mTranslateableViews != null && this.mTranslateableViews.size() > 0) {
            return this.mTranslateableViews.get(0).getTranslationX();
        }
        return 0.0f;
    }

    public Animator getTranslateViewAnimator(float f, ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.cancel();
        }
        if (areGutsExposed()) {
            return null;
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, TRANSLATE_CONTENT, f);
        if (animatorUpdateListener != null) {
            objectAnimatorOfFloat.addUpdateListener(animatorUpdateListener);
        }
        objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.ExpandableNotificationRow.5
            boolean cancelled = false;
            final /* synthetic */ float val$leftTarget;

            AnonymousClass5(float f2) {
                f = f2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.cancelled = true;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (!this.cancelled && f == 0.0f) {
                    ExpandableNotificationRow.this.mMenuRow.resetMenu();
                    ExpandableNotificationRow.this.mTranslateAnim = null;
                }
            }
        });
        this.mTranslateAnim = objectAnimatorOfFloat;
        return objectAnimatorOfFloat;
    }

    /* renamed from: com.android.systemui.statusbar.ExpandableNotificationRow$5 */
    class AnonymousClass5 extends AnimatorListenerAdapter {
        boolean cancelled = false;
        final /* synthetic */ float val$leftTarget;

        AnonymousClass5(float f2) {
            f = f2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            this.cancelled = true;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (!this.cancelled && f == 0.0f) {
                ExpandableNotificationRow.this.mMenuRow.resetMenu();
                ExpandableNotificationRow.this.mTranslateAnim = null;
            }
        }
    }

    public void inflateGuts() {
        if (this.mGuts == null) {
            this.mGutsStub.inflate();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1798=4] */
    private void updateChildrenVisibility() {
        boolean z = this.mExpandAnimationRunning && this.mGuts != null && this.mGuts.isExposed();
        this.mPrivateLayout.setVisibility((shouldShowPublic() || this.mIsSummaryWithChildren || z) ? 4 : 0);
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setVisibility((shouldShowPublic() || !this.mIsSummaryWithChildren || z) ? 4 : 0);
        }
        updateLimits();
    }

    public boolean onRequestSendAccessibilityEventInternal(View view, AccessibilityEvent accessibilityEvent) {
        if (super.onRequestSendAccessibilityEventInternal(view, accessibilityEvent)) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(accessibilityEventObtain);
            dispatchPopulateAccessibilityEvent(accessibilityEventObtain);
            accessibilityEvent.appendRecord(accessibilityEventObtain);
            return true;
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, com.android.systemui.statusbar.ExpandableView
    public void setDark(boolean z, boolean z2, long j) {
        super.setDark(z, z2, j);
        this.mDark = z;
        if (!this.mIsHeadsUp) {
            z2 = false;
        }
        NotificationContentView showingLayout = getShowingLayout();
        if (showingLayout != null) {
            showingLayout.setDark(z, z2, j);
        }
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.setDark(z, z2, j);
        }
        updateShelfIconColor();
    }

    public void applyExpandAnimationParams(ActivityLaunchAnimator.ExpandAnimationParameters expandAnimationParameters) {
        if (expandAnimationParameters == null) {
            return;
        }
        float fLerp = MathUtils.lerp(expandAnimationParameters.getStartTranslationZ(), this.mNotificationLaunchHeight, Interpolators.FAST_OUT_SLOW_IN.getInterpolation(expandAnimationParameters.getProgress(0L, 50L)));
        setTranslationZ(fLerp);
        float width = (expandAnimationParameters.getWidth() - getWidth()) + MathUtils.lerp(0.0f, this.mOutlineRadius * 2.0f, expandAnimationParameters.getProgress());
        setExtraWidthForClipping(width);
        int top = expandAnimationParameters.getTop();
        float interpolation = Interpolators.FAST_OUT_SLOW_IN.getInterpolation(expandAnimationParameters.getProgress());
        int startClipTopAmount = expandAnimationParameters.getStartClipTopAmount();
        if (this.mNotificationParent != null) {
            top = (int) (top - this.mNotificationParent.getTranslationY());
            this.mNotificationParent.setTranslationZ(fLerp);
            int parentStartClipTopAmount = expandAnimationParameters.getParentStartClipTopAmount();
            if (startClipTopAmount != 0) {
                this.mNotificationParent.setClipTopAmount((int) MathUtils.lerp(parentStartClipTopAmount, parentStartClipTopAmount - startClipTopAmount, interpolation));
            }
            this.mNotificationParent.setExtraWidthForClipping(width);
            this.mNotificationParent.setMinimumHeightForClipping(expandAnimationParameters.getHeight() + this.mNotificationParent.getActualHeight());
        } else if (startClipTopAmount != 0) {
            setClipTopAmount((int) MathUtils.lerp(startClipTopAmount, 0.0f, interpolation));
        }
        setTranslationY(top);
        setActualHeight(expandAnimationParameters.getHeight());
        this.mBackgroundNormal.setExpandAnimationParams(expandAnimationParameters);
    }

    public void setExpandAnimationRunning(boolean z) {
        View showingLayout;
        if (this.mIsSummaryWithChildren) {
            showingLayout = this.mChildrenContainer;
        } else {
            showingLayout = getShowingLayout();
        }
        if (this.mGuts != null && this.mGuts.isExposed()) {
            showingLayout = this.mGuts;
        }
        if (z) {
            showingLayout.animate().alpha(0.0f).setDuration(67L).setInterpolator(Interpolators.ALPHA_OUT);
            setAboveShelf(true);
            this.mExpandAnimationRunning = true;
            this.mNotificationViewState.cancelAnimations(this);
            this.mNotificationLaunchHeight = AmbientState.getNotificationLaunchHeight(getContext());
        } else {
            this.mExpandAnimationRunning = false;
            setAboveShelf(isAboveShelf());
            if (this.mGuts != null) {
                this.mGuts.setAlpha(1.0f);
            }
            if (showingLayout != null) {
                showingLayout.setAlpha(1.0f);
            }
            setExtraWidthForClipping(0.0f);
            if (this.mNotificationParent != null) {
                this.mNotificationParent.setExtraWidthForClipping(0.0f);
                this.mNotificationParent.setMinimumHeightForClipping(0);
            }
        }
        if (this.mNotificationParent != null) {
            this.mNotificationParent.setChildIsExpanding(this.mExpandAnimationRunning);
        }
        updateChildrenVisibility();
        updateClipping();
        this.mBackgroundNormal.setExpandAnimationRunning(z);
    }

    private void setChildIsExpanding(boolean z) {
        this.mChildIsExpanding = z;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean hasExpandingChild() {
        return this.mChildIsExpanding;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    protected boolean shouldClipToActualHeight() {
        return (!super.shouldClipToActualHeight() || this.mExpandAnimationRunning || this.mChildIsExpanding) ? false : true;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isExpandAnimationRunning() {
        return this.mExpandAnimationRunning;
    }

    @Override // android.view.View
    public boolean isSoundEffectsEnabled() {
        return !(this.mDark && this.mSecureStateProvider != null && !this.mSecureStateProvider.getAsBoolean()) && super.isSoundEffectsEnabled();
    }

    public boolean isExpandable() {
        if (!this.mIsSummaryWithChildren || shouldShowPublic()) {
            return this.mEnableNonGroupedNotificationExpand && this.mExpandable;
        }
        return !this.mChildrenExpanded;
    }

    public void setExpandable(boolean z) {
        this.mExpandable = z;
        this.mPrivateLayout.updateExpandButtons(isExpandable());
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void setClipToActualHeight(boolean z) {
        boolean z2 = true;
        super.setClipToActualHeight(z || isUserLocked());
        NotificationContentView showingLayout = getShowingLayout();
        if (!z && !isUserLocked()) {
            z2 = false;
        }
        showingLayout.setClipToActualHeight(z2);
    }

    public boolean hasUserChangedExpansion() {
        return this.mHasUserChangedExpansion;
    }

    public boolean isUserExpanded() {
        return this.mUserExpanded;
    }

    public void setUserExpanded(boolean z) {
        setUserExpanded(z, false);
    }

    public void setUserExpanded(boolean z, boolean z2) {
        this.mFalsingManager.setNotificationExpanded();
        if (this.mIsSummaryWithChildren && !shouldShowPublic() && z2 && !this.mChildrenContainer.showingAsLowPriority()) {
            boolean zIsGroupExpanded = this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
            this.mGroupManager.setGroupExpanded(this.mStatusBarNotification, z);
            onExpansionChanged(true, zIsGroupExpanded);
        } else if (!z || this.mExpandable) {
            boolean zIsExpanded = isExpanded();
            this.mHasUserChangedExpansion = true;
            this.mUserExpanded = z;
            onExpansionChanged(true, zIsExpanded);
            if (!zIsExpanded && isExpanded() && getActualHeight() != getIntrinsicHeight()) {
                notifyHeightChanged(true);
            }
        }
    }

    public void resetUserExpansion() {
        boolean z = this.mUserExpanded;
        this.mHasUserChangedExpansion = false;
        this.mUserExpanded = false;
        if (z && this.mIsSummaryWithChildren) {
            this.mChildrenContainer.onExpansionChanged();
        }
        updateShelfIconColor();
    }

    public boolean isUserLocked() {
        return this.mUserLocked && !this.mForceUnlocked;
    }

    public void setUserLocked(boolean z) {
        this.mUserLocked = z;
        this.mPrivateLayout.setUserExpanding(z);
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setUserLocked(z);
            if (this.mIsSummaryWithChildren) {
                if (z || !isGroupExpanded()) {
                    updateBackgroundForGroupState();
                }
            }
        }
    }

    public boolean isSystemExpanded() {
        return this.mIsSystemExpanded;
    }

    public void setSystemExpanded(boolean z) {
        if (z != this.mIsSystemExpanded) {
            boolean zIsExpanded = isExpanded();
            this.mIsSystemExpanded = z;
            notifyHeightChanged(false);
            onExpansionChanged(false, zIsExpanded);
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.updateGroupOverflow();
            }
        }
    }

    public void setOnKeyguard(boolean z) {
        if (z != this.mOnKeyguard) {
            boolean zIsAboveShelf = isAboveShelf();
            boolean zIsExpanded = isExpanded();
            this.mOnKeyguard = z;
            onExpansionChanged(false, zIsExpanded);
            if (zIsExpanded != isExpanded()) {
                if (this.mIsSummaryWithChildren) {
                    this.mChildrenContainer.updateGroupOverflow();
                }
                notifyHeightChanged(false);
            }
            if (isAboveShelf() != zIsAboveShelf) {
                this.mAboveShelfChangedListener.onAboveShelfStateChanged(!zIsAboveShelf);
            }
        }
        updateRippleAllowed();
    }

    private void updateRippleAllowed() {
        setRippleAllowed(isOnKeyguard() || this.mEntry.notification.getNotification().contentIntent == null);
    }

    public boolean isClearable() {
        if (this.mStatusBarNotification == null || !this.mStatusBarNotification.isClearable()) {
            return false;
        }
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                if (!notificationChildren.get(i).isClearable()) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public int getIntrinsicHeight() {
        if (isUserLocked()) {
            return getActualHeight();
        }
        if (this.mGuts != null && this.mGuts.isExposed()) {
            return this.mGuts.getIntrinsicHeight();
        }
        if (isChildInGroup() && !isGroupExpanded()) {
            return this.mPrivateLayout.getMinHeight();
        }
        if (this.mSensitive && this.mHideSensitiveForIntrinsicHeight) {
            return getMinHeight();
        }
        if (this.mIsSummaryWithChildren && (!this.mOnKeyguard || this.mShowAmbient)) {
            return this.mChildrenContainer.getIntrinsicHeight();
        }
        if (isHeadsUpAllowed() && (this.mIsHeadsUp || this.mHeadsupDisappearRunning)) {
            if (isPinned() || this.mHeadsupDisappearRunning) {
                return getPinnedHeadsUpHeight(true);
            }
            if (isExpanded()) {
                return Math.max(getMaxExpandHeight(), getHeadsUpHeight());
            }
            return Math.max(getCollapsedHeight(), getHeadsUpHeight());
        }
        if (isExpanded()) {
            return getMaxExpandHeight();
        }
        return getCollapsedHeight();
    }

    private boolean isHeadsUpAllowed() {
        return (this.mOnKeyguard || this.mShowAmbient) ? false : true;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isGroupExpanded() {
        return this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
    }

    private void onChildrenCountChanged() {
        this.mIsSummaryWithChildren = StatusBar.ENABLE_CHILD_NOTIFICATIONS && this.mChildrenContainer != null && this.mChildrenContainer.getNotificationChildCount() > 0;
        if (this.mIsSummaryWithChildren && this.mChildrenContainer.getHeaderView() == null) {
            this.mChildrenContainer.recreateNotificationHeader(this.mExpandClickListener);
        }
        getShowingLayout().updateBackgroundColor(false);
        this.mPrivateLayout.updateExpandButtons(isExpandable());
        updateChildrenHeaderAppearance();
        updateChildrenVisibility();
        applyChildrenRoundness();
    }

    public int getNumUniqueChannels() {
        ArraySet arraySet = new ArraySet();
        arraySet.add(this.mEntry.channel);
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = getNotificationChildren();
            int size = notificationChildren.size();
            for (int i = 0; i < size; i++) {
                ExpandableNotificationRow expandableNotificationRow = notificationChildren.get(i);
                NotificationChannel notificationChannel = expandableNotificationRow.getEntry().channel;
                StatusBarNotification statusBarNotification = expandableNotificationRow.getStatusBarNotification();
                if (statusBarNotification.getUser().equals(this.mStatusBarNotification.getUser()) && statusBarNotification.getPackageName().equals(this.mStatusBarNotification.getPackageName())) {
                    arraySet.add(notificationChannel);
                }
            }
        }
        return arraySet.size();
    }

    public void updateChildrenHeaderAppearance() {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateChildrenHeaderAppearance();
        }
    }

    public boolean isExpanded() {
        return isExpanded(false);
    }

    public boolean isExpanded(boolean z) {
        return (!this.mOnKeyguard || z) && ((!hasUserChangedExpansion() && (isSystemExpanded() || isSystemChildExpanded())) || isUserExpanded());
    }

    private boolean isSystemChildExpanded() {
        return this.mIsSystemChildExpanded;
    }

    public void setSystemChildExpanded(boolean z) {
        this.mIsSystemChildExpanded = z;
    }

    public void setLayoutListener(LayoutListener layoutListener) {
        this.mLayoutListener = layoutListener;
    }

    public void removeListener() {
        this.mLayoutListener = null;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, com.android.systemui.statusbar.ExpandableView, android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int intrinsicHeight = getIntrinsicHeight();
        super.onLayout(z, i, i2, i3, i4);
        if (intrinsicHeight != getIntrinsicHeight()) {
            notifyHeightChanged(true);
        }
        if (this.mMenuRow.getMenuView() != null) {
            this.mMenuRow.onHeightUpdate();
        }
        updateContentShiftHeight();
        if (this.mLayoutListener != null) {
            this.mLayoutListener.onLayout();
        }
    }

    private void updateContentShiftHeight() {
        NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
        if (visibleNotificationHeader != null) {
            CachingIconView icon = visibleNotificationHeader.getIcon();
            this.mIconTransformContentShift = getRelativeTopPadding(icon) + icon.getHeight();
        } else {
            this.mIconTransformContentShift = this.mIconTransformContentShiftNoIcon;
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void notifyHeightChanged(boolean z) {
        super.notifyHeightChanged(z);
        getShowingLayout().requestSelectLayout(z || isUserLocked());
    }

    public void setSensitive(boolean z, boolean z2) {
        this.mSensitive = z;
        this.mSensitiveHiddenInGeneral = z2;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void setHideSensitiveForIntrinsicHeight(boolean z) {
        this.mHideSensitiveForIntrinsicHeight = z;
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).setHideSensitiveForIntrinsicHeight(z);
            }
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public void setHideSensitive(boolean z, boolean z2, long j, long j2) {
        if (getVisibility() == 8) {
            return;
        }
        boolean z3 = this.mShowingPublic;
        this.mShowingPublic = this.mSensitive && z;
        if ((this.mShowingPublicInitialized && this.mShowingPublic == z3) || this.mPublicLayout.getChildCount() == 0) {
            return;
        }
        if (!z2) {
            this.mPublicLayout.animate().cancel();
            this.mPrivateLayout.animate().cancel();
            if (this.mChildrenContainer != null) {
                this.mChildrenContainer.animate().cancel();
                this.mChildrenContainer.setAlpha(1.0f);
            }
            this.mPublicLayout.setAlpha(1.0f);
            this.mPrivateLayout.setAlpha(1.0f);
            this.mPublicLayout.setVisibility(this.mShowingPublic ? 0 : 4);
            updateChildrenVisibility();
        } else {
            animateShowingPublic(j, j2, this.mShowingPublic);
        }
        NotificationContentView showingLayout = getShowingLayout();
        showingLayout.updateBackgroundColor(z2);
        this.mPrivateLayout.updateExpandButtons(isExpandable());
        updateShelfIconColor();
        showingLayout.setDark(isDark(), false, 0L);
        this.mShowingPublicInitialized = true;
    }

    private void animateShowingPublic(long j, long j2, boolean z) {
        View[] viewArr;
        if (this.mIsSummaryWithChildren) {
            viewArr = new View[]{this.mChildrenContainer};
        } else {
            viewArr = new View[]{this.mPrivateLayout};
        }
        View[] viewArr2 = {this.mPublicLayout};
        View[] viewArr3 = z ? viewArr : viewArr2;
        if (z) {
            viewArr = viewArr2;
        }
        for (View view : viewArr3) {
            view.setVisibility(0);
            view.animate().cancel();
            view.animate().alpha(0.0f).setStartDelay(j).setDuration(j2).withEndAction(new Runnable() { // from class: com.android.systemui.statusbar.ExpandableNotificationRow.6
                final /* synthetic */ View val$hiddenView;

                AnonymousClass6(View view2) {
                    view = view2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    view.setVisibility(4);
                }
            });
        }
        for (View view2 : viewArr) {
            view2.setVisibility(0);
            view2.setAlpha(0.0f);
            view2.animate().cancel();
            view2.animate().alpha(1.0f).setStartDelay(j).setDuration(j2);
        }
    }

    /* renamed from: com.android.systemui.statusbar.ExpandableNotificationRow$6 */
    class AnonymousClass6 implements Runnable {
        final /* synthetic */ View val$hiddenView;

        AnonymousClass6(View view2) {
            view = view2;
        }

        @Override // java.lang.Runnable
        public void run() {
            view.setVisibility(4);
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean mustStayOnScreen() {
        return this.mIsHeadsUp && this.mMustStayOnScreen;
    }

    public boolean canViewBeDismissed() {
        return isClearable() && !(shouldShowPublic() && this.mSensitiveHiddenInGeneral);
    }

    private boolean shouldShowPublic() {
        return this.mSensitive && this.mHideSensitiveForIntrinsicHeight;
    }

    public void makeActionsVisibile() {
        setUserExpanded(true, true);
        if (isChildInGroup()) {
            this.mGroupManager.setGroupExpanded(this.mStatusBarNotification, true);
        }
        notifyHeightChanged(false);
    }

    public void setChildrenExpanded(boolean z, boolean z2) {
        this.mChildrenExpanded = z;
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setChildrenExpanded(z);
        }
        updateBackgroundForGroupState();
        updateClickAndFocus();
    }

    public int getMaxExpandHeight() {
        return this.mPrivateLayout.getExpandHeight();
    }

    private int getHeadsUpHeight() {
        return this.mPrivateLayout.getHeadsUpHeight();
    }

    public boolean areGutsExposed() {
        return this.mGuts != null && this.mGuts.isExposed();
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isContentExpandable() {
        if (this.mIsSummaryWithChildren && !shouldShowPublic()) {
            return true;
        }
        return getShowingLayout().isContentExpandable();
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    protected View getContentView() {
        if (this.mIsSummaryWithChildren && !shouldShowPublic()) {
            return this.mChildrenContainer;
        }
        return getShowingLayout();
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    protected void onAppearAnimationFinished(boolean z) {
        super.onAppearAnimationFinished(z);
        if (z) {
            if (this.mChildrenContainer != null) {
                this.mChildrenContainer.setAlpha(1.0f);
                this.mChildrenContainer.setLayerType(0, null);
            }
            for (NotificationContentView notificationContentView : this.mLayouts) {
                notificationContentView.setAlpha(1.0f);
                notificationContentView.setLayerType(0, null);
            }
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public int getExtraBottomPadding() {
        if (this.mIsSummaryWithChildren && isGroupExpanded()) {
            return this.mIncreasedPaddingBetweenElements;
        }
        return 0;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, com.android.systemui.statusbar.ExpandableOutlineView, com.android.systemui.statusbar.ExpandableView
    public void setActualHeight(int i, boolean z) {
        ViewGroup viewGroup;
        boolean z2 = i != getActualHeight();
        super.setActualHeight(i, z);
        if (z2 && isRemoved() && (viewGroup = (ViewGroup) getParent()) != null) {
            viewGroup.invalidate();
        }
        if (this.mGuts != null && this.mGuts.isExposed()) {
            this.mGuts.setActualHeight(i);
            return;
        }
        int iMax = Math.max(getMinHeight(), i);
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setContentHeight(iMax);
        }
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.setActualHeight(i);
        }
        if (this.mGuts != null) {
            this.mGuts.setActualHeight(i);
        }
        if (this.mMenuRow.getMenuView() != null) {
            this.mMenuRow.onHeightUpdate();
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public int getMaxContentHeight() {
        if (this.mIsSummaryWithChildren && !shouldShowPublic()) {
            return this.mChildrenContainer.getMaxContentHeight();
        }
        return getShowingLayout().getMaxHeight();
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public int getMinHeight(boolean z) {
        if (!z && this.mGuts != null && this.mGuts.isExposed()) {
            return this.mGuts.getIntrinsicHeight();
        }
        if (!z && isHeadsUpAllowed() && this.mIsHeadsUp && this.mHeadsUpManager.isTrackingHeadsUp()) {
            return getPinnedHeadsUpHeight(false);
        }
        if (this.mIsSummaryWithChildren && !isGroupExpanded() && !shouldShowPublic()) {
            return this.mChildrenContainer.getMinHeight();
        }
        if (!z && isHeadsUpAllowed() && this.mIsHeadsUp) {
            return getHeadsUpHeight();
        }
        return getShowingLayout().getMinHeight();
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public int getCollapsedHeight() {
        if (this.mIsSummaryWithChildren && !shouldShowPublic()) {
            return this.mChildrenContainer.getCollapsedHeight();
        }
        return getMinHeight();
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, com.android.systemui.statusbar.ExpandableOutlineView, com.android.systemui.statusbar.ExpandableView
    public void setClipTopAmount(int i) {
        super.setClipTopAmount(i);
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setClipTopAmount(i);
        }
        if (this.mGuts != null) {
            this.mGuts.setClipTopAmount(i);
        }
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, com.android.systemui.statusbar.ExpandableOutlineView, com.android.systemui.statusbar.ExpandableView
    public void setClipBottomAmount(int i) {
        if (this.mExpandAnimationRunning) {
            return;
        }
        if (i != this.mClipBottomAmount) {
            super.setClipBottomAmount(i);
            for (NotificationContentView notificationContentView : this.mLayouts) {
                notificationContentView.setClipBottomAmount(i);
            }
            if (this.mGuts != null) {
                this.mGuts.setClipBottomAmount(i);
            }
        }
        if (this.mChildrenContainer != null && !this.mChildIsExpanding) {
            this.mChildrenContainer.setClipBottomAmount(i);
        }
    }

    public NotificationContentView getShowingLayout() {
        return shouldShowPublic() ? this.mPublicLayout : this.mPrivateLayout;
    }

    public void setLegacy(boolean z) {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setLegacy(z);
        }
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    protected void updateBackgroundTint() {
        super.updateBackgroundTint();
        updateBackgroundForGroupState();
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).updateBackgroundForGroupState();
            }
        }
    }

    public void onFinishedExpansionChange() {
        this.mGroupExpansionChanging = false;
        updateBackgroundForGroupState();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v4, resolved type: boolean */
    /* JADX WARN: Multi-variable type inference failed */
    public void updateBackgroundForGroupState() {
        int i = 0;
        if (this.mIsSummaryWithChildren) {
            this.mShowNoBackground = (this.mShowGroupBackgroundWhenExpanded || !isGroupExpanded() || isGroupExpansionChanging() || isUserLocked()) ? false : true;
            this.mChildrenContainer.updateHeaderForExpansion(this.mShowNoBackground);
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            while (i < notificationChildren.size()) {
                notificationChildren.get(i).updateBackgroundForGroupState();
                i++;
            }
        } else if (isChildInGroup()) {
            int backgroundColorForExpansionState = getShowingLayout().getBackgroundColorForExpansionState();
            if (isGroupExpanded() || ((this.mNotificationParent.isGroupExpansionChanging() || this.mNotificationParent.isUserLocked()) && backgroundColorForExpansionState != 0)) {
                i = 1;
            }
            this.mShowNoBackground = i ^ 1;
        } else {
            this.mShowNoBackground = false;
        }
        updateOutline();
        updateBackground();
    }

    public int getPositionOfChild(ExpandableNotificationRow expandableNotificationRow) {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getPositionInLinearLayout(expandableNotificationRow);
        }
        return 0;
    }

    public void setExpansionLogger(ExpansionLogger expansionLogger, String str) {
        this.mLogger = expansionLogger;
        this.mLoggingKey = str;
    }

    public void onExpandedByGesture(boolean z) {
        int i;
        if (this.mGroupManager.isSummaryOfGroup(getStatusBarNotification())) {
            i = 410;
        } else {
            i = 409;
        }
        MetricsLogger.action(this.mContext, i, z);
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public float getIncreasedPaddingAmount() {
        if (this.mIsSummaryWithChildren) {
            if (isGroupExpanded()) {
                return 1.0f;
            }
            if (isUserLocked()) {
                return this.mChildrenContainer.getIncreasedPaddingAmount();
            }
            return 0.0f;
        }
        if (!isColorized()) {
            return 0.0f;
        }
        if (!this.mIsLowPriority || isExpanded()) {
            return -1.0f;
        }
        return 0.0f;
    }

    private boolean isColorized() {
        return this.mIsColorized && this.mBgTint != 0;
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView
    protected boolean disallowSingleClick(MotionEvent motionEvent) {
        if (areGutsExposed()) {
            return false;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
        if (visibleNotificationHeader != null && visibleNotificationHeader.isInTouchRect(x - getTranslation(), y)) {
            return true;
        }
        if ((!this.mIsSummaryWithChildren || shouldShowPublic()) && getShowingLayout().disallowSingleClick(x, y)) {
            return true;
        }
        return super.disallowSingleClick(motionEvent);
    }

    private void onExpansionChanged(boolean z, boolean z2) {
        boolean zIsExpanded = isExpanded();
        if (this.mIsSummaryWithChildren && (!this.mIsLowPriority || z2)) {
            zIsExpanded = this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
        }
        if (zIsExpanded != z2) {
            updateShelfIconColor();
            if (this.mLogger != null) {
                this.mLogger.logNotificationExpansion(this.mLoggingKey, z, zIsExpanded);
            }
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.onExpansionChanged();
            }
        }
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
        if (canViewBeDismissed()) {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS);
        }
        boolean zShouldShowPublic = shouldShowPublic();
        boolean zIsExpanded = false;
        if (!zShouldShowPublic) {
            if (this.mIsSummaryWithChildren) {
                zShouldShowPublic = true;
                if (!this.mIsLowPriority || isExpanded()) {
                    zIsExpanded = isGroupExpanded();
                }
            } else {
                zShouldShowPublic = this.mPrivateLayout.isContentExpandable();
                zIsExpanded = isExpanded();
            }
        }
        if (zShouldShowPublic) {
            if (zIsExpanded) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE);
            } else {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
            }
        }
        NotificationMenuRowPlugin provider = getProvider();
        if (provider != null && provider.getSnoozeMenuItem(getContext()) != null) {
            accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(com.android.systemui.R.id.action_snooze, getContext().getResources().getString(com.android.systemui.R.string.notification_menu_snooze_action)));
        }
    }

    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        if (super.performAccessibilityActionInternal(i, bundle)) {
            return true;
        }
        if (i == 32) {
            doLongClickCallback();
            return true;
        }
        if (i == 262144 || i == 524288) {
            this.mExpandClickListener.onClick(this);
            return true;
        }
        if (i == 1048576) {
            performDismissWithBlockingHelper(true);
            return true;
        }
        if (i == com.android.systemui.R.id.action_snooze) {
            NotificationMenuRowPlugin provider = getProvider();
            if (provider == null) {
                provider = createMenu();
            }
            NotificationMenuRowPlugin.MenuItem snoozeMenuItem = provider.getSnoozeMenuItem(getContext());
            if (snoozeMenuItem != null) {
                doLongClickCallback(getWidth() / 2, getHeight() / 2, snoozeMenuItem);
            }
            return true;
        }
        return false;
    }

    public boolean shouldRefocusOnDismiss() {
        return this.mRefocusOnDismiss || isAccessibilityFocused();
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public ExpandableViewState createNewViewState(StackScrollState stackScrollState) {
        this.mNotificationViewState = new NotificationViewState(stackScrollState);
        return this.mNotificationViewState;
    }

    public NotificationViewState getViewState() {
        return this.mNotificationViewState;
    }

    @Override // com.android.systemui.statusbar.ExpandableView
    public boolean isAboveShelf() {
        return !isOnKeyguard() && (this.mIsPinned || this.mHeadsupDisappearRunning || ((this.mIsHeadsUp && this.mAboveShelf) || this.mExpandAnimationRunning || this.mChildIsExpanding));
    }

    public void setShowAmbient(boolean z) {
        if (z != this.mShowAmbient) {
            this.mShowAmbient = z;
            if (this.mChildrenContainer != null) {
                this.mChildrenContainer.notifyShowAmbientChanged();
            }
            notifyHeightChanged(false);
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableOutlineView
    public boolean topAmountNeedsClipping() {
        if (isGroupExpanded() || isGroupExpansionChanging() || getShowingLayout().shouldClipToRounding(true, false)) {
            return true;
        }
        return (this.mGuts == null || this.mGuts.getAlpha() == 0.0f) ? false : true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2824=4] */
    @Override // com.android.systemui.statusbar.ActivatableNotificationView, com.android.systemui.statusbar.ExpandableOutlineView
    protected boolean childNeedsClipping(View view) {
        if (view instanceof NotificationContentView) {
            NotificationContentView notificationContentView = (NotificationContentView) view;
            if (isClippingNeeded()) {
                return true;
            }
            if (!hasNoRounding()) {
                if (notificationContentView.shouldClipToRounding(getCurrentTopRoundness() != 0.0f, getCurrentBottomRoundness() != 0.0f)) {
                    return true;
                }
            }
        } else if (view == this.mChildrenContainer) {
            if (!this.mChildIsExpanding && (isClippingNeeded() || !hasNoRounding())) {
                return true;
            }
        } else if (view instanceof NotificationGuts) {
            return !hasNoRounding();
        }
        return super.childNeedsClipping(view);
    }

    @Override // com.android.systemui.statusbar.ActivatableNotificationView, com.android.systemui.statusbar.ExpandableOutlineView
    protected void applyRoundness() {
        super.applyRoundness();
        applyChildrenRoundness();
    }

    private void applyChildrenRoundness() {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.setCurrentBottomRoundness(getCurrentBottomRoundness());
        }
    }

    @Override // com.android.systemui.statusbar.ExpandableOutlineView
    public Path getCustomClipPath(View view) {
        if (view instanceof NotificationGuts) {
            return getClipPath(true, false);
        }
        if (view instanceof NotificationChildrenContainer) {
            return getClipPath(false, true);
        }
        return super.getCustomClipPath(view);
    }

    private boolean hasNoRounding() {
        return getCurrentBottomRoundness() == 0.0f && getCurrentTopRoundness() == 0.0f;
    }

    public boolean isShowingAmbient() {
        return this.mShowAmbient;
    }

    public void setAboveShelf(boolean z) {
        boolean zIsAboveShelf = isAboveShelf();
        this.mAboveShelf = z;
        if (isAboveShelf() != zIsAboveShelf) {
            this.mAboveShelfChangedListener.onAboveShelfStateChanged(!zIsAboveShelf);
        }
    }

    public static class NotificationViewState extends ExpandableViewState {
        private final StackScrollState mOverallState;

        /* synthetic */ NotificationViewState(StackScrollState stackScrollState, AnonymousClass1 anonymousClass1) {
            this(stackScrollState);
        }

        private NotificationViewState(StackScrollState stackScrollState) {
            this.mOverallState = stackScrollState;
        }

        @Override // com.android.systemui.statusbar.stack.ExpandableViewState, com.android.systemui.statusbar.stack.ViewState
        public void applyToView(View view) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (expandableNotificationRow.isExpandAnimationRunning()) {
                    return;
                }
                handleFixedTranslationZ(expandableNotificationRow);
                super.applyToView(view);
                expandableNotificationRow.applyChildrenState(this.mOverallState);
            }
        }

        private void handleFixedTranslationZ(ExpandableNotificationRow expandableNotificationRow) {
            if (expandableNotificationRow.hasExpandingChild()) {
                this.zTranslation = expandableNotificationRow.getTranslationZ();
                this.clipTopAmount = expandableNotificationRow.getClipTopAmount();
            }
        }

        @Override // com.android.systemui.statusbar.stack.ViewState
        protected void onYTranslationAnimationFinished(View view) {
            super.onYTranslationAnimationFinished(view);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (expandableNotificationRow.isHeadsUpAnimatingAway()) {
                    expandableNotificationRow.setHeadsUpAnimatingAway(false);
                }
            }
        }

        @Override // com.android.systemui.statusbar.stack.ExpandableViewState, com.android.systemui.statusbar.stack.ViewState
        public void animateTo(View view, AnimationProperties animationProperties) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (expandableNotificationRow.isExpandAnimationRunning()) {
                    return;
                }
                handleFixedTranslationZ(expandableNotificationRow);
                super.animateTo(view, animationProperties);
                expandableNotificationRow.startChildAnimation(this.mOverallState, animationProperties);
            }
        }
    }

    @VisibleForTesting
    protected void setChildrenContainer(NotificationChildrenContainer notificationChildrenContainer) {
        this.mChildrenContainer = notificationChildrenContainer;
    }

    @VisibleForTesting
    protected void setPrivateLayout(NotificationContentView notificationContentView) {
        this.mPrivateLayout = notificationContentView;
    }

    @VisibleForTesting
    protected void setPublicLayout(NotificationContentView notificationContentView) {
        this.mPublicLayout = notificationContentView;
    }

    private class SystemNotificationAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private SystemNotificationAsyncTask() {
        }

        /* synthetic */ SystemNotificationAsyncTask(ExpandableNotificationRow expandableNotificationRow, AnonymousClass1 anonymousClass1) {
            this();
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected Boolean doInBackground(Void... voidArr) {
            return ExpandableNotificationRow.isSystemNotification(ExpandableNotificationRow.this.mContext, ExpandableNotificationRow.this.mStatusBarNotification);
        }

        /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onPostExecute(Boolean bool) {
            if (ExpandableNotificationRow.this.mEntry != null) {
                ExpandableNotificationRow.this.mEntry.mIsSystemNotification = bool;
            }
        }
    }
}
