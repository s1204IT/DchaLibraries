package android.net.dhcp;

import android.net.util.NetworkConstants;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

class DhcpNakPacket extends DhcpPacket {
    DhcpNakPacket(int i, short s, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4, byte[] bArr) {
        super(i, s, INADDR_ANY, INADDR_ANY, inet4Address3, inet4Address4, bArr, false);
    }

    @Override
    public String toString() {
        String string = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        sb.append(" NAK, reason ");
        sb.append(this.mMessage == null ? "(none)" : this.mMessage);
        return sb.toString();
    }

    @Override
    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        fillInPacket(i, this.mClientIp, this.mYourIp, s, s2, byteBufferAllocate, (byte) 2, this.mBroadcast);
        byteBufferAllocate.flip();
        return byteBufferAllocate;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (byte) 53, (byte) 6);
        addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        addTlv(byteBuffer, (byte) 56, this.mMessage);
        addTlvEnd(byteBuffer);
    }
}
