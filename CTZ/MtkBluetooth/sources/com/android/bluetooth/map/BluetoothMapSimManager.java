package com.android.bluetooth.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import java.util.Iterator;
import java.util.List;

public class BluetoothMapSimManager {
    public static final long INVALID_SUBID = -1;
    private static final String TAG = "[MAP]BluetoothMapSimManager";
    private static SubscriptionManager sSubscriptionManager;
    private static TelephonyManager sTelephonyManager;
    private Context mContext;
    private int mSubCount;
    private List<SubscriptionInfo> mSubInfoList;
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                BluetoothMapSimManager.this.mSubInfoList = BluetoothMapSimManager.sSubscriptionManager.getActiveSubscriptionInfoList();
                BluetoothMapSimManager.this.mSubCount = (BluetoothMapSimManager.this.mSubInfoList == null || BluetoothMapSimManager.this.mSubInfoList.isEmpty()) ? 0 : BluetoothMapSimManager.this.mSubInfoList.size();
            }
        }
    };

    public void init(Context context) {
        this.mContext = context;
        sTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        sSubscriptionManager = SubscriptionManager.from(this.mContext);
        this.mSubInfoList = sSubscriptionManager.getActiveSubscriptionInfoList();
        this.mSubCount = (this.mSubInfoList == null || this.mSubInfoList.isEmpty()) ? 0 : this.mSubInfoList.size();
        this.mContext.registerReceiver(this.mSubReceiver, new IntentFilter("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
    }

    public void unregisterReceiver() {
        this.mContext.unregisterReceiver(this.mSubReceiver);
    }

    public int getSubCount() {
        return this.mSubCount;
    }

    public long getSingleSubId() {
        if (this.mSubInfoList != null && this.mSubInfoList.size() == 1) {
            return this.mSubInfoList.get(0).getSubscriptionId();
        }
        return -1L;
    }

    public List<SubscriptionInfo> getSimList() {
        return this.mSubInfoList;
    }

    public long getSimIdFromOriginator(String str) {
        if (this.mSubCount < 2) {
            return getSingleSubId();
        }
        for (int i = 0; i < this.mSubInfoList.size(); i++) {
            if (PhoneNumberUtils.compareLoosely(this.mSubInfoList.get(i).getNumber(), str)) {
                return this.mSubInfoList.get(i).getSubscriptionId();
            }
        }
        return -1L;
    }

    public static int getSubInfoNumber() {
        List<SubscriptionInfo> activeSubscriptionInfoList;
        if (sSubscriptionManager != null && (activeSubscriptionInfoList = sSubscriptionManager.getActiveSubscriptionInfoList()) != null) {
            return activeSubscriptionInfoList.size();
        }
        return 0;
    }

    public static long getFristSubID() {
        List<SubscriptionInfo> activeSubscriptionInfoList;
        if (sSubscriptionManager != null && (activeSubscriptionInfoList = sSubscriptionManager.getActiveSubscriptionInfoList()) != null && activeSubscriptionInfoList.size() > 0) {
            return activeSubscriptionInfoList.get(0).getSubscriptionId();
        }
        return 0L;
    }

    public static String getNumberBySubID(long j) {
        if (sTelephonyManager != null) {
            return sTelephonyManager.getLine1Number((int) j);
        }
        return "";
    }

    public static boolean isValidSubId(long j) {
        List<SubscriptionInfo> activeSubscriptionInfoList;
        if (sSubscriptionManager != null && (activeSubscriptionInfoList = sSubscriptionManager.getActiveSubscriptionInfoList()) != null) {
            Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
            while (it.hasNext()) {
                if (it.next().getSubscriptionId() == j) {
                    return true;
                }
            }
        }
        return false;
    }
}
