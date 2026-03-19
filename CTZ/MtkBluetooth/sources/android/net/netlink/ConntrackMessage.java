package android.net.netlink;

import android.system.OsConstants;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConntrackMessage extends NetlinkMessage {
    public static final short CTA_IP_V4_DST = 2;
    public static final short CTA_IP_V4_SRC = 1;
    public static final short CTA_PROTO_DST_PORT = 3;
    public static final short CTA_PROTO_NUM = 1;
    public static final short CTA_PROTO_SRC_PORT = 2;
    public static final short CTA_TIMEOUT = 7;
    public static final short CTA_TUPLE_IP = 1;
    public static final short CTA_TUPLE_ORIG = 1;
    public static final short CTA_TUPLE_PROTO = 2;
    public static final short CTA_TUPLE_REPLY = 2;
    public static final short IPCTNL_MSG_CT_NEW = 0;
    public static final short NFNL_SUBSYS_CTNETLINK = 1;
    public static final int STRUCT_SIZE = 20;
    protected StructNfGenMsg mNfGenMsg;

    public static byte[] newIPv4TimeoutUpdateRequest(int i, Inet4Address inet4Address, int i2, Inet4Address inet4Address2, int i3, int i4) {
        StructNlAttr structNlAttr = new StructNlAttr((short) 1, new StructNlAttr((short) 1, new StructNlAttr((short) 1, (InetAddress) inet4Address), new StructNlAttr((short) 2, (InetAddress) inet4Address2)), new StructNlAttr((short) 2, new StructNlAttr((short) 1, (byte) i), new StructNlAttr((short) 2, (short) i2, ByteOrder.BIG_ENDIAN), new StructNlAttr((short) 3, (short) i3, ByteOrder.BIG_ENDIAN)));
        StructNlAttr structNlAttr2 = new StructNlAttr((short) 7, i4, ByteOrder.BIG_ENDIAN);
        byte[] bArr = new byte[20 + structNlAttr.getAlignedLength() + structNlAttr2.getAlignedLength()];
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        byteBufferWrap.order(ByteOrder.nativeOrder());
        ConntrackMessage conntrackMessage = new ConntrackMessage();
        conntrackMessage.mHeader.nlmsg_len = bArr.length;
        conntrackMessage.mHeader.nlmsg_type = (short) 256;
        conntrackMessage.mHeader.nlmsg_flags = (short) 261;
        conntrackMessage.mHeader.nlmsg_seq = 1;
        conntrackMessage.pack(byteBufferWrap);
        structNlAttr.pack(byteBufferWrap);
        structNlAttr2.pack(byteBufferWrap);
        return bArr;
    }

    private ConntrackMessage() {
        super(new StructNlMsgHdr());
        this.mNfGenMsg = new StructNfGenMsg((byte) OsConstants.AF_INET);
    }

    public void pack(ByteBuffer byteBuffer) {
        this.mHeader.pack(byteBuffer);
        this.mNfGenMsg.pack(byteBuffer);
    }
}
