package com.android.server.wm;

import android.graphics.Region;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy;

public class StackTapPointerEventListener implements WindowManagerPolicy.PointerEventListener {
    private static final float TAP_MOTION_SLOP_INCHES = 0.125f;
    private static final int TAP_TIMEOUT_MSEC = 300;
    private final DisplayContent mDisplayContent;
    private float mDownX;
    private float mDownY;
    private final int mMotionSlop;
    private int mPointerId;
    private final WindowManagerService mService;
    private final Region mTouchExcludeRegion;

    public StackTapPointerEventListener(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mTouchExcludeRegion = displayContent.mTouchExcludeRegion;
        DisplayInfo info = displayContent.getDisplayInfo();
        this.mMotionSlop = (int) (info.logicalDensityDpi * TAP_MOTION_SLOP_INCHES);
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        switch (action & 255) {
            case 0:
                this.mPointerId = motionEvent.getPointerId(0);
                this.mDownX = motionEvent.getX();
                this.mDownY = motionEvent.getY();
                break;
            case 1:
            case 6:
                int index = (65280 & action) >> 8;
                if (this.mPointerId == motionEvent.getPointerId(index)) {
                    int x = (int) motionEvent.getX(index);
                    int y = (int) motionEvent.getY(index);
                    if (motionEvent.getEventTime() - motionEvent.getDownTime() < 300 && x - this.mDownX < this.mMotionSlop && y - this.mDownY < this.mMotionSlop && !this.mTouchExcludeRegion.contains(x, y)) {
                        this.mService.mH.obtainMessage(31, x, y, this.mDisplayContent).sendToTarget();
                    }
                    this.mPointerId = -1;
                }
                break;
            case 2:
                if (this.mPointerId >= 0) {
                    int index2 = motionEvent.findPointerIndex(this.mPointerId);
                    if (motionEvent.getEventTime() - motionEvent.getDownTime() > 300 || index2 < 0 || motionEvent.getX(index2) - this.mDownX > this.mMotionSlop || motionEvent.getY(index2) - this.mDownY > this.mMotionSlop) {
                        this.mPointerId = -1;
                    }
                }
                break;
        }
    }
}
