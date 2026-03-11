package com.android.launcher3;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.CellLayout;
import com.android.launcher3.util.FocusLogic;

public class FocusHelper {

    public static class PagedFolderKeyEventListener implements View.OnKeyListener {
        private final Folder mFolder;

        public PagedFolderKeyEventListener(Folder folder) {
            this.mFolder = folder;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent e) {
            boolean consume = FocusLogic.shouldConsume(keyCode);
            if (e.getAction() == 1) {
                return consume;
            }
            if (!(v.getParent() instanceof ShortcutAndWidgetContainer)) {
                if (LauncherAppState.isDogfoodBuild()) {
                    throw new IllegalStateException("Parent of the focused item is not supported.");
                }
                return false;
            }
            ShortcutAndWidgetContainer itemContainer = (ShortcutAndWidgetContainer) v.getParent();
            CellLayout cellLayout = (CellLayout) itemContainer.getParent();
            int iconIndex = itemContainer.indexOfChild(v);
            FolderPagedView pagedView = (FolderPagedView) cellLayout.getParent();
            int pageIndex = pagedView.indexOfChild(cellLayout);
            int pageCount = pagedView.getPageCount();
            boolean isLayoutRtl = Utilities.isRtl(v.getResources());
            int[][] matrix = FocusLogic.createSparseMatrix(cellLayout);
            int newIconIndex = FocusLogic.handleKeyEvent(keyCode, matrix, iconIndex, pageIndex, pageCount, isLayoutRtl);
            if (newIconIndex == -1) {
                handleNoopKey(keyCode, v);
                return consume;
            }
            View child = null;
            switch (newIconIndex) {
                case -10:
                case -9:
                    ShortcutAndWidgetContainer newParent = FocusHelper.getCellLayoutChildrenForIndex(pagedView, pageIndex + 1);
                    if (newParent != null) {
                        pagedView.snapToPage(pageIndex + 1);
                        child = FocusLogic.getAdjacentChildInNextFolderPage(newParent, v, newIconIndex);
                    }
                    break;
                case -8:
                    ShortcutAndWidgetContainer newParent2 = FocusHelper.getCellLayoutChildrenForIndex(pagedView, pageIndex + 1);
                    if (newParent2 != null) {
                        pagedView.snapToPage(pageIndex + 1);
                        child = newParent2.getChildAt(0, 0);
                    }
                    break;
                case -7:
                    child = pagedView.getLastItem();
                    break;
                case -6:
                    child = cellLayout.getChildAt(0, 0);
                    break;
                case -5:
                case -2:
                    ShortcutAndWidgetContainer newParent3 = FocusHelper.getCellLayoutChildrenForIndex(pagedView, pageIndex - 1);
                    if (newParent3 != null) {
                        int row = ((CellLayout.LayoutParams) v.getLayoutParams()).cellY;
                        pagedView.snapToPage(pageIndex - 1);
                        child = newParent3.getChildAt((newIconIndex == -5) ^ newParent3.invertLayoutHorizontally() ? 0 : matrix.length - 1, row);
                    }
                    break;
                case -4:
                    ShortcutAndWidgetContainer newParent4 = FocusHelper.getCellLayoutChildrenForIndex(pagedView, pageIndex - 1);
                    if (newParent4 != null) {
                        pagedView.snapToPage(pageIndex - 1);
                        child = newParent4.getChildAt(matrix.length - 1, matrix[0].length - 1);
                    }
                    break;
                case -3:
                    ShortcutAndWidgetContainer newParent5 = FocusHelper.getCellLayoutChildrenForIndex(pagedView, pageIndex - 1);
                    if (newParent5 != null) {
                        pagedView.snapToPage(pageIndex - 1);
                        child = newParent5.getChildAt(0, 0);
                    }
                    break;
                default:
                    child = itemContainer.getChildAt(newIconIndex);
                    break;
            }
            if (child != null) {
                child.requestFocus();
                FocusHelper.playSoundEffect(keyCode, v);
            } else {
                handleNoopKey(keyCode, v);
            }
            return consume;
        }

        public void handleNoopKey(int keyCode, View v) {
            if (keyCode != 20) {
                return;
            }
            this.mFolder.mFolderName.requestFocus();
            FocusHelper.playSoundEffect(keyCode, v);
        }
    }

    static boolean handleHotseatButtonKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume = FocusLogic.shouldConsume(keyCode);
        if (e.getAction() == 1 || !consume) {
            return consume;
        }
        Launcher launcher = (Launcher) v.getContext();
        DeviceProfile profile = launcher.getDeviceProfile();
        Workspace workspace = (Workspace) v.getRootView().findViewById(R.id.workspace);
        ShortcutAndWidgetContainer hotseatParent = (ShortcutAndWidgetContainer) v.getParent();
        CellLayout hotseatLayout = (CellLayout) hotseatParent.getParent();
        ItemInfo itemInfo = (ItemInfo) v.getTag();
        int pageIndex = workspace.getNextPage();
        int pageCount = workspace.getChildCount();
        int iconIndex = hotseatParent.indexOfChild(v);
        int i = ((CellLayout.LayoutParams) hotseatLayout.getShortcutsAndWidgets().getChildAt(iconIndex).getLayoutParams()).cellX;
        CellLayout iconLayout = (CellLayout) workspace.getChildAt(pageIndex);
        if (iconLayout == null) {
            return consume;
        }
        ViewGroup iconParent = iconLayout.getShortcutsAndWidgets();
        ViewGroup parent = null;
        int[][] matrix = null;
        if (keyCode == 19 && !profile.isVerticalBarLayout()) {
            matrix = FocusLogic.createSparseMatrixWithHotseat(iconLayout, hotseatLayout, true, profile.inv.hotseatAllAppsRank);
            iconIndex += iconParent.getChildCount();
            parent = iconParent;
        } else if (keyCode == 21 && profile.isVerticalBarLayout()) {
            matrix = FocusLogic.createSparseMatrixWithHotseat(iconLayout, hotseatLayout, false, profile.inv.hotseatAllAppsRank);
            iconIndex += iconParent.getChildCount();
            parent = iconParent;
        } else if (keyCode == 22 && profile.isVerticalBarLayout()) {
            keyCode = 93;
        } else if (isUninstallKeyChord(e)) {
            matrix = FocusLogic.createSparseMatrix(iconLayout);
            if (UninstallDropTarget.supportsDrop(launcher, itemInfo)) {
                UninstallDropTarget.startUninstallActivity(launcher, itemInfo);
            }
        } else if (isDeleteKeyChord(e)) {
            matrix = FocusLogic.createSparseMatrix(iconLayout);
            launcher.removeItem(v, itemInfo, true);
        } else {
            matrix = FocusLogic.createSparseMatrix(hotseatLayout);
            parent = hotseatParent;
        }
        int newIconIndex = FocusLogic.handleKeyEvent(keyCode, matrix, iconIndex, pageIndex, pageCount, Utilities.isRtl(v.getResources()));
        View newIcon = null;
        switch (newIconIndex) {
            case -10:
            case -9:
                workspace.snapToPage(pageIndex + 1);
                CellLayout nextPage = (CellLayout) workspace.getPageAt(pageIndex + 1);
                if (checkIsNull(nextPage)) {
                    return consume;
                }
                boolean isNextPageFullscreen = ((CellLayout.LayoutParams) nextPage.getShortcutsAndWidgets().getChildAt(0).getLayoutParams()).isFullscreen;
                if (isNextPageFullscreen) {
                    workspace.getPageAt(pageIndex + 1).requestFocus();
                }
                break;
            case -8:
                parent = getCellLayoutChildrenForIndex(workspace, pageIndex + 1);
                newIcon = parent.getChildAt(0);
                workspace.snapToPage(pageIndex + 1);
                break;
            case -5:
            case -2:
                workspace.snapToPage(pageIndex - 1);
                CellLayout prevPage = (CellLayout) workspace.getPageAt(pageIndex - 1);
                if (checkIsNull(prevPage)) {
                    return consume;
                }
                boolean isPrevPageFullscreen = ((CellLayout.LayoutParams) prevPage.getShortcutsAndWidgets().getChildAt(0).getLayoutParams()).isFullscreen;
                if (isPrevPageFullscreen) {
                    workspace.getPageAt(pageIndex - 1).requestFocus();
                }
                break;
            case -4:
                parent = getCellLayoutChildrenForIndex(workspace, pageIndex - 1);
                newIcon = parent.getChildAt(parent.getChildCount() - 1);
                workspace.snapToPage(pageIndex - 1);
                break;
            case -3:
                parent = getCellLayoutChildrenForIndex(workspace, pageIndex - 1);
                newIcon = parent.getChildAt(0);
                workspace.snapToPage(pageIndex - 1);
                break;
        }
        if (parent == iconParent && newIconIndex >= iconParent.getChildCount()) {
            newIconIndex -= iconParent.getChildCount();
        }
        if (parent != null) {
            if (newIcon == null && newIconIndex >= 0) {
                newIcon = parent.getChildAt(newIconIndex);
            }
            if (newIcon != null) {
                newIcon.requestFocus();
                playSoundEffect(keyCode, v);
            }
        }
        return consume;
    }

    private static boolean checkIsNull(CellLayout page) {
        return page == null || page.getShortcutsAndWidgets() == null || page.getShortcutsAndWidgets().getChildAt(0) == null || page.getShortcutsAndWidgets().getChildAt(0).getLayoutParams() == null;
    }

    static boolean handleIconKeyEvent(View v, int keyCode, KeyEvent e) {
        int[][] matrix;
        boolean consume = FocusLogic.shouldConsume(keyCode);
        if (e.getAction() == 1 || !consume) {
            return consume;
        }
        Launcher launcher = (Launcher) v.getContext();
        DeviceProfile profile = launcher.getDeviceProfile();
        ShortcutAndWidgetContainer parent = (ShortcutAndWidgetContainer) v.getParent();
        CellLayout iconLayout = (CellLayout) parent.getParent();
        Workspace workspace = (Workspace) iconLayout.getParent();
        ViewGroup dragLayer = (ViewGroup) workspace.getParent();
        ViewGroup tabs = (ViewGroup) dragLayer.findViewById(R.id.search_drop_target_bar);
        Hotseat hotseat = (Hotseat) dragLayer.findViewById(R.id.hotseat);
        ItemInfo itemInfo = (ItemInfo) v.getTag();
        int iconIndex = parent.indexOfChild(v);
        int pageIndex = workspace.indexOfChild(iconLayout);
        int pageCount = workspace.getChildCount();
        CellLayout hotseatLayout = (CellLayout) hotseat.getChildAt(0);
        ShortcutAndWidgetContainer hotseatParent = hotseatLayout.getShortcutsAndWidgets();
        if (keyCode == 20 && !profile.isVerticalBarLayout()) {
            matrix = FocusLogic.createSparseMatrixWithHotseat(iconLayout, hotseatLayout, true, profile.inv.hotseatAllAppsRank);
        } else if (keyCode == 22 && profile.isVerticalBarLayout()) {
            matrix = FocusLogic.createSparseMatrixWithHotseat(iconLayout, hotseatLayout, false, profile.inv.hotseatAllAppsRank);
        } else if (isUninstallKeyChord(e)) {
            matrix = FocusLogic.createSparseMatrix(iconLayout);
            if (UninstallDropTarget.supportsDrop(launcher, itemInfo)) {
                UninstallDropTarget.startUninstallActivity(launcher, itemInfo);
            }
        } else if (isDeleteKeyChord(e)) {
            matrix = FocusLogic.createSparseMatrix(iconLayout);
            launcher.removeItem(v, itemInfo, true);
        } else {
            matrix = FocusLogic.createSparseMatrix(iconLayout);
        }
        int newIconIndex = FocusLogic.handleKeyEvent(keyCode, matrix, iconIndex, pageIndex, pageCount, Utilities.isRtl(v.getResources()));
        boolean isRtl = Utilities.isRtl(v.getResources());
        View newIcon = null;
        CellLayout workspaceLayout = (CellLayout) workspace.getChildAt(pageIndex);
        switch (newIconIndex) {
            case -10:
            case -2:
                int newPageIndex = pageIndex - 1;
                if (newIconIndex == -10) {
                    newPageIndex = pageIndex + 1;
                }
                int row = ((CellLayout.LayoutParams) v.getLayoutParams()).cellY;
                ShortcutAndWidgetContainer parent2 = getCellLayoutChildrenForIndex(workspace, newPageIndex);
                if (parent2 != null) {
                    CellLayout iconLayout2 = (CellLayout) parent2.getParent();
                    int[][] matrix2 = FocusLogic.createSparseMatrixWithPivotColumn(iconLayout2, iconLayout2.getCountX(), row);
                    int newIconIndex2 = FocusLogic.handleKeyEvent(keyCode, matrix2, 100, newPageIndex, pageCount, Utilities.isRtl(v.getResources()));
                    if (newIconIndex2 == -8) {
                        newIcon = handleNextPageFirstItem(workspace, hotseatLayout, pageIndex, isRtl);
                    } else if (newIconIndex2 == -4) {
                        newIcon = handlePreviousPageLastItem(workspace, hotseatLayout, pageIndex, isRtl);
                    } else {
                        newIcon = parent2.getChildAt(newIconIndex2);
                    }
                }
                break;
            case -9:
            case -5:
                int newPageIndex2 = pageIndex + 1;
                if (newIconIndex == -5) {
                    newPageIndex2 = pageIndex - 1;
                }
                int row2 = ((CellLayout.LayoutParams) v.getLayoutParams()).cellY;
                ShortcutAndWidgetContainer parent3 = getCellLayoutChildrenForIndex(workspace, newPageIndex2);
                if (parent3 != null) {
                    int[][] matrix3 = FocusLogic.createSparseMatrixWithPivotColumn((CellLayout) parent3.getParent(), -1, row2);
                    int newIconIndex3 = FocusLogic.handleKeyEvent(keyCode, matrix3, 100, newPageIndex2, pageCount, Utilities.isRtl(v.getResources()));
                    if (newIconIndex3 == -8) {
                        newIcon = handleNextPageFirstItem(workspace, hotseatLayout, pageIndex, isRtl);
                    } else if (newIconIndex3 == -4) {
                        newIcon = handlePreviousPageLastItem(workspace, hotseatLayout, pageIndex, isRtl);
                    } else {
                        newIcon = parent3.getChildAt(newIconIndex3);
                    }
                }
                break;
            case -8:
                newIcon = handleNextPageFirstItem(workspace, hotseatLayout, pageIndex, isRtl);
                break;
            case -7:
                newIcon = getFirstFocusableIconInReverseReadingOrder(workspaceLayout, isRtl);
                if (newIcon == null) {
                    newIcon = getFirstFocusableIconInReverseReadingOrder(hotseatLayout, isRtl);
                }
                break;
            case -6:
                newIcon = getFirstFocusableIconInReadingOrder(workspaceLayout, isRtl);
                if (newIcon == null) {
                    newIcon = getFirstFocusableIconInReadingOrder(hotseatLayout, isRtl);
                }
                break;
            case -4:
                newIcon = handlePreviousPageLastItem(workspace, hotseatLayout, pageIndex, isRtl);
                break;
            case -3:
                newIcon = getFirstFocusableIconInReadingOrder((CellLayout) workspace.getChildAt(pageIndex - 1), isRtl);
                if (newIcon == null) {
                    newIcon = getFirstFocusableIconInReadingOrder(hotseatLayout, isRtl);
                    workspace.snapToPage(pageIndex - 1);
                }
                break;
            case -1:
                if (keyCode == 19) {
                    newIcon = tabs;
                }
                break;
            default:
                if (newIconIndex >= 0 && newIconIndex < parent.getChildCount()) {
                    newIcon = parent.getChildAt(newIconIndex);
                } else if (parent.getChildCount() <= newIconIndex && newIconIndex < parent.getChildCount() + hotseatParent.getChildCount()) {
                    newIcon = hotseatParent.getChildAt(newIconIndex - parent.getChildCount());
                }
                break;
        }
        if (newIcon != null) {
            newIcon.requestFocus();
            playSoundEffect(keyCode, v);
        }
        return consume;
    }

    static ShortcutAndWidgetContainer getCellLayoutChildrenForIndex(ViewGroup container, int i) {
        CellLayout parent = (CellLayout) container.getChildAt(i);
        return parent.getShortcutsAndWidgets();
    }

    static void playSoundEffect(int keyCode, View v) {
        switch (keyCode) {
            case 19:
            case 92:
            case 122:
                v.playSoundEffect(2);
                break;
            case 20:
            case 93:
            case 123:
                v.playSoundEffect(4);
                break;
            case 21:
                v.playSoundEffect(1);
                break;
            case 22:
                v.playSoundEffect(3);
                break;
        }
    }

    private static boolean isUninstallKeyChord(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == 67 || keyCode == 112) {
            return event.hasModifiers(4097);
        }
        return false;
    }

    private static boolean isDeleteKeyChord(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == 67 || keyCode == 112) {
            return event.hasModifiers(4096);
        }
        return false;
    }

    private static View handlePreviousPageLastItem(Workspace workspace, CellLayout hotseatLayout, int pageIndex, boolean isRtl) {
        if (pageIndex - 1 < 0) {
            return null;
        }
        CellLayout workspaceLayout = (CellLayout) workspace.getChildAt(pageIndex - 1);
        View newIcon = getFirstFocusableIconInReverseReadingOrder(workspaceLayout, isRtl);
        if (newIcon == null) {
            View newIcon2 = getFirstFocusableIconInReverseReadingOrder(hotseatLayout, isRtl);
            workspace.snapToPage(pageIndex - 1);
            return newIcon2;
        }
        return newIcon;
    }

    private static View handleNextPageFirstItem(Workspace workspace, CellLayout hotseatLayout, int pageIndex, boolean isRtl) {
        if (pageIndex + 1 >= workspace.getPageCount()) {
            return null;
        }
        CellLayout workspaceLayout = (CellLayout) workspace.getChildAt(pageIndex + 1);
        View newIcon = getFirstFocusableIconInReadingOrder(workspaceLayout, isRtl);
        if (newIcon == null) {
            View newIcon2 = getFirstFocusableIconInReadingOrder(hotseatLayout, isRtl);
            workspace.snapToPage(pageIndex + 1);
            return newIcon2;
        }
        return newIcon;
    }

    private static View getFirstFocusableIconInReadingOrder(CellLayout cellLayout, boolean isRtl) {
        int countX = cellLayout.getCountX();
        for (int y = 0; y < cellLayout.getCountY(); y++) {
            int increment = isRtl ? -1 : 1;
            for (int x = isRtl ? countX - 1 : 0; x >= 0 && x < countX; x += increment) {
                View icon = cellLayout.getChildAt(x, y);
                if (icon != null && icon.isFocusable()) {
                    return icon;
                }
            }
        }
        return null;
    }

    private static View getFirstFocusableIconInReverseReadingOrder(CellLayout cellLayout, boolean isRtl) {
        int countX = cellLayout.getCountX();
        for (int y = cellLayout.getCountY() - 1; y >= 0; y--) {
            int increment = isRtl ? 1 : -1;
            for (int x = isRtl ? 0 : countX - 1; x >= 0 && x < countX; x += increment) {
                View icon = cellLayout.getChildAt(x, y);
                if (icon != null && icon.isFocusable()) {
                    return icon;
                }
            }
        }
        return null;
    }
}
