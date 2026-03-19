package com.android.settings.location;

import android.content.Context;
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

public class RecentLocationRequestSeeAllFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.location_recent_requests_see_all;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
            return RecentLocationRequestSeeAllFragment.buildPreferenceControllers(context, null, null);
        }
    };

    @Override
    public int getMetricsCategory() {
        return 1325;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_recent_requests_see_all;
    }

    @Override
    protected String getLogTag() {
        return "RecentLocationReqAll";
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle, RecentLocationRequestSeeAllFragment recentLocationRequestSeeAllFragment) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new RecentLocationRequestSeeAllPreferenceController(context, lifecycle, recentLocationRequestSeeAllFragment));
        return arrayList;
    }
}
