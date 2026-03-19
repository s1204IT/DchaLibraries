package com.android.internal.net;

import android.net.NetworkStats;
import android.os.StrictMode;
import android.os.SystemClock;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ProcFileReader;
import com.android.server.NetworkManagementSocketTagger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import libcore.io.IoUtils;

public class NetworkStatsFactory {
    private static final boolean SANITY_CHECK_NATIVE = false;
    private static final String TAG = "NetworkStatsFactory";
    private static final boolean USE_NATIVE_PARSING = true;
    private static final ConcurrentHashMap<String, String> sStackedIfaces = new ConcurrentHashMap<>();
    private final File mStatsXtIfaceAll;
    private final File mStatsXtIfaceFmt;
    private final File mStatsXtUid;
    private boolean mUseBpfStats;

    @VisibleForTesting
    public static native int nativeReadNetworkStatsDetail(NetworkStats networkStats, String str, int i, String[] strArr, int i2, boolean z);

    @VisibleForTesting
    public static native int nativeReadNetworkStatsDev(NetworkStats networkStats);

    public static void noteStackedIface(String str, String str2) {
        if (str != null && str2 != null) {
            sStackedIfaces.put(str, str2);
        }
    }

    public static String[] augmentWithStackedInterfaces(String[] strArr) {
        if (strArr == NetworkStats.INTERFACES_ALL) {
            return null;
        }
        HashSet hashSet = new HashSet(Arrays.asList(strArr));
        for (Map.Entry<String, String> entry : sStackedIfaces.entrySet()) {
            if (hashSet.contains(entry.getKey())) {
                hashSet.add(entry.getValue());
            } else if (hashSet.contains(entry.getValue())) {
                hashSet.add(entry.getKey());
            }
        }
        return (String[]) hashSet.toArray(new String[hashSet.size()]);
    }

    public static void apply464xlatAdjustments(NetworkStats networkStats, NetworkStats networkStats2) {
        NetworkStats.apply464xlatAdjustments(networkStats, networkStats2, sStackedIfaces);
    }

    @VisibleForTesting
    public static void clearStackedIfaces() {
        sStackedIfaces.clear();
    }

    public NetworkStatsFactory() {
        this(new File("/proc/"), new File("/sys/fs/bpf/traffic_uid_stats_map").exists());
    }

    @VisibleForTesting
    public NetworkStatsFactory(File file, boolean z) {
        this.mStatsXtIfaceAll = new File(file, "net/xt_qtaguid/iface_stat_all");
        this.mStatsXtIfaceFmt = new File(file, "net/xt_qtaguid/iface_stat_fmt");
        this.mStatsXtUid = new File(file, "net/xt_qtaguid/stats");
        this.mUseBpfStats = z;
    }

    public NetworkStats readBpfNetworkStatsDev() throws IOException {
        NetworkStats networkStats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        if (nativeReadNetworkStatsDev(networkStats) != 0) {
            throw new IOException("Failed to parse bpf iface stats");
        }
        return networkStats;
    }

    public NetworkStats readNetworkStatsSummaryDev() throws Throwable {
        ProcFileReader procFileReader;
        if (this.mUseBpfStats) {
            return readBpfNetworkStatsDev();
        }
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
        NetworkStats networkStats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        ProcFileReader procFileReader2 = null;
        try {
            try {
                procFileReader = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceAll));
                while (procFileReader.hasMoreData()) {
                    try {
                        entry.iface = procFileReader.nextString();
                        entry.uid = -1;
                        entry.set = -1;
                        entry.tag = 0;
                        boolean z = procFileReader.nextInt() != 0;
                        entry.rxBytes = procFileReader.nextLong();
                        entry.rxPackets = procFileReader.nextLong();
                        entry.txBytes = procFileReader.nextLong();
                        entry.txPackets = procFileReader.nextLong();
                        if (z) {
                            entry.rxBytes += procFileReader.nextLong();
                            entry.rxPackets += procFileReader.nextLong();
                            entry.txBytes += procFileReader.nextLong();
                            entry.txPackets += procFileReader.nextLong();
                        }
                        networkStats.addValues(entry);
                        procFileReader.finishLine();
                    } catch (NullPointerException | NumberFormatException e) {
                        e = e;
                        procFileReader2 = procFileReader;
                        throw new ProtocolException("problem parsing stats", e);
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(procFileReader);
                        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(procFileReader);
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                return networkStats;
            } catch (NullPointerException | NumberFormatException e2) {
                e = e2;
            }
        } catch (Throwable th2) {
            th = th2;
            procFileReader = procFileReader2;
        }
    }

    public NetworkStats readNetworkStatsSummaryXt() throws Throwable {
        ProcFileReader procFileReader;
        if (this.mUseBpfStats) {
            return readBpfNetworkStatsDev();
        }
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
        ProcFileReader procFileReader2 = null;
        if (!this.mStatsXtIfaceFmt.exists()) {
            return null;
        }
        NetworkStats networkStats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        try {
            try {
                procFileReader = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceFmt));
            } catch (NullPointerException | NumberFormatException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
            procFileReader = procFileReader2;
        }
        try {
            procFileReader.finishLine();
            while (procFileReader.hasMoreData()) {
                entry.iface = procFileReader.nextString();
                entry.uid = -1;
                entry.set = -1;
                entry.tag = 0;
                entry.rxBytes = procFileReader.nextLong();
                entry.rxPackets = procFileReader.nextLong();
                entry.txBytes = procFileReader.nextLong();
                entry.txPackets = procFileReader.nextLong();
                networkStats.addValues(entry);
                procFileReader.finishLine();
            }
            IoUtils.closeQuietly(procFileReader);
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
            return networkStats;
        } catch (NullPointerException | NumberFormatException e2) {
            e = e2;
            procFileReader2 = procFileReader;
            throw new ProtocolException("problem parsing stats", e);
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly(procFileReader);
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
            throw th;
        }
    }

    public NetworkStats readNetworkStatsDetail() throws IOException {
        return readNetworkStatsDetail(-1, null, -1, null);
    }

    public NetworkStats readNetworkStatsDetail(int i, String[] strArr, int i2, NetworkStats networkStats) throws IOException {
        NetworkStats networkStatsDetailInternal = readNetworkStatsDetailInternal(i, strArr, i2, networkStats);
        networkStatsDetailInternal.apply464xlatAdjustments(sStackedIfaces);
        return networkStatsDetailInternal;
    }

    private NetworkStats readNetworkStatsDetailInternal(int i, String[] strArr, int i2, NetworkStats networkStats) throws IOException {
        if (networkStats != null) {
            networkStats.setElapsedRealtime(SystemClock.elapsedRealtime());
        } else {
            networkStats = new NetworkStats(SystemClock.elapsedRealtime(), -1);
        }
        if (nativeReadNetworkStatsDetail(networkStats, this.mStatsXtUid.getAbsolutePath(), i, strArr, i2, this.mUseBpfStats) != 0) {
            throw new IOException("Failed to parse network stats");
        }
        return networkStats;
    }

    @VisibleForTesting
    public static NetworkStats javaReadNetworkStatsDetail(File file, int i, String[] strArr, int i2) throws Throwable {
        ProcFileReader procFileReader;
        int iNextInt;
        int i3;
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskReads = StrictMode.allowThreadDiskReads();
        NetworkStats networkStats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        int i4 = 1;
        try {
            procFileReader = new ProcFileReader(new FileInputStream(file));
            try {
                try {
                    procFileReader.finishLine();
                    i3 = 1;
                } catch (NullPointerException | NumberFormatException e) {
                    e = e;
                    iNextInt = 1;
                }
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(procFileReader);
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
                throw th;
            }
        } catch (NullPointerException | NumberFormatException e2) {
            e = e2;
            procFileReader = null;
        } catch (Throwable th2) {
            th = th2;
            procFileReader = null;
            IoUtils.closeQuietly(procFileReader);
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
            throw th;
        }
        while (procFileReader.hasMoreData()) {
            try {
                iNextInt = procFileReader.nextInt();
                if (iNextInt != i4 + 1) {
                    throw new ProtocolException("inconsistent idx=" + iNextInt + " after lastIdx=" + i4);
                }
                try {
                    entry.iface = procFileReader.nextString();
                    entry.tag = NetworkManagementSocketTagger.kernelToTag(procFileReader.nextString());
                    entry.uid = procFileReader.nextInt();
                    entry.set = procFileReader.nextInt();
                    entry.rxBytes = procFileReader.nextLong();
                    entry.rxPackets = procFileReader.nextLong();
                    entry.txBytes = procFileReader.nextLong();
                    entry.txPackets = procFileReader.nextLong();
                    if ((strArr == null || ArrayUtils.contains(strArr, entry.iface)) && ((i == -1 || i == entry.uid) && (i2 == -1 || i2 == entry.tag))) {
                        networkStats.addValues(entry);
                    }
                    procFileReader.finishLine();
                    i3 = iNextInt;
                    i4 = i3;
                } catch (NullPointerException | NumberFormatException e3) {
                    e = e3;
                }
                e = e3;
            } catch (NullPointerException | NumberFormatException e4) {
                iNextInt = i3;
                e = e4;
            }
            throw new ProtocolException("problem parsing idx " + iNextInt, e);
        }
        IoUtils.closeQuietly(procFileReader);
        StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskReads);
        return networkStats;
    }

    public void assertEquals(NetworkStats networkStats, NetworkStats networkStats2) {
        if (networkStats.size() != networkStats2.size()) {
            throw new AssertionError("Expected size " + networkStats.size() + ", actual size " + networkStats2.size());
        }
        NetworkStats.Entry values = null;
        NetworkStats.Entry values2 = null;
        for (int i = 0; i < networkStats.size(); i++) {
            values = networkStats.getValues(i, values);
            values2 = networkStats2.getValues(i, values2);
            if (!values.equals(values2)) {
                throw new AssertionError("Expected row " + i + ": " + values + ", actual row " + values2);
            }
        }
    }
}
