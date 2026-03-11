package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.ims.ImsManager;
import com.android.settings.nfc.NfcEnabler;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.PhoneConfigurationSettings;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IRCSSettings;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.IWWOPJoynSettingsExt;
import com.mediatek.settings.ext.IWfcSettingsExt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WirelessSettings extends SettingsPreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.wireless_settings;
            return Arrays.asList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList<>();
            UserManager um = (UserManager) context.getSystemService("user");
            boolean isSecondaryUser = !um.isAdminUser();
            boolean isWimaxEnabled = isSecondaryUser ? false : context.getResources().getBoolean(android.R.^attr-private.iconfactoryBadgeSize);
            if (!isWimaxEnabled) {
                result.add("wimax_settings");
            }
            if (isSecondaryUser) {
                result.add("vpn_settings");
            }
            NfcManager manager = (NfcManager) context.getSystemService("nfc");
            if (manager != null) {
                NfcAdapter adapter = manager.getDefaultAdapter();
                if (adapter == null) {
                    result.add("toggle_nfc");
                    result.add("android_beam_settings");
                    result.add("toggle_mtk_nfc");
                } else if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
                    result.add("toggle_nfc");
                    result.add("android_beam_settings");
                } else {
                    result.add("toggle_mtk_nfc");
                }
            }
            if (isSecondaryUser || Utils.isWifiOnly(context)) {
                result.add("mobile_network_settings");
                result.add("manage_mobile_plan");
            }
            boolean isMobilePlanEnabled = context.getResources().getBoolean(R.bool.config_show_mobile_plan);
            if (!isMobilePlanEnabled) {
                result.add("manage_mobile_plan");
            }
            PackageManager pm = context.getPackageManager();
            if (pm.hasSystemFeature("android.hardware.type.television")) {
                result.add("toggle_airplane");
            }
            result.add("proxy_settings");
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
            if (isSecondaryUser || !cm.isTetheringSupported()) {
                result.add("tether_settings");
            }
            if (!ImsManager.isWfcEnabledByPlatform(context)) {
                result.add("wifi_calling_settings");
            }
            IWWOPJoynSettingsExt joynExt = UtilsExt.getJoynSettingsPlugin(context);
            if (!joynExt.isJoynSettingsEnabled()) {
                result.add("rcse_settings");
            }
            return result;
        }
    };
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;
    private PhoneConfigurationSettings mButtonPreferredPhoneConfiguration;
    private PreferenceScreen mButtonWfc;
    private ConnectivityManager mCm;
    private String mManageMobilePlanMessage;
    private NfcAdapter mNfcAdapter;
    private NfcEnabler mNfcEnabler;
    private PackageManager mPm;
    private TelephonyManager mTm;
    private UserManager mUm;
    IWfcSettingsExt mWfcExt;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.d("WirelessSettings", "PhoneStateListener, new state=" + state);
            if (state != 0 || WirelessSettings.this.getActivity() == null) {
                return;
            }
            WirelessSettings.this.updateMobileNetworkEnabled();
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(action)) {
                Log.d("WirelessSettings", "ACTION_SIM_INFO_UPDATE received");
                WirelessSettings.this.updateMobileNetworkEnabled();
                return;
            }
            if (!"android.telephony.action.CARRIER_CONFIG_CHANGED".equals(action)) {
                return;
            }
            Log.d("WirelessSettings", "carrier config changed...");
            if (WirelessSettings.this.mButtonWfc == null) {
                return;
            }
            if (ImsManager.isWfcEnabledByPlatform(context) && !Utils.isMonkeyRunning()) {
                Log.d("WirelessSettings", "wfc enabled, add WCF setting");
                WirelessSettings.this.getPreferenceScreen().addPreference(WirelessSettings.this.mButtonWfc);
                WirelessSettings.this.mWfcExt.initPlugin(WirelessSettings.this);
                WirelessSettings.this.mButtonWfc.setSummary(WirelessSettings.this.mWfcExt.getWfcSummary(context, WifiCallingSettings.getWfcModeSummary(context, ImsManager.getWfcMode(context))));
                WirelessSettings.this.mWfcExt.customizedWfcPreference(WirelessSettings.this.getActivity(), WirelessSettings.this.getPreferenceScreen());
                WirelessSettings.this.mWfcExt.onWirelessSettingsEvent(4);
                return;
            }
            Log.d("WirelessSettings", "wfc disabled, remove WCF setting");
            WirelessSettings.this.mWfcExt.onWirelessSettingsEvent(4);
            WirelessSettings.this.getPreferenceScreen().removePreference(WirelessSettings.this.mButtonWfc);
        }
    };

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        log("onPreferenceTreeClick: preference=" + preference);
        boolean isInECMMode = SystemProperties.get("ril.cdma.inecmmode").contains("true");
        if (preference == this.mAirplaneModePreference && isInECMMode && UserHandle.myUserId() == 0) {
            startActivityForResult(new Intent("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null), 1);
            return true;
        }
        if (preference == findPreference("manage_mobile_plan")) {
            onManageMobilePlanClick();
        }
        return super.onPreferenceTreeClick(preference);
    }

    public void onManageMobilePlanClick() {
        log("onManageMobilePlanClick:");
        this.mManageMobilePlanMessage = null;
        Resources resources = getActivity().getResources();
        NetworkInfo ni = this.mCm.getActiveNetworkInfo();
        if (this.mTm.hasIccCard() && ni != null) {
            Intent provisioningIntent = new Intent("android.intent.action.ACTION_CARRIER_SETUP");
            List<String> carrierPackages = this.mTm.getCarrierPackageNamesForIntent(provisioningIntent);
            if (carrierPackages != null && !carrierPackages.isEmpty()) {
                if (carrierPackages.size() != 1) {
                    Log.w("WirelessSettings", "Multiple matching carrier apps found, launching the first.");
                }
                provisioningIntent.setPackage(carrierPackages.get(0));
                startActivity(provisioningIntent);
                return;
            }
            String url = this.mCm.getMobileProvisioningUrl();
            if (!TextUtils.isEmpty(url)) {
                Intent intent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER");
                intent.setData(Uri.parse(url));
                intent.setFlags(272629760);
                try {
                    int dcha_state = BenesseExtension.getDchaState();
                    if (dcha_state == 0) {
                        startActivity(intent);
                    }
                } catch (ActivityNotFoundException e) {
                    Log.w("WirelessSettings", "onManageMobilePlanClick: startActivity failed" + e);
                }
            } else {
                String operatorName = this.mTm.getSimOperatorName();
                if (TextUtils.isEmpty(operatorName)) {
                    String operatorName2 = this.mTm.getNetworkOperatorName();
                    if (TextUtils.isEmpty(operatorName2)) {
                        this.mManageMobilePlanMessage = resources.getString(R.string.mobile_unknown_sim_operator);
                    } else {
                        this.mManageMobilePlanMessage = resources.getString(R.string.mobile_no_provisioning_url, operatorName2);
                    }
                } else {
                    this.mManageMobilePlanMessage = resources.getString(R.string.mobile_no_provisioning_url, operatorName);
                }
            }
        } else if (!this.mTm.hasIccCard()) {
            this.mManageMobilePlanMessage = resources.getString(R.string.mobile_insert_sim_card);
        } else {
            this.mManageMobilePlanMessage = resources.getString(R.string.mobile_connect_to_internet);
        }
        if (TextUtils.isEmpty(this.mManageMobilePlanMessage)) {
            return;
        }
        log("onManageMobilePlanClick: message=" + this.mManageMobilePlanMessage);
        showDialog(1);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case DefaultWfcSettingsExt.PAUSE:
                return new AlertDialog.Builder(getActivity()).setMessage(this.mManageMobilePlanMessage).setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        WirelessSettings.this.log("MANAGE_MOBILE_PLAN_DIALOG.onClickListener id=" + id);
                        WirelessSettings.this.mManageMobilePlanMessage = null;
                    }
                }).create();
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    public void log(String s) {
        Log.d("WirelessSettings", s);
    }

    @Override
    protected int getMetricsCategory() {
        return 110;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mManageMobilePlanMessage = savedInstanceState.getString("mManageMobilePlanMessage");
        }
        log("onCreate: mManageMobilePlanMessage=" + this.mManageMobilePlanMessage);
        this.mCm = (ConnectivityManager) getSystemService("connectivity");
        this.mTm = (TelephonyManager) getSystemService("phone");
        this.mPm = getPackageManager();
        this.mUm = (UserManager) getSystemService("user");
        addPreferencesFromResource(R.xml.wireless_settings);
        boolean isAdmin = this.mUm.isAdminUser();
        Activity activity = getActivity();
        this.mAirplaneModePreference = (SwitchPreference) findPreference("toggle_airplane");
        SwitchPreference nfc = (SwitchPreference) findPreference("toggle_nfc");
        RestrictedPreference androidBeam = (RestrictedPreference) findPreference("android_beam_settings");
        PreferenceScreen mtkNfc = (PreferenceScreen) findPreference("toggle_mtk_nfc");
        this.mAirplaneModeEnabler = new AirplaneModeEnabler(activity, this.mAirplaneModePreference);
        this.mNfcEnabler = new NfcEnabler(activity, nfc, androidBeam);
        this.mButtonWfc = (PreferenceScreen) findPreference("wifi_calling_settings");
        if (!"no".equals(SystemProperties.get("ro.mtk_carrierexpress_pack", "no")) && SystemProperties.getInt("ro.mtk_cxp_switch_mode", 0) != 2) {
            this.mButtonPreferredPhoneConfiguration = new PhoneConfigurationSettings(activity);
            getPreferenceScreen().addPreference(this.mButtonPreferredPhoneConfiguration);
        }
        String toggleable = Settings.Global.getString(activity.getContentResolver(), "airplane_mode_toggleable_radios");
        boolean isWimaxEnabled = isAdmin ? getResources().getBoolean(android.R.^attr-private.iconfactoryBadgeSize) : false;
        if (!isWimaxEnabled || RestrictedLockUtils.hasBaseUserRestriction(activity, "no_config_mobile_networks", UserHandle.myUserId())) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference("wimax_settings");
            if (ps != null) {
                root.removePreference(ps);
            }
        } else if (toggleable == null || (!toggleable.contains("wimax") && isWimaxEnabled)) {
            findPreference("wimax_settings").setDependency("toggle_airplane");
        }
        if (toggleable == null || !toggleable.contains("wifi")) {
            findPreference("vpn_settings").setDependency("toggle_airplane");
        }
        removePreference("vpn_settings");
        if (toggleable == null || toggleable.contains("bluetooth")) {
        }
        if (toggleable == null || !toggleable.contains("nfc")) {
            findPreference("toggle_nfc").setDependency("toggle_airplane");
            findPreference("android_beam_settings").setDependency("toggle_airplane");
            findPreference("toggle_mtk_nfc").setDependency("toggle_airplane");
        }
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (this.mNfcAdapter == null) {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(androidBeam);
            this.mNfcEnabler = null;
            getPreferenceScreen().removePreference(mtkNfc);
        } else if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(androidBeam);
            this.mNfcEnabler = null;
        } else {
            getPreferenceScreen().removePreference(mtkNfc);
        }
        if (!isAdmin || Utils.isWifiOnly(getActivity()) || RestrictedLockUtils.hasBaseUserRestriction(activity, "no_config_mobile_networks", UserHandle.myUserId())) {
            removePreference("mobile_network_settings");
            removePreference("manage_mobile_plan");
        }
        boolean isMobilePlanEnabled = getResources().getBoolean(R.bool.config_show_mobile_plan);
        if (!isMobilePlanEnabled) {
            Preference pref = findPreference("manage_mobile_plan");
            if (pref != null) {
                removePreference("manage_mobile_plan");
            }
        }
        if (this.mPm.hasSystemFeature("android.hardware.type.television")) {
            removePreference("toggle_airplane");
        }
        Preference mGlobalProxy = findPreference("proxy_settings");
        DevicePolicyManager mDPM = (DevicePolicyManager) activity.getSystemService("device_policy");
        getPreferenceScreen().removePreference(mGlobalProxy);
        mGlobalProxy.setEnabled(mDPM.getGlobalProxyAdmin() == null);
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService("connectivity");
        boolean adminDisallowedTetherConfig = RestrictedLockUtils.checkIfRestrictionEnforced(activity, "no_config_tethering", UserHandle.myUserId()) != null;
        if ((!cm.isTetheringSupported() && !adminDisallowedTetherConfig) || RestrictedLockUtils.hasBaseUserRestriction(activity, "no_config_tethering", UserHandle.myUserId())) {
            getPreferenceScreen().removePreference(findPreference("tether_settings"));
        } else if (!adminDisallowedTetherConfig) {
            Preference p = findPreference("tether_settings");
            p.setTitle(com.android.settingslib.Utils.getTetheringLabel(cm));
            p.setEnabled(!TetherSettings.isProvisioningNeededButUnavailable(getActivity()));
        }
        IWWOPJoynSettingsExt joynExt = UtilsExt.getJoynSettingsPlugin(getActivity());
        if (joynExt.isJoynSettingsEnabled()) {
            Log.d("WirelessSettings", "com.mediatek.rcse.RCSE_SETTINGS is enabled");
            Intent intent = new Intent("com.mediatek.rcse.RCSE_SETTINGS");
            findPreference("rcse_settings").setIntent(intent);
        } else {
            Log.d("WirelessSettings", "com.mediatek.rcse.RCSE_SETTINGS is not enabled");
            getPreferenceScreen().removePreference(findPreference("rcse_settings"));
        }
        IRCSSettings rcsExt = UtilsExt.getRcsSettingsPlugin(getActivity());
        rcsExt.addRCSPreference(getActivity(), getPreferenceScreen());
        this.mWfcExt = UtilsExt.getWfcSettingsPlugin(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAirplaneModeEnabler.resume();
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.resume();
        }
        Context context = getActivity();
        this.mWfcExt.initPlugin(this);
        if (ImsManager.isWfcEnabledByPlatform(context) && !Utils.isMonkeyRunning()) {
            if (this.mWfcExt.isWifiCallingProvisioned(context, SubscriptionManager.getDefaultVoicePhoneId())) {
                getPreferenceScreen().addPreference(this.mButtonWfc);
                this.mButtonWfc.setSummary(WifiCallingSettings.getWfcModeSummary(context, ImsManager.getWfcMode(context)));
                this.mButtonWfc.setSummary(this.mWfcExt.getWfcSummary(context, WifiCallingSettings.getWfcModeSummary(context, ImsManager.getWfcMode(context))));
                this.mWfcExt.customizedWfcPreference(getActivity(), getPreferenceScreen());
            }
        } else {
            removePreference("wifi_calling_settings");
        }
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        telephonyManager.listen(this.mPhoneStateListener, 32);
        updateMobileNetworkEnabled();
        if (!"no".equals(SystemProperties.get("ro.mtk_carrierexpress_pack", "no")) && SystemProperties.getInt("ro.mtk_cxp_switch_mode", 0) != 2) {
            this.mButtonPreferredPhoneConfiguration.initPreference();
        } else {
            removePreference("preferred_phone_configuration_key");
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        getActivity().registerReceiver(this.mReceiver, intentFilter);
        this.mWfcExt.onWirelessSettingsEvent(0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (TextUtils.isEmpty(this.mManageMobilePlanMessage)) {
            return;
        }
        outState.putString("mManageMobilePlanMessage", this.mManageMobilePlanMessage);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mAirplaneModeEnabler.pause();
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.pause();
        }
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        telephonyManager.listen(this.mPhoneStateListener, 0);
        getActivity().unregisterReceiver(this.mReceiver);
        if (!"no".equals(SystemProperties.get("ro.mtk_carrierexpress_pack", "no")) && SystemProperties.getInt("ro.mtk_cxp_switch_mode", 0) != 2) {
            this.mButtonPreferredPhoneConfiguration.deinitPreference();
        }
        this.mWfcExt.onWirelessSettingsEvent(1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            Boolean isChoiceYes = Boolean.valueOf(data.getBooleanExtra("exit_ecm_result", false));
            this.mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes.booleanValue(), this.mAirplaneModePreference.isChecked());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_more_networks;
    }

    public void updateMobileNetworkEnabled() {
        Preference preference = findPreference("mobile_network_settings");
        if (preference == null) {
            return;
        }
        if (preference instanceof RestrictedPreference) {
            RestrictedPreference rp = (RestrictedPreference) preference;
            if (rp.isDisabledByAdmin()) {
                Log.d("WirelessSettings", "MOBILE_NETWORK_SETTINGS disabled by Admin");
                return;
            }
        }
        ISettingsMiscExt miscExt = UtilsExt.getMiscPlugin(getActivity());
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        int callState = telephonyManager.getCallState();
        int simNum = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoCount();
        Log.d("WirelessSettings", "callstate = " + callState + " simNum = " + simNum);
        if (simNum > 0 && callState == 0 && !miscExt.isWifiOnlyModeSet()) {
            preference.setEnabled(true);
        } else {
            preference.setEnabled(UtilsExt.getSimManagmentExtPlugin(getActivity()).useCtTestcard());
        }
    }
}
