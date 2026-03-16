package android.net;

import android.content.Context;
import android.net.INetworkStatsService;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.server.NetworkManagementSocketTagger;
import dalvik.system.SocketTagger;
import java.net.Socket;
import java.net.SocketException;

public class TrafficStats {
    public static final long GB_IN_BYTES = 1073741824;
    public static final long KB_IN_BYTES = 1024;
    public static final long MB_IN_BYTES = 1048576;
    public static final int TAG_SYSTEM_BACKUP = -253;
    public static final int TAG_SYSTEM_DOWNLOAD = -255;
    public static final int TAG_SYSTEM_MEDIA = -254;
    private static final int TYPE_RX_BYTES = 0;
    private static final int TYPE_RX_PACKETS = 1;
    private static final int TYPE_TCP_RX_PACKETS = 4;
    private static final int TYPE_TCP_TX_PACKETS = 5;
    private static final int TYPE_TX_BYTES = 2;
    private static final int TYPE_TX_PACKETS = 3;
    public static final int UID_REMOVED = -4;
    public static final int UID_TETHERING = -5;
    public static final int UNSUPPORTED = -1;
    private static NetworkStats sActiveProfilingStart;
    private static Object sProfilingLock = new Object();
    private static INetworkStatsService sStatsService;

    private static native long nativeGetIfaceStat(String str, int i);

    private static native long nativeGetTotalStat(int i);

    private static native long nativeGetUidStat(int i, int i2);

    private static synchronized INetworkStatsService getStatsService() {
        if (sStatsService == null) {
            sStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        }
        return sStatsService;
    }

    public static void setThreadStatsTag(int tag) {
        NetworkManagementSocketTagger.setThreadSocketStatsTag(tag);
    }

    public static void setThreadStatsTagBackup() {
        setThreadStatsTag(TAG_SYSTEM_BACKUP);
    }

    public static int getThreadStatsTag() {
        return NetworkManagementSocketTagger.getThreadSocketStatsTag();
    }

    public static void clearThreadStatsTag() {
        NetworkManagementSocketTagger.setThreadSocketStatsTag(-1);
    }

    public static void setThreadStatsUid(int uid) {
        NetworkManagementSocketTagger.setThreadSocketStatsUid(uid);
    }

    public static void clearThreadStatsUid() {
        NetworkManagementSocketTagger.setThreadSocketStatsUid(-1);
    }

    public static void tagSocket(Socket socket) throws SocketException {
        SocketTagger.get().tag(socket);
    }

    public static void untagSocket(Socket socket) throws SocketException {
        SocketTagger.get().untag(socket);
    }

    public static void startDataProfiling(Context context) {
        synchronized (sProfilingLock) {
            if (sActiveProfilingStart != null) {
                throw new IllegalStateException("already profiling data");
            }
            sActiveProfilingStart = getDataLayerSnapshotForUid(context);
        }
    }

    public static NetworkStats stopDataProfiling(Context context) {
        NetworkStats profilingDelta;
        synchronized (sProfilingLock) {
            if (sActiveProfilingStart == null) {
                throw new IllegalStateException("not profiling data");
            }
            NetworkStats profilingStop = getDataLayerSnapshotForUid(context);
            profilingDelta = NetworkStats.subtract(profilingStop, sActiveProfilingStart, null, null);
            sActiveProfilingStart = null;
        }
        return profilingDelta;
    }

    public static void incrementOperationCount(int operationCount) {
        int tag = getThreadStatsTag();
        incrementOperationCount(tag, operationCount);
    }

    public static void incrementOperationCount(int tag, int operationCount) {
        int uid = Process.myUid();
        try {
            getStatsService().incrementOperationCount(uid, tag, operationCount);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeQuietly(INetworkStatsSession session) {
        if (session != null) {
            try {
                session.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    public static long getMobileTxPackets() {
        long total = 0;
        String[] arr$ = getMobileIfaces();
        for (String iface : arr$) {
            total += getTxPackets(iface);
        }
        return total;
    }

    public static long getMobileRxPackets() {
        long total = 0;
        String[] arr$ = getMobileIfaces();
        for (String iface : arr$) {
            total += getRxPackets(iface);
        }
        return total;
    }

    public static long getMobileTxBytes() {
        long total = 0;
        String[] arr$ = getMobileIfaces();
        for (String iface : arr$) {
            total += getTxBytes(iface);
        }
        return total;
    }

    public static long getMobileRxBytes() {
        long total = 0;
        String[] arr$ = getMobileIfaces();
        for (String iface : arr$) {
            total += getRxBytes(iface);
        }
        return total;
    }

    public static long getMobileTcpRxPackets() {
        long total = 0;
        String[] arr$ = getMobileIfaces();
        for (String iface : arr$) {
            long stat = nativeGetIfaceStat(iface, 4);
            if (stat != -1) {
                total += stat;
            }
        }
        return total;
    }

    public static long getMobileTcpTxPackets() {
        long total = 0;
        String[] arr$ = getMobileIfaces();
        for (String iface : arr$) {
            long stat = nativeGetIfaceStat(iface, 5);
            if (stat != -1) {
                total += stat;
            }
        }
        return total;
    }

    public static long getTxPackets(String iface) {
        return nativeGetIfaceStat(iface, 3);
    }

    public static long getRxPackets(String iface) {
        return nativeGetIfaceStat(iface, 1);
    }

    public static long getTxBytes(String iface) {
        return nativeGetIfaceStat(iface, 2);
    }

    public static long getRxBytes(String iface) {
        return nativeGetIfaceStat(iface, 0);
    }

    public static long getTotalTxPackets() {
        return nativeGetTotalStat(3);
    }

    public static long getTotalRxPackets() {
        return nativeGetTotalStat(1);
    }

    public static long getTotalTxBytes() {
        return nativeGetTotalStat(2);
    }

    public static long getTotalRxBytes() {
        return nativeGetTotalStat(0);
    }

    public static long getUidTxBytes(int uid) {
        return nativeGetUidStat(uid, 2);
    }

    public static long getUidRxBytes(int uid) {
        return nativeGetUidStat(uid, 0);
    }

    public static long getUidTxPackets(int uid) {
        return nativeGetUidStat(uid, 3);
    }

    public static long getUidRxPackets(int uid) {
        return nativeGetUidStat(uid, 1);
    }

    @Deprecated
    public static long getUidTcpTxBytes(int uid) {
        return -1L;
    }

    @Deprecated
    public static long getUidTcpRxBytes(int uid) {
        return -1L;
    }

    @Deprecated
    public static long getUidUdpTxBytes(int uid) {
        return -1L;
    }

    @Deprecated
    public static long getUidUdpRxBytes(int uid) {
        return -1L;
    }

    @Deprecated
    public static long getUidTcpTxSegments(int uid) {
        return -1L;
    }

    @Deprecated
    public static long getUidTcpRxSegments(int uid) {
        return -1L;
    }

    @Deprecated
    public static long getUidUdpTxPackets(int uid) {
        return -1L;
    }

    @Deprecated
    public static long getUidUdpRxPackets(int uid) {
        return -1L;
    }

    private static NetworkStats getDataLayerSnapshotForUid(Context context) {
        int uid = Process.myUid();
        try {
            return getStatsService().getDataLayerSnapshotForUid(uid);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private static String[] getMobileIfaces() {
        try {
            return getStatsService().getMobileIfaces();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
