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
    private static final HashMap<Integer, Integer> sApnPriorityMap = new HashMap<>();
    public final int apnId;
    public final NetworkRequest networkRequest;
    public final int priority;

    public DcRequest(NetworkRequest nr, Context context) {
        initApnPriorities(context);
        this.networkRequest = nr;
        this.apnId = apnIdForNetworkRequest(this.networkRequest);
        this.priority = priorityForApnId(this.apnId);
    }

    public String toString() {
        return this.networkRequest.toString() + ", priority=" + this.priority + ", apnId=" + this.apnId;
    }

    public int hashCode() {
        return this.networkRequest.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof DcRequest) {
            return this.networkRequest.equals(((DcRequest) o).networkRequest);
        }
        return false;
    }

    @Override
    public int compareTo(DcRequest o) {
        return o.priority - this.priority;
    }

    private int apnIdForNetworkRequest(NetworkRequest nr) {
        NetworkCapabilities nc = nr.networkCapabilities;
        if (nc.getTransportTypes().length > 0 && !nc.hasTransport(0)) {
            return -1;
        }
        int apnId = -1;
        if (nc.hasCapability(12)) {
            apnId = 0;
        }
        if (nc.hasCapability(0)) {
            error = apnId != -1;
            apnId = 1;
        }
        if (nc.hasCapability(1)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 2;
        }
        if (nc.hasCapability(2)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 3;
        }
        if (nc.hasCapability(3)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 6;
        }
        if (nc.hasCapability(4)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 5;
        }
        if (nc.hasCapability(5)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 7;
        }
        if (nc.hasCapability(7)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 8;
        }
        if (nc.hasCapability(10)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 9;
        }
        if (nc.hasCapability(20)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 10;
        }
        if (nc.hasCapability(21)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 11;
        }
        if (nc.hasCapability(22)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 12;
        }
        if (nc.hasCapability(23)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 13;
        }
        if (nc.hasCapability(25)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 14;
        }
        if (nc.hasCapability(9)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 15;
        }
        if (nc.hasCapability(8)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 16;
        }
        if (nc.hasCapability(27)) {
            if (apnId != -1) {
                error = true;
            }
            apnId = 17;
        }
        if (error) {
            loge("Multiple apn types specified in request - result is unspecified!");
        }
        if (apnId == -1) {
            loge("Unsupported NetworkRequest in Telephony: nr=" + nr);
        }
        return apnId;
    }

    private void initApnPriorities(Context context) {
        synchronized (sApnPriorityMap) {
            if (sApnPriorityMap.isEmpty()) {
                String[] networkConfigStrings = context.getResources().getStringArray(R.array.config_ambientThresholdLevels);
                for (String networkConfigString : networkConfigStrings) {
                    NetworkConfig networkConfig = new NetworkConfig(networkConfigString);
                    int apnId = ApnContext.apnIdForType(networkConfig.type);
                    sApnPriorityMap.put(Integer.valueOf(apnId), Integer.valueOf(networkConfig.priority));
                }
            }
        }
    }

    private int priorityForApnId(int apnId) {
        Integer priority = sApnPriorityMap.get(Integer.valueOf(apnId));
        if (priority != null) {
            return priority.intValue();
        }
        return 0;
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }
}
