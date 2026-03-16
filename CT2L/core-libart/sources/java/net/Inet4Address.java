package java.net;

import android.system.OsConstants;
import java.io.ObjectStreamException;
import java.nio.ByteOrder;
import libcore.io.Memory;

public final class Inet4Address extends InetAddress {
    private static final long serialVersionUID = 3286316764910316507L;
    public static final InetAddress ANY = new Inet4Address(new byte[]{0, 0, 0, 0}, null);
    public static final InetAddress ALL = new Inet4Address(new byte[]{-1, -1, -1, -1}, null);
    public static final InetAddress LOOPBACK = new Inet4Address(new byte[]{Byte.MAX_VALUE, 0, 0, 1}, "localhost");

    Inet4Address(byte[] ipaddress, String hostName) {
        super(OsConstants.AF_INET, ipaddress, hostName);
    }

    @Override
    public boolean isAnyLocalAddress() {
        return this.ipaddress[0] == 0 && this.ipaddress[1] == 0 && this.ipaddress[2] == 0 && this.ipaddress[3] == 0;
    }

    @Override
    public boolean isLinkLocalAddress() {
        return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 169 && (this.ipaddress[1] & Character.DIRECTIONALITY_UNDEFINED) == 254;
    }

    @Override
    public boolean isLoopbackAddress() {
        return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 127;
    }

    @Override
    public boolean isMCGlobal() {
        if (!isMulticastAddress()) {
            return false;
        }
        int address = Memory.peekInt(this.ipaddress, 0, ByteOrder.BIG_ENDIAN);
        return (address >>> 8) >= 14680065 && (address >>> 24) <= 238;
    }

    @Override
    public boolean isMCLinkLocal() {
        return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 224 && this.ipaddress[1] == 0 && this.ipaddress[2] == 0;
    }

    @Override
    public boolean isMCNodeLocal() {
        return false;
    }

    @Override
    public boolean isMCOrgLocal() {
        return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 239 && (this.ipaddress[1] & 252) == 192;
    }

    @Override
    public boolean isMCSiteLocal() {
        return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 239 && (this.ipaddress[1] & Character.DIRECTIONALITY_UNDEFINED) == 255;
    }

    @Override
    public boolean isMulticastAddress() {
        return (this.ipaddress[0] & 240) == 224;
    }

    @Override
    public boolean isSiteLocalAddress() {
        if ((this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 10) {
            return true;
        }
        if ((this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 172 && (this.ipaddress[1] & 240) == 16) {
            return true;
        }
        return (this.ipaddress[0] & Character.DIRECTIONALITY_UNDEFINED) == 192 && (this.ipaddress[1] & Character.DIRECTIONALITY_UNDEFINED) == 168;
    }

    private Object writeReplace() throws ObjectStreamException {
        return new Inet4Address(this.ipaddress, this.hostName);
    }
}
