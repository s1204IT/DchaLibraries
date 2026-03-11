package com.android.settings.datausage;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.datausage.AppStateDataUsageBridge;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;

public class UnrestrictedDataAccess extends SettingsPreferenceFragment implements ApplicationsState.Callbacks, AppStateBaseBridge.Callback, Preference.OnPreferenceChangeListener {
    private ApplicationsState mApplicationsState;
    private DataSaverBackend mDataSaverBackend;
    private AppStateDataUsageBridge mDataUsageBridge;
    private boolean mExtraLoaded;
    private ApplicationsState.AppFilter mFilter;
    private ApplicationsState.Session mSession;
    private boolean mShowSystem;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        this.mApplicationsState = ApplicationsState.getInstance((Application) getContext().getApplicationContext());
        this.mDataSaverBackend = new DataSaverBackend(getContext());
        this.mDataUsageBridge = new AppStateDataUsageBridge(this.mApplicationsState, this, this.mDataSaverBackend);
        this.mSession = this.mApplicationsState.newSession(this);
        this.mShowSystem = icicle != null ? icicle.getBoolean("show_system") : false;
        this.mFilter = this.mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 43, 0, this.mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 43:
                this.mShowSystem = !this.mShowSystem;
                item.setTitle(this.mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
                this.mFilter = this.mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;
                if (this.mExtraLoaded) {
                    rebuild();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("show_system", this.mShowSystem);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSession.resume();
        this.mDataUsageBridge.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mDataUsageBridge.pause();
        this.mSession.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mSession.release();
        this.mDataUsageBridge.release();
    }

    @Override
    public void onExtraInfoUpdated() {
        this.mExtraLoaded = true;
        rebuild();
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_unrestricted_data_access;
    }

    private void rebuild() {
        ArrayList<ApplicationsState.AppEntry> apps = this.mSession.rebuild(this.mFilter, ApplicationsState.ALPHA_COMPARATOR);
        if (apps == null) {
            return;
        }
        m683xf40fefa9(apps);
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void m683xf40fefa9(ArrayList<ApplicationsState.AppEntry> apps) {
        if (getContext() == null) {
            return;
        }
        cacheRemoveAllPrefs(getPreferenceScreen());
        int N = apps.size();
        for (int i = 0; i < N; i++) {
            ApplicationsState.AppEntry entry = apps.get(i);
            String key = entry.info.packageName + "|" + entry.info.uid;
            AccessPreference preference = (AccessPreference) getCachedPreference(key);
            if (preference == null) {
                preference = new AccessPreference(getPrefContext(), entry);
                preference.setKey(key);
                preference.setOnPreferenceChangeListener(this);
                getPreferenceScreen().addPreference(preference);
            } else {
                preference.reuse();
            }
            preference.setOrder(i);
        }
        setLoading(false, true);
        removeCachedPrefs(getPreferenceScreen());
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

    @Override
    protected int getMetricsCategory() {
        return 349;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof AccessPreference) {
            AccessPreference accessPreference = (AccessPreference) preference;
            boolean whitelisted = newValue == Boolean.TRUE;
            this.mDataSaverBackend.setIsWhitelisted(accessPreference.mEntry.info.uid, accessPreference.mEntry.info.packageName, whitelisted);
            if (accessPreference.mState != null) {
                accessPreference.mState.isDataSaverWhitelisted = whitelisted;
                return true;
            }
            return true;
        }
        return false;
    }

    private class AccessPreference extends SwitchPreference implements DataSaverBackend.Listener {
        private final ApplicationsState.AppEntry mEntry;
        private final AppStateDataUsageBridge.DataUsageState mState;

        public AccessPreference(Context context, ApplicationsState.AppEntry entry) {
            super(context);
            this.mEntry = entry;
            this.mState = (AppStateDataUsageBridge.DataUsageState) this.mEntry.extraInfo;
            this.mEntry.ensureLabel(getContext());
            setState();
            if (this.mEntry.icon == null) {
                return;
            }
            setIcon(this.mEntry.icon);
        }

        @Override
        public void onAttached() {
            super.onAttached();
            UnrestrictedDataAccess.this.mDataSaverBackend.addListener(this);
        }

        @Override
        public void onDetached() {
            UnrestrictedDataAccess.this.mDataSaverBackend.remListener(this);
            super.onDetached();
        }

        @Override
        protected void onClick() {
            if (this.mState != null && this.mState.isDataSaverBlacklisted) {
                InstalledAppDetails.startAppInfoFragment(AppDataUsage.class, getContext().getString(R.string.app_data_usage), UnrestrictedDataAccess.this, this.mEntry);
            } else {
                super.onClick();
            }
        }

        private void setState() {
            setTitle(this.mEntry.label);
            if (this.mState == null) {
                return;
            }
            setChecked(this.mState.isDataSaverWhitelisted);
            if (this.mState.isDataSaverBlacklisted) {
                setSummary(R.string.restrict_background_blacklisted);
            } else {
                setSummary("");
            }
        }

        public void reuse() {
            setState();
            notifyChanged();
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            if (this.mEntry.icon == null) {
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        UnrestrictedDataAccess.this.mApplicationsState.ensureIcon(AccessPreference.this.mEntry);
                        AccessPreference.this.setIcon(AccessPreference.this.mEntry.icon);
                    }
                });
            }
            holder.findViewById(android.R.id.widget_frame).setVisibility((this.mState == null || !this.mState.isDataSaverBlacklisted) ? 0 : 4);
            super.onBindViewHolder(holder);
        }

        @Override
        public void onDataSaverChanged(boolean isDataSaving) {
        }

        @Override
        public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
            if (this.mState == null || this.mEntry.info.uid != uid) {
                return;
            }
            this.mState.isDataSaverWhitelisted = isWhitelisted;
            reuse();
        }

        @Override
        public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
            if (this.mState == null || this.mEntry.info.uid != uid) {
                return;
            }
            this.mState.isDataSaverBlacklisted = isBlacklisted;
            reuse();
        }
    }
}
