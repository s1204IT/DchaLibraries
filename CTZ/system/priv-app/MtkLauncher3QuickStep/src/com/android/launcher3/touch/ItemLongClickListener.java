package com.android.launcher3.touch;

import android.view.View;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.folder.Folder;

/* loaded from: classes.dex */
public class ItemLongClickListener {
    public static View.OnLongClickListener INSTANCE_WORKSPACE = new View.OnLongClickListener() { // from class: com.android.launcher3.touch.-$$Lambda$ItemLongClickListener$n_ku6Bnp7SQn-CFIT2R46R_RyW8
        @Override // android.view.View.OnLongClickListener
        public final boolean onLongClick(View view) {
            return ItemLongClickListener.onWorkspaceItemLongClick(view);
        }
    };
    public static View.OnLongClickListener INSTANCE_ALL_APPS = new View.OnLongClickListener() { // from class: com.android.launcher3.touch.-$$Lambda$ItemLongClickListener$pr3KEnHIBNhjVshSoZRttm8yUa8
        @Override // android.view.View.OnLongClickListener
        public final boolean onLongClick(View view) {
            return ItemLongClickListener.onAllAppsItemLongClick(view);
        }
    };

    private static boolean onWorkspaceItemLongClick(View view) {
        Launcher launcher = Launcher.getLauncher(view.getContext());
        if (!canStartDrag(launcher)) {
            return false;
        }
        if ((!launcher.isInState(LauncherState.NORMAL) && !launcher.isInState(LauncherState.OVERVIEW)) || !(view.getTag() instanceof ItemInfo)) {
            return false;
        }
        launcher.setWaitingForResult(null);
        beginDrag(view, launcher, (ItemInfo) view.getTag(), new DragOptions());
        return true;
    }

    public static void beginDrag(View view, Launcher launcher, ItemInfo itemInfo, DragOptions dragOptions) {
        Folder open;
        if (itemInfo.container >= 0 && (open = Folder.getOpen(launcher)) != null) {
            if (!open.getItemsInReadingOrder().contains(view)) {
                open.close(true);
            } else {
                open.startDrag(view, dragOptions);
                return;
            }
        }
        launcher.getWorkspace().startDrag(new CellLayout.CellInfo(view, itemInfo), dragOptions);
    }

    private static boolean onAllAppsItemLongClick(final View view) {
        Launcher launcher = Launcher.getLauncher(view.getContext());
        if (!canStartDrag(launcher)) {
            return false;
        }
        if ((!launcher.isInState(LauncherState.ALL_APPS) && !launcher.isInState(LauncherState.OVERVIEW)) || launcher.getWorkspace().isSwitchingState()) {
            return false;
        }
        final DragController dragController = launcher.getDragController();
        dragController.addDragListener(new DragController.DragListener() { // from class: com.android.launcher3.touch.ItemLongClickListener.1
            @Override // com.android.launcher3.dragndrop.DragController.DragListener
            public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
                view.setVisibility(4);
            }

            @Override // com.android.launcher3.dragndrop.DragController.DragListener
            public void onDragEnd() {
                view.setVisibility(0);
                dragController.removeDragListener(this);
            }
        });
        DeviceProfile deviceProfile = launcher.getDeviceProfile();
        DragOptions dragOptions = new DragOptions();
        dragOptions.intrinsicIconScaleFactor = deviceProfile.allAppsIconSizePx / deviceProfile.iconSizePx;
        launcher.getWorkspace().beginDragShared(view, launcher.getAppsView(), dragOptions);
        return false;
    }

    public static boolean canStartDrag(Launcher launcher) {
        if (launcher == null || launcher.isWorkspaceLocked() || launcher.getDragController().isDragging()) {
            return false;
        }
        return true;
    }
}
