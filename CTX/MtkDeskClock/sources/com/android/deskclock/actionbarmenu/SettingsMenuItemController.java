package com.android.deskclock.actionbarmenu;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import com.android.deskclock.R;
import com.android.deskclock.settings.SettingsActivity;

public final class SettingsMenuItemController implements MenuItemController {
    public static final int REQUEST_CHANGE_SETTINGS = 1;
    private static final int SETTING_MENU_RES_ID = 2131361952;
    private final Activity mActivity;

    public SettingsMenuItemController(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public int getId() {
        return R.id.menu_item_settings;
    }

    @Override
    public void onCreateOptionsItem(Menu menu) {
        menu.add(0, R.id.menu_item_settings, 0, R.string.menu_item_settings).setShowAsAction(0);
    }

    @Override
    public void onPrepareOptionsItem(MenuItem menuItem) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        this.mActivity.startActivityForResult(new Intent(this.mActivity, (Class<?>) SettingsActivity.class), 1);
        return true;
    }
}
