package com.android.server.wifi;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WakeupConfigStoreData;

public class WakeupOnboarding {

    @VisibleForTesting
    static final int NOTIFICATIONS_UNTIL_ONBOARDED = 3;
    private static final long NOT_SHOWN_TIMESTAMP = -1;

    @VisibleForTesting
    static final long REQUIRED_NOTIFICATION_DELAY = 86400000;
    private static final String TAG = "WakeupOnboarding";
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final Handler mHandler;
    private boolean mIsNotificationShowing;
    private boolean mIsOnboarded;
    private NotificationManager mNotificationManager;
    private int mTotalNotificationsShown;
    private final WakeupNotificationFactory mWakeupNotificationFactory;
    private final WifiConfigManager mWifiConfigManager;
    private long mLastShownTimestamp = -1;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -1067607823) {
                if (iHashCode != -506616242) {
                    b = (iHashCode == 1771495157 && action.equals(WakeupNotificationFactory.ACTION_DISMISS_NOTIFICATION)) ? (byte) 2 : (byte) -1;
                } else if (action.equals(WakeupNotificationFactory.ACTION_OPEN_WIFI_PREFERENCES)) {
                    b = 1;
                }
            } else if (action.equals(WakeupNotificationFactory.ACTION_TURN_OFF_WIFI_WAKE)) {
                b = 0;
            }
            switch (b) {
                case 0:
                    WakeupOnboarding.this.mFrameworkFacade.setIntegerSetting(WakeupOnboarding.this.mContext, "wifi_wakeup_enabled", 0);
                    WakeupOnboarding.this.dismissNotification(true);
                    break;
                case 1:
                    WakeupOnboarding.this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
                    if (BenesseExtension.getDchaState() == 0) {
                        WakeupOnboarding.this.mContext.startActivity(new Intent("android.settings.WIFI_IP_SETTINGS").addFlags(268435456));
                    }
                    WakeupOnboarding.this.dismissNotification(true);
                    break;
                case 2:
                    WakeupOnboarding.this.dismissNotification(true);
                    break;
                default:
                    Log.e(WakeupOnboarding.TAG, "Unknown action " + intent.getAction());
                    break;
            }
        }
    };
    private final IntentFilter mIntentFilter = new IntentFilter();

    public WakeupOnboarding(Context context, WifiConfigManager wifiConfigManager, Looper looper, FrameworkFacade frameworkFacade, WakeupNotificationFactory wakeupNotificationFactory) {
        this.mContext = context;
        this.mWifiConfigManager = wifiConfigManager;
        this.mHandler = new Handler(looper);
        this.mFrameworkFacade = frameworkFacade;
        this.mWakeupNotificationFactory = wakeupNotificationFactory;
        this.mIntentFilter.addAction(WakeupNotificationFactory.ACTION_TURN_OFF_WIFI_WAKE);
        this.mIntentFilter.addAction(WakeupNotificationFactory.ACTION_DISMISS_NOTIFICATION);
        this.mIntentFilter.addAction(WakeupNotificationFactory.ACTION_OPEN_WIFI_PREFERENCES);
    }

    public boolean isOnboarded() {
        return this.mIsOnboarded;
    }

    public void maybeShowNotification() {
        maybeShowNotification(SystemClock.elapsedRealtime());
    }

    @VisibleForTesting
    void maybeShowNotification(long j) {
        if (!shouldShowNotification(j)) {
            return;
        }
        Log.d(TAG, "Showing onboarding notification.");
        incrementTotalNotificationsShown();
        this.mIsNotificationShowing = true;
        this.mLastShownTimestamp = j;
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter, null, this.mHandler);
        getNotificationManager().notify(43, this.mWakeupNotificationFactory.createOnboardingNotification());
    }

    private void incrementTotalNotificationsShown() {
        this.mTotalNotificationsShown++;
        if (this.mTotalNotificationsShown >= 3) {
            setOnboarded();
        } else {
            this.mWifiConfigManager.saveToStore(false);
        }
    }

    private boolean shouldShowNotification(long j) {
        if (isOnboarded() || this.mIsNotificationShowing) {
            return false;
        }
        return this.mLastShownTimestamp == -1 || j - this.mLastShownTimestamp > REQUIRED_NOTIFICATION_DELAY;
    }

    public void onStop() {
        dismissNotification(false);
    }

    private void dismissNotification(boolean z) {
        if (!this.mIsNotificationShowing) {
            return;
        }
        if (z) {
            setOnboarded();
        }
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        getNotificationManager().cancel(43);
        this.mIsNotificationShowing = false;
    }

    public void setOnboarded() {
        if (this.mIsOnboarded) {
            return;
        }
        Log.d(TAG, "Setting user as onboarded.");
        this.mIsOnboarded = true;
        this.mWifiConfigManager.saveToStore(false);
    }

    private NotificationManager getNotificationManager() {
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        }
        return this.mNotificationManager;
    }

    public WakeupConfigStoreData.DataSource<Boolean> getIsOnboadedDataSource() {
        return new IsOnboardedDataSource();
    }

    public WakeupConfigStoreData.DataSource<Integer> getNotificationsDataSource() {
        return new NotificationsDataSource();
    }

    private class IsOnboardedDataSource implements WakeupConfigStoreData.DataSource<Boolean> {
        private IsOnboardedDataSource() {
        }

        @Override
        public Boolean getData() {
            return Boolean.valueOf(WakeupOnboarding.this.mIsOnboarded);
        }

        @Override
        public void setData(Boolean bool) {
            WakeupOnboarding.this.mIsOnboarded = bool.booleanValue();
        }
    }

    private class NotificationsDataSource implements WakeupConfigStoreData.DataSource<Integer> {
        private NotificationsDataSource() {
        }

        @Override
        public Integer getData() {
            return Integer.valueOf(WakeupOnboarding.this.mTotalNotificationsShown);
        }

        @Override
        public void setData(Integer num) {
            WakeupOnboarding.this.mTotalNotificationsShown = num.intValue();
        }
    }
}
