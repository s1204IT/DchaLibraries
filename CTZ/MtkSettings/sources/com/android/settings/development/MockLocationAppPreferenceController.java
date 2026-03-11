package com.android.settings.development;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.Iterator;
import java.util.List;

public class MockLocationAppPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin, OnActivityResultListener {
    private static final int[] MOCK_LOCATION_APP_OPS = {58};
    private final AppOpsManager mAppsOpsManager;
    private final DevelopmentSettingsDashboardFragment mFragment;
    private final PackageManagerWrapper mPackageManager;

    public MockLocationAppPreferenceController(Context context, DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment) {
        super(context);
        this.mFragment = developmentSettingsDashboardFragment;
        this.mAppsOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mPackageManager = new PackageManagerWrapper(context.getPackageManager());
    }

    @Override
    public String getPreferenceKey() {
        return "mock_location_app";
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        Intent intent = new Intent(this.mContext, (Class<?>) AppPicker.class);
        intent.putExtra("com.android.settings.extra.REQUESTIING_PERMISSION", "android.permission.ACCESS_MOCK_LOCATION");
        this.mFragment.startActivityForResult(intent, 2);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateMockLocation();
    }

    @Override
    public boolean onActivityResult(int i, int i2, Intent intent) {
        if (i != 2 || i2 != -1) {
            return false;
        }
        writeMockLocation(intent.getAction());
        updateMockLocation();
        return true;
    }

    private void updateMockLocation() {
        String currentMockLocationApp = getCurrentMockLocationApp();
        if (!TextUtils.isEmpty(currentMockLocationApp)) {
            this.mPreference.setSummary(this.mContext.getResources().getString(R.string.mock_location_app_set, getAppLabel(currentMockLocationApp)));
        } else {
            this.mPreference.setSummary(this.mContext.getResources().getString(R.string.mock_location_app_not_set));
        }
    }

    private void writeMockLocation(String str) {
        removeAllMockLocations();
        if (!TextUtils.isEmpty(str)) {
            try {
                this.mAppsOpsManager.setMode(58, this.mPackageManager.getApplicationInfo(str, 512).uid, str, 0);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    private String getAppLabel(String str) {
        try {
            CharSequence applicationLabel = this.mPackageManager.getApplicationLabel(this.mPackageManager.getApplicationInfo(str, 512));
            return applicationLabel != null ? applicationLabel.toString() : str;
        } catch (PackageManager.NameNotFoundException e) {
            return str;
        }
    }

    private void removeAllMockLocations() {
        List<AppOpsManager.PackageOps> packagesForOps = this.mAppsOpsManager.getPackagesForOps(MOCK_LOCATION_APP_OPS);
        if (packagesForOps == null) {
            return;
        }
        for (AppOpsManager.PackageOps packageOps : packagesForOps) {
            if (((AppOpsManager.OpEntry) packageOps.getOps().get(0)).getMode() != 2) {
                removeMockLocationForApp(packageOps.getPackageName());
            }
        }
    }

    private void removeMockLocationForApp(String str) {
        try {
            this.mAppsOpsManager.setMode(58, this.mPackageManager.getApplicationInfo(str, 512).uid, str, 2);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private String getCurrentMockLocationApp() {
        List packagesForOps = this.mAppsOpsManager.getPackagesForOps(MOCK_LOCATION_APP_OPS);
        if (packagesForOps != null) {
            Iterator it = packagesForOps.iterator();
            while (it.hasNext()) {
                if (((AppOpsManager.OpEntry) ((AppOpsManager.PackageOps) it.next()).getOps().get(0)).getMode() == 0) {
                    return ((AppOpsManager.PackageOps) packagesForOps.get(0)).getPackageName();
                }
            }
            return null;
        }
        return null;
    }
}
