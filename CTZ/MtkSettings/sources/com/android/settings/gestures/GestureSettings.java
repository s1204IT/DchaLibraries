package com.android.settings.gestures;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.Arrays;
import java.util.List;

public class GestureSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.gestures;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("gesture_assist_input_summary");
            nonIndexableKeys.add("gesture_swipe_down_fingerprint_input_summary");
            nonIndexableKeys.add("gesture_double_tap_power_input_summary");
            nonIndexableKeys.add("gesture_double_twist_input_summary");
            nonIndexableKeys.add("gesture_swipe_up_input_summary");
            nonIndexableKeys.add("gesture_double_tap_screen_input_summary");
            nonIndexableKeys.add("gesture_pick_up_input_summary");
            nonIndexableKeys.add("gesture_prevent_ringing_summary");
            return nonIndexableKeys;
        }
    };
    private AmbientDisplayConfiguration mAmbientDisplayConfig;

    @Override
    public int getMetricsCategory() {
        return 459;
    }

    @Override
    protected String getLogTag() {
        return "GestureSettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.gestures;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((AssistGestureSettingsPreferenceController) use(AssistGestureSettingsPreferenceController.class)).setAssistOnly(false);
        ((PickupGesturePreferenceController) use(PickupGesturePreferenceController.class)).setConfig(getConfig(context));
        ((DoubleTapScreenPreferenceController) use(DoubleTapScreenPreferenceController.class)).setConfig(getConfig(context));
    }

    private AmbientDisplayConfiguration getConfig(Context context) {
        if (this.mAmbientDisplayConfig == null) {
            this.mAmbientDisplayConfig = new AmbientDisplayConfiguration(context);
        }
        return this.mAmbientDisplayConfig;
    }
}
