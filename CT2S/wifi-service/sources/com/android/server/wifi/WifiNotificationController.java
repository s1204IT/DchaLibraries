package com.android.server.wifi;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

final class WifiNotificationController {
    private static final int ICON_NETWORKS_AVAILABLE = 17303120;
    private static final int NUM_SCANS_BEFORE_ACTUALLY_SCANNING = 3;
    private final long NOTIFICATION_REPEAT_DELAY_MS;
    private final Context mContext;
    private NetworkInfo mNetworkInfo;
    private Notification mNotification;
    private boolean mNotificationEnabled;
    private NotificationEnabledSettingObserver mNotificationEnabledSettingObserver;
    private long mNotificationRepeatTime;
    private boolean mNotificationShown;
    private int mNumScansSinceNetworkStateChange;
    private volatile int mWifiState = 4;
    private final WifiStateMachine mWifiStateMachine;

    WifiNotificationController(Context context, WifiStateMachine wsm) {
        this.mContext = context;
        this.mWifiStateMachine = wsm;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    WifiNotificationController.this.mWifiState = intent.getIntExtra("wifi_state", 4);
                    WifiNotificationController.this.resetNotification();
                } else {
                    if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                        WifiNotificationController.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        switch (AnonymousClass2.$SwitchMap$android$net$NetworkInfo$DetailedState[WifiNotificationController.this.mNetworkInfo.getDetailedState().ordinal()]) {
                            case 1:
                            case 2:
                            case 3:
                                WifiNotificationController.this.resetNotification();
                                break;
                        }
                    }
                    if (intent.getAction().equals("android.net.wifi.SCAN_RESULTS")) {
                        WifiNotificationController.this.checkAndSetNotification(WifiNotificationController.this.mNetworkInfo, WifiNotificationController.this.mWifiStateMachine.syncGetScanResultsList());
                    }
                }
            }
        }, filter);
        this.NOTIFICATION_REPEAT_DELAY_MS = ((long) Settings.Global.getInt(context.getContentResolver(), "wifi_networks_available_repeat_delay", 900)) * 1000;
        this.mNotificationEnabledSettingObserver = new NotificationEnabledSettingObserver(new Handler());
        this.mNotificationEnabledSettingObserver.register();
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$android$net$NetworkInfo$DetailedState = new int[NetworkInfo.DetailedState.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.DISCONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private synchronized void checkAndSetNotification(NetworkInfo networkInfo, List<ScanResult> scanResults) {
        if (this.mNotificationEnabled && this.mWifiState == 3) {
            NetworkInfo.State state = NetworkInfo.State.DISCONNECTED;
            if (networkInfo != null) {
                state = networkInfo.getState();
            }
            if ((state == NetworkInfo.State.DISCONNECTED || state == NetworkInfo.State.UNKNOWN) && scanResults != null) {
                int numOpenNetworks = 0;
                for (int i = scanResults.size() - 1; i >= 0; i--) {
                    ScanResult scanResult = scanResults.get(i);
                    if (scanResult.capabilities != null && scanResult.capabilities.equals("[ESS]")) {
                        numOpenNetworks++;
                    }
                }
                if (numOpenNetworks > 0) {
                    int i2 = this.mNumScansSinceNetworkStateChange + 1;
                    this.mNumScansSinceNetworkStateChange = i2;
                    if (i2 >= 3) {
                        int dcha_state = BenesseExtension.getDchaState();
                        if (dcha_state != 0) {
                            setNotificationVisible(false, 0, false, 0);
                        } else {
                            setNotificationVisible(true, numOpenNetworks, false, 0);
                        }
                    }
                } else {
                    setNotificationVisible(false, 0, false, 0);
                }
            }
        }
    }

    private synchronized void resetNotification() {
        this.mNotificationRepeatTime = 0L;
        this.mNumScansSinceNetworkStateChange = 0;
        setNotificationVisible(false, 0, false, 0);
    }

    private void setNotificationVisible(boolean visible, int numNetworks, boolean force, int delay) {
        if (visible || this.mNotificationShown || force) {
            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            if (!visible) {
                notificationManager.cancelAsUser(null, 17303120, UserHandle.ALL);
            } else if (System.currentTimeMillis() >= this.mNotificationRepeatTime) {
                if (this.mNotification == null) {
                    this.mNotification = new Notification();
                    this.mNotification.when = 0L;
                    this.mNotification.icon = 17303120;
                    this.mNotification.flags = 16;
                    this.mNotification.contentIntent = TaskStackBuilder.create(this.mContext).addNextIntentWithParentStack(new Intent("android.net.wifi.PICK_WIFI_NETWORK")).getPendingIntent(0, 0, null, UserHandle.CURRENT);
                }
                CharSequence title = this.mContext.getResources().getQuantityText(R.plurals.duration_days_relative_future, numNetworks);
                CharSequence details = this.mContext.getResources().getQuantityText(R.plurals.duration_years_relative_future, numNetworks);
                this.mNotification.tickerText = title;
                this.mNotification.color = this.mContext.getResources().getColor(R.color.system_accent3_600);
                this.mNotification.setLatestEventInfo(this.mContext, title, details, this.mNotification.contentIntent);
                this.mNotificationRepeatTime = System.currentTimeMillis() + this.NOTIFICATION_REPEAT_DELAY_MS;
                notificationManager.notifyAsUser(null, 17303120, this.mNotification, UserHandle.ALL);
            } else {
                return;
            }
            this.mNotificationShown = visible;
        }
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("mNotificationEnabled " + this.mNotificationEnabled);
        pw.println("mNotificationRepeatTime " + this.mNotificationRepeatTime);
        pw.println("mNotificationShown " + this.mNotificationShown);
        pw.println("mNumScansSinceNetworkStateChange " + this.mNumScansSinceNetworkStateChange);
    }

    private class NotificationEnabledSettingObserver extends ContentObserver {
        public NotificationEnabledSettingObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            ContentResolver cr = WifiNotificationController.this.mContext.getContentResolver();
            cr.registerContentObserver(Settings.Global.getUriFor("wifi_networks_available_notification_on"), true, this);
            synchronized (WifiNotificationController.this) {
                WifiNotificationController.this.mNotificationEnabled = getValue();
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            synchronized (WifiNotificationController.this) {
                WifiNotificationController.this.mNotificationEnabled = getValue();
                WifiNotificationController.this.resetNotification();
            }
        }

        private boolean getValue() {
            return Settings.Global.getInt(WifiNotificationController.this.mContext.getContentResolver(), "wifi_networks_available_notification_on", 1) == 1;
        }
    }
}
