package com.android.server.wifi.scanner;

import android.util.Log;
import com.android.server.wifi.WifiNative;

public class WificondChannelHelper extends KnownBandsChannelHelper {
    private static final String TAG = "WificondChannelHelper";
    private final WifiNative mWifiNative;

    public WificondChannelHelper(WifiNative wifiNative) {
        this.mWifiNative = wifiNative;
        int[] iArr = new int[0];
        setBandChannels(iArr, iArr, iArr);
        updateChannels();
    }

    @Override
    public void updateChannels() {
        int[] channelsForBand = this.mWifiNative.getChannelsForBand(1);
        if (channelsForBand == null) {
            Log.e(TAG, "Failed to get channels for 2.4GHz band");
        }
        int[] channelsForBand2 = this.mWifiNative.getChannelsForBand(2);
        if (channelsForBand2 == null) {
            Log.e(TAG, "Failed to get channels for 5GHz band");
        }
        int[] channelsForBand3 = this.mWifiNative.getChannelsForBand(4);
        if (channelsForBand3 == null) {
            Log.e(TAG, "Failed to get channels for 5GHz DFS only band");
        }
        if (channelsForBand == null || channelsForBand2 == null || channelsForBand3 == null) {
            Log.e(TAG, "Failed to get all channels for band, not updating band channel lists");
        } else if (channelsForBand.length > 0 || channelsForBand2.length > 0 || channelsForBand3.length > 0) {
            setBandChannels(channelsForBand, channelsForBand2, channelsForBand3);
        } else {
            Log.e(TAG, "Got zero length for all channel lists");
        }
    }
}
