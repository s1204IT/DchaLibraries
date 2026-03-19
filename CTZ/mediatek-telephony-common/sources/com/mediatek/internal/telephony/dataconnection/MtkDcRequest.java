package com.mediatek.internal.telephony.dataconnection;

import android.R;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkRequest;
import android.os.SystemProperties;
import com.android.internal.telephony.dataconnection.DcRequest;

public class MtkDcRequest extends DcRequest {
    public MtkDcRequest(NetworkRequest networkRequest, Context context) {
        super(networkRequest, context);
    }

    protected int apnIdForNetworkRequest(NetworkRequest networkRequest) {
        NetworkCapabilities networkCapabilities = networkRequest.networkCapabilities;
        if (networkCapabilities.getTransportTypes().length > 0 && !networkCapabilities.hasTransport(0)) {
            return -1;
        }
        int iApnIdForNetworkRequest = super.apnIdForNetworkRequest(networkRequest);
        if (networkCapabilities.hasCapability(10)) {
            z = iApnIdForNetworkRequest != -1;
            iApnIdForNetworkRequest = 9;
        }
        if (networkCapabilities.hasCapability(25)) {
            if (iApnIdForNetworkRequest != -1) {
                z = true;
            }
            iApnIdForNetworkRequest = 10;
        }
        if (networkCapabilities.hasCapability(9)) {
            if (iApnIdForNetworkRequest != -1) {
                z = true;
            }
            iApnIdForNetworkRequest = 11;
        }
        if (networkCapabilities.hasCapability(8)) {
            if (iApnIdForNetworkRequest != -1) {
                z = true;
            }
            iApnIdForNetworkRequest = 12;
        }
        if (networkCapabilities.hasCapability(27)) {
            if (iApnIdForNetworkRequest != -1) {
                z = true;
            }
            iApnIdForNetworkRequest = 13;
        }
        if (networkCapabilities.hasCapability(26)) {
            if (iApnIdForNetworkRequest != -1) {
                z = true;
            }
            iApnIdForNetworkRequest = 14;
        }
        if (networkCapabilities.hasCapability(28)) {
            if (iApnIdForNetworkRequest != -1) {
                z = true;
            }
            iApnIdForNetworkRequest = 15;
        }
        if (z) {
            loge("Multiple apn types specified in request - result is unspecified!");
        }
        if (iApnIdForNetworkRequest == -1) {
            loge("Unsupported NetworkRequest in Telephony: nr=" + networkRequest);
        }
        return iApnIdForNetworkRequest;
    }

    protected void initApnPriorities(Context context) {
        synchronized (sApnPriorityMap) {
            if (sApnPriorityMap.isEmpty()) {
                for (String str : context.getResources().getStringArray(R.array.config_displayWhiteBalanceHighLightAmbientBrightnesses)) {
                    NetworkConfig networkConfig = new NetworkConfig(str);
                    int iApnIdForType = MtkApnContext.apnIdForType(networkConfig.type);
                    if ((SystemProperties.getInt("persist.vendor.mims_support", 1) > 1) && networkConfig.type == 15) {
                        loge("Force change emergency type APN priority to -1");
                        sApnPriorityMap.put(Integer.valueOf(iApnIdForType), -1);
                    } else {
                        sApnPriorityMap.put(Integer.valueOf(iApnIdForType), Integer.valueOf(networkConfig.priority));
                    }
                }
            }
        }
    }
}
