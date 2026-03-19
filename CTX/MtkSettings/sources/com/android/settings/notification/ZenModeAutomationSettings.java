package com.android.settings.notification;

import android.app.Fragment;
import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.utils.ZenServiceListing;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

public class ZenModeAutomationSettings extends ZenModeSettingsBase {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.zen_mode_automation_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("zen_mode_add_automatic_rule");
            nonIndexableKeys.add("zen_mode_automatic_rules");
            return nonIndexableKeys;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ZenModeAutomationSettings.buildPreferenceControllers(context, null, null, null);
        }
    };
    protected final ManagedServiceSettings.Config CONFIG = getConditionProviderConfig();

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ZenServiceListing zenServiceListing = new ZenServiceListing(getContext(), this.CONFIG);
        zenServiceListing.reloadApprovedServices();
        return buildPreferenceControllers(context, this, zenServiceListing, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Fragment fragment, ZenServiceListing zenServiceListing, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ZenModeAddAutomaticRulePreferenceController(context, fragment, zenServiceListing, lifecycle));
        arrayList.add(new ZenModeAutomaticRulesPreferenceController(context, fragment, lifecycle));
        return arrayList;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_automation_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 142;
    }

    protected static ManagedServiceSettings.Config getConditionProviderConfig() {
        return new ManagedServiceSettings.Config.Builder().setTag("ZenModeSettings").setIntentAction("android.service.notification.ConditionProviderService").setPermission("android.permission.BIND_CONDITION_PROVIDER_SERVICE").setNoun("condition provider").build();
    }
}
