package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.Formatter;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.applications.ProcStatsEntry;
import com.android.settings.applications.ProcStatsPackageEntry;
import com.android.settings.applications.ProcessStatsBase;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import java.util.Iterator;
/* loaded from: classes.dex */
public class AppMemoryPreferenceController extends BasePreferenceController implements LifecycleObserver, OnResume {
    private static final String KEY_MEMORY = "memory";
    private final AppInfoDashboardFragment mParent;
    private Preference mPreference;
    private ProcStatsPackageEntry mStats;
    private ProcStatsData mStatsManager;

    /* loaded from: classes.dex */
    private class MemoryUpdater extends AsyncTask<Void, Void, ProcStatsPackageEntry> {
        private MemoryUpdater() {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public ProcStatsPackageEntry doInBackground(Void... voidArr) {
            PackageInfo packageInfo;
            Activity activity = AppMemoryPreferenceController.this.mParent.getActivity();
            if (activity != null && (packageInfo = AppMemoryPreferenceController.this.mParent.getPackageInfo()) != null) {
                if (AppMemoryPreferenceController.this.mStatsManager == null) {
                    AppMemoryPreferenceController.this.mStatsManager = new ProcStatsData(activity, false);
                    AppMemoryPreferenceController.this.mStatsManager.setDuration(ProcessStatsBase.sDurations[0]);
                }
                AppMemoryPreferenceController.this.mStatsManager.refreshStats(true);
                for (ProcStatsPackageEntry procStatsPackageEntry : AppMemoryPreferenceController.this.mStatsManager.getEntries()) {
                    Iterator<ProcStatsEntry> it = procStatsPackageEntry.getEntries().iterator();
                    while (it.hasNext()) {
                        if (it.next().getUid() == packageInfo.applicationInfo.uid) {
                            procStatsPackageEntry.updateMetrics();
                            return procStatsPackageEntry;
                        }
                    }
                }
                return null;
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(ProcStatsPackageEntry procStatsPackageEntry) {
            if (AppMemoryPreferenceController.this.mParent.getActivity() == null) {
                return;
            }
            if (procStatsPackageEntry != null) {
                AppMemoryPreferenceController.this.mStats = procStatsPackageEntry;
                AppMemoryPreferenceController.this.mPreference.setEnabled(true);
                AppMemoryPreferenceController.this.mPreference.setSummary(AppMemoryPreferenceController.this.mContext.getString(R.string.memory_use_summary, Formatter.formatShortFileSize(AppMemoryPreferenceController.this.mContext, (long) (Math.max(procStatsPackageEntry.getRunWeight(), procStatsPackageEntry.getBgWeight()) * AppMemoryPreferenceController.this.mStatsManager.getMemInfo().getWeightToRam()))));
                return;
            }
            AppMemoryPreferenceController.this.mPreference.setEnabled(false);
            AppMemoryPreferenceController.this.mPreference.setSummary(AppMemoryPreferenceController.this.mContext.getString(R.string.no_memory_use_summary));
        }
    }

    public AppMemoryPreferenceController(Context context, AppInfoDashboardFragment appInfoDashboardFragment, Lifecycle lifecycle) {
        super(context, KEY_MEMORY);
        this.mParent = appInfoDashboardFragment;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_show_app_info_settings_memory)) {
            return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this.mContext) ? 0 : 1;
        }
        return 2;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_MEMORY.equals(preference.getKey())) {
            ProcessStatsBase.launchMemoryDetail((SettingsActivity) this.mParent.getActivity(), this.mStatsManager.getMemInfo(), this.mStats, false);
            return true;
        }
        return false;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (isAvailable()) {
            new MemoryUpdater().execute(new Void[0]);
        }
    }
}
