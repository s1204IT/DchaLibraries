package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

class DhcpRequestPacket extends DhcpPacket {
    DhcpRequestPacket(int i, short s, Inet4Address inet4Address, byte[] bArr, boolean z) {
        super(i, s, inet4Address, INADDR_ANY, INADDR_ANY, INADDR_ANY, bArr, z);
    }

    @Override
    public String toString() {
        String string = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        sb.append(" REQUEST, desired IP ");
        sb.append(this.mRequestedIp);
        sb.append(" from host '");
        sb.append(this.mHostName);
        sb.append("', param list length ");
        sb.append(this.mRequestedParams == null ? 0 : this.mRequestedParams.length);
        return sb.toString();
    }

    @Override
    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1500);
        fillInPacket(i, INADDR_BROADCAST, INADDR_ANY, s, s2, byteBufferAllocate, (byte) 1, this.mBroadcast);
        byteBufferAllocate.flip();
        return byteBufferAllocate;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (byte) 53, (byte) 3);
        addTlv(byteBuffer, (byte) 61, getClientId());
        if (!INADDR_ANY.equals(this.mRequestedIp)) {
            addTlv(byteBuffer, (byte) 50, this.mRequestedIp);
        }
        if (!INADDR_ANY.equals(this.mServerIdentifier)) {
            addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        }
        addCommonClientTlvs(byteBuffer);
        addTlv(byteBuffer, (byte) 55, this.mRequestedParams);
        addTlvEnd(byteBuffer);
    }
}
