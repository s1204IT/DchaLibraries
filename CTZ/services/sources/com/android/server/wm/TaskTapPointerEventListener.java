package com.android.server.wm;

import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.input.InputManager;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;

public class TaskTapPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    private final DisplayContent mDisplayContent;
    private final WindowManagerService mService;
    private final Region mTouchExcludeRegion = new Region();
    private final Rect mTmpRect = new Rect();
    private int mPointerIconType = 1;

    public TaskTapPointerEventListener(WindowManagerService windowManagerService, DisplayContent displayContent) {
        this.mService = windowManagerService;
        this.mDisplayContent = displayContent;
    }

    public void onPointerEvent(MotionEvent motionEvent, int i) {
        if (i == getDisplayId()) {
            onPointerEvent(motionEvent);
        }
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        int i;
        int action = motionEvent.getAction() & 255;
        if (action == 0) {
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();
            synchronized (this) {
                if (!this.mTouchExcludeRegion.contains(x, y)) {
                    this.mService.mTaskPositioningController.handleTapOutsideTask(this.mDisplayContent, x, y);
                }
            }
            return;
        }
        if (action == 7) {
            int x2 = (int) motionEvent.getX();
            int y2 = (int) motionEvent.getY();
            Task taskFindTaskForResizePoint = this.mDisplayContent.findTaskForResizePoint(x2, y2);
            if (taskFindTaskForResizePoint != null) {
                taskFindTaskForResizePoint.getDimBounds(this.mTmpRect);
                if (this.mTmpRect.isEmpty() || this.mTmpRect.contains(x2, y2)) {
                    i = 1;
                } else {
                    i = 1014;
                    if (x2 < this.mTmpRect.left) {
                        if (y2 >= this.mTmpRect.top) {
                            if (y2 > this.mTmpRect.bottom) {
                                i = 1016;
                            }
                        } else {
                            i = 1017;
                        }
                    } else if (x2 > this.mTmpRect.right) {
                        if (y2 >= this.mTmpRect.top) {
                            if (y2 > this.mTmpRect.bottom) {
                            }
                        }
                    } else if (y2 < this.mTmpRect.top || y2 > this.mTmpRect.bottom) {
                        i = 1015;
                    }
                }
            }
            if (this.mPointerIconType != i) {
                this.mPointerIconType = i;
                if (this.mPointerIconType == 1) {
                    this.mService.mH.obtainMessage(55, x2, y2, this.mDisplayContent).sendToTarget();
                } else {
                    InputManager.getInstance().setPointerIconType(this.mPointerIconType);
                }
            }
        }
    }

    void setTouchExcludeRegion(Region region) {
        synchronized (this) {
            this.mTouchExcludeRegion.set(region);
        }
    }

    private int getDisplayId() {
        return this.mDisplayContent.getDisplayId();
    }
}
