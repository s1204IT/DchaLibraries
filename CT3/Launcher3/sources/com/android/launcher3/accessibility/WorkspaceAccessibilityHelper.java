package com.android.launcher3.accessibility;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import com.android.launcher3.AppInfo;
import com.android.launcher3.CellLayout;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;

public class WorkspaceAccessibilityHelper extends DragAndDropAccessibilityDelegate {
    public WorkspaceAccessibilityHelper(CellLayout layout) {
        super(layout);
    }

    @Override
    protected int intersectsValidDropTarget(int id) {
        int mCountX = this.mView.getCountX();
        int mCountY = this.mView.getCountY();
        int x = id % mCountX;
        int y = id / mCountX;
        LauncherAccessibilityDelegate.DragInfo dragInfo = this.mDelegate.getDragInfo();
        if (dragInfo.dragType == LauncherAccessibilityDelegate.DragType.WIDGET && this.mView.isHotseat()) {
            return -1;
        }
        if (dragInfo.dragType == LauncherAccessibilityDelegate.DragType.WIDGET) {
            int spanX = dragInfo.info.spanX;
            int spanY = dragInfo.info.spanY;
            for (int m = 0; m < spanX; m++) {
                for (int n = 0; n < spanY; n++) {
                    boolean fits = true;
                    int x0 = x - m;
                    int y0 = y - n;
                    if (x0 >= 0 && y0 >= 0) {
                        for (int i = x0; i < x0 + spanX && fits; i++) {
                            for (int j = y0; j < y0 + spanY; j++) {
                                if (i >= mCountX || j >= mCountY || this.mView.isOccupied(i, j)) {
                                    fits = false;
                                    break;
                                }
                            }
                        }
                        if (fits) {
                            return (mCountX * y0) + x0;
                        }
                    }
                }
            }
            return -1;
        }
        View child = this.mView.getChildAt(x, y);
        if (child == null || child == dragInfo.item) {
            return id;
        }
        if (dragInfo.dragType != LauncherAccessibilityDelegate.DragType.FOLDER) {
            ItemInfo info = (ItemInfo) child.getTag();
            if ((info instanceof AppInfo) || (info instanceof FolderInfo) || (info instanceof ShortcutInfo)) {
                return id;
            }
            return -1;
        }
        return -1;
    }

    @Override
    protected String getConfirmationForIconDrop(int id) {
        int x = id % this.mView.getCountX();
        int y = id / this.mView.getCountX();
        LauncherAccessibilityDelegate.DragInfo dragInfo = this.mDelegate.getDragInfo();
        View child = this.mView.getChildAt(x, y);
        if (child == null || child == dragInfo.item) {
            return this.mContext.getString(R.string.item_moved);
        }
        ItemInfo info = (ItemInfo) child.getTag();
        if ((info instanceof AppInfo) || (info instanceof ShortcutInfo)) {
            return this.mContext.getString(R.string.folder_created);
        }
        if (info instanceof FolderInfo) {
            return this.mContext.getString(R.string.added_to_folder);
        }
        return "";
    }

    @Override
    protected String getLocationDescriptionForIconDrop(int id) {
        int x = id % this.mView.getCountX();
        int y = id / this.mView.getCountX();
        LauncherAccessibilityDelegate.DragInfo dragInfo = this.mDelegate.getDragInfo();
        View child = this.mView.getChildAt(x, y);
        if (child == null || child == dragInfo.item) {
            return this.mView.isHotseat() ? this.mContext.getString(R.string.move_to_hotseat_position, Integer.valueOf(id + 1)) : this.mContext.getString(R.string.move_to_empty_cell, Integer.valueOf(y + 1), Integer.valueOf(x + 1));
        }
        return getDescriptionForDropOver(child, this.mContext);
    }

    public static String getDescriptionForDropOver(View overChild, Context context) {
        ItemInfo info = (ItemInfo) overChild.getTag();
        if (info instanceof ShortcutInfo) {
            return context.getString(R.string.create_folder_with, info.title);
        }
        if (info instanceof FolderInfo) {
            if (TextUtils.isEmpty(info.title)) {
                FolderInfo folder = (FolderInfo) info;
                ShortcutInfo firstItem = null;
                for (ShortcutInfo shortcut : folder.contents) {
                    if (firstItem == null || firstItem.rank > shortcut.rank) {
                        firstItem = shortcut;
                    }
                }
                if (firstItem != null) {
                    return context.getString(R.string.add_to_folder_with_app, firstItem.title);
                }
            }
            return context.getString(R.string.add_to_folder, info.title);
        }
        return "";
    }
}
