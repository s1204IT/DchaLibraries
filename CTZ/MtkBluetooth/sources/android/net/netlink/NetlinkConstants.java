package android.net.netlink;

import android.system.OsConstants;
import com.android.internal.util.HexDump;
import java.nio.ByteBuffer;

public class NetlinkConstants {
    public static final int NLA_ALIGNTO = 4;
    public static final short NLMSG_DONE = 3;
    public static final short NLMSG_ERROR = 2;
    public static final short NLMSG_MAX_RESERVED = 15;
    public static final short NLMSG_NOOP = 1;
    public static final short NLMSG_OVERRUN = 4;
    public static final short RTM_DELADDR = 21;
    public static final short RTM_DELLINK = 17;
    public static final short RTM_DELNEIGH = 29;
    public static final short RTM_DELROUTE = 25;
    public static final short RTM_DELRULE = 33;
    public static final short RTM_GETADDR = 22;
    public static final short RTM_GETLINK = 18;
    public static final short RTM_GETNEIGH = 30;
    public static final short RTM_GETROUTE = 26;
    public static final short RTM_GETRULE = 34;
    public static final short RTM_NEWADDR = 20;
    public static final short RTM_NEWLINK = 16;
    public static final short RTM_NEWNDUSEROPT = 68;
    public static final short RTM_NEWNEIGH = 28;
    public static final short RTM_NEWROUTE = 24;
    public static final short RTM_NEWRULE = 32;
    public static final short RTM_SETLINK = 19;

    private NetlinkConstants() {
    }

    public static final int alignedLengthOf(short s) {
        return alignedLengthOf(s & 65535);
    }

    public static final int alignedLengthOf(int i) {
        if (i <= 0) {
            return 0;
        }
        return (((i + 4) - 1) / 4) * 4;
    }

    public static String stringForAddressFamily(int i) {
        return i == OsConstants.AF_INET ? "AF_INET" : i == OsConstants.AF_INET6 ? "AF_INET6" : i == OsConstants.AF_NETLINK ? "AF_NETLINK" : String.valueOf(i);
    }

    public static String hexify(byte[] bArr) {
        return bArr == null ? "(null)" : HexDump.toHexString(bArr);
    }

    public static String hexify(ByteBuffer byteBuffer) {
        return byteBuffer == null ? "(null)" : HexDump.toHexString(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
    }

    public static String stringForNlMsgType(short s) {
        if (s != 68) {
            switch (s) {
                case 1:
                    return "NLMSG_NOOP";
                case 2:
                    return "NLMSG_ERROR";
                case 3:
                    return "NLMSG_DONE";
                case 4:
                    return "NLMSG_OVERRUN";
                default:
                    switch (s) {
                        case 16:
                            return "RTM_NEWLINK";
                        case 17:
                            return "RTM_DELLINK";
                        case 18:
                            return "RTM_GETLINK";
                        case 19:
                            return "RTM_SETLINK";
                        case 20:
                            return "RTM_NEWADDR";
                        case 21:
                            return "RTM_DELADDR";
                        case 22:
                            return "RTM_GETADDR";
                        default:
                            switch (s) {
                                case 24:
                                    return "RTM_NEWROUTE";
                                case 25:
                                    return "RTM_DELROUTE";
                                case 26:
                                    return "RTM_GETROUTE";
                                default:
                                    switch (s) {
                                        case 28:
                                            return "RTM_NEWNEIGH";
                                        case 29:
                                            return "RTM_DELNEIGH";
                                        case 30:
                                            return "RTM_GETNEIGH";
                                        default:
                                            switch (s) {
                                                case 32:
                                                    return "RTM_NEWRULE";
                                                case 33:
                                                    return "RTM_DELRULE";
                                                case 34:
                                                    return "RTM_GETRULE";
                                                default:
                                                    return "unknown RTM type: " + String.valueOf((int) s);
                                            }
                                    }
                            }
                    }
            }
        }
        return "RTM_NEWNDUSEROPT";
    }
}
