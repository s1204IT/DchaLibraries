package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.provider.SearchIndexableResource;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionPreferenceController;
import com.android.settings.deviceinfo.imei.ImeiInfoPreferenceController;
import com.android.settings.deviceinfo.simstatus.SimStatusPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.system.AdditionalSystemUpdatePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceInfoSettings extends DashboardFragment implements Indexable {
    static final int NON_SIM_PREFERENCES_COUNT = 2;
    static final int SIM_PREFERENCES_COUNT = 3;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.device_info_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return DeviceInfoSettings.buildPreferenceControllers(context, null, null, null);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("legal_container");
            return nonIndexableKeys;
        }
    };

    @Override
    public int getMetricsCategory() {
        return 40;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public int getInitialExpandedChildCount() {
        return Math.max(3, ((TelephonyManager) getContext().getSystemService("phone")).getPhoneCount() * 3) + 2;
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (((BuildNumberPreferenceController) use(BuildNumberPreferenceController.class)).onActivityResult(i, i2, intent)) {
            return;
        }
        super.onActivityResult(i, i2, intent);
    }

    @Override
    protected String getLogTag() {
        return "DeviceInfoSettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_info_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getActivity(), this, getLifecycle());
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(SummaryLoader summaryLoader) {
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean z) {
            if (z) {
                this.mSummaryLoader.setSummary(this, DeviceModelPreferenceController.getDeviceModel());
            }
        }
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Activity activity, Fragment fragment, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new PhoneNumberPreferenceController(context, lifecycle));
        arrayList.add(new SimStatusPreferenceController(context, fragment, lifecycle));
        arrayList.add(new AdditionalSystemUpdatePreferenceController(context));
        arrayList.add(new DeviceModelPreferenceController(context, fragment));
        arrayList.add(new ImeiInfoPreferenceController(context, fragment, lifecycle));
        arrayList.add(new FirmwareVersionPreferenceController(context, fragment));
        arrayList.add(new IpAddressPreferenceController(context, lifecycle));
        arrayList.add(new WifiMacAddressPreferenceController(context, lifecycle));
        arrayList.add(new BluetoothAddressPreferenceController(context, lifecycle));
        arrayList.add(new RegulatoryInfoPreferenceController(context));
        arrayList.add(new SafetyInfoPreferenceController(context));
        arrayList.add(new ManualPreferenceController(context));
        arrayList.add(new FeedbackPreferenceController(fragment, context));
        arrayList.add(new FccEquipmentIdPreferenceController(context));
        arrayList.add(new BuildNumberPreferenceController(context, activity, fragment, lifecycle));
        return arrayList;
    }
}
