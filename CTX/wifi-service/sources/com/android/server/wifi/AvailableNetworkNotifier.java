package com.android.server.wifi;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.SsidSetStoreData;
import com.android.server.wifi.util.ScanResultUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AvailableNetworkNotifier {

    @VisibleForTesting
    static final int DEFAULT_REPEAT_DELAY_SEC = 900;
    private static final int STATE_CONNECTED_NOTIFICATION = 3;
    private static final int STATE_CONNECTING_IN_NOTIFICATION = 2;
    private static final int STATE_CONNECT_FAILED_NOTIFICATION = 4;
    private static final int STATE_NO_NOTIFICATION = 0;
    private static final int STATE_SHOWING_RECOMMENDATION_NOTIFICATION = 1;
    private static final int TIME_TO_SHOW_CONNECTED_MILLIS = 5000;
    private static final int TIME_TO_SHOW_CONNECTING_MILLIS = 10000;
    private static final int TIME_TO_SHOW_FAILED_MILLIS = 5000;
    private final Clock mClock;
    private final WifiConfigManager mConfigManager;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final Handler mHandler;
    private final ConnectToNetworkNotificationBuilder mNotificationBuilder;
    private final long mNotificationRepeatDelay;
    private long mNotificationRepeatTime;
    private ScanResult mRecommendedNetwork;
    private boolean mSettingEnabled;
    private final Messenger mSrcMessenger;
    private final String mStoreDataIdentifier;
    private final int mSystemMessageNotificationId;
    private final String mTag;
    private final String mToggleSettingsName;
    private final WifiMetrics mWifiMetrics;
    private final WifiStateMachine mWifiStateMachine;
    private int mState = 0;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!AvailableNetworkNotifier.this.mTag.equals(intent.getExtra(ConnectToNetworkNotificationBuilder.AVAILABLE_NETWORK_NOTIFIER_TAG))) {
            }
            String action = intent.getAction();
            byte b = -1;
            int iHashCode = action.hashCode();
            if (iHashCode != -1692061185) {
                if (iHashCode != -1140661470) {
                    if (iHashCode != 303648504) {
                        if (iHashCode == 1260970165 && action.equals(ConnectToNetworkNotificationBuilder.ACTION_USER_DISMISSED_NOTIFICATION)) {
                            b = 0;
                        }
                    } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE)) {
                        b = 3;
                    }
                } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK)) {
                    b = 2;
                }
            } else if (action.equals(ConnectToNetworkNotificationBuilder.ACTION_CONNECT_TO_NETWORK)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    AvailableNetworkNotifier.this.handleUserDismissedAction();
                    break;
                case 1:
                    AvailableNetworkNotifier.this.handleConnectToNetworkAction();
                    break;
                case 2:
                    AvailableNetworkNotifier.this.handleSeeAllNetworksAction();
                    break;
                case 3:
                    AvailableNetworkNotifier.this.handlePickWifiNetworkAfterConnectFailure();
                    break;
                default:
                    Log.e(AvailableNetworkNotifier.this.mTag, "Unknown action " + intent.getAction());
                    break;
            }
        }
    };
    private final Handler.Callback mConnectionStateCallback = new Handler.Callback() {
        @Override
        public final boolean handleMessage(Message message) {
            return AvailableNetworkNotifier.lambda$new$0(this.f$0, message);
        }
    };
    private boolean mScreenOn = false;
    private final Set<String> mBlacklistedSsids = new ArraySet();

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    public AvailableNetworkNotifier(String str, String str2, String str3, int i, Context context, Looper looper, FrameworkFacade frameworkFacade, Clock clock, WifiMetrics wifiMetrics, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiStateMachine wifiStateMachine, ConnectToNetworkNotificationBuilder connectToNetworkNotificationBuilder) {
        this.mTag = str;
        this.mStoreDataIdentifier = str2;
        this.mToggleSettingsName = str3;
        this.mSystemMessageNotificationId = i;
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mFrameworkFacade = frameworkFacade;
        this.mWifiMetrics = wifiMetrics;
        this.mClock = clock;
        this.mConfigManager = wifiConfigManager;
        this.mWifiStateMachine = wifiStateMachine;
        this.mNotificationBuilder = connectToNetworkNotificationBuilder;
        this.mSrcMessenger = new Messenger(new Handler(looper, this.mConnectionStateCallback));
        wifiConfigStore.registerStoreData(new SsidSetStoreData(this.mStoreDataIdentifier, new AvailableNetworkNotifierStoreData()));
        this.mNotificationRepeatDelay = ((long) this.mFrameworkFacade.getIntegerSetting(context, "wifi_networks_available_repeat_delay", DEFAULT_REPEAT_DELAY_SEC)) * 1000;
        new NotificationEnabledSettingObserver(this.mHandler).register();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectToNetworkNotificationBuilder.ACTION_USER_DISMISSED_NOTIFICATION);
        intentFilter.addAction(ConnectToNetworkNotificationBuilder.ACTION_CONNECT_TO_NETWORK);
        intentFilter.addAction(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK);
        intentFilter.addAction(ConnectToNetworkNotificationBuilder.ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter, null, this.mHandler);
    }

    public static boolean lambda$new$0(AvailableNetworkNotifier availableNetworkNotifier, Message message) {
        switch (message.what) {
            case 151554:
                availableNetworkNotifier.handleConnectionAttemptFailedToSend();
                break;
            case 151555:
                break;
            default:
                Log.e("AvailableNetworkNotifier", "Unknown message " + message.what);
                break;
        }
        return true;
    }

    public void clearPendingNotification(boolean z) {
        if (z) {
            this.mNotificationRepeatTime = 0L;
        }
        if (this.mState != 0) {
            getNotificationManager().cancel(this.mSystemMessageNotificationId);
            if (this.mRecommendedNetwork != null) {
                Log.d(this.mTag, "Notification with state=" + this.mState + " was cleared for recommended network: " + this.mRecommendedNetwork.SSID);
            }
            this.mState = 0;
            this.mRecommendedNetwork = null;
        }
    }

    private boolean isControllerEnabled() {
        return this.mSettingEnabled && !UserManager.get(this.mContext).hasUserRestriction("no_config_wifi", UserHandle.CURRENT);
    }

    public void handleScanResults(List<ScanDetail> list) {
        if (!isControllerEnabled()) {
            clearPendingNotification(true);
            return;
        }
        if (list.isEmpty() && this.mState == 1) {
            clearPendingNotification(false);
            return;
        }
        if (this.mState == 0 && this.mClock.getWallClockMillis() < this.mNotificationRepeatTime) {
            return;
        }
        if (this.mState == 0 && !this.mScreenOn) {
            return;
        }
        if (this.mState == 0 || this.mState == 1) {
            ScanResult scanResultRecommendNetwork = recommendNetwork(list, new ArraySet(this.mBlacklistedSsids));
            if (scanResultRecommendNetwork != null) {
                postInitialNotification(scanResultRecommendNetwork);
            } else {
                clearPendingNotification(false);
            }
        }
    }

    public ScanResult recommendNetwork(List<ScanDetail> list, Set<String> set) {
        Iterator<ScanDetail> it = list.iterator();
        int i = Integer.MIN_VALUE;
        ScanResult scanResult = null;
        while (it.hasNext()) {
            ScanResult scanResult2 = it.next().getScanResult();
            if (scanResult2.level > i) {
                i = scanResult2.level;
                scanResult = scanResult2;
            }
        }
        if (scanResult == null || !set.contains(scanResult.SSID)) {
            return scanResult;
        }
        return null;
    }

    public void handleScreenStateChanged(boolean z) {
        this.mScreenOn = z;
    }

    public void handleWifiConnected() {
        if (this.mState != 2) {
            clearPendingNotification(true);
            return;
        }
        if (BenesseExtension.getDchaState() == 0) {
            postNotification(this.mNotificationBuilder.createNetworkConnectedNotification(this.mTag, this.mRecommendedNetwork));
        }
        Log.d(this.mTag, "User connected to recommended network: " + this.mRecommendedNetwork.SSID);
        this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 3);
        this.mState = 3;
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public final void run() {
                AvailableNetworkNotifier.lambda$handleWifiConnected$1(this.f$0);
            }
        }, 5000L);
    }

    public static void lambda$handleWifiConnected$1(AvailableNetworkNotifier availableNetworkNotifier) {
        if (availableNetworkNotifier.mState == 3) {
            availableNetworkNotifier.clearPendingNotification(true);
        }
    }

    public void handleConnectionFailure() {
        if (this.mState != 2) {
            return;
        }
        if (BenesseExtension.getDchaState() == 0) {
            postNotification(this.mNotificationBuilder.createNetworkFailedNotification(this.mTag));
        }
        Log.d(this.mTag, "User failed to connect to recommended network: " + this.mRecommendedNetwork.SSID);
        this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 4);
        this.mState = 4;
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public final void run() {
                AvailableNetworkNotifier.lambda$handleConnectionFailure$2(this.f$0);
            }
        }, 5000L);
    }

    public static void lambda$handleConnectionFailure$2(AvailableNetworkNotifier availableNetworkNotifier) {
        if (availableNetworkNotifier.mState == 4) {
            availableNetworkNotifier.clearPendingNotification(false);
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) this.mContext.getSystemService("notification");
    }

    private void postInitialNotification(ScanResult scanResult) {
        if (this.mRecommendedNetwork != null && TextUtils.equals(this.mRecommendedNetwork.SSID, scanResult.SSID)) {
            return;
        }
        if (BenesseExtension.getDchaState() == 0) {
            postNotification(this.mNotificationBuilder.createConnectToAvailableNetworkNotification(this.mTag, scanResult));
        }
        if (this.mState == 0) {
            this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 1);
        } else {
            this.mWifiMetrics.incrementNumNetworkRecommendationUpdates(this.mTag);
        }
        this.mState = 1;
        this.mRecommendedNetwork = scanResult;
        this.mNotificationRepeatTime = this.mClock.getWallClockMillis() + this.mNotificationRepeatDelay;
    }

    private void postNotification(Notification notification) {
        getNotificationManager().notify(this.mSystemMessageNotificationId, notification);
    }

    private void handleConnectToNetworkAction() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 2);
        if (this.mState != 1) {
            return;
        }
        if (BenesseExtension.getDchaState() == 0) {
            postNotification(this.mNotificationBuilder.createNetworkConnectingNotification(this.mTag, this.mRecommendedNetwork));
        }
        this.mWifiMetrics.incrementConnectToNetworkNotification(this.mTag, 2);
        Log.d(this.mTag, "User initiated connection to recommended network: " + this.mRecommendedNetwork.SSID);
        WifiConfiguration wifiConfigurationCreateRecommendedNetworkConfig = createRecommendedNetworkConfig(this.mRecommendedNetwork);
        Message messageObtain = Message.obtain();
        messageObtain.what = 151553;
        messageObtain.arg1 = -1;
        messageObtain.obj = wifiConfigurationCreateRecommendedNetworkConfig;
        messageObtain.replyTo = this.mSrcMessenger;
        this.mWifiStateMachine.sendMessage(messageObtain);
        this.mState = 2;
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public final void run() {
                AvailableNetworkNotifier.lambda$handleConnectToNetworkAction$3(this.f$0);
            }
        }, 10000L);
    }

    public static void lambda$handleConnectToNetworkAction$3(AvailableNetworkNotifier availableNetworkNotifier) {
        if (availableNetworkNotifier.mState == 2) {
            availableNetworkNotifier.handleConnectionFailure();
        }
    }

    WifiConfiguration createRecommendedNetworkConfig(ScanResult scanResult) {
        return ScanResultUtil.createNetworkFromScanResult(scanResult);
    }

    private void handleSeeAllNetworksAction() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 3);
        startWifiSettings();
    }

    private void startWifiSettings() {
        this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        if (BenesseExtension.getDchaState() == 0) {
            this.mContext.startActivity(new Intent("android.settings.WIFI_SETTINGS").addFlags(268435456));
        }
        clearPendingNotification(false);
    }

    private void handleConnectionAttemptFailedToSend() {
        handleConnectionFailure();
        this.mWifiMetrics.incrementNumNetworkConnectMessageFailedToSend(this.mTag);
    }

    private void handlePickWifiNetworkAfterConnectFailure() {
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 4);
        startWifiSettings();
    }

    private void handleUserDismissedAction() {
        Log.d(this.mTag, "User dismissed notification with state=" + this.mState);
        this.mWifiMetrics.incrementConnectToNetworkNotificationAction(this.mTag, this.mState, 1);
        if (this.mState == 1) {
            this.mBlacklistedSsids.add(this.mRecommendedNetwork.SSID);
            this.mWifiMetrics.setNetworkRecommenderBlacklistSize(this.mTag, this.mBlacklistedSsids.size());
            this.mConfigManager.saveToStore(false);
            Log.d(this.mTag, "Network is added to the network notification blacklist: " + this.mRecommendedNetwork.SSID);
        }
        resetStateAndDelayNotification();
    }

    private void resetStateAndDelayNotification() {
        this.mState = 0;
        this.mNotificationRepeatTime = System.currentTimeMillis() + this.mNotificationRepeatDelay;
        this.mRecommendedNetwork = null;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(this.mTag + ": ");
        printWriter.println("mSettingEnabled " + this.mSettingEnabled);
        printWriter.println("currentTime: " + this.mClock.getWallClockMillis());
        printWriter.println("mNotificationRepeatTime: " + this.mNotificationRepeatTime);
        printWriter.println("mState: " + this.mState);
        printWriter.println("mBlacklistedSsids: " + this.mBlacklistedSsids.toString());
    }

    private class AvailableNetworkNotifierStoreData implements SsidSetStoreData.DataSource {
        private AvailableNetworkNotifierStoreData() {
        }

        @Override
        public Set<String> getSsids() {
            return new ArraySet(AvailableNetworkNotifier.this.mBlacklistedSsids);
        }

        @Override
        public void setSsids(Set<String> set) {
            AvailableNetworkNotifier.this.mBlacklistedSsids.addAll(set);
            AvailableNetworkNotifier.this.mWifiMetrics.setNetworkRecommenderBlacklistSize(AvailableNetworkNotifier.this.mTag, AvailableNetworkNotifier.this.mBlacklistedSsids.size());
        }
    }

    private class NotificationEnabledSettingObserver extends ContentObserver {
        NotificationEnabledSettingObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            AvailableNetworkNotifier.this.mFrameworkFacade.registerContentObserver(AvailableNetworkNotifier.this.mContext, Settings.Global.getUriFor(AvailableNetworkNotifier.this.mToggleSettingsName), true, this);
            AvailableNetworkNotifier.this.mSettingEnabled = getValue();
        }

        @Override
        public void onChange(boolean z) {
            super.onChange(z);
            AvailableNetworkNotifier.this.mSettingEnabled = getValue();
            AvailableNetworkNotifier.this.clearPendingNotification(true);
        }

        private boolean getValue() {
            boolean z = AvailableNetworkNotifier.this.mFrameworkFacade.getIntegerSetting(AvailableNetworkNotifier.this.mContext, AvailableNetworkNotifier.this.mToggleSettingsName, 1) == 1;
            AvailableNetworkNotifier.this.mWifiMetrics.setIsWifiNetworksAvailableNotificationEnabled(AvailableNetworkNotifier.this.mTag, z);
            Log.d(AvailableNetworkNotifier.this.mTag, "Settings toggle enabled=" + z);
            return z;
        }
    }
}
