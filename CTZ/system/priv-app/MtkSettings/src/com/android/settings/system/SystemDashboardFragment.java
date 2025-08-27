package com.android.settings.system;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.backup.BackupSettingsActivityPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class SystemDashboardFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.system.SystemDashboardFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.system_dashboard_fragment;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add(new BackupSettingsActivityPreferenceController(context).getPreferenceKey());
            nonIndexableKeys.add("reset_dashboard");
            return nonIndexableKeys;
        }
    };
    private static IDeviceInfoSettingsExt mExt;

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (getVisiblePreferenceCount(preferenceScreen) == preferenceScreen.getInitialExpandedChildrenCount() + 1) {
            preferenceScreen.setInitialExpandedChildrenCount(Preference.DEFAULT_ORDER);
        }
        mExt = UtilsExt.getDeviceInfoSettingsExt(getActivity());
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 744;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "SystemDashboardFrag";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.system_dashboard_fragment;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_system_dashboard;
    }

    private int getVisiblePreferenceCount(PreferenceGroup preferenceGroup) {
        int visiblePreferenceCount = 0;
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                visiblePreferenceCount += getVisiblePreferenceCount((PreferenceGroup) preference);
            } else if (preference.isVisible()) {
                visiblePreferenceCount++;
            }
        }
        return visiblePreferenceCount;
    }
}
