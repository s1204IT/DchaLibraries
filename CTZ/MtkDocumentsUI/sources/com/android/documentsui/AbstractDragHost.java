package com.android.documentsui;

import android.view.View;
import com.android.documentsui.ItemDragListener;

public abstract class AbstractDragHost implements ItemDragListener.DragHost {
    protected DragAndDropManager mDragAndDropManager;

    public AbstractDragHost(DragAndDropManager dragAndDropManager) {
        this.mDragAndDropManager = dragAndDropManager;
    }

    @Override
    public void onDragExited(View view) {
        this.mDragAndDropManager.resetState(view);
    }

    @Override
    public void onDragEnded() {
        this.mDragAndDropManager.dragEnded();
    }
}
