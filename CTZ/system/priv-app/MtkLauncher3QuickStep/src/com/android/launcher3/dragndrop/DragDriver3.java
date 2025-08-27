package com.android.launcher3.dragndrop;

import android.content.Context;
import android.view.DragEvent;
import android.view.MotionEvent;
import com.android.launcher3.DropTarget;

/* compiled from: DragDriver.java */
/* renamed from: com.android.launcher3.dragndrop.SystemDragDriver */
/* loaded from: classes.dex */
class DragDriver3 extends DragDriver {
    float mLastX;
    float mLastY;

    DragDriver3(DragController dragController, Context context, DropTarget.DragObject dragObject) {
        super(dragController);
        this.mLastX = 0.0f;
        this.mLastY = 0.0f;
    }

    @Override // com.android.launcher3.dragndrop.DragDriver
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override // com.android.launcher3.dragndrop.DragDriver
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override // com.android.launcher3.dragndrop.DragDriver
    public boolean onDragEvent(DragEvent dragEvent) {
        switch (dragEvent.getAction()) {
            case 1:
                this.mLastX = dragEvent.getX();
                this.mLastY = dragEvent.getY();
                break;
            case 2:
                this.mLastX = dragEvent.getX();
                this.mLastY = dragEvent.getY();
                this.mEventListener.onDriverDragMove(dragEvent.getX(), dragEvent.getY());
                break;
            case 3:
                this.mLastX = dragEvent.getX();
                this.mLastY = dragEvent.getY();
                this.mEventListener.onDriverDragMove(dragEvent.getX(), dragEvent.getY());
                this.mEventListener.onDriverDragEnd(this.mLastX, this.mLastY);
                break;
            case 4:
                this.mEventListener.onDriverDragCancel();
                break;
            case 6:
                this.mEventListener.onDriverDragExitWindow();
                break;
        }
        return true;
    }
}
