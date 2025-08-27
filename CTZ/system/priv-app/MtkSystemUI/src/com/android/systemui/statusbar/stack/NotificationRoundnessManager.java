package com.android.systemui.statusbar.stack;

import android.view.View;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.util.HashSet;

/* loaded from: classes.dex */
class NotificationRoundnessManager implements OnHeadsUpChangedListener {
    private HashSet<View> mAnimatedChildren;
    private float mAppearFraction;
    private boolean mExpanded;
    private ActivatableNotificationView mFirst;
    private ActivatableNotificationView mLast;
    private Runnable mRoundingChangedCallback;
    private ExpandableNotificationRow mTrackedHeadsUp;

    NotificationRoundnessManager() {
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinned(ExpandableNotificationRow expandableNotificationRow) {
        updateRounding(expandableNotificationRow, false);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpUnPinned(ExpandableNotificationRow expandableNotificationRow) {
        updateRounding(expandableNotificationRow, true);
    }

    public void onHeadsupAnimatingAwayChanged(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        updateRounding(expandableNotificationRow, false);
    }

    private void updateRounding(ActivatableNotificationView activatableNotificationView, boolean z) {
        float roundness = getRoundness(activatableNotificationView, true);
        float roundness2 = getRoundness(activatableNotificationView, false);
        boolean topRoundness = activatableNotificationView.setTopRoundness(roundness, z);
        boolean bottomRoundness = activatableNotificationView.setBottomRoundness(roundness2, z);
        if (activatableNotificationView == this.mFirst || activatableNotificationView == this.mLast) {
            if (topRoundness || bottomRoundness) {
                this.mRoundingChangedCallback.run();
            }
        }
    }

    private float getRoundness(ActivatableNotificationView activatableNotificationView, boolean z) {
        if ((activatableNotificationView.isPinned() || activatableNotificationView.isHeadsUpAnimatingAway()) && !this.mExpanded) {
            return 1.0f;
        }
        if (activatableNotificationView == this.mFirst && z) {
            return 1.0f;
        }
        if (activatableNotificationView != this.mLast || z) {
            return (activatableNotificationView != this.mTrackedHeadsUp || this.mAppearFraction > 0.0f) ? 0.0f : 1.0f;
        }
        return 1.0f;
    }

    public void setExpanded(float f, float f2) {
        this.mExpanded = f != 0.0f;
        this.mAppearFraction = f2;
        if (this.mTrackedHeadsUp != null) {
            updateRounding(this.mTrackedHeadsUp, true);
        }
    }

    public void setFirstAndLastBackgroundChild(ActivatableNotificationView activatableNotificationView, ActivatableNotificationView activatableNotificationView2) {
        boolean z = false;
        boolean z2 = this.mFirst != activatableNotificationView;
        boolean z3 = this.mLast != activatableNotificationView2;
        if (!z2 && !z3) {
            return;
        }
        ActivatableNotificationView activatableNotificationView3 = this.mFirst;
        ActivatableNotificationView activatableNotificationView4 = this.mLast;
        this.mFirst = activatableNotificationView;
        this.mLast = activatableNotificationView2;
        if (z2 && activatableNotificationView3 != null && !activatableNotificationView3.isRemoved()) {
            updateRounding(activatableNotificationView3, activatableNotificationView3.isShown());
        }
        if (z3 && activatableNotificationView4 != null && !activatableNotificationView4.isRemoved()) {
            updateRounding(activatableNotificationView4, activatableNotificationView4.isShown());
        }
        if (this.mFirst != null) {
            updateRounding(this.mFirst, this.mFirst.isShown() && !this.mAnimatedChildren.contains(this.mFirst));
        }
        if (this.mLast != null) {
            ActivatableNotificationView activatableNotificationView5 = this.mLast;
            if (this.mLast.isShown() && !this.mAnimatedChildren.contains(this.mLast)) {
                z = true;
            }
            updateRounding(activatableNotificationView5, z);
        }
        this.mRoundingChangedCallback.run();
    }

    public void setAnimatedChildren(HashSet<View> hashSet) {
        this.mAnimatedChildren = hashSet;
    }

    public void setOnRoundingChangedCallback(Runnable runnable) {
        this.mRoundingChangedCallback = runnable;
    }

    public void setTrackingHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        this.mTrackedHeadsUp = expandableNotificationRow;
    }
}
