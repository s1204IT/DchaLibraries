package com.android.settings.connecteddevice;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.bluetooth.BluetoothFilesPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.nfc.AndroidBeamPreferenceController;
import com.android.settings.print.PrintSettingPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class AdvancedConnectedDeviceDashboardFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.connecteddevice.AdvancedConnectedDeviceDashboardFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.connected_devices_advanced;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            if (!context.getPackageManager().hasSystemFeature("android.hardware.nfc")) {
                nonIndexableKeys.add(AndroidBeamPreferenceController.KEY_ANDROID_BEAM_SETTINGS);
            }
            nonIndexableKeys.add("bluetooth_settings");
            return nonIndexableKeys;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return AdvancedConnectedDeviceDashboardFragment.buildControllers(context, null);
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1264;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "AdvancedConnectedDeviceFrag";
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_connected_devices;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.connected_devices_advanced;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new BluetoothFilesPreferenceController(context));
        arrayList.add(new BluetoothOnWhileDrivingPreferenceController(context));
        PrintSettingPreferenceController printSettingPreferenceController = new PrintSettingPreferenceController(context);
        if (lifecycle != null) {
            lifecycle.addObserver(printSettingPreferenceController);
        }
        arrayList.add(printSettingPreferenceController);
        return arrayList;
    }
}
