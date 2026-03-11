package com.android.settings.gestures;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.Arrays;
import java.util.List;

public class PickupGestureSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.pick_up_gesture_settings;
            return Arrays.asList(searchIndexableResource);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FeatureFactory.getFactory(context).getSuggestionFeatureProvider(context).getSharedPrefs(context).edit().putBoolean("pref_pickup_gesture_suggestion_complete", true).apply();
        ((PickupGesturePreferenceController) use(PickupGesturePreferenceController.class)).setConfig(new AmbientDisplayConfiguration(context));
    }

    @Override
    public int getMetricsCategory() {
        return 753;
    }

    @Override
    protected String getLogTag() {
        return "PickupGestureSettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.pick_up_gesture_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_pickup_gesture;
    }
}
