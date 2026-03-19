package com.android.internal.telephony.dataconnection;

import android.R;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkRequest;
import android.telephony.Rlog;
import java.util.HashMap;

public class DcRequest implements Comparable<DcRequest> {
    private static final String LOG_TAG = "DcRequest";
    protected static final HashMap<Integer, Integer> sApnPriorityMap = new HashMap<>();
    public final int apnId;
    public final NetworkRequest networkRequest;
    public final int priority;

    public DcRequest(NetworkRequest networkRequest, Context context) {
        initApnPriorities(context);
        this.networkRequest = networkRequest;
        this.apnId = apnIdForNetworkRequest(this.networkRequest);
        this.priority = priorityForApnId(this.apnId);
    }

    public String toString() {
        return this.networkRequest.toString() + ", priority=" + this.priority + ", apnId=" + this.apnId;
    }

    public int hashCode() {
        return this.networkRequest.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof DcRequest) {
            return this.networkRequest.equals(((DcRequest) obj).networkRequest);
        }
        return false;
    }

    @Override
    public int compareTo(DcRequest dcRequest) {
        return dcRequest.priority - this.priority;
    }

    protected int apnIdForNetworkRequest(NetworkRequest networkRequest) {
        NetworkCapabilities networkCapabilities = networkRequest.networkCapabilities;
        if (networkCapabilities.getTransportTypes().length > 0 && !networkCapabilities.hasTransport(0)) {
            return -1;
        }
        int i = networkCapabilities.hasCapability(12) ? 0 : -1;
        if (networkCapabilities.hasCapability(0)) {
            z = i != -1;
            i = 1;
        }
        if (networkCapabilities.hasCapability(1)) {
            if (i != -1) {
                z = true;
            }
            i = 2;
        }
        if (networkCapabilities.hasCapability(2)) {
            if (i != -1) {
                z = true;
            }
            i = 3;
        }
        if (networkCapabilities.hasCapability(3)) {
            if (i != -1) {
                z = true;
            }
            i = 6;
        }
        if (networkCapabilities.hasCapability(4)) {
            if (i != -1) {
                z = true;
            }
            i = 5;
        }
        if (networkCapabilities.hasCapability(5)) {
            if (i != -1) {
                z = true;
            }
            i = 7;
        }
        if (networkCapabilities.hasCapability(7)) {
            if (i != -1) {
                z = true;
            }
            i = 8;
        }
        if (networkCapabilities.hasCapability(8)) {
            if (i != -1) {
                z = true;
            }
            loge("RCS APN type not yet supported");
            i = -1;
        }
        if (networkCapabilities.hasCapability(9)) {
            if (i != -1) {
                z = true;
            }
            loge("XCAP APN type not yet supported");
            i = -1;
        }
        if (networkCapabilities.hasCapability(10)) {
            if (i != -1) {
                z = true;
            }
            i = 9;
        }
        if (z) {
            loge("Multiple apn types specified in request - result is unspecified!");
        }
        if (i == -1) {
            loge("Unsupported NetworkRequest in Telephony: nr=" + networkRequest);
        }
        return i;
    }

    protected void initApnPriorities(Context context) {
        synchronized (sApnPriorityMap) {
            if (sApnPriorityMap.isEmpty()) {
                for (String str : context.getResources().getStringArray(R.array.config_displayWhiteBalanceHighLightAmbientBrightnesses)) {
                    NetworkConfig networkConfig = new NetworkConfig(str);
                    sApnPriorityMap.put(Integer.valueOf(ApnContext.apnIdForType(networkConfig.type)), Integer.valueOf(networkConfig.priority));
                }
            }
        }
    }

    private int priorityForApnId(int i) {
        Integer num = sApnPriorityMap.get(Integer.valueOf(i));
        if (num != null) {
            return num.intValue();
        }
        return 0;
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }
}
