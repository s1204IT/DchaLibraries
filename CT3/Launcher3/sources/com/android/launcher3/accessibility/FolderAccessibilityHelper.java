package com.android.launcher3.accessibility;

import com.android.launcher3.CellLayout;
import com.android.launcher3.FolderPagedView;
import com.android.launcher3.R;

public class FolderAccessibilityHelper extends DragAndDropAccessibilityDelegate {
    private final FolderPagedView mParent;
    private final int mStartPosition;

    public FolderAccessibilityHelper(CellLayout layout) {
        super(layout);
        this.mParent = (FolderPagedView) layout.getParent();
        int index = this.mParent.indexOfChild(layout);
        this.mStartPosition = layout.getCountX() * index * layout.getCountY();
    }

    @Override
    protected int intersectsValidDropTarget(int id) {
        return Math.min(id, (this.mParent.getAllocatedContentSize() - this.mStartPosition) - 1);
    }

    @Override
    protected String getLocationDescriptionForIconDrop(int id) {
        return this.mContext.getString(R.string.move_to_position, Integer.valueOf(this.mStartPosition + id + 1));
    }

    @Override
    protected String getConfirmationForIconDrop(int id) {
        return this.mContext.getString(R.string.item_moved);
    }
}
