package com.android.deskclock.actionbarmenu;

import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class OptionsMenuManager {
    private final List<MenuItemController> mControllers = new ArrayList();

    public OptionsMenuManager addMenuItemController(MenuItemController... menuItemControllerArr) {
        Collections.addAll(this.mControllers, menuItemControllerArr);
        return this;
    }

    public void onCreateOptionsMenu(Menu menu) {
        Iterator<MenuItemController> it = this.mControllers.iterator();
        while (it.hasNext()) {
            it.next().onCreateOptionsItem(menu);
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        for (MenuItemController menuItemController : this.mControllers) {
            MenuItem menuItemFindItem = menu.findItem(menuItemController.getId());
            if (menuItemFindItem != null) {
                menuItemController.onPrepareOptionsItem(menuItemFindItem);
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        for (MenuItemController menuItemController : this.mControllers) {
            if (menuItemController.getId() == itemId && menuItemController.onOptionsItemSelected(menuItem)) {
                return true;
            }
        }
        return false;
    }
}
