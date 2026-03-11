package com.android.settings.datausage;

import android.app.Application;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.datausage.AppStateDataUsageBridge;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;

public class DataSaverSummary extends SettingsPreferenceFragment implements AppStateBaseBridge.Callback, DataSaverBackend.Listener, SwitchBar.OnSwitchChangeListener, ApplicationsState.Callbacks {
    private ApplicationsState mApplicationsState;
    private DataSaverBackend mDataSaverBackend;
    private AppStateDataUsageBridge mDataUsageBridge;
    private ApplicationsState.Session mSession;
    private SwitchBar mSwitchBar;
    private boolean mSwitching;
    private Preference mUnrestrictedAccess;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.data_saver);
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(android.R.string.app_suspended_more_details);
        this.mUnrestrictedAccess = findPreference("unrestricted_access");
        this.mApplicationsState = ApplicationsState.getInstance((Application) getContext().getApplicationContext());
        this.mDataSaverBackend = new DataSaverBackend(getContext());
        this.mDataUsageBridge = new AppStateDataUsageBridge(this.mApplicationsState, this, this.mDataSaverBackend);
        this.mSession = this.mApplicationsState.newSession(this, getLifecycle());
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.setSwitchBarText(R.string.data_saver_switch_title, R.string.data_saver_switch_title);
        this.mSwitchBar.show();
        this.mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mDataSaverBackend.refreshWhitelist();
        this.mDataSaverBackend.refreshBlacklist();
        this.mDataSaverBackend.addListener(this);
        this.mDataUsageBridge.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mDataSaverBackend.remListener(this);
        this.mDataUsageBridge.pause();
    }

    @Override
    public void onSwitchChanged(Switch r1, boolean z) {
        synchronized (this) {
            if (this.mSwitching) {
                return;
            }
            this.mSwitching = true;
            this.mDataSaverBackend.setDataSaverEnabled(z);
        }
    }

    @Override
    public int getMetricsCategory() {
        return 348;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_data_saver;
    }

    @Override
    public void onDataSaverChanged(boolean z) {
        synchronized (this) {
            this.mSwitchBar.setChecked(z);
            this.mSwitching = false;
        }
    }

    @Override
    public void onWhitelistStatusChanged(int i, boolean z) {
    }

    @Override
    public void onBlacklistStatusChanged(int i, boolean z) {
    }

    @Override
    public void onExtraInfoUpdated() {
        if (!isAdded()) {
            return;
        }
        ArrayList<ApplicationsState.AppEntry> allApps = this.mSession.getAllApps();
        int size = allApps.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            ApplicationsState.AppEntry appEntry = allApps.get(i2);
            if (ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER.filterApp(appEntry) && appEntry.extraInfo != null && ((AppStateDataUsageBridge.DataUsageState) appEntry.extraInfo).isDataSaverWhitelisted) {
                i++;
            }
        }
        this.mUnrestrictedAccess.setSummary(getResources().getQuantityString(R.plurals.data_saver_unrestricted_summary, i, Integer.valueOf(i)));
    }

    @Override
    public void onRunningStateChanged(boolean z) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> arrayList) {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String str) {
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onLoadEntriesCompleted() {
    }
}
