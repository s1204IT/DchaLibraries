package com.mediatek.net.arp;

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
    private final byte[] mMyMac = new byte[MAC_ADDR_LENGTH];
    private final InetAddress mPeer;
    private FileDescriptor mSocket;

    public ArpPeer(String str, InetAddress inetAddress, InetAddress inetAddress2) throws SocketException {
        this.mIfaceName = str;
        this.mMyAddr = inetAddress;
        if ((inetAddress instanceof Inet6Address) || (inetAddress2 instanceof Inet6Address)) {
            throw new IllegalArgumentException("IPv6 unsupported");
        }
        this.mPeer = inetAddress2;
        if (!initInterface()) {
            throw new SocketException("initInterface() failed");
        }
        if (!initSocket()) {
            throw new SocketException("initSocket() failed");
        }
        Log.i(TAG, "ArpPeer in " + str + ":" + inetAddress + ":" + inetAddress2);
    }

    private boolean initInterface() {
        try {
            this.mIface = NetworkInterface.getByName(this.mIfaceName);
            if (this.mIface != null) {
                this.mHwAddr = this.mIface.getHardwareAddress();
                Log.i(TAG, "mac addr:" + HexDump.dumpHexString(this.mHwAddr) + ":" + this.mIface.getIndex());
                this.mInterfaceBroadcastAddr = new PacketSocketAddress(this.mIface.getIndex(), L2_BROADCAST);
                this.mInterfaceBroadcastAddr.sll_protocol = (short) 2054;
                return true;
            }
            Log.e(TAG, "mIface is null for name:" + this.mIfaceName);
            return false;
        } catch (SocketException e) {
            Log.e(TAG, "Can't determine ifindex or MAC address for " + this.mIfaceName);
            return false;
        }
    }

    private boolean initSocket() {
        try {
            this.mSocket = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_ARP);
            Os.bind(this.mSocket, new PacketSocketAddress((short) OsConstants.ETH_P_ARP, this.mIface.getIndex()));
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error creating packet socket", e);
            return false;
        }
    }

    public byte[] doArp(int i) throws ErrnoException {
        int i2 = MAX_LENGTH;
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(MAX_LENGTH);
        byte[] address = this.mPeer.getAddress();
        Log.i(TAG, "My MAC:" + HexDump.dumpHexString(this.mMyAddr.getAddress()));
        long jElapsedRealtime = SystemClock.elapsedRealtime() + ((long) i);
        Log.i(TAG, "doArp in " + i);
        byteBufferAllocate.clear();
        byteBufferAllocate.order(ByteOrder.BIG_ENDIAN);
        byteBufferAllocate.put(L2_BROADCAST);
        byteBufferAllocate.put(this.mHwAddr);
        byteBufferAllocate.putShort((short) 2054);
        byteBufferAllocate.putShort((short) 1);
        byteBufferAllocate.putShort((short) OsConstants.ETH_P_IP);
        byteBufferAllocate.put((byte) 6);
        byteBufferAllocate.put((byte) 4);
        byteBufferAllocate.putShort((short) 1);
        byteBufferAllocate.put(this.mHwAddr);
        byteBufferAllocate.put(this.mMyAddr.getAddress());
        byteBufferAllocate.put(new byte[MAC_ADDR_LENGTH]);
        byteBufferAllocate.put(address);
        byteBufferAllocate.flip();
        try {
            Os.sendto(this.mSocket, byteBufferAllocate.array(), 0, byteBufferAllocate.limit(), 0, this.mInterfaceBroadcastAddr);
            byte[] bArr = new byte[MAX_LENGTH];
            while (SystemClock.elapsedRealtime() < jElapsedRealtime) {
                long jElapsedRealtime2 = jElapsedRealtime - SystemClock.elapsedRealtime();
                Os.setsockoptTimeval(this.mSocket, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(jElapsedRealtime2));
                Log.i(TAG, "Wait ARP reply in " + jElapsedRealtime2);
                try {
                    int i3 = Os.read(this.mSocket, bArr, 0, i2);
                    Log.i(TAG, "readLen: " + i3);
                    if (i3 >= 42) {
                        byte[] bArr2 = new byte[ARP_LENGTH];
                        System.arraycopy(bArr, ETHERNET_LENGTH, bArr2, 0, ARP_LENGTH);
                        if (bArr2[0] == 0 && bArr2[1] == 1 && bArr2[2] == 8 && bArr2[3] == 0 && bArr2[IPV4_LENGTH] == MAC_ADDR_LENGTH && bArr2[5] == IPV4_LENGTH && bArr2[MAC_ADDR_LENGTH] == 0 && bArr2[7] == 2 && bArr2[ETHERNET_LENGTH] == address[0] && bArr2[15] == address[1] && bArr2[16] == address[2] && bArr2[17] == address[3]) {
                            byte[] bArr3 = new byte[MAC_ADDR_LENGTH];
                            System.arraycopy(bArr2, 8, bArr3, 0, MAC_ADDR_LENGTH);
                            Log.i(TAG, "target mac addr:" + HexDump.dumpHexString(bArr3));
                            return bArr3;
                        }
                    }
                    i2 = MAX_LENGTH;
                } catch (Exception e) {
                    Log.e(TAG, "ARP read failure: " + e);
                    return null;
                }
            }
            return null;
        } catch (Exception e2) {
            Log.e(TAG, "ARP send failure: " + e2);
            return null;
        }
    }

    public static boolean doArp(String str, InetAddress inetAddress, InetAddress inetAddress2, int i) {
        return doArp(str, inetAddress, inetAddress2, i, 2);
    }

    public static boolean doArp(String str, InetAddress inetAddress, InetAddress inetAddress2, int i, int i2) throws Throwable {
        ArpPeer arpPeer;
        int i3;
        ArpPeer arpPeer2 = null;
        try {
            try {
                arpPeer = new ArpPeer(str, inetAddress, inetAddress2);
                i3 = 0;
                for (int i4 = 0; i4 < i2; i4++) {
                    try {
                        if (arpPeer.doArp(i) != null) {
                            i3++;
                        }
                    } catch (ErrnoException | SocketException e) {
                        e = e;
                        arpPeer2 = arpPeer;
                        Log.e(TAG, "ARP test initiation failure: " + e);
                        if (arpPeer2 != null) {
                            arpPeer2.close();
                        }
                        return false;
                    } catch (Exception e2) {
                        e = e2;
                        arpPeer2 = arpPeer;
                        Log.e(TAG, "exception:" + e);
                        if (arpPeer2 != null) {
                            arpPeer2.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        arpPeer2 = arpPeer;
                        if (arpPeer2 != null) {
                            arpPeer2.close();
                        }
                        throw th;
                    }
                }
                Log.d(TAG, "ARP test result: " + i3);
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (ErrnoException | SocketException e3) {
            e = e3;
        } catch (Exception e4) {
            e = e4;
        }
        if (i3 == i2) {
            arpPeer.close();
            return true;
        }
        arpPeer.close();
        return false;
    }

    public void close() {
        Log.i(TAG, "Close arp");
        closeQuietly(this.mSocket);
        this.mSocket = null;
    }

    private static void closeQuietly(FileDescriptor fileDescriptor) {
        try {
            IoBridge.closeAndSignalBlockedThreads(fileDescriptor);
        } catch (IOException e) {
        }
    }

    protected void finalize() throws Throwable {
        try {
            try {
                if (this.mSocket != null) {
                    Log.e(TAG, "ArpPeer socket was finalized without closing");
                    close();
                }
            } catch (Exception e) {
                Log.e(TAG, "finalize() exception: " + e);
            }
        } finally {
            super.finalize();
        }
    }
}
