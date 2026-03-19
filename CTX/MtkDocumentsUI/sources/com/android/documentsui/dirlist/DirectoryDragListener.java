package com.android.documentsui.dirlist;

import android.view.DragEvent;
import android.view.View;
import com.android.documentsui.ItemDragListener;
import java.util.TimerTask;

class DirectoryDragListener extends ItemDragListener<DragHost<?>> {
    DirectoryDragListener(DragHost<?> dragHost) {
        super(dragHost);
    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {
        boolean zOnDrag = super.onDrag(view, dragEvent);
        if (dragEvent.getAction() == 4) {
            ((DragHost) this.mDragHost).dragStopped(dragEvent.getResult());
        }
        return zOnDrag;
    }

    @Override
    public boolean handleDropEventChecked(View view, DragEvent dragEvent) {
        return ((DragHost) this.mDragHost).handleDropEvent(view, dragEvent);
    }

    @Override
    public TimerTask createOpenTask(View view, DragEvent dragEvent) {
        if (((DragHost) this.mDragHost).canSpringOpen(view)) {
            return super.createOpenTask(view, dragEvent);
        }
        return null;
    }
}
