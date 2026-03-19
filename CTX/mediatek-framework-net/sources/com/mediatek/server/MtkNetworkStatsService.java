package com.mediatek.server;

import android.app.AlarmManager;
import android.content.Context;
import android.net.NetworkIdentity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.net.NetworkStatsObservers;
import com.android.server.net.NetworkStatsService;
import java.io.File;
import java.time.Clock;

public class MtkNetworkStatsService extends NetworkStatsService {
    static final int SUBSCRIPTION_OR_SIM_CHANGED = 0;
    private static final String TAG = MtkNetworkStatsService.class.getSimpleName();
    private final ArrayMap<String, Integer> mActiveSubscriberSubIdMap;
    private Context mContext;
    private long mEmGlobalAlert;
    private InternalHandler mHandler;
    private HandlerThread mHandlerThread;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener;

    public MtkNetworkStatsService(Context context, INetworkManagementService iNetworkManagementService, AlarmManager alarmManager, PowerManager.WakeLock wakeLock, Clock clock, TelephonyManager telephonyManager, NetworkStatsService.NetworkStatsSettings networkStatsSettings, NetworkStatsObservers networkStatsObservers, File file, File file2) {
        super(context, iNetworkManagementService, alarmManager, wakeLock, clock, telephonyManager, networkStatsSettings, networkStatsObservers, file, file2);
        this.mActiveSubscriberSubIdMap = new ArrayMap<>();
        this.mEmGlobalAlert = 2097152L;
        this.mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                MtkNetworkStatsService.this.mHandler.sendEmptyMessage(0);
            }
        };
        Slog.d(TAG, "MtkNetworkStatsService starting up");
        this.mContext = context;
        initDataUsageIntent(context);
    }

    private void initDataUsageIntent(Context context) {
        this.mHandlerThread = new HandlerThread("NetworkStatInternalHandler");
        this.mHandlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        SubscriptionManager.from(context).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
    }

    protected void rebuildActiveVilteIfaceMap() {
        rebuildActiveSubscriberSubIdMap(this.mActiveSubscriberSubIdMap);
    }

    protected boolean findOrCreateMultipleVilteNetworkIdentitySets(NetworkIdentity networkIdentity) {
        Integer num = this.mActiveSubscriberSubIdMap.get(networkIdentity.getSubscriberId());
        if (num != null) {
            int iIntValue = num.intValue();
            Slog.v(TAG, "Get subId=" + iIntValue + " for sbscriberId:" + networkIdentity.getSubscriberId());
            findOrCreateNetworkIdentitySet(this.mActiveIfaces, getVtInterface(iIntValue)).add(networkIdentity);
            findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, getVtInterface(iIntValue)).add(networkIdentity);
            return true;
        }
        Slog.e(TAG, "Get null entry for sbscriberId:" + networkIdentity.getSubscriberId());
        return true;
    }

    private String getVtInterface(int i) {
        return new String("vt_data0" + String.valueOf(i));
    }

    private void rebuildActiveSubscriberSubIdMap(ArrayMap<String, Integer> arrayMap) {
        arrayMap.clear();
        Slog.d(TAG, "rebuildActiveSubscriberSubIdMap");
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(this.mContext);
        for (int i : SubscriptionManager.from(this.mContext).getActiveSubscriptionIdList()) {
            String subscriberId = telephonyManagerFrom.getSubscriberId(i);
            arrayMap.put(subscriberId, new Integer(i));
            Slog.i(TAG, "activeSubscriberSubIdMap put:" + subscriberId + ":" + i);
        }
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                MtkNetworkStatsService.this.handleSimChange();
            }
        }
    }

    private void handleSimChange() {
        boolean zIsTestSim = isTestSim();
        this.mEmGlobalAlert = Settings.Global.getLong(this.mContext.getContentResolver(), "netstats_global_alert_bytes", 2251799813685248L);
        if (zIsTestSim) {
            if (this.mEmGlobalAlert != 2251799813685248L) {
                Settings.Global.putLong(this.mContext.getContentResolver(), "netstats_global_alert_bytes", 2251799813685248L);
                advisePersistThreshold(9223372036854775L);
                Slog.d(TAG, "Configure for test sim with 2TB");
                return;
            }
            return;
        }
        if (this.mEmGlobalAlert == 2251799813685248L) {
            Settings.Global.putLong(this.mContext.getContentResolver(), "netstats_global_alert_bytes", 2097152L);
            advisePersistThreshold(9223372036854775L);
            Slog.d(TAG, "Restore for test sim with 2MB");
        }
    }

    public static boolean isTestSim() {
        return SystemProperties.get("vendor.gsm.sim.ril.testsim").equals("1") || SystemProperties.get("vendor.gsm.sim.ril.testsim.2").equals("1") || SystemProperties.get("vendor.gsm.sim.ril.testsim.3").equals("1") || SystemProperties.get("vendor.gsm.sim.ril.testsim.4").equals("1");
    }
}
