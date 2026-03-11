package com.android.launcher2;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TabHost;
import android.widget.TabWidget;
import com.android.launcher.R;
import com.android.launcher2.CellLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FocusHelper {
    private static TabHost findTabHostParent(View v) {
        ViewParent p = v.getParent();
        while (p != null && !(p instanceof TabHost)) {
            p = p.getParent();
        }
        return (TabHost) p;
    }

    static boolean handleAppsCustomizeTabKeyEvent(View v, int keyCode, KeyEvent e) {
        TabHost tabHost = findTabHostParent(v);
        ViewGroup contents = tabHost.getTabContentView();
        View shop = tabHost.findViewById(R.id.market_button);
        int action = e.getAction();
        boolean handleKeyEvent = action != 1;
        switch (keyCode) {
            case 20:
                if (handleKeyEvent && v == shop) {
                    contents.requestFocus();
                    break;
                }
                break;
            case 22:
                if (handleKeyEvent && v != shop) {
                    shop.requestFocus();
                }
                break;
        }
        return false;
    }

    private static ViewGroup getAppsCustomizePage(ViewGroup container, int index) {
        ViewGroup page = (ViewGroup) ((PagedView) container).getPageAt(index);
        if (page instanceof PagedViewCellLayout) {
            return (ViewGroup) page.getChildAt(0);
        }
        return page;
    }

    static boolean handleAppsCustomizeKeyEvent(View v, int keyCode, KeyEvent e) {
        ViewGroup parentLayout;
        ViewGroup itemContainer;
        int countX;
        int countY;
        ViewGroup newParent;
        ViewGroup newParent2;
        if (v.getParent() instanceof PagedViewCellLayoutChildren) {
            itemContainer = (ViewGroup) v.getParent();
            parentLayout = (ViewGroup) itemContainer.getParent();
            countX = ((PagedViewCellLayout) parentLayout).getCellCountX();
            countY = ((PagedViewCellLayout) parentLayout).getCellCountY();
        } else {
            parentLayout = (ViewGroup) v.getParent();
            itemContainer = parentLayout;
            countX = ((PagedViewGridLayout) parentLayout).getCellCountX();
            countY = ((PagedViewGridLayout) parentLayout).getCellCountY();
        }
        PagedView pagedView = (PagedView) parentLayout.getParent();
        TabHost tabHost = findTabHostParent(pagedView);
        TabWidget tabs = tabHost.getTabWidget();
        int iconIndex = itemContainer.indexOfChild(v);
        int itemCount = itemContainer.getChildCount();
        int pageIndex = pagedView.indexToPage(pagedView.indexOfChild(parentLayout));
        int pageCount = pagedView.getChildCount();
        int x = iconIndex % countX;
        int y = iconIndex / countX;
        int action = e.getAction();
        boolean handleKeyEvent = action != 1;
        switch (keyCode) {
            case 19:
                if (handleKeyEvent) {
                    if (y > 0) {
                        int newiconIndex = ((y - 1) * countX) + x;
                        itemContainer.getChildAt(newiconIndex).requestFocus();
                    } else {
                        tabs.requestFocus();
                    }
                }
                return true;
            case 20:
                if (handleKeyEvent && y < countY - 1) {
                    int newiconIndex2 = Math.min(itemCount - 1, ((y + 1) * countX) + x);
                    itemContainer.getChildAt(newiconIndex2).requestFocus();
                }
                return true;
            case 21:
                if (handleKeyEvent) {
                    if (iconIndex > 0) {
                        itemContainer.getChildAt(iconIndex - 1).requestFocus();
                    } else if (pageIndex > 0 && (newParent2 = getAppsCustomizePage(pagedView, pageIndex - 1)) != null) {
                        pagedView.snapToPage(pageIndex - 1);
                        View child = newParent2.getChildAt(newParent2.getChildCount() - 1);
                        if (child != null) {
                            child.requestFocus();
                        }
                    }
                }
                return true;
            case 22:
                if (handleKeyEvent) {
                    if (iconIndex < itemCount - 1) {
                        itemContainer.getChildAt(iconIndex + 1).requestFocus();
                    } else if (pageIndex < pageCount - 1 && (newParent = getAppsCustomizePage(pagedView, pageIndex + 1)) != null) {
                        pagedView.snapToPage(pageIndex + 1);
                        View child2 = newParent.getChildAt(0);
                        if (child2 != null) {
                            child2.requestFocus();
                        }
                    }
                }
                return true;
            case 23:
            case 66:
                if (handleKeyEvent) {
                    View.OnClickListener clickListener = (View.OnClickListener) pagedView;
                    clickListener.onClick(v);
                }
                return true;
            case 92:
                if (handleKeyEvent) {
                    if (pageIndex > 0) {
                        ViewGroup newParent3 = getAppsCustomizePage(pagedView, pageIndex - 1);
                        if (newParent3 != null) {
                            pagedView.snapToPage(pageIndex - 1);
                            View child3 = newParent3.getChildAt(0);
                            if (child3 != null) {
                                child3.requestFocus();
                            }
                        }
                    } else {
                        itemContainer.getChildAt(0).requestFocus();
                    }
                }
                return true;
            case 93:
                if (handleKeyEvent) {
                    if (pageIndex < pageCount - 1) {
                        ViewGroup newParent4 = getAppsCustomizePage(pagedView, pageIndex + 1);
                        if (newParent4 != null) {
                            pagedView.snapToPage(pageIndex + 1);
                            View child4 = newParent4.getChildAt(0);
                            if (child4 != null) {
                                child4.requestFocus();
                            }
                        }
                    } else {
                        itemContainer.getChildAt(itemCount - 1).requestFocus();
                    }
                }
                return true;
            case 122:
                if (handleKeyEvent) {
                    itemContainer.getChildAt(0).requestFocus();
                }
                return true;
            case 123:
                if (handleKeyEvent) {
                    itemContainer.getChildAt(itemCount - 1).requestFocus();
                }
                return true;
            default:
                return false;
        }
    }

    static boolean handleTabKeyEvent(AccessibleTabView v, int keyCode, KeyEvent e) {
        if (!LauncherApplication.isScreenLarge()) {
            return false;
        }
        FocusOnlyTabWidget parent = (FocusOnlyTabWidget) v.getParent();
        TabHost tabHost = findTabHostParent(parent);
        ViewGroup contents = tabHost.getTabContentView();
        int tabCount = parent.getTabCount();
        int tabIndex = parent.getChildTabIndex(v);
        int action = e.getAction();
        boolean handleKeyEvent = action != 1;
        switch (keyCode) {
            case 20:
                if (handleKeyEvent) {
                    contents.requestFocus();
                }
                break;
            case 21:
                if (handleKeyEvent && tabIndex > 0) {
                    parent.getChildTabViewAt(tabIndex - 1).requestFocus();
                }
                break;
            case 22:
                if (handleKeyEvent) {
                    if (tabIndex < tabCount - 1) {
                        parent.getChildTabViewAt(tabIndex + 1).requestFocus();
                    } else if (v.getNextFocusRightId() != -1) {
                        tabHost.findViewById(v.getNextFocusRightId()).requestFocus();
                    }
                }
                break;
        }
        return false;
    }

    static boolean handleHotseatButtonKeyEvent(View v, int keyCode, KeyEvent e, int orientation) {
        ViewGroup parent = (ViewGroup) v.getParent();
        ViewGroup launcher = (ViewGroup) parent.getParent();
        Workspace workspace = (Workspace) launcher.findViewById(R.id.workspace);
        int buttonIndex = parent.indexOfChild(v);
        int buttonCount = parent.getChildCount();
        int pageIndex = workspace.getCurrentPage();
        int action = e.getAction();
        boolean handleKeyEvent = action != 1;
        switch (keyCode) {
            case 19:
                if (handleKeyEvent) {
                    CellLayout layout = (CellLayout) workspace.getChildAt(pageIndex);
                    ShortcutAndWidgetContainer children = layout.getShortcutsAndWidgets();
                    View newIcon = getIconInDirection(layout, children, -1, 1);
                    if (newIcon != null) {
                        newIcon.requestFocus();
                    } else {
                        workspace.requestFocus();
                    }
                }
                return true;
            case 20:
                return true;
            case 21:
                if (handleKeyEvent) {
                    if (buttonIndex > 0) {
                        parent.getChildAt(buttonIndex - 1).requestFocus();
                    } else {
                        workspace.snapToPage(pageIndex - 1);
                    }
                }
                return true;
            case 22:
                if (handleKeyEvent) {
                    if (buttonIndex < buttonCount - 1) {
                        parent.getChildAt(buttonIndex + 1).requestFocus();
                    } else {
                        workspace.snapToPage(pageIndex + 1);
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private static ShortcutAndWidgetContainer getCellLayoutChildrenForIndex(ViewGroup container, int i) {
        ViewGroup parent = (ViewGroup) container.getChildAt(i);
        return (ShortcutAndWidgetContainer) parent.getChildAt(0);
    }

    private static ArrayList<View> getCellLayoutChildrenSortedSpatially(CellLayout layout, ViewGroup parent) {
        final int cellCountX = layout.getCountX();
        int count = parent.getChildCount();
        ArrayList<View> views = new ArrayList<>();
        for (int j = 0; j < count; j++) {
            views.add(parent.getChildAt(j));
        }
        Collections.sort(views, new Comparator<View>() {
            @Override
            public int compare(View lhs, View rhs) {
                CellLayout.LayoutParams llp = (CellLayout.LayoutParams) lhs.getLayoutParams();
                CellLayout.LayoutParams rlp = (CellLayout.LayoutParams) rhs.getLayoutParams();
                int lvIndex = (llp.cellY * cellCountX) + llp.cellX;
                int rvIndex = (rlp.cellY * cellCountX) + rlp.cellX;
                return lvIndex - rvIndex;
            }
        });
        return views;
    }

    private static View findIndexOfIcon(ArrayList<View> views, int i, int delta) {
        int count = views.size();
        int newI = i + delta;
        while (newI >= 0 && newI < count) {
            View newV = views.get(newI);
            if (!(newV instanceof BubbleTextView) && !(newV instanceof FolderIcon)) {
                newI += delta;
            } else {
                return newV;
            }
        }
        return null;
    }

    private static View getIconInDirection(CellLayout layout, ViewGroup parent, int i, int delta) {
        ArrayList<View> views = getCellLayoutChildrenSortedSpatially(layout, parent);
        return findIndexOfIcon(views, i, delta);
    }

    private static View getIconInDirection(CellLayout layout, ViewGroup parent, View v, int delta) {
        ArrayList<View> views = getCellLayoutChildrenSortedSpatially(layout, parent);
        return findIndexOfIcon(views, views.indexOf(v), delta);
    }

    private static View getClosestIconOnLine(CellLayout layout, ViewGroup parent, View v, int lineDelta) {
        boolean satisfiesRow;
        ArrayList<View> views = getCellLayoutChildrenSortedSpatially(layout, parent);
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
        int cellCountY = layout.getCountY();
        int row = lp.cellY;
        int newRow = row + lineDelta;
        if (newRow >= 0 && newRow < cellCountY) {
            float closestDistance = Float.MAX_VALUE;
            int closestIndex = -1;
            int index = views.indexOf(v);
            int endIndex = lineDelta < 0 ? -1 : views.size();
            while (index != endIndex) {
                View newV = views.get(index);
                CellLayout.LayoutParams tmpLp = (CellLayout.LayoutParams) newV.getLayoutParams();
                if (lineDelta < 0) {
                    satisfiesRow = tmpLp.cellY < row;
                } else {
                    satisfiesRow = tmpLp.cellY > row;
                }
                if (satisfiesRow && ((newV instanceof BubbleTextView) || (newV instanceof FolderIcon))) {
                    float tmpDistance = (float) Math.sqrt(Math.pow(tmpLp.cellX - lp.cellX, 2.0d) + Math.pow(tmpLp.cellY - lp.cellY, 2.0d));
                    if (tmpDistance < closestDistance) {
                        closestIndex = index;
                        closestDistance = tmpDistance;
                    }
                }
                if (index <= endIndex) {
                    index++;
                } else {
                    index--;
                }
            }
            if (closestIndex > -1) {
                return views.get(closestIndex);
            }
        }
        return null;
    }

    static boolean handleIconKeyEvent(View v, int keyCode, KeyEvent e) {
        View newIcon;
        View newIcon2;
        ShortcutAndWidgetContainer parent = (ShortcutAndWidgetContainer) v.getParent();
        CellLayout layout = (CellLayout) parent.getParent();
        Workspace workspace = (Workspace) layout.getParent();
        ViewGroup launcher = (ViewGroup) workspace.getParent();
        ViewGroup tabs = (ViewGroup) launcher.findViewById(R.id.qsb_bar);
        ViewGroup hotseat = (ViewGroup) launcher.findViewById(R.id.hotseat);
        int pageIndex = workspace.indexOfChild(layout);
        int pageCount = workspace.getChildCount();
        int action = e.getAction();
        boolean handleKeyEvent = action != 1;
        switch (keyCode) {
            case 19:
                if (handleKeyEvent) {
                    View newIcon3 = getClosestIconOnLine(layout, parent, v, -1);
                    if (newIcon3 != null) {
                        newIcon3.requestFocus();
                    } else {
                        tabs.requestFocus();
                    }
                }
                break;
            case 20:
                if (handleKeyEvent) {
                    View newIcon4 = getClosestIconOnLine(layout, parent, v, 1);
                    if (newIcon4 != null) {
                        newIcon4.requestFocus();
                    } else if (hotseat != null) {
                        hotseat.requestFocus();
                    }
                }
                break;
            case 21:
                if (handleKeyEvent) {
                    View newIcon5 = getIconInDirection(layout, parent, v, -1);
                    if (newIcon5 != null) {
                        newIcon5.requestFocus();
                    } else if (pageIndex > 0) {
                        ShortcutAndWidgetContainer parent2 = getCellLayoutChildrenForIndex(workspace, pageIndex - 1);
                        View newIcon6 = getIconInDirection(layout, parent2, parent2.getChildCount(), -1);
                        if (newIcon6 != null) {
                            newIcon6.requestFocus();
                        } else {
                            workspace.snapToPage(pageIndex - 1);
                        }
                    }
                }
                break;
            case 22:
                if (handleKeyEvent) {
                    View newIcon7 = getIconInDirection(layout, parent, v, 1);
                    if (newIcon7 != null) {
                        newIcon7.requestFocus();
                    } else if (pageIndex < pageCount - 1) {
                        View newIcon8 = getIconInDirection(layout, getCellLayoutChildrenForIndex(workspace, pageIndex + 1), -1, 1);
                        if (newIcon8 != null) {
                            newIcon8.requestFocus();
                        } else {
                            workspace.snapToPage(pageIndex + 1);
                        }
                    }
                }
                break;
            case 92:
                if (handleKeyEvent) {
                    if (pageIndex > 0) {
                        View newIcon9 = getIconInDirection(layout, getCellLayoutChildrenForIndex(workspace, pageIndex - 1), -1, 1);
                        if (newIcon9 != null) {
                            newIcon9.requestFocus();
                        } else {
                            workspace.snapToPage(pageIndex - 1);
                        }
                    } else {
                        View newIcon10 = getIconInDirection(layout, parent, -1, 1);
                        if (newIcon10 != null) {
                            newIcon10.requestFocus();
                        }
                    }
                }
                break;
            case 93:
                if (handleKeyEvent) {
                    if (pageIndex < pageCount - 1) {
                        View newIcon11 = getIconInDirection(layout, getCellLayoutChildrenForIndex(workspace, pageIndex + 1), -1, 1);
                        if (newIcon11 != null) {
                            newIcon11.requestFocus();
                        } else {
                            workspace.snapToPage(pageIndex + 1);
                        }
                    } else {
                        View newIcon12 = getIconInDirection(layout, parent, parent.getChildCount(), -1);
                        if (newIcon12 != null) {
                            newIcon12.requestFocus();
                        }
                    }
                }
                break;
            case 122:
                if (handleKeyEvent && (newIcon2 = getIconInDirection(layout, parent, -1, 1)) != null) {
                    newIcon2.requestFocus();
                }
                break;
            case 123:
                if (handleKeyEvent && (newIcon = getIconInDirection(layout, parent, parent.getChildCount(), -1)) != null) {
                    newIcon.requestFocus();
                }
                break;
        }
        return false;
    }

    static boolean handleFolderKeyEvent(View v, int keyCode, KeyEvent e) {
        View newIcon;
        View newIcon2;
        View newIcon3;
        View newIcon4;
        ShortcutAndWidgetContainer parent = (ShortcutAndWidgetContainer) v.getParent();
        CellLayout layout = (CellLayout) parent.getParent();
        Folder folder = (Folder) layout.getParent();
        View title = folder.mFolderName;
        int action = e.getAction();
        boolean handleKeyEvent = action != 1;
        switch (keyCode) {
            case 19:
                if (handleKeyEvent && (newIcon3 = getClosestIconOnLine(layout, parent, v, -1)) != null) {
                    newIcon3.requestFocus();
                }
                return true;
            case 20:
                if (handleKeyEvent) {
                    View newIcon5 = getClosestIconOnLine(layout, parent, v, 1);
                    if (newIcon5 != null) {
                        newIcon5.requestFocus();
                    } else {
                        title.requestFocus();
                    }
                }
                return true;
            case 21:
                if (handleKeyEvent && (newIcon4 = getIconInDirection(layout, parent, v, -1)) != null) {
                    newIcon4.requestFocus();
                }
                return true;
            case 22:
                if (handleKeyEvent) {
                    View newIcon6 = getIconInDirection(layout, parent, v, 1);
                    if (newIcon6 != null) {
                        newIcon6.requestFocus();
                    } else {
                        title.requestFocus();
                    }
                }
                return true;
            case 122:
                if (handleKeyEvent && (newIcon2 = getIconInDirection(layout, parent, -1, 1)) != null) {
                    newIcon2.requestFocus();
                }
                return true;
            case 123:
                if (handleKeyEvent && (newIcon = getIconInDirection(layout, parent, parent.getChildCount(), -1)) != null) {
                    newIcon.requestFocus();
                }
                return true;
            default:
                return false;
        }
    }
}
