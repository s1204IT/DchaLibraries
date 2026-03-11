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

public class DataSaverSummary extends SettingsPreferenceFragment implements SwitchBar.OnSwitchChangeListener, DataSaverBackend.Listener, AppStateBaseBridge.Callback, ApplicationsState.Callbacks {
    private ApplicationsState mApplicationsState;
    private DataSaverBackend mDataSaverBackend;
    private AppStateDataUsageBridge mDataUsageBridge;
    private ApplicationsState.Session mSession;
    private SwitchBar mSwitchBar;
    private boolean mSwitching;
    private Preference mUnrestrictedAccess;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.data_saver);
        this.mUnrestrictedAccess = findPreference("unrestricted_access");
        this.mApplicationsState = ApplicationsState.getInstance((Application) getContext().getApplicationContext());
        this.mDataSaverBackend = new DataSaverBackend(getContext());
        this.mDataUsageBridge = new AppStateDataUsageBridge(this.mApplicationsState, this, this.mDataSaverBackend);
        this.mSession = this.mApplicationsState.newSession(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.show();
        this.mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mDataSaverBackend.refreshWhitelist();
        this.mDataSaverBackend.refreshBlacklist();
        this.mDataSaverBackend.addListener(this);
        this.mSession.resume();
        this.mDataUsageBridge.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mDataSaverBackend.remListener(this);
        this.mDataUsageBridge.pause();
        this.mSession.pause();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        synchronized (this) {
            if (this.mSwitching) {
                return;
            }
            this.mSwitching = true;
            this.mDataSaverBackend.setDataSaverEnabled(isChecked);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 348;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_data_saver;
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        synchronized (this) {
            this.mSwitchBar.setChecked(isDataSaving);
            this.mSwitching = false;
        }
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
    }

    @Override
    public void onExtraInfoUpdated() {
        if (!isAdded()) {
            return;
        }
        int count = 0;
        ArrayList<ApplicationsState.AppEntry> allApps = this.mSession.getAllApps();
        int N = allApps.size();
        for (int i = 0; i < N; i++) {
            ApplicationsState.AppEntry entry = allApps.get(i);
            if (ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER.filterApp(entry) && entry.extraInfo != null && ((AppStateDataUsageBridge.DataUsageState) entry.extraInfo).isDataSaverWhitelisted) {
                count++;
            }
        }
        this.mUnrestrictedAccess.setSummary(getResources().getQuantityString(R.plurals.data_saver_unrestricted_summary, count, Integer.valueOf(count)));
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void m683xf40fefa9(ArrayList<ApplicationsState.AppEntry> apps) {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
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
