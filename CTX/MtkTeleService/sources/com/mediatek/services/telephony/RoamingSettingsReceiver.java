package com.mediatek.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

public class RoamingSettingsReceiver extends BroadcastReceiver {
    private static Context sContext;
    private static PhoneConstants.DataState sDataState = PhoneConstants.DataState.DISCONNECTED;
    private static int sRoamingState = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        int i;
        sContext = context.getApplicationContext();
        String action = intent.getAction();
        if (!action.equals("android.intent.action.SIM_STATE_CHANGED")) {
            if (action.equalsIgnoreCase("android.intent.action.ANY_DATA_STATE")) {
                Log.d("OP20RoamingSettingsReceiver", "onReceive android.intent.action.ANY_DATA_STATE");
                if (!checkCarrierConfig()) {
                    return;
                }
                Log.d("OP20RoamingSettingsReceiver", "onReceive android.intent.action.PRECISE_DATA_CONNECTION_STATE_CHANGED");
                handlePreciseDataConnectionStateChange(intent);
                return;
            }
            if (action.equalsIgnoreCase("android.intent.action.SERVICE_STATE")) {
                if (!checkCarrierConfig()) {
                    return;
                }
                Log.d("OP20RoamingSettingsReceiver", "onReceive android.intent.action.SERVICE_STATE");
                handleServiceStateChange(intent);
                return;
            }
            if (action.equalsIgnoreCase("android.intent.action.BOOT_COMPLETED")) {
                Log.d("OP20RoamingSettingsReceiver", "onReceive BOOT completed");
                if (!checkCarrierConfig()) {
                    return;
                }
                context.getApplicationContext().registerReceiver(this, new IntentFilter("android.intent.action.ANY_DATA_STATE"));
                return;
            }
            return;
        }
        String stringExtra = intent.getStringExtra("ss");
        int[] subId = SubscriptionManager.getSubId(intent.getIntExtra("slot", 0));
        if (subId != null && subId.length != 0) {
            i = subId[0];
        } else {
            i = 0;
        }
        Log.d("OP20RoamingSettingsReceiver", "Subinfo Record Update: " + i);
        if (i != -1 && "LOADED".equals(stringExtra)) {
            try {
                if (!((CarrierConfigManager) PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i)).getContext().getSystemService("carrier_config")).getConfigForSubId(i).getBoolean("mtk_key_roaming_bar_guard_bool")) {
                    Log.d("OP20RoamingSettingsReceiver", "No need to update value");
                    return;
                }
                setRoamingEnabled(i);
                try {
                    Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i));
                    Log.d("OP20RoamingSettingsReceiver", "Settings for roaming");
                    int i2 = Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + i, 1);
                    int i3 = Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + i, 0);
                    int i4 = Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + i, 0);
                    int i5 = Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + i, 1);
                    int i6 = Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming" + i, 1);
                    Log.d("OP20RoamingSettingsReceiver", "DOMESTIC_VOICE_TEXT_ROAMING: " + i2);
                    Log.d("OP20RoamingSettingsReceiver", "DOMESTIC_DATA_ROAMING: " + i3);
                    Log.d("OP20RoamingSettingsReceiver", "DOMESTIC_LTE_DATA_ROAMING: " + i4);
                    Log.d("OP20RoamingSettingsReceiver", "INTERNATIONAL_VOICE_TEXT_ROAMING: " + i5);
                    Log.d("OP20RoamingSettingsReceiver", "INTERNATIONAL_DATA_ROAMING: " + i6);
                    int i7 = Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming_guard" + i, 0);
                    int i8 = Settings.Global.getInt(phone.getContext().getContentResolver(), "domestic_data_roaming_guard" + i, 0);
                    int i9 = Settings.Global.getInt(phone.getContext().getContentResolver(), "international_voice_roaming_guard" + i, 1);
                    int i10 = Settings.Global.getInt(phone.getContext().getContentResolver(), "international_data_roaming_guard" + i, 0);
                    Log.d("OP20RoamingSettingsReceiver", "DOMESTIC_VOICE_guard: " + i7);
                    Log.d("OP20RoamingSettingsReceiver", "DOMESTIC_DATA_guard: " + i8);
                    Log.d("OP20RoamingSettingsReceiver", "internatioanl voice guard: " + i9);
                    Log.d("OP20RoamingSettingsReceiver", "INTERNATIONAL data guard: " + i10);
                } catch (Exception e) {
                    Log.e("OP20RoamingSettingsReceiver", "getDataOnRoamingEnabled: SettingNofFoundException snfe=" + e);
                }
            } catch (Exception e2) {
                Log.d("OP20RoamingSettingsReceiver", "Null phone No need to update value");
            }
        }
    }

    private boolean checkCarrierConfig() {
        if (!((CarrierConfigManager) sContext.getSystemService("carrier_config")).getConfig().getBoolean("mtk_key_roaming_bar_guard_bool")) {
            Log.d("OP20RoamingSettingsReceiver", "Not Valid for this OP MCC/MNC, no handling further");
            return false;
        }
        return true;
    }

    public void setRoamingEnabled(int i) {
        if (SubscriptionManager.from(sContext).getActiveSubscriptionInfoList() == null) {
            return;
        }
        if (Settings.Global.getInt(sContext.getContentResolver(), "ROAMING_INIT" + i, 0) == 1) {
            Log.d("OP20RoamingSettingsReceiver", "for subId " + i + "and value already 1");
            return;
        }
        Log.d("OP20RoamingSettingsReceiver", "for subId " + i + "and update roaming value");
        Settings.Global.putInt(sContext.getContentResolver(), "ROAMING_INIT" + i, 1);
        try {
            int phoneId = SubscriptionManager.getPhoneId(i);
            Phone phone = PhoneFactory.getPhone(phoneId);
            Log.d("OP20RoamingSettingsReceiver", "update for subId " + i + "and update roaming value");
            Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming" + i, 1);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + i, 0);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming" + i, 0);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "international_voice_text_roaming" + i, 1);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "international_data_roaming" + i, 1);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_voice_text_roaming_guard" + i, 0);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "domestic_data_roaming_guard" + i, 0);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "international_voice_roaming_guard" + i, 1);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "international_data_roaming_guard" + i, 0);
            PhoneFactory.getPhone(phone.getPhoneId()).setRoamingEnable(new int[]{phoneId, 1, 1, 1, 0, 0}, (Message) null);
        } catch (Exception e) {
            Log.e("OP20RoamingSettingsReceiver", "getDataOnRoamingEnabled: SettingNofFoundException snfe=" + e);
        }
    }

    private void handlePreciseDataConnectionStateChange(Intent intent) {
        PhoneConstants.DataState dataState;
        String stringExtra = intent.getStringExtra("state");
        if (stringExtra != null) {
            dataState = (PhoneConstants.DataState) Enum.valueOf(PhoneConstants.DataState.class, stringExtra);
        } else {
            dataState = PhoneConstants.DataState.DISCONNECTED;
        }
        String stringExtra2 = intent.getStringExtra("apnType");
        Log.d("OP20RoamingSettingsReceiver", "handlePreciseDataConnectionStateChange: apnType= " + stringExtra2 + ", newState= " + dataState + ", currentState= " + sDataState);
        if (dataState != sDataState && dataState == PhoneConstants.DataState.CONNECTED && stringExtra2.equals("default")) {
            sDataState = dataState;
            if (checkRoamingSetting()) {
                showAlertDialog(sRoamingState);
                return;
            }
            return;
        }
        if (dataState != sDataState && dataState == PhoneConstants.DataState.DISCONNECTED && stringExtra2.equals("default")) {
            sDataState = dataState;
        }
    }

    private void handleServiceStateChange(Intent intent) {
        ServiceState serviceStateNewFromBundle;
        Bundle extras = intent.getExtras();
        if (extras != null && (serviceStateNewFromBundle = ServiceState.newFromBundle(extras)) != null) {
            Log.d("OP20RoamingSettingsReceiver", "handleServiceStateChange, serviceState = " + serviceStateNewFromBundle);
            if (serviceStateNewFromBundle.getDataRoaming()) {
                if (serviceStateNewFromBundle.getDataRoamingType() == 2) {
                    sRoamingState = 2;
                } else if (serviceStateNewFromBundle.getDataRoamingType() == 3) {
                    sRoamingState = 3;
                }
                Log.d("OP20RoamingSettingsReceiver", "ServiceStateChanged:In Roaming,sRoamingState= " + sRoamingState);
                if (checkRoamingSetting()) {
                    showAlertDialog(sRoamingState);
                    return;
                }
                return;
            }
            Log.d("OP20RoamingSettingsReceiver", "handleServiceStateChange: getDataRoaming() returns false");
            sRoamingState = 0;
        }
    }

    private static boolean checkRoamingSetting() {
        if (sRoamingState == 0) {
            Log.d("OP20RoamingSettingsReceiver", "User Not in roaming, so return false");
            return false;
        }
        if (sDataState != PhoneConstants.DataState.CONNECTED) {
            Log.d("OP20RoamingSettingsReceiver", "Data not connected, so return false, sDataState = " + sDataState);
            return false;
        }
        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        int i = Settings.Global.getInt(sContext.getContentResolver(), "domestic_data_roaming" + defaultDataSubscriptionId, 0);
        int i2 = Settings.Global.getInt(sContext.getContentResolver(), "international_data_roaming" + defaultDataSubscriptionId, 0);
        int i3 = Settings.Global.getInt(sContext.getContentResolver(), "domestic_data_roaming_guard" + defaultDataSubscriptionId, 0);
        int i4 = Settings.Global.getInt(sContext.getContentResolver(), "international_data_roaming_guard" + defaultDataSubscriptionId, 0);
        Log.d("OP20RoamingSettingsReceiver", "domesticDataRoamingSetting = " + i + ", internationalDataRoamingSetting = " + i2 + ", domesticDataRoamingGuard = " + i3 + ", internationalDataRoamingGuard = " + i4 + " ,subId= " + defaultDataSubscriptionId);
        if (sRoamingState == 2 && i == 1 && i3 == 1) {
            Log.d("OP20RoamingSettingsReceiver", "Need show domestic roaming dialog");
            return true;
        }
        if (sRoamingState == 3 && i2 == 1 && i4 == 1) {
            Log.d("OP20RoamingSettingsReceiver", "Need show international roaming dialog");
            return true;
        }
        Log.d("OP20RoamingSettingsReceiver", "Roaming setting not enabled, return false");
        return false;
    }

    private static void showAlertDialog(int i) {
        Intent intent = new Intent(sContext, (Class<?>) RoamingAlertDialog.class);
        intent.setFlags(268435456);
        intent.putExtra("Roaming type", i);
        sContext.startActivity(intent);
    }

    public static void notifyRoamingSettingsChanged(String str) {
        if (sRoamingState == 0) {
            Log.d("OP20RoamingSettingsReceiver", "Not in roaming,don't handle settings changed ,prefKey = " + str);
            return;
        }
        if (sRoamingState == 2) {
            if (str.equals("domestic_data_roaming_settings") || str.equals("domestic_data_roaming_guard")) {
                Log.d("OP20RoamingSettingsReceiver", "In domestic roaming,Handle setting change,prefKey = " + str);
                if (checkRoamingSetting()) {
                    showAlertDialog(sRoamingState);
                    return;
                }
                return;
            }
            Log.d("OP20RoamingSettingsReceiver", "In domestic roaming,don't handle international,prefKey = " + str);
            return;
        }
        if (sRoamingState == 3) {
            if (str.equals("international_data_roaming_settings") || str.equals("international_data_roaming_guard")) {
                Log.d("OP20RoamingSettingsReceiver", "In international roaming,Handle setting change,prefKey = " + str);
                if (checkRoamingSetting()) {
                    showAlertDialog(sRoamingState);
                    return;
                }
                return;
            }
            Log.d("OP20RoamingSettingsReceiver", "In international roaming,don't handle domestic,prefKey = " + str);
        }
    }
}
