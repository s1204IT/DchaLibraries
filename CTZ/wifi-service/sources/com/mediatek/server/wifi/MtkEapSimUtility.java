package com.mediatek.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.util.TelephonyUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

public class MtkEapSimUtility {
    public static final int GET_SUBID_NULL_ERROR = -1;
    private static final String TAG = "MtkEapSimUtility";
    private static TelephonyManager mTelephonyManager;
    private static WifiConfigManager mWifiConfigManager;
    private static WifiNative mWifiNative;
    private static WifiStateMachine mWifiStateMachine;
    private static boolean mVerboseLoggingEnabled = false;
    private static String mSim1IccState = "UNKNOWN";
    private static String mSim2IccState = "UNKNOWN";
    private static boolean mSim1Present = false;
    private static boolean mSim2Present = false;

    public static void init() {
        if (mTelephonyManager == null) {
            mTelephonyManager = WifiInjector.getInstance().makeTelephonyManager();
        }
        mWifiConfigManager = WifiInjector.getInstance().getWifiConfigManager();
        mWifiNative = WifiInjector.getInstance().getWifiNative();
        mWifiStateMachine = WifiInjector.getInstance().getWifiStateMachine();
    }

    public static boolean setSimSlot(int i, String str) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "Set network sim slot " + str + " for netId " + i);
        }
        WifiConfiguration internalConfiguredNetwork = getInternalConfiguredNetwork(i);
        if (internalConfiguredNetwork == null || !TelephonyUtil.isSimConfig(internalConfiguredNetwork)) {
            return false;
        }
        internalConfiguredNetwork.enterpriseConfig.setSimNum(WifiInfo.removeDoubleQuotes(str));
        mWifiConfigManager.saveToStore(true);
        return true;
    }

    public static void resetSimNetworks(boolean z, int i) {
        if (mVerboseLoggingEnabled) {
            Log.v(TAG, "resetSimNetworks, simPresent: " + z + ", simSlot: " + i);
        }
        for (WifiConfiguration wifiConfiguration : getInternalConfiguredNetworks()) {
            if (TelephonyUtil.isSimConfig(wifiConfiguration) && getIntSimSlot(wifiConfiguration) == i) {
                if (mVerboseLoggingEnabled) {
                    Log.v(TAG, "Reset SSID " + wifiConfiguration.SSID + " with simSlot " + i);
                }
                Pair<String, String> simIdentity = null;
                if (z) {
                    simIdentity = getSimIdentity(mTelephonyManager, new TelephonyUtil(), wifiConfiguration);
                }
                if (simIdentity == null) {
                    Log.d(TAG, "Identity is null");
                    return;
                } else {
                    wifiConfiguration.enterpriseConfig.setIdentity((String) simIdentity.first);
                    if (wifiConfiguration.enterpriseConfig.getEapMethod() != 0) {
                        wifiConfiguration.enterpriseConfig.setAnonymousIdentity("");
                    }
                }
            }
        }
    }

    public static boolean isSim1Present() {
        return mSim1Present;
    }

    public static boolean isSim2Present() {
        return mSim2Present;
    }

    public static int getIntSimSlot(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null || !TelephonyUtil.isSimConfig(wifiConfiguration)) {
            return -1;
        }
        String simNum = wifiConfiguration.enterpriseConfig.getSimNum();
        if (simNum == null) {
            return 0;
        }
        String[] strArrSplit = simNum.split("\"");
        if (strArrSplit.length > 1) {
            return Integer.parseInt(strArrSplit[1]);
        }
        if (strArrSplit.length != 1 || strArrSplit[0].length() <= 0) {
            return 0;
        }
        return Integer.parseInt(strArrSplit[0]);
    }

    public static Pair<String, String> getSimIdentity(TelephonyManager telephonyManager, TelephonyUtil telephonyUtil, WifiConfiguration wifiConfiguration) {
        if (telephonyManager == null) {
            Log.e(TAG, "No valid TelephonyManager");
            return null;
        }
        String subscriberId = telephonyManager.getSubscriberId();
        String simOperator = "";
        if (telephonyManager.getSimState() == 5) {
            simOperator = telephonyManager.getSimOperator();
        }
        int subId = getSubId(getIntSimSlot(wifiConfiguration));
        if (TelephonyManager.getDefault().getPhoneCount() >= 2 && subId != -1) {
            subscriberId = telephonyManager.getSubscriberId(subId);
            simOperator = "";
            if (telephonyManager.getSimState() == 5) {
                simOperator = telephonyManager.getSimOperator(subId);
            }
        }
        try {
            ImsiEncryptionInfo carrierInfoForImsiEncryption = telephonyManager.getCarrierInfoForImsiEncryption(2);
            String strBuildIdentity = buildIdentity(getSimMethodForConfig(wifiConfiguration), subscriberId, simOperator, false);
            if (strBuildIdentity == null) {
                Log.e(TAG, "Failed to build the identity");
                return null;
            }
            String strBuildEncryptedIdentity = buildEncryptedIdentity(telephonyUtil, getSimMethodForConfig(wifiConfiguration), subscriberId, simOperator, carrierInfoForImsiEncryption);
            if (strBuildEncryptedIdentity == null) {
                strBuildEncryptedIdentity = "";
            }
            return Pair.create(strBuildIdentity, strBuildEncryptedIdentity);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to get imsi encryption info: " + e.getMessage());
            return null;
        }
    }

    public static int getSubId(int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId != null) {
            return subId[0];
        }
        return -1;
    }

    public static int getDefaultSim() {
        return SubscriptionManager.getSlotIndex(SubscriptionManager.getDefaultSubscriptionId());
    }

    public static String getIccAuthentication(int i, int i2, String str) {
        int subId;
        int intSimSlot = getIntSimSlot(getTargetWificonfiguration());
        if (intSimSlot != -1) {
            TelephonyManager telephonyManager = mTelephonyManager;
            if (TelephonyManager.getDefault().getPhoneCount() >= 2 && (subId = getSubId(intSimSlot)) != -1) {
                Log.d(TAG, "subId: " + subId + ", appType: " + i + ", authType: " + i2 + ", challenge: " + str);
                Log.d(TAG, "getIccAuthentication for specified subId");
                return mTelephonyManager.getIccAuthentication(subId, i, i2, str);
            }
        }
        Log.d(TAG, "getIccAuthentication for the default subscription");
        return mTelephonyManager.getIccAuthentication(i, i2, str);
    }

    public static boolean isConfigSimCardPresent(WifiConfiguration wifiConfiguration) {
        int intSimSlot = getIntSimSlot(wifiConfiguration);
        if (intSimSlot != -1) {
            return intSimSlot == 0 ? mSim1Present : mSim2Present;
        }
        Log.d(TAG, "simSlot is unspecified, check sim state: (" + mSim1Present + ", " + mSim2Present + ")");
        if (mSim1Present || mSim2Present) {
            return true;
        }
        return false;
    }

    public static boolean isDualSimAbsent() {
        return (mSim1Present || mSim2Present) ? false : true;
    }

    public static void setDefaultSimToUnspecifiedSimSlot() {
        WifiConfiguration targetWificonfiguration = getTargetWificonfiguration();
        if (targetWificonfiguration == null || !TelephonyUtil.isSimConfig(targetWificonfiguration)) {
            Log.e(TAG, "Empty config or invalid config to set sim slot");
            return;
        }
        if (getSubId(getIntSimSlot(targetWificonfiguration)) == -1) {
            Log.d(TAG, "config.simSlot is unspecified(-1), set to default sim slot selected by telephony manager");
            int defaultSim = getDefaultSim();
            targetWificonfiguration.enterpriseConfig.setSimNum("\"" + defaultSim + "\"");
            if (!setSimSlot(targetWificonfiguration.networkId, targetWificonfiguration.enterpriseConfig.getSimNum())) {
                Log.e(TAG, "Fail to set sim slot for config, networkId=" + targetWificonfiguration.networkId);
            }
        }
    }

    public static boolean isSimConfigSameAsCurrent(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        if (wifiConfiguration == null || wifiConfiguration2 == null) {
            Log.e(TAG, "Null config");
            return false;
        }
        if (!TelephonyUtil.isSimConfig(wifiConfiguration)) {
            return false;
        }
        if (wifiConfiguration.enterpriseConfig.getEapMethod() != wifiConfiguration2.enterpriseConfig.getEapMethod()) {
            Log.d(TAG, "EAP method changed, skip checking");
            return false;
        }
        Log.d(TAG, "config sim: " + getIntSimSlot(wifiConfiguration) + " current sim: " + getIntSimSlot(wifiConfiguration2) + " default sim: " + getDefaultSim());
        if (getIntSimSlot(wifiConfiguration) != -1 || getIntSimSlot(wifiConfiguration2) != getDefaultSim()) {
            return getIntSimSlot(wifiConfiguration2) == getIntSimSlot(wifiConfiguration);
        }
        if (!setSimSlot(wifiConfiguration.networkId, "\"" + getDefaultSim() + "\"")) {
            Log.e(TAG, "Fail to set sim slot for config, networkId=" + wifiConfiguration.networkId);
        }
        return true;
    }

    private static WifiConfiguration getTargetWificonfiguration() {
        try {
            Field declaredField = mWifiStateMachine.getClass().getDeclaredField("targetWificonfiguration");
            declaredField.setAccessible(true);
            return (WifiConfiguration) declaredField.get(mWifiStateMachine);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Collection<WifiConfiguration> getInternalConfiguredNetworks() {
        try {
            Method declaredMethod = mWifiConfigManager.getClass().getDeclaredMethod("getInternalConfiguredNetworks", new Class[0]);
            declaredMethod.setAccessible(true);
            return (Collection) declaredMethod.invoke(mWifiConfigManager, new Object[0]);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static WifiConfiguration getInternalConfiguredNetwork(int i) {
        try {
            Method declaredMethod = mWifiConfigManager.getClass().getDeclaredMethod("getInternalConfiguredNetwork", Integer.TYPE);
            declaredMethod.setAccessible(true);
            return (WifiConfiguration) declaredMethod.invoke(mWifiConfigManager, Integer.valueOf(i));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String buildIdentity(int i, String str, String str2, boolean z) {
        try {
            Method declaredMethod = TelephonyUtil.class.getDeclaredMethod("buildIdentity", Integer.TYPE, String.class, String.class, Boolean.TYPE);
            declaredMethod.setAccessible(true);
            return (String) declaredMethod.invoke(null, Integer.valueOf(i), str, str2, Boolean.valueOf(z));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String buildEncryptedIdentity(TelephonyUtil telephonyUtil, int i, String str, String str2, ImsiEncryptionInfo imsiEncryptionInfo) {
        try {
            Method declaredMethod = TelephonyUtil.class.getDeclaredMethod("buildEncryptedIdentity", TelephonyUtil.class, Integer.TYPE, String.class, String.class, ImsiEncryptionInfo.class);
            declaredMethod.setAccessible(true);
            return (String) declaredMethod.invoke(null, telephonyUtil, Integer.valueOf(i), str, str2, imsiEncryptionInfo);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static int getSimMethodForConfig(WifiConfiguration wifiConfiguration) {
        try {
            Method declaredMethod = TelephonyUtil.class.getDeclaredMethod("getSimMethodForConfig", WifiConfiguration.class);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(null, wifiConfiguration)).intValue();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void enableVerboseLogging(int i) {
        if (i > 0) {
            mVerboseLoggingEnabled = true;
        } else {
            mVerboseLoggingEnabled = false;
        }
    }

    public static class MtkSimBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int iIntValue;
            String stringExtra = intent.getStringExtra("ss");
            int intExtra = intent.getIntExtra("slot", -1);
            Log.d(MtkEapSimUtility.TAG, "onReceive ACTION_SIM_STATE_CHANGED iccState: " + stringExtra + ", simSlot: " + intExtra);
            try {
                Field declaredField = MtkEapSimUtility.mWifiStateMachine.getClass().getDeclaredField("CMD_RESET_SIM_NETWORKS");
                declaredField.setAccessible(true);
                iIntValue = ((Integer) declaredField.get(MtkEapSimUtility.mWifiStateMachine)).intValue();
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
                iIntValue = 0;
            }
            if ("ABSENT".equals(stringExtra)) {
                if (intExtra == 0 || -1 == intExtra) {
                    boolean unused = MtkEapSimUtility.mSim1Present = false;
                } else if (1 == intExtra) {
                    boolean unused2 = MtkEapSimUtility.mSim2Present = false;
                }
                Log.d(MtkEapSimUtility.TAG, "resetting networks because SIM" + intExtra + " was removed");
                MtkEapSimUtility.mWifiStateMachine.sendMessage(iIntValue, 0, intExtra);
                return;
            }
            if ("LOADED".equals(stringExtra)) {
                if (intExtra == 0 || -1 == intExtra) {
                    boolean unused3 = MtkEapSimUtility.mSim1Present = true;
                } else if (1 == intExtra) {
                    boolean unused4 = MtkEapSimUtility.mSim2Present = true;
                }
                Log.d(MtkEapSimUtility.TAG, "resetting networks because SIM" + intExtra + " was loaded");
                MtkEapSimUtility.mWifiStateMachine.sendMessage(iIntValue, 1, intExtra);
            }
        }
    }
}
