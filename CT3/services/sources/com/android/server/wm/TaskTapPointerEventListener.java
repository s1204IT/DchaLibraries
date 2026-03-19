package com.android.server.wm;

import android.graphics.Rect;
import android.graphics.Region;
import android.net.dhcp.DhcpPacket;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy;

public class TaskTapPointerEventListener implements WindowManagerPolicy.PointerEventListener {
    private final DisplayContent mDisplayContent;
    private GestureDetector mGestureDetector;
    private boolean mInGestureDetection;
    private final WindowManagerService mService;
    private boolean mTwoFingerScrolling;
    private final Region mTouchExcludeRegion = new Region();
    private final Rect mTmpRect = new Rect();
    private final Region mNonResizeableRegion = new Region();
    private int mPointerIconType = 1;

    public TaskTapPointerEventListener(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
    }

    void init() {
        this.mGestureDetector = new GestureDetector(this.mService.mContext, new TwoFingerScrollListener(this, null), this.mService.mH);
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        doGestureDetection(motionEvent);
        int action = motionEvent.getAction();
        switch (action & DhcpPacket.MAX_OPTION_LEN) {
            case 0:
                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();
                synchronized (this) {
                    if (!this.mTouchExcludeRegion.contains(x, y)) {
                        this.mService.mH.obtainMessage(31, x, y, this.mDisplayContent).sendToTarget();
                    }
                    break;
                }
                return;
            case 1:
            case 6:
                stopTwoFingerScroll();
                return;
            case 2:
                if (motionEvent.getPointerCount() == 2) {
                    return;
                }
                stopTwoFingerScroll();
                return;
            case 3:
            case 4:
            case 5:
            case 8:
            case 9:
            default:
                return;
            case 7:
                int x2 = (int) motionEvent.getX();
                int y2 = (int) motionEvent.getY();
                Task task = this.mDisplayContent.findTaskForControlPoint(x2, y2);
                InputDevice inputDevice = motionEvent.getDevice();
                if (task == null || inputDevice == null) {
                    this.mPointerIconType = 1;
                    return;
                }
                task.getDimBounds(this.mTmpRect);
                if (!this.mTmpRect.isEmpty() && !this.mTmpRect.contains(x2, y2)) {
                    int iconType = 1000;
                    if (x2 < this.mTmpRect.left) {
                        if (y2 < this.mTmpRect.top) {
                            iconType = 1017;
                        } else {
                            iconType = y2 > this.mTmpRect.bottom ? 1016 : 1014;
                        }
                    } else if (x2 > this.mTmpRect.right) {
                        if (y2 < this.mTmpRect.top) {
                            iconType = 1016;
                        } else {
                            iconType = y2 > this.mTmpRect.bottom ? 1017 : 1014;
                        }
                    } else if (y2 < this.mTmpRect.top || y2 > this.mTmpRect.bottom) {
                        iconType = 1015;
                    }
                    if (this.mPointerIconType == iconType) {
                        return;
                    }
                    this.mPointerIconType = iconType;
                    inputDevice.setPointerType(iconType);
                    return;
                }
                this.mPointerIconType = 1;
                return;
            case 10:
                this.mPointerIconType = 1;
                InputDevice inputDevice2 = motionEvent.getDevice();
                if (inputDevice2 == null) {
                    return;
                }
                inputDevice2.setPointerType(1000);
                return;
        }
    }

    private void doGestureDetection(MotionEvent motionEvent) {
        boolean z = true;
        if (this.mGestureDetector == null || this.mNonResizeableRegion.isEmpty()) {
            return;
        }
        int action = motionEvent.getAction() & DhcpPacket.MAX_OPTION_LEN;
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        boolean isTouchInside = this.mNonResizeableRegion.contains(x, y);
        if (!this.mInGestureDetection && (action != 0 || !isTouchInside)) {
            return;
        }
        if (!isTouchInside || action == 1 || action == 6 || action == 3) {
            z = false;
        }
        this.mInGestureDetection = z;
        if (this.mInGestureDetection) {
            this.mGestureDetector.onTouchEvent(motionEvent);
            return;
        }
        MotionEvent cancelEvent = motionEvent.copy();
        cancelEvent.cancel();
        this.mGestureDetector.onTouchEvent(cancelEvent);
        stopTwoFingerScroll();
    }

    private void onTwoFingerScroll(MotionEvent e) {
        int x = (int) e.getX(0);
        int y = (int) e.getY(0);
        if (this.mTwoFingerScrolling) {
            return;
        }
        this.mTwoFingerScrolling = true;
        this.mService.mH.obtainMessage(44, x, y, this.mDisplayContent).sendToTarget();
    }

    private void stopTwoFingerScroll() {
        if (!this.mTwoFingerScrolling) {
            return;
        }
        this.mTwoFingerScrolling = false;
        this.mService.mH.obtainMessage(40).sendToTarget();
    }

    private final class TwoFingerScrollListener extends GestureDetector.SimpleOnGestureListener {
        TwoFingerScrollListener(TaskTapPointerEventListener this$0, TwoFingerScrollListener twoFingerScrollListener) {
            this();
        }

        private TwoFingerScrollListener() {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e2.getPointerCount() == 2) {
                TaskTapPointerEventListener.this.onTwoFingerScroll(e2);
                return true;
            }
            TaskTapPointerEventListener.this.stopTwoFingerScroll();
            return false;
        }
    }

    void setTouchExcludeRegion(Region newRegion, Region nonResizeableRegion) {
        synchronized (this) {
            this.mTouchExcludeRegion.set(newRegion);
            this.mNonResizeableRegion.set(nonResizeableRegion);
        }
    }
}
