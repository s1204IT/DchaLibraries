package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.PermissionsSummaryHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdvancedAppSettings extends SettingsPreferenceFragment implements ApplicationsState.Callbacks, Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.advanced_apps;
            return Arrays.asList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            return Utils.getNonIndexable(R.xml.advanced_apps, context);
        }
    };
    private Preference mAppDomainURLsPreference;
    private Preference mAppPermsPreference;
    private Preference mHighPowerPreference;
    private final PermissionsSummaryHelper.PermissionsResultCallback mPermissionCallback = new PermissionsSummaryHelper.PermissionsResultCallback() {
    };
    private ApplicationsState.Session mSession;
    private Preference mSystemAlertWindowPreference;
    private Preference mWriteSettingsPreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.advanced_apps);
        Preference permissions = getPreferenceScreen().findPreference("manage_perms");
        permissions.setIntent(new Intent("android.intent.action.MANAGE_PERMISSIONS"));
        ApplicationsState applicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        this.mSession = applicationsState.newSession(this);
        this.mAppPermsPreference = findPreference("manage_perms");
        this.mAppDomainURLsPreference = findPreference("domain_urls");
        this.mHighPowerPreference = findPreference("high_power_apps");
        this.mSystemAlertWindowPreference = findPreference("system_alert_window");
        this.mWriteSettingsPreference = findPreference("write_settings_apps");
    }

    @Override
    protected int getMetricsCategory() {
        return 130;
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
