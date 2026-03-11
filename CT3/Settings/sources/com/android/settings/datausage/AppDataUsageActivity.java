package com.android.settings.datausage;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.AppItem;

public class AppDataUsageActivity extends SettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        String packageName = intent.getData().getSchemeSpecificPart();
        PackageManager pm = getPackageManager();
        try {
            int uid = pm.getPackageUid(packageName, 0);
            Bundle args = new Bundle();
            AppItem appItem = new AppItem(uid);
            appItem.addUid(uid);
            args.putParcelable("app_item", appItem);
            intent.putExtra(":settings:show_fragment_args", args);
            intent.putExtra(":settings:show_fragment", AppDataUsage.class.getName());
            intent.putExtra(":settings:show_fragment_title_resid", R.string.app_data_usage);
            super.onCreate(savedInstanceState);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("AppDataUsageActivity", "invalid package: " + packageName);
            try {
                super.onCreate(savedInstanceState);
            } catch (Exception e2) {
            } finally {
                finish();
            }
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (super.isValidFragment(fragmentName)) {
            return true;
        }
        return AppDataUsage.class.getName().equals(fragmentName);
    }
}
