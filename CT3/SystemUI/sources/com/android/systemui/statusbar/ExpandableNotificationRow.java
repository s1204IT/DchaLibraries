package com.android.systemui.statusbar;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Chronometer;
import android.widget.ImageView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.HybridNotificationView;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.stack.NotificationChildrenContainer;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.stack.StackScrollState;
import com.android.systemui.statusbar.stack.StackStateAnimator;
import com.android.systemui.statusbar.stack.StackViewState;
import java.util.ArrayList;
import java.util.List;

public class ExpandableNotificationRow extends ActivatableNotificationView {
    private static final Property<ExpandableNotificationRow, Float> TRANSLATE_CONTENT = new FloatProperty<ExpandableNotificationRow>("translate") {
        @Override
        public void setValue(ExpandableNotificationRow object, float value) {
            object.setTranslation(value);
        }

        @Override
        public Float get(ExpandableNotificationRow object) {
            return Float.valueOf(object.getTranslation());
        }
    };
    private String mAppName;
    private View mChildAfterViewWhenDismissed;
    private NotificationChildrenContainer mChildrenContainer;
    private ViewStub mChildrenContainerStub;
    private boolean mChildrenExpanded;
    private boolean mDismissed;
    private NotificationData.Entry mEntry;
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
    private int mHeadsUpHeight;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHeadsupDisappearRunning;
    private boolean mHideSensitiveForIntrinsicHeight;
    private boolean mIconAnimationRunning;
    private int mIncreasedPaddingBetweenElements;
    private boolean mIsHeadsUp;
    private boolean mIsPinned;
    private boolean mIsSummaryWithChildren;
    private boolean mIsSystemChildExpanded;
    private boolean mIsSystemExpanded;
    private boolean mJustClicked;
    private boolean mKeepInParent;
    private boolean mLastChronometerRunning;
    private ExpansionLogger mLogger;
    private String mLoggingKey;
    private int mMaxExpandHeight;
    private int mMaxHeadsUpHeight;
    private int mMaxHeadsUpHeightLegacy;
    private int mNotificationColor;
    private int mNotificationMaxHeight;
    private int mNotificationMinHeight;
    private int mNotificationMinHeightLegacy;
    private ExpandableNotificationRow mNotificationParent;
    private View.OnClickListener mOnClickListener;
    private OnExpandClickListener mOnExpandClickListener;
    private boolean mOnKeyguard;
    private NotificationContentView mPrivateLayout;
    private NotificationContentView mPublicLayout;
    private boolean mRefocusOnDismiss;
    private boolean mRemoved;
    private boolean mSensitive;
    private boolean mSensitiveHiddenInGeneral;
    private NotificationSettingsIconRow mSettingsIconRow;
    private ViewStub mSettingsIconRowStub;
    private boolean mShowNoBackground;
    private boolean mShowingPublic;
    private boolean mShowingPublicInitialized;
    private StatusBarNotification mStatusBarNotification;
    private Animator mTranslateAnim;
    private ArrayList<View> mTranslateableViews;
    private boolean mUserExpanded;
    private boolean mUserLocked;
    private View mVetoButton;

    public interface ExpansionLogger {
        void logNotificationExpansion(String str, boolean z, boolean z2);
    }

    public interface OnExpandClickListener {
        void onExpandClicked(NotificationData.Entry entry, boolean z);
    }

    @Override
    public boolean isGroupExpansionChanging() {
        if (isChildInGroup()) {
            return this.mNotificationParent.isGroupExpansionChanging();
        }
        return this.mGroupExpansionChanging;
    }

    public void setGroupExpansionChanging(boolean changing) {
        this.mGroupExpansionChanging = changing;
    }

    @Override
    public void setActualHeightAnimating(boolean animating) {
        if (this.mPrivateLayout == null) {
            return;
        }
        this.mPrivateLayout.setContentHeightAnimating(animating);
    }

    public NotificationContentView getPrivateLayout() {
        return this.mPrivateLayout;
    }

    public NotificationContentView getPublicLayout() {
        return this.mPublicLayout;
    }

    public void setIconAnimationRunning(boolean running) {
        setIconAnimationRunning(running, this.mPublicLayout);
        setIconAnimationRunning(running, this.mPrivateLayout);
        if (this.mIsSummaryWithChildren) {
            setIconAnimationRunningForChild(running, this.mChildrenContainer.getHeaderView());
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                ExpandableNotificationRow child = notificationChildren.get(i);
                child.setIconAnimationRunning(running);
            }
        }
        this.mIconAnimationRunning = running;
    }

    private void setIconAnimationRunning(boolean running, NotificationContentView layout) {
        if (layout == null) {
            return;
        }
        View contractedChild = layout.getContractedChild();
        View expandedChild = layout.getExpandedChild();
        View headsUpChild = layout.getHeadsUpChild();
        setIconAnimationRunningForChild(running, contractedChild);
        setIconAnimationRunningForChild(running, expandedChild);
        setIconAnimationRunningForChild(running, headsUpChild);
    }

    private void setIconAnimationRunningForChild(boolean running, View child) {
        if (child == null) {
            return;
        }
        ImageView icon = (ImageView) child.findViewById(R.id.icon);
        setIconRunning(icon, running);
        ImageView rightIcon = (ImageView) child.findViewById(R.id.accessibilityActionShowTooltip);
        setIconRunning(rightIcon, running);
    }

    private void setIconRunning(ImageView imageView, boolean running) {
        if (imageView == null) {
            return;
        }
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof AnimationDrawable) {
            AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
            if (running) {
                animationDrawable.start();
                return;
            } else {
                animationDrawable.stop();
                return;
            }
        }
        if (!(drawable instanceof AnimatedVectorDrawable)) {
            return;
        }
        AnimatedVectorDrawable animationDrawable2 = (AnimatedVectorDrawable) drawable;
        if (running) {
            animationDrawable2.start();
        } else {
            animationDrawable2.stop();
        }
    }

    public void onNotificationUpdated(NotificationData.Entry entry) {
        this.mEntry = entry;
        this.mStatusBarNotification = entry.notification;
        this.mPrivateLayout.onNotificationUpdated(entry);
        this.mPublicLayout.onNotificationUpdated(entry);
        this.mShowingPublicInitialized = false;
        updateNotificationColor();
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.recreateNotificationHeader(this.mExpandClickListener, this.mEntry.notification);
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
    }

    private void updateLimits() {
        updateLimitsForView(this.mPrivateLayout);
        updateLimitsForView(this.mPublicLayout);
    }

    private void updateLimitsForView(NotificationContentView layout) {
        boolean headsUpCustom = false;
        boolean customView = layout.getContractedChild().getId() != 16909232;
        boolean beforeN = this.mEntry.targetSdk < 24;
        int minHeight = (customView && beforeN && !this.mIsSummaryWithChildren) ? this.mNotificationMinHeightLegacy : this.mNotificationMinHeight;
        if (layout.getHeadsUpChild() != null && layout.getHeadsUpChild().getId() != 16909232) {
            headsUpCustom = true;
        }
        int headsUpheight = (headsUpCustom && beforeN) ? this.mMaxHeadsUpHeightLegacy : this.mMaxHeadsUpHeight;
        layout.setHeights(minHeight, headsUpheight, this.mNotificationMaxHeight);
    }

    public StatusBarNotification getStatusBarNotification() {
        return this.mStatusBarNotification;
    }

    public boolean isHeadsUp() {
        return this.mIsHeadsUp;
    }

    public void setHeadsUp(boolean isHeadsUp) {
        int intrinsicBefore = getIntrinsicHeight();
        this.mIsHeadsUp = isHeadsUp;
        this.mPrivateLayout.setHeadsUp(isHeadsUp);
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateGroupOverflow();
        }
        if (intrinsicBefore == getIntrinsicHeight()) {
            return;
        }
        notifyHeightChanged(false);
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
        this.mPrivateLayout.setGroupManager(groupManager);
    }

    public void setRemoteInputController(RemoteInputController r) {
        this.mPrivateLayout.setRemoteInputController(r);
    }

    public void setAppName(String appName) {
        this.mAppName = appName;
        if (this.mSettingsIconRow == null) {
            return;
        }
        this.mSettingsIconRow.setAppName(this.mAppName);
    }

    public void addChildNotification(ExpandableNotificationRow row, int childIndex) {
        if (this.mChildrenContainer == null) {
            this.mChildrenContainerStub.inflate();
        }
        this.mChildrenContainer.addNotification(row, childIndex);
        onChildrenCountChanged();
        row.setIsChildInGroup(true, this);
    }

    public void removeChildNotification(ExpandableNotificationRow row) {
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.removeNotification(row);
        }
        onChildrenCountChanged();
        row.setIsChildInGroup(false, null);
    }

    @Override
    public boolean isChildInGroup() {
        return this.mNotificationParent != null;
    }

    public ExpandableNotificationRow getNotificationParent() {
        return this.mNotificationParent;
    }

    public void setIsChildInGroup(boolean isChildInGroup, ExpandableNotificationRow parent) {
        boolean childInGroup = BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS ? isChildInGroup : false;
        if (!childInGroup) {
            parent = null;
        }
        this.mNotificationParent = parent;
        this.mPrivateLayout.setIsChildInGroup(childInGroup);
        resetBackgroundAlpha();
        updateBackgroundForGroupState();
        updateClickAndFocus();
        if (this.mNotificationParent == null) {
            return;
        }
        this.mNotificationParent.updateBackgroundForGroupState();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == 0 && isChildInGroup() && !isGroupExpanded()) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected boolean handleSlideBack() {
        if (this.mSettingsIconRow != null && this.mSettingsIconRow.isVisible()) {
            animateTranslateNotification(0.0f);
            return true;
        }
        return false;
    }

    @Override
    protected boolean shouldHideBackground() {
        if (super.shouldHideBackground()) {
            return true;
        }
        return this.mShowNoBackground;
    }

    @Override
    public boolean isSummaryWithChildren() {
        return this.mIsSummaryWithChildren;
    }

    @Override
    public boolean areChildrenExpanded() {
        return this.mChildrenExpanded;
    }

    public List<ExpandableNotificationRow> getNotificationChildren() {
        if (this.mChildrenContainer == null) {
            return null;
        }
        return this.mChildrenContainer.getNotificationChildren();
    }

    public boolean applyChildOrder(List<ExpandableNotificationRow> childOrder) {
        if (this.mChildrenContainer != null) {
            return this.mChildrenContainer.applyChildOrder(childOrder);
        }
        return false;
    }

    public void getChildrenStates(StackScrollState resultState) {
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        StackViewState parentState = resultState.getViewStateForView(this);
        this.mChildrenContainer.getState(resultState, parentState);
    }

    public void applyChildrenState(StackScrollState state) {
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        this.mChildrenContainer.applyState(state);
    }

    public void prepareExpansionChanged(StackScrollState state) {
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        this.mChildrenContainer.prepareExpansionChanged(state);
    }

    public void startChildAnimation(StackScrollState finalState, StackStateAnimator stateAnimator, long delay, long duration) {
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        this.mChildrenContainer.startAnimationToState(finalState, stateAnimator, delay, duration);
    }

    public ExpandableNotificationRow getViewAtPosition(float y) {
        ExpandableNotificationRow view;
        return (this.mIsSummaryWithChildren && this.mChildrenExpanded && (view = this.mChildrenContainer.getViewAtPosition(y)) != null) ? view : this;
    }

    public NotificationGuts getGuts() {
        return this.mGuts;
    }

    public void setPinned(boolean pinned) {
        int intrinsicHeight = getIntrinsicHeight();
        this.mIsPinned = pinned;
        if (intrinsicHeight != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
        if (pinned) {
            setIconAnimationRunning(true);
            this.mExpandedWhenPinned = false;
        } else if (this.mExpandedWhenPinned) {
            setUserExpanded(true);
        }
        setChronometerRunning(this.mLastChronometerRunning);
    }

    public boolean isPinned() {
        return this.mIsPinned;
    }

    public int getPinnedHeadsUpHeight(boolean atLeastMinHeight) {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getIntrinsicHeight();
        }
        if (this.mExpandedWhenPinned) {
            return Math.max(getMaxExpandHeight(), this.mHeadsUpHeight);
        }
        if (atLeastMinHeight) {
            return Math.max(getCollapsedHeight(), this.mHeadsUpHeight);
        }
        return this.mHeadsUpHeight;
    }

    public void setJustClicked(boolean justClicked) {
        this.mJustClicked = justClicked;
    }

    public boolean wasJustClicked() {
        return this.mJustClicked;
    }

    public void setChronometerRunning(boolean running) {
        this.mLastChronometerRunning = running;
        setChronometerRunning(running, this.mPrivateLayout);
        setChronometerRunning(running, this.mPublicLayout);
        if (this.mChildrenContainer == null) {
            return;
        }
        List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
        for (int i = 0; i < notificationChildren.size(); i++) {
            ExpandableNotificationRow child = notificationChildren.get(i);
            child.setChronometerRunning(running);
        }
    }

    private void setChronometerRunning(boolean running, NotificationContentView layout) {
        if (layout == null) {
            return;
        }
        boolean running2 = !running ? isPinned() : true;
        View contractedChild = layout.getContractedChild();
        View expandedChild = layout.getExpandedChild();
        View headsUpChild = layout.getHeadsUpChild();
        setChronometerRunningForChild(running2, contractedChild);
        setChronometerRunningForChild(running2, expandedChild);
        setChronometerRunningForChild(running2, headsUpChild);
    }

    private void setChronometerRunningForChild(boolean running, View child) {
        if (child == null) {
            return;
        }
        View chronometer = child.findViewById(R.id.label_error);
        if (!(chronometer instanceof Chronometer)) {
            return;
        }
        ((Chronometer) chronometer).setStarted(running);
    }

    public NotificationHeaderView getNotificationHeader() {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getHeaderView();
        }
        return this.mPrivateLayout.getNotificationHeader();
    }

    private NotificationHeaderView getVisibleNotificationHeader() {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getHeaderView();
        }
        return getShowingLayout().getVisibleNotificationHeader();
    }

    public void setOnExpandClickListener(OnExpandClickListener onExpandClickListener) {
        this.mOnExpandClickListener = onExpandClickListener;
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        super.setOnClickListener(l);
        this.mOnClickListener = l;
        updateClickAndFocus();
    }

    private void updateClickAndFocus() {
        boolean zIsGroupExpanded = isChildInGroup() ? isGroupExpanded() : true;
        boolean z = this.mOnClickListener != null ? zIsGroupExpanded : false;
        if (isFocusable() != zIsGroupExpanded) {
            setFocusable(zIsGroupExpanded);
        }
        if (isClickable() == z) {
            return;
        }
        setClickable(z);
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void reInflateViews() {
        initDimens();
        if (this.mIsSummaryWithChildren && this.mChildrenContainer != null) {
            this.mChildrenContainer.reInflateViews(this.mExpandClickListener, this.mEntry.notification);
        }
        if (this.mGuts != null) {
            View oldGuts = this.mGuts;
            int index = indexOfChild(oldGuts);
            removeView(oldGuts);
            this.mGuts = (NotificationGuts) LayoutInflater.from(this.mContext).inflate(com.android.systemui.R.layout.notification_guts, (ViewGroup) this, false);
            this.mGuts.setVisibility(oldGuts.getVisibility());
            addView(this.mGuts, index);
        }
        if (this.mSettingsIconRow != null) {
            View oldSettings = this.mSettingsIconRow;
            int settingsIndex = indexOfChild(oldSettings);
            removeView(oldSettings);
            this.mSettingsIconRow = (NotificationSettingsIconRow) LayoutInflater.from(this.mContext).inflate(com.android.systemui.R.layout.notification_settings_icon_row, (ViewGroup) this, false);
            this.mSettingsIconRow.setNotificationRowParent(this);
            this.mSettingsIconRow.setAppName(this.mAppName);
            this.mSettingsIconRow.setVisibility(oldSettings.getVisibility());
            addView(this.mSettingsIconRow, settingsIndex);
        }
        this.mPrivateLayout.reInflateViews();
        this.mPublicLayout.reInflateViews();
    }

    public void setContentBackground(int customBackgroundColor, boolean animate, NotificationContentView notificationContentView) {
        if (getShowingLayout() != notificationContentView) {
            return;
        }
        setTintColor(customBackgroundColor, animate);
    }

    public void closeRemoteInput() {
        this.mPrivateLayout.closeRemoteInput();
        this.mPublicLayout.closeRemoteInput();
    }

    public void setSingleLineWidthIndention(int indention) {
        this.mPrivateLayout.setSingleLineWidthIndention(indention);
    }

    public int getNotificationColor() {
        return this.mNotificationColor;
    }

    private void updateNotificationColor() {
        this.mNotificationColor = NotificationColorUtil.resolveContrastColor(this.mContext, getStatusBarNotification().getNotification().color);
    }

    public HybridNotificationView getSingleLineView() {
        return this.mPrivateLayout.getSingleLineView();
    }

    public boolean isOnKeyguard() {
        return this.mOnKeyguard;
    }

    public void removeAllChildren() {
        List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
        ArrayList<ExpandableNotificationRow> clonedList = new ArrayList<>(notificationChildren);
        for (int i = 0; i < clonedList.size(); i++) {
            ExpandableNotificationRow row = clonedList.get(i);
            if (!row.keepInParent()) {
                this.mChildrenContainer.removeNotification(row);
                row.setIsChildInGroup(false, null);
            }
        }
        onChildrenCountChanged();
    }

    public void setForceUnlocked(boolean forceUnlocked) {
        this.mForceUnlocked = forceUnlocked;
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        List<ExpandableNotificationRow> notificationChildren = getNotificationChildren();
        for (ExpandableNotificationRow child : notificationChildren) {
            child.setForceUnlocked(forceUnlocked);
        }
    }

    public void setDismissed(boolean dismissed, boolean fromAccessibility) {
        List<ExpandableNotificationRow> notificationChildren;
        int i;
        this.mDismissed = dismissed;
        this.mGroupParentWhenDismissed = this.mNotificationParent;
        this.mRefocusOnDismiss = fromAccessibility;
        this.mChildAfterViewWhenDismissed = null;
        if (!isChildInGroup() || (i = (notificationChildren = this.mNotificationParent.getNotificationChildren()).indexOf(this)) == -1 || i >= notificationChildren.size() - 1) {
            return;
        }
        this.mChildAfterViewWhenDismissed = notificationChildren.get(i + 1);
    }

    public boolean isDismissed() {
        return this.mDismissed;
    }

    public boolean keepInParent() {
        return this.mKeepInParent;
    }

    public void setKeepInParent(boolean keepInParent) {
        this.mKeepInParent = keepInParent;
    }

    public boolean isRemoved() {
        return this.mRemoved;
    }

    public void setRemoved() {
        this.mRemoved = true;
        this.mPrivateLayout.setRemoved();
    }

    public NotificationChildrenContainer getChildrenContainer() {
        return this.mChildrenContainer;
    }

    public void setHeadsupDisappearRunning(boolean running) {
        this.mHeadsupDisappearRunning = running;
        this.mPrivateLayout.setHeadsupDisappearRunning(running);
    }

    public View getChildAfterViewWhenDismissed() {
        return this.mChildAfterViewWhenDismissed;
    }

    public View getGroupParentWhenDismissed() {
        return this.mGroupParentWhenDismissed;
    }

    public void performDismiss() {
        this.mVetoButton.performClick();
    }

    public void setOnDismissListener(View.OnClickListener listener) {
        this.mVetoButton.setOnClickListener(listener);
    }

    public ExpandableNotificationRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLastChronometerRunning = true;
        this.mExpandClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean nowExpanded;
                if (!ExpandableNotificationRow.this.mShowingPublic && ExpandableNotificationRow.this.mGroupManager.isSummaryOfGroup(ExpandableNotificationRow.this.mStatusBarNotification)) {
                    ExpandableNotificationRow.this.mGroupExpansionChanging = true;
                    boolean wasExpanded = ExpandableNotificationRow.this.mGroupManager.isGroupExpanded(ExpandableNotificationRow.this.mStatusBarNotification);
                    boolean nowExpanded2 = ExpandableNotificationRow.this.mGroupManager.toggleGroupExpansion(ExpandableNotificationRow.this.mStatusBarNotification);
                    ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, nowExpanded2);
                    MetricsLogger.action(ExpandableNotificationRow.this.mContext, 408, nowExpanded2);
                    ExpandableNotificationRow.this.logExpansionEvent(true, wasExpanded);
                    return;
                }
                if (v.isAccessibilityFocused()) {
                    ExpandableNotificationRow.this.mPrivateLayout.setFocusOnVisibilityChange();
                }
                if (ExpandableNotificationRow.this.isPinned()) {
                    nowExpanded = !ExpandableNotificationRow.this.mExpandedWhenPinned;
                    ExpandableNotificationRow.this.mExpandedWhenPinned = nowExpanded;
                } else {
                    nowExpanded = !ExpandableNotificationRow.this.isExpanded();
                    ExpandableNotificationRow.this.setUserExpanded(nowExpanded);
                }
                ExpandableNotificationRow.this.notifyHeightChanged(true);
                ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, nowExpanded);
                MetricsLogger.action(ExpandableNotificationRow.this.mContext, 407, nowExpanded);
            }
        };
        this.mFalsingManager = FalsingManager.getInstance(context);
        initDimens();
    }

    private void initDimens() {
        this.mNotificationMinHeightLegacy = getFontScaledHeight(com.android.systemui.R.dimen.notification_min_height_legacy);
        this.mNotificationMinHeight = getFontScaledHeight(com.android.systemui.R.dimen.notification_min_height);
        this.mNotificationMaxHeight = getFontScaledHeight(com.android.systemui.R.dimen.notification_max_height);
        this.mMaxHeadsUpHeightLegacy = getFontScaledHeight(com.android.systemui.R.dimen.notification_max_heads_up_height_legacy);
        this.mMaxHeadsUpHeight = getFontScaledHeight(com.android.systemui.R.dimen.notification_max_heads_up_height);
        this.mIncreasedPaddingBetweenElements = getResources().getDimensionPixelSize(com.android.systemui.R.dimen.notification_divider_height_increased);
    }

    private int getFontScaledHeight(int dimenId) {
        int dimensionPixelSize = getResources().getDimensionPixelSize(dimenId);
        float factor = Math.max(1.0f, getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density);
        return (int) (dimensionPixelSize * factor);
    }

    @Override
    public void reset() {
        super.reset();
        boolean wasExpanded = isExpanded();
        this.mExpandable = false;
        this.mHasUserChangedExpansion = false;
        this.mUserLocked = false;
        this.mShowingPublic = false;
        this.mSensitive = false;
        this.mShowingPublicInitialized = false;
        this.mIsSystemExpanded = false;
        this.mOnKeyguard = false;
        this.mPublicLayout.reset();
        this.mPrivateLayout.reset();
        resetHeight();
        resetTranslation();
        logExpansionEvent(false, wasExpanded);
    }

    public void resetHeight() {
        this.mMaxExpandHeight = 0;
        this.mHeadsUpHeight = 0;
        onHeightReset();
        requestLayout();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPublicLayout = (NotificationContentView) findViewById(com.android.systemui.R.id.expandedPublic);
        this.mPublicLayout.setContainingNotification(this);
        this.mPrivateLayout = (NotificationContentView) findViewById(com.android.systemui.R.id.expanded);
        this.mPrivateLayout.setExpandClickListener(this.mExpandClickListener);
        this.mPrivateLayout.setContainingNotification(this);
        this.mPublicLayout.setExpandClickListener(this.mExpandClickListener);
        this.mSettingsIconRowStub = (ViewStub) findViewById(com.android.systemui.R.id.settings_icon_row_stub);
        this.mSettingsIconRowStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                ExpandableNotificationRow.this.mSettingsIconRow = (NotificationSettingsIconRow) inflated;
                ExpandableNotificationRow.this.mSettingsIconRow.setNotificationRowParent(ExpandableNotificationRow.this);
                ExpandableNotificationRow.this.mSettingsIconRow.setAppName(ExpandableNotificationRow.this.mAppName);
            }
        });
        this.mGutsStub = (ViewStub) findViewById(com.android.systemui.R.id.notification_guts_stub);
        this.mGutsStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                ExpandableNotificationRow.this.mGuts = (NotificationGuts) inflated;
                ExpandableNotificationRow.this.mGuts.setClipTopAmount(ExpandableNotificationRow.this.getClipTopAmount());
                ExpandableNotificationRow.this.mGuts.setActualHeight(ExpandableNotificationRow.this.getActualHeight());
                ExpandableNotificationRow.this.mGutsStub = null;
            }
        });
        this.mChildrenContainerStub = (ViewStub) findViewById(com.android.systemui.R.id.child_container_stub);
        this.mChildrenContainerStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                ExpandableNotificationRow.this.mChildrenContainer = (NotificationChildrenContainer) inflated;
                ExpandableNotificationRow.this.mChildrenContainer.setNotificationParent(ExpandableNotificationRow.this);
                ExpandableNotificationRow.this.mChildrenContainer.onNotificationUpdated();
                ExpandableNotificationRow.this.mTranslateableViews.add(ExpandableNotificationRow.this.mChildrenContainer);
            }
        });
        this.mVetoButton = findViewById(com.android.systemui.R.id.veto);
        this.mVetoButton.setImportantForAccessibility(2);
        this.mVetoButton.setContentDescription(this.mContext.getString(com.android.systemui.R.string.accessibility_remove_notification));
        this.mTranslateableViews = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            this.mTranslateableViews.add(getChildAt(i));
        }
        this.mTranslateableViews.remove(this.mVetoButton);
        this.mTranslateableViews.remove(this.mSettingsIconRowStub);
        this.mTranslateableViews.remove(this.mChildrenContainerStub);
        this.mTranslateableViews.remove(this.mGutsStub);
    }

    public void resetTranslation() {
        if (this.mTranslateableViews != null) {
            for (int i = 0; i < this.mTranslateableViews.size(); i++) {
                this.mTranslateableViews.get(i).setTranslationX(0.0f);
            }
        }
        invalidateOutline();
        if (this.mSettingsIconRow == null) {
            return;
        }
        this.mSettingsIconRow.resetState();
    }

    public void animateTranslateNotification(float leftTarget) {
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.cancel();
        }
        this.mTranslateAnim = getTranslateViewAnimator(leftTarget, null);
        if (this.mTranslateAnim == null) {
            return;
        }
        this.mTranslateAnim.start();
    }

    @Override
    public void setTranslation(float translationX) {
        if (areGutsExposed()) {
            return;
        }
        for (int i = 0; i < this.mTranslateableViews.size(); i++) {
            if (this.mTranslateableViews.get(i) != null) {
                this.mTranslateableViews.get(i).setTranslationX(translationX);
            }
        }
        invalidateOutline();
        if (this.mSettingsIconRow == null) {
            return;
        }
        this.mSettingsIconRow.updateSettingsIcons(translationX, getMeasuredWidth());
    }

    @Override
    public float getTranslation() {
        if (this.mTranslateableViews != null && this.mTranslateableViews.size() > 0) {
            return this.mTranslateableViews.get(0).getTranslationX();
        }
        return 0.0f;
    }

    public Animator getTranslateViewAnimator(final float leftTarget, ValueAnimator.AnimatorUpdateListener listener) {
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.cancel();
        }
        if (areGutsExposed()) {
            return null;
        }
        ObjectAnimator translateAnim = ObjectAnimator.ofFloat(this, TRANSLATE_CONTENT, leftTarget);
        if (listener != null) {
            translateAnim.addUpdateListener(listener);
        }
        translateAnim.addListener(new AnimatorListenerAdapter() {
            boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator anim) {
                this.cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator anim) {
                if (this.cancelled || ExpandableNotificationRow.this.mSettingsIconRow == null || leftTarget != 0.0f) {
                    return;
                }
                ExpandableNotificationRow.this.mSettingsIconRow.resetState();
                ExpandableNotificationRow.this.mTranslateAnim = null;
            }
        });
        this.mTranslateAnim = translateAnim;
        return translateAnim;
    }

    public float getSpaceForGear() {
        if (this.mSettingsIconRow != null) {
            return this.mSettingsIconRow.getSpaceForGear();
        }
        return 0.0f;
    }

    public NotificationSettingsIconRow getSettingsRow() {
        if (this.mSettingsIconRow == null) {
            this.mSettingsIconRowStub.inflate();
        }
        return this.mSettingsIconRow;
    }

    public void inflateGuts() {
        if (this.mGuts != null) {
            return;
        }
        this.mGutsStub.inflate();
    }

    private void updateChildrenVisibility() {
        this.mPrivateLayout.setVisibility((this.mShowingPublic || this.mIsSummaryWithChildren) ? 4 : 0);
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setVisibility((this.mShowingPublic || !this.mIsSummaryWithChildren) ? 4 : 0);
            this.mChildrenContainer.updateHeaderVisibility((this.mShowingPublic || !this.mIsSummaryWithChildren) ? 4 : 0);
        }
        updateLimits();
    }

    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (super.onRequestSendAccessibilityEventInternal(child, event)) {
            AccessibilityEvent record = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(record);
            dispatchPopulateAccessibilityEvent(record);
            event.appendRecord(record);
            return true;
        }
        return false;
    }

    @Override
    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        NotificationContentView showing = getShowingLayout();
        if (showing != null) {
            showing.setDark(dark, fade, delay);
        }
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        this.mChildrenContainer.setDark(dark, fade, delay);
    }

    public boolean isExpandable() {
        if (!this.mIsSummaryWithChildren || this.mShowingPublic) {
            return this.mExpandable;
        }
        return !this.mChildrenExpanded;
    }

    public void setExpandable(boolean expandable) {
        this.mExpandable = expandable;
        this.mPrivateLayout.updateExpandButtons(isExpandable());
    }

    @Override
    public void setClipToActualHeight(boolean clipToActualHeight) {
        super.setClipToActualHeight(!clipToActualHeight ? isUserLocked() : true);
        getShowingLayout().setClipToActualHeight(clipToActualHeight ? true : isUserLocked());
    }

    public boolean hasUserChangedExpansion() {
        return this.mHasUserChangedExpansion;
    }

    public boolean isUserExpanded() {
        return this.mUserExpanded;
    }

    public void setUserExpanded(boolean userExpanded) {
        setUserExpanded(userExpanded, false);
    }

    public void setUserExpanded(boolean userExpanded, boolean allowChildExpansion) {
        this.mFalsingManager.setNotificationExpanded();
        if (this.mIsSummaryWithChildren && !this.mShowingPublic && allowChildExpansion) {
            boolean wasExpanded = this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
            this.mGroupManager.setGroupExpanded(this.mStatusBarNotification, userExpanded);
            logExpansionEvent(true, wasExpanded);
        } else {
            if (userExpanded && !this.mExpandable) {
                return;
            }
            boolean wasExpanded2 = isExpanded();
            this.mHasUserChangedExpansion = true;
            this.mUserExpanded = userExpanded;
            logExpansionEvent(true, wasExpanded2);
        }
    }

    public void resetUserExpansion() {
        this.mHasUserChangedExpansion = false;
        this.mUserExpanded = false;
    }

    public boolean isUserLocked() {
        return this.mUserLocked && !this.mForceUnlocked;
    }

    public void setUserLocked(boolean userLocked) {
        this.mUserLocked = userLocked;
        this.mPrivateLayout.setUserExpanding(userLocked);
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        this.mChildrenContainer.setUserLocked(userLocked);
        if (!userLocked && (userLocked || isGroupExpanded())) {
            return;
        }
        updateBackgroundForGroupState();
    }

    public boolean isSystemExpanded() {
        return this.mIsSystemExpanded;
    }

    public void setSystemExpanded(boolean expand) {
        if (expand == this.mIsSystemExpanded) {
            return;
        }
        boolean wasExpanded = isExpanded();
        this.mIsSystemExpanded = expand;
        notifyHeightChanged(false);
        logExpansionEvent(false, wasExpanded);
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        this.mChildrenContainer.updateGroupOverflow();
    }

    public void setOnKeyguard(boolean onKeyguard) {
        if (onKeyguard == this.mOnKeyguard) {
            return;
        }
        boolean wasExpanded = isExpanded();
        this.mOnKeyguard = onKeyguard;
        logExpansionEvent(false, wasExpanded);
        if (wasExpanded == isExpanded()) {
            return;
        }
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateGroupOverflow();
        }
        notifyHeightChanged(false);
    }

    public boolean isClearable() {
        if (this.mStatusBarNotification == null || !this.mStatusBarNotification.isClearable()) {
            return false;
        }
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                ExpandableNotificationRow child = notificationChildren.get(i);
                if (!child.isClearable()) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public int getIntrinsicHeight() {
        if (isUserLocked()) {
            return getActualHeight();
        }
        if (this.mGuts != null && this.mGuts.areGutsExposed()) {
            return this.mGuts.getHeight();
        }
        if (isChildInGroup() && !isGroupExpanded()) {
            return this.mPrivateLayout.getMinHeight();
        }
        if (this.mSensitive && this.mHideSensitiveForIntrinsicHeight) {
            return getMinHeight();
        }
        if (this.mIsSummaryWithChildren && !this.mOnKeyguard) {
            return this.mChildrenContainer.getIntrinsicHeight();
        }
        if (this.mIsHeadsUp || this.mHeadsupDisappearRunning) {
            if (isPinned() || this.mHeadsupDisappearRunning) {
                return getPinnedHeadsUpHeight(true);
            }
            if (isExpanded()) {
                return Math.max(getMaxExpandHeight(), this.mHeadsUpHeight);
            }
            return Math.max(getCollapsedHeight(), this.mHeadsUpHeight);
        }
        if (isExpanded()) {
            return getMaxExpandHeight();
        }
        return getCollapsedHeight();
    }

    @Override
    public boolean isGroupExpanded() {
        return this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
    }

    private void onChildrenCountChanged() {
        boolean z = BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS && this.mChildrenContainer != null && this.mChildrenContainer.getNotificationChildCount() > 0;
        this.mIsSummaryWithChildren = z;
        if (this.mIsSummaryWithChildren && this.mChildrenContainer.getHeaderView() == null) {
            this.mChildrenContainer.recreateNotificationHeader(this.mExpandClickListener, this.mEntry.notification);
        }
        getShowingLayout().updateBackgroundColor(false);
        this.mPrivateLayout.updateExpandButtons(isExpandable());
        updateChildrenHeaderAppearance();
        updateChildrenVisibility();
    }

    public void updateChildrenHeaderAppearance() {
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        this.mChildrenContainer.updateChildrenHeaderAppearance();
    }

    public boolean isExpanded() {
        return isExpanded(false);
    }

    public boolean isExpanded(boolean allowOnKeyguard) {
        if (this.mOnKeyguard && !allowOnKeyguard) {
            return false;
        }
        if (!hasUserChangedExpansion() && (isSystemExpanded() || isSystemChildExpanded())) {
            return true;
        }
        return isUserExpanded();
    }

    private boolean isSystemChildExpanded() {
        return this.mIsSystemChildExpanded;
    }

    public void setSystemChildExpanded(boolean expanded) {
        this.mIsSystemChildExpanded = expanded;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateMaxHeights();
        if (this.mSettingsIconRow == null) {
            return;
        }
        this.mSettingsIconRow.updateVerticalLocation();
    }

    private void updateMaxHeights() {
        int intrinsicBefore = getIntrinsicHeight();
        View expandedChild = this.mPrivateLayout.getExpandedChild();
        if (expandedChild == null) {
            expandedChild = this.mPrivateLayout.getContractedChild();
        }
        this.mMaxExpandHeight = expandedChild.getHeight();
        View headsUpChild = this.mPrivateLayout.getHeadsUpChild();
        if (headsUpChild == null) {
            headsUpChild = this.mPrivateLayout.getContractedChild();
        }
        this.mHeadsUpHeight = headsUpChild.getHeight();
        if (intrinsicBefore == getIntrinsicHeight()) {
            return;
        }
        notifyHeightChanged(false);
    }

    @Override
    public void notifyHeightChanged(boolean needsAnimation) {
        super.notifyHeightChanged(needsAnimation);
        getShowingLayout().requestSelectLayout(!needsAnimation ? isUserLocked() : true);
    }

    public void setSensitive(boolean sensitive, boolean hideSensitive) {
        this.mSensitive = sensitive;
        this.mSensitiveHiddenInGeneral = hideSensitive;
    }

    @Override
    public void setHideSensitiveForIntrinsicHeight(boolean hideSensitive) {
        this.mHideSensitiveForIntrinsicHeight = hideSensitive;
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
        for (int i = 0; i < notificationChildren.size(); i++) {
            ExpandableNotificationRow child = notificationChildren.get(i);
            child.setHideSensitiveForIntrinsicHeight(hideSensitive);
        }
    }

    @Override
    public void setHideSensitive(boolean hideSensitive, boolean animated, long delay, long duration) {
        boolean oldShowingPublic = this.mShowingPublic;
        if (!this.mSensitive) {
            hideSensitive = false;
        }
        this.mShowingPublic = hideSensitive;
        if ((this.mShowingPublicInitialized && this.mShowingPublic == oldShowingPublic) || this.mPublicLayout.getChildCount() == 0) {
            return;
        }
        if (!animated) {
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
            animateShowingPublic(delay, duration);
        }
        NotificationContentView showingLayout = getShowingLayout();
        showingLayout.updateBackgroundColor(animated);
        this.mPrivateLayout.updateExpandButtons(isExpandable());
        this.mShowingPublicInitialized = true;
    }

    private void animateShowingPublic(long delay, long duration) {
        View[] privateViews;
        if (this.mIsSummaryWithChildren) {
            privateViews = new View[]{this.mChildrenContainer};
        } else {
            privateViews = new View[]{this.mPrivateLayout};
        }
        View[] publicViews = {this.mPublicLayout};
        View[] hiddenChildren = this.mShowingPublic ? privateViews : publicViews;
        View[] shownChildren = this.mShowingPublic ? publicViews : privateViews;
        for (final View hiddenView : hiddenChildren) {
            hiddenView.setVisibility(0);
            hiddenView.animate().cancel();
            hiddenView.animate().alpha(0.0f).setStartDelay(delay).setDuration(duration).withEndAction(new Runnable() {
                @Override
                public void run() {
                    hiddenView.setVisibility(4);
                }
            });
        }
        for (View showView : shownChildren) {
            showView.setVisibility(0);
            showView.setAlpha(0.0f);
            showView.animate().cancel();
            showView.animate().alpha(1.0f).setStartDelay(delay).setDuration(duration);
        }
    }

    @Override
    public boolean mustStayOnScreen() {
        return this.mIsHeadsUp;
    }

    public boolean canViewBeDismissed() {
        return isClearable() && !(this.mShowingPublic && this.mSensitiveHiddenInGeneral);
    }

    public void makeActionsVisibile() {
        setUserExpanded(true, true);
        if (isChildInGroup()) {
            this.mGroupManager.setGroupExpanded(this.mStatusBarNotification, true);
        }
        notifyHeightChanged(false);
    }

    public void setChildrenExpanded(boolean expanded, boolean animate) {
        this.mChildrenExpanded = expanded;
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setChildrenExpanded(expanded);
        }
        updateBackgroundForGroupState();
        updateClickAndFocus();
    }

    public int getMaxExpandHeight() {
        return this.mMaxExpandHeight;
    }

    public boolean areGutsExposed() {
        if (this.mGuts != null) {
            return this.mGuts.areGutsExposed();
        }
        return false;
    }

    @Override
    public boolean isContentExpandable() {
        NotificationContentView showingLayout = getShowingLayout();
        return showingLayout.isContentExpandable();
    }

    @Override
    protected View getContentView() {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer;
        }
        return getShowingLayout();
    }

    @Override
    public int getExtraBottomPadding() {
        if (this.mIsSummaryWithChildren && isGroupExpanded()) {
            return this.mIncreasedPaddingBetweenElements;
        }
        return 0;
    }

    @Override
    public void setActualHeight(int height, boolean notifyListeners) {
        super.setActualHeight(height, notifyListeners);
        if (this.mGuts != null && this.mGuts.areGutsExposed()) {
            this.mGuts.setActualHeight(height);
            return;
        }
        int contentHeight = Math.max(getMinHeight(), height);
        this.mPrivateLayout.setContentHeight(contentHeight);
        this.mPublicLayout.setContentHeight(contentHeight);
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.setActualHeight(height);
        }
        if (this.mGuts == null) {
            return;
        }
        this.mGuts.setActualHeight(height);
    }

    @Override
    public int getMaxContentHeight() {
        if (this.mIsSummaryWithChildren && !this.mShowingPublic) {
            return this.mChildrenContainer.getMaxContentHeight();
        }
        NotificationContentView showingLayout = getShowingLayout();
        return showingLayout.getMaxHeight();
    }

    @Override
    public int getMinHeight() {
        if (this.mIsHeadsUp && this.mHeadsUpManager.isTrackingHeadsUp()) {
            return getPinnedHeadsUpHeight(false);
        }
        if (this.mIsSummaryWithChildren && !isGroupExpanded() && !this.mShowingPublic) {
            return this.mChildrenContainer.getMinHeight();
        }
        if (this.mIsHeadsUp) {
            return this.mHeadsUpHeight;
        }
        NotificationContentView showingLayout = getShowingLayout();
        return showingLayout.getMinHeight();
    }

    @Override
    public int getCollapsedHeight() {
        if (this.mIsSummaryWithChildren && !this.mShowingPublic) {
            return this.mChildrenContainer.getCollapsedHeight();
        }
        return getMinHeight();
    }

    @Override
    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        this.mPrivateLayout.setClipTopAmount(clipTopAmount);
        this.mPublicLayout.setClipTopAmount(clipTopAmount);
        if (this.mGuts == null) {
            return;
        }
        this.mGuts.setClipTopAmount(clipTopAmount);
    }

    public NotificationContentView getShowingLayout() {
        return this.mShowingPublic ? this.mPublicLayout : this.mPrivateLayout;
    }

    @Override
    public void setShowingLegacyBackground(boolean showing) {
        super.setShowingLegacyBackground(showing);
        this.mPrivateLayout.setShowingLegacyBackground(showing);
        this.mPublicLayout.setShowingLegacyBackground(showing);
    }

    @Override
    protected void updateBackgroundTint() {
        super.updateBackgroundTint();
        updateBackgroundForGroupState();
        if (!this.mIsSummaryWithChildren) {
            return;
        }
        List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
        for (int i = 0; i < notificationChildren.size(); i++) {
            ExpandableNotificationRow child = notificationChildren.get(i);
            child.updateBackgroundForGroupState();
        }
    }

    public void onFinishedExpansionChange() {
        this.mGroupExpansionChanging = false;
        updateBackgroundForGroupState();
    }

    public void updateBackgroundForGroupState() {
        boolean showBackground;
        if (this.mIsSummaryWithChildren) {
            if (isGroupExpanded() && !isGroupExpansionChanging() && !isUserLocked()) {
                z = true;
            }
            this.mShowNoBackground = z;
            this.mChildrenContainer.updateHeaderForExpansion(this.mShowNoBackground);
            List<ExpandableNotificationRow> children = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < children.size(); i++) {
                children.get(i).updateBackgroundForGroupState();
            }
        } else if (isChildInGroup()) {
            int childColor = getShowingLayout().getBackgroundColorForExpansionState();
            if (isGroupExpanded()) {
                showBackground = true;
            } else {
                showBackground = (this.mNotificationParent.isGroupExpansionChanging() || this.mNotificationParent.isUserLocked()) && childColor != 0;
            }
            this.mShowNoBackground = showBackground ? false : true;
        } else {
            this.mShowNoBackground = false;
        }
        updateOutline();
        updateBackground();
    }

    public int getPositionOfChild(ExpandableNotificationRow childRow) {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getPositionInLinearLayout(childRow);
        }
        return 0;
    }

    public void setExpansionLogger(ExpansionLogger logger, String key) {
        this.mLogger = logger;
        this.mLoggingKey = key;
    }

    public void onExpandedByGesture(boolean userExpanded) {
        int event = 409;
        if (this.mGroupManager.isSummaryOfGroup(getStatusBarNotification())) {
            event = 410;
        }
        MetricsLogger.action(this.mContext, event, userExpanded);
    }

    @Override
    public float getIncreasedPaddingAmount() {
        if (this.mIsSummaryWithChildren) {
            if (isGroupExpanded()) {
                return 1.0f;
            }
            if (isUserLocked()) {
                return this.mChildrenContainer.getGroupExpandFraction();
            }
            return 0.0f;
        }
        return 0.0f;
    }

    @Override
    protected boolean disallowSingleClick(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        NotificationHeaderView header = getVisibleNotificationHeader();
        if (header != null) {
            return header.isInTouchRect(x - getTranslation(), y);
        }
        return super.disallowSingleClick(event);
    }

    public void logExpansionEvent(boolean userAction, boolean wasExpanded) {
        boolean nowExpanded = isExpanded();
        if (this.mIsSummaryWithChildren) {
            nowExpanded = this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
        }
        if (wasExpanded == nowExpanded || this.mLogger == null) {
            return;
        }
        this.mLogger.logNotificationExpansion(this.mLoggingKey, userAction, nowExpanded);
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        if (!canViewBeDismissed()) {
            return;
        }
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS);
    }

    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        if (super.performAccessibilityActionInternal(action, arguments)) {
            return true;
        }
        switch (action) {
            case 1048576:
                NotificationStackScrollLayout.performDismiss(this, this.mGroupManager, true);
                break;
        }
        return true;
    }

    public boolean shouldRefocusOnDismiss() {
        if (this.mRefocusOnDismiss) {
            return true;
        }
        return isAccessibilityFocused();
    }
}
