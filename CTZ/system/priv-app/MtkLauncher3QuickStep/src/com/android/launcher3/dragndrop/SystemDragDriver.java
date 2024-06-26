package com.android.launcher3.dragndrop;

import android.content.Context;
import android.view.DragEvent;
import android.view.MotionEvent;
import com.android.launcher3.DropTarget;
/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: DragDriver.java */
/* loaded from: classes.dex */
public class SystemDragDriver extends DragDriver {
    float mLastX;
    float mLastY;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemDragDriver(DragController dragController, Context context, DropTarget.DragObject dragObject) {
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
                return true;
            case 2:
                this.mLastX = dragEvent.getX();
                this.mLastY = dragEvent.getY();
                this.mEventListener.onDriverDragMove(dragEvent.getX(), dragEvent.getY());
                return true;
            case 3:
                this.mLastX = dragEvent.getX();
                this.mLastY = dragEvent.getY();
                this.mEventListener.onDriverDragMove(dragEvent.getX(), dragEvent.getY());
                this.mEventListener.onDriverDragEnd(this.mLastX, this.mLastY);
                return true;
            case 4:
                this.mEventListener.onDriverDragCancel();
                return true;
            case 5:
                return true;
            case 6:
                this.mEventListener.onDriverDragExitWindow();
                return true;
            default:
                return false;
        }
    }
}
