package com.android.deskclock.actionbarmenu;

import android.app.Activity;
import java.util.ArrayList;
import java.util.List;

public final class MenuItemControllerFactory {
    private static final MenuItemControllerFactory INSTANCE = new MenuItemControllerFactory();
    private final List<MenuItemProvider> mMenuItemProviders = new ArrayList();

    public static MenuItemControllerFactory getInstance() {
        return INSTANCE;
    }

    private MenuItemControllerFactory() {
    }

    public MenuItemControllerFactory addMenuItemProvider(MenuItemProvider menuItemProvider) {
        this.mMenuItemProviders.add(menuItemProvider);
        return this;
    }

    public MenuItemController[] buildMenuItemControllers(Activity activity) {
        int size = this.mMenuItemProviders.size();
        MenuItemController[] menuItemControllerArr = new MenuItemController[size];
        for (int i = 0; i < size; i++) {
            menuItemControllerArr[i] = this.mMenuItemProviders.get(i).provide(activity);
        }
        return menuItemControllerArr;
    }
}
