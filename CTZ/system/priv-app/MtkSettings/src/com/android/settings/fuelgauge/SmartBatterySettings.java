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

/* loaded from: classes.dex */
public class SmartBatterySettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.fuelgauge.SmartBatterySettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.smart_battery_detail;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return SmartBatterySettings.buildPreferenceControllers(context, null, null);
        }
    };

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.smart_battery_footer);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1281;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "SmartBatterySettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.smart_battery_detail;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_uri_smart_battery_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, (SettingsActivity) getActivity(), this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, SettingsActivity settingsActivity, InstrumentedPreferenceFragment instrumentedPreferenceFragment) {
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
