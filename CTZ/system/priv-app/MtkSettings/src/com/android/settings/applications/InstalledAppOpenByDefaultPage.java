package com.android.settings.applications;

import android.content.Intent;
import com.android.settings.SettingsActivity;

/* loaded from: classes.dex */
public class InstalledAppOpenByDefaultPage extends SettingsActivity {
    @Override // com.android.settings.SettingsActivity, android.app.Activity
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", AppLaunchSettings.class.getName());
        return intent;
    }

    @Override // com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        return AppLaunchSettings.class.getName().equals(str);
    }
}
