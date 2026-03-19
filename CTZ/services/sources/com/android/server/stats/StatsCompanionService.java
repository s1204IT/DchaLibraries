package com.android.server.stats;

import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProcessMemoryState;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.UidTraffic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.NetworkStats;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiActivityEnergyInfo;
import android.os.BatteryStatsInternal;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.IStatsCompanionService;
import android.os.IStatsManager;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.StatsDimensionsValue;
import android.os.StatsLogEventWrapper;
import android.os.SynchronousResultReceiver;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.ModemActivityInfo;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.os.KernelCpuSpeedReader;
import com.android.internal.os.KernelUidCpuActiveTimeReader;
import com.android.internal.os.KernelUidCpuClusterTimeReader;
import com.android.internal.os.KernelUidCpuFreqTimeReader;
import com.android.internal.os.KernelUidCpuTimeReader;
import com.android.internal.os.KernelWakelockReader;
import com.android.internal.os.KernelWakelockStats;
import com.android.internal.os.PowerProfile;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.DumpState;
import com.android.server.pm.Settings;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StatsCompanionService extends IStatsCompanionService.Stub {
    public static final int CODE_DATA_BROADCAST = 1;
    public static final int CODE_SUBSCRIBER_BROADCAST = 1;
    public static final String CONFIG_DIR = "/data/misc/stats-service";
    public static final int DEATH_THRESHOLD = 10;
    static final boolean DEBUG = false;
    private static final long EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS = 2000;
    public static final String EXTRA_LAST_REPORT_TIME = "android.app.extra.LAST_REPORT_TIME";
    public static final String RESULT_RECEIVER_CONTROLLER_KEY = "controller_activity";
    static final String TAG = "StatsCompanionService";

    @GuardedBy("sStatsdLock")
    private static IStatsManager sStatsd;
    private final AlarmManager mAlarmManager;
    private final PendingIntent mAnomalyAlarmIntent;
    private final Context mContext;
    private KernelCpuSpeedReader[] mKernelCpuSpeedReaders;
    private final PendingIntent mPeriodicAlarmIntent;
    private final PendingIntent mPullingAlarmIntent;
    private static final long MILLIS_IN_A_DAY = TimeUnit.DAYS.toMillis(1);
    private static final Object sStatsdLock = new Object();
    private final KernelWakelockReader mKernelWakelockReader = new KernelWakelockReader();
    private final KernelWakelockStats mTmpWakelockStats = new KernelWakelockStats();
    private IWifiManager mWifiManager = null;
    private TelephonyManager mTelephony = null;
    private final StatFs mStatFsData = new StatFs(Environment.getDataDirectory().getAbsolutePath());
    private final StatFs mStatFsSystem = new StatFs(Environment.getRootDirectory().getAbsolutePath());
    private final StatFs mStatFsTemp = new StatFs(Environment.getDownloadCacheDirectory().getAbsolutePath());

    @GuardedBy("sStatsdLock")
    private final HashSet<Long> mDeathTimeMillis = new HashSet<>();

    @GuardedBy("sStatsdLock")
    private final HashMap<Long, String> mDeletedFiles = new HashMap<>();
    private KernelUidCpuTimeReader mKernelUidCpuTimeReader = new KernelUidCpuTimeReader();
    private KernelUidCpuFreqTimeReader mKernelUidCpuFreqTimeReader = new KernelUidCpuFreqTimeReader();
    private KernelUidCpuActiveTimeReader mKernelUidCpuActiveTimeReader = new KernelUidCpuActiveTimeReader();
    private KernelUidCpuClusterTimeReader mKernelUidCpuClusterTimeReader = new KernelUidCpuClusterTimeReader();
    private final BroadcastReceiver mAppUpdateReceiver = new AppUpdateReceiver();
    private final BroadcastReceiver mUserUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (StatsCompanionService.sStatsdLock) {
                IStatsManager unused = StatsCompanionService.sStatsd = StatsCompanionService.fetchStatsdService();
                if (StatsCompanionService.sStatsd != null) {
                    try {
                        StatsCompanionService.this.informAllUidsLocked(context);
                    } catch (RemoteException e) {
                        Slog.e(StatsCompanionService.TAG, "Failed to inform statsd latest update of all apps", e);
                        StatsCompanionService.this.forgetEverythingLocked();
                    }
                    return;
                }
                Slog.w(StatsCompanionService.TAG, "Could not access statsd for UserUpdateReceiver");
            }
        }
    };
    private final ShutdownEventReceiver mShutdownEventReceiver = new ShutdownEventReceiver();

    public StatsCompanionService(Context context) {
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mAnomalyAlarmIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(this.mContext, (Class<?>) AnomalyAlarmReceiver.class), 0);
        this.mPullingAlarmIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(this.mContext, (Class<?>) PullingAlarmReceiver.class), 0);
        this.mPeriodicAlarmIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(this.mContext, (Class<?>) PeriodicAlarmReceiver.class), 0);
        PowerProfile powerProfile = new PowerProfile(context);
        int numCpuClusters = powerProfile.getNumCpuClusters();
        this.mKernelCpuSpeedReaders = new KernelCpuSpeedReader[numCpuClusters];
        int numCoresInCpuCluster = 0;
        for (int i = 0; i < numCpuClusters; i++) {
            this.mKernelCpuSpeedReaders[i] = new KernelCpuSpeedReader(numCoresInCpuCluster, powerProfile.getNumSpeedStepsInCpuCluster(i));
            numCoresInCpuCluster += powerProfile.getNumCoresInCpuCluster(i);
        }
        this.mKernelUidCpuFreqTimeReader.setThrottleInterval(0L);
        this.mKernelUidCpuFreqTimeReader.readFreqs(powerProfile);
        this.mKernelUidCpuClusterTimeReader.setThrottleInterval(0L);
        this.mKernelUidCpuActiveTimeReader.setThrottleInterval(0L);
    }

    public void sendDataBroadcast(IBinder iBinder, long j) {
        enforceCallingPermission();
        IntentSender intentSender = new IntentSender(iBinder);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_LAST_REPORT_TIME, j);
        try {
            intentSender.sendIntent(this.mContext, 1, intent, null, null);
        } catch (IntentSender.SendIntentException e) {
            Slog.w(TAG, "Unable to send using IntentSender");
        }
    }

    public void sendSubscriberBroadcast(IBinder iBinder, long j, long j2, long j3, long j4, String[] strArr, StatsDimensionsValue statsDimensionsValue) {
        enforceCallingPermission();
        IntentSender intentSender = new IntentSender(iBinder);
        Intent intentPutExtra = new Intent().putExtra("android.app.extra.STATS_CONFIG_UID", j).putExtra("android.app.extra.STATS_CONFIG_KEY", j2).putExtra("android.app.extra.STATS_SUBSCRIPTION_ID", j3).putExtra("android.app.extra.STATS_SUBSCRIPTION_RULE_ID", j4).putExtra("android.app.extra.STATS_DIMENSIONS_VALUE", (Parcelable) statsDimensionsValue);
        ArrayList<String> arrayList = new ArrayList<>(strArr.length);
        for (String str : strArr) {
            arrayList.add(str);
        }
        intentPutExtra.putStringArrayListExtra("android.app.extra.STATS_BROADCAST_SUBSCRIBER_COOKIES", arrayList);
        try {
            intentSender.sendIntent(this.mContext, 1, intentPutExtra, null, null);
        } catch (IntentSender.SendIntentException e) {
            Slog.w(TAG, "Unable to send using IntentSender from uid " + j + "; presumably it had been cancelled.");
        }
    }

    private static final int[] toIntArray(List<Integer> list) {
        int[] iArr = new int[list.size()];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = list.get(i).intValue();
        }
        return iArr;
    }

    private static final long[] toLongArray(List<Long> list) {
        long[] jArr = new long[list.size()];
        for (int i = 0; i < jArr.length; i++) {
            jArr[i] = list.get(i).longValue();
        }
        return jArr;
    }

    @GuardedBy("sStatsdLock")
    private final void informAllUidsLocked(Context context) throws RemoteException {
        UserManager userManager = (UserManager) context.getSystemService("user");
        PackageManager packageManager = context.getPackageManager();
        List users = userManager.getUsers(true);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        Iterator it = users.iterator();
        while (it.hasNext()) {
            List installedPackagesAsUser = packageManager.getInstalledPackagesAsUser(4202496, ((UserInfo) it.next()).id);
            for (int i = 0; i < installedPackagesAsUser.size(); i++) {
                if (((PackageInfo) installedPackagesAsUser.get(i)).applicationInfo != null) {
                    arrayList.add(Integer.valueOf(((PackageInfo) installedPackagesAsUser.get(i)).applicationInfo.uid));
                    arrayList2.add(Long.valueOf(((PackageInfo) installedPackagesAsUser.get(i)).getLongVersionCode()));
                    arrayList3.add(((PackageInfo) installedPackagesAsUser.get(i)).packageName);
                }
            }
        }
        sStatsd.informAllUidData(toIntArray(arrayList), toLongArray(arrayList2), (String[]) arrayList3.toArray(new String[arrayList3.size()]));
    }

    private static final class AppUpdateReceiver extends BroadcastReceiver {
        private AppUpdateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.PACKAGE_REPLACED") || !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                synchronized (StatsCompanionService.sStatsdLock) {
                    if (StatsCompanionService.sStatsd == null) {
                        Slog.w(StatsCompanionService.TAG, "Could not access statsd to inform it of an app update");
                        return;
                    }
                    try {
                        if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
                            int i = intent.getExtras().getInt("android.intent.extra.UID");
                            if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                                context.getPackageManager();
                                StatsCompanionService.sStatsd.informOnePackageRemoved(intent.getData().getSchemeSpecificPart(), i);
                            }
                        } else {
                            PackageManager packageManager = context.getPackageManager();
                            int i2 = intent.getExtras().getInt("android.intent.extra.UID");
                            String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
                            StatsCompanionService.sStatsd.informOnePackage(schemeSpecificPart, i2, packageManager.getPackageInfo(schemeSpecificPart, DumpState.DUMP_CHANGES).getLongVersionCode());
                        }
                    } catch (Exception e) {
                        Slog.w(StatsCompanionService.TAG, "Failed to inform statsd of an app update", e);
                    }
                }
            }
        }
    }

    public static final class AnomalyAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Slog.i(StatsCompanionService.TAG, "StatsCompanionService believes an anomaly has occurred at time " + System.currentTimeMillis() + "ms.");
            synchronized (StatsCompanionService.sStatsdLock) {
                if (StatsCompanionService.sStatsd != null) {
                    try {
                        StatsCompanionService.sStatsd.informAnomalyAlarmFired();
                    } catch (RemoteException e) {
                        Slog.w(StatsCompanionService.TAG, "Failed to inform statsd of anomaly alarm firing", e);
                    }
                    return;
                }
                Slog.w(StatsCompanionService.TAG, "Could not access statsd to inform it of anomaly alarm firing");
            }
        }
    }

    public static final class PullingAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (StatsCompanionService.sStatsdLock) {
                if (StatsCompanionService.sStatsd != null) {
                    try {
                        StatsCompanionService.sStatsd.informPollAlarmFired();
                    } catch (RemoteException e) {
                        Slog.w(StatsCompanionService.TAG, "Failed to inform statsd of pulling alarm firing.", e);
                    }
                    return;
                }
                Slog.w(StatsCompanionService.TAG, "Could not access statsd to inform it of pulling alarm firing.");
            }
        }
    }

    public static final class PeriodicAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (StatsCompanionService.sStatsdLock) {
                if (StatsCompanionService.sStatsd != null) {
                    try {
                        StatsCompanionService.sStatsd.informAlarmForSubscriberTriggeringFired();
                    } catch (RemoteException e) {
                        Slog.w(StatsCompanionService.TAG, "Failed to inform statsd of periodic alarm firing.", e);
                    }
                    return;
                }
                Slog.w(StatsCompanionService.TAG, "Could not access statsd to inform it of periodic alarm firing.");
            }
        }
    }

    public static final class ShutdownEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("android.intent.action.REBOOT") && (!intent.getAction().equals("android.intent.action.ACTION_SHUTDOWN") || (intent.getFlags() & 268435456) == 0)) {
                return;
            }
            Slog.i(StatsCompanionService.TAG, "StatsCompanionService noticed a shutdown.");
            synchronized (StatsCompanionService.sStatsdLock) {
                if (StatsCompanionService.sStatsd != null) {
                    try {
                        StatsCompanionService.sStatsd.informDeviceShutdown();
                    } catch (Exception e) {
                        Slog.w(StatsCompanionService.TAG, "Failed to inform statsd of a shutdown event.", e);
                    }
                    return;
                }
                Slog.w(StatsCompanionService.TAG, "Could not access statsd to inform it of a shutdown event.");
            }
        }
    }

    public void setAnomalyAlarm(long j) {
        enforceCallingPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAlarmManager.setExact(3, j, this.mAnomalyAlarmIntent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void cancelAnomalyAlarm() {
        enforceCallingPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAlarmManager.cancel(this.mAnomalyAlarmIntent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setAlarmForSubscriberTriggering(long j) {
        enforceCallingPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAlarmManager.setExact(3, j, this.mPeriodicAlarmIntent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void cancelAlarmForSubscriberTriggering() {
        enforceCallingPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAlarmManager.cancel(this.mPeriodicAlarmIntent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setPullingAlarm(long j) {
        enforceCallingPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAlarmManager.setExact(3, j, this.mPullingAlarmIntent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void cancelPullingAlarm() {
        enforceCallingPermission();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAlarmManager.cancel(this.mPullingAlarmIntent);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void addNetworkStats(int i, List<StatsLogEventWrapper> list, NetworkStats networkStats, boolean z) {
        int size = networkStats.size();
        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        NetworkStats.Entry entry = new NetworkStats.Entry();
        for (int i2 = 0; i2 < size; i2++) {
            networkStats.getValues(i2, entry);
            StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(jElapsedRealtimeNanos, i, z ? 6 : 5);
            statsLogEventWrapper.writeInt(entry.uid);
            if (z) {
                statsLogEventWrapper.writeInt(entry.set);
            }
            statsLogEventWrapper.writeLong(entry.rxBytes);
            statsLogEventWrapper.writeLong(entry.rxPackets);
            statsLogEventWrapper.writeLong(entry.txBytes);
            statsLogEventWrapper.writeLong(entry.txPackets);
            list.add(statsLogEventWrapper);
        }
    }

    private NetworkStats rollupNetworkStatsByFGBG(NetworkStats networkStats) {
        NetworkStats networkStats2 = new NetworkStats(networkStats.getElapsedRealtime(), 1);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        entry.iface = NetworkStats.IFACE_ALL;
        entry.tag = 0;
        entry.metered = -1;
        entry.roaming = -1;
        int size = networkStats.size();
        NetworkStats.Entry entry2 = new NetworkStats.Entry();
        for (int i = 0; i < size; i++) {
            networkStats.getValues(i, entry2);
            if (entry2.tag == 0) {
                entry.set = entry2.set;
                entry.uid = entry2.uid;
                entry.rxBytes = entry2.rxBytes;
                entry.rxPackets = entry2.rxPackets;
                entry.txBytes = entry2.txBytes;
                entry.txPackets = entry2.txPackets;
                networkStats2.combineValues(entry);
            }
        }
        return networkStats2;
    }

    private static <T extends Parcelable> T awaitControllerInfo(SynchronousResultReceiver synchronousResultReceiver) {
        if (synchronousResultReceiver == null) {
            return null;
        }
        try {
            SynchronousResultReceiver.Result resultAwaitResult = synchronousResultReceiver.awaitResult(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS);
            if (resultAwaitResult.bundle != null) {
                resultAwaitResult.bundle.setDefusable(true);
                T t = (T) resultAwaitResult.bundle.getParcelable(RESULT_RECEIVER_CONTROLLER_KEY);
                if (t != null) {
                    return t;
                }
            }
            Slog.e(TAG, "no controller energy info supplied for " + synchronousResultReceiver.getName());
        } catch (TimeoutException e) {
            Slog.w(TAG, "timeout reading " + synchronousResultReceiver.getName() + " stats");
        }
        return null;
    }

    private void pullKernelWakelock(int i, List<StatsLogEventWrapper> list) {
        KernelWakelockStats kernelWakelockStats = this.mKernelWakelockReader.readKernelWakelockStats(this.mTmpWakelockStats);
        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        for (Map.Entry entry : kernelWakelockStats.entrySet()) {
            String str = (String) entry.getKey();
            KernelWakelockStats.Entry entry2 = (KernelWakelockStats.Entry) entry.getValue();
            StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(jElapsedRealtimeNanos, i, 4);
            statsLogEventWrapper.writeString(str);
            statsLogEventWrapper.writeInt(entry2.mCount);
            statsLogEventWrapper.writeInt(entry2.mVersion);
            statsLogEventWrapper.writeLong(entry2.mTotalTime);
            list.add(statsLogEventWrapper);
        }
    }

    private void pullWifiBytesTransfer(int i, List<StatsLogEventWrapper> list) {
        String[] wifiIfaces;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                wifiIfaces = ((BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class)).getWifiIfaces();
            } catch (IOException e) {
                Slog.e(TAG, "Pulling netstats for wifi bytes has error", e);
            }
            if (wifiIfaces.length == 0) {
                return;
            }
            addNetworkStats(i, list, new NetworkStatsFactory().readNetworkStatsDetail(-1, wifiIfaces, 0, (NetworkStats) null).groupedByUid(), false);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void pullWifiBytesTransferByFgBg(int i, List<StatsLogEventWrapper> list) {
        String[] wifiIfaces;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                wifiIfaces = ((BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class)).getWifiIfaces();
            } catch (IOException e) {
                Slog.e(TAG, "Pulling netstats for wifi bytes w/ fg/bg has error", e);
            }
            if (wifiIfaces.length == 0) {
                return;
            }
            addNetworkStats(i, list, rollupNetworkStatsByFGBG(new NetworkStatsFactory().readNetworkStatsDetail(-1, wifiIfaces, 0, (NetworkStats) null)), true);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void pullMobileBytesTransfer(int i, List<StatsLogEventWrapper> list) {
        String[] mobileIfaces;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                mobileIfaces = ((BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class)).getMobileIfaces();
            } catch (IOException e) {
                Slog.e(TAG, "Pulling netstats for mobile bytes has error", e);
            }
            if (mobileIfaces.length == 0) {
                return;
            }
            addNetworkStats(i, list, new NetworkStatsFactory().readNetworkStatsDetail(-1, mobileIfaces, 0, (NetworkStats) null).groupedByUid(), false);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void pullBluetoothBytesTransfer(int i, List<StatsLogEventWrapper> list) {
        BluetoothActivityEnergyInfo bluetoothActivityEnergyInfoPullBluetoothData = pullBluetoothData();
        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        if (bluetoothActivityEnergyInfoPullBluetoothData.getUidTraffic() != null) {
            for (UidTraffic uidTraffic : bluetoothActivityEnergyInfoPullBluetoothData.getUidTraffic()) {
                StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(jElapsedRealtimeNanos, i, 3);
                statsLogEventWrapper.writeInt(uidTraffic.getUid());
                statsLogEventWrapper.writeLong(uidTraffic.getRxBytes());
                statsLogEventWrapper.writeLong(uidTraffic.getTxBytes());
                list.add(statsLogEventWrapper);
            }
        }
    }

    private void pullMobileBytesTransferByFgBg(int i, List<StatsLogEventWrapper> list) {
        String[] mobileIfaces;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                mobileIfaces = ((BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class)).getMobileIfaces();
            } catch (IOException e) {
                Slog.e(TAG, "Pulling netstats for mobile bytes w/ fg/bg has error", e);
            }
            if (mobileIfaces.length == 0) {
                return;
            }
            addNetworkStats(i, list, rollupNetworkStatsByFGBG(new NetworkStatsFactory().readNetworkStatsDetail(-1, mobileIfaces, 0, (NetworkStats) null)), true);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void pullCpuTimePerFreq(int i, List<StatsLogEventWrapper> list) {
        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        for (int i2 = 0; i2 < this.mKernelCpuSpeedReaders.length; i2++) {
            long[] absolute = this.mKernelCpuSpeedReaders[i2].readAbsolute();
            if (absolute != null) {
                for (int length = absolute.length - 1; length >= 0; length--) {
                    StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(jElapsedRealtimeNanos, i, 3);
                    statsLogEventWrapper.writeInt(i2);
                    statsLogEventWrapper.writeInt(length);
                    statsLogEventWrapper.writeLong(absolute[length]);
                    list.add(statsLogEventWrapper);
                }
            }
        }
    }

    private void pullKernelUidCpuTime(final int i, final List<StatsLogEventWrapper> list) {
        final long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        this.mKernelUidCpuTimeReader.readAbsolute(new KernelUidCpuTimeReader.Callback() {
            public final void onUidCpuTime(int i2, long j, long j2) {
                StatsCompanionService.lambda$pullKernelUidCpuTime$0(jElapsedRealtimeNanos, i, list, i2, j, j2);
            }
        });
    }

    static void lambda$pullKernelUidCpuTime$0(long j, int i, List list, int i2, long j2, long j3) {
        StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(j, i, 3);
        statsLogEventWrapper.writeInt(i2);
        statsLogEventWrapper.writeLong(j2);
        statsLogEventWrapper.writeLong(j3);
        list.add(statsLogEventWrapper);
    }

    private void pullKernelUidCpuFreqTime(final int i, final List<StatsLogEventWrapper> list) {
        final long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        this.mKernelUidCpuFreqTimeReader.readAbsolute(new KernelUidCpuFreqTimeReader.Callback() {
            public final void onUidCpuFreqTime(int i2, long[] jArr) {
                StatsCompanionService.lambda$pullKernelUidCpuFreqTime$1(jElapsedRealtimeNanos, i, list, i2, jArr);
            }
        });
    }

    static void lambda$pullKernelUidCpuFreqTime$1(long j, int i, List list, int i2, long[] jArr) {
        for (int i3 = 0; i3 < jArr.length; i3++) {
            if (jArr[i3] != 0) {
                StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(j, i, 3);
                statsLogEventWrapper.writeInt(i2);
                statsLogEventWrapper.writeInt(i3);
                statsLogEventWrapper.writeLong(jArr[i3]);
                list.add(statsLogEventWrapper);
            }
        }
    }

    private void pullKernelUidCpuClusterTime(final int i, final List<StatsLogEventWrapper> list) {
        final long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        this.mKernelUidCpuClusterTimeReader.readAbsolute(new KernelUidCpuClusterTimeReader.Callback() {
            public final void onUidCpuPolicyTime(int i2, long[] jArr) {
                StatsCompanionService.lambda$pullKernelUidCpuClusterTime$2(jElapsedRealtimeNanos, i, list, i2, jArr);
            }
        });
    }

    static void lambda$pullKernelUidCpuClusterTime$2(long j, int i, List list, int i2, long[] jArr) {
        for (int i3 = 0; i3 < jArr.length; i3++) {
            StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(j, i, 3);
            statsLogEventWrapper.writeInt(i2);
            statsLogEventWrapper.writeInt(i3);
            statsLogEventWrapper.writeLong(jArr[i3]);
            list.add(statsLogEventWrapper);
        }
    }

    private void pullKernelUidCpuActiveTime(final int i, final List<StatsLogEventWrapper> list) {
        final long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        this.mKernelUidCpuActiveTimeReader.readAbsolute(new KernelUidCpuActiveTimeReader.Callback() {
            public final void onUidCpuActiveTime(int i2, long j) {
                StatsCompanionService.lambda$pullKernelUidCpuActiveTime$3(jElapsedRealtimeNanos, i, list, i2, j);
            }
        });
    }

    static void lambda$pullKernelUidCpuActiveTime$3(long j, int i, List list, int i2, long j2) {
        StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(j, i, 2);
        statsLogEventWrapper.writeInt(i2);
        statsLogEventWrapper.writeLong(j2);
        list.add(statsLogEventWrapper);
    }

    private void pullWifiActivityInfo(int i, List<StatsLogEventWrapper> list) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        if (this.mWifiManager == null) {
            this.mWifiManager = IWifiManager.Stub.asInterface(ServiceManager.getService("wifi"));
        }
        try {
            if (this.mWifiManager != null) {
                try {
                    SynchronousResultReceiver synchronousResultReceiver = new SynchronousResultReceiver("wifi");
                    this.mWifiManager.requestActivityInfo(synchronousResultReceiver);
                    WifiActivityEnergyInfo wifiActivityEnergyInfoAwaitControllerInfo = awaitControllerInfo(synchronousResultReceiver);
                    StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(SystemClock.elapsedRealtimeNanos(), i, 6);
                    statsLogEventWrapper.writeLong(wifiActivityEnergyInfoAwaitControllerInfo.getTimeStamp());
                    statsLogEventWrapper.writeInt(wifiActivityEnergyInfoAwaitControllerInfo.getStackState());
                    statsLogEventWrapper.writeLong(wifiActivityEnergyInfoAwaitControllerInfo.getControllerTxTimeMillis());
                    statsLogEventWrapper.writeLong(wifiActivityEnergyInfoAwaitControllerInfo.getControllerRxTimeMillis());
                    statsLogEventWrapper.writeLong(wifiActivityEnergyInfoAwaitControllerInfo.getControllerIdleTimeMillis());
                    statsLogEventWrapper.writeLong(wifiActivityEnergyInfoAwaitControllerInfo.getControllerEnergyUsed());
                    list.add(statsLogEventWrapper);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Pulling wifiManager for wifi controller activity energy info has error", e);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void pullModemActivityInfo(int i, List<StatsLogEventWrapper> list) {
        Binder.clearCallingIdentity();
        if (this.mTelephony == null) {
            this.mTelephony = TelephonyManager.from(this.mContext);
        }
        if (this.mTelephony != null) {
            ResultReceiver synchronousResultReceiver = new SynchronousResultReceiver("telephony");
            this.mTelephony.requestModemActivityInfo(synchronousResultReceiver);
            ModemActivityInfo modemActivityInfoAwaitControllerInfo = awaitControllerInfo(synchronousResultReceiver);
            StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(SystemClock.elapsedRealtimeNanos(), i, 10);
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getTimestamp());
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getSleepTimeMillis());
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getIdleTimeMillis());
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getTxTimeMillis()[0]);
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getTxTimeMillis()[1]);
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getTxTimeMillis()[2]);
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getTxTimeMillis()[3]);
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getTxTimeMillis()[4]);
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getRxTimeMillis());
            statsLogEventWrapper.writeLong(modemActivityInfoAwaitControllerInfo.getEnergyUsed());
            list.add(statsLogEventWrapper);
        }
    }

    private void pullBluetoothActivityInfo(int i, List<StatsLogEventWrapper> list) {
        BluetoothActivityEnergyInfo bluetoothActivityEnergyInfoPullBluetoothData = pullBluetoothData();
        StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(SystemClock.elapsedRealtimeNanos(), i, 6);
        statsLogEventWrapper.writeLong(bluetoothActivityEnergyInfoPullBluetoothData.getTimeStamp());
        statsLogEventWrapper.writeInt(bluetoothActivityEnergyInfoPullBluetoothData.getBluetoothStackState());
        statsLogEventWrapper.writeLong(bluetoothActivityEnergyInfoPullBluetoothData.getControllerTxTimeMillis());
        statsLogEventWrapper.writeLong(bluetoothActivityEnergyInfoPullBluetoothData.getControllerRxTimeMillis());
        statsLogEventWrapper.writeLong(bluetoothActivityEnergyInfoPullBluetoothData.getControllerIdleTimeMillis());
        statsLogEventWrapper.writeLong(bluetoothActivityEnergyInfoPullBluetoothData.getControllerEnergyUsed());
        list.add(statsLogEventWrapper);
    }

    private synchronized BluetoothActivityEnergyInfo pullBluetoothData() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null) {
            SynchronousResultReceiver synchronousResultReceiver = new SynchronousResultReceiver("bluetooth");
            defaultAdapter.requestControllerActivityEnergyInfo(synchronousResultReceiver);
            return awaitControllerInfo(synchronousResultReceiver);
        }
        Slog.e(TAG, "Failed to get bluetooth adapter!");
        return null;
    }

    private void pullSystemElapsedRealtime(int i, List<StatsLogEventWrapper> list) {
        StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(SystemClock.elapsedRealtimeNanos(), i, 1);
        statsLogEventWrapper.writeLong(SystemClock.elapsedRealtime());
        list.add(statsLogEventWrapper);
    }

    private void pullDiskSpace(int i, List<StatsLogEventWrapper> list) {
        StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(SystemClock.elapsedRealtimeNanos(), i, 3);
        statsLogEventWrapper.writeLong(this.mStatFsData.getAvailableBytes());
        statsLogEventWrapper.writeLong(this.mStatFsSystem.getAvailableBytes());
        statsLogEventWrapper.writeLong(this.mStatFsTemp.getAvailableBytes());
        list.add(statsLogEventWrapper);
    }

    private void pullSystemUpTime(int i, List<StatsLogEventWrapper> list) {
        StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(SystemClock.elapsedRealtimeNanos(), i, 1);
        statsLogEventWrapper.writeLong(SystemClock.uptimeMillis());
        list.add(statsLogEventWrapper);
    }

    private void pullProcessMemoryState(int i, List<StatsLogEventWrapper> list) {
        List<ProcessMemoryState> memoryStateForProcesses = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getMemoryStateForProcesses();
        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        for (ProcessMemoryState processMemoryState : memoryStateForProcesses) {
            StatsLogEventWrapper statsLogEventWrapper = new StatsLogEventWrapper(jElapsedRealtimeNanos, i, 8);
            statsLogEventWrapper.writeInt(processMemoryState.uid);
            statsLogEventWrapper.writeString(processMemoryState.processName);
            statsLogEventWrapper.writeInt(processMemoryState.oomScore);
            statsLogEventWrapper.writeLong(processMemoryState.pgfault);
            statsLogEventWrapper.writeLong(processMemoryState.pgmajfault);
            statsLogEventWrapper.writeLong(processMemoryState.rssInBytes);
            statsLogEventWrapper.writeLong(processMemoryState.cacheInBytes);
            statsLogEventWrapper.writeLong(processMemoryState.swapInBytes);
            list.add(statsLogEventWrapper);
        }
    }

    public StatsLogEventWrapper[] pullData(int i) {
        enforceCallingPermission();
        ArrayList arrayList = new ArrayList();
        switch (i) {
            case 10000:
                pullWifiBytesTransfer(i, arrayList);
                break;
            case 10001:
                pullWifiBytesTransferByFgBg(i, arrayList);
                break;
            case 10002:
                pullMobileBytesTransfer(i, arrayList);
                break;
            case 10003:
                pullMobileBytesTransferByFgBg(i, arrayList);
                break;
            case 10004:
                pullKernelWakelock(i, arrayList);
                break;
            case 10005:
            default:
                Slog.w(TAG, "No such tagId data as " + i);
                return null;
            case 10006:
                pullBluetoothBytesTransfer(i, arrayList);
                break;
            case 10007:
                pullBluetoothActivityInfo(i, arrayList);
                break;
            case 10008:
                pullCpuTimePerFreq(i, arrayList);
                break;
            case 10009:
                pullKernelUidCpuTime(i, arrayList);
                break;
            case 10010:
                pullKernelUidCpuFreqTime(i, arrayList);
                break;
            case 10011:
                pullWifiActivityInfo(i, arrayList);
                break;
            case 10012:
                pullModemActivityInfo(i, arrayList);
                break;
            case 10013:
                pullProcessMemoryState(i, arrayList);
                break;
            case 10014:
                pullSystemElapsedRealtime(i, arrayList);
                break;
            case 10015:
                pullSystemUpTime(i, arrayList);
                break;
            case 10016:
                pullKernelUidCpuActiveTime(i, arrayList);
                break;
            case 10017:
                pullKernelUidCpuClusterTime(i, arrayList);
                break;
            case 10018:
                pullDiskSpace(i, arrayList);
                break;
        }
        return (StatsLogEventWrapper[]) arrayList.toArray(new StatsLogEventWrapper[arrayList.size()]);
    }

    public void statsdReady() {
        enforceCallingPermission();
        sayHiToStatsd();
        this.mContext.sendBroadcastAsUser(new Intent("android.app.action.STATSD_STARTED").addFlags(DumpState.DUMP_SERVICE_PERMISSIONS), UserHandle.SYSTEM, "android.permission.DUMP");
    }

    public void triggerUidSnapshot() {
        enforceCallingPermission();
        synchronized (sStatsdLock) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    informAllUidsLocked(this.mContext);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to trigger uid snapshot.", e);
                }
            } finally {
                restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private void enforceCallingPermission() {
        if (Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforceCallingPermission("android.permission.STATSCOMPANION", null);
    }

    private static IStatsManager fetchStatsdService() {
        return IStatsManager.Stub.asInterface(ServiceManager.getService("stats"));
    }

    public static final class Lifecycle extends SystemService {
        private StatsCompanionService mStatsCompanionService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mStatsCompanionService = new StatsCompanionService(getContext());
            try {
                publishBinderService("statscompanion", this.mStatsCompanionService);
            } catch (Exception e) {
                Slog.e(StatsCompanionService.TAG, "Failed to publishBinderService", e);
            }
        }

        @Override
        public void onBootPhase(int i) {
            super.onBootPhase(i);
            if (i == 600) {
                this.mStatsCompanionService.systemReady();
            }
        }
    }

    private void systemReady() {
        sayHiToStatsd();
    }

    private void sayHiToStatsd() {
        synchronized (sStatsdLock) {
            if (sStatsd != null) {
                Slog.e(TAG, "Trying to fetch statsd, but it was already fetched", new IllegalStateException("sStatsd is not null when being fetched"));
                return;
            }
            sStatsd = fetchStatsdService();
            if (sStatsd == null) {
                Slog.i(TAG, "Could not yet find statsd to tell it that StatsCompanion is alive.");
                return;
            }
            try {
                sStatsd.statsCompanionReady();
                try {
                    sStatsd.asBinder().linkToDeath(new StatsdDeathRecipient(), 0);
                } catch (RemoteException e) {
                    Slog.e(TAG, "linkToDeath(StatsdDeathRecipient) failed", e);
                    forgetEverythingLocked();
                }
                IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_REPLACED");
                intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
                intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
                intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
                this.mContext.registerReceiverAsUser(this.mAppUpdateReceiver, UserHandle.ALL, intentFilter, null, null);
                IntentFilter intentFilter2 = new IntentFilter("android.intent.action.USER_INITIALIZE");
                intentFilter2.addAction("android.intent.action.USER_REMOVED");
                this.mContext.registerReceiverAsUser(this.mUserUpdateReceiver, UserHandle.ALL, intentFilter2, null, null);
                IntentFilter intentFilter3 = new IntentFilter("android.intent.action.REBOOT");
                intentFilter3.addAction("android.intent.action.ACTION_SHUTDOWN");
                this.mContext.registerReceiverAsUser(this.mShutdownEventReceiver, UserHandle.ALL, intentFilter3, null, null);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    informAllUidsLocked(this.mContext);
                    restoreCallingIdentity(jClearCallingIdentity);
                    Slog.i(TAG, "Told statsd that StatsCompanionService is alive.");
                } catch (Throwable th) {
                    restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            } catch (RemoteException e2) {
                Slog.e(TAG, "Failed to inform statsd that statscompanion is ready", e2);
                forgetEverythingLocked();
            }
        }
    }

    private class StatsdDeathRecipient implements IBinder.DeathRecipient {
        private StatsdDeathRecipient() {
        }

        @Override
        public void binderDied() {
            Slog.i(StatsCompanionService.TAG, "Statsd is dead - erase all my knowledge.");
            synchronized (StatsCompanionService.sStatsdLock) {
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                for (Long l : StatsCompanionService.this.mDeathTimeMillis) {
                    if (jElapsedRealtime - l.longValue() > StatsCompanionService.MILLIS_IN_A_DAY) {
                        StatsCompanionService.this.mDeathTimeMillis.remove(l);
                    }
                }
                for (Long l2 : StatsCompanionService.this.mDeletedFiles.keySet()) {
                    if (jElapsedRealtime - l2.longValue() > StatsCompanionService.MILLIS_IN_A_DAY * 7) {
                        StatsCompanionService.this.mDeletedFiles.remove(l2);
                    }
                }
                StatsCompanionService.this.mDeathTimeMillis.add(Long.valueOf(jElapsedRealtime));
                if (StatsCompanionService.this.mDeathTimeMillis.size() >= 10) {
                    StatsCompanionService.this.mDeathTimeMillis.clear();
                    File[] fileArrListFilesOrEmpty = FileUtils.listFilesOrEmpty(new File(StatsCompanionService.CONFIG_DIR));
                    if (fileArrListFilesOrEmpty.length > 0) {
                        String name = fileArrListFilesOrEmpty[0].getName();
                        if (fileArrListFilesOrEmpty[0].delete()) {
                            StatsCompanionService.this.mDeletedFiles.put(Long.valueOf(jElapsedRealtime), name);
                        }
                    }
                }
                StatsCompanionService.this.forgetEverythingLocked();
            }
        }
    }

    private void forgetEverythingLocked() {
        sStatsd = null;
        this.mContext.unregisterReceiver(this.mAppUpdateReceiver);
        this.mContext.unregisterReceiver(this.mUserUpdateReceiver);
        this.mContext.unregisterReceiver(this.mShutdownEventReceiver);
        cancelAnomalyAlarm();
        cancelPullingAlarm();
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            synchronized (sStatsdLock) {
                printWriter.println("Number of configuration files deleted: " + this.mDeletedFiles.size());
                if (this.mDeletedFiles.size() > 0) {
                    printWriter.println("  timestamp, deleted file name");
                }
                long jCurrentThreadTimeMillis = SystemClock.currentThreadTimeMillis() - SystemClock.elapsedRealtime();
                for (Long l : this.mDeletedFiles.keySet()) {
                    printWriter.println("  " + (l.longValue() + jCurrentThreadTimeMillis) + ", " + this.mDeletedFiles.get(l));
                }
            }
        }
    }
}
