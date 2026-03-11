package com.android.settings.accessibility;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;

public class VibrationSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.accessibility_vibration_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }
    };

    @Override
    public int getMetricsCategory() {
        return 1292;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_vibration_settings;
    }

    @Override
    protected String getLogTag() {
        return "VibrationSettings";
    }
}
