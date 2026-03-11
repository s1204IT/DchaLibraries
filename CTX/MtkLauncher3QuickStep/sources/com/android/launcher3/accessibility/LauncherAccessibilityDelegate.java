package com.android.launcher3.accessibility;

import android.app.AlertDialog;
import android.appwidget.AppWidgetProviderInfo;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.launcher3.AppInfo;
import com.android.launcher3.AppWidgetResizeFrame;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.ButtonDropTarget;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DropTarget;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherState;
import com.android.launcher3.PendingAddItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import java.util.ArrayList;

public class LauncherAccessibilityDelegate extends View.AccessibilityDelegate implements DragController.DragListener {
    protected static final int ADD_TO_WORKSPACE = 2131361793;
    public static final int DEEP_SHORTCUTS = 2131361795;
    protected static final int MOVE = 2131361799;
    protected static final int MOVE_TO_WORKSPACE = 2131361802;
    public static final int RECONFIGURE = 2131361803;
    public static final int REMOVE = 2131361804;
    protected static final int RESIZE = 2131361805;
    public static final int SHORTCUTS_AND_NOTIFICATIONS = 2131361806;
    private static final String TAG = "LauncherAccessibilityDelegate";
    public static final int UNINSTALL = 2131361808;
    protected final SparseArray<AccessibilityNodeInfo.AccessibilityAction> mActions = new SparseArray<>();
    private DragInfo mDragInfo = null;
    final Launcher mLauncher;

    public static class DragInfo {
        public DragType dragType;
        public ItemInfo info;
        public View item;
    }

    public enum DragType {
        ICON,
        FOLDER,
        WIDGET
    }

    public LauncherAccessibilityDelegate(Launcher launcher) {
        this.mLauncher = launcher;
        this.mActions.put(R.id.action_remove, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_remove, launcher.getText(R.string.remove_drop_target_label)));
        this.mActions.put(R.id.action_uninstall, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_uninstall, launcher.getText(R.string.uninstall_drop_target_label)));
        this.mActions.put(R.id.action_reconfigure, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_reconfigure, launcher.getText(R.string.gadget_setup_text)));
        this.mActions.put(R.id.action_add_to_workspace, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_add_to_workspace, launcher.getText(R.string.action_add_to_workspace)));
        this.mActions.put(R.id.action_move, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move, launcher.getText(R.string.action_move)));
        this.mActions.put(R.id.action_move_to_workspace, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_to_workspace, launcher.getText(R.string.action_move_to_workspace)));
        this.mActions.put(R.id.action_resize, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_resize, launcher.getText(R.string.action_resize)));
        this.mActions.put(R.id.action_deep_shortcuts, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_deep_shortcuts, launcher.getText(R.string.action_deep_shortcut)));
        this.mActions.put(R.id.action_shortcuts_and_notifications, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_deep_shortcuts, launcher.getText(R.string.shortcuts_menu_with_notifications_description)));
    }

    public void addAccessibilityAction(int i, int i2) {
        this.mActions.put(i, new AccessibilityNodeInfo.AccessibilityAction(i, this.mLauncher.getText(i2)));
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
        addSupportedActions(view, accessibilityNodeInfo, false);
    }

    public void addSupportedActions(View view, AccessibilityNodeInfo accessibilityNodeInfo, boolean z) {
        if (view.getTag() instanceof ItemInfo) {
            ItemInfo itemInfo = (ItemInfo) view.getTag();
            if (!z && DeepShortcutManager.supportsShortcuts(itemInfo)) {
                accessibilityNodeInfo.addAction(this.mActions.get(NotificationListener.getInstanceIfConnected() != null ? R.id.action_shortcuts_and_notifications : R.id.action_deep_shortcuts));
            }
            for (ButtonDropTarget buttonDropTarget : this.mLauncher.getDropTargetBar().getDropTargets()) {
                if (buttonDropTarget.supportsAccessibilityDrop(itemInfo, view)) {
                    accessibilityNodeInfo.addAction(this.mActions.get(buttonDropTarget.getAccessibilityAction()));
                }
            }
            if (!z && ((itemInfo instanceof ShortcutInfo) || (itemInfo instanceof LauncherAppWidgetInfo) || (itemInfo instanceof FolderInfo))) {
                accessibilityNodeInfo.addAction(this.mActions.get(R.id.action_move));
                if (itemInfo.container >= 0) {
                    accessibilityNodeInfo.addAction(this.mActions.get(R.id.action_move_to_workspace));
                } else if ((itemInfo instanceof LauncherAppWidgetInfo) && !getSupportedResizeActions(view, (LauncherAppWidgetInfo) itemInfo).isEmpty()) {
                    accessibilityNodeInfo.addAction(this.mActions.get(R.id.action_resize));
                }
            }
            if ((itemInfo instanceof AppInfo) || (itemInfo instanceof PendingAddItemInfo)) {
                accessibilityNodeInfo.addAction(this.mActions.get(R.id.action_add_to_workspace));
            }
        }
    }

    @Override
    public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
        if ((view.getTag() instanceof ItemInfo) && performAction(view, (ItemInfo) view.getTag(), i)) {
            return true;
        }
        return super.performAccessibilityAction(view, i, bundle);
    }

    public boolean performAction(final View view, final ItemInfo itemInfo, int i) {
        if (i == R.id.action_move) {
            beginAccessibleDrag(view, itemInfo);
        } else {
            if (i == R.id.action_add_to_workspace) {
                final int[] iArr = new int[2];
                final long jFindSpaceOnWorkspace = findSpaceOnWorkspace(itemInfo, iArr);
                this.mLauncher.getStateManager().goToState(LauncherState.NORMAL, true, new Runnable() {
                    @Override
                    public void run() {
                        if (itemInfo instanceof AppInfo) {
                            ShortcutInfo shortcutInfoMakeShortcut = ((AppInfo) itemInfo).makeShortcut();
                            LauncherAccessibilityDelegate.this.mLauncher.getModelWriter().addItemToDatabase(shortcutInfoMakeShortcut, -100L, jFindSpaceOnWorkspace, iArr[0], iArr[1]);
                            ArrayList arrayList = new ArrayList();
                            arrayList.add(shortcutInfoMakeShortcut);
                            LauncherAccessibilityDelegate.this.mLauncher.bindItems(arrayList, true);
                        } else if (itemInfo instanceof PendingAddItemInfo) {
                            PendingAddItemInfo pendingAddItemInfo = (PendingAddItemInfo) itemInfo;
                            Workspace workspace = LauncherAccessibilityDelegate.this.mLauncher.getWorkspace();
                            workspace.snapToPage(workspace.getPageIndexForScreenId(jFindSpaceOnWorkspace));
                            LauncherAccessibilityDelegate.this.mLauncher.addPendingItem(pendingAddItemInfo, -100L, jFindSpaceOnWorkspace, iArr, pendingAddItemInfo.spanX, pendingAddItemInfo.spanY);
                        }
                        LauncherAccessibilityDelegate.this.announceConfirmation(R.string.item_added_to_workspace);
                    }
                });
                return true;
            }
            if (i == R.id.action_move_to_workspace) {
                Folder open = Folder.getOpen(this.mLauncher);
                open.close(true);
                ShortcutInfo shortcutInfo = (ShortcutInfo) itemInfo;
                open.getInfo().remove(shortcutInfo, false);
                int[] iArr2 = new int[2];
                this.mLauncher.getModelWriter().moveItemInDatabase(shortcutInfo, -100L, findSpaceOnWorkspace(itemInfo, iArr2), iArr2[0], iArr2[1]);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList arrayList = new ArrayList();
                        arrayList.add(itemInfo);
                        LauncherAccessibilityDelegate.this.mLauncher.bindItems(arrayList, true);
                        LauncherAccessibilityDelegate.this.announceConfirmation(R.string.item_moved);
                    }
                });
            } else {
                if (i == R.id.action_resize) {
                    final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) itemInfo;
                    final ArrayList<Integer> supportedResizeActions = getSupportedResizeActions(view, launcherAppWidgetInfo);
                    CharSequence[] charSequenceArr = new CharSequence[supportedResizeActions.size()];
                    for (int i2 = 0; i2 < supportedResizeActions.size(); i2++) {
                        charSequenceArr[i2] = this.mLauncher.getText(supportedResizeActions.get(i2).intValue());
                    }
                    new AlertDialog.Builder(this.mLauncher).setTitle(R.string.action_resize).setItems(charSequenceArr, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i3) {
                            LauncherAccessibilityDelegate.this.performResizeAction(((Integer) supportedResizeActions.get(i3)).intValue(), view, launcherAppWidgetInfo);
                            dialogInterface.dismiss();
                        }
                    }).show();
                    return true;
                }
                if (i == R.id.action_deep_shortcuts) {
                    return PopupContainerWithArrow.showForIcon((BubbleTextView) view) != null;
                }
                for (ButtonDropTarget buttonDropTarget : this.mLauncher.getDropTargetBar().getDropTargets()) {
                    if (buttonDropTarget.supportsAccessibilityDrop(itemInfo, view) && i == buttonDropTarget.getAccessibilityAction()) {
                        buttonDropTarget.onAccessibilityDrop(view, itemInfo);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ArrayList<Integer> getSupportedResizeActions(View view, LauncherAppWidgetInfo launcherAppWidgetInfo) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        AppWidgetProviderInfo appWidgetInfo = ((LauncherAppWidgetHostView) view).getAppWidgetInfo();
        if (appWidgetInfo == null) {
            return arrayList;
        }
        CellLayout cellLayout = (CellLayout) view.getParent().getParent();
        if ((appWidgetInfo.resizeMode & 1) != 0) {
            if (cellLayout.isRegionVacant(launcherAppWidgetInfo.cellX + launcherAppWidgetInfo.spanX, launcherAppWidgetInfo.cellY, 1, launcherAppWidgetInfo.spanY) || cellLayout.isRegionVacant(launcherAppWidgetInfo.cellX - 1, launcherAppWidgetInfo.cellY, 1, launcherAppWidgetInfo.spanY)) {
                arrayList.add(Integer.valueOf(R.string.action_increase_width));
            }
            if (launcherAppWidgetInfo.spanX > launcherAppWidgetInfo.minSpanX && launcherAppWidgetInfo.spanX > 1) {
                arrayList.add(Integer.valueOf(R.string.action_decrease_width));
            }
        }
        if ((appWidgetInfo.resizeMode & 2) != 0) {
            if (cellLayout.isRegionVacant(launcherAppWidgetInfo.cellX, launcherAppWidgetInfo.cellY + launcherAppWidgetInfo.spanY, launcherAppWidgetInfo.spanX, 1) || cellLayout.isRegionVacant(launcherAppWidgetInfo.cellX, launcherAppWidgetInfo.cellY - 1, launcherAppWidgetInfo.spanX, 1)) {
                arrayList.add(Integer.valueOf(R.string.action_increase_height));
            }
            if (launcherAppWidgetInfo.spanY > launcherAppWidgetInfo.minSpanY && launcherAppWidgetInfo.spanY > 1) {
                arrayList.add(Integer.valueOf(R.string.action_decrease_height));
            }
        }
        return arrayList;
    }

    void performResizeAction(int i, View view, LauncherAppWidgetInfo launcherAppWidgetInfo) {
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
        CellLayout cellLayout = (CellLayout) view.getParent().getParent();
        cellLayout.markCellsAsUnoccupiedForView(view);
        if (i == R.string.action_increase_width) {
            if ((view.getLayoutDirection() == 1 && cellLayout.isRegionVacant(launcherAppWidgetInfo.cellX - 1, launcherAppWidgetInfo.cellY, 1, launcherAppWidgetInfo.spanY)) || !cellLayout.isRegionVacant(launcherAppWidgetInfo.cellX + launcherAppWidgetInfo.spanX, launcherAppWidgetInfo.cellY, 1, launcherAppWidgetInfo.spanY)) {
                layoutParams.cellX--;
                launcherAppWidgetInfo.cellX--;
            }
            layoutParams.cellHSpan++;
            launcherAppWidgetInfo.spanX++;
        } else if (i == R.string.action_decrease_width) {
            layoutParams.cellHSpan--;
            launcherAppWidgetInfo.spanX--;
        } else if (i == R.string.action_increase_height) {
            if (!cellLayout.isRegionVacant(launcherAppWidgetInfo.cellX, launcherAppWidgetInfo.cellY + launcherAppWidgetInfo.spanY, launcherAppWidgetInfo.spanX, 1)) {
                layoutParams.cellY--;
                launcherAppWidgetInfo.cellY--;
            }
            layoutParams.cellVSpan++;
            launcherAppWidgetInfo.spanY++;
        } else if (i == R.string.action_decrease_height) {
            layoutParams.cellVSpan--;
            launcherAppWidgetInfo.spanY--;
        }
        cellLayout.markCellsAsOccupiedForView(view);
        Rect rect = new Rect();
        AppWidgetResizeFrame.getWidgetSizeRanges(this.mLauncher, launcherAppWidgetInfo.spanX, launcherAppWidgetInfo.spanY, rect);
        ((LauncherAppWidgetHostView) view).updateAppWidgetSize(null, rect.left, rect.top, rect.right, rect.bottom);
        view.requestLayout();
        this.mLauncher.getModelWriter().updateItemInDatabase(launcherAppWidgetInfo);
        announceConfirmation(this.mLauncher.getString(R.string.widget_resized, new Object[]{Integer.valueOf(launcherAppWidgetInfo.spanX), Integer.valueOf(launcherAppWidgetInfo.spanY)}));
    }

    void announceConfirmation(int i) {
        announceConfirmation(this.mLauncher.getResources().getString(i));
    }

    void announceConfirmation(String str) {
        this.mLauncher.getDragLayer().announceForAccessibility(str);
    }

    public boolean isInAccessibleDrag() {
        return this.mDragInfo != null;
    }

    public DragInfo getDragInfo() {
        return this.mDragInfo;
    }

    public void handleAccessibleDrop(View view, Rect rect, String str) {
        if (isInAccessibleDrag()) {
            int[] iArr = new int[2];
            if (rect == null) {
                iArr[0] = view.getWidth() / 2;
                iArr[1] = view.getHeight() / 2;
            } else {
                iArr[0] = rect.centerX();
                iArr[1] = rect.centerY();
            }
            this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(view, iArr);
            this.mLauncher.getDragController().completeAccessibleDrag(iArr);
            if (!TextUtils.isEmpty(str)) {
                announceConfirmation(str);
            }
        }
    }

    public void beginAccessibleDrag(View view, ItemInfo itemInfo) {
        this.mDragInfo = new DragInfo();
        this.mDragInfo.info = itemInfo;
        this.mDragInfo.item = view;
        this.mDragInfo.dragType = DragType.ICON;
        if (itemInfo instanceof FolderInfo) {
            this.mDragInfo.dragType = DragType.FOLDER;
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            this.mDragInfo.dragType = DragType.WIDGET;
        }
        Rect rect = new Rect();
        this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(view, rect);
        this.mLauncher.getDragController().prepareAccessibleDrag(rect.centerX(), rect.centerY());
        this.mLauncher.getDragController().addDragListener(this);
        DragOptions dragOptions = new DragOptions();
        dragOptions.isAccessibleDrag = true;
        ItemLongClickListener.beginDrag(view, this.mLauncher, itemInfo, dragOptions);
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
    }

    @Override
    public void onDragEnd() {
        this.mLauncher.getDragController().removeDragListener(this);
        this.mDragInfo = null;
    }

    protected long findSpaceOnWorkspace(ItemInfo itemInfo, int[] iArr) {
        Workspace workspace = this.mLauncher.getWorkspace();
        ArrayList<Long> screenOrder = workspace.getScreenOrder();
        int currentPage = workspace.getCurrentPage();
        long jLongValue = screenOrder.get(currentPage).longValue();
        boolean zFindCellForSpan = ((CellLayout) workspace.getPageAt(currentPage)).findCellForSpan(iArr, itemInfo.spanX, itemInfo.spanY);
        for (int i = 0; !zFindCellForSpan && i < screenOrder.size(); i++) {
            jLongValue = screenOrder.get(i).longValue();
            zFindCellForSpan = ((CellLayout) workspace.getPageAt(i)).findCellForSpan(iArr, itemInfo.spanX, itemInfo.spanY);
        }
        if (zFindCellForSpan) {
            return jLongValue;
        }
        workspace.addExtraEmptyScreen();
        long jCommitExtraEmptyScreen = workspace.commitExtraEmptyScreen();
        if (!workspace.getScreenWithId(jCommitExtraEmptyScreen).findCellForSpan(iArr, itemInfo.spanX, itemInfo.spanY)) {
            Log.wtf(TAG, "Not enough space on an empty screen");
        }
        return jCommitExtraEmptyScreen;
    }
}
