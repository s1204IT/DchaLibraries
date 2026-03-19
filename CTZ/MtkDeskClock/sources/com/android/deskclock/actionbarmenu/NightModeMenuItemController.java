package com.android.deskclock.actionbarmenu;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import com.android.deskclock.R;
import com.android.deskclock.ScreensaverActivity;
import com.android.deskclock.events.Events;

public final class NightModeMenuItemController implements MenuItemController {
    private static final int NIGHT_MODE_MENU_RES_ID = 2131361950;
    private final Context mContext;

    public NightModeMenuItemController(Context context) {
        this.mContext = context;
    }

    @Override
    public int getId() {
        return R.id.menu_item_night_mode;
    }

    @Override
    public void onCreateOptionsItem(Menu menu) {
        menu.add(0, R.id.menu_item_night_mode, 0, R.string.menu_item_night_mode).setShowAsAction(0);
    }

    @Override
    public void onPrepareOptionsItem(MenuItem menuItem) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        this.mContext.startActivity(new Intent(this.mContext, (Class<?>) ScreensaverActivity.class).setFlags(268435456).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));
        return true;
    }
}
