package com.android.settings.deviceinfo.aboutphone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.view.View;
import com.android.settings.R;
import com.android.settings.accounts.EmergencyInfoPreferenceController;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.bluetooth.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.deviceinfo.BluetoothAddressPreferenceController;
import com.android.settings.deviceinfo.BrandedAccountPreferenceController;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.deviceinfo.DeviceModelPreferenceController;
import com.android.settings.deviceinfo.DeviceNamePreferenceController;
import com.android.settings.deviceinfo.FccEquipmentIdPreferenceController;
import com.android.settings.deviceinfo.FeedbackPreferenceController;
import com.android.settings.deviceinfo.IpAddressPreferenceController;
import com.android.settings.deviceinfo.ManualPreferenceController;
import com.android.settings.deviceinfo.PhoneNumberPreferenceController;
import com.android.settings.deviceinfo.RegulatoryInfoPreferenceController;
import com.android.settings.deviceinfo.SafetyInfoPreferenceController;
import com.android.settings.deviceinfo.WifiMacAddressPreferenceController;
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionPreferenceController;
import com.android.settings.deviceinfo.imei.ImeiInfoPreferenceController;
import com.android.settings.deviceinfo.simstatus.SimStatusPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.mediatek.settings.deviceinfo.BasebandVersion2PreferenceController;
import com.mediatek.settings.deviceinfo.CustomizeBuildVersionPreferenceController;
import com.mediatek.settings.deviceinfo.CustomizeSystemUpdatePreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class MyDeviceInfoFragment extends DashboardFragment implements DeviceNamePreferenceController.DeviceNamePreferenceHost {
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.deviceinfo.aboutphone.-$$Lambda$MyDeviceInfoFragment$pzCelMuIMGm16asu34w_Ge8IYsk
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public final SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return MyDeviceInfoFragment.lambda$static$0(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.my_device_info;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return MyDeviceInfoFragment.buildPreferenceControllers(context, null, null, null);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("legal_container");
            return nonIndexableKeys;
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 40;
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        initHeader();
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "MyDeviceInfoFragment";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.my_device_info;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getActivity(), this, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Activity activity, MyDeviceInfoFragment myDeviceInfoFragment, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new EmergencyInfoPreferenceController(context));
        arrayList.add(new PhoneNumberPreferenceController(context, lifecycle));
        arrayList.add(new BrandedAccountPreferenceController(context));
        DeviceNamePreferenceController deviceNamePreferenceController = new DeviceNamePreferenceController(context);
        deviceNamePreferenceController.setLocalBluetoothManager(Utils.getLocalBtManager(context));
        deviceNamePreferenceController.setHost(myDeviceInfoFragment);
        if (lifecycle != null) {
            lifecycle.addObserver(deviceNamePreferenceController);
        }
        arrayList.add(deviceNamePreferenceController);
        arrayList.add(new SimStatusPreferenceController(context, myDeviceInfoFragment, lifecycle));
        arrayList.add(new DeviceModelPreferenceController(context, myDeviceInfoFragment));
        arrayList.add(new ImeiInfoPreferenceController(context, myDeviceInfoFragment, lifecycle));
        arrayList.add(new FirmwareVersionPreferenceController(context, myDeviceInfoFragment));
        arrayList.add(new IpAddressPreferenceController(context, lifecycle));
        arrayList.add(new WifiMacAddressPreferenceController(context, lifecycle));
        arrayList.add(new BluetoothAddressPreferenceController(context, lifecycle));
        arrayList.add(new RegulatoryInfoPreferenceController(context));
        arrayList.add(new SafetyInfoPreferenceController(context));
        arrayList.add(new ManualPreferenceController(context));
        arrayList.add(new FeedbackPreferenceController(myDeviceInfoFragment, context));
        arrayList.add(new FccEquipmentIdPreferenceController(context));
        arrayList.add(new BuildNumberPreferenceController(context, activity, myDeviceInfoFragment, lifecycle));
        arrayList.add(new BasebandVersion2PreferenceController(context));
        arrayList.add(new CustomizeBuildVersionPreferenceController(context));
        arrayList.add(new CustomizeSystemUpdatePreferenceController(context, UserManager.get(context)));
        return arrayList;
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        if (((BuildNumberPreferenceController) use(BuildNumberPreferenceController.class)).onActivityResult(i, i2, intent)) {
            return;
        }
        super.onActivityResult(i, i2, intent);
    }

    private void initHeader() {
        View viewFindViewById = ((LayoutPreference) getPreferenceScreen().findPreference("my_device_info_header")).findViewById(R.id.entity_header);
        Activity activity = getActivity();
        Bundle arguments = getArguments();
        EntityHeaderController buttonActions = EntityHeaderController.newInstance(activity, this, viewFindViewById).setRecyclerView(getListView(), getLifecycle()).setButtonActions(0, 0);
        if (arguments.getInt("icon_id", 0) == 0) {
            UserManager userManager = (UserManager) getActivity().getSystemService("user");
            UserInfo existingUser = com.android.settings.Utils.getExistingUser(userManager, Process.myUserHandle());
            buttonActions.setLabel(existingUser.name);
            buttonActions.setIcon(com.android.settingslib.Utils.getUserIcon(getActivity(), userManager, existingUser));
        }
        buttonActions.done(activity, true);
    }

    @Override // com.android.settings.deviceinfo.DeviceNamePreferenceController.DeviceNamePreferenceHost
    public void showDeviceNameWarningDialog(String str) {
        DeviceNameWarningDialog.show(this);
    }

    public void onSetDeviceNameConfirm() {
        ((DeviceNamePreferenceController) use(DeviceNamePreferenceController.class)).confirmDeviceName();
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(SummaryLoader summaryLoader) {
            this.mSummaryLoader = summaryLoader;
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            if (z) {
                this.mSummaryLoader.setSummary(this, DeviceModelPreferenceController.getDeviceModel());
            }
        }
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: 0x0002: CONSTRUCTOR (r1v0 com.android.settings.dashboard.SummaryLoader) A[MD:(com.android.settings.dashboard.SummaryLoader):void (m)] (LINE:211) call: com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment.SummaryProvider.<init>(com.android.settings.dashboard.SummaryLoader):void type: CONSTRUCTOR */
    static /* synthetic */ SummaryLoader.SummaryProvider lambda$static$0(Activity activity, SummaryLoader summaryLoader) {
        return new SummaryProvider(summaryLoader);
    }
}
