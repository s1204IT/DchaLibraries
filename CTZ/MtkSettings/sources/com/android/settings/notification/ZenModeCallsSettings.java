package com.android.settings.notification;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

public class ZenModeCallsSettings extends ZenModeSettingsBase implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.zen_mode_calls_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            return super.getNonIndexableKeys(context);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ZenModeCallsSettings.buildPreferenceControllers(context, null);
        }
    };

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ZenModeCallsPreferenceController(context, lifecycle));
        arrayList.add(new ZenModeStarredContactsPreferenceController(context, lifecycle, 8));
        arrayList.add(new ZenModeRepeatCallersPreferenceController(context, lifecycle, context.getResources().getInteger(android.R.integer.config_mediaRouter_builtInSpeakerSuitability)));
        arrayList.add(new ZenModeBehaviorFooterPreferenceController(context, lifecycle, R.string.zen_mode_calls_footer));
        return arrayList;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_calls_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 141;
    }
}
