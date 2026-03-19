package android.system;

public final class StructIcmpHdr {
    private byte[] packet = new byte[8];

    private StructIcmpHdr() {
    }

    public static StructIcmpHdr IcmpEchoHdr(boolean z, int i) {
        StructIcmpHdr structIcmpHdr = new StructIcmpHdr();
        structIcmpHdr.packet[0] = (byte) (z ? OsConstants.ICMP_ECHO : OsConstants.ICMP6_ECHO_REQUEST);
        structIcmpHdr.packet[6] = (byte) (i >> 8);
        structIcmpHdr.packet[7] = (byte) i;
        return structIcmpHdr;
    }

    public byte[] getBytes() {
        return (byte[]) this.packet.clone();
    }
}
