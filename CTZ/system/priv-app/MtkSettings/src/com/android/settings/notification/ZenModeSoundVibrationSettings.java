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

/* loaded from: classes.dex */
public class ZenModeSoundVibrationSettings extends ZenModeSettingsBase implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.notification.ZenModeSoundVibrationSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.zen_mode_sound_vibration_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            return super.getNonIndexableKeys(context);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ZenModeSoundVibrationSettings.buildPreferenceControllers(context, null);
        }
    };

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ZenModeAlarmsPreferenceController(context, lifecycle));
        arrayList.add(new ZenModeMediaPreferenceController(context, lifecycle));
        arrayList.add(new ZenModeSystemPreferenceController(context, lifecycle));
        arrayList.add(new ZenModeBehaviorFooterPreferenceController(context, lifecycle, R.string.zen_sound_footer));
        return arrayList;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_sound_vibration_settings;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 141;
    }
}
