package com.android.settings.fuelgauge;

import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import java.util.List;

public class InactiveApps extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {
    private UsageStatsManager mUsageStats;

    @Override
    protected int getMetricsCategory() {
        return 238;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mUsageStats = (UsageStatsManager) getActivity().getSystemService(UsageStatsManager.class);
        addPreferencesFromResource(R.xml.inactive_apps);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        PreferenceGroup screen = getPreferenceScreen();
        screen.removeAll();
        screen.setOrderingAsAdded(false);
        Context context = getActivity();
        PackageManager pm = context.getPackageManager();
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> apps = pm.queryIntentActivities(launcherIntent, 0);
        for (ResolveInfo app : apps) {
            String packageName = app.activityInfo.applicationInfo.packageName;
            Preference p = new Preference(getPrefContext());
            p.setTitle(app.loadLabel(pm));
            p.setIcon(app.loadIcon(pm));
            p.setKey(packageName);
            updateSummary(p);
            p.setOnPreferenceClickListener(this);
            screen.addPreference(p);
        }
    }

    private void updateSummary(Preference p) {
        int i;
        boolean inactive = this.mUsageStats.isAppInactive(p.getKey());
        if (inactive) {
            i = R.string.inactive_app_inactive_summary;
        } else {
            i = R.string.inactive_app_active_summary;
        }
        p.setSummary(i);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String packageName = preference.getKey();
        this.mUsageStats.setAppInactive(packageName, !this.mUsageStats.isAppInactive(packageName));
        updateSummary(preference);
        return false;
    }
}
