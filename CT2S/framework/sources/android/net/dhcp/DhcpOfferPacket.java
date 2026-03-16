package android.net.dhcp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class DhcpOfferPacket extends DhcpPacket {
    private final InetAddress mSrcIp;

    DhcpOfferPacket(int transId, boolean broadcast, InetAddress serverAddress, InetAddress clientIp, byte[] clientMac) {
        super(transId, Inet4Address.ANY, clientIp, Inet4Address.ANY, Inet4Address.ANY, clientMac, broadcast);
        this.mSrcIp = serverAddress;
    }

    @Override
    public String toString() {
        String s = super.toString();
        String dnsServers = ", DNS servers: ";
        if (this.mDnsServers != null) {
            for (InetAddress dnsServer : this.mDnsServers) {
                dnsServers = dnsServers + dnsServer + " ";
            }
        }
        return s + " OFFER, ip " + this.mYourIp + ", mask " + this.mSubnetMask + dnsServers + ", gateway " + this.mGateway + " lease time " + this.mLeaseTime + ", domain " + this.mDomainName;
    }

    @Override
    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(1500);
        InetAddress destIp = this.mBroadcast ? Inet4Address.ALL : this.mYourIp;
        InetAddress srcIp = this.mBroadcast ? Inet4Address.ANY : this.mSrcIp;
        fillInPacket(encap, destIp, srcIp, destUdp, srcUdp, result, (byte) 2, this.mBroadcast);
        result.flip();
        return result;
    }

    @Override
    void finishPacket(ByteBuffer buffer) {
        addTlv(buffer, (byte) 53, (byte) 2);
        addTlv(buffer, (byte) 54, this.mServerIdentifier);
        addTlv(buffer, (byte) 51, this.mLeaseTime);
        if (this.mLeaseTime != null) {
            addTlv(buffer, (byte) 58, Integer.valueOf(this.mLeaseTime.intValue() / 2));
        }
        addTlv(buffer, (byte) 1, this.mSubnetMask);
        addTlv(buffer, (byte) 3, this.mGateway);
        addTlv(buffer, (byte) 15, this.mDomainName);
        addTlv(buffer, (byte) 28, this.mBroadcastAddress);
        addTlv(buffer, (byte) 6, this.mDnsServers);
        addTlvEnd(buffer);
    }

    @Override
    public void doNextOp(DhcpStateMachine machine) {
        machine.onOfferReceived(this.mBroadcast, this.mTransId, this.mClientMac, this.mYourIp, this.mServerIdentifier);
    }
}
