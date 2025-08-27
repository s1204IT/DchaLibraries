package com.android.settings;

import android.util.Log;

/* loaded from: classes.dex */
public class SubSettings extends SettingsActivity {
    @Override // com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override // com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        Log.d("SubSettings", "Launching fragment " + str);
        return true;
    }
}
