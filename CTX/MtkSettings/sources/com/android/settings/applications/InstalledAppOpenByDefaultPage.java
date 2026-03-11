package com.android.settings.applications;

import android.content.Intent;
import com.android.settings.SettingsActivity;

public class InstalledAppOpenByDefaultPage extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", AppLaunchSettings.class.getName());
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        return AppLaunchSettings.class.getName().equals(str);
    }
}
