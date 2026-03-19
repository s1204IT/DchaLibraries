package com.mediatek.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Iterator;

class DhcpAckPacket extends DhcpPacket {
    private final Inet4Address mSrcIp;

    DhcpAckPacket(int i, short s, boolean z, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, byte[] bArr) {
        super(i, s, inet4Address2, inet4Address3, inet4Address, INADDR_ANY, bArr, z);
        this.mBroadcast = z;
        this.mSrcIp = inet4Address;
    }

    @Override
    public String toString() {
        String string = super.toString();
        String str = " DNS servers: ";
        Iterator<Inet4Address> it = this.mDnsServers.iterator();
        while (it.hasNext()) {
            str = str + it.next().toString() + " ";
        }
        return string + " ACK: your new IP " + this.mYourIp + ", netmask " + this.mSubnetMask + ", gateways " + this.mGateways + str + ", lease time " + this.mLeaseTime;
    }

    @Override
    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(1500);
        fillInPacket(i, this.mBroadcast ? INADDR_BROADCAST : this.mYourIp, this.mBroadcast ? INADDR_ANY : this.mSrcIp, s, s2, byteBufferAllocate, (byte) 2, this.mBroadcast);
        byteBufferAllocate.flip();
        return byteBufferAllocate;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (byte) 53, (byte) 5);
        addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        addTlv(byteBuffer, (byte) 51, this.mLeaseTime);
        if (this.mLeaseTime != null) {
            addTlv(byteBuffer, (byte) 58, Integer.valueOf(this.mLeaseTime.intValue() / 2));
        }
        addTlv(byteBuffer, (byte) 1, this.mSubnetMask);
        addTlv(byteBuffer, (byte) 3, this.mGateways);
        addTlv(byteBuffer, (byte) 15, this.mDomainName);
        addTlv(byteBuffer, (byte) 28, this.mBroadcastAddress);
        addTlv(byteBuffer, (byte) 6, this.mDnsServers);
        addTlvEnd(byteBuffer);
    }

    private static final int getInt(Integer num) {
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }
}
