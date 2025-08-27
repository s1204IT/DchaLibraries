package com.android.settingslib.net;

import android.R;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/* loaded from: classes.dex */
public class DataUsageController {
    private static final boolean DEBUG = Log.isLoggable("DataUsageController", 3);
    private static final StringBuilder PERIOD_BUILDER = new StringBuilder(50);
    private static final Formatter PERIOD_FORMATTER = new Formatter(PERIOD_BUILDER, Locale.getDefault());
    private Callback mCallback;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private NetworkNameProvider mNetworkController;
    private final NetworkPolicyManager mPolicyManager;
    private INetworkStatsSession mSession;
    private final INetworkStatsService mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
    private final TelephonyManager mTelephonyManager;

    public interface Callback {
        void onMobileDataEnabled(boolean z);
    }

    public static class DataUsageInfo {
        public String carrier;
        public long cycleEnd;
        public long cycleStart;
        public long limitLevel;
        public String period;
        public long startDate;
        public long usageLevel;
        public long warningLevel;
    }

    public interface NetworkNameProvider {
        String getMobileDataNetworkName();
    }

    public DataUsageController(Context context) {
        this.mContext = context;
        this.mTelephonyManager = TelephonyManager.from(context);
        this.mConnectivityManager = ConnectivityManager.from(context);
        this.mPolicyManager = NetworkPolicyManager.from(this.mContext);
    }

    public void setNetworkController(NetworkNameProvider networkNameProvider) {
        this.mNetworkController = networkNameProvider;
    }

    public long getDefaultWarningLevel() {
        return 1048576 * this.mContext.getResources().getInteger(R.integer.config_notificationsBatteryLedOn);
    }

    private INetworkStatsSession getSession() {
        if (this.mSession == null) {
            try {
                this.mSession = this.mStatsService.openSession();
            } catch (RemoteException e) {
                Log.w("DataUsageController", "Failed to open stats session", e);
            } catch (RuntimeException e2) {
                Log.w("DataUsageController", "Failed to open stats session", e2);
            }
        }
        return this.mSession;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    private DataUsageInfo warn(String str) {
        Log.w("DataUsageController", "Failed to get data usage, " + str);
        return null;
    }

    public DataUsageInfo getDataUsageInfo() {
        String activeSubscriberId = getActiveSubscriberId(this.mContext);
        if (activeSubscriberId == null) {
            return warn("no subscriber id");
        }
        return getDataUsageInfo(NetworkTemplate.normalize(NetworkTemplate.buildTemplateMobileAll(activeSubscriberId), this.mTelephonyManager.getMergedSubscriberIds()));
    }

    public DataUsageInfo getDataUsageInfo(NetworkTemplate networkTemplate) {
        long j;
        long j2;
        long epochMilli;
        long epochMilli2;
        INetworkStatsSession session = getSession();
        if (session == null) {
            return warn("no stats session");
        }
        NetworkPolicy networkPolicyFindNetworkPolicy = findNetworkPolicy(networkTemplate);
        try {
            NetworkStatsHistory historyForNetwork = session.getHistoryForNetwork(networkTemplate, 10);
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (networkPolicyFindNetworkPolicy != null) {
                boolean zHasNext = NetworkPolicyManager.cycleIterator(networkPolicyFindNetworkPolicy).hasNext();
                Log.i("DataUsageController", "getDataUsageInfo , policy != null hasNext = " + zHasNext);
                if (zHasNext) {
                    Pair pair = (Pair) NetworkPolicyManager.cycleIterator(networkPolicyFindNetworkPolicy).next();
                    epochMilli = ((ZonedDateTime) pair.first).toInstant().toEpochMilli();
                    epochMilli2 = ((ZonedDateTime) pair.second).toInstant().toEpochMilli();
                } else {
                    epochMilli = jCurrentTimeMillis - 2419200000L;
                    epochMilli2 = jCurrentTimeMillis;
                }
                j = epochMilli;
                j2 = epochMilli2;
            } else {
                Log.i("DataUsageController", "getDataUsageInfo , policy = null");
                j = jCurrentTimeMillis - 2419200000L;
                j2 = jCurrentTimeMillis;
            }
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            long j3 = j2;
            NetworkStatsHistory.Entry values = historyForNetwork.getValues(j, j2, jCurrentTimeMillis, (NetworkStatsHistory.Entry) null);
            long jCurrentTimeMillis3 = System.currentTimeMillis();
            if (DEBUG) {
                Log.d("DataUsageController", String.format("history call from %s to %s now=%s took %sms: %s", new Date(j), new Date(j3), new Date(jCurrentTimeMillis), Long.valueOf(jCurrentTimeMillis3 - jCurrentTimeMillis2), historyEntryToString(values)));
            }
            if (values == null) {
                return warn("no entry data");
            }
            long j4 = values.rxBytes + values.txBytes;
            DataUsageInfo dataUsageInfo = new DataUsageInfo();
            dataUsageInfo.startDate = j;
            dataUsageInfo.usageLevel = j4;
            dataUsageInfo.period = formatDateRange(j, j3);
            dataUsageInfo.cycleStart = j;
            dataUsageInfo.cycleEnd = j3;
            if (networkPolicyFindNetworkPolicy != null) {
                dataUsageInfo.limitLevel = networkPolicyFindNetworkPolicy.limitBytes > 0 ? networkPolicyFindNetworkPolicy.limitBytes : 0L;
                dataUsageInfo.warningLevel = networkPolicyFindNetworkPolicy.warningBytes > 0 ? networkPolicyFindNetworkPolicy.warningBytes : 0L;
            } else {
                dataUsageInfo.warningLevel = getDefaultWarningLevel();
            }
            if (this.mNetworkController != null) {
                dataUsageInfo.carrier = this.mNetworkController.getMobileDataNetworkName();
            }
            return dataUsageInfo;
        } catch (RemoteException e) {
            return warn("remote call failed");
        }
    }

    private NetworkPolicy findNetworkPolicy(NetworkTemplate networkTemplate) {
        NetworkPolicy[] networkPolicies;
        if (this.mPolicyManager == null || networkTemplate == null || (networkPolicies = this.mPolicyManager.getNetworkPolicies()) == null) {
            return null;
        }
        for (NetworkPolicy networkPolicy : networkPolicies) {
            if (networkPolicy != null && networkTemplate.equals(networkPolicy.template)) {
                return networkPolicy;
            }
        }
        return null;
    }

    private static String historyEntryToString(NetworkStatsHistory.Entry entry) {
        if (entry == null) {
            return null;
        }
        return "Entry[bucketDuration=" + entry.bucketDuration + ",bucketStart=" + entry.bucketStart + ",activeTime=" + entry.activeTime + ",rxBytes=" + entry.rxBytes + ",rxPackets=" + entry.rxPackets + ",txBytes=" + entry.txBytes + ",txPackets=" + entry.txPackets + ",operations=" + entry.operations + ']';
    }

    public void setMobileDataEnabled(boolean z) {
        Log.d("DataUsageController", "setMobileDataEnabled: enabled=" + z);
        this.mTelephonyManager.setDataEnabled(z);
        if (this.mCallback != null) {
            this.mCallback.onMobileDataEnabled(z);
        }
    }

    public boolean isMobileDataSupported() {
        return this.mConnectivityManager.isNetworkSupported(0) && this.mTelephonyManager.getSimState() == 5;
    }

    public boolean isMobileDataEnabled() {
        return this.mTelephonyManager.getDataEnabled();
    }

    private static String getActiveSubscriberId(Context context) {
        return TelephonyManager.from(context).getSubscriberId(SubscriptionManager.getDefaultDataSubscriptionId());
    }

    private String formatDateRange(long j, long j2) {
        String string;
        synchronized (PERIOD_BUILDER) {
            PERIOD_BUILDER.setLength(0);
            string = DateUtils.formatDateRange(this.mContext, PERIOD_FORMATTER, j, j2, 65552, null).toString();
        }
        return string;
    }
}
