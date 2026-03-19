package com.android.server.wifi.util;

import android.net.wifi.WifiConfiguration;
import android.util.Log;
import com.android.server.wifi.WifiNative;
import java.util.ArrayList;
import java.util.Random;

public class ApConfigUtil {
    public static final int DEFAULT_AP_BAND = 0;
    public static final int DEFAULT_AP_CHANNEL = 6;
    public static final int ERROR_GENERIC = 2;
    public static final int ERROR_NO_CHANNEL = 1;
    public static final int SUCCESS = 0;
    private static final String TAG = "ApConfigUtil";
    private static final Random sRandom = new Random();

    public static int convertFrequencyToChannel(int i) {
        if (i >= 2412 && i <= 2472) {
            return ((i - 2412) / 5) + 1;
        }
        if (i == 2484) {
            return 14;
        }
        if (i >= 5170 && i <= 5825) {
            return ((i - 5170) / 5) + 34;
        }
        return -1;
    }

    public static int chooseApChannel(int i, ArrayList<Integer> arrayList, int[] iArr) {
        if (i != 0 && i != 1 && i != -1) {
            Log.e(TAG, "Invalid band: " + i);
            return -1;
        }
        if (i == 0 || i == -1) {
            if (arrayList == null || arrayList.size() == 0) {
                Log.d(TAG, "2GHz allowed channel list not specified");
                return 6;
            }
            return arrayList.get(sRandom.nextInt(arrayList.size())).intValue();
        }
        if (iArr != null && iArr.length > 0) {
            return convertFrequencyToChannel(iArr[sRandom.nextInt(iArr.length)]);
        }
        Log.e(TAG, "No available channels on 5GHz band");
        return -1;
    }

    public static int updateApChannelConfig(WifiNative wifiNative, String str, ArrayList<Integer> arrayList, WifiConfiguration wifiConfiguration) {
        if (!wifiNative.isHalStarted()) {
            wifiConfiguration.apBand = 0;
            wifiConfiguration.apChannel = 6;
            return 0;
        }
        if (wifiConfiguration.apBand == 1 && str == null) {
            Log.e(TAG, "5GHz band is not allowed without country code");
            return 2;
        }
        if (wifiConfiguration.apChannel == 0) {
            wifiConfiguration.apChannel = chooseApChannel(wifiConfiguration.apBand, arrayList, wifiNative.getChannelsForBand(2));
            if (wifiConfiguration.apChannel == -1) {
                Log.e(TAG, "Failed to get available channel.");
                return 1;
            }
        }
        return 0;
    }
}
