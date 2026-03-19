package com.mediatek.net.dhcp;

import java.nio.ByteBuffer;

class MtkDhcp6SolicitPacket extends MtkDhcp6Packet {
    MtkDhcp6SolicitPacket(byte[] bArr, byte[] bArr2) {
        super(bArr, INADDR_ANY, INADDR_ANY, INADDR_ANY, bArr2);
    }

    @Override
    public String toString() {
        return super.toString() + " SOLICIT broadcast";
    }

    @Override
    public ByteBuffer buildPacket(short s, short s2) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1500);
        fillInPacket(INADDR_BROADCAST_ROUTER, INADDR_ANY, s, s2, byteBufferAllocate, (byte) 1);
        byteBufferAllocate.flip();
        return byteBufferAllocate;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (short) 1, getClientId());
        addCommonClientTlvs(byteBuffer);
        addTlv(byteBuffer, (short) 6, this.mRequestedParams);
    }
}
