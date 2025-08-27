package com.android.launcher3.accessibility;

import com.android.launcher3.CellLayout;
import com.android.launcher3.R;
import com.android.launcher3.folder.FolderPagedView;

/* loaded from: classes.dex */
public class FolderAccessibilityHelper extends DragAndDropAccessibilityDelegate {
    private final FolderPagedView mParent;
    private final int mStartPosition;

    public FolderAccessibilityHelper(CellLayout cellLayout) {
        super(cellLayout);
        this.mParent = (FolderPagedView) cellLayout.getParent();
        this.mStartPosition = this.mParent.indexOfChild(cellLayout) * cellLayout.getCountX() * cellLayout.getCountY();
    }

    @Override // com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate
    protected int intersectsValidDropTarget(int i) {
        return Math.min(i, (this.mParent.getAllocatedContentSize() - this.mStartPosition) - 1);
    }

    @Override // com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate
    protected String getLocationDescriptionForIconDrop(int i) {
        return this.mContext.getString(R.string.move_to_position, Integer.valueOf(i + this.mStartPosition + 1));
    }

    @Override // com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate
    protected String getConfirmationForIconDrop(int i) {
        return this.mContext.getString(R.string.item_moved);
    }
}
