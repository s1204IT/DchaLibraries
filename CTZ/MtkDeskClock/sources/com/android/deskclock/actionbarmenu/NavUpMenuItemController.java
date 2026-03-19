package com.android.deskclock.actionbarmenu;

import android.R;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

public final class NavUpMenuItemController implements MenuItemController {
    private final Activity mActivity;

    public NavUpMenuItemController(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public int getId() {
        return R.id.home;
    }

    @Override
    public void onCreateOptionsItem(Menu menu) {
    }

    @Override
    public void onPrepareOptionsItem(MenuItem menuItem) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        this.mActivity.finish();
        return true;
    }
}
