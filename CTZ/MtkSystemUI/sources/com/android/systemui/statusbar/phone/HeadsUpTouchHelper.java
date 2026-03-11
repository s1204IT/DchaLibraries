package com.android.systemui.statusbar.phone;

import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class HeadsUpTouchHelper {
    private boolean mCollapseSnoozes;
    private HeadsUpManagerPhone mHeadsUpManager;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private NotificationPanelView mPanel;
    private ExpandableNotificationRow mPickedChild;
    private NotificationStackScrollLayout mStackScroller;
    private float mTouchSlop;
    private boolean mTouchingHeadsUpView;
    private boolean mTrackingHeadsUp;
    private int mTrackingPointer;

    public HeadsUpTouchHelper(HeadsUpManagerPhone headsUpManagerPhone, NotificationStackScrollLayout notificationStackScrollLayout, NotificationPanelView notificationPanelView) {
        this.mHeadsUpManager = headsUpManagerPhone;
        this.mStackScroller = notificationStackScrollLayout;
        this.mPanel = notificationPanelView;
        this.mTouchSlop = ViewConfiguration.get(notificationStackScrollLayout.getContext()).getScaledTouchSlop();
    }

    public boolean isTrackingHeadsUp() {
        return this.mTrackingHeadsUp;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        NotificationData.Entry topEntry;
        if (!this.mTouchingHeadsUpView && motionEvent.getActionMasked() != 0) {
            return false;
        }
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
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    setTrackingHeadsUp(false);
                    ExpandableView childAtRawPosition = this.mStackScroller.getChildAtRawPosition(x, y);
                    this.mTouchingHeadsUpView = false;
                    if (childAtRawPosition instanceof ExpandableNotificationRow) {
                        this.mPickedChild = (ExpandableNotificationRow) childAtRawPosition;
                        this.mTouchingHeadsUpView = !this.mStackScroller.isExpanded() && this.mPickedChild.isHeadsUp() && this.mPickedChild.isPinned();
                    } else if (childAtRawPosition == null && !this.mStackScroller.isExpanded() && (topEntry = this.mHeadsUpManager.getTopEntry()) != null && topEntry.row.isPinned()) {
                        this.mPickedChild = topEntry.row;
                        this.mTouchingHeadsUpView = true;
                    }
                    break;
                case 1:
                case 3:
                    if (this.mPickedChild != null && this.mTouchingHeadsUpView && this.mHeadsUpManager.shouldSwallowClick(this.mPickedChild.getStatusBarNotification().getKey())) {
                        endMotion();
                        return true;
                    }
                    endMotion();
                    break;
                    break;
                case 2:
                    float f = y - this.mInitialTouchY;
                    if (this.mTouchingHeadsUpView && Math.abs(f) > this.mTouchSlop && Math.abs(f) > Math.abs(x - this.mInitialTouchX)) {
                        setTrackingHeadsUp(true);
                        this.mCollapseSnoozes = f < 0.0f;
                        this.mInitialTouchX = x;
                        this.mInitialTouchY = y;
                        float actualHeight = (int) (this.mPickedChild.getActualHeight() + this.mPickedChild.getTranslationY());
                        this.mPanel.setPanelScrimMinFraction(actualHeight / this.mPanel.getMaxPanelHeight());
                        this.mPanel.startExpandMotion(x, y, true, actualHeight);
                        this.mPanel.startExpandingFromPeek();
                        this.mHeadsUpManager.unpinAll();
                        this.mPanel.clearNotificationEffects();
                        endMotion();
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

    private void setTrackingHeadsUp(boolean z) {
        this.mTrackingHeadsUp = z;
        this.mHeadsUpManager.setTrackingHeadsUp(z);
        this.mPanel.setTrackedHeadsUp(z ? this.mPickedChild : null);
    }

    public void notifyFling(boolean z) {
        if (z && this.mCollapseSnoozes) {
            this.mHeadsUpManager.snooze();
        }
        this.mCollapseSnoozes = false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mTrackingHeadsUp) {
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 1 || actionMasked == 3) {
            endMotion();
            setTrackingHeadsUp(false);
        }
        return true;
    }

    private void endMotion() {
        this.mTrackingPointer = -1;
        this.mPickedChild = null;
        this.mTouchingHeadsUpView = false;
    }
}
