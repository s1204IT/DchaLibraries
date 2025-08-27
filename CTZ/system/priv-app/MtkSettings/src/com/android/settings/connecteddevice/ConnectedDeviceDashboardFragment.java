package com.android.settings.connecteddevice;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class ConnectedDeviceDashboardFragment extends DashboardFragment {
    static final String KEY_AVAILABLE_DEVICES = "available_device_list";
    static final String KEY_CONNECTED_DEVICES = "connected_device_list";
    private static final boolean DEBUG = Log.isLoggable("ConnectedDeviceFrag", 3);
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment.1
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment.2
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.connected_devices;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ConnectedDeviceDashboardFragment.buildPreferenceControllers(context, null);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add(ConnectedDeviceDashboardFragment.KEY_AVAILABLE_DEVICES);
            nonIndexableKeys.add(ConnectedDeviceDashboardFragment.KEY_CONNECTED_DEVICES);
            return nonIndexableKeys;
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 747;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "ConnectedDeviceFrag";
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_connected_devices;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.connected_devices;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        DiscoverableFooterPreferenceController discoverableFooterPreferenceController = new DiscoverableFooterPreferenceController(context);
        arrayList.add(discoverableFooterPreferenceController);
        if (lifecycle != null) {
            lifecycle.addObserver(discoverableFooterPreferenceController);
        }
        return arrayList;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        String callingAppPackageName = getCallingAppPackageName(getActivity().getActivityToken());
        if (DEBUG) {
            Log.d("ConnectedDeviceFrag", "onAttach() calling package name is : " + callingAppPackageName);
        }
        ((AvailableMediaDeviceGroupController) use(AvailableMediaDeviceGroupController.class)).init(this);
        ((ConnectedDeviceGroupController) use(ConnectedDeviceGroupController.class)).init(this);
        ((PreviouslyConnectedDevicePreferenceController) use(PreviouslyConnectedDevicePreferenceController.class)).init(this);
        ((DiscoverableFooterPreferenceController) use(DiscoverableFooterPreferenceController.class)).init(this, TextUtils.equals("com.android.settings", callingAppPackageName) || TextUtils.equals("com.android.systemui", callingAppPackageName));
    }

    private String getCallingAppPackageName(IBinder iBinder) {
        try {
            return ActivityManager.getService().getLaunchedFromPackage(iBinder);
        } catch (RemoteException e) {
            Log.v("ConnectedDeviceFrag", "Could not talk to activity manager.", e);
            return null;
        }
    }

    static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            if (z) {
                this.mSummaryLoader.setSummary(this, this.mContext.getText(AdvancedConnectedDeviceController.getConnectedDevicesSummaryResourceId(this.mContext)));
            }
        }
    }
}
