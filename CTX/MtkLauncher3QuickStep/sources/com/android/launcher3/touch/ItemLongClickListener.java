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

public class ItemLongClickListener {
    public static View.OnLongClickListener INSTANCE_WORKSPACE = new View.OnLongClickListener() {
        @Override
        public final boolean onLongClick(View view) {
            return ItemLongClickListener.onWorkspaceItemLongClick(view);
        }
    };
    public static View.OnLongClickListener INSTANCE_ALL_APPS = new View.OnLongClickListener() {
        @Override
        public final boolean onLongClick(View view) {
            return ItemLongClickListener.onAllAppsItemLongClick(view);
        }
    };

    public static boolean onWorkspaceItemLongClick(View view) {
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

    public static boolean onAllAppsItemLongClick(final View view) {
        Launcher launcher = Launcher.getLauncher(view.getContext());
        if (!canStartDrag(launcher)) {
            return false;
        }
        if ((!launcher.isInState(LauncherState.ALL_APPS) && !launcher.isInState(LauncherState.OVERVIEW)) || launcher.getWorkspace().isSwitchingState()) {
            return false;
        }
        final DragController dragController = launcher.getDragController();
        dragController.addDragListener(new DragController.DragListener() {
            @Override
            public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
                view.setVisibility(4);
            }

            @Override
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
