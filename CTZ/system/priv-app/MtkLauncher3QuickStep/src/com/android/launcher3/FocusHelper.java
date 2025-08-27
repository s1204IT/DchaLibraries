package com.android.launcher3;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.CellLayout;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderPagedView;
import com.android.launcher3.util.FocusLogic;

/* loaded from: classes.dex */
public class FocusHelper {
    private static final boolean DEBUG = false;
    private static final String TAG = "FocusHelper";

    public static class PagedFolderKeyEventListener implements View.OnKeyListener {
        private final Folder mFolder;

        public PagedFolderKeyEventListener(Folder folder) {
            this.mFolder = folder;
        }

        @Override // android.view.View.OnKeyListener
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            boolean zShouldConsume = FocusLogic.shouldConsume(i);
            if (keyEvent.getAction() == 1) {
                return zShouldConsume;
            }
            if (!(view.getParent() instanceof ShortcutAndWidgetContainer)) {
                return false;
            }
            ShortcutAndWidgetContainer shortcutAndWidgetContainer = (ShortcutAndWidgetContainer) view.getParent();
            CellLayout cellLayout = (CellLayout) shortcutAndWidgetContainer.getParent();
            int iIndexOfChild = shortcutAndWidgetContainer.indexOfChild(view);
            FolderPagedView folderPagedView = (FolderPagedView) cellLayout.getParent();
            int iIndexOfChild2 = folderPagedView.indexOfChild(cellLayout);
            int pageCount = folderPagedView.getPageCount();
            boolean zIsRtl = Utilities.isRtl(view.getResources());
            int[][] iArrCreateSparseMatrix = FocusLogic.createSparseMatrix(cellLayout);
            int iHandleKeyEvent = FocusLogic.handleKeyEvent(i, iArrCreateSparseMatrix, iIndexOfChild, iIndexOfChild2, pageCount, zIsRtl);
            if (iHandleKeyEvent == -1) {
                handleNoopKey(i, view);
                return zShouldConsume;
            }
            View adjacentChildInNextFolderPage = null;
            switch (iHandleKeyEvent) {
                case FocusLogic.NEXT_PAGE_RIGHT_COLUMN /* -10 */:
                case FocusLogic.NEXT_PAGE_LEFT_COLUMN /* -9 */:
                    int i2 = iIndexOfChild2 + 1;
                    ShortcutAndWidgetContainer cellLayoutChildrenForIndex = FocusHelper.getCellLayoutChildrenForIndex(folderPagedView, i2);
                    if (cellLayoutChildrenForIndex != null) {
                        folderPagedView.snapToPage(i2);
                        adjacentChildInNextFolderPage = FocusLogic.getAdjacentChildInNextFolderPage(cellLayoutChildrenForIndex, view, iHandleKeyEvent);
                        break;
                    }
                    break;
                case FocusLogic.NEXT_PAGE_FIRST_ITEM /* -8 */:
                    int i3 = iIndexOfChild2 + 1;
                    ShortcutAndWidgetContainer cellLayoutChildrenForIndex2 = FocusHelper.getCellLayoutChildrenForIndex(folderPagedView, i3);
                    if (cellLayoutChildrenForIndex2 != null) {
                        folderPagedView.snapToPage(i3);
                        adjacentChildInNextFolderPage = cellLayoutChildrenForIndex2.getChildAt(0, 0);
                        break;
                    }
                    break;
                case FocusLogic.CURRENT_PAGE_LAST_ITEM /* -7 */:
                    adjacentChildInNextFolderPage = folderPagedView.getLastItem();
                    break;
                case FocusLogic.CURRENT_PAGE_FIRST_ITEM /* -6 */:
                    adjacentChildInNextFolderPage = cellLayout.getChildAt(0, 0);
                    break;
                case FocusLogic.PREVIOUS_PAGE_LEFT_COLUMN /* -5 */:
                case -2:
                    int i4 = iIndexOfChild2 - 1;
                    ShortcutAndWidgetContainer cellLayoutChildrenForIndex3 = FocusHelper.getCellLayoutChildrenForIndex(folderPagedView, i4);
                    if (cellLayoutChildrenForIndex3 != null) {
                        int i5 = ((CellLayout.LayoutParams) view.getLayoutParams()).cellY;
                        folderPagedView.snapToPage(i4);
                        adjacentChildInNextFolderPage = cellLayoutChildrenForIndex3.getChildAt((iHandleKeyEvent == -5) ^ cellLayoutChildrenForIndex3.invertLayoutHorizontally() ? 0 : iArrCreateSparseMatrix.length - 1, i5);
                        break;
                    }
                    break;
                case -4:
                    int i6 = iIndexOfChild2 - 1;
                    ShortcutAndWidgetContainer cellLayoutChildrenForIndex4 = FocusHelper.getCellLayoutChildrenForIndex(folderPagedView, i6);
                    if (cellLayoutChildrenForIndex4 != null) {
                        folderPagedView.snapToPage(i6);
                        adjacentChildInNextFolderPage = cellLayoutChildrenForIndex4.getChildAt(iArrCreateSparseMatrix.length - 1, iArrCreateSparseMatrix[0].length - 1);
                        break;
                    }
                    break;
                case -3:
                    int i7 = iIndexOfChild2 - 1;
                    ShortcutAndWidgetContainer cellLayoutChildrenForIndex5 = FocusHelper.getCellLayoutChildrenForIndex(folderPagedView, i7);
                    if (cellLayoutChildrenForIndex5 != null) {
                        folderPagedView.snapToPage(i7);
                        adjacentChildInNextFolderPage = cellLayoutChildrenForIndex5.getChildAt(0, 0);
                        break;
                    }
                    break;
                default:
                    adjacentChildInNextFolderPage = shortcutAndWidgetContainer.getChildAt(iHandleKeyEvent);
                    break;
            }
            if (adjacentChildInNextFolderPage != null) {
                adjacentChildInNextFolderPage.requestFocus();
                FocusHelper.playSoundEffect(i, view);
            } else {
                handleNoopKey(i, view);
            }
            return zShouldConsume;
        }

        public void handleNoopKey(int i, View view) {
            if (i == 20) {
                this.mFolder.mFolderName.requestFocus();
                FocusHelper.playSoundEffect(i, view);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [264=4] */
    /* JADX WARN: Removed duplicated region for block: B:29:0x00c3  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x00d0  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x00e2  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x00e7  */
    /* JADX WARN: Removed duplicated region for block: B:33:0x00f4  */
    /* JADX WARN: Removed duplicated region for block: B:39:0x0107  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static boolean handleHotseatButtonKeyEvent(View view, int i, KeyEvent keyEvent) {
        int[][] iArrCreateSparseMatrix;
        int i2;
        int[][] iArrCreateSparseMatrixWithHotseat;
        int childCount;
        int iHandleKeyEvent;
        int i3 = i;
        boolean zShouldConsume = FocusLogic.shouldConsume(i);
        if (keyEvent.getAction() == 1 || !zShouldConsume) {
            return zShouldConsume;
        }
        DeviceProfile deviceProfile = Launcher.getLauncher(view.getContext()).getDeviceProfile();
        Workspace workspace = (Workspace) view.getRootView().findViewById(R.id.workspace);
        ShortcutAndWidgetContainer cellLayoutChildrenForIndex = (ShortcutAndWidgetContainer) view.getParent();
        CellLayout cellLayout = (CellLayout) cellLayoutChildrenForIndex.getParent();
        int nextPage = workspace.getNextPage();
        int childCount2 = workspace.getChildCount();
        int iIndexOfChild = cellLayoutChildrenForIndex.indexOfChild(view);
        int i4 = ((CellLayout.LayoutParams) cellLayout.getShortcutsAndWidgets().getChildAt(iIndexOfChild).getLayoutParams()).cellX;
        CellLayout cellLayout2 = (CellLayout) workspace.getChildAt(nextPage);
        if (cellLayout2 == null) {
            return zShouldConsume;
        }
        ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout2.getShortcutsAndWidgets();
        View childAt = null;
        if (i3 == 19 && !deviceProfile.isVerticalBarLayout()) {
            iArrCreateSparseMatrixWithHotseat = FocusLogic.createSparseMatrixWithHotseat(cellLayout2, cellLayout, deviceProfile);
            childCount = iIndexOfChild + shortcutsAndWidgets.getChildCount();
        } else {
            if (i3 != 21 || !deviceProfile.isVerticalBarLayout()) {
                if (i3 == 22 && deviceProfile.isVerticalBarLayout()) {
                    i3 = 93;
                    i2 = iIndexOfChild;
                    cellLayoutChildrenForIndex = null;
                    iArrCreateSparseMatrix = null;
                } else {
                    iArrCreateSparseMatrix = FocusLogic.createSparseMatrix(cellLayout);
                    i2 = iIndexOfChild;
                }
                iHandleKeyEvent = FocusLogic.handleKeyEvent(i3, iArrCreateSparseMatrix, i2, nextPage, childCount2, Utilities.isRtl(view.getResources()));
                switch (iHandleKeyEvent) {
                    case FocusLogic.NEXT_PAGE_RIGHT_COLUMN /* -10 */:
                    case FocusLogic.NEXT_PAGE_LEFT_COLUMN /* -9 */:
                        workspace.snapToPage(nextPage + 1);
                        break;
                    case FocusLogic.NEXT_PAGE_FIRST_ITEM /* -8 */:
                        int i5 = nextPage + 1;
                        cellLayoutChildrenForIndex = getCellLayoutChildrenForIndex(workspace, i5);
                        childAt = cellLayoutChildrenForIndex.getChildAt(0);
                        workspace.snapToPage(i5);
                        break;
                    case FocusLogic.PREVIOUS_PAGE_LEFT_COLUMN /* -5 */:
                    case -2:
                        workspace.snapToPage(nextPage - 1);
                        break;
                    case -4:
                        int i6 = nextPage - 1;
                        cellLayoutChildrenForIndex = getCellLayoutChildrenForIndex(workspace, i6);
                        childAt = cellLayoutChildrenForIndex.getChildAt(cellLayoutChildrenForIndex.getChildCount() - 1);
                        workspace.snapToPage(i6);
                        break;
                    case -3:
                        int i7 = nextPage - 1;
                        cellLayoutChildrenForIndex = getCellLayoutChildrenForIndex(workspace, i7);
                        childAt = cellLayoutChildrenForIndex.getChildAt(0);
                        workspace.snapToPage(i7);
                        break;
                }
                if (cellLayoutChildrenForIndex == shortcutsAndWidgets && iHandleKeyEvent >= shortcutsAndWidgets.getChildCount()) {
                    iHandleKeyEvent -= shortcutsAndWidgets.getChildCount();
                }
                if (cellLayoutChildrenForIndex != null) {
                    if (childAt == null && iHandleKeyEvent >= 0) {
                        childAt = cellLayoutChildrenForIndex.getChildAt(iHandleKeyEvent);
                    }
                    View view2 = childAt;
                    if (view2 != null) {
                        view2.requestFocus();
                        playSoundEffect(i3, view);
                    }
                }
                return zShouldConsume;
            }
            iArrCreateSparseMatrixWithHotseat = FocusLogic.createSparseMatrixWithHotseat(cellLayout2, cellLayout, deviceProfile);
            childCount = iIndexOfChild + shortcutsAndWidgets.getChildCount();
        }
        iArrCreateSparseMatrix = iArrCreateSparseMatrixWithHotseat;
        i2 = childCount;
        cellLayoutChildrenForIndex = shortcutsAndWidgets;
        iHandleKeyEvent = FocusLogic.handleKeyEvent(i3, iArrCreateSparseMatrix, i2, nextPage, childCount2, Utilities.isRtl(view.getResources()));
        switch (iHandleKeyEvent) {
        }
        if (cellLayoutChildrenForIndex == shortcutsAndWidgets) {
            iHandleKeyEvent -= shortcutsAndWidgets.getChildCount();
        }
        if (cellLayoutChildrenForIndex != null) {
        }
        return zShouldConsume;
    }

    /* JADX WARN: Removed duplicated region for block: B:67:0x01c1  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    static boolean handleIconKeyEvent(View view, int i, KeyEvent keyEvent) {
        int[][] iArrCreateSparseMatrix;
        boolean zShouldConsume = FocusLogic.shouldConsume(i);
        if (keyEvent.getAction() == 1 || !zShouldConsume) {
            return zShouldConsume;
        }
        DeviceProfile deviceProfile = Launcher.getLauncher(view.getContext()).getDeviceProfile();
        ShortcutAndWidgetContainer shortcutAndWidgetContainer = (ShortcutAndWidgetContainer) view.getParent();
        CellLayout cellLayout = (CellLayout) shortcutAndWidgetContainer.getParent();
        Workspace workspace = (Workspace) cellLayout.getParent();
        ViewGroup viewGroup = (ViewGroup) workspace.getParent();
        View childAt = (ViewGroup) viewGroup.findViewById(R.id.drop_target_bar);
        Hotseat hotseat = (Hotseat) viewGroup.findViewById(R.id.hotseat);
        int iIndexOfChild = shortcutAndWidgetContainer.indexOfChild(view);
        int iIndexOfChild2 = workspace.indexOfChild(cellLayout);
        int childCount = workspace.getChildCount();
        CellLayout cellLayout2 = (CellLayout) hotseat.getChildAt(0);
        ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout2.getShortcutsAndWidgets();
        if (i == 20 && !deviceProfile.isVerticalBarLayout()) {
            iArrCreateSparseMatrix = FocusLogic.createSparseMatrixWithHotseat(cellLayout, cellLayout2, deviceProfile);
        } else if (i == 22 && deviceProfile.isVerticalBarLayout()) {
            iArrCreateSparseMatrix = FocusLogic.createSparseMatrixWithHotseat(cellLayout, cellLayout2, deviceProfile);
        } else {
            iArrCreateSparseMatrix = FocusLogic.createSparseMatrix(cellLayout);
        }
        int iHandleKeyEvent = FocusLogic.handleKeyEvent(i, iArrCreateSparseMatrix, iIndexOfChild, iIndexOfChild2, childCount, Utilities.isRtl(view.getResources()));
        boolean zIsRtl = Utilities.isRtl(view.getResources());
        CellLayout cellLayout3 = (CellLayout) workspace.getChildAt(iIndexOfChild2);
        switch (iHandleKeyEvent) {
            case FocusLogic.NEXT_PAGE_RIGHT_COLUMN /* -10 */:
            case -2:
                int i2 = iHandleKeyEvent == -10 ? iIndexOfChild2 + 1 : iIndexOfChild2 - 1;
                int i3 = ((CellLayout.LayoutParams) view.getLayoutParams()).cellY;
                ShortcutAndWidgetContainer cellLayoutChildrenForIndex = getCellLayoutChildrenForIndex(workspace, i2);
                if (cellLayoutChildrenForIndex != null) {
                    CellLayout cellLayout4 = (CellLayout) cellLayoutChildrenForIndex.getParent();
                    int iHandleKeyEvent2 = FocusLogic.handleKeyEvent(i, FocusLogic.createSparseMatrixWithPivotColumn(cellLayout4, cellLayout4.getCountX(), i3), 100, i2, childCount, Utilities.isRtl(view.getResources()));
                    if (iHandleKeyEvent2 == -8) {
                        childAt = handleNextPageFirstItem(workspace, cellLayout2, iIndexOfChild2, zIsRtl);
                        break;
                    } else if (iHandleKeyEvent2 == -4) {
                        childAt = handlePreviousPageLastItem(workspace, cellLayout2, iIndexOfChild2, zIsRtl);
                        break;
                    } else {
                        childAt = cellLayoutChildrenForIndex.getChildAt(iHandleKeyEvent2);
                        break;
                    }
                } else {
                    childAt = null;
                    break;
                }
            case FocusLogic.NEXT_PAGE_LEFT_COLUMN /* -9 */:
            case FocusLogic.PREVIOUS_PAGE_LEFT_COLUMN /* -5 */:
                int i4 = iHandleKeyEvent == -5 ? iIndexOfChild2 - 1 : iIndexOfChild2 + 1;
                int i5 = ((CellLayout.LayoutParams) view.getLayoutParams()).cellY;
                ShortcutAndWidgetContainer cellLayoutChildrenForIndex2 = getCellLayoutChildrenForIndex(workspace, i4);
                if (cellLayoutChildrenForIndex2 != null) {
                    int iHandleKeyEvent3 = FocusLogic.handleKeyEvent(i, FocusLogic.createSparseMatrixWithPivotColumn((CellLayout) cellLayoutChildrenForIndex2.getParent(), -1, i5), 100, i4, childCount, Utilities.isRtl(view.getResources()));
                    if (iHandleKeyEvent3 == -8) {
                        childAt = handleNextPageFirstItem(workspace, cellLayout2, iIndexOfChild2, zIsRtl);
                        break;
                    } else if (iHandleKeyEvent3 == -4) {
                        childAt = handlePreviousPageLastItem(workspace, cellLayout2, iIndexOfChild2, zIsRtl);
                        break;
                    } else {
                        childAt = cellLayoutChildrenForIndex2.getChildAt(iHandleKeyEvent3);
                        break;
                    }
                }
                break;
            case FocusLogic.NEXT_PAGE_FIRST_ITEM /* -8 */:
                childAt = handleNextPageFirstItem(workspace, cellLayout2, iIndexOfChild2, zIsRtl);
                break;
            case FocusLogic.CURRENT_PAGE_LAST_ITEM /* -7 */:
                childAt = getFirstFocusableIconInReverseReadingOrder(cellLayout3, zIsRtl);
                if (childAt == null) {
                    childAt = getFirstFocusableIconInReverseReadingOrder(cellLayout2, zIsRtl);
                    break;
                }
                break;
            case FocusLogic.CURRENT_PAGE_FIRST_ITEM /* -6 */:
                childAt = getFirstFocusableIconInReadingOrder(cellLayout3, zIsRtl);
                if (childAt == null) {
                    childAt = getFirstFocusableIconInReadingOrder(cellLayout2, zIsRtl);
                    break;
                }
                break;
            case -4:
                childAt = handlePreviousPageLastItem(workspace, cellLayout2, iIndexOfChild2, zIsRtl);
                break;
            case -3:
                int i6 = iIndexOfChild2 - 1;
                childAt = getFirstFocusableIconInReadingOrder((CellLayout) workspace.getChildAt(i6), zIsRtl);
                if (childAt == null) {
                    childAt = getFirstFocusableIconInReadingOrder(cellLayout2, zIsRtl);
                    workspace.snapToPage(i6);
                    break;
                }
                break;
            case -1:
                if (i != 19) {
                }
                break;
            default:
                if (iHandleKeyEvent >= 0 && iHandleKeyEvent < shortcutAndWidgetContainer.getChildCount()) {
                    childAt = shortcutAndWidgetContainer.getChildAt(iHandleKeyEvent);
                    break;
                } else if (shortcutAndWidgetContainer.getChildCount() <= iHandleKeyEvent && iHandleKeyEvent < shortcutAndWidgetContainer.getChildCount() + shortcutsAndWidgets.getChildCount()) {
                    childAt = shortcutsAndWidgets.getChildAt(iHandleKeyEvent - shortcutAndWidgetContainer.getChildCount());
                    break;
                }
                break;
        }
        if (childAt != null) {
            childAt.requestFocus();
            playSoundEffect(i, view);
        }
        return zShouldConsume;
    }

    static ShortcutAndWidgetContainer getCellLayoutChildrenForIndex(ViewGroup viewGroup, int i) {
        return ((CellLayout) viewGroup.getChildAt(i)).getShortcutsAndWidgets();
    }

    static void playSoundEffect(int i, View view) {
        switch (i) {
            case 19:
            case 92:
            case 122:
                view.playSoundEffect(2);
                break;
            case 20:
            case 93:
            case 123:
                view.playSoundEffect(4);
                break;
            case 21:
                view.playSoundEffect(1);
                break;
            case 22:
                view.playSoundEffect(3);
                break;
        }
    }

    private static View handlePreviousPageLastItem(Workspace workspace, CellLayout cellLayout, int i, boolean z) {
        int i2 = i - 1;
        if (i2 < 0) {
            return null;
        }
        View firstFocusableIconInReverseReadingOrder = getFirstFocusableIconInReverseReadingOrder((CellLayout) workspace.getChildAt(i2), z);
        if (firstFocusableIconInReverseReadingOrder == null) {
            View firstFocusableIconInReverseReadingOrder2 = getFirstFocusableIconInReverseReadingOrder(cellLayout, z);
            workspace.snapToPage(i2);
            return firstFocusableIconInReverseReadingOrder2;
        }
        return firstFocusableIconInReverseReadingOrder;
    }

    private static View handleNextPageFirstItem(Workspace workspace, CellLayout cellLayout, int i, boolean z) {
        int i2 = i + 1;
        if (i2 >= workspace.getPageCount()) {
            return null;
        }
        View firstFocusableIconInReadingOrder = getFirstFocusableIconInReadingOrder((CellLayout) workspace.getChildAt(i2), z);
        if (firstFocusableIconInReadingOrder == null) {
            View firstFocusableIconInReadingOrder2 = getFirstFocusableIconInReadingOrder(cellLayout, z);
            workspace.snapToPage(i2);
            return firstFocusableIconInReadingOrder2;
        }
        return firstFocusableIconInReadingOrder;
    }

    private static View getFirstFocusableIconInReadingOrder(CellLayout cellLayout, boolean z) {
        int countX = cellLayout.getCountX();
        for (int i = 0; i < cellLayout.getCountY(); i++) {
            int i2 = z ? -1 : 1;
            for (int i3 = z ? countX - 1 : 0; i3 >= 0 && i3 < countX; i3 += i2) {
                View childAt = cellLayout.getChildAt(i3, i);
                if (childAt != null && childAt.isFocusable()) {
                    return childAt;
                }
            }
        }
        return null;
    }

    private static View getFirstFocusableIconInReverseReadingOrder(CellLayout cellLayout, boolean z) {
        int countX = cellLayout.getCountX();
        for (int countY = cellLayout.getCountY() - 1; countY >= 0; countY--) {
            int i = z ? 1 : -1;
            for (int i2 = z ? 0 : countX - 1; i2 >= 0 && i2 < countX; i2 += i) {
                View childAt = cellLayout.getChildAt(i2, countY);
                if (childAt != null && childAt.isFocusable()) {
                    return childAt;
                }
            }
        }
        return null;
    }
}
