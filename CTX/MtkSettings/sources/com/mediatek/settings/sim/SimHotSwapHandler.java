package com.mediatek.settings.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import java.util.List;

public class SimHotSwapHandler {
    private Context mContext;
    private OnSimHotSwapListener mListener;
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SimHotSwapHandler.this.handleHotSwap();
        }
    };
    private List<SubscriptionInfo> mSubscriptionInfoList;
    private SubscriptionManager mSubscriptionManager;

    public interface OnSimHotSwapListener {
        void onSimHotSwap();
    }

    public SimHotSwapHandler(Context context) {
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        Log.d("SimHotSwapHandler", "handler=" + this + ", cacheList=" + this.mSubscriptionInfoList);
    }

    public void registerOnSimHotSwap(OnSimHotSwapListener onSimHotSwapListener) {
        if (this.mContext != null) {
            this.mContext.registerReceiver(this.mSubReceiver, new IntentFilter("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
            this.mListener = onSimHotSwapListener;
            Log.d("SimHotSwapHandler", "registerOnSimHotSwap, handler=" + this + ", listener=" + onSimHotSwapListener);
        }
    }

    public void unregisterOnSimHotSwap() {
        if (this.mContext != null) {
            this.mContext.unregisterReceiver(this.mSubReceiver);
            Log.d("SimHotSwapHandler", "unregisterOnSimHotSwap, handler=" + this);
        }
        this.mListener = null;
    }

    private void handleHotSwap() {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        Log.d("SimHotSwapHandler", "handleHotSwap, handler=" + this + ", currentSubIdList=" + activeSubscriptionInfoList);
        if (hasHotSwapHappened(this.mSubscriptionInfoList, activeSubscriptionInfoList) && this.mListener != null) {
            this.mListener.onSimHotSwap();
        }
    }

    private boolean hasHotSwapHappened(List<SubscriptionInfo> list, List<SubscriptionInfo> list2) {
        int size;
        int size2;
        boolean z = false;
        if (list != null) {
            size = list.size();
        } else {
            size = 0;
        }
        if (list2 != null) {
            size2 = list2.size();
        } else {
            size2 = 0;
        }
        if (size == 0 && size2 == 0) {
            return false;
        }
        if (size == 0 || size2 == 0 || list.size() != list2.size()) {
            Log.d("SimHotSwapHandler", "hasHotSwapHappened, SIM count is different, oriCount=" + size + ", curCount=" + size2);
            return true;
        }
        int i = 0;
        while (true) {
            if (i >= list2.size()) {
                break;
            }
            if (list2.get(i).getIccId().equals(list.get(i).getIccId())) {
                i++;
            } else {
                z = true;
                break;
            }
        }
        Log.d("SimHotSwapHandler", "hasHotSwapHappened, result=" + z);
        return z;
    }
}
