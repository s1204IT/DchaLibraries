package com.android.deskclock.actionbarmenu;

import android.view.Menu;
import android.view.MenuItem;

public interface MenuItemController {
    int getId();

    void onCreateOptionsItem(Menu menu);

    boolean onOptionsItemSelected(MenuItem menuItem);

    void onPrepareOptionsItem(MenuItem menuItem);
}
