package com.android.launcher3;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.DropTarget;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.userevent.nano.LauncherLogProto;

public class DeleteDropTarget extends ButtonDropTarget {
    private int mControlType;

    public DeleteDropTarget(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DeleteDropTarget(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mControlType = 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHoverColor = getResources().getColor(R.color.delete_target_hover_tint);
        setDrawable(R.drawable.ic_remove_shadow);
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        super.onDragStart(dragObject, dragOptions);
        setTextBasedOnDragSource(dragObject.dragInfo);
        setControlTypeBasedOnDragSource(dragObject.dragInfo);
    }

    @Override
    public boolean supportsAccessibilityDrop(ItemInfo itemInfo, View view) {
        return (itemInfo instanceof ShortcutInfo) || (itemInfo instanceof LauncherAppWidgetInfo) || (itemInfo instanceof FolderInfo);
    }

    @Override
    public int getAccessibilityAction() {
        return R.id.action_remove;
    }

    @Override
    protected boolean supportsDrop(ItemInfo itemInfo) {
        return true;
    }

    private void setTextBasedOnDragSource(ItemInfo itemInfo) {
        int i;
        if (!TextUtils.isEmpty(this.mText)) {
            Resources resources = getResources();
            if (itemInfo.id != -1) {
                i = R.string.remove_drop_target_label;
            } else {
                i = android.R.string.cancel;
            }
            this.mText = resources.getString(i);
            requestLayout();
        }
    }

    private void setControlTypeBasedOnDragSource(ItemInfo itemInfo) {
        this.mControlType = itemInfo.id != -1 ? 5 : 14;
    }

    @Override
    public void completeDrop(DropTarget.DragObject dragObject) {
        ItemInfo itemInfo = dragObject.dragInfo;
        if ((dragObject.dragSource instanceof Workspace) || (dragObject.dragSource instanceof Folder)) {
            onAccessibilityDrop(null, itemInfo);
        }
    }

    @Override
    public void onAccessibilityDrop(View view, ItemInfo itemInfo) {
        this.mLauncher.removeItem(view, itemInfo, true);
        this.mLauncher.getWorkspace().stripEmptyScreens();
        this.mLauncher.getDragLayer().announceForAccessibility(getContext().getString(R.string.item_removed));
    }

    @Override
    public LauncherLogProto.Target getDropTargetForLogging() {
        LauncherLogProto.Target targetNewTarget = LoggerUtils.newTarget(2);
        targetNewTarget.controlType = this.mControlType;
        return targetNewTarget;
    }
}
