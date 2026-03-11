package com.android.settings.applications;

import android.content.Intent;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;

public class InstalledAppDetailsTop extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", AppInfoDashboardFragment.class.getName());
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        return AppInfoDashboardFragment.class.getName().equals(str);
    }
}
