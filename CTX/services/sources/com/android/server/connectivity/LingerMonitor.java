package com.android.server.connectivity;

import android.R;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.os.BenesseExtension;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.MessageUtils;
import com.android.server.connectivity.NetworkNotificationManager;
import java.util.HashMap;

public class LingerMonitor {
    private static final boolean DBG = true;
    public static final int DEFAULT_NOTIFICATION_DAILY_LIMIT = 3;
    public static final long DEFAULT_NOTIFICATION_RATE_LIMIT_MILLIS = 60000;

    @VisibleForTesting
    public static final int NOTIFY_TYPE_NONE = 0;
    public static final int NOTIFY_TYPE_NOTIFICATION = 1;
    public static final int NOTIFY_TYPE_TOAST = 2;
    private static final boolean VDBG = false;
    private final Context mContext;
    private final int mDailyLimit;
    private long mFirstNotificationMillis;
    private long mLastNotificationMillis;
    private int mNotificationCounter;
    private final NetworkNotificationManager mNotifier;
    private final long mRateLimitMillis;
    private static final String TAG = LingerMonitor.class.getSimpleName();
    private static final HashMap<String, Integer> TRANSPORT_NAMES = makeTransportToNameMap();

    @VisibleForTesting
    public static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
    private static SparseArray<String> sNotifyTypeNames = MessageUtils.findMessageNames(new Class[]{LingerMonitor.class}, new String[]{"NOTIFY_TYPE_"});
    private final SparseIntArray mNotifications = new SparseIntArray();
    private final SparseBooleanArray mEverNotified = new SparseBooleanArray();

    public LingerMonitor(Context context, NetworkNotificationManager networkNotificationManager, int i, long j) {
        this.mContext = context;
        this.mNotifier = networkNotificationManager;
        this.mDailyLimit = i;
        this.mRateLimitMillis = j;
    }

    private static HashMap<String, Integer> makeTransportToNameMap() {
        SparseArray sparseArrayFindMessageNames = MessageUtils.findMessageNames(new Class[]{NetworkCapabilities.class}, new String[]{"TRANSPORT_"});
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < sparseArrayFindMessageNames.size(); i++) {
            map.put((String) sparseArrayFindMessageNames.valueAt(i), Integer.valueOf(sparseArrayFindMessageNames.keyAt(i)));
        }
        return map;
    }

    private static boolean hasTransport(NetworkAgentInfo networkAgentInfo, int i) {
        return networkAgentInfo.networkCapabilities.hasTransport(i);
    }

    private int getNotificationSource(NetworkAgentInfo networkAgentInfo) {
        for (int i = 0; i < this.mNotifications.size(); i++) {
            if (this.mNotifications.valueAt(i) == networkAgentInfo.network.netId) {
                return this.mNotifications.keyAt(i);
            }
        }
        return 0;
    }

    private boolean everNotified(NetworkAgentInfo networkAgentInfo) {
        return this.mEverNotified.get(networkAgentInfo.network.netId, false);
    }

    @VisibleForTesting
    public boolean isNotificationEnabled(NetworkAgentInfo networkAgentInfo, NetworkAgentInfo networkAgentInfo2) {
        for (String str : this.mContext.getResources().getStringArray(R.array.config_companionDeviceCerts)) {
            if (!TextUtils.isEmpty(str)) {
                String[] strArrSplit = str.split("-", 2);
                if (strArrSplit.length != 2) {
                    Log.e(TAG, "Invalid network switch notification configuration: " + str);
                } else {
                    int iIntValue = TRANSPORT_NAMES.get("TRANSPORT_" + strArrSplit[0]).intValue();
                    int iIntValue2 = TRANSPORT_NAMES.get("TRANSPORT_" + strArrSplit[1]).intValue();
                    if (hasTransport(networkAgentInfo, iIntValue) && hasTransport(networkAgentInfo2, iIntValue2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void showNotification(NetworkAgentInfo networkAgentInfo, NetworkAgentInfo networkAgentInfo2) {
        this.mNotifier.showNotification(networkAgentInfo.network.netId, NetworkNotificationManager.NotificationType.NETWORK_SWITCH, networkAgentInfo, networkAgentInfo2, createNotificationIntent(), true);
    }

    @VisibleForTesting
    protected PendingIntent createNotificationIntent() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        return PendingIntent.getActivityAsUser(this.mContext, 0, CELLULAR_SETTINGS, 268435456, null, UserHandle.CURRENT);
    }

    private void maybeStopNotifying(NetworkAgentInfo networkAgentInfo) {
        int notificationSource = getNotificationSource(networkAgentInfo);
        if (notificationSource != 0) {
            this.mNotifications.delete(notificationSource);
            this.mNotifier.clearNotification(notificationSource);
        }
    }

    private void notify(NetworkAgentInfo networkAgentInfo, NetworkAgentInfo networkAgentInfo2, boolean z) {
        int integer = this.mContext.getResources().getInteger(R.integer.config_defaultUndimsRequired);
        if (integer == 1 && z) {
            integer = 2;
        }
        switch (integer) {
            case 0:
                return;
            case 1:
                showNotification(networkAgentInfo, networkAgentInfo2);
                break;
            case 2:
                this.mNotifier.showToast(networkAgentInfo, networkAgentInfo2);
                break;
            default:
                Log.e(TAG, "Unknown notify type " + integer);
                return;
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Notifying switch from=");
        sb.append(networkAgentInfo.name());
        sb.append(" to=");
        sb.append(networkAgentInfo2.name());
        sb.append(" type=");
        sb.append(sNotifyTypeNames.get(integer, "unknown(" + integer + ")"));
        Log.d(str, sb.toString());
        this.mNotifications.put(networkAgentInfo.network.netId, networkAgentInfo2.network.netId);
        this.mEverNotified.put(networkAgentInfo.network.netId, true);
    }

    public void noteLingerDefaultNetwork(NetworkAgentInfo networkAgentInfo, NetworkAgentInfo networkAgentInfo2) {
        maybeStopNotifying(networkAgentInfo);
        if (networkAgentInfo.everValidated) {
            boolean zHasCapability = networkAgentInfo.networkCapabilities.hasCapability(17);
            if (everNotified(networkAgentInfo) || networkAgentInfo.lastValidated || !isNotificationEnabled(networkAgentInfo, networkAgentInfo2)) {
                return;
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            if (isRateLimited(jElapsedRealtime) || isAboveDailyLimit(jElapsedRealtime)) {
                return;
            }
            notify(networkAgentInfo, networkAgentInfo2, zHasCapability);
        }
    }

    public void noteDisconnect(NetworkAgentInfo networkAgentInfo) {
        this.mNotifications.delete(networkAgentInfo.network.netId);
        this.mEverNotified.delete(networkAgentInfo.network.netId);
        maybeStopNotifying(networkAgentInfo);
    }

    private boolean isRateLimited(long j) {
        if (j - this.mLastNotificationMillis < this.mRateLimitMillis) {
            return true;
        }
        this.mLastNotificationMillis = j;
        return false;
    }

    private boolean isAboveDailyLimit(long j) {
        if (this.mFirstNotificationMillis == 0) {
            this.mFirstNotificationMillis = j;
        }
        if (j - this.mFirstNotificationMillis > 86400000) {
            this.mNotificationCounter = 0;
            this.mFirstNotificationMillis = 0L;
        }
        if (this.mNotificationCounter >= this.mDailyLimit) {
            return true;
        }
        this.mNotificationCounter++;
        return false;
    }
}
