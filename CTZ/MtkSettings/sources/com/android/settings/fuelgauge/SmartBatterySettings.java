package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SmartBatterySettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.smart_battery_detail;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return SmartBatterySettings.buildPreferenceControllers(context, null, null);
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.smart_battery_footer);
    }

    @Override
    public int getMetricsCategory() {
        return 1281;
    }

    @Override
    protected String getLogTag() {
        return "SmartBatterySettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.smart_battery_detail;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_smart_battery_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, (SettingsActivity) getActivity(), this);
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context, SettingsActivity settingsActivity, InstrumentedPreferenceFragment instrumentedPreferenceFragment) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new SmartBatteryPreferenceController(context));
        if (settingsActivity != null && instrumentedPreferenceFragment != null) {
            arrayList.add(new RestrictAppPreferenceController(instrumentedPreferenceFragment));
        } else {
            arrayList.add(new RestrictAppPreferenceController(context));
        }
        return arrayList;
    }
}
