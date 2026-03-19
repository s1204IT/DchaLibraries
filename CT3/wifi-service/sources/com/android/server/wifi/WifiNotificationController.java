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
import android.net.wifi.IWifiManager;
import android.net.wifi.ScanResult;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.internal.telephony.ITelephony;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

final class WifiNotificationController {
    private static final int ICON_NETWORKS_AVAILABLE = 17303229;
    private static final int NUM_SCANS_BEFORE_ACTUALLY_SCANNING = 0;
    private static final String TAG = "WifiNotificationController";
    private final long NOTIFICATION_REPEAT_DELAY_MS;
    private final Context mContext;
    private FrameworkFacade mFrameworkFacade;
    private NetworkInfo mNetworkInfo;
    private Notification.Builder mNotificationBuilder;
    private boolean mNotificationEnabled;
    private NotificationEnabledSettingObserver mNotificationEnabledSettingObserver;
    private long mNotificationRepeatTime;
    private boolean mNotificationShown;
    private int mNumScansSinceNetworkStateChange;
    private final WifiStateMachine mWifiStateMachine;
    private boolean mWaitForScanResult = false;
    private boolean mShowReselectDialog = false;
    private volatile int mWifiState = 4;
    private NetworkInfo.DetailedState mDetailedState = NetworkInfo.DetailedState.IDLE;

    WifiNotificationController(Context context, Looper looper, WifiStateMachine wsm, FrameworkFacade framework, Notification.Builder builder) {
        this.mContext = context;
        this.mWifiStateMachine = wsm;
        this.mFrameworkFacade = framework;
        this.mNotificationBuilder = builder;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mContext.registerReceiver(new BroadcastReceiver() {

            private static final int[] f4androidnetNetworkInfo$DetailedStateSwitchesValues = null;
            final int[] $SWITCH_TABLE$android$net$NetworkInfo$DetailedState;

            private static int[] m135getandroidnetNetworkInfo$DetailedStateSwitchesValues() {
                if (f4androidnetNetworkInfo$DetailedStateSwitchesValues != null) {
                    return f4androidnetNetworkInfo$DetailedStateSwitchesValues;
                }
                int[] iArr = new int[NetworkInfo.DetailedState.values().length];
                try {
                    iArr[NetworkInfo.DetailedState.AUTHENTICATING.ordinal()] = 4;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.BLOCKED.ordinal()] = 5;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK.ordinal()] = 1;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.CONNECTED.ordinal()] = 2;
                } catch (NoSuchFieldError e4) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.CONNECTING.ordinal()] = 6;
                } catch (NoSuchFieldError e5) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.DISCONNECTED.ordinal()] = 3;
                } catch (NoSuchFieldError e6) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.DISCONNECTING.ordinal()] = 7;
                } catch (NoSuchFieldError e7) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.FAILED.ordinal()] = 8;
                } catch (NoSuchFieldError e8) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.IDLE.ordinal()] = 9;
                } catch (NoSuchFieldError e9) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.OBTAINING_IPADDR.ordinal()] = 10;
                } catch (NoSuchFieldError e10) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.SCANNING.ordinal()] = 11;
                } catch (NoSuchFieldError e11) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.SUSPENDED.ordinal()] = 12;
                } catch (NoSuchFieldError e12) {
                }
                try {
                    iArr[NetworkInfo.DetailedState.VERIFYING_POOR_LINK.ordinal()] = 13;
                } catch (NoSuchFieldError e13) {
                }
                f4androidnetNetworkInfo$DetailedStateSwitchesValues = iArr;
                return iArr;
            }

            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    WifiNotificationController.this.mWifiState = intent.getIntExtra("wifi_state", 4);
                    WifiNotificationController.this.resetNotification();
                    WifiNotificationController.this.mWaitForScanResult = false;
                    WifiNotificationController.this.mShowReselectDialog = false;
                    return;
                }
                if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                    WifiNotificationController.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    NetworkInfo.DetailedState detailedState = WifiNotificationController.this.mNetworkInfo.getDetailedState();
                    if (detailedState == NetworkInfo.DetailedState.SCANNING || detailedState == WifiNotificationController.this.mDetailedState) {
                        return;
                    }
                    WifiNotificationController.this.mDetailedState = detailedState;
                    switch (m135getandroidnetNetworkInfo$DetailedStateSwitchesValues()[WifiNotificationController.this.mDetailedState.ordinal()]) {
                        case 1:
                        case 3:
                            break;
                        case 2:
                            WifiNotificationController.this.mWaitForScanResult = false;
                            break;
                        default:
                            return;
                    }
                    WifiNotificationController.this.resetNotification();
                    return;
                }
                if (!intent.getAction().equals("android.net.wifi.SCAN_RESULTS")) {
                    return;
                }
                WifiNotificationController.this.mShowReselectDialog = intent.getBooleanExtra("SHOW_RESELECT_DIALOG", false);
                WifiNotificationController.this.checkAndSetNotification(WifiNotificationController.this.mNetworkInfo, WifiNotificationController.this.mWifiStateMachine.syncGetScanResultsList());
            }
        }, filter);
        this.NOTIFICATION_REPEAT_DELAY_MS = ((long) this.mFrameworkFacade.getIntegerSetting(context, "wifi_networks_available_repeat_delay", 900)) * 1000;
        this.mNotificationEnabledSettingObserver = new NotificationEnabledSettingObserver(new Handler(looper));
        this.mNotificationEnabledSettingObserver.register();
    }

    private synchronized void checkAndSetNotification(NetworkInfo networkInfo, List<ScanResult> scanResults) {
        if (this.mNotificationEnabled) {
            if (this.mWifiState != 3) {
                return;
            }
            NetworkInfo.State state = NetworkInfo.State.DISCONNECTED;
            if (networkInfo != null) {
                state = networkInfo.getState();
            }
            if (this.mWifiStateMachine.hasCustomizedAutoConnect()) {
                Slog.i(TAG, "checkAndSetNotification, mWaitForScanResult:" + this.mWaitForScanResult);
                if (this.mWaitForScanResult && scanResults == null) {
                    showSwitchDialog();
                }
            }
            Slog.i(TAG, "checkAndSetNotification, state:" + state);
            if ((state == NetworkInfo.State.DISCONNECTED || state == NetworkInfo.State.UNKNOWN) && scanResults != null) {
                int numOpenNetworks = 0;
                for (int i = scanResults.size() - 1; i >= 0; i--) {
                    ScanResult scanResult = scanResults.get(i);
                    if (scanResult.capabilities != null && scanResult.capabilities.equals("[ESS]")) {
                        numOpenNetworks++;
                    }
                }
                IBinder binder = ServiceManager.getService("wifi");
                IWifiManager wifiService = IWifiManager.Stub.asInterface(binder);
                int networkId = -1;
                if (wifiService != null) {
                    try {
                        networkId = wifiService.syncGetConnectingNetworkId();
                    } catch (RemoteException e) {
                        Slog.d(TAG, "syncGetConnectingNetworkId failed!");
                    }
                }
                boolean isConnecting = this.mWifiStateMachine.isWifiConnecting(networkId);
                Slog.d(TAG, "Connecting networkId:" + networkId + ", isConnecting:" + isConnecting);
                if (this.mWifiStateMachine.hasCustomizedAutoConnect()) {
                    if (isConnecting) {
                        return;
                    }
                    if (this.mWaitForScanResult) {
                        showSwitchDialog();
                    }
                }
                Slog.i(TAG, "Open network num:" + numOpenNetworks);
                if (numOpenNetworks > 0) {
                    int i2 = this.mNumScansSinceNetworkStateChange + 1;
                    this.mNumScansSinceNetworkStateChange = i2;
                    if (i2 >= 0) {
                        if (BenesseExtension.getDchaState() == 0) {
                            setNotificationVisible(true, numOpenNetworks, false, 0);
                        } else {
                            setNotificationVisible(false, 0, false, 0);
                        }
                    }
                    return;
                }
            }
            setNotificationVisible(false, 0, false, 0);
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
                Slog.d(TAG, "cancel notification");
                notificationManager.cancelAsUser(null, 17303229, UserHandle.ALL);
            } else {
                if (System.currentTimeMillis() < this.mNotificationRepeatTime) {
                    return;
                }
                if (this.mNotificationBuilder == null) {
                    this.mNotificationBuilder = new Notification.Builder(this.mContext).setWhen(0L).setSmallIcon(17303229).setAutoCancel(true).setContentIntent(TaskStackBuilder.create(this.mContext).addNextIntentWithParentStack(new Intent("android.net.wifi.PICK_WIFI_NETWORK")).getPendingIntent(0, 0, null, UserHandle.CURRENT)).setColor(this.mContext.getResources().getColor(R.color.system_accent3_600));
                }
                CharSequence title = this.mContext.getResources().getQuantityText(R.plurals.wifi_available, numNetworks);
                CharSequence details = this.mContext.getResources().getQuantityText(R.plurals.wifi_available_detailed, numNetworks);
                this.mNotificationBuilder.setTicker(title);
                this.mNotificationBuilder.setContentTitle(title);
                this.mNotificationBuilder.setContentText(details);
                this.mNotificationRepeatTime = System.currentTimeMillis() + this.NOTIFICATION_REPEAT_DELAY_MS;
                notificationManager.notifyAsUser(null, 17303229, this.mNotificationBuilder.build(), UserHandle.CURRENT);
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
            return WifiNotificationController.this.mFrameworkFacade.getIntegerSetting(WifiNotificationController.this.mContext, "wifi_networks_available_notification_on", 1) == 1;
        }
    }

    private boolean isDataAvailable() {
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
            if (phone == null || !phone.isRadioOn(this.mContext.getPackageName()) || tm == null) {
                return false;
            }
            boolean isSim1Insert = tm.hasIccCard(0);
            boolean isSim2Insert = false;
            if (TelephonyManager.getDefault().getPhoneCount() >= 2) {
                isSim2Insert = tm.hasIccCard(1);
            }
            return isSim1Insert || isSim2Insert;
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get phone service, error:" + e);
            return false;
        }
    }

    private void showSwitchDialog() {
        this.mWaitForScanResult = false;
        boolean isDataAvailable = isDataAvailable();
        Slog.d(TAG, "showSwitchDialog, isDataAvailable:" + isDataAvailable + ", mShowReselectDialog:" + this.mShowReselectDialog);
        if (this.mShowReselectDialog || !isDataAvailable) {
            return;
        }
        Intent intent = new Intent("android.intent.action_WIFI_FAILOVER_GPRS_DIALOG");
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void setWaitForScanResult(boolean value) {
        this.mWaitForScanResult = value;
    }
}
