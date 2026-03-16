package com.android.internal.net;

import android.net.NetworkStats;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.ArrayMap;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ProcFileReader;
import com.android.server.NetworkManagementSocketTagger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Objects;
import libcore.io.IoUtils;

public class NetworkStatsFactory {
    private static final boolean SANITY_CHECK_NATIVE = false;
    private static final String TAG = "NetworkStatsFactory";
    private static final boolean USE_NATIVE_PARSING = true;

    @GuardedBy("sStackedIfaces")
    private static final ArrayMap<String, String> sStackedIfaces = new ArrayMap<>();
    private final File mStatsXtIfaceAll;
    private final File mStatsXtIfaceFmt;
    private final File mStatsXtUid;

    public static native int nativeReadNetworkStatsDetail(NetworkStats networkStats, String str, int i, String[] strArr, int i2);

    public static void noteStackedIface(String stackedIface, String baseIface) {
        synchronized (sStackedIfaces) {
            if (baseIface != null) {
                sStackedIfaces.put(stackedIface, baseIface);
            } else {
                sStackedIfaces.remove(stackedIface);
            }
        }
    }

    public NetworkStatsFactory() {
        this(new File("/proc/"));
    }

    public NetworkStatsFactory(File procRoot) {
        this.mStatsXtIfaceAll = new File(procRoot, "net/xt_qtaguid/iface_stat_all");
        this.mStatsXtIfaceFmt = new File(procRoot, "net/xt_qtaguid/iface_stat_fmt");
        this.mStatsXtUid = new File(procRoot, "net/xt_qtaguid/stats");
    }

    public NetworkStats readNetworkStatsSummaryDev() throws Throwable {
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        ProcFileReader reader = null;
        try {
            try {
                ProcFileReader reader2 = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceAll));
                while (reader2.hasMoreData()) {
                    try {
                        entry.iface = reader2.nextString();
                        entry.uid = -1;
                        entry.set = -1;
                        entry.tag = 0;
                        boolean active = reader2.nextInt() != 0;
                        entry.rxBytes = reader2.nextLong();
                        entry.rxPackets = reader2.nextLong();
                        entry.txBytes = reader2.nextLong();
                        entry.txPackets = reader2.nextLong();
                        if (active) {
                            entry.rxBytes += reader2.nextLong();
                            entry.rxPackets += reader2.nextLong();
                            entry.txBytes += reader2.nextLong();
                            entry.txPackets += reader2.nextLong();
                        }
                        stats.addValues(entry);
                        reader2.finishLine();
                    } catch (NullPointerException e) {
                        e = e;
                        throw new ProtocolException("problem parsing stats", e);
                    } catch (NumberFormatException e2) {
                        e = e2;
                        throw new ProtocolException("problem parsing stats", e);
                    } catch (Throwable th) {
                        th = th;
                        reader = reader2;
                        IoUtils.closeQuietly(reader);
                        StrictMode.setThreadPolicy(savedPolicy);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(reader2);
                StrictMode.setThreadPolicy(savedPolicy);
                return stats;
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (NullPointerException e3) {
            e = e3;
        } catch (NumberFormatException e4) {
            e = e4;
        }
    }

    public NetworkStats readNetworkStatsSummaryXt() throws Throwable {
        ProcFileReader reader;
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        if (!this.mStatsXtIfaceFmt.exists()) {
            return null;
        }
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 6);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        ProcFileReader reader2 = null;
        try {
            try {
                reader = new ProcFileReader(new FileInputStream(this.mStatsXtIfaceFmt));
            } catch (Throwable th) {
                th = th;
            }
            try {
                reader.finishLine();
                while (reader.hasMoreData()) {
                    entry.iface = reader.nextString();
                    entry.uid = -1;
                    entry.set = -1;
                    entry.tag = 0;
                    entry.rxBytes = reader.nextLong();
                    entry.rxPackets = reader.nextLong();
                    entry.txBytes = reader.nextLong();
                    entry.txPackets = reader.nextLong();
                    stats.addValues(entry);
                    reader.finishLine();
                }
                IoUtils.closeQuietly(reader);
                StrictMode.setThreadPolicy(savedPolicy);
                return stats;
            } catch (NullPointerException e) {
                e = e;
                throw new ProtocolException("problem parsing stats", e);
            } catch (NumberFormatException e2) {
                e = e2;
                throw new ProtocolException("problem parsing stats", e);
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                IoUtils.closeQuietly(reader2);
                StrictMode.setThreadPolicy(savedPolicy);
                throw th;
            }
        } catch (NullPointerException e3) {
            e = e3;
        } catch (NumberFormatException e4) {
            e = e4;
        }
    }

    public NetworkStats readNetworkStatsDetail() throws IOException {
        return readNetworkStatsDetail(-1, null, -1, null);
    }

    public NetworkStats readNetworkStatsDetail(int limitUid, String[] limitIfaces, int limitTag, NetworkStats lastStats) throws IOException {
        NetworkStats stats = readNetworkStatsDetailInternal(limitUid, limitIfaces, limitTag, lastStats);
        synchronized (sStackedIfaces) {
            int size = sStackedIfaces.size();
            for (int i = 0; i < size; i++) {
                String stackedIface = sStackedIfaces.keyAt(i);
                String baseIface = sStackedIfaces.valueAt(i);
                NetworkStats.Entry adjust = new NetworkStats.Entry(baseIface, 0, 0, 0, 0L, 0L, 0L, 0L, 0L);
                NetworkStats.Entry entry = null;
                for (int j = 0; j < stats.size(); j++) {
                    entry = stats.getValues(j, entry);
                    if (Objects.equals(entry.iface, stackedIface)) {
                        adjust.txBytes -= entry.txBytes;
                        adjust.txPackets -= entry.txPackets;
                    }
                }
                stats.combineValues(adjust);
            }
        }
        NetworkStats.Entry entry2 = null;
        for (int i2 = 0; i2 < stats.size(); i2++) {
            entry2 = stats.getValues(i2, entry2);
            if (entry2.iface != null && entry2.iface.startsWith("clat")) {
                entry2.rxBytes = entry2.rxPackets * 20;
                entry2.rxPackets = 0L;
                entry2.txBytes = 0L;
                entry2.txPackets = 0L;
                stats.combineValues(entry2);
            }
        }
        return stats;
    }

    private NetworkStats readNetworkStatsDetailInternal(int limitUid, String[] limitIfaces, int limitTag, NetworkStats lastStats) throws IOException {
        NetworkStats stats;
        if (lastStats != null) {
            stats = lastStats;
            stats.setElapsedRealtime(SystemClock.elapsedRealtime());
        } else {
            stats = new NetworkStats(SystemClock.elapsedRealtime(), -1);
        }
        if (nativeReadNetworkStatsDetail(stats, this.mStatsXtUid.getAbsolutePath(), limitUid, limitIfaces, limitTag) != 0) {
            throw new IOException("Failed to parse network stats");
        }
        return stats;
    }

    public static NetworkStats javaReadNetworkStatsDetail(File detailPath, int limitUid, String[] limitIfaces, int limitTag) throws Throwable {
        ProcFileReader reader;
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 24);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        int idx = 1;
        int lastIdx = 1;
        ProcFileReader reader2 = null;
        try {
            try {
                reader = new ProcFileReader(new FileInputStream(detailPath));
            } catch (Throwable th) {
                th = th;
            }
        } catch (NullPointerException e) {
            e = e;
        } catch (NumberFormatException e2) {
            e = e2;
        }
        try {
            reader.finishLine();
            while (reader.hasMoreData()) {
                idx = reader.nextInt();
                if (idx != lastIdx + 1) {
                    throw new ProtocolException("inconsistent idx=" + idx + " after lastIdx=" + lastIdx);
                }
                lastIdx = idx;
                entry.iface = reader.nextString();
                entry.tag = NetworkManagementSocketTagger.kernelToTag(reader.nextString());
                entry.uid = reader.nextInt();
                entry.set = reader.nextInt();
                entry.rxBytes = reader.nextLong();
                entry.rxPackets = reader.nextLong();
                entry.txBytes = reader.nextLong();
                entry.txPackets = reader.nextLong();
                if ((limitIfaces == null || ArrayUtils.contains(limitIfaces, entry.iface)) && ((limitUid == -1 || limitUid == entry.uid) && (limitTag == -1 || limitTag == entry.tag))) {
                    stats.addValues(entry);
                }
                reader.finishLine();
            }
            IoUtils.closeQuietly(reader);
            StrictMode.setThreadPolicy(savedPolicy);
            return stats;
        } catch (NullPointerException e3) {
            e = e3;
            throw new ProtocolException("problem parsing idx " + idx, e);
        } catch (NumberFormatException e4) {
            e = e4;
            throw new ProtocolException("problem parsing idx " + idx, e);
        } catch (Throwable th2) {
            th = th2;
            reader2 = reader;
            IoUtils.closeQuietly(reader2);
            StrictMode.setThreadPolicy(savedPolicy);
            throw th;
        }
    }

    public void assertEquals(NetworkStats expected, NetworkStats actual) {
        if (expected.size() != actual.size()) {
            throw new AssertionError("Expected size " + expected.size() + ", actual size " + actual.size());
        }
        NetworkStats.Entry expectedRow = null;
        NetworkStats.Entry actualRow = null;
        for (int i = 0; i < expected.size(); i++) {
            expectedRow = expected.getValues(i, expectedRow);
            actualRow = actual.getValues(i, actualRow);
            if (!expectedRow.equals(actualRow)) {
                throw new AssertionError("Expected row " + i + ": " + expectedRow + ", actual row " + actualRow);
            }
        }
    }
}
