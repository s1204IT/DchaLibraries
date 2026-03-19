package com.android.server.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetdEventCallback;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.metrics.ConnectStats;
import android.net.metrics.DnsEvent;
import android.net.metrics.INetdEventListener;
import android.net.metrics.NetworkMetrics;
import android.net.metrics.WakeupEvent;
import android.net.metrics.WakeupStats;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.util.StatsLog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.BitUtils;
import com.android.internal.util.RingBuffer;
import com.android.internal.util.TokenBucket;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public class NetdEventListenerService extends INetdEventListener.Stub {
    private static final int CONNECT_LATENCY_BURST_LIMIT = 5000;
    private static final int CONNECT_LATENCY_FILL_RATE = 15000;
    private static final boolean DBG = false;
    private static final int METRICS_SNAPSHOT_BUFFER_SIZE = 48;
    private static final long METRICS_SNAPSHOT_SPAN_MS = 300000;
    public static final String SERVICE_NAME = "netd_listener";

    @VisibleForTesting
    static final int WAKEUP_EVENT_BUFFER_LENGTH = 1024;

    @VisibleForTesting
    static final String WAKEUP_EVENT_IFACE_PREFIX = "iface:";
    private final ConnectivityManager mCm;

    @GuardedBy("this")
    private final TokenBucket mConnectTb;

    @GuardedBy("this")
    private long mLastSnapshot;

    @GuardedBy("this")
    private INetdEventCallback[] mNetdEventCallbackList;

    @GuardedBy("this")
    private final SparseArray<NetworkMetrics> mNetworkMetrics;

    @GuardedBy("this")
    private final RingBuffer<NetworkMetricsSnapshot> mNetworkMetricsSnapshots;

    @GuardedBy("this")
    private final RingBuffer<WakeupEvent> mWakeupEvents;

    @GuardedBy("this")
    private final ArrayMap<String, WakeupStats> mWakeupStats;
    private static final String TAG = NetdEventListenerService.class.getSimpleName();

    @GuardedBy("this")
    private static final int[] ALLOWED_CALLBACK_TYPES = {0, 1, 2, 3};

    public synchronized boolean addNetdEventCallback(int i, INetdEventCallback iNetdEventCallback) {
        if (!isValidCallerType(i)) {
            Log.e(TAG, "Invalid caller type: " + i);
            return false;
        }
        this.mNetdEventCallbackList[i] = iNetdEventCallback;
        return true;
    }

    public synchronized boolean removeNetdEventCallback(int i) {
        if (!isValidCallerType(i)) {
            Log.e(TAG, "Invalid caller type: " + i);
            return false;
        }
        this.mNetdEventCallbackList[i] = null;
        return true;
    }

    private static boolean isValidCallerType(int i) {
        for (int i2 = 0; i2 < ALLOWED_CALLBACK_TYPES.length; i2++) {
            if (i == ALLOWED_CALLBACK_TYPES[i2]) {
                return true;
            }
        }
        return false;
    }

    public NetdEventListenerService(Context context) {
        this((ConnectivityManager) context.getSystemService(ConnectivityManager.class));
    }

    @VisibleForTesting
    public NetdEventListenerService(ConnectivityManager connectivityManager) {
        this.mNetworkMetrics = new SparseArray<>();
        this.mNetworkMetricsSnapshots = new RingBuffer<>(NetworkMetricsSnapshot.class, 48);
        this.mLastSnapshot = 0L;
        this.mWakeupStats = new ArrayMap<>();
        this.mWakeupEvents = new RingBuffer<>(WakeupEvent.class, 1024);
        this.mConnectTb = new TokenBucket(15000, CONNECT_LATENCY_BURST_LIMIT);
        this.mNetdEventCallbackList = new INetdEventCallback[ALLOWED_CALLBACK_TYPES.length];
        this.mCm = connectivityManager;
    }

    private static long projectSnapshotTime(long j) {
        return (j / 300000) * 300000;
    }

    private NetworkMetrics getMetricsForNetwork(long j, int i) {
        collectPendingMetricsSnapshot(j);
        NetworkMetrics networkMetrics = this.mNetworkMetrics.get(i);
        if (networkMetrics == null) {
            NetworkMetrics networkMetrics2 = new NetworkMetrics(i, getTransports(i), this.mConnectTb);
            this.mNetworkMetrics.put(i, networkMetrics2);
            return networkMetrics2;
        }
        return networkMetrics;
    }

    private NetworkMetricsSnapshot[] getNetworkMetricsSnapshots() {
        collectPendingMetricsSnapshot(System.currentTimeMillis());
        return (NetworkMetricsSnapshot[]) this.mNetworkMetricsSnapshots.toArray();
    }

    private void collectPendingMetricsSnapshot(long j) {
        if (Math.abs(j - this.mLastSnapshot) <= 300000) {
            return;
        }
        this.mLastSnapshot = projectSnapshotTime(j);
        NetworkMetricsSnapshot networkMetricsSnapshotCollect = NetworkMetricsSnapshot.collect(this.mLastSnapshot, this.mNetworkMetrics);
        if (networkMetricsSnapshotCollect.stats.isEmpty()) {
            return;
        }
        this.mNetworkMetricsSnapshots.append(networkMetricsSnapshotCollect);
    }

    @Override
    public synchronized void onDnsEvent(int i, int i2, int i3, int i4, String str, String[] strArr, int i5, int i6) throws RemoteException {
        long jCurrentTimeMillis = System.currentTimeMillis();
        getMetricsForNetwork(jCurrentTimeMillis, i).addDnsResult(i2, i3, i4);
        for (INetdEventCallback iNetdEventCallback : this.mNetdEventCallbackList) {
            if (iNetdEventCallback != null) {
                iNetdEventCallback.onDnsEvent(str, strArr, i5, jCurrentTimeMillis, i6);
            }
        }
    }

    @Override
    public synchronized void onPrivateDnsValidationEvent(int i, String str, String str2, boolean z) throws RemoteException {
        for (INetdEventCallback iNetdEventCallback : this.mNetdEventCallbackList) {
            if (iNetdEventCallback != null) {
                iNetdEventCallback.onPrivateDnsValidationEvent(i, str, str2, z);
            }
        }
    }

    @Override
    public synchronized void onConnectEvent(int i, int i2, int i3, String str, int i4, int i5) throws RemoteException {
        long jCurrentTimeMillis = System.currentTimeMillis();
        getMetricsForNetwork(jCurrentTimeMillis, i).addConnectResult(i2, i3, str);
        for (INetdEventCallback iNetdEventCallback : this.mNetdEventCallbackList) {
            if (iNetdEventCallback != null) {
                iNetdEventCallback.onConnectEvent(str, i4, jCurrentTimeMillis, i5);
            }
        }
    }

    @Override
    public synchronized void onWakeupEvent(String str, int i, int i2, int i3, byte[] bArr, String str2, String str3, int i4, int i5, long j) {
        long jCurrentTimeMillis;
        String strReplaceFirst = str.replaceFirst(WAKEUP_EVENT_IFACE_PREFIX, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        if (j > 0) {
            jCurrentTimeMillis = j / 1000000;
        } else {
            jCurrentTimeMillis = System.currentTimeMillis();
        }
        WakeupEvent wakeupEvent = new WakeupEvent();
        wakeupEvent.iface = strReplaceFirst;
        wakeupEvent.timestampMs = jCurrentTimeMillis;
        wakeupEvent.uid = i;
        wakeupEvent.ethertype = i2;
        wakeupEvent.dstHwAddr = MacAddress.fromBytes(bArr);
        wakeupEvent.srcIp = str2;
        wakeupEvent.dstIp = str3;
        wakeupEvent.ipNextHeader = i3;
        wakeupEvent.srcPort = i4;
        wakeupEvent.dstPort = i5;
        addWakeupEvent(wakeupEvent);
        StatsLog.write(44, i, strReplaceFirst, i2, wakeupEvent.dstHwAddr.toString(), str2, str3, i3, i4, i5);
    }

    @Override
    public synchronized void onTcpSocketStatsEvent(int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4, int[] iArr5) {
        if (iArr.length == iArr2.length && iArr.length == iArr3.length && iArr.length == iArr4.length && iArr.length == iArr5.length) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            for (int i = 0; i < iArr.length; i++) {
                int i2 = iArr[i];
                getMetricsForNetwork(jCurrentTimeMillis, i2).addTcpStatsResult(iArr2[i], iArr3[i], iArr4[i], iArr5[i]);
            }
            return;
        }
        Log.e(TAG, "Mismatched lengths of TCP socket stats data arrays");
    }

    private void addWakeupEvent(WakeupEvent wakeupEvent) {
        String str = wakeupEvent.iface;
        this.mWakeupEvents.append(wakeupEvent);
        WakeupStats wakeupStats = this.mWakeupStats.get(str);
        if (wakeupStats == null) {
            wakeupStats = new WakeupStats(str);
            this.mWakeupStats.put(str, wakeupStats);
        }
        wakeupStats.countEvent(wakeupEvent);
    }

    public synchronized void flushStatistics(List<IpConnectivityLogClass.IpConnectivityEvent> list) {
        for (int i = 0; i < this.mNetworkMetrics.size(); i++) {
            ConnectStats connectStats = this.mNetworkMetrics.valueAt(i).connectMetrics;
            if (connectStats.eventCount != 0) {
                list.add(IpConnectivityEventBuilder.toProto(connectStats));
            }
        }
        for (int i2 = 0; i2 < this.mNetworkMetrics.size(); i2++) {
            DnsEvent dnsEvent = this.mNetworkMetrics.valueAt(i2).dnsMetrics;
            if (dnsEvent.eventCount != 0) {
                list.add(IpConnectivityEventBuilder.toProto(dnsEvent));
            }
        }
        for (int i3 = 0; i3 < this.mWakeupStats.size(); i3++) {
            list.add(IpConnectivityEventBuilder.toProto(this.mWakeupStats.valueAt(i3)));
        }
        this.mNetworkMetrics.clear();
        this.mWakeupStats.clear();
    }

    public synchronized void list(PrintWriter printWriter) {
        printWriter.println("dns/connect events:");
        for (int i = 0; i < this.mNetworkMetrics.size(); i++) {
            printWriter.println(this.mNetworkMetrics.valueAt(i).connectMetrics);
        }
        for (int i2 = 0; i2 < this.mNetworkMetrics.size(); i2++) {
            printWriter.println(this.mNetworkMetrics.valueAt(i2).dnsMetrics);
        }
        printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        printWriter.println("network statistics:");
        for (NetworkMetricsSnapshot networkMetricsSnapshot : getNetworkMetricsSnapshots()) {
            printWriter.println(networkMetricsSnapshot);
        }
        printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        printWriter.println("packet wakeup events:");
        for (int i3 = 0; i3 < this.mWakeupStats.size(); i3++) {
            printWriter.println(this.mWakeupStats.valueAt(i3));
        }
        for (WakeupEvent wakeupEvent : (WakeupEvent[]) this.mWakeupEvents.toArray()) {
            printWriter.println(wakeupEvent);
        }
    }

    public synchronized void listAsProtos(PrintWriter printWriter) {
        for (int i = 0; i < this.mNetworkMetrics.size(); i++) {
            printWriter.print(IpConnectivityEventBuilder.toProto(this.mNetworkMetrics.valueAt(i).connectMetrics));
        }
        for (int i2 = 0; i2 < this.mNetworkMetrics.size(); i2++) {
            printWriter.print(IpConnectivityEventBuilder.toProto(this.mNetworkMetrics.valueAt(i2).dnsMetrics));
        }
        for (int i3 = 0; i3 < this.mWakeupStats.size(); i3++) {
            printWriter.print(IpConnectivityEventBuilder.toProto(this.mWakeupStats.valueAt(i3)));
        }
    }

    private long getTransports(int i) {
        NetworkCapabilities networkCapabilities = this.mCm.getNetworkCapabilities(new Network(i));
        if (networkCapabilities == null) {
            return 0L;
        }
        return BitUtils.packBits(networkCapabilities.getTransportTypes());
    }

    private static void maybeLog(String str, Object... objArr) {
    }

    static class NetworkMetricsSnapshot {
        public List<NetworkMetrics.Summary> stats = new ArrayList();
        public long timeMs;

        NetworkMetricsSnapshot() {
        }

        static NetworkMetricsSnapshot collect(long j, SparseArray<NetworkMetrics> sparseArray) {
            NetworkMetricsSnapshot networkMetricsSnapshot = new NetworkMetricsSnapshot();
            networkMetricsSnapshot.timeMs = j;
            for (int i = 0; i < sparseArray.size(); i++) {
                NetworkMetrics.Summary pendingStats = sparseArray.valueAt(i).getPendingStats();
                if (pendingStats != null) {
                    networkMetricsSnapshot.stats.add(pendingStats);
                }
            }
            return networkMetricsSnapshot;
        }

        public String toString() {
            StringJoiner stringJoiner = new StringJoiner(", ");
            Iterator<NetworkMetrics.Summary> it = this.stats.iterator();
            while (it.hasNext()) {
                stringJoiner.add(it.next().toString());
            }
            return String.format("%tT.%tL: %s", Long.valueOf(this.timeMs), Long.valueOf(this.timeMs), stringJoiner.toString());
        }
    }
}
