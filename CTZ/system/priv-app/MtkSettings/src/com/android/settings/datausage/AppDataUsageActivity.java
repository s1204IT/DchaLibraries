package com.android.settings.datausage;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.AppItem;

/* loaded from: classes.dex */
public class AppDataUsageActivity extends SettingsActivity {
    @Override // com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    protected void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        Intent intent = getIntent();
        String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
        try {
            int packageUid = getPackageManager().getPackageUid(schemeSpecificPart, 0);
            Bundle bundle2 = new Bundle();
            AppItem appItem = new AppItem(packageUid);
            appItem.addUid(packageUid);
            bundle2.putParcelable("app_item", appItem);
            intent.putExtra(":settings:show_fragment_args", bundle2);
            intent.putExtra(":settings:show_fragment", AppDataUsage.class.getName());
            intent.putExtra(":settings:show_fragment_title_resid", R.string.app_data_usage);
            super.onCreate(bundle);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("AppDataUsageActivity", "invalid package: " + schemeSpecificPart);
            try {
                super.onCreate(bundle);
            } catch (Exception e2) {
            } catch (Throwable th) {
                finish();
                throw th;
            }
            finish();
        }
    }

    @Override // com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        return super.isValidFragment(str) || AppDataUsage.class.getName().equals(str);
    }
}
