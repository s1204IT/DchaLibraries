package android.net.dhcp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class DhcpNakPacket extends DhcpPacket {
    DhcpNakPacket(int transId, InetAddress clientIp, InetAddress yourIp, InetAddress nextIp, InetAddress relayIp, byte[] clientMac) {
        super(transId, Inet4Address.ANY, Inet4Address.ANY, nextIp, relayIp, clientMac, false);
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " NAK, reason " + (this.mMessage == null ? "(none)" : this.mMessage);
    }

    @Override
    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(1500);
        InetAddress destIp = this.mClientIp;
        InetAddress srcIp = this.mYourIp;
        fillInPacket(encap, destIp, srcIp, destUdp, srcUdp, result, (byte) 2, this.mBroadcast);
        result.flip();
        return result;
    }

    @Override
    void finishPacket(ByteBuffer buffer) {
        addTlv(buffer, (byte) 53, (byte) 6);
        addTlv(buffer, (byte) 54, this.mServerIdentifier);
        addTlv(buffer, (byte) 56, this.mMessage);
        addTlvEnd(buffer);
    }

    @Override
    public void doNextOp(DhcpStateMachine machine) {
        machine.onNakReceived();
    }
}
