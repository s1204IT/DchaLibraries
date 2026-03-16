package android.net.dhcp;

import java.net.InetAddress;
import java.nio.ByteBuffer;

class DhcpInformPacket extends DhcpPacket {
    DhcpInformPacket(int transId, InetAddress clientIp, InetAddress yourIp, InetAddress nextIp, InetAddress relayIp, byte[] clientMac) {
        super(transId, clientIp, yourIp, nextIp, relayIp, clientMac, false);
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " INFORM";
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
        byte[] clientId = new byte[7];
        clientId[0] = 1;
        System.arraycopy(this.mClientMac, 0, clientId, 1, 6);
        addTlv(buffer, (byte) 53, (byte) 3);
        addTlv(buffer, (byte) 55, this.mRequestedParams);
        addTlvEnd(buffer);
    }

    @Override
    public void doNextOp(DhcpStateMachine machine) {
        InetAddress clientRequest = this.mRequestedIp == null ? this.mClientIp : this.mRequestedIp;
        machine.onInformReceived(this.mTransId, this.mClientMac, clientRequest, this.mRequestedParams);
    }
}
