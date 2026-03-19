package java.net;

import android.system.OsConstants;
import java.io.ObjectStreamException;

public final class Inet4Address extends InetAddress {
    static final int INADDRSZ = 4;
    private static final long serialVersionUID = 3286316764910316507L;
    public static final InetAddress ANY = new Inet4Address((String) null, new byte[]{0, 0, 0, 0});
    public static final InetAddress ALL = new Inet4Address((String) null, new byte[]{-1, -1, -1, -1});
    public static final InetAddress LOOPBACK = new Inet4Address("localhost", new byte[]{Byte.MAX_VALUE, 0, 0, 1});

    Inet4Address() {
        holder().hostName = null;
        holder().address = 0;
        holder().family = OsConstants.AF_INET;
    }

    Inet4Address(String str, byte[] bArr) {
        holder().hostName = str;
        holder().family = OsConstants.AF_INET;
        if (bArr != null && bArr.length == 4) {
            holder().address = ((bArr[0] << 24) & (-16777216)) | (bArr[3] & Character.DIRECTIONALITY_UNDEFINED) | ((bArr[2] << 8) & 65280) | ((bArr[1] << 16) & 16711680);
        }
        holder().originalHostName = str;
    }

    Inet4Address(String str, int i) {
        holder().hostName = str;
        holder().family = OsConstants.AF_INET;
        holder().address = i;
        holder().originalHostName = str;
    }

    private Object writeReplace() throws ObjectStreamException {
        InetAddress inetAddress = new InetAddress();
        inetAddress.holder().hostName = holder().getHostName();
        inetAddress.holder().address = holder().getAddress();
        inetAddress.holder().family = 2;
        return inetAddress;
    }

    @Override
    public boolean isMulticastAddress() {
        return (holder().getAddress() & (-268435456)) == -536870912;
    }

    @Override
    public boolean isAnyLocalAddress() {
        return holder().getAddress() == 0;
    }

    @Override
    public boolean isLoopbackAddress() {
        return getAddress()[0] == 127;
    }

    @Override
    public boolean isLinkLocalAddress() {
        int address = holder().getAddress();
        return ((address >>> 24) & 255) == 169 && ((address >>> 16) & 255) == 254;
    }

    @Override
    public boolean isSiteLocalAddress() {
        int address = holder().getAddress();
        int i = (address >>> 24) & 255;
        return i == 10 || (i == 172 && ((address >>> 16) & 240) == 16) || (i == 192 && ((address >>> 16) & 255) == 168);
    }

    @Override
    public boolean isMCGlobal() {
        byte[] address = getAddress();
        if ((address[0] & Character.DIRECTIONALITY_UNDEFINED) < 224 || (address[0] & Character.DIRECTIONALITY_UNDEFINED) > 238) {
            return false;
        }
        return ((address[0] & Character.DIRECTIONALITY_UNDEFINED) == 224 && address[1] == 0 && address[2] == 0) ? false : true;
    }

    @Override
    public boolean isMCNodeLocal() {
        return false;
    }

    @Override
    public boolean isMCLinkLocal() {
        int address = holder().getAddress();
        return ((address >>> 24) & 255) == 224 && ((address >>> 16) & 255) == 0 && ((address >>> 8) & 255) == 0;
    }

    @Override
    public boolean isMCSiteLocal() {
        int address = holder().getAddress();
        return ((address >>> 24) & 255) == 239 && ((address >>> 16) & 255) == 255;
    }

    @Override
    public boolean isMCOrgLocal() {
        int i;
        int address = holder().getAddress();
        return ((address >>> 24) & 255) == 239 && (i = (address >>> 16) & 255) >= 192 && i <= 195;
    }

    @Override
    public byte[] getAddress() {
        int address = holder().getAddress();
        return new byte[]{(byte) ((address >>> 24) & 255), (byte) ((address >>> 16) & 255), (byte) ((address >>> 8) & 255), (byte) (address & 255)};
    }

    @Override
    public String getHostAddress() {
        return numericToTextFormat(getAddress());
    }

    @Override
    public int hashCode() {
        return holder().getAddress();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (obj instanceof Inet4Address) && ((InetAddress) obj).holder().getAddress() == holder().getAddress();
    }

    static String numericToTextFormat(byte[] bArr) {
        return (bArr[0] & Character.DIRECTIONALITY_UNDEFINED) + "." + (bArr[1] & Character.DIRECTIONALITY_UNDEFINED) + "." + (bArr[2] & Character.DIRECTIONALITY_UNDEFINED) + "." + (bArr[3] & Character.DIRECTIONALITY_UNDEFINED);
    }
}
