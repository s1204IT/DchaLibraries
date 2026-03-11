package com.android.launcher3.accessibility;

import com.android.launcher3.CellLayout;
import com.android.launcher3.R;
import com.android.launcher3.folder.FolderPagedView;

public class FolderAccessibilityHelper extends DragAndDropAccessibilityDelegate {
    private final FolderPagedView mParent;
    private final int mStartPosition;

    public FolderAccessibilityHelper(CellLayout cellLayout) {
        super(cellLayout);
        this.mParent = (FolderPagedView) cellLayout.getParent();
        this.mStartPosition = this.mParent.indexOfChild(cellLayout) * cellLayout.getCountX() * cellLayout.getCountY();
    }

    @Override
    protected int intersectsValidDropTarget(int i) {
        return Math.min(i, (this.mParent.getAllocatedContentSize() - this.mStartPosition) - 1);
    }

    @Override
    protected String getLocationDescriptionForIconDrop(int i) {
        return this.mContext.getString(R.string.move_to_position, Integer.valueOf(i + this.mStartPosition + 1));
    }

    @Override
    protected String getConfirmationForIconDrop(int i) {
        return this.mContext.getString(R.string.item_moved);
    }
}
