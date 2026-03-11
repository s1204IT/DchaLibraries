package com.android.settings.datausage;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.datausage.AppStateDataUsageBridge;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.AppSwitchPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;

public class UnrestrictedDataAccess extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, AppStateBaseBridge.Callback, ApplicationsState.Callbacks {
    private ApplicationsState mApplicationsState;
    private DataSaverBackend mDataSaverBackend;
    private AppStateDataUsageBridge mDataUsageBridge;
    private boolean mExtraLoaded;
    private ApplicationsState.AppFilter mFilter;
    private ApplicationsState.Session mSession;
    private boolean mShowSystem;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setAnimationAllowed(true);
        this.mApplicationsState = ApplicationsState.getInstance((Application) getContext().getApplicationContext());
        this.mDataSaverBackend = new DataSaverBackend(getContext());
        this.mDataUsageBridge = new AppStateDataUsageBridge(this.mApplicationsState, this, this.mDataSaverBackend);
        this.mSession = this.mApplicationsState.newSession(this, getLifecycle());
        this.mShowSystem = bundle != null && bundle.getBoolean("show_system");
        this.mFilter = this.mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 43, 0, this.mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 43) {
            this.mShowSystem = !this.mShowSystem;
            menuItem.setTitle(this.mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
            this.mFilter = this.mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;
            if (this.mExtraLoaded) {
                rebuild();
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("show_system", this.mShowSystem);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mDataUsageBridge.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mDataUsageBridge.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mDataUsageBridge.release();
    }

    @Override
    public void onExtraInfoUpdated() {
        this.mExtraLoaded = true;
        rebuild();
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_unrestricted_data_access;
    }

    private void rebuild() {
        ArrayList<ApplicationsState.AppEntry> arrayListRebuild = this.mSession.rebuild(this.mFilter, ApplicationsState.ALPHA_COMPARATOR);
        if (arrayListRebuild != null) {
            onRebuildComplete(arrayListRebuild);
        }
    }

    @Override
    public void onRunningStateChanged(boolean z) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> arrayList) {
        if (getContext() == null) {
            return;
        }
        cacheRemoveAllPrefs(getPreferenceScreen());
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ApplicationsState.AppEntry appEntry = arrayList.get(i);
            if (shouldAddPreference(appEntry)) {
                String str = appEntry.info.packageName + "|" + appEntry.info.uid;
                AccessPreference accessPreference = (AccessPreference) getCachedPreference(str);
                if (accessPreference == null) {
                    accessPreference = new AccessPreference(getPrefContext(), appEntry);
                    accessPreference.setKey(str);
                    accessPreference.setOnPreferenceChangeListener(this);
                    getPreferenceScreen().addPreference(accessPreference);
                } else {
                    accessPreference.setDisabledByAdmin(RestrictedLockUtils.checkIfMeteredDataRestricted(getContext(), appEntry.info.packageName, UserHandle.getUserId(appEntry.info.uid)));
                    accessPreference.reuse();
                }
                accessPreference.setOrder(i);
            }
        }
        setLoading(false, true);
        removeCachedPrefs(getPreferenceScreen());
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

    @Override
    public int getMetricsCategory() {
        return 349;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.unrestricted_data_access_settings;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (!(preference instanceof AccessPreference)) {
            return false;
        }
        AccessPreference accessPreference = (AccessPreference) preference;
        boolean z = obj == Boolean.TRUE;
        logSpecialPermissionChange(z, accessPreference.mEntry.info.packageName);
        this.mDataSaverBackend.setIsWhitelisted(accessPreference.mEntry.info.uid, accessPreference.mEntry.info.packageName, z);
        if (accessPreference.mState != null) {
            accessPreference.mState.isDataSaverWhitelisted = z;
        }
        return true;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean z, String str) {
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), z ? 781 : 782, str, new Pair[0]);
    }

    @VisibleForTesting
    boolean shouldAddPreference(ApplicationsState.AppEntry appEntry) {
        return appEntry != null && UserHandle.isApp(appEntry.info.uid);
    }

    @VisibleForTesting
    class AccessPreference extends AppSwitchPreference implements DataSaverBackend.Listener {
        private final ApplicationsState.AppEntry mEntry;
        private final RestrictedPreferenceHelper mHelper;
        private final AppStateDataUsageBridge.DataUsageState mState;

        public AccessPreference(Context context, ApplicationsState.AppEntry appEntry) {
            super(context);
            setWidgetLayoutResource(R.layout.restricted_switch_widget);
            this.mHelper = new RestrictedPreferenceHelper(context, this, null);
            this.mEntry = appEntry;
            this.mState = (AppStateDataUsageBridge.DataUsageState) this.mEntry.extraInfo;
            this.mEntry.ensureLabel(getContext());
            setDisabledByAdmin(RestrictedLockUtils.checkIfMeteredDataRestricted(context, appEntry.info.packageName, UserHandle.getUserId(appEntry.info.uid)));
            setState();
            if (this.mEntry.icon != null) {
                setIcon(this.mEntry.icon);
            }
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
                AppInfoDashboardFragment.startAppInfoFragment(AppDataUsage.class, R.string.app_data_usage, null, UnrestrictedDataAccess.this, this.mEntry);
            } else {
                super.onClick();
            }
        }

        @Override
        public void performClick() {
            if (!this.mHelper.performClick()) {
                super.performClick();
            }
        }

        private void setState() {
            setTitle(this.mEntry.label);
            if (this.mState != null) {
                setChecked(this.mState.isDataSaverWhitelisted);
                if (isDisabledByAdmin()) {
                    setSummary(R.string.disabled_by_admin);
                } else if (this.mState.isDataSaverBlacklisted) {
                    setSummary(R.string.restrict_background_blacklisted);
                } else {
                    setSummary("");
                }
            }
        }

        public void reuse() {
            setState();
            notifyChanged();
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
            int i;
            if (this.mEntry.icon == null) {
                preferenceViewHolder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        UnrestrictedDataAccess.this.mApplicationsState.ensureIcon(AccessPreference.this.mEntry);
                        AccessPreference.this.setIcon(AccessPreference.this.mEntry.icon);
                    }
                });
            }
            boolean zIsDisabledByAdmin = isDisabledByAdmin();
            View viewFindViewById = preferenceViewHolder.findViewById(android.R.id.widget_frame);
            int i2 = 0;
            if (zIsDisabledByAdmin) {
                viewFindViewById.setVisibility(0);
            } else {
                if (this.mState == null || !this.mState.isDataSaverBlacklisted) {
                    i = 0;
                } else {
                    i = 4;
                }
                viewFindViewById.setVisibility(i);
            }
            super.onBindViewHolder(preferenceViewHolder);
            this.mHelper.onBindViewHolder(preferenceViewHolder);
            preferenceViewHolder.findViewById(R.id.restricted_icon).setVisibility(zIsDisabledByAdmin ? 0 : 8);
            View viewFindViewById2 = preferenceViewHolder.findViewById(android.R.id.switch_widget);
            if (zIsDisabledByAdmin) {
                i2 = 8;
            }
            viewFindViewById2.setVisibility(i2);
        }

        @Override
        public void onDataSaverChanged(boolean z) {
        }

        @Override
        public void onWhitelistStatusChanged(int i, boolean z) {
            if (this.mState != null && this.mEntry.info.uid == i) {
                this.mState.isDataSaverWhitelisted = z;
                reuse();
            }
        }

        @Override
        public void onBlacklistStatusChanged(int i, boolean z) {
            if (this.mState != null && this.mEntry.info.uid == i) {
                this.mState.isDataSaverBlacklisted = z;
                reuse();
            }
        }

        public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
            this.mHelper.setDisabledByAdmin(enforcedAdmin);
        }

        public boolean isDisabledByAdmin() {
            return this.mHelper.isDisabledByAdmin();
        }

        @VisibleForTesting
        public ApplicationsState.AppEntry getEntryForTest() {
            return this.mEntry;
        }
    }
}
