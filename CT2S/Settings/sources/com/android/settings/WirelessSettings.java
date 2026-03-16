package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.SmsApplication;
import com.android.settings.nfc.NfcEnabler;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class WirelessSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Indexable {
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
            result.add("toggle_nsd");
            UserManager um = (UserManager) context.getSystemService("user");
            int myUserId = UserHandle.myUserId();
            boolean isSecondaryUser = myUserId != 0;
            boolean isRestrictedUser = um.getUserInfo(myUserId).isRestricted();
            boolean isWimaxEnabled = !isSecondaryUser && context.getResources().getBoolean(android.R.^attr-private.fromTop);
            if (!isWimaxEnabled || um.hasUserRestriction("no_config_mobile_networks")) {
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
            TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
            if (!tm.isSmsCapable() || isRestrictedUser) {
                result.add("sms_application");
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
            boolean isCellBroadcastAppLinkEnabled = context.getResources().getBoolean(android.R.^attr-private.hideWheelUntilFocused);
            if (isCellBroadcastAppLinkEnabled) {
                try {
                    if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver") == 2) {
                        isCellBroadcastAppLinkEnabled = false;
                    }
                } catch (IllegalArgumentException e) {
                    isCellBroadcastAppLinkEnabled = false;
                }
            }
            if (isSecondaryUser || !isCellBroadcastAppLinkEnabled) {
                result.add("cell_broadcast_settings");
            }
            return result;
        }
    };
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;
    private ConnectivityManager mCm;
    private String mManageMobilePlanMessage;
    private NfcAdapter mNfcAdapter;
    private NfcEnabler mNfcEnabler;
    private NsdEnabler mNsdEnabler;
    private PackageManager mPm;
    private AppListPreference mSmsApplicationPreference;
    private TelephonyManager mTm;
    private UserManager mUm;

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        log("onPreferenceTreeClick: preference=" + preference);
        if (preference == this.mAirplaneModePreference && Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
            startActivityForResult(new Intent("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null), 1);
            return true;
        }
        if (preference == findPreference("manage_mobile_plan")) {
            onManageMobilePlanClick();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onManageMobilePlanClick() {
        log("onManageMobilePlanClick:");
        this.mManageMobilePlanMessage = null;
        Resources resources = getActivity().getResources();
        NetworkInfo ni = this.mCm.getProvisioningOrActiveNetworkInfo();
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
                    startActivity(intent);
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
        if (!TextUtils.isEmpty(this.mManageMobilePlanMessage)) {
            log("onManageMobilePlanClick: message=" + this.mManageMobilePlanMessage);
            showDialog(1);
        }
    }

    private void initSmsApplicationSetting() {
        log("initSmsApplicationSetting:");
        Collection<SmsApplication.SmsApplicationData> smsApplications = SmsApplication.getApplicationCollection(getActivity());
        int count = smsApplications.size();
        String[] packageNames = new String[count];
        int i = 0;
        for (SmsApplication.SmsApplicationData smsApplicationData : smsApplications) {
            packageNames[i] = smsApplicationData.mPackageName;
            i++;
        }
        String defaultPackageName = null;
        ComponentName appName = SmsApplication.getDefaultSmsApplication(getActivity(), true);
        if (appName != null) {
            defaultPackageName = appName.getPackageName();
        }
        this.mSmsApplicationPreference.setPackageNames(packageNames, defaultPackageName);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case 1:
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

    private void log(String s) {
        Log.d("WirelessSettings", s);
    }

    public static boolean isRadioAllowed(Context context, String type) {
        if (!AirplaneModeEnabler.isAirplaneModeOn(context)) {
            return true;
        }
        String toggleable = Settings.Global.getString(context.getContentResolver(), "airplane_mode_toggleable_radios");
        return toggleable != null && toggleable.contains(type);
    }

    private boolean isSmsSupported() {
        return this.mTm.isSmsCapable();
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
        int myUserId = UserHandle.myUserId();
        boolean isSecondaryUser = myUserId != 0;
        boolean isRestrictedUser = this.mUm.getUserInfo(myUserId).isRestricted();
        Activity activity = getActivity();
        this.mAirplaneModePreference = (SwitchPreference) findPreference("toggle_airplane");
        SwitchPreference nfc = (SwitchPreference) findPreference("toggle_nfc");
        PreferenceScreen androidBeam = (PreferenceScreen) findPreference("android_beam_settings");
        SwitchPreference nsd = (SwitchPreference) findPreference("toggle_nsd");
        this.mAirplaneModeEnabler = new AirplaneModeEnabler(activity, this.mAirplaneModePreference);
        this.mNfcEnabler = new NfcEnabler(activity, nfc, androidBeam);
        this.mSmsApplicationPreference = (AppListPreference) findPreference("sms_application");
        if (isRestrictedUser) {
            removePreference("sms_application");
        } else {
            this.mSmsApplicationPreference.setOnPreferenceChangeListener(this);
            initSmsApplicationSetting();
        }
        getPreferenceScreen().removePreference(nsd);
        String toggleable = Settings.Global.getString(activity.getContentResolver(), "airplane_mode_toggleable_radios");
        boolean isWimaxEnabled = !isSecondaryUser && getResources().getBoolean(android.R.^attr-private.fromTop);
        if (!isWimaxEnabled || this.mUm.hasUserRestriction("no_config_mobile_networks")) {
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
        if (isSecondaryUser || this.mUm.hasUserRestriction("no_config_vpn")) {
            removePreference("vpn_settings");
        }
        if (toggleable == null || !toggleable.contains("bluetooth")) {
        }
        if (toggleable == null || !toggleable.contains("nfc")) {
            findPreference("toggle_nfc").setDependency("toggle_airplane");
            findPreference("android_beam_settings").setDependency("toggle_airplane");
        }
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (this.mNfcAdapter == null) {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(androidBeam);
            this.mNfcEnabler = null;
        }
        if (isSecondaryUser || Utils.isWifiOnly(getActivity()) || this.mUm.hasUserRestriction("no_config_mobile_networks")) {
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
        if (!isSmsSupported()) {
            removePreference("sms_application");
        }
        if (this.mPm.hasSystemFeature("android.hardware.type.television")) {
            removePreference("toggle_airplane");
        }
        Preference mGlobalProxy = findPreference("proxy_settings");
        DevicePolicyManager mDPM = (DevicePolicyManager) activity.getSystemService("device_policy");
        getPreferenceScreen().removePreference(mGlobalProxy);
        mGlobalProxy.setEnabled(mDPM.getGlobalProxyAdmin() == null);
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService("connectivity");
        if (isSecondaryUser || !cm.isTetheringSupported() || this.mUm.hasUserRestriction("no_config_tethering")) {
            getPreferenceScreen().removePreference(findPreference("tether_settings"));
        } else {
            Preference p = findPreference("tether_settings");
            p.setTitle(Utils.getTetheringLabel(cm));
            p.setEnabled(!TetherSettings.isProvisioningNeededButUnavailable(getActivity()));
        }
        boolean isCellBroadcastAppLinkEnabled = getResources().getBoolean(android.R.^attr-private.hideWheelUntilFocused);
        if (isCellBroadcastAppLinkEnabled) {
            try {
                if (this.mPm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver") == 2) {
                    isCellBroadcastAppLinkEnabled = false;
                }
            } catch (IllegalArgumentException e) {
                isCellBroadcastAppLinkEnabled = false;
            }
        }
        if (isSecondaryUser || !isCellBroadcastAppLinkEnabled || this.mUm.hasUserRestriction("no_config_cell_broadcasts")) {
            PreferenceScreen root2 = getPreferenceScreen();
            Preference ps2 = findPreference("cell_broadcast_settings");
            if (ps2 != null) {
                root2.removePreference(ps2);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initSmsApplicationSetting();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAirplaneModeEnabler.resume();
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.resume();
        }
        if (this.mNsdEnabler != null) {
            this.mNsdEnabler.resume();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!TextUtils.isEmpty(this.mManageMobilePlanMessage)) {
            outState.putString("mManageMobilePlanMessage", this.mManageMobilePlanMessage);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mAirplaneModeEnabler.pause();
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.pause();
        }
        if (this.mNsdEnabler != null) {
            this.mNsdEnabler.pause();
        }
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference != this.mSmsApplicationPreference || newValue == null) {
            return false;
        }
        SmsApplication.setDefaultApplication(newValue.toString(), getActivity());
        return true;
    }
}
