package com.android.systemui.statusbar.notification;

import android.support.v4.util.ArraySet;
import android.view.View;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.util.ArrayList;

public class VisualStabilityManager implements OnHeadsUpChangedListener {
    private boolean mPanelExpanded;
    private boolean mPulsing;
    private boolean mReorderingAllowed;
    private boolean mScreenOn;
    private VisibilityLocationProvider mVisibilityLocationProvider;
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private ArraySet<View> mAllowedReorderViews = new ArraySet<>();
    private ArraySet<View> mLowPriorityReorderingViews = new ArraySet<>();
    private ArraySet<View> mAddedChildren = new ArraySet<>();

    public interface Callback {
        void onReorderingAllowed();
    }

    public void addReorderingAllowedCallback(Callback callback) {
        if (this.mCallbacks.contains(callback)) {
            return;
        }
        this.mCallbacks.add(callback);
    }

    public void setPanelExpanded(boolean z) {
        this.mPanelExpanded = z;
        updateReorderingAllowed();
    }

    public void setScreenOn(boolean z) {
        this.mScreenOn = z;
        updateReorderingAllowed();
    }

    public void setPulsing(boolean z) {
        if (this.mPulsing == z) {
            return;
        }
        this.mPulsing = z;
        updateReorderingAllowed();
    }

    private void updateReorderingAllowed() {
        boolean z = false;
        boolean z2 = ((this.mScreenOn && this.mPanelExpanded) || this.mPulsing) ? false : true;
        if (z2 && !this.mReorderingAllowed) {
            z = true;
        }
        this.mReorderingAllowed = z2;
        if (z) {
            notifyCallbacks();
        }
    }

    private void notifyCallbacks() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onReorderingAllowed();
        }
        this.mCallbacks.clear();
    }

    public boolean isReorderingAllowed() {
        return this.mReorderingAllowed;
    }

    public boolean canReorderNotification(ExpandableNotificationRow expandableNotificationRow) {
        if (this.mReorderingAllowed || this.mAddedChildren.contains(expandableNotificationRow) || this.mLowPriorityReorderingViews.contains(expandableNotificationRow)) {
            return true;
        }
        return this.mAllowedReorderViews.contains(expandableNotificationRow) && !this.mVisibilityLocationProvider.isInVisibleLocation(expandableNotificationRow);
    }

    public void setVisibilityLocationProvider(VisibilityLocationProvider visibilityLocationProvider) {
        this.mVisibilityLocationProvider = visibilityLocationProvider;
    }

    public void onReorderingFinished() {
        this.mAllowedReorderViews.clear();
        this.mAddedChildren.clear();
        this.mLowPriorityReorderingViews.clear();
    }

    @Override
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean z) {
        if (z) {
            this.mAllowedReorderViews.add(entry.row);
        }
    }

    public void onLowPriorityUpdated(NotificationData.Entry entry) {
        this.mLowPriorityReorderingViews.add(entry.row);
    }

    public void notifyViewAddition(View view) {
        this.mAddedChildren.add(view);
    }
}
