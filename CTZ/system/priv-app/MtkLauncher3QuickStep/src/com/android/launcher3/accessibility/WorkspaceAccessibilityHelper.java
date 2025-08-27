package com.android.launcher3.accessibility;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.TextUtils;
import android.view.View;
import com.android.launcher3.AppInfo;
import com.android.launcher3.CellLayout;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.dragndrop.DragLayer;
import java.util.Iterator;

/* loaded from: classes.dex */
public class WorkspaceAccessibilityHelper extends DragAndDropAccessibilityDelegate {
    private final int[] mTempCords;
    private final Rect mTempRect;

    public WorkspaceAccessibilityHelper(CellLayout cellLayout) {
        super(cellLayout);
        this.mTempRect = new Rect();
        this.mTempCords = new int[2];
    }

    @Override // com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate
    protected int intersectsValidDropTarget(int i) {
        int countX = this.mView.getCountX();
        int countY = this.mView.getCountY();
        int i2 = i % countX;
        int i3 = i / countX;
        LauncherAccessibilityDelegate.DragInfo dragInfo = this.mDelegate.getDragInfo();
        if (dragInfo.dragType == LauncherAccessibilityDelegate.DragType.WIDGET && !this.mView.acceptsWidget()) {
            return -1;
        }
        if (dragInfo.dragType == LauncherAccessibilityDelegate.DragType.WIDGET) {
            int i4 = dragInfo.info.spanX;
            int i5 = dragInfo.info.spanY;
            for (int i6 = 0; i6 < i4; i6++) {
                for (int i7 = 0; i7 < i5; i7++) {
                    int i8 = i2 - i6;
                    int i9 = i3 - i7;
                    if (i8 >= 0 && i9 >= 0) {
                        boolean z = true;
                        for (int i10 = i8; i10 < i8 + i4 && z; i10++) {
                            for (int i11 = i9; i11 < i9 + i5; i11++) {
                                if (i10 >= countX || i11 >= countY || this.mView.isOccupied(i10, i11)) {
                                    z = false;
                                    break;
                                }
                            }
                        }
                        if (z) {
                            return i8 + (countX * i9);
                        }
                    }
                }
            }
            return -1;
        }
        View childAt = this.mView.getChildAt(i2, i3);
        if (childAt == null || childAt == dragInfo.item) {
            return i;
        }
        if (dragInfo.dragType != LauncherAccessibilityDelegate.DragType.FOLDER) {
            ItemInfo itemInfo = (ItemInfo) childAt.getTag();
            if ((itemInfo instanceof AppInfo) || (itemInfo instanceof FolderInfo) || (itemInfo instanceof ShortcutInfo)) {
                return i;
            }
        }
        return -1;
    }

    @Override // com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate
    protected String getConfirmationForIconDrop(int i) {
        int countX = i % this.mView.getCountX();
        int countX2 = i / this.mView.getCountX();
        LauncherAccessibilityDelegate.DragInfo dragInfo = this.mDelegate.getDragInfo();
        View childAt = this.mView.getChildAt(countX, countX2);
        if (childAt == null || childAt == dragInfo.item) {
            return this.mContext.getString(R.string.item_moved);
        }
        ItemInfo itemInfo = (ItemInfo) childAt.getTag();
        if ((itemInfo instanceof AppInfo) || (itemInfo instanceof ShortcutInfo)) {
            return this.mContext.getString(R.string.folder_created);
        }
        if (itemInfo instanceof FolderInfo) {
            return this.mContext.getString(R.string.added_to_folder);
        }
        return "";
    }

    @Override // com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate, android.support.v4.widget.ExploreByTouchHelper
    protected void onPopulateNodeForVirtualView(int i, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
        super.onPopulateNodeForVirtualView(i, accessibilityNodeInfoCompat);
        DragLayer dragLayer = Launcher.getLauncher(this.mView.getContext()).getDragLayer();
        int[] iArr = this.mTempCords;
        this.mTempCords[1] = 0;
        iArr[0] = 0;
        float descendantCoordRelativeToSelf = dragLayer.getDescendantCoordRelativeToSelf(this.mView, this.mTempCords);
        accessibilityNodeInfoCompat.getBoundsInParent(this.mTempRect);
        this.mTempRect.left = this.mTempCords[0] + ((int) (this.mTempRect.left * descendantCoordRelativeToSelf));
        this.mTempRect.right = this.mTempCords[0] + ((int) (this.mTempRect.right * descendantCoordRelativeToSelf));
        this.mTempRect.top = this.mTempCords[1] + ((int) (this.mTempRect.top * descendantCoordRelativeToSelf));
        this.mTempRect.bottom = this.mTempCords[1] + ((int) (this.mTempRect.bottom * descendantCoordRelativeToSelf));
        accessibilityNodeInfoCompat.setBoundsInScreen(this.mTempRect);
    }

    @Override // com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate
    protected String getLocationDescriptionForIconDrop(int i) {
        int countX = i % this.mView.getCountX();
        int countX2 = i / this.mView.getCountX();
        LauncherAccessibilityDelegate.DragInfo dragInfo = this.mDelegate.getDragInfo();
        View childAt = this.mView.getChildAt(countX, countX2);
        if (childAt == null || childAt == dragInfo.item) {
            return this.mView.getItemMoveDescription(countX, countX2);
        }
        return getDescriptionForDropOver(childAt, this.mContext);
    }

    public static String getDescriptionForDropOver(View view, Context context) {
        ItemInfo itemInfo = (ItemInfo) view.getTag();
        if (itemInfo instanceof ShortcutInfo) {
            return context.getString(R.string.create_folder_with, itemInfo.title);
        }
        if (itemInfo instanceof FolderInfo) {
            if (TextUtils.isEmpty(itemInfo.title)) {
                ShortcutInfo shortcutInfo = null;
                Iterator<ShortcutInfo> it = ((FolderInfo) itemInfo).contents.iterator();
                while (it.hasNext()) {
                    ShortcutInfo next = it.next();
                    if (shortcutInfo == null || shortcutInfo.rank > next.rank) {
                        shortcutInfo = next;
                    }
                }
                if (shortcutInfo != null) {
                    return context.getString(R.string.add_to_folder_with_app, shortcutInfo.title);
                }
            }
            return context.getString(R.string.add_to_folder, itemInfo.title);
        }
        return "";
    }
}
