package com.android.quicksearchbox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/* loaded from: classes.dex */
public class Help {
    private final Config mConfig;
    private final Context mContext;

    public Help(Context context, Config config) {
        this.mContext = context;
        this.mConfig = config;
    }

    public void addHelpMenuItem(Menu menu, String str) {
        addHelpMenuItem(menu, str, false);
    }

    public void addHelpMenuItem(Menu menu, String str, boolean z) {
        Intent helpIntent;
        if (Settings.System.getInt(this.mContext.getContentResolver(), "dcha_state", 0) == 0) {
            helpIntent = getHelpIntent(str);
        } else {
            helpIntent = null;
        }
        if (helpIntent != null) {
            new MenuInflater(this.mContext).inflate(R.menu.help, menu);
            MenuItem menuItemFindItem = menu.findItem(R.id.menu_help);
            menuItemFindItem.setIntent(helpIntent);
            if (z) {
                menuItemFindItem.setShowAsAction(2);
            }
        }
    }

    private Intent getHelpIntent(String str) {
        Uri helpUrl = this.mConfig.getHelpUrl(str);
        if (helpUrl == null) {
            return null;
        }
        return new Intent("android.intent.action.VIEW", helpUrl);
    }
}
