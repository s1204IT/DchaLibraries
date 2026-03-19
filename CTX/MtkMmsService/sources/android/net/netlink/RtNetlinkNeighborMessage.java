package android.net.netlink;

import android.system.OsConstants;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RtNetlinkNeighborMessage extends NetlinkMessage {
    private StructNdaCacheInfo mCacheInfo;
    private InetAddress mDestination;
    private byte[] mLinkLayerAddr;
    private StructNdMsg mNdmsg;
    private int mNumProbes;

    private static StructNlAttr findNextAttrOfType(short s, ByteBuffer byteBuffer) {
        while (byteBuffer != null && byteBuffer.remaining() > 0) {
            StructNlAttr structNlAttrPeek = StructNlAttr.peek(byteBuffer);
            if (structNlAttrPeek != null) {
                if (structNlAttrPeek.nla_type == s) {
                    return StructNlAttr.parse(byteBuffer);
                }
                if (byteBuffer.remaining() >= structNlAttrPeek.getAlignedLength()) {
                    byteBuffer.position(byteBuffer.position() + structNlAttrPeek.getAlignedLength());
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    public static RtNetlinkNeighborMessage parse(StructNlMsgHdr structNlMsgHdr, ByteBuffer byteBuffer) {
        RtNetlinkNeighborMessage rtNetlinkNeighborMessage = new RtNetlinkNeighborMessage(structNlMsgHdr);
        rtNetlinkNeighborMessage.mNdmsg = StructNdMsg.parse(byteBuffer);
        if (rtNetlinkNeighborMessage.mNdmsg == null) {
            return null;
        }
        int iPosition = byteBuffer.position();
        StructNlAttr structNlAttrFindNextAttrOfType = findNextAttrOfType((short) 1, byteBuffer);
        if (structNlAttrFindNextAttrOfType != null) {
            rtNetlinkNeighborMessage.mDestination = structNlAttrFindNextAttrOfType.getValueAsInetAddress();
        }
        byteBuffer.position(iPosition);
        StructNlAttr structNlAttrFindNextAttrOfType2 = findNextAttrOfType((short) 2, byteBuffer);
        if (structNlAttrFindNextAttrOfType2 != null) {
            rtNetlinkNeighborMessage.mLinkLayerAddr = structNlAttrFindNextAttrOfType2.nla_value;
        }
        byteBuffer.position(iPosition);
        StructNlAttr structNlAttrFindNextAttrOfType3 = findNextAttrOfType((short) 4, byteBuffer);
        if (structNlAttrFindNextAttrOfType3 != null) {
            rtNetlinkNeighborMessage.mNumProbes = structNlAttrFindNextAttrOfType3.getValueAsInt(0);
        }
        byteBuffer.position(iPosition);
        StructNlAttr structNlAttrFindNextAttrOfType4 = findNextAttrOfType((short) 3, byteBuffer);
        if (structNlAttrFindNextAttrOfType4 != null) {
            rtNetlinkNeighborMessage.mCacheInfo = StructNdaCacheInfo.parse(structNlAttrFindNextAttrOfType4.getValueAsByteBuffer());
        }
        int iAlignedLengthOf = NetlinkConstants.alignedLengthOf(rtNetlinkNeighborMessage.mHeader.nlmsg_len - 28);
        if (byteBuffer.remaining() < iAlignedLengthOf) {
            byteBuffer.position(byteBuffer.limit());
        } else {
            byteBuffer.position(iPosition + iAlignedLengthOf);
        }
        return rtNetlinkNeighborMessage;
    }

    public static byte[] newNewNeighborMessage(int i, InetAddress inetAddress, short s, int i2, byte[] bArr) {
        StructNlMsgHdr structNlMsgHdr = new StructNlMsgHdr();
        structNlMsgHdr.nlmsg_type = (short) 28;
        structNlMsgHdr.nlmsg_flags = (short) 261;
        structNlMsgHdr.nlmsg_seq = i;
        RtNetlinkNeighborMessage rtNetlinkNeighborMessage = new RtNetlinkNeighborMessage(structNlMsgHdr);
        rtNetlinkNeighborMessage.mNdmsg = new StructNdMsg();
        rtNetlinkNeighborMessage.mNdmsg.ndm_family = (byte) (inetAddress instanceof Inet6Address ? OsConstants.AF_INET6 : OsConstants.AF_INET);
        rtNetlinkNeighborMessage.mNdmsg.ndm_ifindex = i2;
        rtNetlinkNeighborMessage.mNdmsg.ndm_state = s;
        rtNetlinkNeighborMessage.mDestination = inetAddress;
        rtNetlinkNeighborMessage.mLinkLayerAddr = bArr;
        byte[] bArr2 = new byte[rtNetlinkNeighborMessage.getRequiredSpace()];
        structNlMsgHdr.nlmsg_len = bArr2.length;
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr2);
        byteBufferWrap.order(ByteOrder.nativeOrder());
        rtNetlinkNeighborMessage.pack(byteBufferWrap);
        return bArr2;
    }

    private RtNetlinkNeighborMessage(StructNlMsgHdr structNlMsgHdr) {
        super(structNlMsgHdr);
        this.mNdmsg = null;
        this.mDestination = null;
        this.mLinkLayerAddr = null;
        this.mNumProbes = 0;
        this.mCacheInfo = null;
    }

    public StructNdMsg getNdHeader() {
        return this.mNdmsg;
    }

    public InetAddress getDestination() {
        return this.mDestination;
    }

    public byte[] getLinkLayerAddress() {
        return this.mLinkLayerAddr;
    }

    public int getRequiredSpace() {
        int iAlignedLengthOf = this.mDestination != null ? 28 + NetlinkConstants.alignedLengthOf(this.mDestination.getAddress().length + 4) : 28;
        if (this.mLinkLayerAddr != null) {
            return iAlignedLengthOf + NetlinkConstants.alignedLengthOf(4 + this.mLinkLayerAddr.length);
        }
        return iAlignedLengthOf;
    }

    private static void packNlAttr(short s, byte[] bArr, ByteBuffer byteBuffer) {
        StructNlAttr structNlAttr = new StructNlAttr();
        structNlAttr.nla_type = s;
        structNlAttr.nla_value = bArr;
        structNlAttr.nla_len = (short) (4 + structNlAttr.nla_value.length);
        structNlAttr.pack(byteBuffer);
    }

    public void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        this.mNdmsg.pack(byteBuffer);
        if (this.mDestination != null) {
            packNlAttr((short) 1, this.mDestination.getAddress(), byteBuffer);
        }
        if (this.mLinkLayerAddr != null) {
            packNlAttr((short) 2, this.mLinkLayerAddr, byteBuffer);
        }
    }

    @Override
    public String toString() {
        String hostAddress = this.mDestination == null ? "" : this.mDestination.getHostAddress();
        StringBuilder sb = new StringBuilder();
        sb.append("RtNetlinkNeighborMessage{ nlmsghdr{");
        sb.append(this.mHeader == null ? "" : this.mHeader.toString());
        sb.append("}, ndmsg{");
        sb.append(this.mNdmsg == null ? "" : this.mNdmsg.toString());
        sb.append("}, destination{");
        sb.append(hostAddress);
        sb.append("} linklayeraddr{");
        sb.append(NetlinkConstants.hexify(this.mLinkLayerAddr));
        sb.append("} probes{");
        sb.append(this.mNumProbes);
        sb.append("} cacheinfo{");
        sb.append(this.mCacheInfo == null ? "" : this.mCacheInfo.toString());
        sb.append("} }");
        return sb.toString();
    }
}
