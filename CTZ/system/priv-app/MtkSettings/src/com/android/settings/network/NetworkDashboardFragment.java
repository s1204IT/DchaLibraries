package com.android.settings.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.text.BidiFormatter;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.network.MobilePlanPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.wifi.WifiMasterSwitchPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.network.RcsePreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class NetworkDashboardFragment extends DashboardFragment implements MobilePlanPreferenceController.MobilePlanPreferenceHost {
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.network.NetworkDashboardFragment.1
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.network.NetworkDashboardFragment.2
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.network_and_internet;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return NetworkDashboardFragment.buildPreferenceControllers(context, null, null, null, null);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("toggle_wifi");
            return nonIndexableKeys;
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 746;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "NetworkDashboardFrag";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.network_and_internet;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        ((AirplaneModePreferenceController) use(AirplaneModePreferenceController.class)).setFragment(this);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        UtilsExt.getRCSSettingsExt(getActivity()).addRCSPreference(getActivity(), getPreferenceScreen());
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_network_dashboard;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), this.mMetricsFeatureProvider, this, this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle, MetricsFeatureProvider metricsFeatureProvider, Fragment fragment, MobilePlanPreferenceController.MobilePlanPreferenceHost mobilePlanPreferenceHost) {
        MobilePlanPreferenceController mobilePlanPreferenceController = new MobilePlanPreferenceController(context, mobilePlanPreferenceHost);
        WifiMasterSwitchPreferenceController wifiMasterSwitchPreferenceController = new WifiMasterSwitchPreferenceController(context, metricsFeatureProvider);
        MobileNetworkPreferenceController mobileNetworkPreferenceController = new MobileNetworkPreferenceController(context);
        VpnPreferenceController vpnPreferenceController = new VpnPreferenceController(context);
        PrivateDnsPreferenceController privateDnsPreferenceController = new PrivateDnsPreferenceController(context);
        RcsePreferenceController rcsePreferenceController = new RcsePreferenceController(context);
        if (lifecycle != null) {
            lifecycle.addObserver(mobilePlanPreferenceController);
            lifecycle.addObserver(wifiMasterSwitchPreferenceController);
            lifecycle.addObserver(mobileNetworkPreferenceController);
            lifecycle.addObserver(vpnPreferenceController);
            lifecycle.addObserver(privateDnsPreferenceController);
            lifecycle.addObserver(rcsePreferenceController);
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(mobileNetworkPreferenceController);
        arrayList.add(new TetherPreferenceController(context, lifecycle));
        arrayList.add(vpnPreferenceController);
        arrayList.add(new ProxyPreferenceController(context));
        arrayList.add(mobilePlanPreferenceController);
        arrayList.add(wifiMasterSwitchPreferenceController);
        arrayList.add(privateDnsPreferenceController);
        arrayList.add(rcsePreferenceController);
        return arrayList;
    }

    @Override // com.android.settings.network.MobilePlanPreferenceController.MobilePlanPreferenceHost
    public void showMobilePlanMessageDialog() {
        showDialog(1);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public Dialog onCreateDialog(int i) {
        Log.d("NetworkDashboardFrag", "onCreateDialog: dialogId=" + i);
        if (i == 1) {
            final MobilePlanPreferenceController mobilePlanPreferenceController = (MobilePlanPreferenceController) use(MobilePlanPreferenceController.class);
            return new AlertDialog.Builder(getActivity()).setMessage(mobilePlanPreferenceController.getMobilePlanDialogMessage()).setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.network.-$$Lambda$NetworkDashboardFragment$ezC2Ol_SOf4CDiS8HjkkdWzGu_s
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i2) {
                    mobilePlanPreferenceController.setMobilePlanDialogMessage(null);
                }
            }).create();
        }
        return super.onCreateDialog(i);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public int getDialogMetricsCategory(int i) {
        if (1 == i) {
            return 609;
        }
        return 0;
    }

    static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final MobileNetworkPreferenceController mMobileNetworkPreferenceController;
        private final SummaryLoader mSummaryLoader;
        private final TetherPreferenceController mTetherPreferenceController;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this(context, summaryLoader, new MobileNetworkPreferenceController(context), new TetherPreferenceController(context, null));
        }

        SummaryProvider(Context context, SummaryLoader summaryLoader, MobileNetworkPreferenceController mobileNetworkPreferenceController, TetherPreferenceController tetherPreferenceController) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
            this.mMobileNetworkPreferenceController = mobileNetworkPreferenceController;
            this.mTetherPreferenceController = tetherPreferenceController;
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            if (z) {
                String strUnicodeWrap = BidiFormatter.getInstance().unicodeWrap(this.mContext.getString(R.string.wifi_settings_title));
                if (this.mMobileNetworkPreferenceController.isAvailable()) {
                    strUnicodeWrap = this.mContext.getString(R.string.join_many_items_middle, strUnicodeWrap, this.mContext.getString(R.string.network_dashboard_summary_mobile));
                }
                String string = this.mContext.getString(R.string.join_many_items_middle, strUnicodeWrap, this.mContext.getString(R.string.network_dashboard_summary_data_usage));
                if (this.mTetherPreferenceController.isAvailable()) {
                    string = this.mContext.getString(R.string.join_many_items_middle, string, this.mContext.getString(R.string.network_dashboard_summary_hotspot));
                }
                this.mSummaryLoader.setSummary(this, string);
            }
        }
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        if (((AirplaneModePreferenceController) use(AirplaneModePreferenceController.class)).onActivityResult(i, i2, intent)) {
            return;
        }
        super.onActivityResult(i, i2, intent);
    }
}
