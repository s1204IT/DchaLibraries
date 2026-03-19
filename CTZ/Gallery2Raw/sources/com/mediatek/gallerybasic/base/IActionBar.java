package com.mediatek.gallerybasic.base;

import android.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

public interface IActionBar {
    void onCreateOptionsMenu(ActionBar actionBar, Menu menu);

    boolean onOptionsItemSelected(MenuItem menuItem, MediaData mediaData);

    void onPrepareOptionsMenu(Menu menu, MediaData mediaData);
}
