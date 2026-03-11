package com.android.settings.gestures;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.Arrays;
import java.util.List;

public class PreventRingingGestureSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.prevent_ringing_gesture_settings;
            return Arrays.asList(searchIndexableResource);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public int getMetricsCategory() {
        return 1360;
    }

    @Override
    protected String getLogTag() {
        return "RingingGestureSettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.prevent_ringing_gesture_settings;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }
}
