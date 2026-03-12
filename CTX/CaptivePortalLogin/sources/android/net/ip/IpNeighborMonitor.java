package android.net.ip;

import android.net.MacAddress;
import android.net.ip.IpNeighborMonitor;
import android.net.netlink.NetlinkConstants;
import android.net.netlink.NetlinkErrorMessage;
import android.net.netlink.NetlinkMessage;
import android.net.netlink.NetlinkSocket;
import android.net.netlink.RtNetlinkNeighborMessage;
import android.net.netlink.StructNdMsg;
import android.net.util.PacketReader;
import android.net.util.SharedLog;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.NetlinkSocketAddress;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import com.android.internal.util.BitUtils;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringJoiner;
import libcore.io.IoUtils;

public class IpNeighborMonitor extends PacketReader {
    private final NeighborEventConsumer mConsumer;
    private final SharedLog mLog;
    private static final String TAG = IpNeighborMonitor.class.getSimpleName();
    private static final boolean DBG = !Build.IS_USER;

    public interface NeighborEventConsumer {
        void accept(NeighborEvent neighborEvent);
    }

    public static int startKernelNeighborProbe(int i, InetAddress inetAddress) {
        String str = "probing ip=" + inetAddress.getHostAddress() + "%" + i;
        if (DBG) {
            Log.d(TAG, str);
        }
        try {
            NetlinkSocket.sendOneShotKernelMessage(OsConstants.NETLINK_ROUTE, RtNetlinkNeighborMessage.newNewNeighborMessage(1, inetAddress, (short) 16, i, null));
            return 0;
        } catch (ErrnoException e) {
            Log.e(TAG, "Error " + str + ": " + e);
            return -e.errno;
        }
    }

    public static class NeighborEvent {
        final long elapsedMs;
        final int ifindex;
        final InetAddress ip;
        final MacAddress macAddr;
        final short msgType;
        final short nudState;

        public NeighborEvent(long j, short s, int i, InetAddress inetAddress, short s2, MacAddress macAddress) {
            this.elapsedMs = j;
            this.msgType = s;
            this.ifindex = i;
            this.ip = inetAddress;
            this.nudState = s2;
            this.macAddr = macAddress;
        }

        public String toString() {
            return new StringJoiner(",", "NeighborEvent{", "}").add("@" + this.elapsedMs).add(NetlinkConstants.stringForNlMsgType(this.msgType)).add("if=" + this.ifindex).add(this.ip.getHostAddress()).add(StructNdMsg.stringForNudState(this.nudState)).add("[" + this.macAddr + "]").toString();
        }
    }

    public IpNeighborMonitor(Handler handler, SharedLog sharedLog, NeighborEventConsumer neighborEventConsumer) {
        super(handler, 8192);
        this.mLog = sharedLog.forSubComponent(TAG);
        this.mConsumer = neighborEventConsumer == null ? new NeighborEventConsumer() {
            @Override
            public final void accept(IpNeighborMonitor.NeighborEvent neighborEvent) {
                IpNeighborMonitor.lambda$new$0(neighborEvent);
            }
        } : neighborEventConsumer;
    }

    static void lambda$new$0(NeighborEvent neighborEvent) {
    }

    @Override
    protected FileDescriptor createFd() {
        FileDescriptor fileDescriptorForProto;
        try {
            fileDescriptorForProto = NetlinkSocket.forProto(OsConstants.NETLINK_ROUTE);
            try {
                Os.bind(fileDescriptorForProto, new NetlinkSocketAddress(0, OsConstants.RTMGRP_NEIGH));
                Os.connect(fileDescriptorForProto, new NetlinkSocketAddress(0, 0));
                return fileDescriptorForProto;
            } catch (ErrnoException | SocketException e) {
                e = e;
                logError("Failed to create rtnetlink socket", e);
                IoUtils.closeQuietly(fileDescriptorForProto);
                return null;
            }
        } catch (ErrnoException | SocketException e2) {
            e = e2;
            fileDescriptorForProto = null;
        }
    }

    @Override
    protected void handlePacket(byte[] bArr, int i) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr, 0, i);
        byteBufferWrap.order(ByteOrder.nativeOrder());
        parseNetlinkMessageBuffer(byteBufferWrap, jElapsedRealtime);
    }

    private void parseNetlinkMessageBuffer(ByteBuffer byteBuffer, long j) {
        while (byteBuffer.remaining() > 0) {
            int iPosition = byteBuffer.position();
            NetlinkMessage netlinkMessage = NetlinkMessage.parse(byteBuffer);
            if (netlinkMessage == null || netlinkMessage.getHeader() == null) {
                byteBuffer.position(iPosition);
                this.mLog.e("unparsable netlink msg: " + NetlinkConstants.hexify(byteBuffer));
                return;
            }
            int i = netlinkMessage.getHeader().nlmsg_pid;
            if (i != 0) {
                this.mLog.e("non-kernel source portId: " + BitUtils.uint32(i));
                return;
            }
            if (netlinkMessage instanceof NetlinkErrorMessage) {
                this.mLog.e("netlink error: " + netlinkMessage);
            } else if (!(netlinkMessage instanceof RtNetlinkNeighborMessage)) {
                this.mLog.i("non-rtnetlink neighbor msg: " + netlinkMessage);
            } else {
                evaluateRtNetlinkNeighborMessage((RtNetlinkNeighborMessage) netlinkMessage, j);
            }
        }
    }

    private void evaluateRtNetlinkNeighborMessage(RtNetlinkNeighborMessage rtNetlinkNeighborMessage, long j) {
        short s;
        short s2 = rtNetlinkNeighborMessage.getHeader().nlmsg_type;
        StructNdMsg ndHeader = rtNetlinkNeighborMessage.getNdHeader();
        if (ndHeader == null) {
            this.mLog.e("RtNetlinkNeighborMessage without ND message header!");
            return;
        }
        int i = ndHeader.ndm_ifindex;
        InetAddress destination = rtNetlinkNeighborMessage.getDestination();
        if (s2 == 29) {
            s = 0;
        } else {
            s = ndHeader.ndm_state;
        }
        NeighborEvent neighborEvent = new NeighborEvent(j, s2, i, destination, s, getMacAddress(rtNetlinkNeighborMessage.getLinkLayerAddress()));
        if (DBG) {
            Log.d(TAG, neighborEvent.toString());
        }
        this.mConsumer.accept(neighborEvent);
    }

    private static MacAddress getMacAddress(byte[] bArr) {
        if (bArr != null) {
            try {
                return MacAddress.fromBytes(bArr);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to parse link-layer address: " + NetlinkConstants.hexify(bArr));
                return null;
            }
        }
        return null;
    }
}
