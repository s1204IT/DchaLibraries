package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class PowerUsageAdvanced extends PowerUsageBase {
    static final int MENU_TOGGLE_APPS = 2;
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.fuelgauge.PowerUsageAdvanced.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.power_usage_advanced;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            ArrayList arrayList = new ArrayList();
            arrayList.add(new BatteryAppListPreferenceController(context, "app_list", null, null, null));
            return arrayList;
        }
    };
    private BatteryAppListPreferenceController mBatteryAppListPreferenceController;
    private BatteryUtils mBatteryUtils;
    BatteryHistoryPreference mHistPref;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    boolean mShowAllApps = false;

    @Override // com.android.settings.fuelgauge.PowerUsageBase, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Context context = getContext();
        this.mHistPref = (BatteryHistoryPreference) findPreference("battery_graph");
        this.mPowerUsageFeatureProvider = FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
        this.mBatteryUtils = BatteryUtils.getInstance(context);
        updateHistPrefSummary(context);
        restoreSavedInstance(bundle);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 51;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "AdvancedBatteryUsage";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_advanced;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 2, 0, this.mShowAllApps ? R.string.hide_extra_apps : R.string.show_all_apps);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 2) {
            this.mShowAllApps = !this.mShowAllApps;
            menuItem.setTitle(this.mShowAllApps ? R.string.hide_extra_apps : R.string.show_all_apps);
            this.mMetricsFeatureProvider.action(getContext(), 852, this.mShowAllApps);
            restartBatteryStatsLoader(0);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    void restoreSavedInstance(Bundle bundle) {
        if (bundle != null) {
            this.mShowAllApps = bundle.getBoolean("show_all_apps", false);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("show_all_apps", this.mShowAllApps);
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mBatteryAppListPreferenceController = new BatteryAppListPreferenceController(context, "app_list", getLifecycle(), (SettingsActivity) getActivity(), this);
        arrayList.add(this.mBatteryAppListPreferenceController);
        return arrayList;
    }

    @Override // com.android.settings.fuelgauge.PowerUsageBase
    protected void refreshUi(int i) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        updatePreference(this.mHistPref);
        updateHistPrefSummary(context);
        this.mBatteryAppListPreferenceController.refreshAppListGroup(this.mStatsHelper, this.mShowAllApps);
    }

    private void updateHistPrefSummary(Context context) {
        boolean z = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("plugged", -1) != 0;
        if (this.mPowerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(context) && !z) {
            this.mHistPref.setBottomSummary(this.mPowerUsageFeatureProvider.getAdvancedUsageScreenInfoString());
        } else {
            this.mHistPref.hideBottomSummary();
        }
    }
}
