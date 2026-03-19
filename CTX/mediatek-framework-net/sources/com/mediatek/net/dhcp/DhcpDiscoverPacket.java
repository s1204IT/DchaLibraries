package com.mediatek.net.dhcp;

import java.nio.ByteBuffer;

class DhcpDiscoverPacket extends DhcpPacket {
    DhcpDiscoverPacket(int i, short s, byte[] bArr, boolean z) {
        super(i, s, INADDR_ANY, INADDR_ANY, INADDR_ANY, INADDR_ANY, bArr, z);
    }

    @Override
    public String toString() {
        String string = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        sb.append(" DISCOVER ");
        sb.append(this.mBroadcast ? "broadcast " : "unicast ");
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
        addTlv(byteBuffer, (byte) 53, (byte) 1);
        addTlv(byteBuffer, (byte) 61, getClientId());
        addCommonClientTlvs(byteBuffer);
        addTlv(byteBuffer, (byte) 55, this.mRequestedParams);
        addTlvEnd(byteBuffer);
    }
}
