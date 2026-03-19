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

public class AdvancedConnectedDeviceDashboardFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.connected_devices_advanced;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            if (!context.getPackageManager().hasSystemFeature("android.hardware.nfc")) {
                nonIndexableKeys.add(AndroidBeamPreferenceController.KEY_ANDROID_BEAM_SETTINGS);
            }
            nonIndexableKeys.add("bluetooth_settings");
            return nonIndexableKeys;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return AdvancedConnectedDeviceDashboardFragment.buildControllers(context, null);
        }
    };

    @Override
    public int getMetricsCategory() {
        return 1264;
    }

    @Override
    protected String getLogTag() {
        return "AdvancedConnectedDeviceFrag";
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_connected_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.connected_devices_advanced;
    }

    @Override
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
