package com.android.server.connectivity;

import android.R;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkIdentity;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkRequest;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.os.BestClock;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DebugUtils;
import android.util.Range;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.net.NetworkStatsManagerInternal;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MultipathPolicyTracker {
    private static final boolean DBG = false;
    private static final int OPQUOTA_USER_SETTING_DIVIDER = 20;
    private static String TAG = MultipathPolicyTracker.class.getSimpleName();
    private ConnectivityManager mCM;
    private final Clock mClock;
    private final ConfigChangeReceiver mConfigChangeReceiver;
    private final Context mContext;
    private final Dependencies mDeps;
    private final Handler mHandler;
    private ConnectivityManager.NetworkCallback mMobileNetworkCallback;
    private final ConcurrentHashMap<Network, MultipathTracker> mMultipathTrackers;
    private NetworkPolicyManager mNPM;
    private NetworkPolicyManager.Listener mPolicyListener;
    private final ContentResolver mResolver;

    @VisibleForTesting
    final ContentObserver mSettingsObserver;
    private NetworkStatsManager mStatsManager;

    public static class Dependencies {
        public Clock getClock() {
            return new BestClock(ZoneOffset.UTC, new Clock[]{SystemClock.currentNetworkTimeClock(), Clock.systemUTC()});
        }
    }

    public MultipathPolicyTracker(Context context, Handler handler) {
        this(context, handler, new Dependencies());
    }

    public MultipathPolicyTracker(Context context, Handler handler, Dependencies dependencies) {
        this.mMultipathTrackers = new ConcurrentHashMap<>();
        this.mContext = context;
        this.mHandler = handler;
        this.mClock = dependencies.getClock();
        this.mDeps = dependencies;
        this.mResolver = this.mContext.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mConfigChangeReceiver = new ConfigChangeReceiver();
    }

    public void start() {
        this.mCM = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mNPM = (NetworkPolicyManager) this.mContext.getSystemService(NetworkPolicyManager.class);
        this.mStatsManager = (NetworkStatsManager) this.mContext.getSystemService(NetworkStatsManager.class);
        registerTrackMobileCallback();
        registerNetworkPolicyListener();
        this.mResolver.registerContentObserver(Settings.Global.getUriFor("network_default_daily_multipath_quota_bytes"), false, this.mSettingsObserver);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiverAsUser(this.mConfigChangeReceiver, UserHandle.ALL, intentFilter, null, this.mHandler);
    }

    public void shutdown() {
        maybeUnregisterTrackMobileCallback();
        unregisterNetworkPolicyListener();
        Iterator<MultipathTracker> it = this.mMultipathTrackers.values().iterator();
        while (it.hasNext()) {
            it.next().shutdown();
        }
        this.mMultipathTrackers.clear();
        this.mResolver.unregisterContentObserver(this.mSettingsObserver);
        this.mContext.unregisterReceiver(this.mConfigChangeReceiver);
    }

    public Integer getMultipathPreference(Network network) {
        MultipathTracker multipathTracker;
        if (network == null || (multipathTracker = this.mMultipathTrackers.get(network)) == null) {
            return null;
        }
        return Integer.valueOf(multipathTracker.getMultipathPreference());
    }

    class MultipathTracker {
        private long mMultipathBudget;
        private NetworkCapabilities mNetworkCapabilities;
        private final NetworkTemplate mNetworkTemplate;
        private long mQuota;
        private final NetworkStatsManager.UsageCallback mUsageCallback;
        final Network network;
        final int subId;
        final String subscriberId;

        public MultipathTracker(final Network network, NetworkCapabilities networkCapabilities) {
            this.network = network;
            this.mNetworkCapabilities = new NetworkCapabilities(networkCapabilities);
            try {
                this.subId = Integer.parseInt(networkCapabilities.getNetworkSpecifier().toString());
                TelephonyManager telephonyManager = (TelephonyManager) MultipathPolicyTracker.this.mContext.getSystemService(TelephonyManager.class);
                if (telephonyManager == null) {
                    throw new IllegalStateException(String.format("Missing TelephonyManager", new Object[0]));
                }
                TelephonyManager telephonyManagerCreateForSubscriptionId = telephonyManager.createForSubscriptionId(this.subId);
                if (telephonyManagerCreateForSubscriptionId == null) {
                    throw new IllegalStateException(String.format("Can't get TelephonyManager for subId %d", Integer.valueOf(this.subId)));
                }
                this.subscriberId = telephonyManagerCreateForSubscriptionId.getSubscriberId();
                this.mNetworkTemplate = new NetworkTemplate(1, this.subscriberId, new String[]{this.subscriberId}, (String) null, -1, -1, 0);
                this.mUsageCallback = new NetworkStatsManager.UsageCallback() {
                    @Override
                    public void onThresholdReached(int i, String str) {
                        MultipathTracker.this.mMultipathBudget = 0L;
                        MultipathTracker.this.updateMultipathBudget();
                    }
                };
                updateMultipathBudget();
            } catch (ClassCastException | NullPointerException | NumberFormatException e) {
                throw new IllegalStateException(String.format("Can't get subId from mobile network %s (%s): %s", network, networkCapabilities, e.getMessage()));
            }
        }

        public void setNetworkCapabilities(NetworkCapabilities networkCapabilities) {
            this.mNetworkCapabilities = new NetworkCapabilities(networkCapabilities);
        }

        private long getDailyNonDefaultDataUsage() {
            ZonedDateTime zonedDateTimeOfInstant = ZonedDateTime.ofInstant(MultipathPolicyTracker.this.mClock.instant(), ZoneId.systemDefault());
            return getNetworkTotalBytes(zonedDateTimeOfInstant.truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli(), zonedDateTimeOfInstant.toInstant().toEpochMilli());
        }

        private long getNetworkTotalBytes(long j, long j2) {
            try {
                return ((NetworkStatsManagerInternal) LocalServices.getService(NetworkStatsManagerInternal.class)).getNetworkTotalBytes(this.mNetworkTemplate, j, j2);
            } catch (RuntimeException e) {
                Slog.w(MultipathPolicyTracker.TAG, "Failed to get data usage: " + e);
                return -1L;
            }
        }

        private NetworkIdentity getTemplateMatchingNetworkIdentity(NetworkCapabilities networkCapabilities) {
            return new NetworkIdentity(0, 0, this.subscriberId, (String) null, !networkCapabilities.hasCapability(18), !networkCapabilities.hasCapability(11), false);
        }

        private long getRemainingDailyBudget(long j, Range<ZonedDateTime> range) {
            long epochMilli = ((ZonedDateTime) range.getLower()).toInstant().toEpochMilli();
            long epochMilli2 = ((ZonedDateTime) range.getUpper()).toInstant().toEpochMilli();
            long networkTotalBytes = getNetworkTotalBytes(epochMilli, epochMilli2);
            return (networkTotalBytes != -1 ? Math.max(0L, j - networkTotalBytes) : 0L) / Math.max(1L, (((epochMilli2 - MultipathPolicyTracker.this.mClock.millis()) - 1) / TimeUnit.DAYS.toMillis(1L)) + 1);
        }

        private long getUserPolicyOpportunisticQuotaBytes() {
            NetworkIdentity templateMatchingNetworkIdentity = getTemplateMatchingNetworkIdentity(this.mNetworkCapabilities);
            long jMin = Long.MAX_VALUE;
            for (NetworkPolicy networkPolicy : MultipathPolicyTracker.this.mNPM.getNetworkPolicies()) {
                if (networkPolicy.hasCycle() && networkPolicy.template.matches(templateMatchingNetworkIdentity)) {
                    long epochMilli = ((ZonedDateTime) ((Range) networkPolicy.cycleIterator().next()).getLower()).toInstant().toEpochMilli();
                    long activeWarning = MultipathPolicyTracker.getActiveWarning(networkPolicy, epochMilli);
                    if (activeWarning == -1) {
                        activeWarning = MultipathPolicyTracker.getActiveLimit(networkPolicy, epochMilli);
                    }
                    if (activeWarning != -1 && activeWarning != -1) {
                        jMin = Math.min(jMin, getRemainingDailyBudget(activeWarning, (Range) networkPolicy.cycleIterator().next()));
                    }
                }
            }
            if (jMin == JobStatus.NO_LATEST_RUNTIME) {
                return -1L;
            }
            return jMin / 20;
        }

        void updateMultipathBudget() {
            long subscriptionOpportunisticQuota = ((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class)).getSubscriptionOpportunisticQuota(this.network, 2);
            if (subscriptionOpportunisticQuota == -1) {
                subscriptionOpportunisticQuota = getUserPolicyOpportunisticQuotaBytes();
            }
            if (subscriptionOpportunisticQuota == -1) {
                subscriptionOpportunisticQuota = MultipathPolicyTracker.this.getDefaultDailyMultipathQuotaBytes();
            }
            if (haveMultipathBudget() && subscriptionOpportunisticQuota == this.mQuota) {
                return;
            }
            this.mQuota = subscriptionOpportunisticQuota;
            long dailyNonDefaultDataUsage = getDailyNonDefaultDataUsage();
            long jMax = dailyNonDefaultDataUsage != -1 ? Math.max(0L, subscriptionOpportunisticQuota - dailyNonDefaultDataUsage) : 0L;
            if (jMax > NetworkStatsManager.MIN_THRESHOLD_BYTES) {
                registerUsageCallback(jMax);
            } else {
                maybeUnregisterUsageCallback();
            }
        }

        public int getMultipathPreference() {
            if (haveMultipathBudget()) {
                return 3;
            }
            return 0;
        }

        public long getQuota() {
            return this.mQuota;
        }

        public long getMultipathBudget() {
            return this.mMultipathBudget;
        }

        private boolean haveMultipathBudget() {
            return this.mMultipathBudget > 0;
        }

        private void registerUsageCallback(long j) {
            maybeUnregisterUsageCallback();
            MultipathPolicyTracker.this.mStatsManager.registerUsageCallback(this.mNetworkTemplate, 0, j, this.mUsageCallback, MultipathPolicyTracker.this.mHandler);
            this.mMultipathBudget = j;
        }

        private void maybeUnregisterUsageCallback() {
            if (haveMultipathBudget()) {
                MultipathPolicyTracker.this.mStatsManager.unregisterUsageCallback(this.mUsageCallback);
                this.mMultipathBudget = 0L;
            }
        }

        void shutdown() {
            maybeUnregisterUsageCallback();
        }
    }

    private static long getActiveWarning(NetworkPolicy networkPolicy, long j) {
        if (networkPolicy.lastWarningSnooze < j) {
            return networkPolicy.warningBytes;
        }
        return -1L;
    }

    private static long getActiveLimit(NetworkPolicy networkPolicy, long j) {
        if (networkPolicy.lastLimitSnooze < j) {
            return networkPolicy.limitBytes;
        }
        return -1L;
    }

    private long getDefaultDailyMultipathQuotaBytes() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "network_default_daily_multipath_quota_bytes");
        if (string != null) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException e) {
            }
        }
        return this.mContext.getResources().getInteger(R.integer.config_defaultRingVibrationIntensity);
    }

    private void registerTrackMobileCallback() {
        NetworkRequest networkRequestBuild = new NetworkRequest.Builder().addCapability(12).addTransportType(0).build();
        this.mMobileNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                MultipathTracker multipathTracker = (MultipathTracker) MultipathPolicyTracker.this.mMultipathTrackers.get(network);
                if (multipathTracker == null) {
                    try {
                        MultipathPolicyTracker.this.mMultipathTrackers.put(network, MultipathPolicyTracker.this.new MultipathTracker(network, networkCapabilities));
                        return;
                    } catch (IllegalStateException e) {
                        Slog.e(MultipathPolicyTracker.TAG, "Can't track mobile network " + network + ": " + e.getMessage());
                        return;
                    }
                }
                multipathTracker.setNetworkCapabilities(networkCapabilities);
                multipathTracker.updateMultipathBudget();
            }

            @Override
            public void onLost(Network network) {
                MultipathTracker multipathTracker = (MultipathTracker) MultipathPolicyTracker.this.mMultipathTrackers.get(network);
                if (multipathTracker != null) {
                    multipathTracker.shutdown();
                    MultipathPolicyTracker.this.mMultipathTrackers.remove(network);
                }
            }
        };
        this.mCM.registerNetworkCallback(networkRequestBuild, this.mMobileNetworkCallback, this.mHandler);
    }

    private void updateAllMultipathBudgets() {
        Iterator<MultipathTracker> it = this.mMultipathTrackers.values().iterator();
        while (it.hasNext()) {
            it.next().updateMultipathBudget();
        }
    }

    private void maybeUnregisterTrackMobileCallback() {
        if (this.mMobileNetworkCallback != null) {
            this.mCM.unregisterNetworkCallback(this.mMobileNetworkCallback);
        }
        this.mMobileNetworkCallback = null;
    }

    class AnonymousClass2 extends NetworkPolicyManager.Listener {
        AnonymousClass2() {
        }

        public void onMeteredIfacesChanged(String[] strArr) {
            MultipathPolicyTracker.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    MultipathPolicyTracker.this.updateAllMultipathBudgets();
                }
            });
        }
    }

    private void registerNetworkPolicyListener() {
        this.mPolicyListener = new AnonymousClass2();
        this.mNPM.registerListener(this.mPolicyListener);
    }

    private void unregisterNetworkPolicyListener() {
        this.mNPM.unregisterListener(this.mPolicyListener);
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            Slog.wtf(MultipathPolicyTracker.TAG, "Should never be reached.");
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (!Settings.Global.getUriFor("network_default_daily_multipath_quota_bytes").equals(uri)) {
                Slog.wtf(MultipathPolicyTracker.TAG, "Unexpected settings observation: " + uri);
            }
            MultipathPolicyTracker.this.updateAllMultipathBudgets();
        }
    }

    private final class ConfigChangeReceiver extends BroadcastReceiver {
        private ConfigChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MultipathPolicyTracker.this.updateAllMultipathBudgets();
        }
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("MultipathPolicyTracker:");
        indentingPrintWriter.increaseIndent();
        for (MultipathTracker multipathTracker : this.mMultipathTrackers.values()) {
            indentingPrintWriter.println(String.format("Network %s: quota %d, budget %d. Preference: %s", multipathTracker.network, Long.valueOf(multipathTracker.getQuota()), Long.valueOf(multipathTracker.getMultipathBudget()), DebugUtils.flagsToString(ConnectivityManager.class, "MULTIPATH_PREFERENCE_", multipathTracker.getMultipathPreference())));
        }
        indentingPrintWriter.decreaseIndent();
    }
}
