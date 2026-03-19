package com.mediatek.settings;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.MobileNetworkSettings;
import com.android.phone.R;
import com.android.phone.settings.SettingsConstants;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import java.util.List;

public class MobileNetworkSettingsOmEx {
    private static final String[] MCCMNC_TABLE_TYPE_CT = {"45502", "46003", "46011", "46012", "46013"};
    private Context mContext;
    private boolean mEng;
    private ListPreference mListPreference;
    private PreferenceScreen mPreferenceScreen;
    private int mCurrentTab = -1;
    private boolean mFlag = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MobileNetworkSettingsOmEx.this.log("action: " + action);
            if (action.equals("android.intent.action.SIM_STATE_CHANGED") && MobileNetworkSettingsOmEx.this.mListPreference != null) {
                MobileNetworkSettingsOmEx.this.updateNetworkTypeSummary(MobileNetworkSettingsOmEx.this.mListPreference);
            }
        }
    };

    public MobileNetworkSettingsOmEx(Context context) {
        this.mContext = null;
        this.mEng = true;
        if (!SystemProperties.get("ro.vendor.cmcc_light_cust_support").equals(SettingsConstants.DUA_VAL_ON) || !isLTESupport()) {
            return;
        }
        this.mContext = context;
        this.mEng = "eng".equals(SystemProperties.get("ro.build.type"));
        log("mContext =" + this.mContext + " mEng = " + this.mEng);
    }

    public void initMobileNetworkSettings(PreferenceScreen preferenceScreen, int i) {
        if (!SystemProperties.get("ro.vendor.cmcc_light_cust_support").equals(SettingsConstants.DUA_VAL_ON) || !isLTESupport()) {
            return;
        }
        log("init currentTab: " + i);
        this.mCurrentTab = i;
        this.mPreferenceScreen = preferenceScreen;
        if (this.mPreferenceScreen == null) {
            log("init mPreferenceScreen null");
            return;
        }
        if (this.mPreferenceScreen != null) {
            if (!this.mFlag) {
                log("init, mFlag==false");
                this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
                this.mFlag = true;
            }
            ListPreference listPreference = (ListPreference) this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_ENABLED_NETWORKS_KEY);
            ListPreference listPreference2 = (ListPreference) this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_PREFERED_NETWORK_MODE);
            ListPreference listPreference3 = (ListPreference) this.mPreferenceScreen.findPreference("button_network_mode_LTE_key");
            ListPreference listPreference4 = null;
            if (this.mPreferenceScreen.findPreference("enable_4g_data") != null) {
                log("remove ENABLE_4G_DATA");
                this.mPreferenceScreen.removePreference(this.mPreferenceScreen.findPreference("enable_4g_data"));
            }
            if (isCTCard(this.mCurrentTab) && this.mPreferenceScreen.findPreference(MobileNetworkSettings.MobileNetworkFragment.BUTTON_ENABLED_NETWORKS_KEY) == null) {
                log("init, CT card add ENABLED_NETWORK_MODE");
                listPreference4 = new ListPreference(this.mContext);
                listPreference4.setTitle(this.mContext.getString(R.string.preferred_network_mode_title));
                listPreference4.setKey(MobileNetworkSettings.MobileNetworkFragment.BUTTON_ENABLED_NETWORKS_KEY);
                listPreference4.setSummary(R.string.preferred_network_mode_cmcc_om);
                listPreference4.setEnabled(false);
                SwitchPreference switchPreference = (SwitchPreference) this.mPreferenceScreen.findPreference("button_roaming_key");
                listPreference4.setOrder((switchPreference != null ? switchPreference.getOrder() : 0) + 1);
                this.mPreferenceScreen.addPreference(listPreference4);
            }
            if (listPreference != null) {
                updateNetworkTypeSummary(listPreference);
                this.mListPreference = listPreference;
                return;
            }
            if (listPreference2 != null) {
                updateNetworkTypeSummary(listPreference2);
                this.mListPreference = listPreference2;
                return;
            } else if (listPreference4 != null && isNotPrimarySIM(get34GCapabilitySIMSlotId())) {
                updateNetworkTypeSummary(listPreference4);
                this.mListPreference = listPreference4;
                return;
            } else {
                if (listPreference3 != null) {
                    updateNetworkTypeSummary(listPreference3);
                    this.mListPreference = listPreference3;
                    return;
                }
                return;
            }
        }
        log("init with preferenceScreen null");
    }

    public void updateNetworkTypeSummary(ListPreference listPreference) {
        if (!SystemProperties.get("ro.vendor.cmcc_light_cust_support").equals(SettingsConstants.DUA_VAL_ON) || !isLTESupport() || listPreference == null) {
            return;
        }
        int i = get34GCapabilitySubId();
        int i2 = get34GCapabilitySIMSlotId();
        log("update, slotId:" + i2 + "type: " + getSIMType(i));
        if (i2 == -1 || isNotPrimarySIM(i2) || !isLTEModeEnable(i2, listPreference.getContext())) {
            if (this.mPreferenceScreen == null) {
                log("update, mPreferenceScreen = null");
                return;
            } else {
                log("update, removePreference");
                this.mPreferenceScreen.removePreference(listPreference);
                return;
            }
        }
        log("update mode:" + Settings.Global.getInt(getPhoneUsingSubId(i).getContext().getContentResolver(), "preferred_network_mode" + i, 9));
        listPreference.setSummary(R.string.preferred_network_mode_cmcc_om);
        listPreference.setEnabled(false);
        log("update mode, 4G/3G/2G(AUTO) disable");
    }

    public void updateLTEModeStatus(ListPreference listPreference) {
        Dialog dialog;
        if (!SystemProperties.get("ro.vendor.cmcc_light_cust_support").equals(SettingsConstants.DUA_VAL_ON) || !isLTESupport()) {
            return;
        }
        log("updateLTEModeStatus");
        updateNetworkTypeSummary(listPreference);
        if (!listPreference.isEnabled() && (dialog = listPreference.getDialog()) != null && dialog.isShowing()) {
            dialog.dismiss();
            log("updateLTEModeStatus: dismiss dialog ");
        }
    }

    private boolean isLTEModeEnable(int i, Context context) {
        log("isLTEModeEnable, slotId = " + i);
        if (!getRadioStateForSlotId(i, context) || getSimOperator(i) == null || getSimOperator(i).equals("")) {
            log("RadioState OFF, or SimOperator is null");
            return false;
        }
        log("isLTEModeEnable, should enable");
        return true;
    }

    private int get34GCapabilitySIMSlotId() {
        int phoneId;
        int i = get34GCapabilitySubId();
        if (i >= 0) {
            phoneId = SubscriptionManager.getPhoneId(i);
        } else {
            phoneId = -1;
        }
        log("get4GCapabilitySIMSlotId, slotId: " + phoneId);
        return phoneId;
    }

    private int get34GCapabilitySubId() {
        return MtkSubscriptionManager.getSubIdUsingPhoneId(SystemProperties.getInt("persist.vendor.radio.simswitch", 1) - 1);
    }

    private boolean getRadioStateForSlotId(int i, Context context) {
        boolean z = true;
        if ((Settings.System.getInt(context.getContentResolver(), "msim_mode_setting", -1) & (1 << i)) == 0) {
            z = false;
        }
        log("soltId: " + i + ", radiosState : " + z);
        return z;
    }

    private boolean isValidSlot(int i) {
        for (int i2 : new int[]{0, 1}) {
            if (i2 == i) {
                return true;
            }
        }
        return false;
    }

    private boolean isGeminiSupport() {
        TelephonyManager.MultiSimVariants multiSimConfiguration = TelephonyManager.getDefault().getMultiSimConfiguration();
        if (multiSimConfiguration == TelephonyManager.MultiSimVariants.DSDS || multiSimConfiguration == TelephonyManager.MultiSimVariants.DSDA) {
            return true;
        }
        return false;
    }

    private int getSimState(int i) {
        int simState;
        if (isGeminiSupport() && isValidSlot(i)) {
            simState = TelephonyManager.getDefault().getSimState(i);
        } else {
            simState = TelephonyManager.getDefault().getSimState();
        }
        log("getSimState, slotId = " + i + "; status = " + simState);
        return simState;
    }

    private String getSimOperator(int i) {
        String simOperator;
        int subIdUsingPhoneId = -1;
        if (5 == getSimState(i)) {
            if (isGeminiSupport()) {
                SubscriptionManager.from(this.mContext);
                subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(i);
                simOperator = TelephonyManager.getDefault().getSimOperator(subIdUsingPhoneId);
            } else {
                simOperator = TelephonyManager.getDefault().getSimOperator();
            }
        } else {
            simOperator = null;
        }
        log("getSimOperator, simOperator = " + simOperator + " slotId = " + i + " subId = " + subIdUsingPhoneId);
        return simOperator;
    }

    private boolean isLTESupport() {
        return SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.boot.opt_lte_support"));
    }

    private String getSIMType(int i) {
        if (i > 0) {
            try {
                return IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")).getIccCardType(i);
            } catch (RemoteException e) {
                Log.e("MobileNetworkSettingsOmEx", "getSIMType, exception: ", e);
            }
        }
        return null;
    }

    private Phone getPhoneUsingSubId(int i) {
        log("getPhoneUsingSubId subId:" + i);
        int phoneId = SubscriptionManager.getPhoneId(i);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount() || phoneId == Integer.MAX_VALUE) {
            return PhoneFactory.getPhone(0);
        }
        return PhoneFactory.getPhone(phoneId);
    }

    private boolean isNotPrimarySIM(int i) {
        List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            log("isNotPrimarySIM false, result null");
        } else if (isGeminiSupport() && activeSubscriptionInfoList.size() > 1 && i != this.mCurrentTab) {
            return true;
        }
        log("isNotPrimarySIM false, slotId:" + i);
        return false;
    }

    public void unRegister() {
        if (!SystemProperties.get("ro.vendor.cmcc_light_cust_support").equals(SettingsConstants.DUA_VAL_ON) || !isLTESupport()) {
            return;
        }
        log("unRegister");
        if (this.mFlag && this.mReceiver != null) {
            log("unRegister, mFlag = true");
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mFlag = false;
        }
        this.mListPreference = null;
        this.mPreferenceScreen = null;
    }

    private void log(String str) {
        if (this.mEng) {
            Log.d("MobileNetworkSettingsOmEx", str);
        }
    }

    private boolean isCTCard(int i) {
        log("isCTCard, slotId = " + i);
        String simOperator = getSimOperator(i);
        if (simOperator != null) {
            for (String str : MCCMNC_TABLE_TYPE_CT) {
                if (simOperator.equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }
}
