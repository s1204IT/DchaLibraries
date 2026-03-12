package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

class DhcpInformPacket extends DhcpPacket {
    DhcpInformPacket(int i, short s, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4, byte[] bArr) {
        super(i, s, inet4Address, inet4Address2, inet4Address3, inet4Address4, bArr, false);
    }

    @Override
    public String toString() {
        return super.toString() + " INFORM";
    }

    @Override
    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1500);
        fillInPacket(i, this.mClientIp, this.mYourIp, s, s2, byteBufferAllocate, (byte) 1, false);
        byteBufferAllocate.flip();
        return byteBufferAllocate;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (byte) 53, (byte) 8);
        addTlv(byteBuffer, (byte) 61, getClientId());
        addCommonClientTlvs(byteBuffer);
        addTlv(byteBuffer, (byte) 55, this.mRequestedParams);
        addTlvEnd(byteBuffer);
    }
}
