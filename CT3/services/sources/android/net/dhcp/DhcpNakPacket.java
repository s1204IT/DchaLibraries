package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

class DhcpNakPacket extends DhcpPacket {
    DhcpNakPacket(int transId, short secs, Inet4Address clientIp, Inet4Address yourIp, Inet4Address nextIp, Inet4Address relayIp, byte[] clientMac) {
        super(transId, secs, INADDR_ANY, INADDR_ANY, nextIp, relayIp, clientMac, false);
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " NAK, reason " + (this.mMessage == null ? "(none)" : this.mMessage);
    }

    @Override
    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(1500);
        Inet4Address destIp = this.mClientIp;
        Inet4Address srcIp = this.mYourIp;
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
}
