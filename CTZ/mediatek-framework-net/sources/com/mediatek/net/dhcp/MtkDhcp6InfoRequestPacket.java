package com.mediatek.net.dhcp;

import java.nio.ByteBuffer;

class MtkDhcp6InfoRequestPacket extends MtkDhcp6Packet {
    MtkDhcp6InfoRequestPacket(byte[] bArr, byte[] bArr2) {
        super(bArr, INADDR_ANY, INADDR_ANY, INADDR_ANY, bArr2);
    }

    @Override
    public String toString() {
        String string = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        sb.append(" REQUEST, desired IP ");
        sb.append(this.mRequestedIp);
        sb.append(", param list length ");
        sb.append(this.mRequestedParams == null ? 0 : this.mRequestedParams.length);
        return sb.toString();
    }

    @Override
    public ByteBuffer buildPacket(short s, short s2) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1500);
        fillInPacket(INADDR_BROADCAST_ROUTER, INADDR_ANY, s, s2, byteBufferAllocate, (byte) 11);
        byteBufferAllocate.flip();
        return byteBufferAllocate;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (short) 1, getClientId());
        if (this.mServerIdentifier != null) {
            addTlv(byteBuffer, (short) 2, this.mServerIdentifier);
        }
        addCommonClientTlvs(byteBuffer);
        addTlv(byteBuffer, (short) 6, this.mRequestedParams);
    }
}
