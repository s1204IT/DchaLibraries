package android.net.dhcp;

import java.nio.ByteBuffer;

class Dhcp6SolicitPacket extends Dhcp6Packet {
    Dhcp6SolicitPacket(byte[] transId, byte[] clientMac) {
        super(transId, INADDR_ANY, INADDR_ANY, INADDR_ANY, clientMac);
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " SOLICIT broadcast";
    }

    @Override
    public ByteBuffer buildPacket(short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(1500);
        fillInPacket(INADDR_BROADCAST_ROUTER, INADDR_ANY, destUdp, srcUdp, result, (byte) 1);
        result.flip();
        return result;
    }

    @Override
    void finishPacket(ByteBuffer buffer) {
        addTlv(buffer, (short) 1, getClientId());
        addCommonClientTlvs(buffer);
        addTlv(buffer, (short) 6, this.mRequestedParams);
    }
}
