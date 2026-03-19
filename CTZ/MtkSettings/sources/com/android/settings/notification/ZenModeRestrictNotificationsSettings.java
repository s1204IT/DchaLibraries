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

public class ZenModeRestrictNotificationsSettings extends ZenModeSettingsBase implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.zen_mode_restrict_notifications_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            return super.getNonIndexableKeys(context);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ZenModeRestrictNotificationsSettings.buildPreferenceControllers(context, null);
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ZenModeVisEffectsNonePreferenceController(context, lifecycle, "zen_mute_notifications"));
        arrayList.add(new ZenModeVisEffectsAllPreferenceController(context, lifecycle, "zen_hide_notifications"));
        arrayList.add(new ZenModeVisEffectsCustomPreferenceController(context, lifecycle, "zen_custom"));
        arrayList.add(new ZenFooterPreferenceController(context, lifecycle, "footer_preference"));
        return arrayList;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_restrict_notifications_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 1400;
    }
}
