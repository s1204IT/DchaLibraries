package com.android.mms.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.ArrayMap;
import android.util.Log;
import java.util.List;
import java.util.Map;

public class MmsConfigManager {
    private static volatile MmsConfigManager sInstance = new MmsConfigManager();
    private Context mContext;
    private SubscriptionManager mSubscriptionManager;
    private final Map<Integer, MmsConfig> mSubIdConfigMap = new ArrayMap();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("MmsService", "mReceiver action: " + action);
            if (action.equals("LOADED")) {
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
        IntentFilter intentFilterLoaded = new IntentFilter("LOADED");
        context.registerReceiver(this.mReceiver, intentFilterLoaded);
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
    }

    private void loadInBackground() {
        new Thread() {
            @Override
            public void run() {
                Configuration configuration = MmsConfigManager.this.mContext.getResources().getConfiguration();
                Log.i("MmsService", "MmsConfigManager.loadInBackground(): mcc/mnc: " + configuration.mcc + "/" + configuration.mnc);
                MmsConfigManager.this.load(MmsConfigManager.this.mContext);
            }
        }.start();
    }

    public MmsConfig getMmsConfigBySubId(int subId) {
        MmsConfig mmsConfig;
        synchronized (this.mSubIdConfigMap) {
            mmsConfig = this.mSubIdConfigMap.get(Integer.valueOf(subId));
        }
        Log.i("MmsService", "getMmsConfigBySubId -- for sub: " + subId + " mmsConfig: " + mmsConfig);
        return mmsConfig;
    }

    private void load(Context context) {
        List<SubscriptionInfo> subs = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subs == null || subs.size() < 1) {
            Log.e("MmsService", "MmsConfigManager.load -- empty getActiveSubInfoList");
            return;
        }
        Map<Integer, MmsConfig> newConfigMap = new ArrayMap<>();
        for (SubscriptionInfo sub : subs) {
            Configuration configuration = new Configuration();
            if (sub.getMcc() == 0 && sub.getMnc() == 0) {
                Configuration config = this.mContext.getResources().getConfiguration();
                configuration.mcc = config.mcc;
                configuration.mnc = config.mnc;
                Log.i("MmsService", "MmsConfigManager.load -- no mcc/mnc for sub: " + sub + " using mcc/mnc from main context: " + configuration.mcc + "/" + configuration.mnc);
            } else {
                Log.i("MmsService", "MmsConfigManager.load -- mcc/mnc for sub: " + sub);
                configuration.mcc = sub.getMcc();
                configuration.mnc = sub.getMnc();
            }
            Context subContext = context.createConfigurationContext(configuration);
            int subId = sub.getSubscriptionId();
            newConfigMap.put(Integer.valueOf(subId), new MmsConfig(subContext, subId));
        }
        synchronized (this.mSubIdConfigMap) {
            this.mSubIdConfigMap.clear();
            this.mSubIdConfigMap.putAll(newConfigMap);
        }
    }
}
