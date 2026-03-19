package com.android.settings.notification;

import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

public class ZenModeBlockedEffectsSettings extends ZenModeSettingsBase implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.zen_mode_block_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            return super.getNonIndexableKeys(context);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ZenModeBlockedEffectsSettings.buildPreferenceControllers(context, null);
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.zen_mode_blocked_effects_footer);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ZenModeVisEffectPreferenceController(context, lifecycle, "zen_effect_intent", 4, 1332, null));
        arrayList.add(new ZenModeVisEffectPreferenceController(context, lifecycle, "zen_effect_light", 8, 1333, null));
        arrayList.add(new ZenModeVisEffectPreferenceController(context, lifecycle, "zen_effect_peek", 16, 1334, null));
        arrayList.add(new ZenModeVisEffectPreferenceController(context, lifecycle, "zen_effect_status", 32, 1335, new int[]{256}));
        arrayList.add(new ZenModeVisEffectPreferenceController(context, lifecycle, "zen_effect_badge", 64, 1336, null));
        arrayList.add(new ZenModeVisEffectPreferenceController(context, lifecycle, "zen_effect_ambient", 128, 1337, null));
        arrayList.add(new ZenModeVisEffectPreferenceController(context, lifecycle, "zen_effect_list", 256, 1338, null));
        return arrayList;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_block_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 1339;
    }
}
