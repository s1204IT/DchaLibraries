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

public class SpecialAccessSettings extends DashboardFragment {
    private static final String[] DISABLED_FEATURES_LOW_RAM = {"notification_access", "zen_access", "enabled_vr_listeners", "picture_in_picture"};
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.special_access;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return SpecialAccessSettings.buildPreferenceControllers(context);
        }
    };

    @Override
    protected String getLogTag() {
        return "SpecialAccessSettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.special_access;
    }

    @Override
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

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new HighPowerAppsController(context));
        arrayList.add(new DeviceAdministratorsController(context));
        arrayList.add(new PremiumSmsController(context));
        arrayList.add(new DataSaverController(context));
        arrayList.add(new EnabledVrListenersController(context));
        return arrayList;
    }

    @Override
    public int getMetricsCategory() {
        return 351;
    }
}
