package com.android.settings.accessibility;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class VibrationSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.accessibility.VibrationSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.accessibility_vibration_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1292;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_vibration_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "VibrationSettings";
    }
}
