package com.android.deskclock.actionbarmenu;

import android.app.Activity;

public interface MenuItemProvider {
    MenuItemController provide(Activity activity);
}
