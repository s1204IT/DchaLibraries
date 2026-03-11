package com.mediatek.browser.ext;

import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class DefaultBrowserHistoryExt implements IBrowserHistoryExt {
    private static final String TAG = "DefaultBrowserHistoryExt";

    @Override
    public void createHistoryPageOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i("@M_DefaultBrowserHistoryExt", "Enter: createHistoryPageOptionsMenu --default implement");
    }

    @Override
    public void prepareHistoryPageOptionsMenuItem(Menu menu, boolean isNull, boolean isEmpty) {
        Log.i("@M_DefaultBrowserHistoryExt", "Enter: prepareHistoryPageOptionsMenuItem --default implement");
    }

    @Override
    public boolean historyPageOptionsMenuItemSelected(MenuItem item, Activity activity) {
        Log.i("@M_DefaultBrowserHistoryExt", "Enter: historyPageOptionsMenuItemSelected --default implement");
        return false;
    }
}
