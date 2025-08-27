package com.android.launcher3.accessibility;

import android.view.ViewGroup;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragOptions;

/* loaded from: classes.dex */
public class AccessibleDragListenerAdapter implements DragController.DragListener {
    private final int mDragType;
    private final ViewGroup mViewGroup;

    public AccessibleDragListenerAdapter(ViewGroup viewGroup, int i) {
        this.mViewGroup = viewGroup;
        this.mDragType = i;
    }

    @Override // com.android.launcher3.dragndrop.DragController.DragListener
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        enableAccessibleDrag(true);
    }

    @Override // com.android.launcher3.dragndrop.DragController.DragListener
    public void onDragEnd() {
        enableAccessibleDrag(false);
        Launcher.getLauncher(this.mViewGroup.getContext()).getDragController().removeDragListener(this);
    }

    protected void enableAccessibleDrag(boolean z) {
        for (int i = 0; i < this.mViewGroup.getChildCount(); i++) {
            setEnableForLayout((CellLayout) this.mViewGroup.getChildAt(i), z);
        }
    }

    protected final void setEnableForLayout(CellLayout cellLayout, boolean z) {
        cellLayout.enableAccessibleDrag(z, this.mDragType);
    }
}
