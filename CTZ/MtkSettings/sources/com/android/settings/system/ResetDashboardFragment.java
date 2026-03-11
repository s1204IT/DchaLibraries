package com.android.settings.system;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.applications.manageapplications.ResetAppPrefPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.NetworkResetPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.mediatek.settings.system.DrmResetPreferenceController;
import java.util.ArrayList;
import java.util.List;

public class ResetDashboardFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.reset_dashboard_fragment;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ResetDashboardFragment.buildPreferenceControllers(context, null);
        }
    };

    @Override
    public int getMetricsCategory() {
        return 924;
    }

    @Override
    protected String getLogTag() {
        return "ResetDashboardFragment";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.reset_dashboard_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new NetworkResetPreferenceController(context));
        arrayList.add(new FactoryResetPreferenceController(context));
        arrayList.add(new ResetAppPrefPreferenceController(context, lifecycle));
        arrayList.add(new DrmResetPreferenceController(context));
        return arrayList;
    }
}
