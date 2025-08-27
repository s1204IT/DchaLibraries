package com.android.settings.applications;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class SpecialAccessSettings extends DashboardFragment {
    private static final String[] DISABLED_FEATURES_LOW_RAM = {"notification_access", "zen_access", "enabled_vr_listeners", "picture_in_picture"};
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.applications.SpecialAccessSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.special_access;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return SpecialAccessSettings.buildPreferenceControllers(context);
        }
    };

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "SpecialAccessSettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.special_access;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (ActivityManager.isLowRamDeviceStatic()) {
            for (String str : DISABLED_FEATURES_LOW_RAM) {
                if (findPreference(str) != null) {
                    removePreference(str);
                }
            }
        }
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new HighPowerAppsController(context));
        arrayList.add(new DeviceAdministratorsController(context));
        arrayList.add(new PremiumSmsController(context));
        arrayList.add(new DataSaverController(context));
        arrayList.add(new EnabledVrListenersController(context));
        return arrayList;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 351;
    }
}
