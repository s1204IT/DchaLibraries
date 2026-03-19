package android.net.arp;

import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.system.StructTimeval;
import android.util.Log;
import com.android.internal.util.HexDump;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import libcore.io.IoBridge;

public class ArpPeer {
    private static final int ARP_LENGTH = 28;
    private static final int ARP_TYPE = 2054;
    private static final boolean DBG = false;
    private static final int ETHERNET_LENGTH = 14;
    private static final int ETHERNET_TYPE = 1;
    private static final int IPV4_LENGTH = 4;
    private static final byte[] L2_BROADCAST = {-1, -1, -1, -1, -1, -1};
    private static final int MAC_ADDR_LENGTH = 6;
    private static final int MAX_LENGTH = 1500;
    private static final boolean PKT_DBG = false;
    private static final String TAG = "ArpPeer";
    private byte[] mHwAddr;
    private NetworkInterface mIface;
    private String mIfaceName;
    private PacketSocketAddress mInterfaceBroadcastAddr;
    private final InetAddress mMyAddr;
    private final byte[] mMyMac = new byte[6];
    private final InetAddress mPeer;
    private FileDescriptor mSocket;

    public ArpPeer(String interfaceName, InetAddress myAddr, InetAddress peer) throws SocketException {
        this.mIfaceName = interfaceName;
        this.mMyAddr = myAddr;
        if ((myAddr instanceof Inet6Address) || (peer instanceof Inet6Address)) {
            throw new IllegalArgumentException("IPv6 unsupported");
        }
        this.mPeer = peer;
        initInterface();
        initSocket();
        Log.i(TAG, "ArpPeer in " + interfaceName + ":" + myAddr + ":" + peer);
    }

    private boolean initInterface() {
        try {
            this.mIface = NetworkInterface.getByName(this.mIfaceName);
            this.mHwAddr = this.mIface.getHardwareAddress();
            Log.i(TAG, "mac addr:" + HexDump.dumpHexString(this.mHwAddr) + ":" + this.mIface.getIndex());
            this.mInterfaceBroadcastAddr = new PacketSocketAddress(this.mIface.getIndex(), L2_BROADCAST);
            this.mInterfaceBroadcastAddr.sll_protocol = (short) 2054;
            return true;
        } catch (SocketException e) {
            Log.wtf(TAG, "Can't determine ifindex or MAC address for " + this.mIfaceName);
            return false;
        }
    }

    private boolean initSocket() {
        try {
            this.mSocket = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_ARP);
            PacketSocketAddress addr = new PacketSocketAddress((short) OsConstants.ETH_P_ARP, this.mIface.getIndex());
            Os.bind(this.mSocket, addr);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating packet socket", e);
            return false;
        }
    }

    public byte[] doArp(int timeoutMillis) throws ErrnoException {
        ByteBuffer buf = ByteBuffer.allocate(MAX_LENGTH);
        byte[] desiredIp = this.mPeer.getAddress();
        Log.i(TAG, "My MAC:" + HexDump.dumpHexString(this.mMyAddr.getAddress()));
        long timeout = SystemClock.elapsedRealtime() + ((long) timeoutMillis);
        Log.i(TAG, "doArp in " + timeoutMillis);
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put(L2_BROADCAST);
        buf.put(this.mHwAddr);
        buf.putShort((short) 2054);
        buf.putShort((short) 1);
        buf.putShort((short) OsConstants.ETH_P_IP);
        buf.put((byte) 6);
        buf.put((byte) 4);
        buf.putShort((short) 1);
        buf.put(this.mHwAddr);
        buf.put(this.mMyAddr.getAddress());
        buf.put(new byte[6]);
        buf.put(desiredIp);
        buf.flip();
        try {
            Os.sendto(this.mSocket, buf.array(), 0, buf.limit(), 0, this.mInterfaceBroadcastAddr);
            byte[] socketBuf = new byte[MAX_LENGTH];
            while (SystemClock.elapsedRealtime() < timeout) {
                long duration = timeout - SystemClock.elapsedRealtime();
                StructTimeval t = StructTimeval.fromMillis(duration);
                Os.setsockoptTimeval(this.mSocket, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, t);
                Log.i(TAG, "Wait ARP reply in " + duration);
                try {
                    int readLen = Os.read(this.mSocket, socketBuf, 0, MAX_LENGTH);
                    Log.i(TAG, "readLen: " + readLen);
                    if (readLen >= 42) {
                        byte[] recvBuf = new byte[28];
                        System.arraycopy(socketBuf, 14, recvBuf, 0, 28);
                        if (recvBuf[0] == 0 && recvBuf[1] == 1 && recvBuf[2] == 8 && recvBuf[3] == 0 && recvBuf[4] == 6 && recvBuf[5] == 4 && recvBuf[6] == 0 && recvBuf[7] == 2 && recvBuf[14] == desiredIp[0] && recvBuf[15] == desiredIp[1] && recvBuf[16] == desiredIp[2] && recvBuf[17] == desiredIp[3]) {
                            byte[] result = new byte[6];
                            System.arraycopy(recvBuf, 8, result, 0, 6);
                            Log.i(TAG, "target mac addr:" + HexDump.dumpHexString(result));
                            return result;
                        }
                    }
                } catch (Exception se) {
                    Log.e(TAG, "ARP read failure: " + se);
                    return null;
                }
            }
            return null;
        } catch (Exception se2) {
            Log.e(TAG, "ARP send failure: " + se2);
            return null;
        }
    }

    public static boolean doArp(String interfaceName, InetAddress myAddr, InetAddress peerAddr, int timeoutMillis) {
        return doArp(interfaceName, myAddr, peerAddr, timeoutMillis, 2);
    }

    public static boolean doArp(String interfaceName, InetAddress myAddr, InetAddress peerAddr, int timeoutMillis, int totalTimes) throws Throwable {
        ArpPeer peer;
        int responses;
        ArpPeer peer2 = null;
        try {
            try {
                peer = new ArpPeer(interfaceName, myAddr, peerAddr);
                responses = 0;
                for (int i = 0; i < totalTimes; i++) {
                    try {
                        if (peer.doArp(timeoutMillis) != null) {
                            responses++;
                        }
                    } catch (ErrnoException | SocketException e) {
                        se = e;
                        peer2 = peer;
                        Log.e(TAG, "ARP test initiation failure: " + se);
                        if (peer2 != null) {
                            peer2.close();
                        }
                        return false;
                    } catch (Exception e2) {
                        e = e2;
                        peer2 = peer;
                        Log.e(TAG, "exception:" + e);
                        if (peer2 != null) {
                            peer2.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        peer2 = peer;
                        if (peer2 != null) {
                            peer2.close();
                        }
                        throw th;
                    }
                }
                Log.d(TAG, "ARP test result: " + responses);
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (ErrnoException | SocketException e3) {
            se = e3;
        } catch (Exception e4) {
            e = e4;
        }
        if (responses == totalTimes) {
            if (peer != null) {
                peer.close();
            }
            return true;
        }
        if (peer != null) {
            peer.close();
        }
        peer2 = peer;
        return false;
    }

    public void close() {
        Log.i(TAG, "Close arp");
        closeQuietly(this.mSocket);
        this.mSocket = null;
    }

    private static void closeQuietly(FileDescriptor fd) {
        try {
            IoBridge.closeAndSignalBlockedThreads(fd);
        } catch (IOException e) {
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mSocket != null) {
                Log.wtf(TAG, " ArpPeer was finalized without closing");
                close();
            }
        } finally {
            super.finalize();
        }
    }
}
