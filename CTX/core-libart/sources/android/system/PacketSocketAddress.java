package android.system;

import java.net.SocketAddress;

public final class PacketSocketAddress extends SocketAddress {
    public byte[] sll_addr;
    public short sll_hatype;
    public int sll_ifindex;
    public byte sll_pkttype;
    public short sll_protocol;

    public PacketSocketAddress(short s, int i, short s2, byte b, byte[] bArr) {
        this.sll_protocol = s;
        this.sll_ifindex = i;
        this.sll_hatype = s2;
        this.sll_pkttype = b;
        this.sll_addr = bArr;
    }

    public PacketSocketAddress(short s, int i) {
        this(s, i, (short) 0, (byte) 0, null);
    }

    public PacketSocketAddress(int i, byte[] bArr) {
        this((short) 0, i, (short) 0, (byte) 0, bArr);
    }
}
