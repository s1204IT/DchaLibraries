package com.mediatek.settings.cdma;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.phone.MobileNetworkSettings;
import com.android.phone.R;
import com.mediatek.settings.TelephonyUtils;

public class CdmaNetworkSettings {
    private static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
    private Activity mActivity;
    private SwitchPreference mEnable4GDataPreference;
    private SwitchPreference mEnableSigle4GDataPreference;
    private IntentFilter mIntentFilter;
    private Phone mPhone;
    private PreferenceScreen mPreferenceScreen;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("CdmaNetworkSettings", "on receive broadcast action = " + action);
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                int intExtra = intent.getIntExtra("slot", -1);
                Log.d("CdmaNetworkSettings", "slotId: " + intExtra);
                if (intExtra == SubscriptionManager.getPhoneId(CdmaNetworkSettings.this.mPhone.getSubId())) {
                    CdmaNetworkSettings.this.updateSwitch();
                }
            }
        }
    };
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            Log.d("CdmaNetworkSettings", "onChange...");
            CdmaNetworkSettings.this.updateSwitch();
        }
    };
    private Enable4GHandler mEnable4GHandler = new Enable4GHandler();

    public CdmaNetworkSettings(Activity activity, PreferenceScreen preferenceScreen, Phone phone) {
        this.mActivity = activity;
        this.mPhone = phone;
        this.mPreferenceScreen = preferenceScreen;
        if (this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_ENABLED_NETWORKS_KEY) != null) {
            this.mPreferenceScreen.removePreference(this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_ENABLED_NETWORKS_KEY));
        }
        if (this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_PREFERED_NETWORK_MODE) != null) {
            this.mPreferenceScreen.removePreference(this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_PREFERED_NETWORK_MODE));
        }
        if (!TelephonyUtilsEx.isRoaming(this.mPhone) && this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_PLMN_LIST) != null) {
            this.mPreferenceScreen.removePreference(this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_PLMN_LIST));
        }
        addEnable4GNetworkItem();
        if (TelephonyUtils.isMtkTddDataOnlySupport()) {
            addEnable4GSingleDataOnlyItem();
        }
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        this.mIntentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mActivity.registerReceiver(this.mReceiver, this.mIntentFilter);
        this.mActivity.getContentResolver().registerContentObserver(Settings.Global.getUriFor("volte_vt_enabled"), true, this.mContentObserver);
        if (SubscriptionManager.getDefaultDataSubscriptionId() == this.mPhone.getSubId()) {
            this.mActivity.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_data" + this.mPhone.getSubId()), true, this.mContentObserver);
        }
    }

    public void onResume() {
        Log.d("CdmaNetworkSettings", "onResume");
        updateSwitch();
    }

    public void onDestroy() {
        Log.d("CdmaNetworkSettings", "onDestroy");
        this.mActivity.unregisterReceiver(this.mReceiver);
        this.mActivity.getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("single_lte_data")) {
            handleEnable4GDataOnlyClick(preference);
            return true;
        }
        if (preference.getKey().equals("enable_4g_data")) {
            handleEnable4GDataClick(preference);
            return true;
        }
        return false;
    }

    private void addEnable4GSingleDataOnlyItem() {
        this.mEnableSigle4GDataPreference = new SwitchPreference(this.mActivity);
        this.mEnableSigle4GDataPreference.setTitle(R.string.only_use_LTE_data);
        this.mEnableSigle4GDataPreference.setKey("single_lte_data");
        this.mEnableSigle4GDataPreference.setSummaryOn(R.string.only_use_LTE_data_summary);
        this.mEnableSigle4GDataPreference.setSummaryOff(R.string.only_use_LTE_data_summary);
        this.mEnableSigle4GDataPreference.setOrder(this.mPreferenceScreen.findPreference("enable_4g_data").getOrder() + 1);
        this.mPreferenceScreen.addPreference(this.mEnableSigle4GDataPreference);
    }

    private void addEnable4GNetworkItem() {
        if (this.mEnable4GDataPreference == null) {
            this.mEnable4GDataPreference = new SwitchPreference(this.mActivity);
            this.mEnable4GDataPreference.setTitle(R.string.enable_4G_data);
            this.mEnable4GDataPreference.setKey("enable_4g_data");
            this.mEnable4GDataPreference.setSummary(R.string.enable_4G_data_summary);
            Preference preferenceFindPreference = this.mPreferenceScreen.findPreference("button_roaming_key");
            if (preferenceFindPreference != null) {
                this.mEnable4GDataPreference.setOrder(preferenceFindPreference.getOrder() + 1);
            }
        }
        this.mPreferenceScreen.addPreference(this.mEnable4GDataPreference);
    }

    private boolean isCallStateIDLE() {
        boolean z = ((TelephonyManager) this.mActivity.getSystemService("phone")).getCallState() == 0;
        Log.d("CdmaNetworkSettings", "isCallStateIDLE: " + z);
        return z;
    }

    private boolean isLteCardReady() {
        boolean zIsAirPlaneMode = TelephonyUtilsEx.isAirPlaneMode();
        boolean zIsCallStateIDLE = isCallStateIDLE();
        boolean zIsCdma4gCard = TelephonyUtilsEx.isCdma4gCard(this.mPhone.getSubId());
        boolean z = zIsCdma4gCard && !zIsAirPlaneMode && zIsCallStateIDLE;
        Log.d("CdmaNetworkSettings", "isLteCardReady: " + z + ";isCdma4GCard:" + zIsCdma4gCard);
        return z;
    }

    private void updateSwitch() {
        int i = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + this.mPhone.getSubId(), preferredNetworkMode);
        boolean z = false;
        boolean z2 = SystemProperties.get("persist.vendor.radio.mtk_ps2_rat").indexOf(76) != -1;
        boolean z3 = (isLteCardReady() && (TelephonyUtilsEx.isCapabilityPhone(this.mPhone) || z2)) || TelephonyUtils.isCTLteTddTestSupport();
        boolean z4 = z3 && (i == 10 || i == 31 || i == 22);
        Log.d("CdmaNetworkSettings", " enable = " + z3 + " ,settingsNetworkMode: " + i + " dualLte = " + z2 + " checked = " + z4);
        this.mEnable4GDataPreference.setEnabled(z3);
        this.mEnable4GDataPreference.setChecked(z4);
        if (TelephonyUtils.isMtkTddDataOnlySupport()) {
            boolean z5 = SubscriptionManager.getDefaultDataSubscriptionId() == this.mPhone.getSubId() && TelephonyManager.getDefault().getDataEnabled();
            boolean z6 = TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCt4gSim(this.mPhone.getSubId()) && ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this.mActivity);
            Log.d("CdmaNetworkSettings", "isDataEnabled = " + z5 + ", isCtVolteOn = " + z6);
            this.mEnableSigle4GDataPreference.setEnabled(z4 && z5 && !TelephonyUtilsEx.isCdmaRoaming(this.mPhone) && !z6);
            SwitchPreference switchPreference = this.mEnableSigle4GDataPreference;
            if (z3 && i == 31) {
                z = true;
            }
            switchPreference.setChecked(z);
        }
    }

    private void handleEnable4GDataClick(Preference preference) {
        boolean zIsChecked = ((SwitchPreference) preference).isChecked();
        int i = zIsChecked ? 10 : 7;
        Log.d("CdmaNetworkSettings", "handleEnable4GDataClick isChecked = " + zIsChecked);
        Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + this.mPhone.getSubId(), i);
        Phone phone = this.mPhone;
        Enable4GHandler enable4GHandler = this.mEnable4GHandler;
        Enable4GHandler enable4GHandler2 = this.mEnable4GHandler;
        phone.setPreferredNetworkType(i, enable4GHandler.obtainMessage(0));
        CdmaVolteServiceChecker.getInstance(this.mPhone.getContext()).onEnable4gStateChanged();
    }

    private void handleEnable4GDataOnlyClick(Preference preference) {
        boolean zIsChecked = ((SwitchPreference) preference).isChecked();
        int i = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + this.mPhone.getSubId(), preferredNetworkMode);
        if (i != 7) {
            i = 10;
        }
        if (zIsChecked) {
            i = 31;
        }
        Log.d("CdmaNetworkSettings", "handleEnable4GDataOnlyClick isChecked = " + zIsChecked);
        Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + this.mPhone.getSubId(), i);
        Phone phone = this.mPhone;
        Enable4GHandler enable4GHandler = this.mEnable4GHandler;
        Enable4GHandler enable4GHandler2 = this.mEnable4GHandler;
        phone.setPreferredNetworkType(i, enable4GHandler.obtainMessage(1));
        Intent intent = new Intent("com.mediatek.intent.action.ACTION_NETWORK_CHANGED");
        intent.setComponent(new ComponentName("com.android.phone", "com.mediatek.settings.cdma.LteDataOnlySwitchReceiver"));
        this.mActivity.sendBroadcast(intent);
        CdmaVolteServiceChecker.getInstance(this.mPhone.getContext()).onEnable4gStateChanged();
    }

    private void updateEnable4GNetworkUIFromDb() {
        int i = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + this.mPhone.getSubId(), preferredNetworkMode);
        Log.d("CdmaNetworkSettings", "updateEnable4GNetworkUIFromDb: settingsNetworkMode = " + i);
        this.mEnable4GDataPreference.setChecked(i == 10);
    }

    private void updateEnableSingle4GDataNetworkUIFromDb() {
        int i = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + this.mPhone.getSubId(), preferredNetworkMode);
        Log.d("CdmaNetworkSettings", "updateEnableSingle4GDataNetworkUIFromDb: settingsNetworkMode = " + i);
        this.mEnable4GDataPreference.setChecked(i == 31);
    }

    private class Enable4GHandler extends Handler {
        private Enable4GHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleSetEnable4GNetworkTypeResponse(message);
                    break;
                case 1:
                    handleSetEnableSigle4GDataNetworkTypeResponse(message);
                    break;
            }
        }

        private void handleSetEnable4GNetworkTypeResponse(Message message) {
            if (((AsyncResult) message.obj).exception == null) {
                if (CdmaNetworkSettings.this.mEnable4GDataPreference != null) {
                    Log.d("CdmaNetworkSettings", "isChecked = " + CdmaNetworkSettings.this.mEnable4GDataPreference.isChecked());
                    return;
                }
                return;
            }
            Log.d("CdmaNetworkSettings", "handleSetEnable4GNetworkTypeResponse: exception.");
            CdmaNetworkSettings.this.updateEnable4GNetworkUIFromDb();
        }

        private void handleSetEnableSigle4GDataNetworkTypeResponse(Message message) {
            if (((AsyncResult) message.obj).exception == null) {
                if (CdmaNetworkSettings.this.mEnableSigle4GDataPreference != null) {
                    boolean zIsChecked = CdmaNetworkSettings.this.mEnableSigle4GDataPreference.isChecked();
                    int i = Settings.Global.getInt(CdmaNetworkSettings.this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + CdmaNetworkSettings.this.mPhone.getSubId(), CdmaNetworkSettings.preferredNetworkMode);
                    if (i != 7) {
                        i = 10;
                    }
                    int i2 = zIsChecked ? 31 : i;
                    Log.d("CdmaNetworkSettings", "isChecked = " + zIsChecked);
                    if (i2 != i) {
                        Settings.Global.putInt(CdmaNetworkSettings.this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + CdmaNetworkSettings.this.mPhone.getSubId(), i2);
                        return;
                    }
                    return;
                }
                return;
            }
            Log.d("CdmaNetworkSettings", "handleSetEnableSigle4GDataNetworkTypeResponse: exception.");
            CdmaNetworkSettings.this.updateEnableSingle4GDataNetworkUIFromDb();
        }
    }
}
