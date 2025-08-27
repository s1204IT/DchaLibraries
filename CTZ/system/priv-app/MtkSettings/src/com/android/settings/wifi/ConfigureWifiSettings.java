package com.android.settings.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.wifi.p2p.WifiP2pPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.settings.wifi.WapiCertPreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class ConfigureWifiSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.wifi.ConfigureWifiSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.wifi_configure_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
            if (activeNetworkInfo == null || activeNetworkInfo.getType() == 1) {
                nonIndexableKeys.add("current_ip_address");
            }
            return nonIndexableKeys;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        protected boolean isPageSearchEnabled(Context context) {
            return context.getResources().getBoolean(R.bool.config_show_wifi_settings);
        }
    };
    private IWifiExt iWifiExt;
    private UseOpenWifiPreferenceController mUseOpenWifiPreferenceController;
    private WifiWakeupPreferenceController mWifiWakeupPreferenceController;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 338;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "ConfigureWifiSettings";
    }

    @Override // com.android.settings.SettingsPreferenceFragment
    public int getInitialExpandedChildCount() {
        if (this.mUseOpenWifiPreferenceController.isAvailable()) {
            return 3;
        }
        return 2;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_configure_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        this.mWifiWakeupPreferenceController = new WifiWakeupPreferenceController(context, this);
        this.mUseOpenWifiPreferenceController = new UseOpenWifiPreferenceController(context, this, getLifecycle());
        WifiManager wifiManager = (WifiManager) getSystemService("wifi");
        ArrayList arrayList = new ArrayList();
        this.iWifiExt = UtilsExt.getWifiExt(context);
        arrayList.add(this.mWifiWakeupPreferenceController);
        arrayList.add(new NotifyOpenNetworksPreferenceController(context, getLifecycle()));
        arrayList.add(this.mUseOpenWifiPreferenceController);
        arrayList.add(new WifiInfoPreferenceController(context, getLifecycle(), wifiManager));
        arrayList.add(new CellularFallbackPreferenceController(context));
        arrayList.add(new WifiP2pPreferenceController(context, getLifecycle(), wifiManager));
        this.iWifiExt.addPreferenceController(arrayList, this.iWifiExt.createWifiPreferenceController(context, getLifecycle()));
        arrayList.add(new WapiCertPreferenceController(context));
        return arrayList;
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 600 && this.mWifiWakeupPreferenceController != null) {
            this.mWifiWakeupPreferenceController.onActivityResult(i, i2);
        } else if (i == 400 && this.mUseOpenWifiPreferenceController != null) {
            this.mUseOpenWifiPreferenceController.onActivityResult(i, i2);
        } else {
            super.onActivityResult(i, i2, intent);
        }
    }
}
