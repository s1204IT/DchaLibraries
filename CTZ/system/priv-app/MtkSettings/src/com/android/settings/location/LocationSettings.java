package com.android.settings.location;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class LocationSettings extends DashboardFragment {
    private static ISettingsMiscExt mExt;
    private LocationSwitchBarController mSwitchBarController;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.location.LocationSettings.2
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.location.LocationSettings.3
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.location_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return LocationSettings.buildPreferenceControllers(context, null, null);
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 63;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        Log.i("LocationSettings", "onActivityCreatedd");
        super.onActivityCreated(bundle);
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        SwitchBar switchBar = settingsActivity.getSwitchBar();
        switchBar.setSwitchBarText(R.string.location_settings_master_switch_title, R.string.location_settings_master_switch_title);
        this.mSwitchBarController = new LocationSwitchBarController(settingsActivity, switchBar, getLifecycle());
        switchBar.show();
        mExt.customizeAGPRS(getPreferenceScreen());
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.location_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "LocationSettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this, getLifecycle());
    }

    static void addPreferencesSorted(List<Preference> list, PreferenceGroup preferenceGroup) {
        Collections.sort(list, new Comparator<Preference>() { // from class: com.android.settings.location.LocationSettings.1
            /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
            @Override // java.util.Comparator
            public int compare(Preference preference, Preference preference2) {
                return preference.getTitle().toString().compareTo(preference2.getTitle().toString());
            }
        });
        Iterator<Preference> it = list.iterator();
        while (it.hasNext()) {
            preferenceGroup.addPreference(it.next());
        }
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, LocationSettings locationSettings, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new AppLocationPermissionPreferenceController(context));
        arrayList.add(new LocationForWorkPreferenceController(context, lifecycle));
        arrayList.add(new RecentLocationRequestPreferenceController(context, locationSettings, lifecycle));
        arrayList.add(new LocationScanningPreferenceController(context));
        arrayList.add(new LocationServicePreferenceController(context, locationSettings, lifecycle));
        arrayList.add(new LocationFooterPreferenceController(context, lifecycle));
        Log.i("LocationSettings", "add addPreferenceControllerdd");
        mExt = UtilsExt.getMiscPlugin(context);
        mExt.addPreferenceController(arrayList, mExt.createPreferenceController(context, lifecycle));
        return arrayList;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            if (z) {
                this.mSummaryLoader.setSummary(this, LocationPreferenceController.getLocationSummary(this.mContext));
            }
        }
    }
}
