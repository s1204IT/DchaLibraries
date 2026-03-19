package com.android.settings.gestures;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssistGestureSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.assist_gesture_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return AssistGestureSettings.buildPreferenceControllers(context, null);
        }

        @Override
        protected boolean isPageSearchEnabled(Context context) {
            AssistGestureSettingsPreferenceController assistGestureSettingsPreferenceController = new AssistGestureSettingsPreferenceController(context, "gesture_assist_input_summary");
            assistGestureSettingsPreferenceController.setAssistOnly(false);
            return assistGestureSettingsPreferenceController.isAvailable();
        }
    };

    @Override
    public int getMetricsCategory() {
        return 996;
    }

    @Override
    protected String getLogTag() {
        return "AssistGesture";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.assist_gesture_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(FeatureFactory.getFactory(context).getAssistGestureFeatureProvider().getControllers(context, lifecycle));
        return arrayList;
    }
}
