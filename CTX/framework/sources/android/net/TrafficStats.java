package android.net;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.Context;
import android.net.INetworkStatsService;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.server.NetworkManagementSocketTagger;
import dalvik.system.SocketTagger;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

public class TrafficStats {

    @Deprecated
    public static final long GB_IN_BYTES = 1073741824;

    @Deprecated
    public static final long KB_IN_BYTES = 1024;
    private static final String LOOPBACK_IFACE = "lo";

    @Deprecated
    public static final long MB_IN_BYTES = 1048576;

    @Deprecated
    public static final long PB_IN_BYTES = 1125899906842624L;
    public static final int TAG_SYSTEM_APP = -251;
    public static final int TAG_SYSTEM_BACKUP = -253;
    public static final int TAG_SYSTEM_DHCP = -192;
    public static final int TAG_SYSTEM_DOWNLOAD = -255;
    public static final int TAG_SYSTEM_GPS = -188;
    public static final int TAG_SYSTEM_MEDIA = -254;
    public static final int TAG_SYSTEM_NEIGHBOR = -189;
    public static final int TAG_SYSTEM_NTP = -191;
    public static final int TAG_SYSTEM_PAC = -187;
    public static final int TAG_SYSTEM_PROBE = -190;
    public static final int TAG_SYSTEM_RESTORE = -252;

    @Deprecated
    public static final long TB_IN_BYTES = 1099511627776L;
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

    private static synchronized INetworkStatsService getStatsService() {
        if (sStatsService == null) {
            sStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        }
        return sStatsService;
    }

    public static void setThreadStatsTag(int i) {
        NetworkManagementSocketTagger.setThreadSocketStatsTag(i);
    }

    public static int getAndSetThreadStatsTag(int i) {
        return NetworkManagementSocketTagger.setThreadSocketStatsTag(i);
    }

    @SystemApi
    public static void setThreadStatsTagBackup() {
        setThreadStatsTag(TAG_SYSTEM_BACKUP);
    }

    @SystemApi
    public static void setThreadStatsTagRestore() {
        setThreadStatsTag(TAG_SYSTEM_RESTORE);
    }

    @SystemApi
    public static void setThreadStatsTagApp() {
        setThreadStatsTag(TAG_SYSTEM_APP);
    }

    public static int getThreadStatsTag() {
        return NetworkManagementSocketTagger.getThreadSocketStatsTag();
    }

    public static void clearThreadStatsTag() {
        NetworkManagementSocketTagger.setThreadSocketStatsTag(-1);
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public static void setThreadStatsUid(int i) {
        NetworkManagementSocketTagger.setThreadSocketStatsUid(i);
    }

    public static int getThreadStatsUid() {
        return NetworkManagementSocketTagger.getThreadSocketStatsUid();
    }

    @Deprecated
    public static void setThreadStatsUidSelf() {
        setThreadStatsUid(Process.myUid());
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public static void clearThreadStatsUid() {
        NetworkManagementSocketTagger.setThreadSocketStatsUid(-1);
    }

    public static void tagSocket(Socket socket) throws SocketException {
        SocketTagger.get().tag(socket);
    }

    public static void untagSocket(Socket socket) throws SocketException {
        SocketTagger.get().untag(socket);
    }

    public static void tagDatagramSocket(DatagramSocket datagramSocket) throws SocketException {
        SocketTagger.get().tag(datagramSocket);
    }

    public static void untagDatagramSocket(DatagramSocket datagramSocket) throws SocketException {
        SocketTagger.get().untag(datagramSocket);
    }

    public static void tagFileDescriptor(FileDescriptor fileDescriptor) throws IOException {
        SocketTagger.get().tag(fileDescriptor);
    }

    public static void untagFileDescriptor(FileDescriptor fileDescriptor) throws IOException {
        SocketTagger.get().untag(fileDescriptor);
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
        NetworkStats networkStatsSubtract;
        synchronized (sProfilingLock) {
            if (sActiveProfilingStart == null) {
                throw new IllegalStateException("not profiling data");
            }
            networkStatsSubtract = NetworkStats.subtract(getDataLayerSnapshotForUid(context), sActiveProfilingStart, null, null);
            sActiveProfilingStart = null;
        }
        return networkStatsSubtract;
    }

    public static void incrementOperationCount(int i) {
        incrementOperationCount(getThreadStatsTag(), i);
    }

    public static void incrementOperationCount(int i, int i2) {
        try {
            getStatsService().incrementOperationCount(Process.myUid(), i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static void closeQuietly(INetworkStatsSession iNetworkStatsSession) {
        if (iNetworkStatsSession != null) {
            try {
                iNetworkStatsSession.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e2) {
            }
        }
    }

    private static long addIfSupported(long j) {
        if (j == -1) {
            return 0L;
        }
        return j;
    }

    public static long getMobileTxPackets() {
        long jAddIfSupported = 0;
        for (String str : getMobileIfaces()) {
            jAddIfSupported += addIfSupported(getTxPackets(str));
        }
        return jAddIfSupported;
    }

    public static long getMobileRxPackets() {
        long jAddIfSupported = 0;
        for (String str : getMobileIfaces()) {
            jAddIfSupported += addIfSupported(getRxPackets(str));
        }
        return jAddIfSupported;
    }

    public static long getMobileTxBytes() {
        long jAddIfSupported = 0;
        for (String str : getMobileIfaces()) {
            jAddIfSupported += addIfSupported(getTxBytes(str));
        }
        return jAddIfSupported;
    }

    public static long getMobileRxBytes() {
        long jAddIfSupported = 0;
        for (String str : getMobileIfaces()) {
            jAddIfSupported += addIfSupported(getRxBytes(str));
        }
        return jAddIfSupported;
    }

    public static long getMobileTcpRxPackets() {
        long jAddIfSupported = 0;
        for (String str : getMobileIfaces()) {
            try {
                jAddIfSupported += addIfSupported(getStatsService().getIfaceStats(str, 4));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return jAddIfSupported;
    }

    public static long getMobileTcpTxPackets() {
        long jAddIfSupported = 0;
        for (String str : getMobileIfaces()) {
            try {
                jAddIfSupported += addIfSupported(getStatsService().getIfaceStats(str, 5));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return jAddIfSupported;
    }

    public static long getTxPackets(String str) {
        try {
            return getStatsService().getIfaceStats(str, 3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getRxPackets(String str) {
        try {
            return getStatsService().getIfaceStats(str, 1);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getTxBytes(String str) {
        try {
            return getStatsService().getIfaceStats(str, 2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getRxBytes(String str) {
        try {
            return getStatsService().getIfaceStats(str, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getLoopbackTxPackets() {
        try {
            return getStatsService().getIfaceStats(LOOPBACK_IFACE, 3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getLoopbackRxPackets() {
        try {
            return getStatsService().getIfaceStats(LOOPBACK_IFACE, 1);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getLoopbackTxBytes() {
        try {
            return getStatsService().getIfaceStats(LOOPBACK_IFACE, 2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getLoopbackRxBytes() {
        try {
            return getStatsService().getIfaceStats(LOOPBACK_IFACE, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getTotalTxPackets() {
        try {
            return getStatsService().getTotalStats(3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getTotalRxPackets() {
        try {
            return getStatsService().getTotalStats(1);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getTotalTxBytes() {
        try {
            return getStatsService().getTotalStats(2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getTotalRxBytes() {
        try {
            return getStatsService().getTotalStats(0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static long getUidTxBytes(int i) {
        int iMyUid = Process.myUid();
        if (iMyUid == 1000 || iMyUid == i) {
            try {
                return getStatsService().getUidStats(i, 2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -1L;
    }

    public static long getUidRxBytes(int i) {
        int iMyUid = Process.myUid();
        if (iMyUid == 1000 || iMyUid == i) {
            try {
                return getStatsService().getUidStats(i, 0);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -1L;
    }

    public static long getUidTxPackets(int i) {
        int iMyUid = Process.myUid();
        if (iMyUid == 1000 || iMyUid == i) {
            try {
                return getStatsService().getUidStats(i, 3);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -1L;
    }

    public static long getUidRxPackets(int i) {
        int iMyUid = Process.myUid();
        if (iMyUid == 1000 || iMyUid == i) {
            try {
                return getStatsService().getUidStats(i, 1);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -1L;
    }

    @Deprecated
    public static long getUidTcpTxBytes(int i) {
        return -1L;
    }

    @Deprecated
    public static long getUidTcpRxBytes(int i) {
        return -1L;
    }

    @Deprecated
    public static long getUidUdpTxBytes(int i) {
        return -1L;
    }

    @Deprecated
    public static long getUidUdpRxBytes(int i) {
        return -1L;
    }

    @Deprecated
    public static long getUidTcpTxSegments(int i) {
        return -1L;
    }

    @Deprecated
    public static long getUidTcpRxSegments(int i) {
        return -1L;
    }

    @Deprecated
    public static long getUidUdpTxPackets(int i) {
        return -1L;
    }

    @Deprecated
    public static long getUidUdpRxPackets(int i) {
        return -1L;
    }

    private static NetworkStats getDataLayerSnapshotForUid(Context context) {
        try {
            return getStatsService().getDataLayerSnapshotForUid(Process.myUid());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static String[] getMobileIfaces() {
        try {
            return getStatsService().getMobileIfaces();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
