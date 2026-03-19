package android.net.dhcp;

import android.net.util.NetworkConstants;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Iterator;

class DhcpOfferPacket extends DhcpPacket {
    private final Inet4Address mSrcIp;

    DhcpOfferPacket(int i, short s, boolean z, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, byte[] bArr) {
        super(i, s, inet4Address2, inet4Address3, INADDR_ANY, INADDR_ANY, bArr, z);
        this.mSrcIp = inet4Address;
    }

    @Override
    public String toString() {
        String string = super.toString();
        String str = ", DNS servers: ";
        if (this.mDnsServers != null) {
            Iterator<Inet4Address> it = this.mDnsServers.iterator();
            while (it.hasNext()) {
                str = str + it.next() + " ";
            }
        }
        return string + " OFFER, ip " + this.mYourIp + ", mask " + this.mSubnetMask + str + ", gateways " + this.mGateways + " lease time " + this.mLeaseTime + ", domain " + this.mDomainName;
    }

    @Override
    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(NetworkConstants.ETHER_MTU);
        fillInPacket(i, this.mBroadcast ? INADDR_BROADCAST : this.mYourIp, this.mBroadcast ? INADDR_ANY : this.mSrcIp, s, s2, byteBufferAllocate, (byte) 2, this.mBroadcast);
        byteBufferAllocate.flip();
        return byteBufferAllocate;
    }

    @Override
    void finishPacket(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (byte) 53, (byte) 2);
        addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        addTlv(byteBuffer, (byte) 51, this.mLeaseTime);
        if (this.mLeaseTime != null) {
            addTlv(byteBuffer, (byte) 58, Integer.valueOf(this.mLeaseTime.intValue() / 2));
        }
        addTlv(byteBuffer, (byte) 1, this.mSubnetMask);
        addTlv(byteBuffer, (byte) 3, this.mGateways);
        addTlv(byteBuffer, UsbDescriptor.DESCRIPTORTYPE_BOS, this.mDomainName);
        addTlv(byteBuffer, (byte) 28, this.mBroadcastAddress);
        addTlv(byteBuffer, (byte) 6, this.mDnsServers);
        addTlvEnd(byteBuffer);
    }
}
