package com.android.quicksearchbox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Help {
    private final Config mConfig;
    private final Context mContext;

    public Help(Context context, Config config) {
        this.mContext = context;
        this.mConfig = config;
    }

    public void addHelpMenuItem(Menu menu, String activityName) {
        addHelpMenuItem(menu, activityName, false);
    }

    public void addHelpMenuItem(Menu menu, String activityName, boolean showAsAction) {
        Intent helpIntent = null;
        int dcha_state = Settings.System.getInt(this.mContext.getContentResolver(), "dcha_state", 0);
        if (dcha_state == 0) {
            helpIntent = getHelpIntent(activityName);
        }
        if (helpIntent == null) {
            return;
        }
        MenuInflater inflater = new MenuInflater(this.mContext);
        inflater.inflate(R.menu.help, menu);
        MenuItem item = menu.findItem(R.id.menu_help);
        item.setIntent(helpIntent);
        if (!showAsAction) {
            return;
        }
        item.setShowAsAction(2);
    }

    private Intent getHelpIntent(String activityName) {
        Uri helpUrl = this.mConfig.getHelpUrl(activityName);
        if (helpUrl == null) {
            return null;
        }
        return new Intent("android.intent.action.VIEW", helpUrl);
    }
}
