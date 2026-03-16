package com.android.calendar;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

public interface AllInOneMenuExtensionsInterface {
    Integer getExtensionMenuResource(Menu menu);

    boolean handleItemSelected(MenuItem menuItem, Context context);
}
