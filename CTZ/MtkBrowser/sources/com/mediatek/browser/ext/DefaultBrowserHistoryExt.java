package com.mediatek.browser.ext;

import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class DefaultBrowserHistoryExt implements IBrowserHistoryExt {
    @Override
    public void createHistoryPageOptionsMenu(Menu menu, MenuInflater menuInflater) {
        Log.i("@M_DefaultBrowserHistoryExt", "Enter: createHistoryPageOptionsMenu --default implement");
    }

    @Override
    public void prepareHistoryPageOptionsMenuItem(Menu menu, boolean z, boolean z2) {
        Log.i("@M_DefaultBrowserHistoryExt", "Enter: prepareHistoryPageOptionsMenuItem --default implement");
    }

    @Override
    public boolean historyPageOptionsMenuItemSelected(MenuItem menuItem, Activity activity) {
        Log.i("@M_DefaultBrowserHistoryExt", "Enter: historyPageOptionsMenuItemSelected --default implement");
        return false;
    }
}
