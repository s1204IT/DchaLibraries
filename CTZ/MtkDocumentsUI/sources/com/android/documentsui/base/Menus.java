package com.android.documentsui.base;

import android.view.Menu;
import android.view.MenuItem;

public final class Menus {
    public static void disableHiddenItems(Menu menu, MenuItem... menuItemArr) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (!item.isVisible() && !contains(menuItemArr, item)) {
                item.setEnabled(false);
            }
        }
    }

    private static boolean contains(MenuItem[] menuItemArr, MenuItem menuItem) {
        for (MenuItem menuItem2 : menuItemArr) {
            if (menuItem2 == menuItem) {
                return true;
            }
        }
        return false;
    }
}
