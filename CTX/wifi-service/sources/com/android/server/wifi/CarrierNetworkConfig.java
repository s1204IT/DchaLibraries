package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarrierNetworkConfig {
    private static final int CONFIG_ELEMENT_SIZE = 2;
    private static final Uri CONTENT_URI = Uri.parse("content://carrier_information/carrier");
    private static final int EAP_TYPE_INDEX = 1;
    private static final int ENCODED_SSID_INDEX = 0;
    private static final String NETWORK_CONFIG_SEPARATOR = ",";
    private static final String TAG = "CarrierNetworkConfig";
    private boolean mIsCarrierImsiEncryptionInfoAvailable = false;
    private final Map<String, NetworkInfo> mCarrierNetworkMap = new HashMap();

    public CarrierNetworkConfig(final Context context, Looper looper, FrameworkFacade frameworkFacade) {
        updateNetworkConfig(context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                CarrierNetworkConfig.this.updateNetworkConfig(context2);
            }
        }, intentFilter);
        frameworkFacade.registerContentObserver(context, CONTENT_URI, false, new ContentObserver(new Handler(looper)) {
            @Override
            public void onChange(boolean z) {
                CarrierNetworkConfig.this.updateNetworkConfig(context);
            }
        });
    }

    public boolean isCarrierNetwork(String str) {
        return this.mCarrierNetworkMap.containsKey(str);
    }

    public int getNetworkEapType(String str) {
        NetworkInfo networkInfo = this.mCarrierNetworkMap.get(str);
        if (networkInfo == null) {
            return -1;
        }
        return networkInfo.mEapType;
    }

    public String getCarrierName(String str) {
        NetworkInfo networkInfo = this.mCarrierNetworkMap.get(str);
        if (networkInfo == null) {
            return null;
        }
        return networkInfo.mCarrierName;
    }

    public boolean isCarrierEncryptionInfoAvailable() {
        return this.mIsCarrierImsiEncryptionInfoAvailable;
    }

    private boolean verifyCarrierImsiEncryptionInfoIsAvailable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        if (telephonyManager == null) {
            return false;
        }
        try {
            if (telephonyManager.getCarrierInfoForImsiEncryption(2) == null) {
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to get imsi encryption info: " + e.getMessage());
            return false;
        }
    }

    private static class NetworkInfo {
        final String mCarrierName;
        final int mEapType;

        NetworkInfo(int i, String str) {
            this.mEapType = i;
            this.mCarrierName = str;
        }
    }

    private void updateNetworkConfig(Context context) {
        SubscriptionManager subscriptionManager;
        List<SubscriptionInfo> activeSubscriptionInfoList;
        this.mIsCarrierImsiEncryptionInfoAvailable = verifyCarrierImsiEncryptionInfoIsAvailable(context);
        this.mCarrierNetworkMap.clear();
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        if (carrierConfigManager == null || (subscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service")) == null || (activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList()) == null) {
            return;
        }
        for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
            processNetworkConfig(carrierConfigManager.getConfigForSubId(subscriptionInfo.getSubscriptionId()), (subscriptionInfo.getDisplayName() == null ? "" : subscriptionInfo.getDisplayName()).toString());
        }
    }

    private void processNetworkConfig(PersistableBundle persistableBundle, String str) {
        String[] stringArray;
        if (persistableBundle == null || (stringArray = persistableBundle.getStringArray("carrier_wifi_string_array")) == null) {
            return;
        }
        for (String str2 : stringArray) {
            String[] strArrSplit = str2.split(NETWORK_CONFIG_SEPARATOR);
            if (strArrSplit.length != 2) {
                Log.e(TAG, "Ignore invalid config: " + str2);
            } else {
                try {
                    String str3 = new String(Base64.decode(strArrSplit[0], 0));
                    int eapType = parseEapType(Integer.parseInt(strArrSplit[1]));
                    if (eapType == -1) {
                        Log.e(TAG, "Invalid EAP type: " + strArrSplit[1]);
                    } else {
                        this.mCarrierNetworkMap.put(str3, new NetworkInfo(eapType, str));
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed to parse EAP type: " + e.getMessage());
                } catch (IllegalArgumentException e2) {
                    Log.e(TAG, "Failed to decode SSID: " + e2.getMessage());
                }
            }
        }
    }

    private static int parseEapType(int i) {
        if (i == 18) {
            return 4;
        }
        if (i == 23) {
            return 5;
        }
        if (i == 50) {
            return 6;
        }
        return -1;
    }
}
