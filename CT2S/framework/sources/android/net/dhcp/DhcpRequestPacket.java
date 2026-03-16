package android.net.dhcp;

import android.util.Log;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

class DhcpRequestPacket extends DhcpPacket {
    DhcpRequestPacket(int transId, InetAddress clientIp, byte[] clientMac, boolean broadcast) {
        super(transId, clientIp, Inet4Address.ANY, Inet4Address.ANY, Inet4Address.ANY, clientMac, broadcast);
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + " REQUEST, desired IP " + this.mRequestedIp + " from host '" + this.mHostName + "', param list length " + (this.mRequestedParams == null ? 0 : this.mRequestedParams.length);
    }

    @Override
    public ByteBuffer buildPacket(int encap, short destUdp, short srcUdp) {
        ByteBuffer result = ByteBuffer.allocate(1500);
        fillInPacket(encap, Inet4Address.ALL, Inet4Address.ANY, destUdp, srcUdp, result, (byte) 1, this.mBroadcast);
        result.flip();
        return result;
    }

    @Override
    void finishPacket(ByteBuffer buffer) {
        byte[] clientId = new byte[7];
        clientId[0] = 1;
        System.arraycopy(this.mClientMac, 0, clientId, 1, 6);
        addTlv(buffer, (byte) 53, (byte) 3);
        addTlv(buffer, (byte) 55, this.mRequestedParams);
        addTlv(buffer, (byte) 50, this.mRequestedIp);
        addTlv(buffer, (byte) 54, this.mServerIdentifier);
        addTlv(buffer, (byte) 61, clientId);
        addTlvEnd(buffer);
    }

    @Override
    public void doNextOp(DhcpStateMachine machine) {
        InetAddress clientRequest = this.mRequestedIp == null ? this.mClientIp : this.mRequestedIp;
        Log.v("DhcpPacket", "requested IP is " + this.mRequestedIp + " and client IP is " + this.mClientIp);
        machine.onRequestReceived(this.mBroadcast, this.mTransId, this.mClientMac, clientRequest, this.mRequestedParams, this.mHostName);
    }
}
