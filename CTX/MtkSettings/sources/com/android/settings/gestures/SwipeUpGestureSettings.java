package com.android.settings.gestures;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.Arrays;
import java.util.List;

public class SwipeUpGestureSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.swipe_up_gesture_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        protected boolean isPageSearchEnabled(Context context) {
            return SwipeUpPreferenceController.isGestureAvailable(context);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FeatureFactory.getFactory(context).getSuggestionFeatureProvider(context).getSharedPrefs(context).edit().putBoolean("pref_swipe_up_suggestion_complete", true).apply();
    }

    @Override
    public int getMetricsCategory() {
        return 1374;
    }

    @Override
    protected String getLogTag() {
        return "SwipeUpGesture";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.swipe_up_gesture_settings;
    }
}
