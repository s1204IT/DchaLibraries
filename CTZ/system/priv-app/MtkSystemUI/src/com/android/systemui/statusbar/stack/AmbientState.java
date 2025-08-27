package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.util.ArrayList;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class AmbientState {
    private ActivatableNotificationView mActivatedChild;
    private boolean mAppearing;
    private int mBaseZHeight;
    private float mCurrentScrollVelocity;
    private boolean mDark;
    private float mDarkAmount;
    private int mDarkTopPadding;
    private boolean mDimmed;
    private boolean mDismissAllInProgress;
    private int mExpandAnimationTopChange;
    private ExpandableNotificationRow mExpandingNotification;
    private float mExpandingVelocity;
    private boolean mExpansionChanging;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHideSensitive;
    private int mIntrinsicPadding;
    private ActivatableNotificationView mLastVisibleBackgroundChild;
    private int mLayoutHeight;
    private int mLayoutMinHeight;
    private float mMaxHeadsUpTranslation;
    private int mMaxLayoutHeight;
    private float mOverScrollBottomAmount;
    private float mOverScrollTopAmount;
    private boolean mPanelFullWidth;
    private boolean mPanelTracking;
    private boolean mPulsing;
    private boolean mQsCustomizerShowing;
    private int mScrollY;
    private boolean mShadeExpanded;
    private NotificationShelf mShelf;
    private float mStackTranslation;
    private int mStatusBarState;
    private int mTopPadding;
    private boolean mUnlockHintRunning;
    private int mZDistanceBetweenElements;
    private ArrayList<View> mDraggedViews = new ArrayList<>();
    private int mSpeedBumpIndex = -1;

    public AmbientState(Context context) {
        reload(context);
    }

    public void reload(Context context) {
        this.mZDistanceBetweenElements = getZDistanceBetweenElements(context);
        this.mBaseZHeight = getBaseHeight(this.mZDistanceBetweenElements);
    }

    private static int getZDistanceBetweenElements(Context context) {
        return Math.max(1, context.getResources().getDimensionPixelSize(R.dimen.z_distance_between_notifications));
    }

    private static int getBaseHeight(int i) {
        return 4 * i;
    }

    public static int getNotificationLaunchHeight(Context context) {
        return getBaseHeight(getZDistanceBetweenElements(context)) * 2;
    }

    public int getBaseZHeight() {
        return this.mBaseZHeight;
    }

    public int getZDistanceBetweenElements() {
        return this.mZDistanceBetweenElements;
    }

    public int getScrollY() {
        return this.mScrollY;
    }

    public void setScrollY(int i) {
        this.mScrollY = i;
    }

    public void onBeginDrag(View view) {
        this.mDraggedViews.add(view);
    }

    public void onDragFinished(View view) {
        this.mDraggedViews.remove(view);
    }

    public ArrayList<View> getDraggedViews() {
        return this.mDraggedViews;
    }

    public void setDimmed(boolean z) {
        this.mDimmed = z;
    }

    public void setDark(boolean z) {
        this.mDark = z;
    }

    public void setDarkAmount(float f) {
        this.mDarkAmount = f;
    }

    public float getDarkAmount() {
        return this.mDarkAmount;
    }

    public void setHideSensitive(boolean z) {
        this.mHideSensitive = z;
    }

    public void setActivatedChild(ActivatableNotificationView activatableNotificationView) {
        this.mActivatedChild = activatableNotificationView;
    }

    public boolean isDimmed() {
        return this.mDimmed;
    }

    public boolean isDark() {
        return this.mDark;
    }

    public boolean isHideSensitive() {
        return this.mHideSensitive;
    }

    public ActivatableNotificationView getActivatedChild() {
        return this.mActivatedChild;
    }

    public void setOverScrollAmount(float f, boolean z) {
        if (z) {
            this.mOverScrollTopAmount = f;
        } else {
            this.mOverScrollBottomAmount = f;
        }
    }

    public float getOverScrollAmount(boolean z) {
        return z ? this.mOverScrollTopAmount : this.mOverScrollBottomAmount;
    }

    public int getSpeedBumpIndex() {
        return this.mSpeedBumpIndex;
    }

    public void setSpeedBumpIndex(int i) {
        this.mSpeedBumpIndex = i;
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public float getStackTranslation() {
        return this.mStackTranslation;
    }

    public void setStackTranslation(float f) {
        this.mStackTranslation = f;
    }

    public void setLayoutHeight(int i) {
        this.mLayoutHeight = i;
    }

    public float getTopPadding() {
        return this.mTopPadding;
    }

    public void setTopPadding(int i) {
        this.mTopPadding = i;
    }

    public int getInnerHeight() {
        return Math.max(Math.min(this.mLayoutHeight, this.mMaxLayoutHeight) - this.mTopPadding, this.mLayoutMinHeight);
    }

    public boolean isShadeExpanded() {
        return this.mShadeExpanded;
    }

    public void setShadeExpanded(boolean z) {
        this.mShadeExpanded = z;
    }

    public void setMaxHeadsUpTranslation(float f) {
        this.mMaxHeadsUpTranslation = f;
    }

    public float getMaxHeadsUpTranslation() {
        return this.mMaxHeadsUpTranslation;
    }

    public void setDismissAllInProgress(boolean z) {
        this.mDismissAllInProgress = z;
    }

    public void setLayoutMinHeight(int i) {
        this.mLayoutMinHeight = i;
    }

    public void setShelf(NotificationShelf notificationShelf) {
        this.mShelf = notificationShelf;
    }

    public NotificationShelf getShelf() {
        return this.mShelf;
    }

    public void setLayoutMaxHeight(int i) {
        this.mMaxLayoutHeight = i;
    }

    public void setLastVisibleBackgroundChild(ActivatableNotificationView activatableNotificationView) {
        this.mLastVisibleBackgroundChild = activatableNotificationView;
    }

    public ActivatableNotificationView getLastVisibleBackgroundChild() {
        return this.mLastVisibleBackgroundChild;
    }

    public void setCurrentScrollVelocity(float f) {
        this.mCurrentScrollVelocity = f;
    }

    public float getCurrentScrollVelocity() {
        return this.mCurrentScrollVelocity;
    }

    public boolean isOnKeyguard() {
        return this.mStatusBarState == 1;
    }

    public void setStatusBarState(int i) {
        this.mStatusBarState = i;
    }

    public void setExpandingVelocity(float f) {
        this.mExpandingVelocity = f;
    }

    public void setExpansionChanging(boolean z) {
        this.mExpansionChanging = z;
    }

    public boolean isExpansionChanging() {
        return this.mExpansionChanging;
    }

    public float getExpandingVelocity() {
        return this.mExpandingVelocity;
    }

    public void setPanelTracking(boolean z) {
        this.mPanelTracking = z;
    }

    public boolean hasPulsingNotifications() {
        return this.mPulsing;
    }

    public void setPulsing(boolean z) {
        this.mPulsing = z;
    }

    public boolean isPulsing(final NotificationData.Entry entry) {
        if (!this.mPulsing || this.mHeadsUpManager == null) {
            return false;
        }
        return this.mHeadsUpManager.getAllEntries().anyMatch(new Predicate() { // from class: com.android.systemui.statusbar.stack.-$$Lambda$AmbientState$05gi9X6uLduWjWHWb7e4F7kHS2o
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AmbientState.lambda$isPulsing$0(entry, (NotificationData.Entry) obj);
            }
        });
    }

    static /* synthetic */ boolean lambda$isPulsing$0(NotificationData.Entry entry, NotificationData.Entry entry2) {
        return entry2 == entry;
    }

    public boolean isPanelTracking() {
        return this.mPanelTracking;
    }

    public boolean isPanelFullWidth() {
        return this.mPanelFullWidth;
    }

    public void setPanelFullWidth(boolean z) {
        this.mPanelFullWidth = z;
    }

    public void setUnlockHintRunning(boolean z) {
        this.mUnlockHintRunning = z;
    }

    public boolean isUnlockHintRunning() {
        return this.mUnlockHintRunning;
    }

    public boolean isQsCustomizerShowing() {
        return this.mQsCustomizerShowing;
    }

    public void setQsCustomizerShowing(boolean z) {
        this.mQsCustomizerShowing = z;
    }

    public void setIntrinsicPadding(int i) {
        this.mIntrinsicPadding = i;
    }

    public int getIntrinsicPadding() {
        return this.mIntrinsicPadding;
    }

    public boolean isAboveShelf(ExpandableView expandableView) {
        if (!(expandableView instanceof ExpandableNotificationRow)) {
            return expandableView.isAboveShelf();
        }
        ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
        return expandableNotificationRow.isAboveShelf() && !isDozingAndNotPulsing(expandableNotificationRow);
    }

    public boolean isDozingAndNotPulsing(ExpandableView expandableView) {
        if (expandableView instanceof ExpandableNotificationRow) {
            return isDozingAndNotPulsing((ExpandableNotificationRow) expandableView);
        }
        return false;
    }

    public boolean isDozingAndNotPulsing(ExpandableNotificationRow expandableNotificationRow) {
        return isDark() && !isPulsing(expandableNotificationRow.getEntry());
    }

    public void setExpandAnimationTopChange(int i) {
        this.mExpandAnimationTopChange = i;
    }

    public void setExpandingNotification(ExpandableNotificationRow expandableNotificationRow) {
        this.mExpandingNotification = expandableNotificationRow;
    }

    public ExpandableNotificationRow getExpandingNotification() {
        return this.mExpandingNotification;
    }

    public int getExpandAnimationTopChange() {
        return this.mExpandAnimationTopChange;
    }

    public boolean isFullyDark() {
        return this.mDarkAmount == 1.0f;
    }

    public void setDarkTopPadding(int i) {
        this.mDarkTopPadding = i;
    }

    public int getDarkTopPadding() {
        return this.mDarkTopPadding;
    }

    public void setAppearing(boolean z) {
        this.mAppearing = z;
    }

    public boolean isAppearing() {
        return this.mAppearing;
    }
}
