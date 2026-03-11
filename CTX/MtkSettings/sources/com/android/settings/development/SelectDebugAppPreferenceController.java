package com.android.settings.development;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public class SelectDebugAppPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin, OnActivityResultListener {
    private final DevelopmentSettingsDashboardFragment mFragment;
    private final PackageManagerWrapper mPackageManager;

    public SelectDebugAppPreferenceController(Context context, DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment) {
        super(context);
        this.mFragment = developmentSettingsDashboardFragment;
        this.mPackageManager = new PackageManagerWrapper(this.mContext.getPackageManager());
    }

    @Override
    public String getPreferenceKey() {
        return "debug_app";
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if ("debug_app".equals(preference.getKey())) {
            Intent activityStartIntent = getActivityStartIntent();
            activityStartIntent.putExtra("com.android.settings.extra.DEBUGGABLE", true);
            this.mFragment.startActivityForResult(activityStartIntent, 1);
            return true;
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        updatePreferenceSummary();
    }

    @Override
    public boolean onActivityResult(int i, int i2, Intent intent) {
        if (i != 1 || i2 != -1) {
            return false;
        }
        Settings.Global.putString(this.mContext.getContentResolver(), "debug_app", intent.getAction());
        updatePreferenceSummary();
        return true;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        this.mPreference.setSummary(this.mContext.getResources().getString(R.string.debug_app_not_set));
    }

    Intent getActivityStartIntent() {
        return new Intent(this.mContext, (Class<?>) AppPicker.class);
    }

    private void updatePreferenceSummary() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "debug_app");
        if (string != null && string.length() > 0) {
            this.mPreference.setSummary(this.mContext.getResources().getString(R.string.debug_app_set, getAppLabel(string)));
        } else {
            this.mPreference.setSummary(this.mContext.getResources().getString(R.string.debug_app_not_set));
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
}
