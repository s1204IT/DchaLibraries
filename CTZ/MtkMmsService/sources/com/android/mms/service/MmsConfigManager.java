package com.android.mms.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.ArrayMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MmsConfigManager {
    private static volatile MmsConfigManager sInstance = new MmsConfigManager();
    private Context mContext;
    private SubscriptionManager mSubscriptionManager;
    private final Map<Integer, Bundle> mSubIdConfigMap = new ArrayMap();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.i("MmsConfigManager receiver action: " + action);
            if (action.equals("android.intent.action.SIM_STATE_CHANGED") || action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                MmsConfigManager.this.loadInBackground();
            }
        }
    };
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            MmsConfigManager.this.loadInBackground();
        }
    };

    public static MmsConfigManager getInstance() {
        return sInstance;
    }

    public void init(Context context) {
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(context);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(this.mReceiver, intentFilter);
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
    }

    private void loadInBackground() {
        new Thread() {
            @Override
            public void run() {
                Configuration configuration = MmsConfigManager.this.mContext.getResources().getConfiguration();
                LogUtil.i("MmsConfigManager loads in background mcc/mnc: " + configuration.mcc + "/" + configuration.mnc);
                MmsConfigManager.this.load(MmsConfigManager.this.mContext);
            }
        }.start();
    }

    public Bundle getMmsConfigBySubId(int i) {
        Bundle bundle;
        synchronized (this.mSubIdConfigMap) {
            bundle = this.mSubIdConfigMap.get(Integer.valueOf(i));
        }
        if (bundle != null) {
            return new Bundle(bundle);
        }
        LogUtil.d("mms config for sub " + i + ": null!!!");
        return null;
    }

    private void load(Context context) {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size() < 1) {
            LogUtil.e(" Failed to load mms config: empty getActiveSubInfoList");
            return;
        }
        ArrayMap arrayMap = new ArrayMap();
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
        while (it.hasNext()) {
            int subscriptionId = it.next().getSubscriptionId();
            arrayMap.put(Integer.valueOf(subscriptionId), SmsManager.getMmsConfig(carrierConfigManager.getConfigForSubId(subscriptionId)));
        }
        synchronized (this.mSubIdConfigMap) {
            this.mSubIdConfigMap.clear();
            this.mSubIdConfigMap.putAll(arrayMap);
        }
    }
}
