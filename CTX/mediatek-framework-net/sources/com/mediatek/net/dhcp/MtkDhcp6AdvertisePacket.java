package com.mediatek.net.dhcp;

import java.net.Inet6Address;
import java.nio.ByteBuffer;
import java.util.Iterator;

class MtkDhcp6AdvertisePacket extends MtkDhcp6Packet {
    private final Inet6Address mSrcIp;

    MtkDhcp6AdvertisePacket(byte[] bArr, Inet6Address inet6Address, Inet6Address inet6Address2, byte[] bArr2) {
        super(bArr, inet6Address2, inet6Address, INADDR_ANY, bArr2);
        this.mSrcIp = inet6Address;
    }

    @Override
    public String toString() {
        String string = super.toString();
        String str = " DNS servers: ";
        Iterator<Inet6Address> it = this.mDnsServers.iterator();
        while (it.hasNext()) {
            str = str + it.next().toString() + " ";
        }
        return string + " ADVERTISE: your new IP " + this.mRequestedIp + ", netmask " + this.mSubnetMask + ", gateway " + this.mGateway + str + ", lease time " + this.mLeaseTime + ", status code " + this.mStatusCode;
    }

    @Override
    public ByteBuffer buildPacket(short s, short s2) {
        return null;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
    }

    private static final int getInt(Integer num) {
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }
}
