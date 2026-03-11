package com.android.launcher3.dragndrop;

import android.view.DragEvent;

class InternalDragDriver extends DragDriver {
    InternalDragDriver(DragController dragController) {
        super(dragController);
    }

    @Override
    public boolean onDragEvent(DragEvent dragEvent) {
        return false;
    }
}
