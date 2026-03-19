package com.mediatek.net.dhcp;

import java.net.Inet6Address;
import java.nio.ByteBuffer;

class MtkDhcp6NakPacket extends MtkDhcp6Packet {
    MtkDhcp6NakPacket(byte[] bArr, Inet6Address inet6Address, Inet6Address inet6Address2) {
        super(bArr, inet6Address2, inet6Address, INADDR_ANY, null);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public ByteBuffer buildPacket(short s, short s2) {
        return null;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
    }
}
