package com.android.settings.applications.assist;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.gestures.AssistGestureSettingsPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageAssist extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.manage_assist;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ManageAssist.buildPreferenceControllers(context, null);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("gesture_assist_application");
            return nonIndexableKeys;
        }
    };

    @Override
    protected String getLogTag() {
        return "ManageAssist";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.manage_assist;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    public int getMetricsCategory() {
        return 201;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((AssistGestureSettingsPreferenceController) use(AssistGestureSettingsPreferenceController.class)).setAssistOnly(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.assist_footer);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new DefaultAssistPreferenceController(context, "default_assist", true));
        arrayList.add(new AssistContextPreferenceController(context, lifecycle));
        arrayList.add(new AssistScreenshotPreferenceController(context, lifecycle));
        arrayList.add(new AssistFlashScreenPreferenceController(context, lifecycle));
        arrayList.add(new DefaultVoiceInputPreferenceController(context, lifecycle));
        return arrayList;
    }
}
