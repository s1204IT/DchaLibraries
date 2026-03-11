package com.android.systemui.statusbar.phone;

import android.R;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.util.ArraySet;
import android.util.Pools;
import android.view.View;
import android.view.ViewTreeObserver;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dumpable;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Stack;

public class HeadsUpManagerPhone extends HeadsUpManager implements ViewTreeObserver.OnComputeInternalInsetsListener, Dumpable, VisualStabilityManager.Callback, ConfigurationController.ConfigurationListener, OnHeadsUpChangedListener {
    private final StatusBar mBar;
    private HashSet<NotificationData.Entry> mEntriesToRemoveAfterExpand;
    private ArraySet<NotificationData.Entry> mEntriesToRemoveWhenReorderingAllowed;
    private final Pools.Pool<HeadsUpEntryPhone> mEntryPool;
    private final NotificationGroupManager mGroupManager;
    private boolean mHeadsUpGoingAway;
    private int mHeadsUpInset;
    private boolean mIsExpanded;
    private boolean mIsObserving;
    private boolean mReleaseOnExpandFinish;
    private int mStatusBarHeight;
    private int mStatusBarState;
    private final View mStatusBarWindowView;
    private HashSet<String> mSwipedOutKeys;
    private int[] mTmpTwoArray;
    private boolean mTrackingHeadsUp;
    private final VisualStabilityManager mVisualStabilityManager;
    private boolean mWaitingOnCollapseWhenGoingAway;

    public HeadsUpManagerPhone(Context context, View view, NotificationGroupManager notificationGroupManager, StatusBar statusBar, VisualStabilityManager visualStabilityManager) {
        super(context);
        this.mSwipedOutKeys = new HashSet<>();
        this.mEntriesToRemoveAfterExpand = new HashSet<>();
        this.mEntriesToRemoveWhenReorderingAllowed = new ArraySet<>();
        this.mTmpTwoArray = new int[2];
        this.mEntryPool = new Pools.Pool<HeadsUpEntryPhone>() {
            private Stack<HeadsUpEntryPhone> mPoolObjects = new Stack<>();

            public HeadsUpEntryPhone m16acquire() {
                if (!this.mPoolObjects.isEmpty()) {
                    return this.mPoolObjects.pop();
                }
                return HeadsUpManagerPhone.this.new HeadsUpEntryPhone();
            }

            public boolean release(HeadsUpEntryPhone headsUpEntryPhone) {
                this.mPoolObjects.push(headsUpEntryPhone);
                return true;
            }
        };
        this.mStatusBarWindowView = view;
        this.mGroupManager = notificationGroupManager;
        this.mBar = statusBar;
        this.mVisualStabilityManager = visualStabilityManager;
        initResources();
        addListener(new OnHeadsUpChangedListener() {
            @Override
            public void onHeadsUpPinnedModeChanged(boolean z) {
                HeadsUpManagerPhone.this.updateTouchableRegionListener();
            }
        });
    }

    private void initResources() {
        Resources resources = this.mContext.getResources();
        this.mStatusBarHeight = resources.getDimensionPixelSize(R.dimen.floating_window_z);
        this.mHeadsUpInset = this.mStatusBarHeight + resources.getDimensionPixelSize(com.android.systemui.R.dimen.heads_up_status_bar_padding);
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        super.onDensityOrFontScaleChanged();
        initResources();
    }

    public boolean shouldSwallowClick(String str) {
        HeadsUpManager.HeadsUpEntry headsUpEntry = getHeadsUpEntry(str);
        return headsUpEntry != null && this.mClock.currentTimeMillis() < headsUpEntry.postTime;
    }

    public void onExpandingFinished() {
        if (this.mReleaseOnExpandFinish) {
            releaseAllImmediately();
            this.mReleaseOnExpandFinish = false;
        } else {
            for (NotificationData.Entry entry : this.mEntriesToRemoveAfterExpand) {
                if (isHeadsUp(entry.key)) {
                    removeHeadsUpEntry(entry);
                }
            }
        }
        this.mEntriesToRemoveAfterExpand.clear();
    }

    public void setTrackingHeadsUp(boolean z) {
        this.mTrackingHeadsUp = z;
    }

    public void setIsPanelExpanded(boolean z) {
        if (z != this.mIsExpanded) {
            this.mIsExpanded = z;
            if (z) {
                this.mWaitingOnCollapseWhenGoingAway = false;
                this.mHeadsUpGoingAway = false;
                updateTouchableRegionListener();
            }
        }
    }

    public void setStatusBarState(int i) {
        this.mStatusBarState = i;
    }

    public void setHeadsUpGoingAway(boolean z) {
        if (z != this.mHeadsUpGoingAway) {
            this.mHeadsUpGoingAway = z;
            if (!z) {
                waitForStatusBarLayout();
            }
            updateTouchableRegionListener();
        }
    }

    public void setRemoteInputActive(NotificationData.Entry entry, boolean z) {
        HeadsUpEntryPhone headsUpEntryPhone = getHeadsUpEntryPhone(entry.key);
        if (headsUpEntryPhone != null && headsUpEntryPhone.remoteInputActive != z) {
            headsUpEntryPhone.remoteInputActive = z;
            if (z) {
                headsUpEntryPhone.removeAutoRemovalCallbacks();
            } else {
                headsUpEntryPhone.updateEntry(false);
            }
        }
    }

    @VisibleForTesting
    public void removeMinimumDisplayTimeForTesting() {
        this.mMinimumDisplayTime = 0;
        this.mHeadsUpNotificationDecay = 0;
        this.mTouchAcceptanceDelay = 0;
    }

    @Override
    public boolean isTrackingHeadsUp() {
        return this.mTrackingHeadsUp;
    }

    @Override
    public void snooze() {
        super.snooze();
        this.mReleaseOnExpandFinish = true;
    }

    @Override
    public boolean removeNotification(String str, boolean z) {
        if (wasShownLongEnough(str) || z) {
            return super.removeNotification(str, z);
        }
        getHeadsUpEntryPhone(str).removeAsSoonAsPossible();
        return false;
    }

    public void addSwipedOutNotification(String str) {
        this.mSwipedOutKeys.add(str);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("HeadsUpManagerPhone state:");
        dumpInternal(fileDescriptor, printWriter, strArr);
    }

    public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
        ExpandableNotificationRow groupSummary;
        if (this.mIsExpanded || this.mBar.isBouncerShowing()) {
            return;
        }
        if (!hasPinnedHeadsUp()) {
            if (this.mHeadsUpGoingAway || this.mWaitingOnCollapseWhenGoingAway) {
                internalInsetsInfo.setTouchableInsets(3);
                internalInsetsInfo.touchableRegion.set(0, 0, this.mStatusBarWindowView.getWidth(), this.mStatusBarHeight);
                return;
            }
            return;
        }
        ExpandableNotificationRow expandableNotificationRow = getTopEntry().row;
        if (expandableNotificationRow.isChildInGroup() && (groupSummary = this.mGroupManager.getGroupSummary(expandableNotificationRow.getStatusBarNotification())) != null) {
            expandableNotificationRow = groupSummary;
        }
        expandableNotificationRow.getLocationOnScreen(this.mTmpTwoArray);
        int i = this.mTmpTwoArray[0];
        int width = this.mTmpTwoArray[0] + expandableNotificationRow.getWidth();
        int intrinsicHeight = expandableNotificationRow.getIntrinsicHeight();
        internalInsetsInfo.setTouchableInsets(3);
        internalInsetsInfo.touchableRegion.set(i, 0, width, this.mHeadsUpInset + intrinsicHeight);
    }

    @Override
    public void onConfigChanged(Configuration configuration) {
        this.mStatusBarHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.floating_window_z);
    }

    @Override
    public void onReorderingAllowed() {
        this.mBar.getNotificationScrollLayout().setHeadsUpGoingAwayAnimationsAllowed(false);
        for (NotificationData.Entry entry : this.mEntriesToRemoveWhenReorderingAllowed) {
            if (isHeadsUp(entry.key)) {
                removeHeadsUpEntry(entry);
            }
        }
        this.mEntriesToRemoveWhenReorderingAllowed.clear();
        this.mBar.getNotificationScrollLayout().setHeadsUpGoingAwayAnimationsAllowed(true);
    }

    @Override
    protected HeadsUpManager.HeadsUpEntry createHeadsUpEntry() {
        return (HeadsUpManager.HeadsUpEntry) this.mEntryPool.acquire();
    }

    @Override
    protected void releaseHeadsUpEntry(HeadsUpManager.HeadsUpEntry headsUpEntry) {
        headsUpEntry.reset();
        this.mEntryPool.release((HeadsUpEntryPhone) headsUpEntry);
    }

    @Override
    protected boolean shouldHeadsUpBecomePinned(NotificationData.Entry entry) {
        return !(this.mStatusBarState == 1 || this.mIsExpanded) || super.shouldHeadsUpBecomePinned(entry);
    }

    @Override
    protected void dumpInternal(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dumpInternal(fileDescriptor, printWriter, strArr);
        printWriter.print("  mStatusBarState=");
        printWriter.println(this.mStatusBarState);
    }

    private HeadsUpEntryPhone getHeadsUpEntryPhone(String str) {
        return (HeadsUpEntryPhone) getHeadsUpEntry(str);
    }

    private HeadsUpEntryPhone getTopHeadsUpEntryPhone() {
        return (HeadsUpEntryPhone) getTopHeadsUpEntry();
    }

    private boolean wasShownLongEnough(String str) {
        if (this.mSwipedOutKeys.contains(str)) {
            this.mSwipedOutKeys.remove(str);
            return true;
        }
        HeadsUpEntryPhone headsUpEntryPhone = getHeadsUpEntryPhone(str);
        return headsUpEntryPhone != getTopHeadsUpEntryPhone() || headsUpEntryPhone.wasShownLongEnough();
    }

    private void waitForStatusBarLayout() {
        this.mWaitingOnCollapseWhenGoingAway = true;
        this.mStatusBarWindowView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                if (HeadsUpManagerPhone.this.mStatusBarWindowView.getHeight() <= HeadsUpManagerPhone.this.mStatusBarHeight) {
                    HeadsUpManagerPhone.this.mStatusBarWindowView.removeOnLayoutChangeListener(this);
                    HeadsUpManagerPhone.this.mWaitingOnCollapseWhenGoingAway = false;
                    HeadsUpManagerPhone.this.updateTouchableRegionListener();
                }
            }
        });
    }

    public void updateTouchableRegionListener() {
        boolean z = hasPinnedHeadsUp() || this.mHeadsUpGoingAway || this.mWaitingOnCollapseWhenGoingAway;
        if (z == this.mIsObserving) {
            return;
        }
        if (z) {
            this.mStatusBarWindowView.getViewTreeObserver().addOnComputeInternalInsetsListener(this);
            this.mStatusBarWindowView.requestLayout();
        } else {
            this.mStatusBarWindowView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this);
        }
        this.mIsObserving = z;
    }

    protected class HeadsUpEntryPhone extends HeadsUpManager.HeadsUpEntry {
        protected HeadsUpEntryPhone() {
            super();
        }

        @Override
        public void setEntry(final NotificationData.Entry entry) {
            super.setEntry(entry, new Runnable() {
                @Override
                public final void run() {
                    HeadsUpManagerPhone.HeadsUpEntryPhone.lambda$setEntry$0(this.f$0, entry);
                }
            });
        }

        public static void lambda$setEntry$0(HeadsUpEntryPhone headsUpEntryPhone, NotificationData.Entry entry) {
            if (!HeadsUpManagerPhone.this.mVisualStabilityManager.isReorderingAllowed()) {
                HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.add(entry);
                HeadsUpManagerPhone.this.mVisualStabilityManager.addReorderingAllowedCallback(HeadsUpManagerPhone.this);
            } else if (!HeadsUpManagerPhone.this.mTrackingHeadsUp) {
                HeadsUpManagerPhone.this.removeHeadsUpEntry(entry);
            } else {
                HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.add(entry);
            }
        }

        public boolean wasShownLongEnough() {
            return this.earliestRemovaltime < HeadsUpManagerPhone.this.mClock.currentTimeMillis();
        }

        @Override
        public void updateEntry(boolean z) {
            super.updateEntry(z);
            if (HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.contains(this.entry)) {
                HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.remove(this.entry);
            }
            if (HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.contains(this.entry)) {
                HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.remove(this.entry);
            }
        }

        @Override
        public void expanded(boolean z) {
            if (this.expanded == z) {
                return;
            }
            this.expanded = z;
            if (z) {
                removeAutoRemovalCallbacks();
            } else {
                updateEntry(false);
            }
        }
    }
}
