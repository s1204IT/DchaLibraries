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

/* loaded from: classes.dex */
public class ZenModeRestrictNotificationsSettings extends ZenModeSettingsBase implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.notification.ZenModeRestrictNotificationsSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.zen_mode_restrict_notifications_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            return super.getNonIndexableKeys(context);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ZenModeRestrictNotificationsSettings.buildPreferenceControllers(context, null);
        }
    };

    @Override // com.android.settings.notification.ZenModeSettingsBase, com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
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

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_restrict_notifications_settings;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1400;
    }
}
