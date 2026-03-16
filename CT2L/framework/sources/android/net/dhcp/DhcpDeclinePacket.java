package android.net.dhcp;

import java.net.InetAddress;
import java.nio.ByteBuffer;

class DhcpDeclinePacket extends DhcpPacket {
    DhcpDeclinePacket(int transId, InetAddress clientIp, InetAddress yourIp, InetAddress nextIp, InetAddress relayIp, byte[] clientMac) {
        super(transId, clientIp, yourIp, nextIp, relayIp, clientMac, false);
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " DECLINE";
    }

    @Override
    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(1500);
        fillInPacket(encap, this.mClientIp, this.mYourIp, destUdp, srcUdp, result, (byte) 1, false);
        result.flip();
        return result;
    }

    @Override
    void finishPacket(ByteBuffer buffer) {
    }

    @Override
    public void doNextOp(DhcpStateMachine machine) {
        machine.onDeclineReceived(this.mClientMac, this.mRequestedIp);
    }
}
