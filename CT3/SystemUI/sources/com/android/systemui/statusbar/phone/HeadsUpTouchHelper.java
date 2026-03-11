package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class HeadsUpTouchHelper {
    private boolean mCollapseSnoozes;
    private HeadsUpManager mHeadsUpManager;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private NotificationPanelView mPanel;
    private ExpandableNotificationRow mPickedChild;
    private NotificationStackScrollLayout mStackScroller;
    private float mTouchSlop;
    private boolean mTouchingHeadsUpView;
    private boolean mTrackingHeadsUp;
    private int mTrackingPointer;

    public HeadsUpTouchHelper(HeadsUpManager headsUpManager, NotificationStackScrollLayout stackScroller, NotificationPanelView notificationPanelView) {
        this.mHeadsUpManager = headsUpManager;
        this.mStackScroller = stackScroller;
        this.mPanel = notificationPanelView;
        Context context = stackScroller.getContext();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTouchSlop = configuration.getScaledTouchSlop();
    }

    public boolean isTrackingHeadsUp() {
        return this.mTrackingHeadsUp;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!this.mTouchingHeadsUpView && event.getActionMasked() != 0) {
            return false;
        }
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        switch (event.getActionMasked()) {
            case 0:
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                setTrackingHeadsUp(false);
                ExpandableView child = this.mStackScroller.getChildAtRawPosition(x, y);
                this.mTouchingHeadsUpView = false;
                if (child instanceof ExpandableNotificationRow) {
                    this.mPickedChild = (ExpandableNotificationRow) child;
                    this.mTouchingHeadsUpView = (this.mStackScroller.isExpanded() || !this.mPickedChild.isHeadsUp()) ? false : this.mPickedChild.isPinned();
                }
                return false;
            case 1:
            case 3:
                if (this.mPickedChild != null && this.mTouchingHeadsUpView && this.mHeadsUpManager.shouldSwallowClick(this.mPickedChild.getStatusBarNotification().getKey())) {
                    endMotion();
                    return true;
                }
                endMotion();
                return false;
            case 2:
                float h = y - this.mInitialTouchY;
                if (this.mTouchingHeadsUpView && Math.abs(h) > this.mTouchSlop && Math.abs(h) > Math.abs(x - this.mInitialTouchX)) {
                    setTrackingHeadsUp(true);
                    this.mCollapseSnoozes = h < 0.0f;
                    this.mInitialTouchX = x;
                    this.mInitialTouchY = y;
                    int expandedHeight = this.mPickedChild.getActualHeight();
                    this.mPanel.setPanelScrimMinFraction(expandedHeight / this.mPanel.getMaxPanelHeight());
                    this.mPanel.startExpandMotion(x, y, true, expandedHeight);
                    this.mHeadsUpManager.unpinAll();
                    this.mPanel.clearNotificationEffects();
                    return true;
                }
                return false;
            case 4:
            case 5:
            default:
                return false;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    this.mInitialTouchX = event.getX(newIndex);
                    this.mInitialTouchY = event.getY(newIndex);
                }
                return false;
        }
    }

    private void setTrackingHeadsUp(boolean tracking) {
        this.mTrackingHeadsUp = tracking;
        this.mHeadsUpManager.setTrackingHeadsUp(tracking);
        this.mPanel.setTrackingHeadsUp(tracking);
    }

    public void notifyFling(boolean collapse) {
        if (collapse && this.mCollapseSnoozes) {
            this.mHeadsUpManager.snooze();
        }
        this.mCollapseSnoozes = false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mTrackingHeadsUp) {
            return false;
        }
        switch (event.getActionMasked()) {
            case 1:
            case 3:
                endMotion();
                setTrackingHeadsUp(false);
                return true;
            case 2:
            default:
                return true;
        }
    }

    private void endMotion() {
        this.mTrackingPointer = -1;
        this.mPickedChild = null;
        this.mTouchingHeadsUpView = false;
    }
}
