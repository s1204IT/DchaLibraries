package com.mediatek.internal.telephony.dataconnection;

import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.os.Bundle;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DcTracker;
import java.util.ArrayList;

public class MtkApnContext extends ApnContext {
    private static final String SLOG_TAG = "MtkApnContext";
    private boolean mNeedNotify;
    private ArrayList<ApnSetting> mWifiApns;

    public MtkApnContext(Phone phone, String str, String str2, NetworkConfig networkConfig, DcTracker dcTracker) {
        super(phone, str, str2, networkConfig, dcTracker);
        this.mWifiApns = null;
        this.mNeedNotify = needNotifyType(str);
    }

    public synchronized void setWifiApns(ArrayList<ApnSetting> arrayList) {
        this.mWifiApns = arrayList;
    }

    public synchronized ArrayList<ApnSetting> getWifiApns() {
        return this.mWifiApns;
    }

    public void setEnabled(boolean z) {
        super.setEnabled(z);
        this.mNeedNotify = true;
    }

    private static int apnIdForTypeEx(int i) {
        if (i == 21) {
            return 10;
        }
        switch (i) {
            case 25:
                return 11;
            case 26:
                return 12;
            case 27:
                return 13;
            case 28:
                return 14;
            case 29:
                return 15;
            default:
                return -1;
        }
    }

    private static Bundle apnIdForNetworkRequestEx(NetworkCapabilities networkCapabilities, int i, boolean z) {
        if (networkCapabilities.hasCapability(10)) {
            if (i != -1) {
                z = true;
            }
            i = 9;
        }
        if (networkCapabilities.hasCapability(25)) {
            if (i != -1) {
                z = true;
            }
            i = 10;
        }
        if (networkCapabilities.hasCapability(9)) {
            if (i != -1) {
                z = true;
            }
            i = 11;
        }
        if (networkCapabilities.hasCapability(8)) {
            if (i != -1) {
                z = true;
            }
            i = 12;
        }
        if (networkCapabilities.hasCapability(27)) {
            if (i != -1) {
                z = true;
            }
            i = 13;
        }
        if (networkCapabilities.hasCapability(26)) {
            if (i != -1) {
                z = true;
            }
            i = 14;
        }
        if (networkCapabilities.hasCapability(28)) {
            if (i != -1) {
                z = true;
            }
            i = 15;
        }
        Bundle bundle = new Bundle();
        bundle.putInt("apnId", i);
        bundle.putBoolean("error", z);
        return bundle;
    }

    private static int apnIdForApnNameEx(String str) {
        switch (str) {
            case "wap":
                return 10;
            case "xcap":
                return 11;
            case "rcs":
                return 12;
            case "bip":
                return 13;
            case "vsim":
                return 14;
            case "preempt":
                return 15;
            default:
                return -1;
        }
    }

    private boolean needNotifyType(String str) {
        if (str.equals("wap") || str.equals("xcap") || str.equals("rcs") || str.equals("bip") || str.equals("vsim")) {
            return false;
        }
        return true;
    }

    public boolean isNeedNotify() {
        return this.mNeedNotify;
    }

    public synchronized String toString() {
        return super.toString() + " mWifiApns={" + this.mWifiApns + "}";
    }
}
