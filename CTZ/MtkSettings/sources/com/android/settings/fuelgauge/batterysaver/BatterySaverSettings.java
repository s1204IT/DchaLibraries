package com.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatterySaverSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.battery_saver_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return BatterySaverSettings.buildPreferenceControllers(context, null);
        }
    };

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    @Override
    public int getMetricsCategory() {
        return 52;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.battery_saver_settings;
    }

    @Override
    protected String getLogTag() {
        return "BatterySaverSettings";
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_battery_saver_settings;
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new AutoBatterySaverPreferenceController(context));
        arrayList.add(new AutoBatterySeekBarPreferenceController(context, lifecycle));
        return arrayList;
    }
}
